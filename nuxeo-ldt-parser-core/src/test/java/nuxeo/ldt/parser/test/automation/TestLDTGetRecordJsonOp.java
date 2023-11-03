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
package nuxeo.ldt.parser.test.automation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.JSONBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import nuxeo.ldt.parser.automation.LDTGetRecordJsonOp;
import nuxeo.ldt.parser.test.TestUtils;
import nuxeo.ldt.parser.service.LDTParser;
import nuxeo.ldt.parser.service.LDTParserService;

import javax.inject.Inject;

/* See TestUtils comment for info aboout the test .LDT files
 * 
 */
@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.default.config", "org.nuxeo.ecm.platform.types", "org.nuxeo.ecm.platform.tag",
        "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.scripting" })
@Deploy("nuxeo.ldt.parser.nuxeo-ldt-parser-core")
public class TestLDTGetRecordJsonOp {

    protected final List<String> events = Arrays.asList("documentCreated");

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected LDTParserService ldtParserService;
    
    // Shared by misc tests.
    protected void checkReturnedJson(String jsonStr) {
        
        JSONObject json = new JSONObject(jsonStr);
        JSONObject root = json.getJSONObject("record");
        
        assertEquals("9874567890ABC12", root.getString("clientId"));
        assertEquals("MARCH", root.getString("month"));
        JSONArray items = root.getJSONArray("items");
        assertEquals(TestUtils.SIMPLELDT_RECORD2_ITEMS_COUNT, items.length());
    }

    @Test
    public void shouldGetJsonRecord() throws Exception {

        // ==============================
        // Setup
        // ==============================
        // We need to have a document holding an LDT file, because it is used when retrieving a simple record
        // 1. Create the LDT doc
        final String LDT_DOC_NAME = "Test";

        DocumentModel ldtDoc = coreSession.createDocumentModel("/", LDT_DOC_NAME,
                nuxeo.ldt.parser.service.Constants.DOC_TYPE_LDT);
        Blob blob = TestUtils.getSimpleTestFileBlob();
        ldtDoc.setPropertyValue("file:content", (Serializable) blob);
        ldtDoc = coreSession.createDocument(ldtDoc);
        coreSession.save();
        // We may set some listeners for LDT creation in the future, let's wait.
        transactionalFeature.nextTransaction();
        
        // 2. Create the LDTRecords, using the default parser
        LDTParser parser = ldtParserService.newParser(null);
        parser.parseAndCreateRecords(ldtDoc);
        transactionalFeature.nextTransaction();

        // * Get one LDTRecord
        // (we test the second entry in the test.LDT file)
        // We used the fields defined in the "default" contribution. taxId is in dc:format,
        // client id in dc:description, etc.
        String nxql = "SELECT * FROM LDTRecord WHERE dc:format = '12345678901567'";
        nxql += " AND dc:description = '9874567890ABC12'";
        nxql += " AND dc:source = 'MARCH'";
        nxql += " AND dc:rights = '2023'";
        DocumentModelList docs = coreSession.query(nxql);
        assertEquals(1, docs.size());

        // ==============================
        // Test
        // ==============================
        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(docs.get(0));
        // No parameter, we use "default" parser and the signature with an input document
        Blob jsonBlob = (JSONBlob) automationService.run(ctx, LDTGetRecordJsonOp.ID);
        assertNotNull(jsonBlob);
        checkReturnedJson(jsonBlob.getString());

        // Test the signature with void as input and required parameters
        ctx = new OperationContext(coreSession);
        ctx.setInput(null);
        Map<String, Object> params = new HashMap<>();
        params.put("sourceLdtDocId", ldtDoc.getId());
        params.put("startOffset", (long) TestUtils.SIMPLELDT_RECORD2_STARTOFFSET);
        params.put("recordSize", (long) TestUtils.SIMPLELDT_RECORD2_RECORDSIZE);
        jsonBlob = (JSONBlob) automationService.run(ctx, LDTGetRecordJsonOp.ID, params);
        assertNotNull(jsonBlob);
        checkReturnedJson(jsonBlob.getString());
        
    }
}
