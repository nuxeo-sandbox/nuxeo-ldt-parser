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
 *     Michael Vachette
 *     Thibaud Arguillere
 */
package nuxeo.ldt.parser.service.elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.JSONBlob;

import com.fasterxml.jackson.annotation.JsonProperty;

import nuxeo.ldt.parser.service.descriptors.LDTItemDescriptor;
import nuxeo.ldt.parser.service.descriptors.LDTParserDescriptor;
import nuxeo.ldt.parser.service.Constants;

/**
 * An Item is a line in the LDT that, basically, is not a MainLine.
 * Fields and mapping is defined in the contribution.
 * 
 * @since 2021
 */
public class Item {

    private static final Logger log = LogManager.getLogger(Item.class);

    protected String line;

    @JsonProperty("type")
    protected String type = null;

    protected List<String> fieldList = null;

    protected boolean isEndOfPage = false;

    @JsonProperty("values")
    protected Map<String, String> fieldsAndValues = null;

    public Item(String line, String type, List<String> fieldList, Map<String, String> fieldsAndValues,
            boolean isEndOfPage) {

        this.line = line;
        this.type = type;
        this.fieldList = fieldList;
        this.fieldsAndValues = fieldsAndValues;

        this.isEndOfPage = isEndOfPage;

        if (fieldsAndValues == null || fieldsAndValues.size() < 1) {
            // throw new NuxeoException("No fieldsAndValues set for the new Item.");
            log.warn("No item fieldsAndValues set.");
        }
    }

    /*
     * Expected JSON:
     * {
     * "type": "TheLineType",
     * "fieldList": ["field1", "field2, "field3"], // String array:
     * "fieldsAndValues": [ // array of objects key/value. ALL VALUES ARE STRING
     * {"field": "field1", "value": "value1"},
     * {"field": "field2", "value2": "value3"},
     * {"field": "field3", "value4": "value3"}
     * ],
     * "isEndOfPage": true/false
     * }
     */
    public Item(String line, JSONBlob jsonBlob) {

        this.line = line;

        JSONObject json = new JSONObject(jsonBlob.getString());

        this.type = json.getString("type");

        this.isEndOfPage = json.optBoolean("isEndOfPage");

        JSONArray jsonArray = json.getJSONArray("fieldList");
        fieldList = new ArrayList<String>();
        for (int i = 0; i < jsonArray.length(); i++) {
            fieldList.add(jsonArray.getString(i));
        }

        jsonArray = json.getJSONArray("fieldsAndValues");
        fieldsAndValues = new HashMap<String, String>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            fieldsAndValues.put(obj.getString("field"), obj.getString("value"));
        }

        if (fieldsAndValues.size() < 1) {
            // throw new NuxeoException("No fieldsAndValues set for the new Item.");
            log.warn("No item fieldsAndValues set.");
        }
    }

    public Item(String line, LDTParserDescriptor config) {
        this.line = line;

        LDTItemDescriptor[] itemsDesc = config.getItems();
        for (LDTItemDescriptor oneDesc : itemsDesc) {
            // log.info("Testing with " + oneDesc.getType() + "...");
            Matcher m = oneDesc.getPattern().matcher(line);
            if (m.matches()) {
                // System.out.println("NEW ITEM: <" + oneDesc.getType() + ">");
                type = oneDesc.getType();
                fieldList = oneDesc.getFields();
                if (m.groupCount() != fieldList.size()) {
                    throw new NuxeoException(String.format(
                            "Count of captured groups (%d) should be equal to the number of fields set in the configuration (%d) for line <%s>",
                            m.groupCount(), fieldList.size(), line));
                }
                fieldsAndValues = new HashMap<String, String>();
                for (int i = 0; i < fieldList.size(); i++) {
                    if (!Constants.IGNORE_ITEM_FIELD_TAG.equals(fieldList.get(i))) {
                        fieldsAndValues.put(fieldList.get(i), m.group(i + 1).trim());
                    }
                }
                
                this.isEndOfPage = oneDesc.isEndOfPage();
                
                break;
            }
        }

        if (fieldsAndValues == null) {
            // throw new NuxeoException("No item descriptor found matching the line <" + line + ">");
            log.warn("No item descriptor found matching the line <" + line + ">");
        }

    }

    public String toString() {
        StringBuilder str = new StringBuilder("Line:\n");
        str.append(line).append("\nType: ").append(type).append("\nfieldsAndValues:\n");
        for (Map.Entry<String, String> entry : fieldsAndValues.entrySet()) {
            str.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        return str.toString();
    }

    public String getLine() {
        return line;
    }

    public String getType() {
        return type;
    }

    public List<String> getFields() {
        return fieldList;
    }

    public Map<String, String> getFieldsAndValues() {
        return fieldsAndValues;
    }

    public String getValue(String field) {
        if (fieldsAndValues != null) {
            return fieldsAndValues.get(field);
        }

        return null;
    }

    public boolean isEndOfPage() {
        return this.isEndOfPage;
    }

}
