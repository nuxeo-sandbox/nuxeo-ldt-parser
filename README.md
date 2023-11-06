# nuxeo-ldt-parser

nuxeo-ldt-parser provides a **configurable service for parsing LDT files**.

LDT files usually contain a lot of data, several thousands, sometimes hundreds of thousands, and this makes billions after a while (example: LDT file holding bank statements for all the customers).

The parser aims to store only retrieval information and optional custom fields:

* Retrieval information: See below, it is about quickly getting all the info inside the LDT file without re-parsing it
* Custom fields: Only the fields needed for the business, typically for search (like a clientID for example)

So, still with the bank statement example, we would store only some bytes for retrieval and a couple fields for searching, but not the dozens of lines of transactions, these will be retrieved only when it is time to download a rendition of the statement, with all its lines of transaction.

## Description

> [!NOTE]
> For the rest of this documentation, we name "Record" the set of information saved in the LDT file. Such file holds _n_ lines defining _m_ records. A record typically has one or more headers and then several items.

### Parse the File, extract _Records_, with _header(s)_ and _items_

The plugin parses an LDT file and extracts _records_ based on configuration. _Records_ can be saved as Nuxeo `Documents`, with utility fields required for a fast retrieval (see below) plus any custom field you need (if the ldt holds bank statements, you would store client name, amounts, dates, …).

The plugin parses the file line by line and expects 2 kinds of lines: A line is either a _header_ or an _item_:

* A _header_ usually defines fields/values shared by the _record_. In a bank statement example, it will be the date of the statement, the client Id, the bank account, etc.

> [!IMPORTANT]
> The plugin expects that every _record_ has at least one header and this header starts with the `startRecordToken` defined in the contribution.

* After the header(s), come the _items_. In the bank statement example, it will be several lines with the date, the reason, the amount, etc.


### Rendering a _Record_
To render a record, you will first get its JSON and then render it as you need. In the unit tests, we provide rendering examples: render to html (using freemarker for templating), render to pdf (which actually is a rendering to html then a conversion to pdf)


### Retrieving a _Record_ inside the LDT File
The plugin provides the `LDTRecord` facet that comes with the `ldtrecord` schema, and a `LDTRecord` document type that has this facet (the configuration allows for using another document type, as long as you give it the `LDTRecord` facet.)

The `ldtrecord` schema stores:

* The ID of the related Nuxeo document storing the LDT file
* The start offset and record size (in bytes) of the _record_ inside the ldt file.

So, when a _record_ needs to be fetched from the document, we just get the required bytes in the original ldt file, no need to re-parse the whole file to find a record. Notice this also works if you store your binaries in S3 using nuxeo-s3-binary-storage (see below, "S3 BlobProvider Configuration").


### Configuration
Typically, you contribute the LDTParserService, "ldtParser" extension point to define :

- The Start and End of _record_ tokens
- As many Regex as needed to parse the _header(s)_ and the _items_ of the _record_, with field mapping to your Nuxeo document (so you map the "clientId" header to the `statement:clientId` field of your document)

  > [!IMPORTANT]
  > The plugin uses `java.util.regex.Pattern` for handling Regex. Make sure your expressions are compatible with this usage.
 
* The JSON template to use when reading a record inside the LDT
* Document type(s) to create (they must be declared with the "LDTRecord" facet)
* Callbacks (java) for fine tuning when needed (mainly, when a Regex can't resolve a line). There also is an Automation Callback for quick test/POC (see below "Automation Callback for Items")

 > [!NOTE]
> See `ldtparser-service.xml` for a full example and all the configuration properties


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
* Has 2 lines of _headers_ with fields like bank type, client id, client name, etc. Then has n lines of items.
* _Items_ start with an `OPENING BALANCE` and end with a `CLOSING BALANCE`
* The record itself ends with `CLOSING BALANCE`

=> See the "default" pdtParser contributed in `/resources/OSGI-INF/ldtparser-service.xml` for the values used to parse this ldt. Here we just put the fields needed for the example, assuming we are mapping to the default `LDTRecord` document type.

#### The contribution must define/declare:

* A new parser with a unique name
* Start/end record tokens
* 2 regex and captured fields for the _headers_
* 3 regex and captured fiels for the _items_
* A mapping between the fields captured and XPaths in an `LDTRecord` document
* The JSON properties we want when getting the JSON of the record

#### Details.

* **Declare the contribution** and a **new parser** with a unique name:

```
<extension target="nuxeo.ldt.parser.service.LDTParser"
		    point="ldtParser">
  <ldtParser>
    <name>MyParser</name>
```

* Declare the **start/end of a record**:

```
    <recordStartToken>$12345ABCD$</recordStartToken>
    <recordEndToken>CLOSING BALANCE    </recordEndToken>
```

* Now **parse the _headers_**. We have 2 lines of _header_ here:
  * First one starts with the `startRecordToken` and has some fields we want to capture in a Regex:
 
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

* **Then we have _items_**. Here we have 3 kinds:
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
* Optionally, any other fields used in the XML configuration for the mapping (for example, a "statement:clientId", "statement:month", "statement:year", etc.
> [!NOTE]
> There is no mapping for the _items_


These documents are created in a container (doc type configurable, `recordsContainerDocType`) at the same level as the LDT document itself. The title of this container is `{LDT document title}-Records`. This suffix is also configurable (`recordsContainerSuffix`).

The `Services.LDTParseAndCreateRecords` operation parses the input `Document` (whose `file:content` must store an LDT file) and creates as many `LDTRecord` as found in the LDT file. (doc type is configurable, see `recordDocType`. If using a custom one, it must have the `LDTRecord` facet).

Typically, you would use these in a listener/Studio Event Handler when a new document containing an LDT file in its main blob is created. Of course we strongly recommend using an asynchronous-post commit event.

> [!NOTE]
> An entry is added to `server.log` every 1,000 _records_. This is done at `INFO` level, so if you want to see this, change the log level for this class in the `log4j2.xml` file:

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
The plugin provides:

* Automation Operations
* And an Automation Callback for quick test/POC (see below "Automation Callback for Items")

Automation Operations are:

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


## Callbacks

#### Java Callbacks
As it may happen that defining Regex Patterns may not fullfill a custom business rule, the plugin provides 3 callbacks that can be used instead: One for parsing a header, one for an item and one for the whole record.

This is set at configuration level, and it is not possible to mix Regex pattern and callbacks. If a callback is defined it is used in place of the regex.

So, you can set `useCallbackForHeaders`, `useCallbackForItems` and `useCallbackForRecord`. See the `Callbacks` interface for the signature, and `CallbacksExample` for an example (also look at the unit tests). Notice that if you use a callback for the whole record, the callbacks on header/item will never be called. 

#### Automation Callback for Items
For quick tests and POC, it may be convenient to use Automation. This is implemented only when parsing items (not headers, not the whole record), since headers should be a fixed format anyway.

> [!WARNING]
> The callback will be called for every line that is not a header. This is done only when getting a record inside the LDT file, not when parsing it to create all the `LDTRecord`. Still, for each line, all the machinery of Automation will be instantiated, which means it will be far slower that using a java callback.
> 
> If a record has a dozen items and there are not dozens, hundreds of concurrent request, it will be fine. But remember it does not scale. It is still very usefull for quick tests and POC.

You set up the chain to call in the configuration, using the `parseItemAutomationCallback` configuration property. The chain receives no input and two parameters:

* `line`, the line to parse
* And `config`,  the `LDTParserDescriptor`, that allows, should you need so, for accessing the other configuration parameters (record start/end tokens for example)

Your chain must return a `JSONBlob`, a JSON that defines all the properties of an `Item` object:

```
{
  "type": "TheLineType",
   "fieldList": ["field1", "field2, "field3"], // String array:
   "fieldsAndValues": [ // array of objects key/value. ALL VALUES ARE STRING
     {"field": "field1", "value": "value1"},
     {"field": "field2", "value2": "value3"},
     {"field": "field3", "value4": "value3"}
   ]
}
```

Here is a small example:

```
function run(input, params) {
  
  var line = params.line;
  var config = params.config;
  var regex;
  var match = null;
  var jsonItem = {};
  
  // =============================================
  // Detect las line
  // =============================================
  if(line.indexOf(config.getRecordEndToken()) > -1) {
    // Last item of the record, easy to parse with a Regex
    regex = /(\d+)\s+(\d{2}\/\d{2})\s+CLOSING BALANCE\s+([\d.]+)/;
    match = line.match(regex);
    jsonItem.type = "ClosingBalance";
    jsonItem.fieldList = ["date", "amount"];
    jsonItem.fieldsAndValues = [
      {"field": "date", "value": match[2]},
      {"field": "amount", "value": match[3]},
    ];
      
    return org.nuxeo.ecm.core.api.Blobs.createJSONBlob(JSON.stringify(jsonItem));
  }
  
  // =============================================
  // Something based on the length of the line
  // =============================================
  if(line.length < 60) {
    return null;
  }
  
  var date = line.substring(3, 8);
  var label = line.substring(12, 50).trim();
  var amount = line.length >= 61 ? line.substring(50, 61).trim() : line.substring(50, 60).trim();
  var balance = line.length >= 80 ? line.substring(62, 81).trim() : null;
  jsonItem.type = "Item";
  jsonItem.fieldList = ["date", "label", "amount"];
  jsonItem.fieldsAndValues = [
    {"field": "date", "value": date},
    {"field": "amount", "value": label},
    {"field": "amount", "value": amount},
  ];
    
  return org.nuxeo.ecm.core.api.Blobs.createJSONBlob(JSON.stringify(jsonItem));

```


## Example of Usage with Nuxeo Studio

[Nuxeo Studio](https://doc.nuxeo.com/studio/nuxeo-studio/) is Nuxeo **Low-Code configuration tool**. If your LDT files don't need Java callback, the usage of the plugin is even easier:

1. In Studio, add as many XML configurations as parsers you want to use (when you have different content for your LDT files)
1. Use the `Services.LDTParseAndCreateDocuments` operation to create the Documents from the LDT file
    * You would typically use an _async_ EventHandler for the `documentCreated` event, filtered on your document type, so the parsing is automatic. Make sure it is asynchronous
1. If you just need to get the JSON, then call `Services.GetLDTJsonRecord` when needed
2. If you need a different rendering:
    * Get the record as JSON, then yse _freemarker_ and a template (see the example in the unit tests).
    * For example, an HTML template
    * Use Nuxeo Template Rendering (with freemarker) to render a word/pdf document
    * . . . use any template you want
    * The principle is to inject the values of the JSON

Typically, and still with the banck statement example (as used in the unit test and the "default" parser), you would...

* Insert values, like (see below for the usage of the `Context` object):

```
<div class="label">Bank Id</div><br/>
<div>${Context.statement.bankId}</div>

<div class="label">Date</div><br/>
<div>${Context.statement.month}/${Context.statement.year}</div>
```

For items, using a table:

```
<table class="operations">
  <#list Context.items as item>
    <tr>
      <td class="date">${item.date}</td>
      <td>${item.label}</td>
      <#if item.amount < 0>
        <td class="amount negative">${item.amount}</td>
      <#else>
        <td class="amount">${item.amount}</td>
      </#if>
    </tr>
  </#list>
</table>
```

* Friom Studio, as `Services.GetLDTJsonRecord` returns a String, converted to JSON and `freemarker` expects more Java objects, you may need to "massage" a bit the values. Again, see the unit test for an example (automation-render-pdf-with-any2pdf.xml). Something like:

```
function run(input, params) {
  // input is an LDT Record
  var jsonBlob = Services.GetLDTJsonRecord(input, {'parserName': "MyCustomParser"});
  var json = JSON.parse(jsonBlob.getString());

  // Make sure items are ordered. In our "MySuctomParser", we defined the root of JSON as "statement"
  json.statement.items.sort(function(a, b) {
    return a.order - b.order;
  });
  
  // We also add missing fields, set them to "" and convert the negative string to number
  // This happens when different types of items are used (here, our opening balance items don't have a "customRef", for example)
  json.statement.items.forEach(function(item) {
    if(!item.label) {
      item.label = "";
    }
    if(!item.ref) {
      item.ref = "";
    }
    if(item.amount.endsWith("-")) {
      item.amount = item.amount.replace("-", "");
      item.amount = +item.amount;
      item.amount *= -1;
    } else {
      item.amount = +item.amount;
    }
  });

  // The template expects a "statement" context variable...
  ctx.statement = json.statement;
  // .. and an "items" Java array of objects, we need a conversion
   ctx.items = Java.to(json.statement.items);

  // Now we can render as html using our template.
  // We use the input as a convenience (it is not modified)
  var html = Render.Document(dummyDoc,{
                    template: "template:BankStatementTemplate",
                    filename: "statement.html",
                    mimetype: "text/html",
                    type: "ftl"
                  });

  // Return the html blob
  return var;
}
```


## S3 `BlobProvider` Configuration

Retrieval is Super Fast also when Using S3.

As explained above (_Retrieving a Record inside the LDT File_), the plugin, when parsing an ldt file, creates documents and store retrieval information in the `ldtrecord`. This way, when a single record is requested, the plugin just gets the bytes without parsing the file.

This works exactly the same if you store your binaries on S3 using [Nuxeop S3 Online Storage plugin](https://doc.nuxeo.com/nxdoc/amazon-s3-online-storage/), the plugin will directly read the `recordSize` bytes from the file on S3: No need to first download the file from S3 to local storage (typically an EBS column). This is extremely performant, because it makes no sense to download locally a 600MB file from S3 just to get 2KB from it. It would not scale : Imagine, 50 concurrent users asking their statement from 50. different big LDT files.

It is transparent. The only thing to do is add the `allowByteRange` property to the configuration. The contribution to add is:

```
<require>s3</require>
<extension target="org.nuxeo.ecm.core.blob.BlobManager" point="configuration">
  <blobprovider name="default">
    <property name="allowByteRange">true</property>
  </blobprovider>
</extension>
```


## Build and run

Without building Docker:

```
cd /path/to/nuxeo-ldt-parser
mvn clean install -DskipDocker=true
```

To test with a S3 BinaryStore, see the `testLDTParserWithS3BinaryStore` class. You need to setup the following environment variables:

* For accessing AWS: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` and `AWS_REGION`
* Info on the bucket: `TEST_BUCKET` and `TEST_BUCKET_PREFIX`

## Support

> [!IMPORTANT]
> These features are not part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.


## Licensing

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)


## About Nuxeo

[Nuxeo](www.nuxeo.com), developer of the leading Content Services Platform, is reinventing enterprise content management (ECM) and digital asset management (DAM). Nuxeo is fundamentally changing how people work with data and content to realize new value from digital information. Its cloud-native platform has been deployed by large enterprises, mid-sized businesses and government agencies worldwide. Customers like Verizon, Electronic Arts, ABN Amro, and the Department of Defense have used Nuxeo's technology to transform the way they do business. Founded in 2008, the company is based in New York with offices across the United States, Europe, and Asia.
