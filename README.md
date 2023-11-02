# nuxeo-ldt-parser

nuxeo-ldt-parser provides a **configurable service for parsing LDT files**.
 
## Description

<div style="margin-left:50px">
ℹ️ For the rest of this documentation, we name "Record" the set of information saved in the LDT file. Such file holds _n_ lines defining _m_ records. A record typically has one or more headers and then several items.
</div>

The plugin parses an LDT file and extracts _records_ based on configuration. _Records_ can be saved as Nuxeo `Documents`, with fields required for a fast retrieval plus any custom field you need, mapped to XPaths.

To render a record, you will get its JSON and render it (for example, using an HTML template and freemarker).

Each _Record_ stores:

* The ID of the related Nuxeo document storing the LDT file
* The start offset and record size (bytes) inside the ldt file.

So, when a record needs to be fetched, we just get the required bytes, no need to re-parse the whole file to find a record.

Typically, you contribute the LDTParserService, "ldtParser" extension point to define :

- The Start and End of _record_ tokens
- As many Regex as needed to parse the header(s) and the items of the record, with field mapping to your Nuxeo document
- The JSON template to use when reading a record inside the LDT
- Document type(s) to create (they must be declared with the "LDTRecord" facet)
- Callbacks (java) for fine tuning when needed (mainly, when a Regex can't resolve a line)

**See `ldtparser-service.xml` for a full example and all the configuration properties**

## Retrieval is Super Fast when Using S3

The plugin, when parsing an ldt file, creates documents (see below) that store retrieval information. This way, when a single record is requested, the plugin just gets the bytes from a stream in S3 => no need to first download the file from S3 to local storage (typically an EBS column). This is extremely performant, because it makes no sense to download a 600MB file just to get 2KB from it. It would not scale : Imagine, 50 concurrent users asking their statement from 50. different big LDT files.

This works when binaries are stored by Nuxeo in the S3 Binary Manager of course, it is transparent. The only thing to do is add the `allowByteRange` property to the configuration. The contribution to add is :

```
<require>s3</require>
<extension target="org.nuxeo.ecm.core.blob.BlobManager" point="configuration">
  <blobprovider name="default">
    <property name="allowByteRange">true</property>
  </blobprovider>
</extension>
```



## Simple example with 2 records

```
$12345ABCD$    TYPE=BANK0003  CLIENT TYPE: A     TAX ID: 12345678901234    CLIENT ID: 1234567890ABC12
003090         John & Marie DOE          MARCH-2023      098765432
1   01/03    OPENING BALANCE                                                    999.77
2   01/03     The label for the item           657.20-          NF99
. . . more lines . . .
3   26/03    CLOSING BALANCE                                                   8575.55-
$12345ABCD$    TYPE=BANK0003  CLIENT TYPE: B     TAX ID: 12345678901567    CLIENT ID: 9874567890ABC12
003090         ACME Ltd                  MARCH-2023      098765000
1   01/03    OPENING BALANCE                                                    10567.89
2   01/03     The label for the item           100.00          T999
. . . more lines . . .
3   31/03    CLOSING BALANCE                                                     9554.26
```

#### In this file, each _Record_:

* Starts with exactly `$12345ABCD$`
* Has 2 lines of headers with fields like bank type, client id, client name, etc. Then has n lines of items.
* Items start with an `OPENING BALANCE` and end with a `CLOSING BALANCE`
* The record itself ends with `CLOSING BALANCE`

=> see the "default" pdtParser contributed in `/resources/OSGI-INF/ldtparser-service.xml` for the values used to parse this ldt. Here we just put the fields needed for the example, assuming we are mapping to a custom document type.

#### So, our contribution must define/declare:

* A new parser with a unique name
* Start/end record tokens
* 2 regex and captured fields for the headers
* 3 regex and captured fiels for the items
* A mapping between the fields captured and XPaths in an `LDTRecord` document
* The JSON properties we want when getting the JSON of the record

#### Here are more details.

* **Declare the contribution** and a new parser:

```
<extension target="nuxeo.ldt.parser.service.LDTParser"
		    point="ldtParser">
  <ldtParser>
```

* Give it a **unique name**: `<name>MyParser</name>`
* Declare the **start/end of a record**:

```
    <recordStartToken>$12345ABCD$</recordStartToken>
    <recordEndToken>CLOSING BALANCE    </recordEndToken>
```

* Now **parse the headers**. We have 2 lines of header here:
  * First one starts with the startRecordToken and has some fields we want to capture in a Regex:

```
    <headers>
      <header>
        <name>firstLine</name>
        <pattern>^\$12345ABCD\$ *TYPE=(BANK.{4}) *CLIENT TYPE: *([a-zA-Z]) *TAX ID: *([A-Z0-9]*) *CLIENT ID: *([A-Z0-9]*)</pattern>
        <!-- fields MUST BE same number and order as the pattern groups captured above -->
        <fields>
          <field>bankType</field>
          <field>clientType</field>
          <field>taxId</field>
          <field>clientId</field>
		</fields>
	</header>
```

  * Second has more info and fields we want to capture

```
    <header>
      <name>secondLine</name>
      <pattern>^([A-Z0-9]*) *(.*?) *(JANUARY|FEBRUARY|MARCH|APRIL|MAY|JUNE|JULY|AUGUST|SEPTEMBER|OCTOBER|NOVEMBER|DECEMBER)-(\d{4}) *([A-Z0-9]*)</pattern>
      <fields>
        <field>bankId</field>
        <field>clientName</field>
        <field>month</field>
        <field>year</field>
        <field>customRef</field>
      </fields>
   </header>
 </headers>
```

* **Then we have items**. Here we have 3 kinds:
  * An opening balance:

```
  <itemLine>
    <type>OpeningBalance</type>
    <pattern>^([0-9]*) *([0-9]{2}/[0-9]{2}) *OPENING BALANCE *([0-9]*.[0-9]{2}-?) *</pattern>
    <fields>
      <field>lineCode</field>
      <field>date</field>
      <field>amount</field>
    </fields>
  </itemLine>
```
  * A closing balance:

```
  <itemLine>
    <type>ClosingBalance</type>
    <pattern>^([0-9]*) *([0-9]{2}/[0-9]{2}) *CLOSING BALANCE *([0-9]*.[0-9]{2}-?) *</pattern>
    <fields>
      <field>lineCode</field>
      <field>date</field>
      <field>amount</field>
    </fields>
  </itemLine>
```

  * And each item. We capture 5 groups here:
```
  <itemLine>
    <type>ItemLine</type>
    <pattern>^([0-9]*) *(\d{2}/\d{2}) *(.*?) *(\d+\.\d{2}-?) *([A-Z0-9]*)</pattern>
    <fields>
      <field>lineCode</field>
      <field>date</field>
      <field>label</field>
      <field>amount</field>
      <field>ref</field>
    </fields>
  </itemLine>
```

* **Now we want to use the provided `LDTRecord`**. In Nuxeo Studio, we added to it a custom schema, `statement`, which contains the XPaths we want to map to the fields defined above

```
  <recordDocType>LDTRecord</recordDocType>
  <recordFieldsMapping>
    <field xpath="statement:clientId">clientId</field>
    <field xpath="statement:taxId">taxId</field>
    <field xpath="statement:month">month</field>
    <field xpath="statement:year">year</field>
  </recordFieldsMapping>
```

  * For each `LDTRecord created, we want a title concatenating some fields (by default, the parser adds an incremented number)

```
  <recordTitleFields>
    <field>clientId</field>
    <field>taxId</field>
  </recordTitleFields>
```

* Last, when **getting the JSON of the record**, we want define the template to use:

```
<recordJsonTemplate>
  <root>statement</root>
  <properties>
    <property>bankType</property>
    <property>clientType</property>
    <property>taxId</property>
    <property>clientId</property>
    <property>bankId</property>
    <property>clientName</property>
    <property>month</property>
    <property>year</property>
    <property>customRef</property>
  </properties>
</recordJsonTemplate>
```

## Mapping _Records_ to Documents

`LDTParser#parseAndCreateRecords` parses the input `Document` (whose `file:content` _must_ store an LDT file) and creates as many `LDTRecord` as found in the LDT file. The doc type is configurable, see `recordDocType`. If using a custom one, it _must_ have the `LDTRecord` facet.

This document type has:

* The `ldtrecord` schema that is used internally to retrieve the record: ID of the source LDT document, the startBytes and the recordSize
* Optionnaly, any other fields used in the XML configuration for the mapping (for example, a "statement:clientId", "statement:month", "statement:year", etc.

These documents are created in a container (doc type configurable, `recordsContainerDocType`) at same level than the LDT document itself. The title of this container is `{LDT document title}-Records`. This suffix also is configurable (`recordsContainerSuffix`).

The `Services.LDTParseAndCreateRecords` operation parses the input `Document` (whose `file:content` must store an LDT file) and creates as many `LDTRecord` as found in the LDT file. (doc type is configurable, see `recordDocType`. If using a custom one, it must have the `LDTRecord` facet).

Typically, you would use these in a listener/Studio Event Handler when a new document containing an LDT file in its main blob is created. Of course we strongly recommend using an asynchronous-post commit event.

ℹ️ An entry is added to `server.log` every 1,000 records. This is done at `INFO` level, so if you want to see this, change the log level for this class in the `log4j2.xml` file:

```
. . .
  <Loggers>
    . . .
    <Logger name="nuxeo.ldt.parser.service.LDTParser" level="info" />
    . . .
  </Loggers>
. . .
```


## Automation
The plugin provides the following operations:

#### `Services.LDTParseAndCreateRecords`
* Input:
  * A `Document` whose `file:content` contains the LDT file to parse
  * This document _must have_ the `ldt` schema.
* Parameter:
  * `parserName`, string. Must be the name of an "ldtParser" contribution. If not passed or empty, the operation uses the `"default"` configuration
* Output:
  * The input `Document` with its `ldt` schema updated.
  * For now, this schema ha sa single field, `ldt:countRecords`.
* The operation creates as many records as found in the LDT field, using the `parserName` configuration for detecting start/end record tokens, Regex to use, fields to map to XPaths, etc.


#### `Services.GetLDTJsonRecord`
* Input:
  * A `Document` having the ù LDTRecord` facet (and, thus, schema)
* Parameter:
  * `parserName`, string. Must be the name of an "ldtParser" contribution. If not passed or empty, the operation uses the `"default"` configuration
* Output:
  * A JSON `Blob` containing the record
  * Fields of the JSON are defined in the XML configuration, using `recordJsonTemplate` (see above).
* The operation received a `Document` having the `ldtrecord` schema. It then gets the related LDT document, and fetches only the required data from it (no need, for example, to download the file from S3 to parse it locally)



## Build and run

Without building Docker:

```
cd /path/to/nuxeo-ldt-parser
mvn clean install -DskipDocker=true
```


## Support

**These features are not part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.


## Licensing

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)


## About Nuxeo

[Nuxeo](www.nuxeo.com), developer of the leading Content Services Platform, is reinventing enterprise content management (ECM) and digital asset management (DAM). Nuxeo is fundamentally changing how people work with data and content to realize new value from digital information. Its cloud-native platform has been deployed by large enterprises, mid-sized businesses and government agencies worldwide. Customers like Verizon, Electronic Arts, ABN Amro, and the Department of Defense have used Nuxeo's technology to transform the way they do business. Founded in 2008, the company is based in New York with offices across the United States, Europe, and Asia.
