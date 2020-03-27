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
package org.outerj.daisy.emailnotifier.serverimpl.formatters;

import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.emailnotifier.serverimpl.MailTemplate;
import org.outerj.daisy.emailnotifier.serverimpl.MailTemplateFactory;
import org.outerj.daisy.emailnotifier.serverimpl.DocumentURLProvider;
import org.outerj.daisy.xmlutil.XmlEncodingDetector;
import org.outerx.daisy.x10.DocumentDocument;
import org.outerx.daisy.x10.LinksDocument;
import org.outerx.daisy.x10.DocumentVariantCreatedDocument;
import org.apache.xmlbeans.XmlObject;

import java.text.DateFormat;
import java.util.*;

public class DocumentVariantCreatedTemplateFactory implements MailTemplateFactory {

    private static final long INCLUDE_LIMIT = 200000;

    public MailTemplate createMailTemplate(XmlObject eventDescription, Repository repository, DocumentURLProvider urlProvider) throws Exception {
        DocumentVariantCreatedDocument documentVariantCreatedDocument = (DocumentVariantCreatedDocument)eventDescription;
        DocumentVariantCreatedDocument.DocumentVariantCreated documentVariantCreatedXml = documentVariantCreatedDocument.getDocumentVariantCreated();
        DocumentDocument.Document documentXml = documentVariantCreatedXml.getNewDocumentVariant().getDocument();

        Document document = repository.getDocument(documentXml.getId(), documentXml.getBranchId(), documentXml.getLanguageId(), false);
        Version version = document.getVersion(1);
        UserManager userManager = repository.getUserManager();
        VariantManager variantManager = repository.getVariantManager();
        RepositorySchema repositorySchema = repository.getRepositorySchema();
        DocumentType documentType = repositorySchema.getDocumentTypeById(documentXml.getTypeId(), false);

        DocumentVariantCreatedMailTemplate template = new DocumentVariantCreatedMailTemplate();
        template.repository = repository;
        template.docId = documentXml.getId();
        template.branch = TemplateUtil.getBranchName(documentXml.getBranchId(), variantManager);
        template.language = TemplateUtil.getLanguageName(documentXml.getLanguageId(), variantManager);
        template.docName = documentXml.getName();
        template.docType = documentType;
        template.created = documentXml.getVariantLastModified().getTime();
        template.creator = TemplateUtil.getUserName(documentXml.getVariantLastModifier(), userManager);
        template.state = documentXml.getNewVersionState().toString();
        template.url = urlProvider.getURL(document);

        Part[] parts = version.getPartsInOrder().getArray();
        for (int i = 0; i < parts.length; i++) {
            PartInfo partInfo = new PartInfo();
            partInfo.partType = repositorySchema.getPartTypeById(parts[i].getTypeId(), false);
            String mimeType = parts[i].getMimeType();
            partInfo.mimeType = mimeType;
            partInfo.size = parts[i].getSize();

            if (mimeType.startsWith("text/") && parts[i].getSize() < INCLUDE_LIMIT) {
                byte[] data = parts[i].getData();
                String encoding = null;
                if (mimeType.equals("text/xml")) {
                    encoding = XmlEncodingDetector.detectEncoding(data);
                }
                partInfo.content = encoding != null ? new String(data, encoding) : new String(data);
            }
            template.addPart(partInfo);
        }

        LinksDocument.Links.Link[] links = documentXml.getLinks().getLinkArray();
        for (int i = 0; i < links.length; i++) {
            LinkInfo link = new LinkInfo();
            link.title = links[i].getTitle();
            link.target = links[i].getTarget();
            template.addLink(link);
        }

        DocumentDocument.Document.CustomFields.CustomField[] customFields = documentXml.getCustomFields().getCustomFieldArray();
        for (int i = 0; i < customFields.length; i++) {
            CustomFieldInfo customFieldInfo = new CustomFieldInfo();
            customFieldInfo.name = customFields[i].getName();
            customFieldInfo.value = customFields[i].getValue();
            template.addCustomField(customFieldInfo);
        }

        Field[] fields = version.getFieldsInOrder().getArray();
        for (int i = 0; i < fields.length; i++) {
            FieldInfo fieldInfo = new FieldInfo();
            fieldInfo.fieldType = repositorySchema.getFieldTypeById(fields[i].getTypeId(), false);
            fieldInfo.value = fields[i].getValue();
            fieldInfo.valueType = fields[i].getValueType();
            template.addField(fieldInfo);
        }


        long[] collectionIds = documentXml.getCollectionIds().getCollectionIdArray();
        CollectionManager collectionManager = repository.getCollectionManager();
        for (int i = 0; i < collectionIds.length; i++) {
            CollectionInfo collectionInfo = new CollectionInfo();
            collectionInfo.name = collectionManager.getCollection(collectionIds[i], false).getName();
            template.addCollection(collectionInfo);
        }

        return template;
    }

    static class DocumentVariantCreatedMailTemplate implements MailTemplate
    {
        private Map cachedByLocale = new HashMap();
        private Repository repository;
        public String subject;
        public String docId;
        public String branch;
        public String language;
        public String docName;
        public DocumentType docType;
        public Date created;
        public String creator;
        public String state;
        public List parts;
        public List links;
        public List fields;
        public List customFields;
        public List collections;
        public String url;

        private ResourceBundle getBundle(Locale locale) {
            return ResourceBundle.getBundle("org/outerj/daisy/emailnotifier/serverimpl/formatters/messages", locale);
        }

        public String getSubject(Locale locale) {
            ResourceBundle bundle = getBundle(locale);
            return bundle.getString("created.subject") + " " + docName;
        }

        public String getMessage(Locale locale) {
            String message = (String)cachedByLocale.get(locale);
            if (message == null) {
                ResourceBundle bundle = getBundle(locale);
                DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);

                StringBuilder buffer = new StringBuilder();
                buffer.append(bundle.getString("created.intro")).append('\n');
                if (url != null) {
                    buffer.append('\n').append(url).append('\n');
                }
                buffer.append('\n').append(bundle.getString("common.id")).append(": ").append(docId);
                buffer.append('\n').append(bundle.getString("common.branch")).append(": ").append(branch);
                buffer.append('\n').append(bundle.getString("common.language")).append(": ").append(language);
                buffer.append('\n').append(bundle.getString("common.name")).append(": ").append(docName);
                buffer.append('\n').append(bundle.getString("common.document-type")).append(": ").append(docType.getLabel(locale));
                buffer.append('\n').append(bundle.getString("created.created")).append(": ").append(dateFormat.format(created));
                buffer.append('\n').append(bundle.getString("created.creator")).append(": ").append(creator);
                buffer.append('\n').append(bundle.getString("created.state")).append(": ").append(bundle.getString("state." + state));

                if (parts != null) {
                    String title = bundle.getString("common.parts-title");
                    buffer.append("\n\n").append(title);
                    buffer.append('\n').append(TemplateUtil.repeatChar('=', title.length()));
                    Iterator partIt = parts.iterator();
                    while (partIt.hasNext()) {
                        PartInfo partInfo = (PartInfo)partIt.next();
                        String label = partInfo.partType.getLabel(locale);
                        buffer.append("\n\n");
                        buffer.append(label);
                        buffer.append("\n");
                        buffer.append(TemplateUtil.repeatChar('-', label.length()));
                        buffer.append('\n').append(bundle.getString("common.part.mime-type")).append(": ").append(partInfo.mimeType);
                        buffer.append('\n').append(bundle.getString("common.part.size")).append(": ").append(partInfo.size).append(' ').append(bundle.getString("common.bytes"));
                        if (partInfo.content != null) {
                            buffer.append('\n').append(bundle.getString("common.part.content")).append(":\n");
                            buffer.append(partInfo.content);
                        }
                    }
                }

                if (links != null) {
                    String title = bundle.getString("common.links-title");
                    buffer.append("\n\n").append(title);
                    buffer.append('\n').append(TemplateUtil.repeatChar('=', title.length()));

                    Iterator linkIt = links.iterator();
                    while (linkIt.hasNext()) {
                        LinkInfo link = (LinkInfo)linkIt.next();
                        buffer.append("\n\n").append(bundle.getString("common.link.title")).append(": ").append(link.title);
                        buffer.append("\n").append(bundle.getString("common.link.target")).append(": ").append(link.target);
                    }
                }

                if (fields != null) {
                    String title = bundle.getString("common.fields-title");
                    buffer.append("\n\n").append(title);
                    buffer.append('\n').append(TemplateUtil.repeatChar('=', title.length()));

                    Iterator fieldIt = fields.iterator();
                    while (fieldIt.hasNext()) {
                        FieldInfo fieldInfo = (FieldInfo)fieldIt.next();
                        buffer.append("\n").append(fieldInfo.fieldType.getLabel(locale)).append(": ");
                        buffer.append(FieldHelper.getFormattedValue(fieldInfo.value, fieldInfo.valueType, locale, repository));

                    }
                }

                if (customFields != null) {
                    String title = bundle.getString("common.customfields-title");
                    buffer.append("\n\n").append(title);
                    buffer.append('\n').append(TemplateUtil.repeatChar('=', title.length()));

                    Iterator customFieldIt = customFields.iterator();
                    while (customFieldIt.hasNext()) {
                        CustomFieldInfo customFieldInfo = (CustomFieldInfo)customFieldIt.next();
                        buffer.append("\n").append(customFieldInfo.name).append(": ").append(customFieldInfo.value);
                    }
                }

                if (collections != null) {
                    String title = bundle.getString("common.collections-title");
                    buffer.append("\n\n").append(title);
                    buffer.append('\n').append(TemplateUtil.repeatChar('=', title.length()));
                    buffer.append('\n').append(bundle.getString("created.collections-intro")).append(' ');

                    Iterator collectionIt = collections.iterator();
                    boolean first = true;
                    while (collectionIt.hasNext()) {
                        CollectionInfo collectionInfo = (CollectionInfo)collectionIt.next();
                        if (!first)
                            buffer.append(", ");
                        buffer.append(collectionInfo.name);
                        first = false;
                    }
                }

                message = buffer.toString();
                cachedByLocale.put(locale, message);
            }
            return message;
        }

        public void addPart(PartInfo partInfo) {
            if (parts == null)
                parts = new ArrayList();
            parts.add(partInfo);
        }

        public void addLink(LinkInfo linkInfo) {
            if (links == null)
                links = new ArrayList();
            links.add(linkInfo);
        }

        public void addField(FieldInfo fieldInfo) {
            if (fields == null)
                fields = new ArrayList();
            fields.add(fieldInfo);
        }

        public void addCustomField(CustomFieldInfo customFieldInfo) {
            if (customFields == null)
                customFields = new ArrayList();
            customFields.add(customFieldInfo);
        }

        public void addCollection(CollectionInfo collectionInfo) {
            if (collections == null)
                collections = new ArrayList();
            collections.add(collectionInfo);
        }
    }

    static class PartInfo {
        public PartType partType;
        public String mimeType;
        public long size;
        public String content;
    }

    static class LinkInfo {
        public String title;
        public String target;
    }

    static class FieldInfo {
        public FieldType fieldType;
        public Object value;
        public ValueType valueType;
    }

    static class CustomFieldInfo {
        public String name;
        public String value;
    }

    static class CollectionInfo {
        public String name;
    }

}
