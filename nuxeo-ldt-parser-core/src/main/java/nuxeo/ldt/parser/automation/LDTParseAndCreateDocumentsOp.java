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

import nuxeo.ldt.parser.service.LDTParser;
import nuxeo.ldt.parser.service.LDTParserService;
import nuxeo.ldt.parser.service.LDTParser.LDTInfo;

@Operation(id = LDTParseAndCreateDocumentsOp.ID, category = Constants.CAT_SERVICES, label = "LDT: Parse and Create Documents", description = "Parses the input LDT, reate as many LDTRecord (or other doctype, based on configuiration of the parser) as needed, update the LDT input doc with the info")
public class LDTParseAndCreateDocumentsOp {

    public static final String ID = "Services.LDTParseAndCreateDocuments";
    
    private static final Logger log = LogManager.getLogger(LDTParseAndCreateDocumentsOp.class);

    @Context
    protected CoreSession session;
    
    @Context
    protected LDTParserService ldtParserService;
    
    @Param(name = "parserName", required = false, values = { "default" })
    protected String parserName = "default";
    
    @Param(name = "compressLdt", required = false, values = { "false" })
    protected boolean compressLdt = false;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws IOException {

        if(!doc.hasSchema(nuxeo.ldt.parser.service.Constants.SCHEMA_LDT)) {
            log.warn(ID + ": input document does not have the ldt schema => ignoring");
            return doc;
        }
        
        Blob blob = (Blob) doc.getPropertyValue("file:content");
        if(blob == null) {
            log.warn(ID + ": input document has no blob => ignoring");
            return doc;
        }
        
        LDTParser parser = ldtParserService.newParser(parserName);
        @SuppressWarnings("unused")
        LDTInfo info = parser.parseAndCreateDocuments(doc, compressLdt);
        
        doc = session.getDocument(doc.getRef());
        
        return doc;
    }

}
