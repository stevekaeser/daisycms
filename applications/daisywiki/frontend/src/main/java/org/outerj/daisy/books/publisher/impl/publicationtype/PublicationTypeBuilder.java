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
package org.outerj.daisy.books.publisher.impl.publicationtype;

import org.outerx.daisy.x10Bookpubtype.PublicationTypeDocument;
import org.outerj.daisy.books.publisher.impl.publicationprocess.PublicationProcessBuilder;
import org.outerj.daisy.books.publisher.impl.publicationprocess.PublicationProcess;
import org.outerj.daisy.books.publisher.impl.util.XMLPropertiesHelper;
import org.outerj.daisy.books.publisher.impl.util.CustomImplementationHelper;
import org.outerj.daisy.books.publisher.impl.dataretrieval.PartDecider;
import org.outerj.daisy.books.store.impl.XmlUtil;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.excalibur.source.SourceResolver;
import org.apache.excalibur.source.Source;
import org.apache.xmlbeans.XmlOptions;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.util.*;

public class PublicationTypeBuilder {
    public static PublicationType build(String publicationTypeName, ServiceManager serviceManager) throws Exception {
        SourceResolver sourceResolver = null;
        Source source = null;
        Source commonPropertiesSource = null;
        Source propertiesSource = null;
        try {
            sourceResolver = (SourceResolver)serviceManager.lookup(SourceResolver.ROLE);

            commonPropertiesSource = sourceResolver.resolveURI("wikidata:/books/publicationtypes/common/properties.xml");
            Map<String, String> commonProperties = loadProperties(commonPropertiesSource, null);

            propertiesSource = sourceResolver.resolveURI("wikidata:/books/publicationtypes/" + publicationTypeName + "/properties.xml");
            Map<String, String> properties = loadProperties(propertiesSource, commonProperties);

            source = sourceResolver.resolveURI("wikidata:/books/publicationtypes/" + publicationTypeName + "/publicationtype.xml");

            return build(publicationTypeName, source.getInputStream(), properties);
        } finally {
            if (source != null)
                sourceResolver.release(source);
            if (propertiesSource != null)
                sourceResolver.release(propertiesSource);
            if (commonPropertiesSource != null)
                sourceResolver.release(commonPropertiesSource);
            if (sourceResolver != null)
                serviceManager.release(sourceResolver);
        }
    }

    public static Map<String, String> loadProperties(String publicationTypeName, ServiceManager serviceManager) throws Exception {
        SourceResolver sourceResolver = null;
        Source publicationTypeSource = null;
        Source commonPropertiesSource = null;
        Source propertiesSource = null;
        try {
            sourceResolver = (SourceResolver)serviceManager.lookup(SourceResolver.ROLE);

            // Check the publication type exist
            publicationTypeSource = sourceResolver.resolveURI("wikidata:/books/publicationtypes/" + publicationTypeName + "/publicationtype.xml");
            if (!publicationTypeSource.exists())
                throw new Exception("A publication type named \"" + publicationTypeName + "\" does not exist.");

            commonPropertiesSource = sourceResolver.resolveURI("wikidata:/books/publicationtypes/common/properties.xml");
            Map<String, String> commonProperties = loadProperties(commonPropertiesSource, null);

            propertiesSource = sourceResolver.resolveURI("wikidata:/books/publicationtypes/" + publicationTypeName + "/properties.xml");
            return loadProperties(propertiesSource, commonProperties);
        } finally {
            if (publicationTypeSource != null)
                sourceResolver.release(publicationTypeSource);
            if (propertiesSource != null)
                sourceResolver.release(propertiesSource);
            if (commonPropertiesSource != null)
                sourceResolver.release(commonPropertiesSource);
            if (sourceResolver != null)
                serviceManager.release(sourceResolver);
        }
    }

    private static Map<String, String> loadProperties(Source source, Map<String, String> defaults) throws Exception {
        Map<String, String> properties;
        if (source.exists()) {
            InputStream propertiesIs = null;
            try {
                propertiesIs = new BufferedInputStream(source.getInputStream());
                properties = XMLPropertiesHelper.load(propertiesIs, defaults);
            } finally {
                if (propertiesIs != null)
                    propertiesIs.close();
            }
        } else {
            if (defaults != null) {
                properties = new LinkedHashMap<String, String>();
                properties.putAll(defaults);
            } else {
                return new LinkedHashMap<String, String>();
            }
        }
        return properties;
    }

    public static PublicationType build(String publicationTypeName, InputStream is, Map<String, String> properties) throws Exception {
        PublicationTypeDocument publicationTypeDocument;
        try {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            publicationTypeDocument = PublicationTypeDocument.Factory.parse(is, xmlOptions);
        } finally {
            is.close();
        }
        return build(publicationTypeName, publicationTypeDocument.getPublicationType(), properties);
    }

    public static PublicationType build(String publicationTypeName, PublicationTypeDocument.PublicationType publicationTypeXml, Map<String, String> properties) throws Exception {
        String errors = XmlUtil.validate(publicationTypeXml);
        if (errors != null)
            throw new Exception("The publication type XML is not valid according to its XML Schema, encountered errors: " + errors);

        PublicationProcess publicationProcess = PublicationProcessBuilder.build(publicationTypeXml.getPublicationProcess());

        PublicationTypeDocument.PublicationType.RequiredParts requiredPartsXml = publicationTypeXml.getRequiredParts();
        PartDecider partDecider;
        if (requiredPartsXml != null) {
            Class partDeciderClass = DefaultPartDecider.class;
            if (requiredPartsXml.isSetClass1()) {
                try {
                    partDeciderClass = PublicationTypeBuilder.class.getClassLoader().loadClass(requiredPartsXml.getClass1());
                } catch (ClassNotFoundException e) {
                    throw new Exception("Publication type: specified class for requiredParts not found: " + requiredPartsXml.getClass1(), e);
                }
                if (!PartDecider.class.isAssignableFrom(partDeciderClass)) {
                    throw new Exception("Publication type: specified class does for requiredParts not implement PartDecider interface: " + requiredPartsXml.getClass1());
                }
            }
            partDecider = (PartDecider)CustomImplementationHelper.instantiateComponent(partDeciderClass, requiredPartsXml);
        } else {
            partDecider = new DefaultPartDecider(Collections.EMPTY_MAP);
        }

        PublicationType publicationType = new PublicationType(publicationTypeName, publicationTypeXml.getLabel(),
                publicationTypeXml.getStartResource(), publicationProcess, partDecider, properties);
        return publicationType;
    }
}
