/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
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

import org.apache.cocoon.transformation.AbstractTransformer;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.components.flow.FlowHelper;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.commons.jxpath.JXPathContext;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.util.DaisyLinkUtil;
import org.outerj.daisy.util.Constants;
import org.outerj.daisy.repository.VariantKey;

import java.util.Map;
import java.util.regex.Matcher;
import java.io.IOException;

public class DaisyLinkTransformer extends AbstractTransformer {
    private String mountPoint;
    private SiteConf siteConf;
    private VariantKey documentKey;

    private static final String LT_NAMESPACE = "http://outerx.org/daisy/1.0#linktransformer";
    private static final String PUBLISHER_NAMESPACE = "http://outerx.org/daisy/1.0#publisher";

    public void setup(SourceResolver sourceResolver, Map objectModel, String s, Parameters parameters) throws ProcessingException, SAXException, IOException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        FrontEndContext frontEndContext = FrontEndContext.get(request);
        siteConf = frontEndContext.getSiteConf();
        mountPoint = frontEndContext.getMountPoint();

        Object flowContext = FlowHelper.getContextObject(objectModel);
        JXPathContext jxpc = JXPathContext.newContext(flowContext);
        documentKey = (VariantKey)jxpc.getValue("/documentKey");
        if (documentKey == null)
            throw new ProcessingException("Unexpected error in DaisyLinkTransformer: documentKey is missing in flow context.");
    }

    public void recycle() {
        super.recycle();
        this.documentKey = null;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes attributes) throws SAXException {
        int attrIndex = -1;
        if (namespaceURI.equals(Constants.DAISY_NAMESPACE) && localName.equals("link")) {
            attrIndex = attributes.getIndex("", "target");            
        } else if (localName.equals("a") && namespaceURI.equals("")) {
            attrIndex = attributes.getIndex("", "href");            
        } else if (localName.equals("img") && namespaceURI.equals("")) {
            attrIndex = attributes.getIndex("", "src");
        } else if (localName.equals("span") && namespaceURI.equals("") && "crossreference".equals(attributes.getValue("class"))) {
            // This assumes cross references have been handled by the CrossRefParserTransformer.
            attrIndex = attributes.getIndex("","crossRefTarget");
        } else if (localName.equals("a") && namespaceURI.equals("http://www.w3.org/2000/svg")) {
            attrIndex = attributes.getIndex("http://www.w3.org/1999/xlink", "href");                       
        } else if (localName.equals("area") && namespaceURI.equals("")) {
            attrIndex = attributes.getIndex("", "href");
        }
        

        if (attrIndex > -1) {
            // link translation can be disabled for particular links using an lt:ignore='true' attribute
            String ignoreAttr = attributes.getValue(LT_NAMESPACE, "ignore");
            if ("true".equals(ignoreAttr)) {
                // remove lt:ignore attribute
                attributes = copyAttributesExceptHrefAndOwn(attributes, -1);
            } else {
                attributes = translateLink(attributes, attrIndex);
            }
        }

        super.startElement(namespaceURI, localName, qName, attributes);
    }

    Attributes translateLink(Attributes attributes, int attrIndex) {
        String href = attributes.getValue(attrIndex);
        
        if (href != null) {
            Matcher matcher = Constants.DAISY_LINK_PATTERN.matcher(href);
            if (matcher.matches()) {
                String navigationPath = attributes.getValue(PUBLISHER_NAMESPACE, "navigationPath");

                String documentId = matcher.group(1);
                String version = matcher.group(4);

                String partLink = attributes.getValue(LT_NAMESPACE, "partLink");
                String fileName = attributes.getValue(LT_NAMESPACE, "fileName");
                fileName = fileName != null && fileName.length() != 0 ? "/" + fileName : "";

                StringBuilder path = new StringBuilder(300);
                path.append(mountPoint).append('/').append(siteConf.getName());
                if (navigationPath != null)
                    path.append(navigationPath);
                else
                    path.append('/').append(documentId);

                if (partLink != null) {
                    if (version == null)
                        version = "default";
                    path.append("/version/").append(version).append("/part/").append(partLink).append("/data").append(fileName);
                } else if (version != null) {
                    path.append("/version/").append(version);
                } else {
                    path.append(".html");
                }

                path.append(DaisyLinkUtil.getBranchLangQueryString(matcher, siteConf, documentKey.getBranchId(), documentKey.getLanguageId()));

                String fragmentIdentifier = matcher.group(7);
                if (fragmentIdentifier != null && !fragmentIdentifier.startsWith("#dsy")) {
                    fragmentIdentifier = "#dsy" + documentId + "_" + fragmentIdentifier.substring(1);
                    path.append(fragmentIdentifier);
                }

                
                AttributesImpl newAttributes = copyAttributesExceptHrefAndOwn(attributes, attrIndex);
                newAttributes.addAttribute(attributes.getURI(attrIndex), attributes.getLocalName(attrIndex), attributes.getQName(attrIndex), "CDATA", path.toString());
                attributes = newAttributes;
            } else if (href.startsWith("#") && !href.startsWith("#dsy")) {
                String newHref = "#dsy" + documentKey.getDocumentId() + "_" + href.substring(1);
                AttributesImpl newAttributes = new AttributesImpl(attributes);
                newAttributes.setAttribute(attrIndex, attributes.getURI(attrIndex), attributes.getLocalName(attrIndex), attributes.getQName(attrIndex), "CDATA", newHref);
                attributes = newAttributes;
            }
        }
        return attributes;
    }

    private AttributesImpl copyAttributesExceptHrefAndOwn(Attributes attrs, int attrIndex) {
        AttributesImpl newAttrs = new AttributesImpl();
        for (int i = 0; i < attrs.getLength(); i++) {
            if (!attrs.getURI(i).equals(LT_NAMESPACE) && !attrs.getURI(i).equals(PUBLISHER_NAMESPACE) && i != attrIndex) {
                newAttrs.addAttribute(attrs.getURI(i), attrs.getLocalName(i), attrs.getQName(i), attrs.getType(i), attrs.getValue(i));
            }
        }
        return newAttrs;
    }
}
