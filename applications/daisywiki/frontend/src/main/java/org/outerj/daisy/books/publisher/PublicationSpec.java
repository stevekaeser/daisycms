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
package org.outerj.daisy.books.publisher;

import org.outerx.daisy.x10Bookpubspecs.PublicationSpecsDocument;

import java.util.Map;

public class PublicationSpec {
    private final String publicationTypeName;
    private final String publicationOutputName;
    private final String publicationOutputLabel;
    private final Map<String, String> publicationProperties;

    public PublicationSpec(String publicationTypeName, String publicationOutputName, String publicationOutputLabel, Map<String, String> properties) {
        if (publicationTypeName == null)
            throw new IllegalArgumentException("publicationTypeName param can not be null");
        if (publicationOutputName == null)
            throw new IllegalArgumentException("publicationOutputName param can not be null");
        if (properties == null)
            throw new IllegalArgumentException("properties param can not be null");

        this.publicationTypeName = publicationTypeName;
        this.publicationOutputName = publicationOutputName;
        this.publicationOutputLabel = publicationOutputLabel;
        this.publicationProperties = properties;
    }

    public String getPublicationTypeName() {
        return publicationTypeName;
    }

    public String getPublicationOutputName() {
        return publicationOutputName;
    }

    public String getPublicationOutputLabel() {
        return publicationOutputLabel;
    }

    public Map<String, String> getPublicationProperties() {
        return publicationProperties;
    }

    public PublicationSpecsDocument.PublicationSpecs.PublicationSpec getXml() {
        PublicationSpecsDocument.PublicationSpecs.PublicationSpec pubSpecXml = PublicationSpecsDocument.PublicationSpecs.PublicationSpec.Factory.newInstance();
        pubSpecXml.setName(publicationOutputName);
        pubSpecXml.setLabel(publicationOutputLabel);
        pubSpecXml.setType(publicationTypeName);

        Map.Entry[] entries = publicationProperties.entrySet().toArray(new Map.Entry[publicationProperties.size()]);
        PublicationSpecsDocument.PublicationSpecs.PublicationSpec.Properties.Entry[] entriesXml =
                new PublicationSpecsDocument.PublicationSpecs.PublicationSpec.Properties.Entry[entries.length];
        for (int i = 0; i < entries.length; i++) {
            entriesXml[i] = PublicationSpecsDocument.PublicationSpecs.PublicationSpec.Properties.Entry.Factory.newInstance();
            entriesXml[i].setKey((String)entries[i].getKey());
            entriesXml[i].setStringValue((String)entries[i].getValue());
        }

        pubSpecXml.addNewProperties().setEntryArray(entriesXml);

        return pubSpecXml;
    }

    public static PublicationSpecsDocument getXml(PublicationSpec[] specs) {
        PublicationSpecsDocument.PublicationSpecs.PublicationSpec[] specsXml = new PublicationSpecsDocument.PublicationSpecs.PublicationSpec[specs.length];
        for (int i = 0; i < specsXml.length; i++) {
            specsXml[i] = specs[i].getXml();
        }

        PublicationSpecsDocument doc = PublicationSpecsDocument.Factory.newInstance();
        doc.addNewPublicationSpecs().setPublicationSpecArray(specsXml);
        return doc;
    }
}
