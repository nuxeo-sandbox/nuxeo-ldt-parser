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

import nuxeo.ldt.parser.service.LDTParser;
import nuxeo.ldt.parser.service.LDTParserService;
import nuxeo.ldt.parser.service.elements.Record;

@Operation(id = LDTGetRecordJsonOp.ID, category = Constants.CAT_DOCUMENT, label = "LDT: Get JSON record", description = "Input document  must have the ldtrecord schema, and the related LDT document must exist and current user must have read permission on it.")
public class LDTGetRecordJsonOp {

    public static final String ID = "Services.GetLDTJsonRecord";

    private static final Logger log = LogManager.getLogger(LDTGetRecordJsonOp.class);

    @Context
    protected CoreSession session;

    @Context
    protected LDTParserService ldtParserService;

    @Param(name = "parserName", required = false, values = { "default" })
    protected String parserName = "default";

    @OperationMethod
    public Blob run(DocumentModel input) throws IOException {

        if (!input.hasSchema(nuxeo.ldt.parser.service.Constants.SCHEMA_LDTRECORD)) {
            log.warn(ID + ": input document does not have the ldtrecord schema => ignoring");
            return null;
        }

        String ldtDocId = (String) input.getPropertyValue(
                nuxeo.ldt.parser.service.Constants.XPATH_LDTRECORD_RELATED_LDT_DOC);
        if (StringUtils.isBlank(ldtDocId)) {
            throw new NuxeoException("No related document containing an ldt file in its blob ("
                    + nuxeo.ldt.parser.service.Constants.XPATH_LDTRECORD_RELATED_LDT_DOC + " is empty)");
        }
        
        IdRef ref = new IdRef(ldtDocId);
        DocumentModel ldtDoc = session.getDocument(ref);

        Blob ldtBlob = (Blob) ldtDoc.getPropertyValue("file:content");
        if (ldtBlob == null) {
            throw new NuxeoException("The related document (id " + ref + ") has no blob.");
        }

        LDTParser parser = ldtParserService.newParser(parserName);

        long startOfsset = (long) input.getPropertyValue(
                nuxeo.ldt.parser.service.Constants.XPATH_LDTRECORD_STARTOFFSET);
        long recordSize = (long) input.getPropertyValue(nuxeo.ldt.parser.service.Constants.XPATH_LDTRECORD_RECORDSIZE);
        Record record = parser.getRecord(ldtBlob, startOfsset, recordSize);

        String recordJsonStr = record.toJson();
        return new JSONBlob(recordJsonStr);
    }

}
