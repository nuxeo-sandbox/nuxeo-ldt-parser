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
import org.nuxeo.ecm.blob.s3.S3BlobProviderFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import nuxeo.ldt.parser.service.LDTParser;
import nuxeo.ldt.parser.service.LDTParser.Item;
import nuxeo.ldt.parser.service.LDTParser.Record;
import nuxeo.ldt.parser.test.SimpleFeatureCustom;

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
 * <br><br>
 * WARNING: The misc test file with .ldt data have content that is specially tested here, like
 * byt offessts expected to be an exact value.
 * => DO NOT CHANGE THE FILEs (or also change the test code, byte offsets etc.)
 * 
 * Expected values for test.LDT:
 * {"startOffset": 0,"size": 3968,"clientId": "099900001X100000","taxId": "00005622612810","month": "FEVEREIRO","year": "2021"}
 * {"startOffset": 3968,"size": 10169,"clientId": "099900008X100000","taxId": "00007915107860","month": "FEVEREIRO","year": "2021"}
 * {"startOffset": 14137,"size": 17819,"clientId": "099900033X100000","taxId": "61274726000107","month": "FEVEREIRO","year": "2021"}
 *
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, SimpleFeatureCustom.class /*, S3BlobProviderFeature.class*/ })
//@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3")
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests")
@Deploy("nuxeo.ldt.parser.nuxeo-ldt-parser-core")
@Deploy("nuxeo.ldt.parser.nuxeo-ldt-parser-core:s3-blob-provider.xml")
public class testLDTParserWithS3BinaryStore {
    
    // As defined in s3-blob-provider.xml
    public static String TEST_BLOB_PROVIDER_ID = "test";

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature transactionalFeature;
    
    @Inject
    protected BlobManager blobManager;
    
    // @TODO - WiP, this is the original test, to be adapted to a more generic parser
    @Test
    public void shouldStreamWithBytRange() throws Exception {
        
        Assume.assumeTrue("At least one AWS BlobStore configuration key is missing. Not doing the test", SimpleFeatureCustom.hasAllKeys());
        
        DocumentModel doc = session.createDocumentModel("/", "test-doc", "File");
        doc.setPropertyValue("dc:title", "test-doc");
        File testFile = FileUtils.getResourceFileFromContext("test.LDT");
        FileBlob blob = new FileBlob(testFile);
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);
        
        BlobProvider provider = blobManager.getBlobProvider(TEST_BLOB_PROVIDER_ID);
        //provider.allowByteRange();
        assertTrue(provider instanceof S3BlobProvider);
        
        transactionalFeature.nextTransaction();
        
        // Check it was uploaded abd all is OK
        ManagedBlob resultBlob = (ManagedBlob) doc.getPropertyValue("file:content");
        String digest = resultBlob.getDigest(); // digest is the key
        
        assertEquals(TEST_BLOB_PROVIDER_ID, resultBlob.getProviderId());
        assertEquals(TEST_BLOB_PROVIDER_ID + ':' + digest, resultBlob.getKey());
        
        // Now get a value by range
        /*
        LDTParser parser = new LDTParser();
        Record rec = parser.getRecord(resultBlob, 3968, 10169, "099900008X100000", "00007915107860", "FEVEREIRO", "2021");
        assertNotNull(rec);
        assertEquals("099900008X100000", rec.clientId);
        assertEquals("00007915107860", rec.taxId);
        assertEquals("FEVEREIRO/2021", rec.monthYear);
        */
        // Just checking a couple entries
        /*
        String msg = "";
        for(Item item : rec.items) {
            msg += item.toString() + "\n";
        }
        System.out.println("==========================\n" + msg + "\n==========================");
        */
        /*
        assertNotNull(rec.items);
        assertTrue(rec.items.size() > 0);
        Item item = rec.items.get(0);
        assertEquals("SALDO INICIAL, 01/02, , 12.999,51", item.toString());
        item = rec.items.get(6);
        assertEquals("XRT TROCTE XATY   JAN/21, 02/02, 79,50-, 64.999,65", item.toString());
        */
        
    }

}
