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
import nuxeo.ldt.parser.service.descriptors.LDTParserDescriptor;
import nuxeo.ldt.parser.service.elements.Item;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

/**
 * (IMPORTANT => See comments in TestLDTParser)
 * 
 * @since 2021
 */

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy("nuxeo.ldt.parser.nuxeo-ldt-parser-core")
public class TestLDTParserAutomationCallbacks {

    @Inject
    protected LDTParserService ldtParserService;
    
    @Test
    @Deploy("nuxeo.ldt.parser.nuxeo-ldt-parser-core:automation-callback/automation-parseitem-callback.xml")
    public void shouldUseAutomationCallback() {
        
        LDTParser parser = ldtParserService.newParser("test-automation-callback");
        LDTParserDescriptor config = parser.getDescriptor();
        assertNotNull(config.getParseItemAutomationCallback());
        
        String line = "2 01/03    OPENING BALANCE                                     999.77";
        Item item = parser.parseItem(line);
        assertNotNull(item);
        // See automation-parseitem-callback.xml
        assertEquals("DONE", item.getValue("UNIT_TEST"));
        
        line = "2  30/03    This one is special      12345678ABCDEF       100.00            950.00 ";
        item = parser.parseItem(line);
        assertNotNull(item);
        // See automation-parseitem-callback.xml
        assertEquals("DONE", item.getValue("UNIT_TEST"));
        assertEquals("Item6Groups", item.getType());
    }

}
