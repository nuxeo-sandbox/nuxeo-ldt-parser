/*
 * (C) Copyright 2023 Hyland (http://hyland.com/)  and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thibaud Arguillere
 */
package nuxeo.ldt.parser.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoException;

import nuxeo.ldt.parser.service.descriptors.LDTParserDescriptor;
import nuxeo.ldt.parser.service.elements.HeaderLine;
import nuxeo.ldt.parser.service.elements.Item;
import nuxeo.ldt.parser.service.elements.Record;

/**
 * 
 * @since 2021
 */
public class CallbacksExample implements Callbacks {
    
    @Override
    public Record parseRecord(LDTParserDescriptor config, List<String> lines) {
        // So, here, you loop the lines, get headers, get items etc.
        // You would line.indexOf(), or use your Regex, etc.
        // We hard code with no relation to the lines parameter
        List<HeaderLine> headers = new ArrayList<HeaderLine>();
        List<String> fieldList = null;
        Map<String, String> fieldsAndValues = null;
        String name;
        List<Item> items = new ArrayList<Item>();
        long lineNumber = 0;
        for(String line : lines) {
            lineNumber += 1;
            if(line.startsWith(config.getRecordStartToken())) {
                // First line
                // Get values with whatever you need : indexOf(), Regex, …
                // Here, we hard code the values (with no relation to line)
                fieldList = Arrays.asList("ClientId", "TaxId");
                fieldsAndValues = Map.of("ClientId", "12345", "TaxId", "67890");
                name = "HEADER_1";
                headers.add(new HeaderLine(fieldList, fieldsAndValues, lineNumber, name));
            } else if (line.indexOf("Some Token") > -1) {
                name = "OTHER_HEADER";
                fieldList = Arrays.asList("header2_field1", "header2_field2");
                fieldsAndValues = Map.of("header2_field1", "ABCDEF", "header2_field2", "GHIJKL");
                headers.add(new HeaderLine(fieldList, fieldsAndValues, lineNumber, name));
            } else {
                // Not a header
                // Still, check we do have at least one
                if(headers.size() == 0) {
                    throw new NuxeoException("SHould have at list one header");
                }
                // Now, parsing items. We may have some kind of "opening" line, "closing" line,
                // 'detail" line, etc.:
                if(line.indexOf("Opening") > -1) {
                    // ...parse the line, create an Item
                } else if(line.indexOf("OtherLineType") > -1) {
                    // ...parse the line, create an Item
                } else if(line.indexOf("AndAnotherOne") > -1) {
                    // ...parse the line, create an Item
                } // etc.
                // Here, we hard code the values (with no relation to line)
                // So, warning; all items will be the same
                fieldList = Arrays.asList("itemField1", "itemField2");
                fieldsAndValues = Map.of("itemField1", "value1", "itemField2", "value2");
                
                items.add(new Item(line, "TheType", fieldList, fieldsAndValues));
            }
        }
        
        
        return new Record(headers, items);
    }

    @Override
    public HeaderLine parseHeader(LDTParserDescriptor config, String line, long lineNumber) {
        
        List<String> fieldList = null;
        Map<String, String> fieldsAndValues = null;
        String name;
        
        // Here, we would test the line.
        // Possibly check the config and act accordingly. For example:
        if(line.startsWith(config.getRecordStartToken())) {
            // First line, let's get the values, via RegEx or several indexOf and such
            // Here, we hard code the values (with no relation to line parameter)
            fieldList = Arrays.asList("ClientId", "TaxId");
            fieldsAndValues = Map.of("ClientId", "12345", "TaxId", "67890");
            name = "HEADER_1";
        } else /* here, some other test */{
            // ...other cases...
            fieldList = Arrays.asList("name", "monthYear");
            fieldsAndValues = Map.of("name", "John & Marie Doe", "monthYear", "NOVEMBER/2023");
            name = "HEADER_2";
        } /* else {
            return null;
        } */
        
        return new HeaderLine(fieldList, fieldsAndValues, lineNumber, name);
    }

    @Override
    public Item parseItem(LDTParserDescriptor config, String line) {
        
        // Possibly check the config and act accordingly.
        // Here, we hard code the values (with no relation to line parameter)
        List<String> fieldList = Arrays.asList("testField1", "testField2");
        Map<String, String> fieldsAndValues = Map.of("testField1", "value1", "testField2", "value2");
        
        return new Item(line, "TheType", fieldList, fieldsAndValues);
    }

}
