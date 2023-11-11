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
package nuxeo.ldt.parser.service.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.blob.s3.S3BlobProvider;
import org.nuxeo.ecm.blob.s3.S3BlobStore;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.AbstractBlobStore;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.ByteRange;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.runtime.api.Framework;

/**
 * When reading a record inside an LDT File, we want to be able to get only the bytes we need.
 * This means if the blobs are stored on S3, we must get the bytes by range, so S3 gives us these
 * bytes and we don't have to download all the file to parse it locally.
 * <br>
 * Unfortunately, when using the S3 Binary Manager, blobProvider.getStream(key, range) leads to Nuxeo
 * downloading the whole file, because S3BlobStore#getStream(key) (with a key including the range)
 * hard codes the return to an unknow stream, which leads the caller to download the file, and we
 * don't want that.
 * So we need to explicitly read the blob with range, doing some casting when using S3, etc.
 * 
 * @since 2021
 */
public class LDTParserRecordStream {
    
    private static final Logger log = LogManager.getLogger(LDTParserRecordStream.class);

    protected static int checkS3BlobProviderClass = -1;

    protected static boolean usingS3WithRangeLogged = false;

    protected static boolean hasS3BlobProviderClass() {
        if (checkS3BlobProviderClass == -1) {
            try {
                @SuppressWarnings("unused")
                Class<?> theClass = Class.forName("org.nuxeo.ecm.blob.s3.S3BlobProvider");
                checkS3BlobProviderClass = 1;
            } catch (ClassNotFoundException e) {
                checkS3BlobProviderClass = 0;
            }
        }

        return checkS3BlobProviderClass == 1;
    }

    public static InputStream getStream(Blob blob, ByteRange range) throws IOException {

        InputStream stream = null;

        BlobManager blobManager = Framework.getService(BlobManager.class);
        BlobProvider blobProvider = blobManager.getBlobProvider(blob);

        if (blobProvider == null) { // Ex.: A temp FileBlob for example, not stored in a BinaryStore
            stream = blob.getStream();
            long skipped = stream.skip(range.getStart());
            if (skipped != range.getStart()) {
                throw new NuxeoException("Could not jump to the startOffset of " + range.getStart());
            }
            return stream;
        }

        // We have a blobProvider
        ManagedBlob managedBlob = (ManagedBlob) blob;
        String key = managedBlob.getKey();

        // If it is a S3BlobProvider, we have to workaround (see class doc)
        if (hasS3BlobProviderClass() && blobProvider instanceof S3BlobProvider) {
            S3BlobProvider s3BlobProvider = (S3BlobProvider) blobProvider;
            if (!s3BlobProvider.allowByteRange()) {
                throw new NuxeoException("The s3BlobProvider should allow ByteRange.");
            }

            String keyWithRange = AbstractBlobStore.setByteRangeInKey(key, range);
            S3BlobStore blobStore = (S3BlobStore) s3BlobProvider.store;

            Blob recordFileBlob = Blobs.createBlobWithExtension(".txt");
            Path path = recordFileBlob.getFile().toPath();
            // We also need to remove the provider id prefix, for sure
            // (as is done when blobProvider.getStream(key, range))
            keyWithRange = keyWithRange.replace(s3BlobProvider.blobProviderId + ":", "");
            if (blobStore.readBlob(keyWithRange, path)) {
                if(!usingS3WithRangeLogged) {
                    usingS3WithRangeLogged = true;
                    log.info("Correctly using S3 with a range (this is logged only once.)");
                }
                stream = recordFileBlob.getStream();
            } else {
                // See source code. false is returned only if the key was missing...
                throw new NuxeoException("Error reading the blob <" + key + "> with a ByteRange, readBlob() returned false, check the log.");
            }

        } else if (blobProvider.allowByteRange()) {
            stream = blobProvider.getStream(key, range);
        } else {
            stream = blobProvider.getStream(managedBlob);
            if(stream == null) { // Possible an not an error
                stream = blob.getStream();
            }
            long skipped = stream.skip(range.getStart());
            if (skipped != range.getStart()) {
                throw new NuxeoException("Could not jump to the startOffset of " + range.getStart());
            }
        }

        return stream;
    }

}
