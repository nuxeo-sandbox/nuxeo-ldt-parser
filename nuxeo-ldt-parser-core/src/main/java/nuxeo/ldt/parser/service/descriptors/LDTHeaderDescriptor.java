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
import java.util.List;
import java.util.regex.Pattern;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * See description of values in ldtparser-service.xml
 * 
 * @since 2021
 */
@XObject("header")
public class LDTHeaderDescriptor {
    
    protected Pattern compiledPattern = null;
   
    @XNode("name")
    protected String name = null;
    
    @XNode("pattern")
    protected String pattern = null;
    
    @XNodeList(value = "fields/field", type = ArrayList.class, componentType = String.class)
    protected List<String> fields = new ArrayList<>();

    public String getName() {
        return name;
    }
    
    public String getPattern() {
        return pattern;
    }
    
    public Pattern getCompiledPattern() {
        
        if(compiledPattern == null) {
            compiledPattern = Pattern.compile(pattern);
        }
        
        return compiledPattern;
    }
    
    public List<String> getFields() {
        return fields;
    }
    
}
