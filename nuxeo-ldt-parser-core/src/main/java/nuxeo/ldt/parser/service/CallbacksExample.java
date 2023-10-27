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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import nuxeo.ldt.parser.service.LDTParser.Item;

/**
 * 
 * @since TODO
 */
public class CallbacksExample implements Callbacks {

    @Override
    public Item parseItem(String line) {
        
        List<String> fieldList = Arrays.asList("testField1", "testField2");
        Map<String, String> fieldsAndValues = Map.of("testField1", "value1", "testField2", "value2");
        
        return new Item(line, "TheType", fieldList, fieldsAndValues);
    }

}
