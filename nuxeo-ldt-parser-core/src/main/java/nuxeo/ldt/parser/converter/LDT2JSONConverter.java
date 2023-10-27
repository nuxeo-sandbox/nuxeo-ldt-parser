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

package nuxeo.ldt.parser.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import nuxeo.ldt.parser.service.LDTParser;
import nuxeo.ldt.parser.service.LDTParserService;
import nuxeo.ldt.parser.service.elements.Record;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.JSONBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.runtime.api.Framework;

import java.io.Serializable;
import java.util.Map;

public class LDT2JSONConverter implements Converter {

    private static final Logger log = LogManager.getLogger(LDT2JSONConverter.class);

    @Override
    public void init(ConverterDescriptor converterDescriptor) {
        //nothing to do
    }

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> map) throws ConversionException {
        
        String parserName = (String) map.get("parserName");
        String startOffsetStr = (String) map.get("startOffset");
        String recordSizeStr = (String) map.get("recordSize");
        String targetfilename = (String) map.get("targetFileName");
        
        if(StringUtils.isAnyEmpty(startOffsetStr, recordSizeStr)) {
            throw new ConversionException("Missing the startOffset and/or the recordSize");
        }
        
        long startOffset = Long.parseLong(startOffsetStr);
        long recordSize = Long.parseLong(recordSizeStr);

        LDTParser parser = Framework.getService(LDTParserService.class).getParser(parserName);
        Record record = parser.getRecord(blobHolder.getBlob(), startOffset, recordSize);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            //objectMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
            String json = objectMapper.writeValueAsString(record);
            Blob jsonBlob = new JSONBlob(json);
            if (StringUtils.isBlank(targetfilename)) {
                targetfilename = "output.json";
            }
            if(!targetfilename.endsWith(".json")) {
                targetfilename += ".json";
            }

            return new SimpleBlobHolder(jsonBlob);
            
        } catch (JsonProcessingException e) {
            throw new ConversionException(e);
        }
    }
}
