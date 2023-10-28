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
 *     Michael Vachette
 *     Thibaud Arguillere
 */

package nuxeo.ldt.parser.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nuxeo.ldt.parser.service.elements.Item;
import nuxeo.ldt.parser.service.elements.MainLine;
import nuxeo.ldt.parser.service.elements.Record;
import nuxeo.ldt.parser.service.elements.RecordInfo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.blob.s3.S3BlobProvider;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.ByteRange;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * TODO write the main usage class
 * 
 * @since TODO
 */
public class LDTParser {

    private static final Logger log = LogManager.getLogger(LDTParser.class);

    protected LDTParserDescriptor config;

    protected String name;

    protected String recordStartToken;

    protected String recordEndToken;

    protected String patternLine1Str;

    protected Pattern patternLine1;

    protected String altPatternLine1Str;

    protected Pattern altPatternLine1 = null;

    protected List<String> fieldsLine1;

    protected String patternLine2Str;

    protected Pattern patternLine2;

    protected String altPatternLine2Str;

    protected Pattern altPatternLine2 = null;

    protected List<String> fieldsLine2;

    protected Callbacks callbacks = null;

    protected int lengthOfEOF;

    protected long totalBytesRead = 0;

    protected long lineCount = 0;

    public LDTParser(LDTParserDescriptor config) {
        super();

        this.config = config;
        this.name = config.getName();
        this.recordStartToken = config.getRecordStartToken();
        this.recordEndToken = config.getRecordEndToken();
        this.patternLine1 = Pattern.compile(config.getPatternLine1());
        if (StringUtils.isNotBlank(config.getAltPatternLine1())) {
            this.altPatternLine1 = Pattern.compile(config.getAltPatternLine1());
        }
        this.fieldsLine1 = config.getFieldsLine1();
        this.patternLine2 = Pattern.compile(config.getPatternLine2());
        if (StringUtils.isNotBlank(config.getAltPatternLine2())) {
            this.altPatternLine2 = Pattern.compile(config.getAltPatternLine2());
        }
        this.fieldsLine2 = config.getFieldsLine2();

        if (config.useCallbackForItems()) {
            loadCallbacksClass();
        }
    }

    protected void loadCallbacksClass() {
        if (config.getCallbacksClass() != null) {
            try {
                callbacks = (Callbacks) config.getCallbacksClass().getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                // TODO Auto-generated catch block
                throw new NuxeoException("Cannot load LDTParser", e);
            }
        } else {
            callbacks = null;
        }
    }

    public LDTParserDescriptor getDescriptor() {
        return config;
    }

    public boolean isRecordStart(String line) {
        return line.startsWith(recordStartToken);
    }

    public boolean isRecordEnd(String line) {
        return line.indexOf(recordEndToken) > -1;
    }

    public MainLine parseRecordFirstLine(String line) {
        Matcher m = patternLine1.matcher(line);
        if (m.matches()) {
            return new MainLine(m, fieldsLine1);
        }

        log.info("patternLine1 not found, searching for altPatternLine1");
        if (altPatternLine1 != null) {
            m = altPatternLine1.matcher(line);
            if (m.matches()) {
                return new MainLine(m, fieldsLine1);
            }
        }

        throw new NuxeoException("malformed record first line " + line);
    }

    public MainLine parseRecordSecondLine(String line) {

        Matcher m = patternLine2.matcher(line);
        if (m.matches()) {
            return new MainLine(m, fieldsLine2);
        }

        log.info("patternLine2 not found, searching for altPatternLine2");
        if (altPatternLine2 != null) {
            m = altPatternLine2.matcher(line);
            if (m.matches()) {
                return new MainLine(m, fieldsLine2);
            }
        }

        // If we are here, it means we did not find a valid pattern. We give up
        if (config.ignoreMalformedLines) {
            log.warn("IGNORED Malformed/Unexpected group(s) in second line of record <" + line + ">");
            return null;
        }

        throw new NuxeoException("malformed record second line " + line);
    }

    public Item parseItem(String line) {
        
        //System.out.println("PARSE ITEM: <" + line + ">");

        if (config.useCallbackForItems()) {
            if (callbacks == null) { // Can happen when LDTParserDescriptor#setUseCallbackForItems is changed
                loadCallbacksClass();
            }
            return callbacks.parseItem(line);
        }

        if (config.getDetailsLineMinSize() > 0 && line.length() < config.getDetailsLineMinSize()) {
            System.out.println("LESS THAN MINIMAL LENGTH: " + line);
            return null;
        }

        return new Item(line, config);
    }

    public Record parseRecord(List<String> lines) {
        MainLine firstLine = parseRecordFirstLine(lines.get(0));
        MainLine secondLine = parseRecordSecondLine(lines.get(1));
        if (secondLine == null) { // This was a malformed second line
            return null;
        }
        List<Item> listItems = lines.subList(2, lines.size())
                                    .stream()
                                    .map(this::parseItem)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());
        return new Record(this, firstLine, secondLine, listItems);
    }

    public Record getRecord(Blob blob, long startOffset, long recordSize) {

        // recordSize < 100 is arbitrary, but the 2 first lines are > 100 anyway
        if (startOffset < 0 || recordSize < 100) {
            throw new NuxeoException(
                    "getRecord: Invalid startOffest (" + startOffset + ") or recordSize (" + recordSize + ")");
        }

        // ==================================================
        // Get the String of the record
        // ==================================================
        String recordStr = null;
        // Fill recordStr from the the blob.
        // Check the blob, If on S3, get the bytes by range...
        BlobManager blobManager = Framework.getService(BlobManager.class);
        BlobProvider blobProvider = blobManager.getBlobProvider(blob);
        if (blobProvider instanceof S3BlobProvider) {
            S3BlobProvider s3BlobProvider = (S3BlobProvider) blobProvider;

            if (!s3BlobProvider.allowByteRange()) {
                throw new NuxeoException("The s3BlobProvider should allow ByteRange.");
            }
            String key = ((ManagedBlob) blob).getKey();
            ByteRange range = ByteRange.inclusive(startOffset, startOffset + recordSize);

            try (InputStream stream = s3BlobProvider.getStream(key, range)) {
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = stream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                recordStr = result.toString(StandardCharsets.UTF_8.name());

            } catch (IOException e) {
                throw new NuxeoException("Error reading the blob with a ByteRange.", e);
            }
        }
        // ...else (not in S3 storage):
        if (recordStr == null) {
            try (InputStream is = blob.getStream()) {
                long skipped = is.skip(startOffset);
                if (skipped != startOffset) {
                    throw new NuxeoException("Could not jump to the startOffset of " + startOffset);
                }

                byte[] recordBytes = is.readNBytes((int) recordSize);
                recordStr = new String(recordBytes, StandardCharsets.UTF_8);

            } catch (IOException e) {
                throw new NuxeoException(e);
            }
        }
        
        // Not found. And no exception => most likely invalid range/size.
        if(StringUtils.isEmpty(recordStr)) {
            return null;
        }

        // ==================================================
        // Get the record itself
        // ==================================================
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(recordStr.getBytes(StandardCharsets.UTF_8));
                InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                LineIterator it = new LineIterator(reader)) {

            String line = it.nextLine();
            if (!isRecordStart(line)) {
                boolean found = false;
                while (it.hasNext()) {
                    line = it.nextLine();
                    if (isRecordStart(line)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new NuxeoException(
                            "Error parsing the lines. startOffset=" + startOffset + ", recordSize=" + recordSize
                                    + " => Record start token ('" + config.getRecordStartToken() + "') not found.");
                }
            }

            List<String> rawRecord = new ArrayList<>();

            MainLine firstLine = parseRecordFirstLine(line);
            String second = it.nextLine();
            MainLine secondLine = parseRecordSecondLine(second);
            if (secondLine == null) { // This was a malformed second line
                return null;
            }
            // if (firstLine.clientId.equals(clientId) && firstLine.taxId.equals(taxId) &&
            // secondLine.month.equals(month)
            // && secondLine.year.equals(year)) {

            rawRecord.add(line);
            rawRecord.add(second);
            boolean reachedEnd = false;
            while (!reachedEnd && it.hasNext()) {
                String item = it.nextLine();
                if (isRecordStart(item)) {
                    reachedEnd = true;
                } else {
                    rawRecord.add(item);
                }
            }

            // } else {
            // throw new NuxeoException("Error parsing the lines: Record found at offset " + startOffset
            // + " does not match the info (cllientId, taxId, month, year)");
            // }

            return rawRecord.isEmpty() ? null : parseRecord(rawRecord);

        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    protected int getEOLLength(CloseableFile closFile) throws IOException {

        try (BufferedReader reader = new BufferedReader(new FileReader(closFile.getFile()))) {

            int c;
            while ((c = reader.read()) != -1) {
                if (c == '\r') {
                    c = reader.read();
                    if (c == '\n') {
                        return 2;// CRLF;
                    }
                    return 1; // CR; // This would be unusual but is technically possible
                } else if (c == '\n') {
                    return 1; // LF;
                }
            }

        } catch (IOException e) {
            throw new NuxeoException(e);
        }

        throw new NuxeoException("Cannot determine the type of End of Line (LF, CRLF)");
    }

    /*
     * The it parameter MUST be positionned at the beginning of a record
     */
    protected RecordInfo getOneRecordInfo(LineIterator it) throws IOException {

        if (!it.hasNext()) {
            return null;
        }

        long recordStart = totalBytesRead;
        long lineStart = lineCount;

        String line = it.nextLine();
        if (!isRecordStart(line)) {
            throw new NuxeoException("Line should be a Record-Start, and starts with '" + recordStartToken + "'");
        }
        lineCount += 1;

        // If using non ASCII char:
        // byte[] utf8Bytes = line.getBytes("UTF-8");
        // totalBytesRead += utf8Bytes.length + lengthOfEOF;
        totalBytesRead += line.length() + lengthOfEOF;

        MainLine firstLine = parseRecordFirstLine(line);
        // We assume there is a second line if the first is a recordStart.
        String second = it.nextLine();
        lineCount += 1;
        // utf8Bytes = second.getBytes("UTF-8");
        // totalBytesRead += utf8Bytes.length + lengthOfEOF;
        totalBytesRead += second.length() + lengthOfEOF;
        MainLine secondLine = parseRecordSecondLine(second);

        // If second line was malformed, we still must continue to parse
        // until the end of the statement, to cumulate line numbers and bytes read
        // (then return null, so caller knows something went wrong)
        if (secondLine == null) {
            // (nothing)
        }

        while (it.hasNext()) {
            String moreLine = it.nextLine();
            lineCount += 1;
            // utf8Bytes = moreLine.getBytes("UTF-8");
            // totalBytesRead += utf8Bytes.length + lengthOfEOF;
            totalBytesRead += moreLine.length() + lengthOfEOF;
            if (isRecordEnd(moreLine)) {
                break;
            }
        }
        if (secondLine == null) {
            return null;
        }

        long recordSize = totalBytesRead - recordStart;
        RecordInfo recInfo = new RecordInfo(recordStart, recordSize, lineStart, firstLine, secondLine);

        return recInfo;

    }

    /**
     * Create as many LDTRecords (or config.recordDocType) as found in the blob of the input inputLdtDoc.
     * Update inputLdtDoc with LDT info
     * Records are created at the same level than the LDT itself, in folder whose title is the title of the LDT +
     * config.recordsContainerSuffix
     * Each record:
     * - Is created as a recordDocType document tyoe (LDTRecord by default)
     * - Is filled with the retrieval info (start offset, size, line start) in the ldtrecord schema
     * - And possibly with any field defined in the recordFieldsMapping mapping.
     * - The dc-title is set to recordTitleFields
     * Caller is in charge of making sure permissions allow for creating content.
     * <br>
     * <b>WARNING</b>: Assume the file is UTF-8, even pure ASCII (no multibytes char)
     * 
     * @param inputLdtDoc, the input document whose file:content contains the LDT to parse
     * @return
     * @since TODO
     */
    public LDTInfo parseAndCreateStatements(DocumentModel inputLdtDoc) {

        LDTInfo ldtInfo = null;

        log.info("LDTParser#parseAndCreateStatements: Parsing <" + inputLdtDoc.getTitle() + "> (" + inputLdtDoc.getId()
                + ")");

        // Sanitycheck
        if (!inputLdtDoc.hasSchema(Constants.SCHEMA_LDT)) {
            throw new NuxeoException(
                    "LDTParser#parseAndCreateStatements: Cannot parse the input document, it does not have the ldt schema");
        }

        Blob blob = (Blob) inputLdtDoc.getPropertyValue("file:content");
        if (blob == null) {
            log.warn("LDTParser#parseAndCreateStatements: Input document has no blob.");
            return new LDTInfo(0);
        }

        // Parse
        int countRecords = 0;
        CoreSession session = inputLdtDoc.getCoreSession();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // Create a folder for the statements
        DocumentModel docParent = session.getParentDocument(inputLdtDoc.getRef());
        DocumentModel parent = session.createDocumentModel(docParent.getPathAsString(),
                inputLdtDoc.getName() + config.getRecordsContainerSuffix(), config.getRecordsContainerDocType());
        parent.setPropertyValue("dc:title", inputLdtDoc.getTitle() + config.getRecordsContainerSuffix());
        parent = session.createDocument(parent);
        String parentPath = parent.getPathAsString();

        totalBytesRead = 0;
        lineCount = 1;

        boolean hasCustomTitle = config.getRecordTitleFields() != null && config.getRecordTitleFields().size() > 0;

        try (CloseableFile closFile = blob.getCloseableFile();
                LineIterator it = FileUtils.lineIterator(closFile.getFile(), "UTF-8")) {

            long fileSize = closFile.getFile().length();
            NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

            lengthOfEOF = getEOLLength(closFile);
            // log.warn("==============================");
            // log.warn("lengthOfEOF: " + lengthOfEOF);
            // log.warn("==============================");

            while (it.hasNext()) {

                // Get the whole record
                RecordInfo record = getOneRecordInfo(it);

                // If parsing whent wrong, the log has the info
                if (record == null) {
                    continue;
                }

                // log.warn(record.toString());
                // log.warn("totalBytesRead: " + totalBytesRead);

                // Create the document
                String title;
                if (hasCustomTitle) {
                    title = record.getValue(config.getRecordTitleFields().get(0));
                    for (int i = 1; i < config.getRecordTitleFields().size(); i++) {
                        title += "-" + record.getValue(config.getRecordTitleFields().get(i));
                    }
                } else {
                    title = inputLdtDoc.getTitle() + "-" + countRecords + 1;
                }

                DocumentModel recordDoc = session.createDocumentModel(parentPath, title, config.getRecordDocType());
                recordDoc.setPropertyValue("dc:title", title);
                recordDoc.setPropertyValue(Constants.XPATH_LDTRECORD_STARTOFFSET, record.startOffset);
                recordDoc.setPropertyValue(Constants.XPATH_LDTRECORD_RECORDSIZE, record.size);
                recordDoc.setPropertyValue(Constants.XPATH_LDTRECORD_STARTLINE, record.startLine);
                if (config.getRecordFieldsMapping() != null) {
                    for (Map.Entry<String, String> xpathField : config.getRecordFieldsMapping().entrySet()) {
                        String fieldInMap = xpathField.getValue();
                        String fieldValue = record.getValue(fieldInMap);
                        recordDoc.setPropertyValue(xpathField.getKey(), fieldValue);
                    }
                }
                recordDoc = session.createDocument(recordDoc);

                countRecords += 1;
                if ((countRecords % 100) == 0) {
                    session.save();
                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();
                }
                if ((countRecords % 1000) == 0) {
                    String msg = "LDTParser#parseAndCreateStatements, created/commited: "
                            + numberFormat.format(countRecords);
                    msg += "\nlineCount: " + numberFormat.format(lineCount);
                    msg += "\nBytes Read: " + numberFormat.format(totalBytesRead) + "/" + numberFormat.format(fileSize);
                    log.info(msg);
                }
            }

            ldtInfo = new LDTInfo(countRecords);
            inputLdtDoc.setPropertyValue(Constants.XPATH_LDT_COUNTRECORDS, countRecords);
            inputLdtDoc = session.saveDocument(inputLdtDoc);

            session.save();
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

        } catch (IOException e) {
            throw new NuxeoException(e);
        }

        log.info("parseAndCreateStatementsWithRanges: Parsing done. " + ldtInfo.countRecords + " records created.");
        return ldtInfo;
    }

    /**
     * This is mainly a hepler for debug
     * 
     * @param recordDoc
     * @param parser can benull (will use "default")
     * @return
     * @since TODO
     */
    public static String documentToJsonString(DocumentModel recordDoc, String parser) {
        /*
         * Map<String, String> fields = new HashMap<String, String>();
         * fields.put(Constants.XPATH_LDTRECORD_STARTLINE,
         * "" + recordDoc.getPropertyValue(Constants.XPATH_LDTRECORD_STARTLINE));
         * fields.put(Constants.XPATH_LDTRECORD_STARTOFFSET,
         * "" + recordDoc.getPropertyValue(Constants.XPATH_LDTRECORD_STARTOFFSET));
         * fields.put(Constants.XPATH_LDTRECORD_RECORDSIZE,
         * "" + recordDoc.getPropertyValue(Constants.XPATH_LDTRECORD_RECORDSIZE));
         * LDTParserDescriptor desc = Framework.getService(LDTParserService.class).getDescriptor(parser);
         * List<String> xpaths = desc.getLDTRecordXPaths();
         * for (String xpath : xpaths) {
         * fields.put(xpath, "" + recordDoc.getPropertyValue(xpath));
         * }
         * ObjectMapper objectMapper = new ObjectMapper();
         * JsonNode json = objectMapper.valueToTree(fields);
         * return json.toString();
         */
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonObject = objectMapper.createObjectNode();
        ObjectNode objNode = (ObjectNode) jsonObject;
        objNode.put(Constants.XPATH_LDTRECORD_STARTLINE,
                (long) recordDoc.getPropertyValue(Constants.XPATH_LDTRECORD_STARTLINE));
        objNode.put(Constants.XPATH_LDTRECORD_STARTOFFSET,
                (long) recordDoc.getPropertyValue(Constants.XPATH_LDTRECORD_STARTOFFSET));
        objNode.put(Constants.XPATH_LDTRECORD_RECORDSIZE,
                (long) recordDoc.getPropertyValue(Constants.XPATH_LDTRECORD_RECORDSIZE));

        LDTParserDescriptor desc = Framework.getService(LDTParserService.class).getDescriptor(parser);
        List<String> xpaths = desc.getLDTRecordXPaths();
        for (String xpath : xpaths) {
            objNode.put(xpath, "" + recordDoc.getPropertyValue(xpath));

        }

        // JsonNode json = objectMapper.valueToTree(fields);

        return jsonObject.toString();
    }

    public static class LDTInfo {

        public int countRecords = 0;

        public LDTInfo(int countRecords) {
            this.countRecords = countRecords;
        }
    }

}
