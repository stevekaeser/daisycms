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
package org.outerj.daisy.frontend;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.transformation.AbstractTransformer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class FilterNameSpaceTransformer extends AbstractTransformer {
    
    private Map<String, String> prefixMappings = new HashMap<String, String>();
    private boolean enabled = false;
    private String namespace;

    public void setup(SourceResolver sourceResolver, Map resolver, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException {
        enabled = parameters.getParameterAsBoolean("condition", false);
        try {
            namespace = parameters.getParameter("namespace");
        } catch (ParameterException e) {
            throw new SAXException("namespace parameter is missing");
        }
    }
    
    @Override
    public void recycle() {
        this.prefixMappings.clear();
        this.enabled = false;
        this.namespace = null;
    }



    @Override
    public void endElement(String uri, String loc, String raw)
            throws SAXException {
        if (enabled && uri != null && uri.equals(namespace))
            return;
        super.endElement(uri, loc, raw);
    }

    @Override
    public void startElement(String uri, String loc, String raw, Attributes a)
            throws SAXException {
        if (!enabled) {
            super.startElement(uri, loc, raw, a);
            return;
        }
        
        // if element is in the namespace, drop it entirely
        if (uri != null && uri.equals(namespace))
            return;
        
        // else just filter the attributes
        AttributesImpl atts = new AttributesImpl();
        for (int i=0; i<a.getLength(); i++) {
            if (a.getURI(i) == null || !a.getURI(i).equals(namespace)) {
                atts.addAttribute(a.getURI(i), a.getLocalName(i), a.getQName(i), a.getType(i), a.getValue(i));
            }
        }
        super.startElement(uri, loc, raw, atts);
    }

}
