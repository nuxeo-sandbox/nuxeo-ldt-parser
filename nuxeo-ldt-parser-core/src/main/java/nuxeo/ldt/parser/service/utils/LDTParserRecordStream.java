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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.blob.s3.S3BlobProvider;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.ByteRange;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.runtime.api.Framework;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.transfer.TransferManager;

/**
 * When reading a record inside an LDT File, we want to be able to get only the bytes we need.
 * This means if the blobs are stored on S3, we must get the bytes by range, so S3 gives us these
 * bytes and we don't have to download all the file to parse it locally.
 * <br>
 * Nuxeo does all this for us, also handling local cache: To get a stream on this byte range, it
 * first checks if it's already in the local cache (the key contains the byte range, making it unique).
 * If not, it downloads only the bytes from S3 and save them to a temp and cached file. If we request
 * the same, it will be there.
 * (notice in production, it is very unklikely we request the same record before the end of life of
 * the file in the cache, except in tests (but a cient, for example, will rarely download 2 times the
 * same bank statement one after the other. May happen though mainly if the fonrt end has some bugs ;-))
 * <br>
 * So, we let Nuxeo handles all this. Notice that it means we don't know for sure if it gets the byte
 * range from S3 but it's ok, getting the 2k bytes form a local stored file is perfectly fine.
 * <br>
 * If you want to make sure the bytes are fecthed from S3 with ByteRange, then activate debug level
 * for {@code org.nuxeo.ecm.blob.s3.S3BlobStore}, its {@code readBlob} logs good info.
 * <br>
 * This class also handles other cases, mainly found in unit test (like a FileBlob, not part of a BlobStore)
 * <br>
 * Also, <b>IMPORTANT</b> (see the README): When using S3 for blob storage, do not forget to allowByteRange. If you
 * don't, anyway, you will get an error.
 * This class also handles a warning when using S3BlobProvider with no byte range (it will still get the stream, but
 * downloads the whole file, which is not optimized)
 * <br>
 * Last IMPORTANT: as usual, do not forget to close the received inputStream.
 * 
 * @since 2021
 */
public class LDTParserRecordStream {

    private static final Logger log = LogManager.getLogger(LDTParserRecordStream.class);

    protected static boolean usingS3WithNoRangeLogged = false;

    protected static int checkS3BlobProviderClass = -1;

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

    /**
     * @param blob
     * @param range
     * @return
     * @throws IOException
     * @since TODO
     */
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

        if (blobProvider.allowByteRange()) {
            // This is where Nuxeo does all the job if we have a S3BlobProvider wth ByteRange allowed.
            return blobProvider.getStream(key, range);
        } else if (!usingS3WithNoRangeLogged && hasS3BlobProviderClass() && blobProvider instanceof S3BlobProvider) {
            usingS3WithNoRangeLogged = true;
            String msg = "\nWARNING ==========================================\n";
            msg += "Using S3 Blob Provider without allowing byteRange. The blob is downloaded from S3, this is not efficient.";
            msg += "\n==================================================\n";
            log.warn(msg);
        }

        // BlobProvider does not allow ByteRange
        stream = blobProvider.getStream(managedBlob);
        if (stream == null) { // Not an error with a ManagedBlob
            stream = blob.getStream();
        }
        long skipped = stream.skip(range.getStart());
        if (skipped != range.getStart()) {
            throw new NuxeoException("Could not jump to the startOffset of " + range.getStart());
        }

        return stream;
    }

    /**
     * Return directly a S3ObjectInputStream to the bytes. MUST BE CLOSED by caller "ASAP" (dixit Amazon doc)
     * Once received, just byte[] bytes = stream.readAllBytes(); (if you know you don't
     * have MBs, GBs of them)
     * <br>
     * WARNING
     * Assume S3 storage is set and all. We don't check that, you will get error if it is not
     * Also assume, of course, all access is already set (secret key etc.) in the configuration
     * 
     * @param blob
     * @param range
     * @return an input stream
     * @throws IOException
     * @since 2021
     */
    public static InputStream getStreamWithByteRangeOnS3(Blob blob, ByteRange range) throws IOException {

        ManagedBlob managedBlob = (ManagedBlob) blob;
        String key = managedBlob.getKey();
        
        // We need to remove the provider prefix
        if(key.startsWith(managedBlob.getProviderId() + ":")) {
            key = key.replace(managedBlob.getProviderId() + ":", "");
        }

        String objectKey = GetS3Config.getBucketPrefix() + key;        
        
        // We do not handle versions.
        GetObjectRequest getObjectRequest = new GetObjectRequest(GetS3Config.getBucket(), objectKey);
        getObjectRequest.setRange(range.getStart(), range.getEnd());

        AmazonS3 s3 = GetS3Config.getTransferManager(blob).getAmazonS3Client();

        S3ObjectInputStream stream = s3.getObject(getObjectRequest).getObjectContent();
        return stream;

        /*
         * Blob tmpBlob = Blobs.createBlobWithExtension(".txt");
         * Download download = GetS3Config.getTransferManager().download(getObjectRequest, tmpBlob.getFile());
         * try {
         * download.waitForCompletion();
         * return tmpBlob.getStream();
         * } catch (AmazonClientException | InterruptedException e) {
         * throw new NuxeoException("Error downloading ByteRange(" + range.getStart() + ", " + range.getEnd()
         * + ") for blob " + key + ", using " + objectKey, e);
         * }
         */
    }

    /**
     * Utility class to fetch misc.info only once.
     * Not a killer optimization though, more a habit :-)
     * <br>
     * This utility, still, assumes the S3 Binary Storage is correctly configured.
     * 
     * @since 2021
     */
    protected static class GetS3Config {

        protected static String bucket = null;

        protected static String bucketPrefix = null;

        protected static TransferManager transferManager = null;

        public static String getBucket() {
            if (bucket == null) {
                bucket = Framework.getProperty("nuxeo.s3storage.bucket");
                if(StringUtils.isBlank(bucket)) {
                    bucket = Framework.getProperty("nuxeo.test.s3storage.provider.test.bucket");
                }
            }
            return bucket;
        }

        public static String getBucketPrefix() {

            if (bucketPrefix == null) {
                bucketPrefix = Framework.getProperty("nuxeo.s3storage.bucket_prefix");
                if (StringUtils.isBlank(bucketPrefix)) {
                    bucketPrefix = Framework.getProperty("nuxeo.test.s3storage.provider.test.bucket_prefix");
                }
                if (StringUtils.isBlank(bucketPrefix)) {
                    bucketPrefix = "";
                }
            }

            return bucketPrefix;
        }

        public static TransferManager getTransferManager(Blob blob) {

            if (transferManager == null) {
                ManagedBlob managedBlob = (ManagedBlob) blob;

                BlobManager blobManager = Framework.getService(BlobManager.class);
                S3BlobProvider s3BlobProvider = (S3BlobProvider) blobManager.getBlobProvider(managedBlob.getProviderId());
                transferManager = s3BlobProvider.getTransferManager();
            }

            return transferManager;
        }
    }

}
