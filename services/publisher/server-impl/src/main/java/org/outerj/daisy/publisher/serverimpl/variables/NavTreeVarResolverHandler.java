/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.publisher.serverimpl.variables;

import org.outerj.daisy.publisher.serverimpl.AbstractHandler;
import org.outerj.daisy.util.Constants;
import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * A SAX handler which will resolve variables in the labels of navigation
 * tree nodes.
 */
public class NavTreeVarResolverHandler extends AbstractHandler {
    private static final String NS = Constants.NAVIGATION_NS;
    private Variables variables;

    public NavTreeVarResolverHandler(ContentHandler consumer, Variables variables) {
        super(consumer);
        this.variables = variables;
    }

    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (uri.equals(NS) && (localName.equals("doc") || localName.equals("group") || localName.equals("link"))) {
            String label = atts.getValue("label");
            if (label != null) {
                label = VariablesHelper.substituteVariables(label, variables);
                if (label != null) {
                    AttributesImpl newAttrs = new AttributesImpl(atts);
                    newAttrs.setValue(newAttrs.getIndex("label"), label);
                    atts = newAttrs;
                }
            }
        }
        super.startElement(uri, localName, qName, atts);
    }
}
