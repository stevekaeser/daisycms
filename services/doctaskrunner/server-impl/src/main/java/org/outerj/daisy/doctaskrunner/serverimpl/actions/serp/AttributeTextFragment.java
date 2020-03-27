/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
 */
package org.outerj.daisy.doctaskrunner.serverimpl.actions.serp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.xml.sax.helpers.AttributesImpl;

/**
 * TextFragment that represents a particular attribute in a Sax Attributes instance 
 */
public class AttributeTextFragment implements TextFragment {
    
    private final String originalAttributeValue;
    private final String elementName;
    private final AttributesImpl attributes;
    private final String attributeName;
    
    private final Map<String, String> infoAttributes;

    public AttributeTextFragment(String elementName, AttributesImpl attributes, String attributeName, Map<String, String> partInfo) {
        this.originalAttributeValue = attributes.getValue(attributes.getIndex(attributeName));
        this.elementName = elementName;
        this.attributes = attributes;
        this.attributeName = attributeName;
        
        Map<String, String> tmp = new HashMap<String, String>(partInfo);
        tmp.put("type", "attribute");
        tmp.put("elementName", elementName);
        tmp.put("attributeName", attributeName);
        this.infoAttributes = Collections.unmodifiableMap(tmp);
    }
    
    public String getOriginalText() {
        return attributes.getValue(attributeName);
    }

    /**
     * alters the current saxbits
     */
    public int replace(Pattern pattern, String replacement, boolean useSensibleCase) {
        String resultText = SearchAndReplaceUtil.performSimpleTextReplacement(originalAttributeValue, replacement, pattern, useSensibleCase);
        attributes.setValue(attributes.getIndex(attributeName), resultText);
        return SearchAndReplaceUtil.countMatches(originalAttributeValue, pattern);
    }
    
    public String getAttributeName() {
        return attributeName;
    }
    
    public String getElementName() {
        return elementName;
    }
    
    public Map<String, String> getAttributes() {
        return infoAttributes;
    }

    public String getFragment() {
        return attributes.getValue(attributeName);
    }
    
}
