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

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

import nuxeo.ldt.parser.service.LDTParser.Record;

/**
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
    public static final Map<String, String> SIMPLELDT_RECORD2_VALUES_MAP = Map.of(
            "bankType", "BANK0003",
            "clientType", "F",
            "taxId", "12345678901567",
            "clientId", "9874567890ABC12",
            "bankId", "003090",
            "clientName", "Ah Que Coucou",
            "month", "MARCH",
            "year", "2023",
            "customRef", "098765000");

    public static final long SIMPLELDT_RECORD2_ITEMS_COUNT = 22;

    public static File getSimpleTestFile() {
        File testFile = FileUtils.getResourceFileFromContext("test.LDT");
        assertNotNull(testFile);

        return testFile;
    }

    public static Blob getSimpleTestFileBlob() {

        File testFile = getSimpleTestFile();
        return new FileBlob(testFile);
    }
    
    public static void checkSimpleTestFileRecord2Values(Record record) {
        
        assertNotNull(record);
        
        assertEquals(SIMPLELDT_RECORD2_VALUES_MAP.get("bankType"), record.getMainLinesValue("bankType"));
        assertEquals(SIMPLELDT_RECORD2_VALUES_MAP.get("clientType"), record.getMainLinesValue("clientType"));
        assertEquals(SIMPLELDT_RECORD2_VALUES_MAP.get("taxId"), record.getMainLinesValue("taxId"));
        assertEquals(SIMPLELDT_RECORD2_VALUES_MAP.get("clientId"), record.getMainLinesValue("clientId"));
        assertEquals(SIMPLELDT_RECORD2_VALUES_MAP.get("bankId"), record.getMainLinesValue("bankId"));
        assertEquals(SIMPLELDT_RECORD2_VALUES_MAP.get("clientName"), record.getMainLinesValue("clientName"));
        assertEquals(SIMPLELDT_RECORD2_VALUES_MAP.get("month"), record.getMainLinesValue("month"));
        assertEquals(SIMPLELDT_RECORD2_VALUES_MAP.get("year"), record.getMainLinesValue("year"));
        assertEquals(SIMPLELDT_RECORD2_VALUES_MAP.get("customRef"), record.getMainLinesValue("customRef"));

        assertEquals(SIMPLELDT_RECORD2_ITEMS_COUNT, record.getItems().size());
    }

}
