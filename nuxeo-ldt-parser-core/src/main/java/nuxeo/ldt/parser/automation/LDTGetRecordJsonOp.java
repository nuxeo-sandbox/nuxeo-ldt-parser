/*
 * (C) Copyright 2023 Hyland (http://hyland.com/) and others.
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
package nuxeo.ldt.parser.automation;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.JSONBlob;

import com.fasterxml.jackson.core.JacksonException;

import nuxeo.ldt.parser.service.LDTParser;
import nuxeo.ldt.parser.service.LDTParserService;
import nuxeo.ldt.parser.service.elements.Record;

@Operation(id = LDTGetRecordJsonOp.ID, category = Constants.CAT_DOCUMENT, label = "LDT: Get JSON record", description = ""
        + "Returns the JSON of a record. Input is a document (optional). If passed, it must have the ldtrecord schema, "
        + "the related LDT document must exist, and current user must have read permission on it. "
        + "Also, if Input is passed, sourceLdtDocId/startOffset/recordSize are ignored. "
        + "If input is not passed, then sourceLdtDocId/startOffset/recordSize are required.")
public class LDTGetRecordJsonOp {

    public static final String ID = "Services.GetLDTJsonRecord";

    private static final Logger log = LogManager.getLogger(LDTGetRecordJsonOp.class);

    @Context
    protected CoreSession session;

    @Context
    protected LDTParserService ldtParserService;

    @Param(name = "parserName", required = false, values = { "default" })
    protected String parserName = "default";

    @Param(name = "sourceLdtDocId", required = false)
    protected String sourceLdtDocId;

    @Param(name = "startOffset", required = false)
    protected Long startOffset;

    @Param(name = "recordSize", required = false)
    protected Long recordSize;

    @Param(name = "firstPage", required = false)
    protected Long firstPage;

    @Param(name = "lastPage", required = false)
    protected Long lastPage;

    protected Blob getRecordJson(String ldtDocId, Long startOffset, Long recordSize) throws JacksonException {

        if (StringUtils.isBlank(ldtDocId)) {
            throw new IllegalArgumentException("No Source LDT document");
        }

        if (startOffset == null) {
            throw new IllegalArgumentException("Missing startOffset parameter");
        }

        if (recordSize == null) {
            throw new IllegalArgumentException("Missing recordSize parameter");
        }

        IdRef ref = new IdRef(ldtDocId);
        DocumentModel ldtDoc = session.getDocument(ref);

        Blob ldtBlob = (Blob) ldtDoc.getPropertyValue("file:content");
        if (ldtBlob == null) {
            throw new NuxeoException("The related LDT document (id " + ref + ") has no blob.");
        }

        LDTParser parser = ldtParserService.newParser(parserName);
        Record record = parser.getRecord(ldtBlob, startOffset, recordSize);
        if(firstPage != null && lastPage != null) {
            record = record.buildForPageRange(firstPage.intValue(), lastPage.intValue());
        }

        String recordJsonStr = record.toJson();
        return new JSONBlob(recordJsonStr);
    }

    @OperationMethod
    public Blob run() throws Exception {

        return getRecordJson(sourceLdtDocId, startOffset, recordSize);
    }

    @OperationMethod
    public Blob run(DocumentModel input) throws IOException {

        if (!input.hasSchema(nuxeo.ldt.parser.service.Constants.SCHEMA_LDTRECORD)) {
            log.warn(ID + ": input document does not have the ldtrecord schema => ignoring");
            return null;
        }

        sourceLdtDocId = (String) input.getPropertyValue(
                nuxeo.ldt.parser.service.Constants.XPATH_LDTRECORD_RELATED_LDT_DOC);
        if (StringUtils.isBlank(sourceLdtDocId)) {
            throw new NuxeoException(
                    "" + nuxeo.ldt.parser.service.Constants.XPATH_LDTRECORD_RELATED_LDT_DOC + " is empty)");
        }

        startOffset = (long) input.getPropertyValue(nuxeo.ldt.parser.service.Constants.XPATH_LDTRECORD_STARTOFFSET);
        recordSize = (long) input.getPropertyValue(nuxeo.ldt.parser.service.Constants.XPATH_LDTRECORD_RECORDSIZE);

        return getRecordJson(sourceLdtDocId, startOffset, recordSize);
    }

}
