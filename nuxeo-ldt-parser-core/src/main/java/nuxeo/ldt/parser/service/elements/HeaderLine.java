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
package nuxeo.ldt.parser.service.elements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.nuxeo.ecm.core.api.NuxeoException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A MainLine is a line that is, basically, the header(s) of the record.
 * Below a MainLine come n Item.
 * 
 * lineNumber is the indice in the records, not in the whole LDT file, and it starts at 1
 * 
 * @since 2021
 */
public class HeaderLine {

    protected List<String> fieldList;

    protected Map<String, String> fieldsAndValues;
    
    protected long lineNumber;
    
    public HeaderLine(List<String> fieldList, Map<String, String> fieldsAndValues, long lineNumber) {
        this.fieldList = fieldList;
        this.fieldsAndValues = fieldsAndValues;
        this.lineNumber = lineNumber;
    }

    public HeaderLine(Matcher m, List<String> fieldList, long lineNumber) {

        if (m.groupCount() != fieldList.size()) {
            throw new NuxeoException(String.format(
                    "Count of captured groups (%d) should be equal to the number of fields set in the configuration (%d)",
                    m.groupCount(), fieldList.size()));
        }
        
        this.lineNumber = lineNumber;

        this.fieldList = fieldList;
        fieldsAndValues = new HashMap<String, String>();
        for (int i = 0; i < fieldList.size(); i++) {
            fieldsAndValues.put(fieldList.get(i), m.group(i + 1).trim());
        }
    }

    public String getValue(String fieldName) {

        return fieldsAndValues.get(fieldName);
    }
    
    public long getLineNumber() {
        return lineNumber;
    }

    public String toString() {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString;
        try {
            jsonString = objectMapper.writeValueAsString(fieldsAndValues);
            jsonString = "{\"lineNumber\": " + lineNumber + ", \"fieldsAndValues\": " + jsonString +"}";
        } catch (JsonProcessingException e) {
            jsonString = "Error processing the fields to JSON";
        }
        return jsonString;
    }

}
