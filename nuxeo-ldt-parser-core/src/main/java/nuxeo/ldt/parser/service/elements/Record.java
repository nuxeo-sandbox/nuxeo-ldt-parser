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
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JacksonException;

import nuxeo.ldt.parser.service.descriptors.LDTRecordJsonTemplateDescriptor;
import nuxeo.ldt.parser.service.LDTParser;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A Record holds the full record : MainLines and Items
 * 
 * @since 2021
 */
@JsonRootName(value = "record")
public class Record {
    
    LDTParser parser;

    @JsonProperty("headers")
    protected ArrayList<HeaderLine> headers;

    @JsonProperty("items")
    protected List<Item> items;

    public Record(LDTParser parser, ArrayList<HeaderLine> headers, List<Item> items) {
        this.parser = parser;
        this.headers = headers;
        this.items = items;
    }

    public String toJson() throws JacksonException {
        
        LDTRecordJsonTemplateDescriptor templateDescriptor = parser.getDescriptor().getRecordJsonTemplate();
        String rootName = templateDescriptor.getRootName();
        JSONObject mainJson;
        mainJson = new JSONObject();
        JSONObject rootObject;
        if(!StringUtils.isBlank(rootName) && !"null".equals(rootName.toLowerCase())) {
            rootObject = new JSONObject();
            mainJson.put(rootName, rootObject);
        } else {
            rootObject = mainJson;
        }
        
        for(String field : templateDescriptor.getProperties()) {
            rootObject.put(field, getHeadersValue(field));
        }
        
        // Now the items
        JSONArray jsonItems = new JSONArray();
        for(Item item : items) {
            
            JSONObject oneJsonItem = new JSONObject();
            oneJsonItem.put("type", item.getType());
            oneJsonItem.put("line", item.getLine());
            for (Map.Entry<String, String> entry : item.getFieldsAndValues().entrySet()) {
                oneJsonItem.put(entry.getKey(), entry.getValue());
            }
            
            jsonItems.put(oneJsonItem);
        }
        rootObject.put("items", jsonItems);

        return mainJson.toString();
    }

    public String getHeadersValue(String key) {

        String value = null;
        
        for(HeaderLine header : headers) {
            value = header.getValue(key);
            if(value != null) {
                return value;
            }
        }

        return value;
    }

    public List<Item> getItems() {
        return items;
    }
    
    public String toString() {
        
        StringBuilder builder = new StringBuilder();
        
        for(HeaderLine header : headers) {
            builder.append("Header:\n")
                   .append(header.toString())
                   .append("\n");
        }
        
        builder.append("\nItems: ")
               .append(items.size())
               .append("\n");
        for(Item item : items) {
            builder.append(item.toString());
        }
        
        return builder.toString();
    }

}