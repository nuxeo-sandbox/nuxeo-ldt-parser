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
package nuxeo.ldt.parser.service;

import java.util.List;

import nuxeo.ldt.parser.service.descriptors.LDTParserDescriptor;
import nuxeo.ldt.parser.service.elements.HeaderLine;
import nuxeo.ldt.parser.service.elements.Item;
import nuxeo.ldt.parser.service.elements.Record;

/**
 * When parsing an ldt file, the {@code LDTParse} class analyses each line using the configuration defined in the
 * extension point.
 * If you know the lines can't really be parsed with a Regex extracting groups (typically, values can change, be totally
 * empty, you actually need to indexOf() etc.), then you can configure callbacks.
 * Instead of trying to parse the file and extract a HeaderLine, an Item and their fields, the LDTParser will call your
 * callback. For cases when headers and items can't be parsed with Regex, you can use the callback on the whole record
 * (in this case you likely don't need callbacks for headers and items)
 * <br>
 * re is a call
 * <br>
 * The configuration parameters to use are:<br>
 * <ul>
 * <li>One boolean for each callback, telling the parser to use a callback or not: useCallbackForHeaders,
 * useCallbackForItems</li>
 * <li>And the calss to use, which must of course implements this interface: callbacksClass</li>
 * </ul>
 * See {@code LDTParserDescriptor} and the ldtparser-service.xml default parser.
 * <br>
 * Also, you can look at the {@codeCallbacksExample} call and the unit tests
 * 
 * @since 2021
 */
public interface Callbacks {

    /**
     * This callback receives all the lines of the record, from the start token to the endtoken and must return a
     * {@code Record}.
     * 
     * @param config
     * @param lines
     * @return a Record
     * @since 2021
     */
    Record parseRecord(LDTParserDescriptor config, List<String> lines);

    /**
     * This callback received one line and returns a {@code HeaderLine}.
     * If the line is not a header, return {@code null}
     * 
     * @param config
     * @param line
     * @param lineNumber
     * @return a {@code HeaderLine} or {@code null}
     * @since 2021
     */
    HeaderLine parseHeader(LDTParserDescriptor config, String line, long lineNumber);

    /**
     * This callbacks receives the line of an item (basically, a line that is not a header) and returns an {@code Item}.
     * The parser expects a valid Item, the callback canoot return{@code null}
     * 
     * @param config
     * @param line
     * @return a new {@code Item}
     * @since TODO
     */
    Item parseItem(LDTParserDescriptor config, String line);

}
