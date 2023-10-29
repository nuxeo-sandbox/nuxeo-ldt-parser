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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;

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

    @JsonProperty("values")
    protected Map<String, String> fieldsAndValues = null;

    public Item(String line, String type, List<String> fieldList, Map<String, String> fieldsAndValues) {
        this.line = line;
        this.type = type;
        this.fieldList = fieldList;
        this.fieldsAndValues = fieldsAndValues;
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
                break;
            }
        }

        if (fieldsAndValues == null) {
            // throw new NuxeoException("No item descriptor found matching the line <" + line + ">");
            log.warn("No item descriptor found matching the line <" + line + ">");
        }

    }

    public String toString() {
        return line;
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

}
