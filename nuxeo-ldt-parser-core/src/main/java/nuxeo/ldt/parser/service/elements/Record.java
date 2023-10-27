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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * A Record holds the full record : MainLines and Items
 * 
 * @since 2021
 */
@JsonRootName(value = "record")
public class Record {

    @JsonProperty("line1")
    protected MainLine line1;

    @JsonProperty("line2")
    protected MainLine line2;

    @JsonProperty("items")
    protected List<Item> items;

    public Record(MainLine line1, MainLine line2, List<Item> items) {
        this.line1 = line1;
        this.line2 = line2;
        this.items = items;
    }

    public String toJson() throws JacksonException {
        // @TODO - tune all this, allows for sending a "proper" json, not all in a raw
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
        String json = objectMapper.writeValueAsString(this);

        return json;
    }

    public String getMainLinesValue(String key) {

        String value = null;

        if (line1 != null) {
            value = line1.getValue(key);
        }

        if (value == null && line2 != null) {
            value = line2.getValue(key);
        }

        return value;
    }

    public List<Item> getItems() {
        return items;
    }

}