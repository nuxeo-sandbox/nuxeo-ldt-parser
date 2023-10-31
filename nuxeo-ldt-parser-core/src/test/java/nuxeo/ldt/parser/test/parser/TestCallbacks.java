/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
package nuxeo.ldt.parser.test.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import nuxeo.ldt.parser.service.Callbacks;
import nuxeo.ldt.parser.service.descriptors.LDTParserDescriptor;
import nuxeo.ldt.parser.service.elements.HeaderLine;
import nuxeo.ldt.parser.service.elements.Item;
import nuxeo.ldt.parser.service.elements.Record;

/**
 * @since TODO
 */
public class TestCallbacks implements Callbacks {

    public static final List<String> RECORD_LINES = Arrays.asList(
            "RECORDSTART    CLIENT=12345    TAXID=67890",
            "OPENING BALANCE       1234.56",
            "ITEM blahblahblah      100.00",
            "CLOSING BALANCE       1134.56");

    /**
     * Values are totally hard coded, you can pass whatever lines you want,
     * just look at this code to check you get the correct values
     */
    @Override
    public Record parseRecord(LDTParserDescriptor config, List<String> lines) {
        List<HeaderLine> headers = new ArrayList<HeaderLine>();
        List<String> fieldList = null;
        Map<String, String> fieldsAndValues = null;
        List<Item> items = new ArrayList<Item>();

        // Hard code a simple header
        fieldList = Arrays.asList("HEADER");
        fieldsAndValues = Map.of("HEADER", "12345");
        HeaderLine header = new HeaderLine(fieldList, fieldsAndValues, 1);
        headers.add(header);

        // HArd code a simple item
        fieldList = Arrays.asList("ITEM_VALUE");
        fieldsAndValues = Map.of("ITEM_VALUE", "1234.56");
        Item item = new Item("we do not care", "ITEM", fieldList, fieldsAndValues);
        items.add(item);

        return new Record(headers, items);
    }

    @Override
    public HeaderLine parseHeader(LDTParserDescriptor config, String line, long lineNumber) {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Item parseItem(LDTParserDescriptor config, String line) {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

}
