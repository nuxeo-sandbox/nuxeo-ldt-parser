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

import com.fasterxml.jackson.core.JacksonException;

import nuxeo.ldt.parser.service.descriptors.LDTParserDescriptor;
import nuxeo.ldt.parser.service.LDTParser;
import nuxeo.ldt.parser.service.LDTParserService;
import nuxeo.ldt.parser.service.elements.Item;
import nuxeo.ldt.parser.service.elements.HeaderLine;
import nuxeo.ldt.parser.service.elements.Record;
import nuxeo.ldt.parser.test.TestUtils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import javax.inject.Inject;

/**
 * We are testing with the "default" parser => see its XML for the fields used in the unit test.
 * To avoid deploying Nuxeo at each start, we should create a LDTParser with hard coded LDTParserDescriptor, likely
 * using Nuxeo code to map XML to the LDTParserDescriptor.
 * <br>
 * WARNING: The misc test files with .ldt data have content that is specially tested here, like
 * recordSize expected, exact values for clientId/taxId, etc.
 * => DO NOT CHANGE THE FILES (or also change the test code)
 * Expected values for test.LDT:
 * {"ldtrecord:startLineInLDT":1,"ldtrecord:startOffsetInLDT":0,"ldtrecord:recordSize":3921,"dc:description":"1234567890ABC12","dc:format":"12345678901234","dc:rights":"2023","dc:source":"MARCH"}
 * {"ldtrecord:startLineInLDT":25,"ldtrecord:startOffsetInLDT":3921,"ldtrecord:recordSize":10126,"dc:description":"9874567890ABC12","dc:format":"12345678901567","dc:rights":"2023","dc:source":"MARCH"}
 * {"ldtrecord:startLineInLDT":76,"ldtrecord:startOffsetInLDT":14047,"ldtrecord:recordSize":17733,"dc:description":"7890567890ABC12","dc:format":"12345678907890","dc:rights":"2023","dc:source":"MARCH"}
 * 
 * @since 2021
 */

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@Deploy("nuxeo.ldt.parser.nuxeo-ldt-parser-core")
public class TestLDTParser {

    @Inject
    protected LDTParserService ldtParserService;

    @Test
    public void testIsFirstLine() {
        String line = "$12345ABCD$    TYPE=BANK0003  CLIENT TYPE: F     TAX ID: 12345678901567    CLIENT ID: 9874567890ABC12";
        LDTParser parser = ldtParserService.newParser(null);
        Assert.assertTrue(parser.isRecordStart(line));
    }

    @Test
    public void testiIsNotFirstLine() {
        String line = "003090         Ah Que Coucou             MARCH-2023      098765000";
        LDTParser parser = ldtParserService.newParser(null);
        Assert.assertFalse(parser.isRecordStart(line));
    }

    @Test
    public void testParseFirstLine() {
        String line = "$12345ABCD$    TYPE=BANK0003  CLIENT TYPE: F     TAX ID: 12345678901567    CLIENT ID: 9874567890ABC12";

        LDTParser parser = ldtParserService.newParser(null);
        HeaderLine parsedLine = parser.parseRecordHeader(line, 1);
        assertEquals("BANK0003", parsedLine.getValue("bankType"));
        assertEquals("F", parsedLine.getValue("clientType"));
        assertEquals("12345678901567", parsedLine.getValue("taxId"));
        assertEquals("9874567890ABC12", parsedLine.getValue("clientId"));

    }

    @Test
    public void shouldFailParsingFirstLine() {
        // TYPE should be BANK{4digits}
        String line = "$12345ABCD$    TYPE=0003  CLIENT TYPE: F     TAX ID: 12345678901567    CLIENT ID: 9874567890ABC12";

        LDTParser parser = ldtParserService.newParser(null);
        @SuppressWarnings("unused")
        HeaderLine parsedLine = parser.parseRecordHeader(line, 1);
        assertNull(parsedLine);
    }

    @Test
    public void testParseSecondLine() {
        String line = "003090         Ah Que Coucou             MARCH-2023      098765000";
        LDTParser parser = ldtParserService.newParser(null);
        HeaderLine parsedLine = parser.parseRecordHeader(line, 1);
        assertEquals("003090", parsedLine.getValue("bankId"));
        assertEquals("Ah Que Coucou", parsedLine.getValue("clientName"));
        assertEquals("MARCH", parsedLine.getValue("month"));
        assertEquals("2023", parsedLine.getValue("year"));
        assertEquals("098765000", parsedLine.getValue("customRef"));
    }

    @Test
    public void shouldFailParsingSecondLine() {
        // Month should be an uppercase month, in English
        String line = "003090         Ah Que Coucou             march-2023      098765000";

        LDTParser parser = ldtParserService.newParser(null);

        HeaderLine parsedLine = parser.parseRecordHeader(line, 1);
        assertNull(parsedLine);

    }

    @Test
    public void testParseItemWithCallback() {

        String line = "Nothing useful here, the CallbacksExample class hardcodes the values";

        LDTParser parser = ldtParserService.newParser(null);
        LDTParserDescriptor config = parser.getDescriptor();
        config.setUseCallbackForItems(true);
        // The class is already set in the config, we use the CallbacksExample class
        Item item = parser.parseItem(line);
        /*
         * ================================================================================
         * It is fundamental to reset the value to its original state, since the service
         * is not reinitialized for every test method => if you use a configuration setter
         * the value stays modified for the next tests.
         * ================================================================================
         */
        config.setUseCallbackForItems(false);

        // Our callback does not do a lot
        assertEquals(line, item.getLine());
        assertEquals("TheType", item.getType());
        assertEquals("value1", item.getValue("testField1"));
        assertEquals("value2", item.getValue("testField2"));

    }

    protected void testParseBalanceItem(String line, String type, String lineCode, String date, String amout) {

        LDTParser parser = ldtParserService.newParser(null);
        LDTParserDescriptor config = parser.getDescriptor();
        config.setUseCallbackForItems(false);
        Item item = parser.parseItem(line);

        assertNotNull(item);
        assertEquals(type, item.getType());
        assertEquals(lineCode, item.getValue("lineCode"));
        assertEquals(date, item.getValue("date"));
        assertEquals(amout, item.getValue("amount"));
    }

    @Test
    public void testParseBalanceItems() {
        // As, in the "default" parser, all balance items have the same pattern, we group all the tests here.
        // Adding extra spaces, and minus sign here and there

        String line = "2 01/03    OPENING BALANCE                                     999.77";
        testParseBalanceItem(line, "OpeningBalance", "2", "01/03", "999.77");

        line = "2 01/03    INTERMEDIATE BALANCE                                     999.77-";
        testParseBalanceItem(line, "IntermediateBalance", "2", "01/03", "999.77-");

        line = "2 01/03    PREVIOUS BALANCE                                     999.77  ";
        testParseBalanceItem(line, "PreviousBalance", "2", "01/03", "999.77");

        line = "2 01/03    CLOSING BALANCE                                     999.77-    ";
        testParseBalanceItem(line, "ClosingBalance", "2", "01/03", "999.77-");

    }

    @Test
    public void testParseNoBalanceItem() {
        String line = "2   01/03     KT11 IS78 IS78                499.95-          MQ47";

        LDTParser parser = ldtParserService.newParser(null);
        Item item = parser.parseItem(line);
        LDTParserDescriptor config = parser.getDescriptor();
        config.setUseCallbackForItems(false);

        assertNotNull(item);
        assertEquals("ItemLine", item.getType());
        assertEquals("2", item.getValue("lineCode"));
        assertEquals("01/03", item.getValue("date"));

        assertEquals("KT11 IS78 IS78", item.getValue("label"));
        assertEquals("499.95-", item.getValue("amount"));
        assertEquals("MQ47", item.getValue("ref"));
    }

    @Test
    public void testParseRecord() throws Exception {
        String source = "$12345ABCD$    TYPE=BANK0003  CLIENT TYPE: F     TAX ID: 12345678901234    CLIENT ID: 1234567890ABC12\n"
                + "003090         John & Marie DOE          MARCH-2023      098765432\n" + "2\n" + "2\n"
                + "2   01/03    OPENING BALANCE                                                    999.77\n"
                + "2   01/03     MZ68 QN45 IS78 IS78           657.20-          NF99\n"
                + "2   01/03     KT11 IS78 IS78                499.95-          MQ47\n"
                + "2   02/03     RW60 EQ48                    1084.94-          HD29\n"
                + "2   04/03     BV97 ZH47                      27.00-          HO61\n"
                + "2   08/03     NK31 RH52 IS78                658.75           XD25\n"
                + "2   17/03     MR82 GQ22                      15.48-          BX97\n"
                + "2   17/03     WD79 XJ33 IS78 IS78           536.00-          NF99\n"
                + "2   18/03     GB27 RJ68                    1084.17-          MQ47\n"
                + "2   18/03     JD80 QB89 IS78 IS78 IS78       77.90           HD29\n"
                + "2   22/03     NC53 SX22                      75.89           HO61\n"
                + "2   23/03     PX89 PV25 IS78               1002.54-          XD25\n"
                + "2   23/03     WY60 VB47 IS78                928.07-          NF99\n"
                + "2   23/03     KT22 XK12                     692.09-          MQ47\n"
                + "2   23/03     ZI72 KM41 IS78 IS78           628.92-          HD29\n"
                + "2   24/03     DC32 HP97                      97.31-          HO61\n"
                + "2   25/03     AS23 KB27                     297.29-          HO61\n"
                + "2   26/03     PF69 DX81 IS78                363.84-          XD25\n"
                + "2   26/03     AC72 EB81 IS78               1111.51-          NF99\n"
                + "2   26/03     OV19 YM45 IS78 IS78 IS78      600.25-          MQ47\n"
                + "2   26/03     UI61 IA28 IS78                761.30-          MQ47\n"
                + "2 26/03    CLOSING BALANCE                                                   8575.55-";

        List<String> lines = List.of(source.split("\n"));

        LDTParser parser = ldtParserService.newParser(null);
        Record record = parser.parseRecord(lines);
        LDTParserDescriptor config = parser.getDescriptor();
        config.setUseCallbackForItems(false);

        assertNotNull(record);
        assertEquals("BANK0003", record.getHeadersValue("bankType"));
        assertEquals("F", record.getHeadersValue("clientType"));
        assertEquals("12345678901234", record.getHeadersValue("taxId"));
        assertEquals("1234567890ABC12", record.getHeadersValue("clientId"));
        assertEquals("003090", record.getHeadersValue("bankId"));
        assertEquals("John & Marie DOE", record.getHeadersValue("clientName"));
        assertEquals("MARCH", record.getHeadersValue("month"));
        assertEquals("2023", record.getHeadersValue("year"));
        assertEquals("098765432", record.getHeadersValue("customRef"));

        assertEquals(22, record.getItems().size());
    }

    @Test
    public void testGetRecordSuccess() {
        Blob blob = TestUtils.getSimpleTestFileBlob();
        LDTParser parser = ldtParserService.newParser(null);
        Record record = parser.getRecord(blob, TestUtils.SIMPLELDT_RECORD2_STARTOFFSET,
                TestUtils.SIMPLELDT_RECORD2_RECORDSIZE);

        // System.out.println(record.toString());

        TestUtils.checkSimpleTestFileRecord2Values(record);
    }

    @Test
    public void testGetRecordFailure() {

        Blob blob = TestUtils.getSimpleTestFileBlob();
        LDTParser parser = ldtParserService.newParser(null);
        Record record = parser.getRecord(blob, 90000000, 10000);
        Assert.assertNull(record);
    }

    @Test
    public void testRecordObject2Json() throws JacksonException {

        Blob blob = TestUtils.getSimpleTestFileBlob();
        LDTParser parser = ldtParserService.newParser(null);
        Record record = parser.getRecord(blob, TestUtils.SIMPLELDT_RECORD2_STARTOFFSET,
                TestUtils.SIMPLELDT_RECORD2_RECORDSIZE);

        assertNotNull(record);
        String jsonStr = record.toJson();
        assertNotNull(jsonStr);
        //System.out.println(jsonStr);
        TestUtils.checkSimpleTestFileRecord2Values(jsonStr);

    }

    @Test
    public void testRecordObject2JsonNoRoot() throws JacksonException {

        Blob blob = TestUtils.getSimpleTestFileBlob();
        LDTParser parser = ldtParserService.newParser(null);
        Record record = parser.getRecord(blob, TestUtils.SIMPLELDT_RECORD2_STARTOFFSET,
                TestUtils.SIMPLELDT_RECORD2_RECORDSIZE);
        assertNotNull(record);

        LDTParserDescriptor parserDesc = parser.getDescriptor();
        parserDesc.getRecordJsonTemplate().setRootName(null);
        String jsonStr = record.toJson();
        parserDesc.getRecordJsonTemplate().setRootName("record");
        assertNotNull(jsonStr);
        
        TestUtils.checkSimpleTestFileRecord2Values(jsonStr, false);

    }

}
