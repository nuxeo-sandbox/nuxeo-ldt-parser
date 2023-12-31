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
 *     Michael Vachette
 *     Thibaud Arguillere
 */

package nuxeo.ldt.parser.test.parser;


import nuxeo.ldt.parser.service.LDTParser;
import nuxeo.ldt.parser.service.LDTParserService;
import nuxeo.ldt.parser.service.elements.Item;
import nuxeo.ldt.parser.service.elements.HeaderLine;
import nuxeo.ldt.parser.service.elements.Record;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

/**
 * (IMPORTANT => See comments in TestLDTParser)
 * 
 * @since 2021
 */

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@Deploy("nuxeo.ldt.parser.nuxeo-ldt-parser-core")
@Deploy("nuxeo.ldt.parser.nuxeo-ldt-parser-core:ldt-parsers-test-contrib.xml")
public class TestLDTParserCallbacks {

    @Inject
    protected LDTParserService ldtParserService;

    @Test
    public void testCustomParsersAreLoaded() {
        
        List<String> names = Arrays.asList("test-callbacks-record", "test-callbacks-header", "test-callbacks-item");
        names.forEach(name -> {
            LDTParser parser = ldtParserService.newParser(name);
            assertNotNull(parser);
            
            assertEquals(name, parser.getName());
        });
        
    }
    
    @Test
    public void shouldUseCallbackForRecord() {
        
        LDTParser parser = ldtParserService.newParser("test-callbacks-record");
        assertNotNull(parser);
        
        // The callbacks hard code the values, whatever the input line(s)
        Record record = parser.parseRecord(Arrays.asList("some line"));
        assertNotNull(record);
        
        assertEquals("12345", record.getHeadersValue("HEADER"));
        
        List<Item> items = record.getItems();
        assertEquals(1, items.size());
        assertEquals("ITEM", items.get(0).getType());
        assertEquals("1234.56", items.get(0).getValue("ITEM_VALUE"));
    }
    
    @Test
    public void shouldUseCallbackFoHeader() {
        
        LDTParser parser = ldtParserService.newParser("test-callbacks-header");
        assertNotNull(parser);
        
        HeaderLine header = parser.parseRecordHeader("some line", 3);
        assertNotNull(header);
        assertEquals(3, header.getLineNumber());
        assertEquals("12345", header.getValue("HEADER"));
        
    }
    
    @Test
    public void shouldUseCallbackFoItem() {
        
        LDTParser parser = ldtParserService.newParser("test-callbacks-item");
        assertNotNull(parser);
        
        Item item = parser.parseItem("some line");
        assertNotNull(item);
        assertEquals("some line", item.getLine());
        assertEquals("ITEM", item.getType());
        assertEquals("1234.56", item.getValue("ITEM_VALUE"));
        
    }

}
