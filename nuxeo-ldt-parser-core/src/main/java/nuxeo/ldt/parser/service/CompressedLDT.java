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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.ByteRange;

import nuxeo.ldt.parser.service.elements.Record;

/**
 * @since TODO
 */
public class CompressedLDT {

    protected LDTParser ldtParser;

    protected Blob sourceLdt;

    protected CloseableFile ldtFile;

    protected RandomAccessFile ldtRandomAccessFile;

    protected Blob destination;

    protected RandomAccessFile destinaTionRandomAccessFile;

    protected long nextStartOffset = 0;

    public CompressedLDT(LDTParser parser, Blob sourceLdtBlob) throws IOException {

        this.ldtParser = parser;
        this.sourceLdt = sourceLdtBlob;

        ldtFile = sourceLdt.getCloseableFile();
        ldtRandomAccessFile = new RandomAccessFile(ldtFile.file, "r");

    }

    public ByteRange add(long startOffsetInLDT, long recordSizeInLDT) throws IOException {

        if (destinaTionRandomAccessFile == null) {
            destination = Blobs.createBlobWithExtension(".cldt");
            destination.setMimeType("application/binary");
            destinaTionRandomAccessFile = new RandomAccessFile(destination.getFile(), "rw");
        }
        // 1. Read the text
        byte[] bytes = new byte[(int) recordSizeInLDT];
        ldtRandomAccessFile.read(bytes);

        // 2. Get the JSON
        /*
        String str = new String(bytes, StandardCharsets.UTF_8);
        String[] linesArray = str.split("\\r?\\n");
        List<String> linesList = new ArrayList<>(Arrays.asList(linesArray));
        Record record = ldtParser.parseRecord(linesList);
        String recordJsonStr = record.toJson();

        // 3. Compress
        byte[] jsonBytes = recordJsonStr.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            // Write the string's bytes to the gzip output stream
            gzipOutputStream.write(jsonBytes);
        }
        byte[] compressedData = byteArrayOutputStream.toByteArray();
        */
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            // Write the string's bytes to the gzip output stream
            gzipOutputStream.write(bytes);
        }
        byte[] compressedData = byteArrayOutputStream.toByteArray();


        // 4. add to file
        destinaTionRandomAccessFile.write(compressedData);

        ByteRange range = ByteRange.inclusive(nextStartOffset, nextStartOffset + compressedData.length - 1);
        nextStartOffset += compressedData.length;

        return range;
    }

    public Blob close() throws IOException {

        ldtRandomAccessFile.close();
        ldtFile.close();

        destinaTionRandomAccessFile.close();
        return destination;
    }

    public static String uncompress(byte[] compressedBytes) {

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
            throw new NuxeoException("Error while expanding the copressed string", e);
        }

        return decompressedString;
    }

}
