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
package nuxeo.ldt.parser.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.blob.ByteRange;

/**
 * Allows for compressing a .LDT file. The principle is to parse a LDT file and generate the compressed one on the fly.
 * The format is simple: Each record is compressed (GZIP) and added to the compressed file, one after the other.
 * Caller is in charge of keepin track of the startOffset and recordSize inside the compressed ldt.
 * The file extension is .cldt, the mimetype is application/cldt.
 * <br />
 * Usage:
 * <ol>
 * <li>Create a new {@code CompressedLDT}, with a {@code LDTParser} and the blob holding the source LDT</li>
 * <li>Iterate/parse the LDT and for each record found, call {@code add(startOffset, recordSize} (both parameters are
 * related to the source LDT)</li>
 * <li><b>Important</b>: Once done, do not forget to {@code close()} the CompressedLDT object, so he misc. internal
 * streams and files are correctly closed</li>
 * </ol>
 * See {@code LDTParser#parseAndCreateDocuments(DocumentModel, boolean)} for an example of use.
 * <br />
 * All methods can throw an {@code IOException}
 * <br />
 * To extract a record from the compressed LDT, use the static {@code CompressedLDT#uncompress} method.
 * 
 * @since 2021
 */
public class CompressedLDT {

    public static final String COMPRESSED_LDT_MIMETYPE = "application/cldt";

    public static final String COMPRESSED_LDT_FILEXTENSTION = "cldt";

    protected Blob ldtBlob;

    protected CloseableFile ldtFile;

    protected RandomAccessFile ldtRandomAccessFile;

    protected Blob destinationBlob;

    protected RandomAccessFile destinaTionRandomAccessFile;

    protected long nextStartOffset = 0;

    /**
     * Prepare the object to use the LDT for future calls
     * 
     * @param sourceLdtBlob
     * @throws IOException
     */
    public CompressedLDT(Blob sourceLdtBlob) throws IOException {

        this.ldtBlob = sourceLdtBlob;

        ldtFile = ldtBlob.getCloseableFile();
        ldtRandomAccessFile = new RandomAccessFile(ldtFile.file, "r");

    }

    /**
     * Read the record inside the source LDT, get all its bytes, compresses them and adds the result to the .cldt.
     * Return the byte range inside the compressed ldt.
     * 
     * @param startOffsetInLDT
     * @param recordSizeInLDT
     * @return the byte range inside the compressed ldt.
     * @throws IOException
     */
    public ByteRange add(long startOffsetInLDT, long recordSizeInLDT) throws IOException {

        if (destinaTionRandomAccessFile == null) {
            destinationBlob = Blobs.createBlobWithExtension("." + COMPRESSED_LDT_FILEXTENSTION);
            destinaTionRandomAccessFile = new RandomAccessFile(destinationBlob.getFile(), "rw");
        }
        // 1. Read the text
        byte[] bytes = new byte[(int) recordSizeInLDT];
        ldtRandomAccessFile.read(bytes);

        // 2. Compress
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            // Write the string's bytes to the gzip output stream
            gzipOutputStream.write(bytes);
        }
        byte[] compressedData = byteArrayOutputStream.toByteArray();

        // 3. add to file
        destinaTionRandomAccessFile.write(compressedData);

        ByteRange range = ByteRange.inclusive(nextStartOffset, nextStartOffset + compressedData.length - 1);
        nextStartOffset += compressedData.length;

        return range;
    }

    /**
     * To be called once parsing the source ldt is done.
     * Closes all the internbal objects (streams/files) and returns the blob of the compressed ldt.
     * 
     * @return the blob of the compressed ldt
     * @throws IOException

     */
    public Blob close() throws IOException {

        ldtRandomAccessFile.close();
        ldtFile.close();

        destinaTionRandomAccessFile.close();

        String name = ldtBlob.getFilename();
        if (StringUtils.isNotBlank(name)) {
            name = FilenameUtils.getBaseName(name) + "." + COMPRESSED_LDT_FILEXTENSTION;
            destinationBlob.setFilename(name);
        }
        destinationBlob.setMimeType(COMPRESSED_LDT_MIMETYPE);

        return destinationBlob;
    }

    /**
     * get all the compressed bytes of a record, uncompress them, return the corresponding text.
     * 
     * @param compressedBytes
     * @return the whole text of the record
     */
    public static String uncompress(byte[] compressedBytes) throws IOException {

        String decompressedString = null;

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedBytes);
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
                InputStreamReader inputStreamReader = new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8)) {

            StringBuilder stringBuilder = new StringBuilder();
            char[] buffer = new char[4096];
            int bytesRead;
            while ((bytesRead = inputStreamReader.read(buffer)) != -1) {
                stringBuilder.append(buffer, 0, bytesRead);
            }

            // Decompressed string
            decompressedString = stringBuilder.toString();

        } catch (IOException e) {
            throw new IOException("Error while expanding the compressed string", e);
        }

        return decompressedString;
    }

}
