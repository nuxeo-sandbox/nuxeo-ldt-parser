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

    @XNode("useCallbackForHeaders")
    protected Boolean useCallbackForHeaders = false;
    
    @XNodeList(value = "headers/header", type = LDTHeaderDescriptor[].class, componentType = LDTHeaderDescriptor.class)
    protected LDTHeaderDescriptor[] headers;

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

    public boolean useCallbackForHeaders() {
        return useCallbackForHeaders.booleanValue();
    }
    
    public LDTHeaderDescriptor[] getHeaders() {
        return headers;
    }

    public List<String> getAllHeaderfields() {
        
        List<String> finalList = new ArrayList<String>();
        
        for(LDTHeaderDescriptor headerDesc : headers) {
            List<String> mergedList = new ArrayList<String>(headerDesc.getFields());
            finalList.addAll(mergedList);
        }

        return finalList;
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
