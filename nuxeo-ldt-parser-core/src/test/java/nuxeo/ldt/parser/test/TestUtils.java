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
package nuxeo.ldt.parser.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.runtime.api.Framework;

import nuxeo.ldt.parser.service.LDTParser;
import nuxeo.ldt.parser.service.LDTParserService;
import nuxeo.ldt.parser.service.elements.Record;

/**
 * Utility to centralise test file(s) and their expected values.
 * WARNING: The misc test files with .ldt data have content that is specially tested here, like
 * recordSize expected, exact values for clientId/taxId, etc.
 * ======================================================================
 * => DO NOT CHANGE THE FILES (or also change the test codes and the values here)
 * ======================================================================
 * <br>
 * Also, we are using the default mapper => see definition for fields and xpaths used
 * Expected values for test.LDT once an LDTRecord is created:
 * {"ldtrecord:startLineInLDT":1,"ldtrecord:startOffsetInLDT":0,"ldtrecord:recordSize":1692,"dc:description":"1234567890ABC12","dc:format":"12345678901234","dc:rights":"2023","dc:source":"MARCH"}
 * {"ldtrecord:startLineInLDT":27,"ldtrecord:startOffsetInLDT":1692,"ldtrecord:recordSize":1692,"dc:description":"9874567890ABC12","dc:format":"12345678901567","dc:rights":"2023","dc:source":"MARCH"}
 * {"ldtrecord:startLineInLDT":53,"ldtrecord:startOffsetInLDT":3384,"ldtrecord:recordSize":3385,"dc:description":"7890567890ABC12","dc:format":"12345678907890","dc:rights":"2023","dc:source":"MARCH"}
 * 
 * @since TODO
 */
public class TestUtils {

    public static final long SIMPLELDT_STATEMENT_COUNT = 3;

    public static final long SIMPLELDT_RECORD2_STARTOFFSET = 1692;

    public static final long SIMPLELDT_RECORD2_RECORDSIZE = 1692;

    // See "default" parser xml contribution
    public static final Map<String, String> SIMPLELDT_RECORD2_VALUES_MAP = Map.of("bankType", "BANK0003", "clientType",
            "F", "taxId", "12345678901567", "clientId", "9874567890ABC12", "bankId", "003090", "clientName",
            "Ah Que Coucou", "month", "MARCH", "year", "2023", "customRef", "098765000");

    public static final long SIMPLELDT_RECORD2_ITEMS_COUNT = 22;

    public static File getSimpleTestFile() {
        File testFile = FileUtils.getResourceFileFromContext("test.LDT");
        assertNotNull(testFile);

        return testFile;
    }

    public static Blob getSimpleTestFileBlob() {

        File testFile = getSimpleTestFile();
        Blob b = new FileBlob(testFile);
        b.setMimeType("application/ldt");
        return b;
    }

    public static void checkSimpleTestFileRecord2Values(Record record) {

        assertNotNull(record);

        assertEquals(SIMPLELDT_RECORD2_VALUES_MAP.get("bankType"), record.getHeadersValue("bankType"));
        assertEquals(SIMPLELDT_RECORD2_VALUES_MAP.get("clientType"), record.getHeadersValue("clientType"));
        assertEquals(SIMPLELDT_RECORD2_VALUES_MAP.get("taxId"), record.getHeadersValue("taxId"));
        assertEquals(SIMPLELDT_RECORD2_VALUES_MAP.get("clientId"), record.getHeadersValue("clientId"));
        assertEquals(SIMPLELDT_RECORD2_VALUES_MAP.get("bankId"), record.getHeadersValue("bankId"));
        assertEquals(SIMPLELDT_RECORD2_VALUES_MAP.get("clientName"), record.getHeadersValue("clientName"));
        assertEquals(SIMPLELDT_RECORD2_VALUES_MAP.get("month"), record.getHeadersValue("month"));
        assertEquals(SIMPLELDT_RECORD2_VALUES_MAP.get("year"), record.getHeadersValue("year"));
        assertEquals(SIMPLELDT_RECORD2_VALUES_MAP.get("customRef"), record.getHeadersValue("customRef"));

        assertEquals(SIMPLELDT_RECORD2_ITEMS_COUNT, record.getItems().size());
    }

    public static void checkSimpleTestFileRecord2Values(String jsonStr) {

        checkSimpleTestFileRecord2Values(jsonStr, true);
    }

    public static void checkSimpleTestFileRecord2Values(String jsonStr, boolean hasRoot) {

        assertNotNull(jsonStr);

        JSONObject mainJson = new JSONObject(jsonStr);
        assertNotNull(mainJson);

        JSONObject rootElement;
        if (hasRoot) {
            rootElement = mainJson.getJSONObject("record");
        } else {
            rootElement = mainJson;
        }
        assertNotNull(rootElement);

        assertEquals(TestUtils.SIMPLELDT_RECORD2_VALUES_MAP.get("clientId"), rootElement.get("clientId"));
        assertEquals(TestUtils.SIMPLELDT_RECORD2_VALUES_MAP.get("taxId"), rootElement.get("taxId"));

        JSONArray items = rootElement.getJSONArray("items");
        assertEquals(TestUtils.SIMPLELDT_RECORD2_ITEMS_COUNT, items.length());

        JSONObject item = (JSONObject) items.get(0);
        assertEquals("OpeningBalance", (String) item.get("type"));
        assertEquals("999.77", (String) item.get("amount"));

        item = (JSONObject) items.get(items.length() - 1);
        assertEquals("ClosingBalance", (String) item.get("type"));
        assertEquals("8575.55-", (String) item.get("amount"));
    }
    
    // This is utility to generate some test html
    public static void record2Html() throws Exception {
        
        Blob blob = TestUtils.getSimpleTestFileBlob();
        LDTParser parser = Framework.getService(LDTParserService.class).newParser(null);
        Record record = parser.getRecord(blob, TestUtils.SIMPLELDT_RECORD2_STARTOFFSET,
                TestUtils.SIMPLELDT_RECORD2_RECORDSIZE);

        assertNotNull(record);
        String jsonStr = record.toJson();
        
        JSONObject mainJson = new JSONObject(jsonStr);
        assertNotNull(mainJson);

        JSONObject rootElement = mainJson.getJSONObject("record");

        StringBuilder sb = new StringBuilder();
        
        sb.append("<body>\n<main>\n");
        
        sb.append("<div class=\"info\">\n");
        sb.append("<div>\n<div class=\"label\">Bank Id</div>\n<div>003090</div>\n</div>\n");
        sb.append("<div>\n<div class=\"label\">Tax Id</div>\n<div>12345678901567</div>\n</div>\n");
        sb.append("<div>\n<div class=\"label\">Client Id</div>\n<div>9874567890ABC12</div>\n</div>\n");
        sb.append("<div>\n<div class=\"label\">Client Name</div>\n<div>Ah Que Coucou</div>\n</div>\n");
        sb.append("<div class=\"nolabel\">MARCH-2023</div>\n");
        sb.append("<div class=\"nolabel\">098765000</div>\n");
        sb.append("</div >");
        
        sb.append("<table class=\"operations\">\n");
        if(true) {// Just for indentation
            sb.append("<thead>\n");
            sb.append("<tr class=\"header\">\n");
            sb.append("<th class=\"date\">Date</th>\n");
            sb.append("<th>Label</th>\n");
            sb.append("<th>Amount</th>\n");
            sb.append("<th>Ref</th>\n");
            sb.append("</tr>\n");
            sb.append("</thead>\n");
        
            sb.append("<tbody>\n");
            JSONArray items = rootElement.getJSONArray("items");
            items.forEach(item -> {
                JSONObject itemObj = (JSONObject) item;
                sb.append("<tr>\n");
                sb.append("<td class=\"date\">" + itemObj.getString("date") + "</td>\n");
                String label = itemObj.optString("label");
                if(StringUtils.isBlank(label)) {
                    sb.append("<td>" + itemObj.getString("type") + "</td>\n");
                } else {
                    sb.append("<td>" + label + "</td>\n");
                }
                double amount;
                String amountStr = itemObj.getString("amount");
                if(amountStr.endsWith("-")) {
                    amountStr = amountStr.replace("-", "");
                    amount = Double.valueOf(amountStr);
                    amount *= -1;
                } else {
                    amount = Double.valueOf(amountStr);
                }
                sb.append("<td>" + amount + "</td>\n");
                sb.append("<td>" + itemObj.optString("ref") + "</td>\n");
                
                sb.append("</tr>\n");
            });
            sb.append("</tbody>\n");
        }
        sb.append("</table>\n");
        
        sb.append("</body>\n</main>\n");
        
        String html = sb.toString();
        Blob b = new StringBlob(html);
        File f = new File("/Users/thibaud/Downloads/blah.txt");
        b.transferTo(f);
    }

}
