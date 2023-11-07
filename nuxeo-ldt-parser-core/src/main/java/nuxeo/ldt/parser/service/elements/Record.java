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

    protected LDTParser parser;

    @JsonProperty("headers")
    protected List<HeaderLine> headers;

    @JsonProperty("items")
    protected List<Item> items;

    protected int pageCount = 1;

    public Record(List<HeaderLine> headers, List<Item> items, int pageCount) {
        this.headers = headers;
        this.items = items;

        if(pageCount == 0) {
            pageCount = 1;
        }
        this.pageCount = pageCount;
    }

    public Record(LDTParser parser, List<HeaderLine> headers, List<Item> items, int pageCount) {
        this.parser = parser;
        this.headers = headers;
        this.items = items;

        if(pageCount == 0) {
            pageCount = 1;
        }
        this.pageCount = pageCount;
    }

    public void setParser(LDTParser parser) {
        parser = this.parser;
    }

    public int getPageCount() {
        return pageCount;
    }

    public String getHeadersValue(String key) {

        String value = null;

        for (HeaderLine header : headers) {
            value = header.getValue(key);
            if (value != null) {
                return value;
            }
        }

        return value;
    }

    public List<Item> getItems() {
        return items;
    }

    public Record buildForPageRange(int firstPage, int lastPage) {

        if (firstPage < 1) {
            firstPage = 1;
        }
        if (lastPage > this.pageCount) {
            lastPage = this.pageCount;
        }
        if (lastPage < firstPage) {
            lastPage = firstPage;
        }

        List<Item> newItems = new ArrayList<Item>();
        int pageCount = 0;
        for (Item item : this.items) {
            if (pageCount >= firstPage && pageCount <= lastPage) {
                newItems.add(item);
            }
            if (item.isEndOfPage()) {
                pageCount += 1;
            }
        }

        return new Record(parser, headers, newItems, lastPage - firstPage + 1);

    }

    public String toJson() throws JacksonException {

        LDTRecordJsonTemplateDescriptor templateDescriptor = parser.getDescriptor().getRecordJsonTemplate();
        String rootName = templateDescriptor.getRootName();
        JSONObject mainJson;
        mainJson = new JSONObject();
        JSONObject rootObject;
        if (!StringUtils.isBlank(rootName) && !"null".equals(rootName.toLowerCase())) {
            rootObject = new JSONObject();
            mainJson.put(rootName, rootObject);
        } else {
            rootObject = mainJson;
        }
        
        rootObject.put("pageCount", this.pageCount);

        for (String field : templateDescriptor.getProperties()) {
            rootObject.put(field, getHeadersValue(field));
        }

        // Now the items
        JSONArray jsonItems = new JSONArray();
        int order = 0;
        int pageCount = 1;
        for (Item item : items) {
            order += 1;
            JSONObject oneJsonItem = new JSONObject();
            oneJsonItem.put("order", order);
            oneJsonItem.put("type", item.getType());
            oneJsonItem.put("line", item.getLine());
            oneJsonItem.put("page", pageCount);
            if(item.getFieldsAndValues() != null) {
                for (Map.Entry<String, String> entry : item.getFieldsAndValues().entrySet()) {
                    oneJsonItem.put(entry.getKey(), entry.getValue());
                }
            }
            
            if(item.isEndOfPage()) {
                pageCount += 1;
            }

            jsonItems.put(oneJsonItem);
        }
        rootObject.put("items", jsonItems);

        return mainJson.toString();
    }

    public String toString() {

        StringBuilder builder = new StringBuilder();

        for (HeaderLine header : headers) {
            builder.append("Header:\n").append(header.toString()).append("\n");
        }

        builder.append("\nItems: ").append(items.size()).append("\n");
        for (Item item : items) {
            builder.append(item.toString());
        }

        return builder.toString();
    }

}