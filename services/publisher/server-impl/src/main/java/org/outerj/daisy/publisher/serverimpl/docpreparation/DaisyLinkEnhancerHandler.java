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
package org.outerj.daisy.publisher.serverimpl.docpreparation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.outerj.daisy.navigation.NavigationManager;
import org.outerj.daisy.publisher.serverimpl.PublisherImpl;
import org.outerj.daisy.publisher.serverimpl.requestmodel.PublisherContext;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.FieldHelper;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.repository.query.ValueExpression;
import org.outerj.daisy.util.Constants;
import org.outerj.daisy.xmlutil.SaxBuffer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Adds the document name as an attribute on links pointing to daisy documents.
 */
public class DaisyLinkEnhancerHandler implements ContentHandler {
    private ContentHandler consumer;
    private Repository repository;
    private VariantKey navigationDoc;
    private PublisherContext publisherContext;
    private VersionMode versionMode;
    private VersionMode navVersionMode;
    private NavigationManager navigationManager;
    private Log logger;
    private long documentBranchId;
    private long documentLanguageId;
    private long imageWidthFieldId = -1;
    private long imageHeightFieldId = -1;
    private Map<QName, LinkAnnotationConfig> linkAnnotationConfig;
    private static Map<QName, LinkAnnotationConfig> DEFAULT_ANNOTATIONS = new HashMap<QName, LinkAnnotationConfig>();
    static {
        DEFAULT_ANNOTATIONS.put(new QName("a"), new LinkAnnotationConfig(new QName("href"), true, false, Collections.EMPTY_MAP));
        DEFAULT_ANNOTATIONS.put(new QName("img"), new LinkAnnotationConfig(new QName("src"), false, true, Collections.EMPTY_MAP));
        DEFAULT_ANNOTATIONS.put(new QName(Constants.DAISY_NAMESPACE, "link"), new LinkAnnotationConfig(new QName("target"), true, false, Collections.EMPTY_MAP));
    }

    public DaisyLinkEnhancerHandler(long documentBranchId, long documentLanguageId,
            Map<QName, LinkAnnotationConfig> linkAnnotationConfig, PublisherContext publisherContext,
            ContentHandler consumer) {
        this.repository = publisherContext.getRepository();
        this.consumer = consumer;
        this.publisherContext = publisherContext;
        this.navigationDoc = publisherContext.getPreparedDocuments().getNavigationDoc();
        this.versionMode = publisherContext.getVersionMode();
        this.navVersionMode = versionMode == null ? VersionMode.LIVE : versionMode;
        this.navigationManager = (NavigationManager)repository.getExtension("NavigationManager");
        this.logger = publisherContext.getLogger();
        this.documentBranchId = documentBranchId;
        this.documentLanguageId = documentLanguageId;
        this.linkAnnotationConfig = linkAnnotationConfig;
    }

    public void startElement (String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        boolean forwardStartElement = true;
        LinkAnnotationConfig config = getLinkAnnotationConfig(namespaceURI, localName);
        if (config != null) {
            String href = atts.getValue(config.getAttribute().getNamespaceURI(), config.getAttribute().getLocalPart());
            if (href != null) {
                Matcher matcher = Constants.DAISY_LINK_PATTERN.matcher(href);
                if (matcher.matches()) {
                    String navigationPath = null;
                    Version version = null;

                    Document document = null;
                    String documentId;
                    AclResultInfo aclInfo = null;
                    try {
                        documentId = matcher.group(1);
                        String branch = matcher.group(2);
                        if (branch == null || branch.equals(""))
                            branch = String.valueOf(documentBranchId);
                        String language = matcher.group(3);
                        if (language == null || language.equals(""))
                            language = String.valueOf(documentLanguageId);
                        document = repository.getDocument(documentId, branch, language, false);
                        aclInfo = repository.getAccessManager().getAclInfoOnLive(repository.getUserId(), repository.getActiveRoleIds(), document.getVariantKey());
                    } catch (Exception e) {
                        // ignore (invalid document id, non existing document, no permission, ...)
                    }

                    String versionSpec = null;
                    if (document != null) {
                        versionSpec = matcher.group(4);
                        try {
                            if (versionSpec != null) {
                                try {
                                    VersionMode versionMode = VersionMode.get(versionSpec);
                                    version = document.getVersion(versionMode);
                                } catch (IllegalArgumentException iae) {
                                    version = document.getVersion(Long.parseLong(versionSpec));
                                }
                            } else {
                                version = document.getVersion(publisherContext.getVersionMode());
                            }
                        } catch (Exception e) {
                            // ignore exceptions that happen when retrieving version
                        }
                    }

                    if (config.navigationPath() && navigationDoc != null && document != null) {
                        try {
                            navigationPath = navigationManager.reverseLookup(document.getVariantKey(), navigationDoc, navVersionMode, true);
                        } catch (RepositoryException e) {
                            logger.error("Error performing reverse document lookup for navigation tree " + navigationDoc + " and document " + document.getVariantKey(), e);
                        }
                    }

                    if (version != null) {
                        AttributesImpl newAttrs = new AttributesImpl(atts);
                        if (navigationPath != null)
                            newAttrs.addAttribute(PublisherImpl.NAMESPACE, "navigationPath", "p:navigationPath", "CDATA", navigationPath);

                        if (config.imageAnnotations()) {
                            addImageAttributes(newAttrs, version);
                        }

                        consumer.startElement(namespaceURI, localName, qName, newAttrs);
                        forwardStartElement = false;

                        SaxBuffer linkInfoBuffer = new SaxBuffer();
                        try {
                            AttributesImpl linkInfoAttrs = new AttributesImpl(atts);
                            String documentName = version.getDocumentName();
                            String resolvedDocumentName = publisherContext.resolveVariables(documentName);
                            documentName = resolvedDocumentName != null ? resolvedDocumentName : documentName;
                            linkInfoAttrs.addAttribute("", "documentName", "documentName", "CDATA", documentName);
                            String documentTypeName = repository.getRepositorySchema().getDocumentTypeById(document.getDocumentTypeId(), false).getName();
                            linkInfoAttrs.addAttribute("", "documentType", "documentType", "CDATA", documentTypeName);
                            linkInfoAttrs.addAttribute("", "lastVersionId", "lastVersionId", "CDATA", String.valueOf(document.getLastVersionId()));
                            linkInfoAttrs.addAttribute("", "liveVersionId", "liveVersionId", "CDATA", String.valueOf(document.getLiveVersionId()));
                            if (versionSpec != null) {
                                linkInfoAttrs.addAttribute("", "versionSpec", "versionSpec", "CDATA", versionSpec);
                            }
                            if (aclInfo != null)
                                linkInfoAttrs.addAttribute("", "access", "access", "CDATA", aclInfo.getCompactString());

                            linkInfoBuffer.startElement(PublisherImpl.NAMESPACE, "linkInfo", "p:linkInfo", linkInfoAttrs);
                            Part[] parts = version.getParts().getArray();
                            for (Part part : parts) {
                                AttributesImpl partInfoAttrs = new AttributesImpl();
                                partInfoAttrs.addAttribute("", "id", "id", "CDATA", String.valueOf(part.getTypeId()));
                                partInfoAttrs.addAttribute("", "name", "name", "CDATA", part.getTypeName());
                                partInfoAttrs.addAttribute("", "mimeType", "mimeType", "CDATA", part.getMimeType());
                                partInfoAttrs.addAttribute("", "size", "size", "CDATA", String.valueOf(part.getSize()));
                                if (part.getFileName() != null)
                                    partInfoAttrs.addAttribute("", "fileName", "fileName", "CDATA", part.getFileName());
                                linkInfoBuffer.startElement(PublisherImpl.NAMESPACE, "linkPartInfo", "p:linkPartInfo", partInfoAttrs);
                                linkInfoBuffer.endElement(PublisherImpl.NAMESPACE, "linkPartInfo", "p:linkPartInfo");
                            }
                            
                            addCustomAnnotations(document, version, config.getCustomAnnotations(), linkInfoBuffer);
                            linkInfoBuffer.endElement(PublisherImpl.NAMESPACE, "linkInfo", "p:linkInfo");
                        } catch (Throwable e) {
                            linkInfoBuffer = null;
                        }
                        if (linkInfoBuffer != null)
                            linkInfoBuffer.toSAX(consumer);
                    }
                }
            }
        }

        if (forwardStartElement)
            consumer.startElement(namespaceURI, localName, qName, atts);
    }

    private void addImageAttributes(AttributesImpl attrs, Version version) {
        // If the ID and existence of the ImageWidth and ImageHeight fields has not yet been determined
        if (imageWidthFieldId == -1 || imageHeightFieldId == -1) {
            try {
                imageWidthFieldId = repository.getRepositorySchema().getFieldTypeByName("ImageWidth", false).getId();
                imageHeightFieldId = repository.getRepositorySchema().getFieldTypeByName("ImageHeight", false).getId();
            } catch (Throwable e) {
                // ignore this error, sets fields to -2 to avoid trying the same for every image
                imageWidthFieldId = -2;
                imageHeightFieldId = -2;
            }
        }

        // If the ImageWidth and ImageHeight fields have been successfully found
        if (imageWidthFieldId > 0 && imageHeightFieldId > 0) {
            if (version.hasField(imageWidthFieldId))
                attrs.addAttribute(PublisherImpl.NAMESPACE, "imageWidth", "p:imageWidth", "CDATA", version.getField(imageWidthFieldId).getValue().toString());
            if (version.hasField(imageHeightFieldId))
                attrs.addAttribute(PublisherImpl.NAMESPACE, "imageHeight", "p:imageHeight", "CDATA", version.getField(imageHeightFieldId).getValue().toString());
        }
    }
    
    private void addCustomAnnotations(Document document, Version version, Map<String, ValueExpression> customAnnotations, SaxBuffer linkInfoBuffer) throws SAXException {

        for (String name: customAnnotations.keySet()) {
            try {
                ValueExpression expr = customAnnotations.get(name);
                Object value = expr.evaluate(document, version);
                if (value != null) {
                    ValueType valueType = expr.getValueType();
                    String formattedValue= FieldHelper.getFormattedValue(value, valueType, publisherContext.getLocale(), repository);
                    AttributesImpl atts = new AttributesImpl();
                    atts.addAttribute(PublisherImpl.NAMESPACE, "name", "p:name", "CDATA", name);
                    atts.addAttribute(PublisherImpl.NAMESPACE, "value", "p:value", "CDATA", formattedValue);
                    linkInfoBuffer.startElement(PublisherImpl.NAMESPACE, "customAnnotation", "p:customAnnotation", atts);
                    linkInfoBuffer.endElement(PublisherImpl.NAMESPACE, "customAnnotation", "p:customAnnotation");
                }
            } catch (QueryException qe) {
                throw new RuntimeException("TODO: handle me", qe);
            }
        }

    }

    private LinkAnnotationConfig getLinkAnnotationConfig(String namespaceURI, String localName) {
        QName qname = new QName(namespaceURI.equals("") ? XMLConstants.NULL_NS_URI : namespaceURI, localName);
        LinkAnnotationConfig config = linkAnnotationConfig.get(qname);
        if (config != null)
            return config;

        config = DEFAULT_ANNOTATIONS.get(qname);
        return config;
    }

    public void endDocument() throws SAXException {
        consumer.endDocument();
    }

    public void startDocument () throws SAXException {
        consumer.startDocument();
    }

    public void characters (char ch[], int start, int length) throws SAXException {
        consumer.characters(ch, start, length);
    }

    public void ignorableWhitespace (char ch[], int start, int length) throws SAXException {
        consumer.ignorableWhitespace(ch, start, length);
    }

    public void endPrefixMapping (String prefix) throws SAXException {
        consumer.endPrefixMapping(prefix);
    }

    public void skippedEntity (String name) throws SAXException {
        consumer.skippedEntity(name);
    }

    public void setDocumentLocator (Locator locator) {
        consumer.setDocumentLocator(locator);
    }

    public void processingInstruction (String target, String data) throws SAXException {
        consumer.processingInstruction(target, data);
    }

    public void startPrefixMapping (String prefix, String uri) throws SAXException {
        consumer.startPrefixMapping(prefix, uri);
    }

    public void endElement (String namespaceURI, String localName, String qName) throws SAXException {
        consumer.endElement(namespaceURI, localName, qName);
    }

}
