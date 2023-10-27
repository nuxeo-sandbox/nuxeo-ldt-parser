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

/**
 * RecordInfo is useful during the initial parsing of the ldt file.
 * It stores information about the record, but not the whole rerocd itseld:
 * - MainLine(s)
 * - Retrieval information (start offset etc.)
 * 
 * @since 2021
 */
public class RecordInfo {

    public long startOffset;

    public long size;

    public long startLine;

    public MainLine line1;

    public MainLine line2;

    public RecordInfo(long startOffset, long size, long startLine, MainLine line1, MainLine line2) {

        this.startOffset = startOffset;
        this.size = size;
        this.startLine = startLine;
        this.line1 = line1;
        this.line2 = line2;
    }

    public String toString() {

        String str = "{";
        str += "\"startOffset\": " + startOffset + ",";
        str += "\"size\": " + size + ",";
        str += "\"line1\": \"" + line1.toString() + "\",";
        str += "\"line2\": \"" + line2.toString() + "\"";
        str += "}";

        return str;
    }

    public String getValue(String key) {
        String value = line1.fieldsAndValues.get(key);
        if (value == null) {
            value = line2.fieldsAndValues.get(key);
        }

        return value;
    }

}
