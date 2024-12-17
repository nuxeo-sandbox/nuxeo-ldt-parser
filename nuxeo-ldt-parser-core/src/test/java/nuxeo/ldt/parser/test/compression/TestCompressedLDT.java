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
package nuxeo.ldt.parser.test.compression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import nuxeo.ldt.parser.automation.LDTParseAndCreateDocumentsOp;
import nuxeo.ldt.parser.service.LDTParser;
import nuxeo.ldt.parser.service.LDTParserService;
import nuxeo.ldt.parser.service.elements.Record;
import nuxeo.ldt.parser.test.TestUtils;

import jakarta.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.default.config", "org.nuxeo.ecm.platform.types", "org.nuxeo.ecm.platform.tag",
        "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.scripting" })
@Deploy("nuxeo.ldt.parser.nuxeo-ldt-parser-core")
public class TestCompressedLDT {

    /*
     * The test file is test-bigger.ldt.
     * This file contains copy/paste of the simple test.ldt, so the same 3 records are
     * repeated n times, but it's OK in the a unit test.
     * The corresponding compressed ldt is test-bigger.cldt
     * WARNING: If you change the compression algorythm, change this test file.
     */
    public static final String BIGGER_LDT = "test-bigger.ldt";

    public static final int BIGGER_LDT_COUNT_RECORDS = 648;

    public static final String COMPRESSED_BIGGER_LDT = "test-bigger.cldt";

    // {"ldtrecord:startLineInLDT":26439,"ldtrecord:startOffsetInLDT":378200,"ldtrecord:recordSize":-562,
    // "dc:description":"9874567890ABC12","dc:format":"12345678901567","dc:rights":"2023","dc:source":"MARCH"}

    public static final long COMPRESSED_START_RECORD = 378200;

    // Negative is the flag to tell the parser fhe main file is compressed
    public static final long COMPRESSED_RECORD_SIZE = -562;

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected LDTParserService ldtParserService;

    protected DocumentModel createStatementsFromBiggerLdt() throws Exception {

        // Create the LDT doc
        final String LDT_DOC_NAME = "Test";

        DocumentModel doc = coreSession.createDocumentModel("/", LDT_DOC_NAME,
                nuxeo.ldt.parser.service.Constants.DOC_TYPE_LDT);

        File testFile = FileUtils.getResourceFileFromContext(BIGGER_LDT);
        assertNotNull(testFile);
        Blob blob = new FileBlob(testFile);
        blob.setMimeType("application/ldt");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = coreSession.createDocument(doc);
        coreSession.save();
        // We may set some listeners for LDT creation in the future, let's wait.
        transactionalFeature.nextTransaction();

        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(doc);
        Map<String, Object> params = new HashMap<>();
        params.put("compressLdt", true);
        DocumentModel docResult = (DocumentModel) automationService.run(ctx, LDTParseAndCreateDocumentsOp.ID, params);
        assertNotNull(docResult);
        // We may set some listeners for LDTRecord creation in the future, let's wait.
        transactionalFeature.nextTransaction();

        // Reload the doc
        docResult = coreSession.getDocument(doc.getRef());
        /*
          String testNxql = "SELECT * FROM LDTRecord";
          DocumentModelList testDocs = coreSession.query(testNxql);
          String msg = "===========================================================================\n";
          for(DocumentModel testDoc : testDocs) {
          msg += LDTParser.documentToJsonString(testDoc, null) + "\n";
          }
          msg += "===========================================================================\n";
          System.out.println(msg);
         */
        return docResult;

    }

    @Test
    public void shouldCreateStatementsAndCompress() throws Exception {

        DocumentModel docResult = createStatementsFromBiggerLdt();

        // Check statements were created
        long countStatements = (long) docResult.getPropertyValue(
                nuxeo.ldt.parser.service.Constants.XPATH_LDT_COUNTRECORDS);
        assertEquals(BIGGER_LDT_COUNT_RECORDS, countStatements);

        // Check the file was compressed
        File testFile = FileUtils.getResourceFileFromContext(BIGGER_LDT);
        assertNotNull(testFile);
        Blob blob = new FileBlob(testFile);

        Blob resultBlob = (Blob) docResult.getPropertyValue("file:content");
        assertTrue(resultBlob.getLength() > 0);
        assertTrue(blob.getLength() > 0);
        assertTrue(resultBlob.getLength() < blob.getLength());

    }

    @Test
    public void shouldGetRecordFromCompressedLdt() throws Exception {

        DocumentModel docResult = createStatementsFromBiggerLdt();
        Blob resultBlob = (Blob) docResult.getPropertyValue("file:content");

        LDTParser parser = ldtParserService.newParser(null);
        Record record = parser.getRecord(resultBlob, COMPRESSED_START_RECORD, COMPRESSED_RECORD_SIZE);
        assertNotNull(record);
        
        TestUtils.checkSimpleTestFileRecord2Values(record);

    }
}
