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
package nuxeo.ldt.parser.service.descriptors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

import nuxeo.ldt.parser.service.Constants;

/**
 * See description of values in ldtparser-service.xml
 * 
 * @since 2021
 */
@XObject("ldtParser")
public class LDTParserDescriptor {

    @XNode("name")
    protected String name = null;

    /*
     * @XNode("class")
     * protected Class<?> klass;
     */

    @XNode("recordStartToken")
    protected String recordStartToken = null;

    @XNode("recordEndToken")
    protected String recordEndToken = null;

    // @XNode("useCallback")
    // protected Boolean useCallback;
    
    
    // ================================ TEST
    @XNodeList(value = "headers", type = LDTHeaderLineDescriptor.class, componentType = LDTHeaderLineDescriptor.class)
    public LDTItemDescriptor headers;
    // ================================ TEST
    
    public LDTItemDescriptor getHeaders() {
        return headers;
    }

    @XNode("patternLine1")
    protected String patternLine1 = null;

    @XNode("altPatternLine1")
    protected String altPatternLine1 = null;

    @XNodeList(value = "fieldsLine1/field", type = ArrayList.class, componentType = String.class)
    protected List<String> fieldsLine1 = new ArrayList<>();

    @XNode("patternLine2")
    protected String patternLine2 = null;

    @XNode("altPatternLine2")
    protected String altPatternLine2 = null;

    @XNodeList(value = "fieldsLine2/field", type = ArrayList.class, componentType = String.class)
    protected List<String> fieldsLine2 = new ArrayList<>();

    @XNode("useCallbackForItems")
    protected Boolean useCallbackForItems = false;

    @XNode("callbacksClass")
    protected Class<?> callbacksClass;

    @XNodeList(value = "itemLine", type = LDTItemDescriptor[].class, componentType = LDTItemDescriptor.class)
    public LDTItemDescriptor[] items;

    @XNode("detailsLineMinSize")
    protected Integer detailsLineMinSize = 0;

    @XNode("ignoreMalformedLines")
    protected Boolean ignoreMalformedLines = false;

    @XNode("recordDocType")
    protected String recordDocType = null;

    @XNodeMap(value = "recordFieldsMapping/field", key = "@xpath", type = HashMap.class, componentType = String.class)
    protected Map<String, String> recordFieldsMapping = new HashMap<>();

    @XNode("recordsContainerDocType")
    protected String recordsContainerDocType = null;

    @XNode("recordsContainerSuffix")
    protected String recordsContainerSuffix = null;

    @XNodeList(value = "recordTitleFields/field", type = ArrayList.class, componentType = String.class)
    protected List<String> recordTitleFields = new ArrayList<>();

    @XNode(value = "recordJsonTemplate")
    protected LDTRecordJsonTemplateDescriptor recordJsonTemplate;

    public String getName() {
        return name;
    }

    // public Class<?> getKlass() {
    // return klass;
    // }

    public String getRecordStartToken() {
        return recordStartToken;
    }

    public String getRecordEndToken() {
        return recordEndToken;
    }

    public String getPatternLine1() {
        return patternLine1;
    }

    public String getAltPatternLine1() {
        return altPatternLine1;
    }

    public List<String> getFieldsLine1() {
        return fieldsLine1;
    }

    public String getPatternLine2() {
        return patternLine2;
    }

    public String getAltPatternLine2() {
        return altPatternLine2;
    }

    public List<String> getFieldsLine2() {
        return fieldsLine2;
    }

    public List<String> getAllLinefields() {
        List<String> mergedList = null;

        if (fieldsLine1 != null) {
            mergedList = new ArrayList<>(fieldsLine1);
        }

        if (fieldsLine2 != null) {
            if (mergedList == null) {
                mergedList = new ArrayList<>(fieldsLine2);
            } else {
                mergedList.addAll(fieldsLine2);
            }
        }

        return mergedList;
    }

    public boolean useCallbackForItems() {
        return useCallbackForItems.booleanValue();
    }

    public void setUseCallbackForItems(boolean value) {
        useCallbackForItems = value;
    }

    public Class<?> getCallbacksClass() {
        return callbacksClass;
    }

    public void setCallbacksClass(Class<?> klass) {
        callbacksClass = klass;
    }

    public LDTItemDescriptor[] getItems() {
        return items;
    }

    public boolean ignoreMalformedLines() {
        return ignoreMalformedLines.booleanValue();
    }

    public void setIgnoreMalformedLines(boolean value) {
        ignoreMalformedLines = value;
    }

    public int getDetailsLineMinSize() {
        if (detailsLineMinSize == null) {
            detailsLineMinSize = 0;
        }
        return detailsLineMinSize.intValue();
    }

    public String getRecordDocType() {
        if (recordDocType == null) {
            recordDocType = Constants.DOC_TYPE_LDTRECORD;
        }

        return recordDocType;
    }

    public Map<String, String> getRecordFieldsMapping() {
        return recordFieldsMapping;
    }

    /**
     * Concatenate the list of 2 fields, return only the keys
     * 
     * @return the list of fields. If none was defined, returns an empty list (nont null)
     */
    public List<String> getLDTRecordXPaths() {

        if (recordFieldsMapping != null) {
            return new ArrayList<String>(recordFieldsMapping.keySet());
        }

        return new ArrayList<String>();
    }

    public String getRecordsContainerDocType() {
        if (recordsContainerDocType == null) {
            recordsContainerDocType = Constants.RECORDS_CONTAINER_TYPE;
        }

        return recordsContainerDocType;
    }

    public String getRecordsContainerSuffix() {
        if (recordsContainerSuffix == null) {
            recordsContainerSuffix = Constants.RECORDS_CONTAINER_SUFFIX;
        }

        return recordsContainerSuffix;
    }

    public List<String> getRecordTitleFields() {
        return recordTitleFields;
    }
    
    public LDTRecordJsonTemplateDescriptor getRecordJsonTemplate() {
        return recordJsonTemplate;
    }

}
