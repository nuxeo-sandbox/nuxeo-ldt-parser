package nuxeo.ldt.parser.test.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.blob.s3.S3BlobProvider;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import nuxeo.ldt.parser.service.LDTParser;
import nuxeo.ldt.parser.service.LDTParserService;
import nuxeo.ldt.parser.service.elements.Record;
import nuxeo.ldt.parser.test.SimpleFeatureCustom;
import nuxeo.ldt.parser.test.TestUtils;

/**
 * The following environment variables *must* have been set prior to the unit tests:
 * <ul>
 * <li>AWS standard env. variables:
 * <ul>
 * <li>AWS_ACCESS_KEY_ID</li>
 * <li>AWS_SECRET_ACCESS_KEY</li>
 * <li>AWS_SESSION_TOKEN</li>
 * <li>AWS_REGION</li>
 * </ul>
 * </li>
 * <li>Other expected env. variables:
 * <ul>
 * <li>TEST_BUCKET</li>
 * <li>TEST_BUCKET_PREFIX</li>
 * </ul>
 * </li>
 * </ul>
 * <br>
 * <br>
 * WARNING: The misc test files with .ldt data have content that is specially tested here, like
 * recordSize expected, exact values for clientId/taxId, etc.
 * => DO NOT CHANGE THE FILES (or also change the test code)
 * => See TestUtils for details
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, SimpleFeatureCustom.class})
// @RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3")
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests")
@Deploy("nuxeo.ldt.parser.nuxeo-ldt-parser-core")
@Deploy("nuxeo.ldt.parser.nuxeo-ldt-parser-core:s3-blob-provider.xml")
public class TestLDTParserWithS3BinaryStore {

    // As defined in s3-blob-provider.xml
    public static String TEST_BLOB_PROVIDER_ID = "test";

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected BlobManager blobManager;

    @Inject
    protected LDTParserService ldtParserService;

    @Test
    public void shouldGetRecordFromS3WithByteRange() throws Exception {

        Assume.assumeTrue("At least one AWS BlobStore configuration key is missing. Not doing the test",
                SimpleFeatureCustom.hasAllKeys());

        DocumentModel doc = session.createDocumentModel("/", "test-doc", "File");
        doc.setPropertyValue("dc:title", "test-doc");
        Blob blob = TestUtils.getSimpleTestFileBlob();
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        BlobProvider provider = blobManager.getBlobProvider(TEST_BLOB_PROVIDER_ID);
        // provider.allowByteRange();
        assertTrue(provider instanceof S3BlobProvider);

        transactionalFeature.nextTransaction();

        ManagedBlob resultBlob = (ManagedBlob) doc.getPropertyValue("file:content");
        String digest = resultBlob.getDigest(); // digest is the key

        assertEquals(TEST_BLOB_PROVIDER_ID, resultBlob.getProviderId());
        assertEquals(TEST_BLOB_PROVIDER_ID + ':' + digest, resultBlob.getKey());

        LDTParser parser = ldtParserService.newParser(null);
        Record record = parser.getRecord(resultBlob, TestUtils.SIMPLELDT_RECORD2_STARTOFFSET,
                TestUtils.SIMPLELDT_RECORD2_RECORDSIZE);
        assertNotNull(record);
        TestUtils.checkSimpleTestFileRecord2Values(record);

    }

}
