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
package org.outerj.daisy.books.publisher.impl.publicationprocess;

import org.outerj.daisy.util.Constants;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.books.publisher.impl.BookInstanceLayout;
import org.outerx.daisy.x10Bookstoremeta.ResourcePropertiesDocument;
import org.apache.xmlbeans.XmlOptions;

import java.util.regex.Matcher;
import java.io.InputStream;

public class GetDocumentPartTask implements PublicationProcessTask {
    private final String propertyName;
    private final String propertyOrigin;
    private final String partName;
    private final String saveAs;
    private final String setProperty;

    public GetDocumentPartTask(String propertyName, String propertyOrigin, String partName, String saveAs, String setProperty) {
        this.propertyName = propertyName;
        this.propertyOrigin = propertyOrigin;
        this.partName = partName;
        this.saveAs = saveAs;
        this.setProperty = setProperty;
    }

    public void run(PublicationContext context) throws Exception {
        String propertyValue;
        if (propertyOrigin.equals("metadata")) {
            propertyValue = context.getBookMetadata().get(propertyName);
        } else if (propertyOrigin.equals("publication")) {
            propertyValue = context.getProperties().get(propertyName);
        } else {
            context.getPublicationLog().info("GetDocumentPart: property origin \"" + propertyOrigin + "\" is invalid. It should be either 'metadata' or 'publication' (without the quotes).");
            return;
        }


        if (propertyValue == null || propertyValue.length() == 0) {
            context.getPublicationLog().info("GetDocumentPart: no property named \"" + propertyName + "\" found in " + propertyOrigin);
            return;
        }

        Matcher matcher = Constants.DAISY_LINK_PATTERN.matcher(propertyValue);
        if (!matcher.matches()) {
            context.getPublicationLog().info("GetDocumentPart: property named \"" + propertyName + "\" in " + propertyOrigin + " does not contain a daisy: link but \"" + propertyValue + "\".");
            return;
        }

        String documentId = matcher.group(1);
        String branch = matcher.group(2);
        if (branch == null || branch.trim().length() == 0)
            branch = "1";
        String language = matcher.group(3);
        if (language == null || language.trim().length() == 0)
            language = "1";

        try {
            Version version = context.getRepository().getDocument(documentId, branch, language, false).getLiveVersion();
            if (!version.hasPart(partName)) {
                context.getPublicationLog().info("GetDocumentPart: document " + documentId + " as refered to in property \"" + propertyName + "\" does not have a part called \"" + partName + "\".");
                return;
            }

            String savePath = BookInstanceLayout.getPublicationOutputPath(context.getPublicationOutputName()) + saveAs;
            String mimeType = version.getPart(partName).getMimeType();

            InputStream is = null;
            try {
                is = version.getPart(partName).getDataStream();
                context.getBookInstance().storeResource(savePath, is);
            } finally {
                if (is != null)
                    is.close();
            }

            // save metadata with mime type
            ResourcePropertiesDocument propertiesDocument = ResourcePropertiesDocument.Factory.newInstance();
            propertiesDocument.addNewResourceProperties().setMimeType(mimeType);
            XmlOptions xmlOptions = new XmlOptions();
            xmlOptions.setSavePrettyPrint();
            xmlOptions.setUseDefaultNamespace();
            context.getBookInstance().storeResource(savePath + ".meta.xml", propertiesDocument.newInputStream());

            context.getPublicationLog().info("GetDocumentPart: resource " + propertyValue + " successfully saved to " + savePath);

            if (setProperty != null)
                context.getProperties().put(setProperty, "true");
        } catch (Throwable e) {
            context.getPublicationLog().error("GetDocumentPart: error retrieving resource " + propertyValue, e);            
        }
    }
}
