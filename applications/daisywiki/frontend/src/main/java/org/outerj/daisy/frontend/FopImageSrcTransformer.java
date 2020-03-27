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
import org.apache.avalon.framework.parameters.Parameters;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.outerj.daisy.util.Constants;
import org.outerj.daisy.repository.Repository;

import java.util.Map;
import java.util.regex.Matcher;
import java.io.IOException;

/**
 * Translate img/@src attributes containing "daisy:" references to "URLs" usable
 * by the daisy source ({@link org.outerj.daisy.frontend.components.daisysource.DaisySourceFactory}.
 *
 * When not logged in as guest, the user ID is also encoded in the URL to avoid
 * that FOP reuses cached images for users who have no access to them.
 */
public class FopImageSrcTransformer extends AbstractTransformer {
    private long documentBranchId;
    private long documentLanguageId;
    private Repository repository;
    
    private static final String LT_NAMESPACE = "http://outerx.org/daisy/1.0#linktransformer";

    public void setup(SourceResolver sourceResolver, Map objectModel, String s, Parameters parameters) throws ProcessingException, SAXException, IOException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        try {
            repository = FrontEndContext.get(request).getRepository();
        } catch (Exception e) {
            throw new ProcessingException("Error getting Daisy repository instance.", e);
        }
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes attributes) throws SAXException {
        if (namespaceURI.equals(Constants.DAISY_NAMESPACE) && localName.equals("document")) {
            documentBranchId = Long.parseLong(attributes.getValue("branchId"));
            documentLanguageId = Long.parseLong(attributes.getValue("languageId"));
        } else if (namespaceURI.equals("") && localName.equals("img")) {
            String src = attributes.getValue("src");
            if (src != null) {
                Matcher matcher = Constants.DAISY_LINK_PATTERN.matcher(src);
                if (matcher.matches()) {
                    String documentId = matcher.group(1);
                    String versionSpec = matcher.group(4);
                    if (versionSpec == null)
                        versionSpec = "default";

                    String branch = matcher.group(2);
                    branch = branch != null ? branch : String.valueOf(documentBranchId);
                    String language = matcher.group(3);
                    language = language != null ? language : String.valueOf(documentLanguageId);
                    String newSrc = "daisy:" + documentId + "@" + branch + ":" + language + ":" + versionSpec + "!ImageData";

                    if (!repository.getUserLogin().equals("guest")) {
                        // If we're not logged in as guest, make the user ID part of the URL since otherwise
                        // FOP might used cached copies of the image for other users, who might not have access
                        // rights to the image
                        newSrc += "?fopUserId=" + repository.getUserId();
                    }

                    AttributesImpl newAttrs = new AttributesImpl(attributes);
                    newAttrs.setAttribute(attributes.getIndex("src"), "", "src", "src", "CDATA", newSrc);
                    // since there is a link transformer in the pipeline make sure that the src attribute doesn't get transformed since FOP understands the daisy protocol now
                    newAttrs.addAttribute(LT_NAMESPACE, "ignore", "lt:ignore", "CDATA", "true");
                    attributes = newAttrs;
                }
            }
        }
        super.startElement(namespaceURI, localName, qName, attributes);
    }
}
