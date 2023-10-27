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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * See description of values in ldtparser-service.xml
 * 
 * @since 2021
 */
@XObject("itemLine")
public class LDTItemDescriptor {

    @XNode("type")
    protected String type = null;
    
    @XNode("pattern")
    protected String patternStr = null;
    
    protected Pattern pattern = null;
    
    @XNodeList(value = "fields/field", type = ArrayList.class, componentType = String.class)
    protected List<String> fields = new ArrayList<>();

    public String getType() {
        return type;
    }

    public Pattern getPattern() {
        if(pattern == null) {
            pattern = Pattern.compile(patternStr);
        }
        return pattern;
    }
    
    public List<String> getFields() {
        return fields;
    }
    
}
