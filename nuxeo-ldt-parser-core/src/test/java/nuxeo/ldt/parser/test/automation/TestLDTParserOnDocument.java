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
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import nuxeo.ldt.parser.automation.LDTParseAndCreateDocumentsOp;
import nuxeo.ldt.parser.service.descriptors.LDTParserDescriptor;
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
public class TestLDTParserOnDocument {

    protected final List<String> events = Arrays.asList("documentCreated");

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected LDTParserService ldtParserService;

    @Test
    public void shouldCreateStatementsWithDefaultParser() throws Exception {

        // Create the LDT doc
        final String LDT_DOC_NAME = "Test";

        DocumentModel doc = coreSession.createDocumentModel("/", LDT_DOC_NAME,
                nuxeo.ldt.parser.service.Constants.DOC_TYPE_LDT);

        Blob blob = TestUtils.getSimpleTestFileBlob();
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = coreSession.createDocument(doc);
        coreSession.save();
        // We may set some listeners for LDT creation in the future, let's wait.
        transactionalFeature.nextTransaction();

        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(doc);
        DocumentModel docResult = (DocumentModel) automationService.run(ctx, LDTParseAndCreateDocumentsOp.ID);
        assertNotNull(docResult);
        // We may set some listeners for LDTRecord creation in the future, let's wait.
        transactionalFeature.nextTransaction();

        // Check LDT doc was updated
        docResult = coreSession.getDocument(doc.getRef());
        // Check statements
        long countStatements = (long) docResult.getPropertyValue(
                nuxeo.ldt.parser.service.Constants.XPATH_LDT_COUNTRECORDS);
        assertEquals(TestUtils.SIMPLELDT_STATEMENT_COUNT, countStatements);

        // ======================================================================
        // We are using the "default" ldtParser contribution.
        // See ldtparser-service.xml for expected values
        // ======================================================================

        // If we are here, we know things were created at the correct place
        LDTParser parser = ldtParserService.newParser(null);
        LDTParserDescriptor desc = parser.getDescriptor();
        String pathStr = "/" + LDT_DOC_NAME + desc.getRecordsContainerSuffix();
        PathRef recordsFolderRef = new PathRef(pathStr);
        assertTrue(coreSession.exists(recordsFolderRef));
        
        /*
        String testNxql = "SELECT * FROM " + desc.getRecordDocType();
        DocumentModelList testDocs = coreSession.query(testNxql);
        String msg = "===========================================================================\n";
        for(DocumentModel testDoc : testDocs) {
            msg += LDTParser.documentToJsonString(testDoc, null) + "\n";
        }
        msg += "===========================================================================\n";
        System.out.println(msg);
        */

        // Just check one, as expected in the test ldt file.
        // (we test the second entry in the test.LDT file)
        // We used the fields defined in the "default" contribution. taxId is in dc:format,
        // client id in dc:description, etc.
        String nxql = "SELECT * FROM " + desc.getRecordDocType() + " WHERE dc:format = '12345678901567'";
        nxql += " AND dc:description = '9874567890ABC12'";
        nxql += " AND dc:source = 'MARCH'";
        nxql += " AND dc:rights = '2023'";
        DocumentModelList docs = coreSession.query(nxql);
        assertEquals(1, docs.size());

    }
}
