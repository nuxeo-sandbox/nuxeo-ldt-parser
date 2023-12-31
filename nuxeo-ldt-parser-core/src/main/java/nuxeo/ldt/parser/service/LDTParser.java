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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nuxeo.ldt.parser.service.descriptors.LDTHeaderDescriptor;
import nuxeo.ldt.parser.service.descriptors.LDTParserDescriptor;
import nuxeo.ldt.parser.service.elements.HeaderLine;
import nuxeo.ldt.parser.service.elements.Item;
import nuxeo.ldt.parser.service.elements.Record;
import nuxeo.ldt.parser.service.elements.RecordInfo;
import nuxeo.ldt.parser.service.utils.LDTParserRecordStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.JSONBlob;
import org.nuxeo.ecm.core.blob.ByteRange;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
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
import java.util.stream.Collectors;

/**
 * This class is in charge of parsing an LDT file and, depending on the caller's need, return a Record, a header of a
 * record, an item of a record, the JSON of a record, etc.
 * <br>
 * It also can create one document/record {@code LDTParser#parseAndCreateDocuments} saving its start offset and record
 * size for quick retrieval. The retrieval is very fast because we just get the bytes of the record inside the LDT file,
 * even if it is stored on S3.<br />
 * Also, when parseing and creating the data, it is possible to compress the ldt file (it is text, gain is ~70%)
 * <br>
 * The behavior is based on the configuration. We provide a "default" configuration that must be used as an example,
 * since it contains, by essence, each ldt files will be different for each user of the plugin.
 * See {@link resources/OSGI-INF/ldtparser-servoce.xml}
 * <br>
 * The main principle is based on the usage of:
 * - A start and end tokens to get the beginning and end of a record inside the file
 * - Regex to parse each line and extract headers and items.
 * <br>
 * The parser reads the file line by line and checks against the misc. configuration values to extract fields (a
 * "clientId", a "mont/year", etc.) and items of each record inside the ldt.
 * => Detail of of misc. configuration values can be found in {@link resources/OSGI-INF/ldtparser-servoce.xml}
 * => Also see the different descriptors at @{link nuxeo.ldt.parser.service.descriptors}
 * <br>
 * Also, callbacks are provided for the user of the class to be handle special cases, where, typically, a Regex can't be
 * used because the same line can contain different fields depending on custom rules.
 * <br>
 * The most common usage is the following:
 * <ul>
 * <li>Contribute an "ldtParser" extension with all the values you need</li>
 * <li>When an LDT file is uploaded, either automatically (via listener) or "manualy", parse it and create as many
 * LDTRecord document type that needed, also using your custom fields (like a clientId, a taxId, …)<br>
 * This is done using {@code LDTParser#parseAndCreateStatements}, and/or the operation calling it
 * ({@link nuxeo.ldt.parser.automation.LDTParseAndCreateDocumentsOp}<br>
 * Configuration lets you define the document type to use, the fields to map, etc., see ldtarser-service.xml
 * </li>
 * <li>When a record is needed, just call @{code LDTParser#getRecord}, and then @{code Record#toJson}. From this json,
 * you can render the record as you want (for example, using Nuxeo Template Rendering)<br>
 * The configuration lets you define the JSON properties you want (see recordJsonTemplate in ldtarser-service.xml)<br>
 * The plugin has an example using a converter. The converter gets the record as json, and uses Nuxeo Template Rendering
 * to generate a pdf. It first creates an HTML from the json, then uwe WebkitHtmlTopPDF to generate a pdf.
 * </li>
 * </ul>
 * 
 * @since 2021
 */
public class LDTParser {

    private static final Logger log = LogManager.getLogger(LDTParser.class);

    protected LDTParserDescriptor config;

    protected String name;

    protected String recordStartToken;

    protected String recordEndToken;

    protected Callbacks callbacks = null;

    protected int lengthOfEOF;

    protected long totalBytesRead = 0;

    protected long lineCount = 0;

    protected static int checkS3BlobProviderClass = -1;

    protected static boolean noS3BlobProviderWarnLogged = false;

    public LDTParser(LDTParserDescriptor config) {
        super();

        this.config = config;
        this.name = config.getName();
        this.recordStartToken = config.getRecordStartToken();
        this.recordEndToken = config.getRecordEndToken();

        loadCallbacksClass();
    }

    protected boolean hasS3BlobProviderClass() {
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

    public String getName() {
        return name;
    }

    /*
     * Returns the Callbacks object.
     * If configuration asks for a callback but there is no getCallbacksClass(), we trhow an error
     */
    protected Callbacks loadCallbacksClass() {
        if (config.useCallbackForRecord() || config.useCallbackForHeaders() || config.useCallbackForItems()) {
            if (callbacks == null) {
                if (config.getCallbacksClass() != null) {
                    try {
                        callbacks = (Callbacks) config.getCallbacksClass().getConstructor().newInstance();
                    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                        throw new NuxeoException("Cannot instantiate the callback class", e);
                    }
                } else {
                    throw new NuxeoException("Configuration set to use callbacks, but no callback class set");
                }
            }
        } else {
            callbacks = null;
        }

        return callbacks;
    }

    protected Callbacks getCallbacks() {
        return loadCallbacksClass();
    }

    /**
     * @return the {@code LDTParserDescriptor} for this parser
     * @since 2021
     */
    public LDTParserDescriptor getDescriptor() {
        return config;
    }

    /**
     * @param line
     * @return {@code true} is the line <i>starts with</i> the startRecordToken, as defined in the configuration
     * @since TODO
     */
    public boolean isRecordStart(String line) {
        return line.startsWith(recordStartToken);
    }

    /**
     * @param line
     * @return {@code true} is the line <i>contains</i> the endRecordToken, as defined in the configuration
     * @since TODO
     */
    public boolean isRecordEnd(String line) {
        return line.indexOf(recordEndToken) > -1;
    }

    /**
     * Parses the line and returns a {@code HeaderLine} if the line is a header.
     * If the configuration is set to use callbacks, the callback is used and in charge of
     * returning the {@code HeaderLine} or {@code null}
     * 
     * @param line
     * @param lineNumber
     * @return a {@code HeaderLine} (or {@code null} if the line is not a header)
     * @since 2021
     */
    public HeaderLine parseRecordHeader(String line, long lineNumber) {

        if (config.useCallbackForHeaders()) {
            return getCallbacks().parseHeader(config, line, lineNumber);
        }

        for (LDTHeaderDescriptor header : config.getHeaders()) {
            Matcher m = header.getCompiledPattern().matcher(line);
            if (m.matches()) {
                return new HeaderLine(m, header.getFields(), lineNumber, header.getName());
            }
        }

        return null;

    }

    /**
     * Check if a line is a header.
     * 
     * @param line
     * @return
     * @since 2021
     */
    public boolean isRecordHeader(String line) {

        if (config.useCallbackForHeaders()) {
            return getCallbacks().parseHeader(config, line, 0) == null;
        }

        for (LDTHeaderDescriptor header : config.getHeaders()) {
            Matcher m = header.getCompiledPattern().matcher(line);
            if (m.matches()) {
                return true;
            }
        }

        return false;

    }

    /**
     * Parses the line and returns an {@code Item} if the line is an Item (aka not a header).
     * If the configuration is set to use callbacks, the callback is used and in charge of
     * returning the {@code Item} or {@code null}
     * 
     * @param line
     * @return a {@code Item} (or {@code null} if the line is not an item)
     * @since 2021
     */
    public Item parseItem(String line) {

        // System.out.println("PARSE ITEM: <" + line + ">");

        if (config.useCallbackForItems()) {
            return getCallbacks().parseItem(config, line);
        }

        if (config.getParseItemAutomationCallback() != null) {
            String chainId = config.getParseItemAutomationCallback();

            AutomationService as = Framework.getService(AutomationService.class);
            OperationContext ctx = new OperationContext();
            Map<String, Object> params = new HashMap<>();
            params.put("line", line);
            params.put("config", config);
            try {

                JSONBlob jsonBlob = (JSONBlob) as.run(ctx, chainId, params);
                return new Item(line, jsonBlob);

            } catch (OperationException e) {
                throw new NuxeoException("Error while calling the <" + chainId + "> chain.", e);
            }

        }

        if (config.getDetailsLineMinSize() > 0 && line.length() < config.getDetailsLineMinSize()) {
            // System.out.println("LESS THAN MINIMAL LENGTH: " + line);
            return null;
        }

        // When parseItem is called, headers have been parsed.
        // But in multipage records, they are present in the "next" pages
        // We should ignore them
        if (isRecordHeader(line)) {
            return null;
        }

        return new Item(line, config);
    }

    /**
     * Receives all the lines from startRecordToken to endRecordToken (both included) and returns a {@code Record}.
     * If the configuration is set to use callbacks, the callback is used and in charge of returning the {@code Record}
     * 
     * @param lines
     * @return a {@code Record}
     * @since 2021
     */
    public Record parseRecord(List<String> lines) {

        if (config.useCallbackForRecord()) {
            Record rec = getCallbacks().parseRecord(config, lines);
            rec.setParser(this);
            return rec;
        }

        int idx = -1;
        ArrayList<HeaderLine> headers = new ArrayList<HeaderLine>();
        // A record always starts with at least one header line
        for (String oneLine : lines) {
            HeaderLine header = parseRecordHeader(oneLine, idx + 2);
            if (header != null) {
                headers.add(header);
                idx += 1;
            } else {
                // If we are at the first line then we have an issue.
                if (idx == -1) {
                    if (config.ignoreMalformedLines()) {
                        log.warn("IGNORED Malformed/Unexpected group(s) in headers (no header found) for line <"
                                + oneLine + ">");
                        return null;
                    }
                    throw new NuxeoException("Malformed header line, no matching Pattern found for <" + oneLine + ">");

                }
                break;
            }
        }

        List<Item> listItems = lines.subList(idx + 1, lines.size())
                                    .stream()
                                    .map(this::parseItem)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());

        // Counf pages
        int pageCount = 0;
        for (Item item : listItems) {
            if (item.isEndOfPage()) {
                pageCount += 1;
            }
        }

        return new Record(this, headers, listItems, pageCount);
    }

    /**
     * Fetches {@code recordSize} bytes from {@code startOffset} inside the blob. The blob must hold and LDT file.
     * The method does not parse the whole file, it fetches directly the bytes, even if the file is stored on s3.
     * 
     * @param blob
     * @param startOffset
     * @param recordSize
     * @return a @{code Record} or {@code null} if no record is found
     * @since 2021
     */
    public Record getRecord(Blob blob, long startOffset, long recordSize) {

        // a recordsize of 1, or even 10 or 100 is likely an error, but we can't make 100% sure.
        if (startOffset < 0 || Math.abs(recordSize) < 1) {
            throw new NuxeoException(
                    "getRecord: Invalid startOffest (" + startOffset + ") or recordSize (" + recordSize + ")");
        }

        // ==================================================
        // Get the String of the record
        // ==================================================
        String recordStr = null;
        boolean isCompressedLdt = false;
        if (recordSize < 0) {
            isCompressedLdt = true;
            recordSize = Math.abs(recordSize);
        }
        ByteRange range = ByteRange.inclusive(startOffset, startOffset + recordSize - 1);
        try (InputStream stream = LDTParserRecordStream.getStream(blob, range)) {
            byte[] recordBytes = stream.readNBytes((int) recordSize);
            if (isCompressedLdt) {
                recordStr = CompressedLDT.expand(recordBytes);
            } else {
                recordStr = new String(recordBytes, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new NuxeoException("Error reading the blob with a ByteRange.", e);
        }

        // Not found. And no exception => most likely invalid range/size.
        if (StringUtils.isEmpty(recordStr)) {
            return null;
        }

        // ==================================================
        // Get the record itself
        // ==================================================
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(recordStr.getBytes(StandardCharsets.UTF_8));
                InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                LineIterator it = new LineIterator(reader)) {

            // Go to start of record
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

            // ==================================================
            // Get the header(s)
            // ==================================================
            ArrayList<HeaderLine> headers = new ArrayList<HeaderLine>();
            // Assume we will always have at least one header
            HeaderLine header = parseRecordHeader(line, 1);
            if (header == null) {
                throw new NuxeoException("Malformed header line, no matching Pattern found for <" + line + ">");
            }
            rawRecord.add(line);
            int lineNumber = 1;
            do {
                line = it.nextLine();
                rawRecord.add(line);

                lineNumber += 1;
                header = parseRecordHeader(line, lineNumber);
                if (header != null) {
                    headers.add(header);
                }

            } while (header != null);

            // ==================================================
            // Then the items
            // ==================================================
            boolean reachedEnd = false;
            while (!reachedEnd && it.hasNext()) {
                String item = it.nextLine();
                if (isRecordStart(item)) {
                    // We have a header. Need to ignore all headers
                    // We don't check hasNext(): as long as we have not reached
                    // the recordEnd token, we *must* have a line
                    do {
                        item = it.nextLine();
                    } while (parseRecordHeader(line, 0) != null);
                }
                // We have an item
                rawRecord.add(item);
                if (isRecordEnd(item)) {
                    reachedEnd = true;
                }
            }

            return rawRecord.isEmpty() ? null : parseRecord(rawRecord);

        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    /**
     * Get a {@code Record} built from pages from a multi-page record.
     * See {@code getRecord(Blob blob, long startOffset, long recordSize)} for details on parameters.
     * 
     * @param blob
     * @param startOffset
     * @param recordSize
     * @param firstPage
     * @param lastPage
     * @return a @{code Record} or {@code null} if no record is found
     * @since 2021
     */
    public Record getRecord(Blob blob, long startOffset, long recordSize, int firstPage, int lastPage) {

        Record record = getRecord(blob, startOffset, recordSize);
        if (record != null) {
            return record.buildForPageRange(firstPage, lastPage);
        }

        return null;
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
     * The it parameter MUST be positioned at the beginning of a record
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

        ArrayList<HeaderLine> headers = new ArrayList<HeaderLine>();
        // Assume we will always have at least one header
        HeaderLine header = parseRecordHeader(line, 1);
        if (header == null) {
            throw new NuxeoException("Malformed header line, no matching Pattern found for <" + line + ">");
        }
        headers.add(header);

        // Assume we have no more than 100 header lines...
        for (int lineNumber = 1; lineNumber < 100; lineNumber++) {
            line = it.nextLine();
            lineCount += 1;
            totalBytesRead += line.length() + lengthOfEOF;
            header = parseRecordHeader(line, lineNumber);
            if (header != null) {
                headers.add(header);
            } else {
                break;
            }
        }
        // We catched the first itemLine above, but it's ok, there will be at least one item.

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

        long recordSize = totalBytesRead - recordStart;
        RecordInfo recInfo = new RecordInfo(recordStart, recordSize, lineStart, headers);

        return recInfo;

    }

    /**
     * Create as many @{code LDTRecords} (or @{code config.recordDocType}) as found in the blob of the input
     * inputLdtDoc.<br>
     * Update inputLdtDoc with LDT info
     * <br>
     * Records are created at the same level than the LDT itself, in folder whose title is set to @{code title of the
     * LDT + config.recordsContainerSuffix}<br>
     * <br>
     * Each record:
     * <ul>
     * <li>Is created as a @{code recordDocType} (see configuration) document type (@{code LDTRecord} by default)</li>
     * <li>Is filled with the retrieval info (start offset, size, line start) in the @{code ldtrecord} schema</li>
     * <li>And possibly with any field defined in the @{code recordFieldsMapping} mapping configuration.</li>
     * <li>The dc:title is set to @{code recordTitleFields} (or the inputLdtDoc.title + a the sequence number)</li>
     * </ul>
     * Caller is in charge of making sure permissions allow for creating content.
     * <br>
     * <b>WARNING</b>: Assume the file is UTF-8, even pure ASCII (no multibytes char)
     * 
     * @param inputLdtDoc, the input document whose file:content contains the LDT to parse
     * @return the @{code LDTInfo}
     * @since 2021
     */
    public LDTInfo parseAndCreateDocuments(DocumentModel inputLdtDoc, boolean compressLdt) {

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

            CompressedLDT compressedLdt = null;
            while (it.hasNext()) {

                // Get the whole record
                RecordInfo record = getOneRecordInfo(it);

                // If parsing whent wrong, the log has the info
                if (record == null) {
                    continue;
                }
                ByteRange range;
                if (compressLdt) {
                    if (compressedLdt == null) {
                        compressedLdt = new CompressedLDT(blob);
                    }
                    range = compressedLdt.add(record.startOffset, record.size);
                    // Realign the values, make record size negative, as flag for "values for compressed file"
                    // (we do it for recordSize, since sartoffset may be 0)
                    record.startOffset = range.getStart();
                    record.size = -range.getLength();
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
                recordDoc.setPropertyValue(Constants.XPATH_LDTRECORD_RELATED_LDT_DOC, inputLdtDoc.getId());
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

            if (compressedLdt != null) {
                Blob compressedLdtBlob = compressedLdt.close();
                inputLdtDoc.setPropertyValue("file:content", (Serializable) compressedLdtBlob);
            }
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
     * By default, we do not compress the LDT file.
     * See {@code parseAndCreateDocuments(DocumentModel inputLdtDoc, boolean compressLdt} for details
     * 
     * @param inputLdtDoc
     * @return
     * @since 2021
     */
    public LDTInfo parseAndCreateDocuments(DocumentModel inputLdtDoc) {
        return parseAndCreateDocuments(inputLdtDoc, false);
    }

    /**
     * This is mainly a helper for debug
     * 
     * @param recordDoc
     * @param parser can be null (will use "default")
     * @return
     * @since 2021
     */
    public static String documentToJsonString(DocumentModel recordDoc, String parserName) {
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

        LDTParser parser = Framework.getService(LDTParserService.class).newParser(parserName);
        LDTParserDescriptor desc = parser.getDescriptor();
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
