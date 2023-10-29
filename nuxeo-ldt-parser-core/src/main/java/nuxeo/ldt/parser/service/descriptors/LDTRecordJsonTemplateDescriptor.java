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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * See description of values in ldtparser-service.xml
 * 
 * @since 2021
 */
@XObject("itemLine")
public class LDTRecordJsonTemplateDescriptor {
   

    @XNode("rootName")
    protected String rootName = null;
    
    @XNodeList(value = "properties/property", type = ArrayList.class, componentType = String.class)
    protected List<String> properties = new ArrayList<>();

    public String getRootName() {
        return rootName;
    }
    
    /**
     * value can be empty or null (no root element)
     * @param value
     * @since TODO
     */
    public void setRootName(String value) {
        rootName = value;
    }
    
    public List<String> getProperties() {
        return properties;
    }
    
    
    
}
