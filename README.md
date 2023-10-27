# nuxeo-ldt-parser

⚠️ ⚠️ ⚠️ This is **W**ork **I**n **P**rogress ⚠️ ⚠️ ⚠️

(GitHub used as backup for now)
 
## Description
Goal is to build a configurable parser for LDT file. Most would be configured in XML (Regex patterns for extracting lines, mapping with fields, callbacks if custom tunig is required, …)

=> **See `ldtparser-service.xml` for configuration properties**

What is ready "now" => Parsing an LDT, extract fields and avlues, map to a DocType and schema(s), retrieve.


## About LDT Parsing

The `Document.ParseAndCreateStatements` operation (`nuxeo.ldt.parser.automation.LDTParseAndCreateStatementsOp`) parses the input blob and creates as many `LDTRecord` as found in the LDT file. (doc type is configurable, `recordDocType`. If using a custom one, it must have the `LDTRecord` facet)

These records are created in a container (doc type configurable, `recordsContainerDocType`) at same level than the LDT document itself. The title of this container is `{LDT document title}-Records`. This suffix also is configurable (`recordsContainerSuffix`)

Each record also has the `ldtrecord` schema that contains retrieval information (ldt source doc id, and the startOffst/recordSize properties, used when getting the record)



## Build and run

Without building Docker:

```
cd /path/to/nuxeo-ldt-parser
mvn clean install -DskipDocker=true
```

⚠️ **WARNING** ⚠️

As this is **W**ork **I**n **P**rogress, tests that don't pass (yet ;-)) are `@Ignore`


## Support

**These features are not part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.


## Licensing

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)


## About Nuxeo

[Nuxeo](www.nuxeo.com), developer of the leading Content Services Platform, is reinventing enterprise content management (ECM) and digital asset management (DAM). Nuxeo is fundamentally changing how people work with data and content to realize new value from digital information. Its cloud-native platform has been deployed by large enterprises, mid-sized businesses and government agencies worldwide. Customers like Verizon, Electronic Arts, ABN Amro, and the Department of Defense have used Nuxeo's technology to transform the way they do business. Founded in 2008, the company is based in New York with offices across the United States, Europe, and Asia.
