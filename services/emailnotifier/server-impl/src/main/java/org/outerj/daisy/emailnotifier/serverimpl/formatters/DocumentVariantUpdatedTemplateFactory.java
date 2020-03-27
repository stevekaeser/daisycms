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

import org.outerj.daisy.emailnotifier.serverimpl.MailTemplateFactory;
import org.outerj.daisy.emailnotifier.serverimpl.MailTemplate;
import org.outerj.daisy.emailnotifier.serverimpl.DocumentURLProvider;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.xmlutil.XmlEncodingDetector;
import org.outerj.daisy.docdiff.DocDiffOutputHelper;
import org.outerj.daisy.docdiff.DiffGenerator;
import org.outerj.daisy.docdiff.DocDiffOutput;
import org.outerj.daisy.diff.TextDiffOutput;
import org.outerj.daisy.diff.Diff;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlObject;
import org.outerx.daisy.x10.*;

import java.util.*;
import java.text.DateFormat;
import java.io.StringWriter;

public class DocumentVariantUpdatedTemplateFactory implements MailTemplateFactory {
    private static final long INCLUDE_LIMIT = 200000;

    public MailTemplate createMailTemplate(XmlObject eventDescription, Repository repository, DocumentURLProvider urlProvider) throws Exception {
        DocumentVariantUpdatedDocument documentVariantUpdatedDocument = (DocumentVariantUpdatedDocument)eventDescription;
        DocumentDocument.Document oldDocumentXml = documentVariantUpdatedDocument.getDocumentVariantUpdated().getOldDocumentVariant().getDocument();
        DocumentDocument.Document newDocumentXml = documentVariantUpdatedDocument.getDocumentVariantUpdated().getNewDocumentVariant().getDocument();
        Document document = repository.getDocument(newDocumentXml.getId(), newDocumentXml.getBranchId(), newDocumentXml.getLanguageId(), false);

        DocumentVariantUpdatedMailTemplate template = new DocumentVariantUpdatedMailTemplate();
        template.repository = repository;
        RepositorySchema repositorySchema = repository.getRepositorySchema();
        UserManager userManager = repository.getUserManager();
        VariantManager variantManager = repository.getVariantManager();

        template.url = urlProvider.getURL(document);
        template.docId = newDocumentXml.getId();
        template.branch = TemplateUtil.getBranchName(newDocumentXml.getBranchId(), variantManager);
        template.language = TemplateUtil.getLanguageName(newDocumentXml.getLanguageId(), variantManager);
        template.docName = newDocumentXml.getName();
        if (!newDocumentXml.getName().equals(oldDocumentXml.getName())) {
            template.oldDocName = oldDocumentXml.getName();
        }

        template.documentType = repositorySchema.getDocumentTypeById(newDocumentXml.getTypeId(), false);
        if (oldDocumentXml.getTypeId() != newDocumentXml.getTypeId()) {
            template.oldDocumentType = repositorySchema.getDocumentTypeById(oldDocumentXml.getTypeId(), false);
        }

        template.modified = newDocumentXml.getVariantLastModified().getTime();
        template.modifier = TemplateUtil.getUserName(newDocumentXml.getVariantLastModifier(), userManager);

        if (oldDocumentXml.getLastVersionId() != newDocumentXml.getLastVersionId()) {
            template.versionCreated = true;
            template.newVersionState = newDocumentXml.getNewVersionState().toString();
            if (newDocumentXml.isSetNewSyncedWithLanguageId()) {
                template.newSyncedWith = new StringBuffer(TemplateUtil.getLanguageName(newDocumentXml.getNewSyncedWithLanguageId(),variantManager))
                .append(":").append(String.valueOf(newDocumentXml.getNewSyncedWithVersionId())).toString(); 
            }
            template.newChangeType = newDocumentXml.getNewChangeType().toString();
            template.newChangeComment = newDocumentXml.getNewChangeComment();
            Version version1 = document.getVersion(oldDocumentXml.getLastVersionId());
            Version version2 = document.getVersion(newDocumentXml.getLastVersionId());

            DocDiffOutputHelper outputHelper = new DocDiffOutputHelper(document, document, version1, version2, repository, null);
            MyDocDiffOutput diffOutput = new MyDocDiffOutput(template, outputHelper);
            DiffGenerator.generateDiff(version1, version2, diffOutput);
        }

        //
        // Calculate changes to custom fields
        //
        DocumentDocument.Document.CustomFields.CustomField[] oldCustomFields = oldDocumentXml.getCustomFields().getCustomFieldArray();
        DocumentDocument.Document.CustomFields.CustomField[] newCustomFields = newDocumentXml.getCustomFields().getCustomFieldArray();

        for (int i = 0; i < oldCustomFields.length; i++) {
            String name = oldCustomFields[i].getName();
            boolean found = false;
            for (int j = 0; j < newCustomFields.length; j++) {
                if (newCustomFields[j].getName().equals(name)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                CustomFieldInfo customFieldInfo = new CustomFieldInfo();
                customFieldInfo.name = name;
                template.addRemovedCustomField(customFieldInfo);
            }
        }

        for (int i = 0; i < newCustomFields.length; i++) {
            String name = newCustomFields[i].getName();
            boolean found = false;
            int j;
            for (j = 0; j < oldCustomFields.length; j++) {
                if (oldCustomFields[j].getName().equals(name)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                CustomFieldInfo customFieldInfo = new CustomFieldInfo();
                customFieldInfo.name = name;
                customFieldInfo.value = newCustomFields[i].getValue();
                template.addAddedCustomField(customFieldInfo);
            } else {
                if (!newCustomFields[i].getValue().equals(oldCustomFields[j].getValue())) {
                    CustomFieldInfo customFieldInfo = new CustomFieldInfo();
                    customFieldInfo.name = name;
                    customFieldInfo.value = newCustomFields[i].getValue();
                    customFieldInfo.oldValue = oldCustomFields[j].getValue();
                    template.addUpdatedCustomField(customFieldInfo);
                }
            }
        }

        //
        // Calculate changes to collections
        //
        long[] oldCollections = oldDocumentXml.getCollectionIds().getCollectionIdArray();
        long[] newCollections = newDocumentXml.getCollectionIds().getCollectionIdArray();
        CollectionManager collectionManager = repository.getCollectionManager();

        for (int i = 0; i < oldCollections.length; i++) {
            long collectionId = oldCollections[i];
            boolean found = false;
            for (int j = 0; j < newCollections.length; j++) {
                if (newCollections[j] == collectionId) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                CollectionInfo collectionInfo = new CollectionInfo();
                collectionInfo.name = getCollectionName(collectionId, collectionManager);
                template.addRemovedCollection(collectionInfo);
            }
        }

        for (int i = 0; i < newCollections.length; i++) {
            long collectionId = newCollections[i];
            boolean found = false;
            for (int j = 0; j < oldCollections.length; j++) {
                if (oldCollections[j] == collectionId) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                CollectionInfo collectionInfo = new CollectionInfo();
                collectionInfo.name = getCollectionName(collectionId, collectionManager);
                template.addAddedCollection(collectionInfo);
            }
        }

        return template;
    }

    private static String getCollectionName(long collectionId, CollectionManager collectionManager) throws RepositoryException {
        try {
            return collectionManager.getCollection(collectionId, false).getName();
        } catch (CollectionNotFoundException e) {
            return String.valueOf(collectionId);
        }
    }

    static class DocumentVariantUpdatedMailTemplate implements MailTemplate {
        private Map cachedByLocale = new HashMap();
        public Repository repository;
        public String docId;
        public String docName;
        public String branch;
        public String language;
        public String oldDocName;
        public DocumentType documentType;
        public DocumentType oldDocumentType;
        public Date modified;
        public String modifier;
        public String url;

        public boolean versionCreated;
        public String newVersionState;
        public String newSyncedWith;
        public String newChangeType;
        public String newChangeComment;
        public List parts;
        public List fields;
        public List addedLinks;
        public List removedLinks;
        public List addedCustomFields;
        public List removedCustomFields;
        public List updatedCustomFields;
        public List addedCollections;
        public List removedCollections;

        private ResourceBundle getBundle(Locale locale) {
            return ResourceBundle.getBundle("org/outerj/daisy/emailnotifier/serverimpl/formatters/messages", locale);
        }

        public String getSubject(Locale locale) {
            ResourceBundle bundle = getBundle(locale);
            return bundle.getString("updated.subject") + " " + docName;
        }

        public String getMessage(Locale locale) {
            String message = (String)cachedByLocale.get(locale);
            if (message == null) {
                ResourceBundle bundle = getBundle(locale);
                StringBuilder buffer = new StringBuilder();
                DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);


                buffer.append(bundle.getString("updated.intro")).append('\n');

                if (url != null) {
                    buffer.append('\n').append(url).append('\n');
                }

                buffer.append('\n').append(bundle.getString("common.id")).append(": ").append(docId);
                buffer.append('\n').append(bundle.getString("common.branch")).append(": ").append(branch);
                buffer.append('\n').append(bundle.getString("common.language")).append(": ").append(language);
                buffer.append('\n').append(bundle.getString("common.name")).append(": ").append(docName);
                if (oldDocName == null) {
                    buffer.append(" (").append(bundle.getString("updated.unchanged")).append(')');
                } else {
                    buffer.append(" (").append(bundle.getString("updated.previously")).append(' ').append(oldDocName).append(')');
                }
                buffer.append('\n').append(bundle.getString("common.document-type")).append(": ").append(documentType.getLabel(locale));
                if (oldDocumentType == null) {
                    buffer.append(" (").append(bundle.getString("updated.unchanged")).append(")");
                } else {
                    buffer.append(" (").append(bundle.getString("updated.previously")).append(": ").append(oldDocumentType.getLabel(locale)).append(")");
                }
                buffer.append('\n').append(bundle.getString("updated.updated-on")).append(": ").append(dateFormat.format(modified))
                    .append(' ').append(bundle.getString("updated.updated-by")).append(": ").append(modifier);

                if (versionCreated) {
                    buffer.append("\n\n").append(bundle.getString("updated.version-created")).append(' ').append(bundle.getString("state." + newVersionState));
                    buffer.append("\n").append(bundle.getString("updated.synced-with")).append(": ").append(newSyncedWith == null ? bundle.getString("updated.not-synced") : newSyncedWith);
                    buffer.append("\n").append(bundle.getString("updated.change-type")).append(": ").append(bundle.getString("changetype." + newChangeType));
                    buffer.append("\n").append(bundle.getString("updated.change-comment")).append(": ").append(StringUtils.trimToEmpty(newChangeComment));

                    if (parts != null) {
                        String partsTitle = bundle.getString("common.parts-title");
                        buffer.append("\n\n").append(partsTitle);
                        buffer.append('\n').append(TemplateUtil.repeatChar('=', partsTitle.length()));

                        Iterator it = parts.iterator();
                        while (it.hasNext()) {
                            Object object = it.next();
                            if (object instanceof UpdatedPartInfo) {
                                UpdatedPartInfo partInfo = (UpdatedPartInfo)object;
                                String label = partInfo.partType.getLabel(locale);
                                buffer.append("\n\n").append(label);
                                buffer.append("\n").append(TemplateUtil.repeatChar('-', label.length()));
                                buffer.append('\n').append(bundle.getString("updated.part-updated"));

                                buffer.append("\n").append(bundle.getString("common.part.mime-type")).append(": ").append(partInfo.mimeType);
                                if (partInfo.oldMimeType == null) {
                                    buffer.append(" (").append(bundle.getString("updated.unchanged")).append(')');
                                } else {
                                    buffer.append(" (").append(bundle.getString("updated.previous-version")).append(": ").append(partInfo.oldMimeType).append(")");
                                }

                                buffer.append("\n").append(bundle.getString("common.part.filename")).append(": ").append(partInfo.fileName);
                                if (partInfo.oldFileName == null) {
                                    buffer.append(" (").append(bundle.getString("updated.unchanged")).append(')');
                                } else {
                                    buffer.append(" (").append(bundle.getString("updated.previous-version")).append(": ").append(partInfo.oldFileName).append(")");
                                }

                                buffer.append('\n').append(bundle.getString("common.part.size")).append(": ").append(partInfo.size).append(" ").append(bundle.getString("common.bytes"));
                                if (partInfo.oldSize == -1) {
                                    buffer.append(" (").append(bundle.getString("updated.unchanged")).append(")");
                                } else {
                                    buffer.append(" (").append(bundle.getString("updated.previous-version")).append(": ").append(partInfo.oldSize).append(" ").append(bundle.getString("common.bytes")).append(")");
                                }

                                if (partInfo.oldContent != null) {
                                    StringWriter writer = new StringWriter();
                                    TextDiffOutput textDiffOutput = new TextDiffOutput(writer, false, locale);
                                    try {
                                        Diff.diff(partInfo.oldContent, partInfo.newContent, textDiffOutput, 3);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }

                                    buffer.append('\n').append(bundle.getString("updated.content-diff")).append(":\n");
                                    buffer.append(writer.getBuffer());
                                }
                            } else if (object instanceof AddedPartInfo) {
                                AddedPartInfo partInfo = (AddedPartInfo)object;
                                String label = partInfo.partType.getLabel(locale);
                                buffer.append("\n\n").append(label);
                                buffer.append("\n").append(TemplateUtil.repeatChar('-', label.length()));
                                buffer.append('\n').append(bundle.getString("updated.part-added"));
                                buffer.append('\n').append(bundle.getString("common.part.mime-type")).append(": ").append(partInfo.mimeType);
                                buffer.append('\n').append(bundle.getString("common.part.filename")).append(": ").append(partInfo.fileName);
                                buffer.append('\n').append(bundle.getString("common.part.size")).append(": ").append(partInfo.size).append(" ").append(bundle.getString("common.bytes"));
                                if (partInfo.content != null) {
                                    buffer.append('\n').append(bundle.getString("common.part.content")).append(":\n");
                                    buffer.append(partInfo.content);
                                    buffer.append("\n");

                                }
                            } else if (object instanceof RemovedPartInfo) {
                                RemovedPartInfo partInfo = (RemovedPartInfo)object;
                                String label = partInfo.partType.getLabel(locale);
                                buffer.append("\n\n");
                                buffer.append(label);
                                buffer.append("\n").append(TemplateUtil.repeatChar('-', label.length()));
                                buffer.append('\n').append(bundle.getString("updated.part-removed")).append('\n');
                            /* We don't do this case anymore
                            } else if (object instanceof UnchangedPartInfo) {
                                UnchangedPartInfo partInfo = (UnchangedPartInfo)object;
                                String label = partInfo.partType.getLabel(locale);
                                buffer.append("\n");
                                buffer.append(label);
                                buffer.append("\n").append(TemplateUtil.repeatChar('-', label.length()));
                                buffer.append('\n').append(bundle.getString("updated.part-unchanged")).append('\n'); */
                            } else if (object instanceof MightBeUpdatedPartInfo) {
                                MightBeUpdatedPartInfo partInfo = (MightBeUpdatedPartInfo)object;
                                String label = partInfo.partType.getLabel(locale);
                                buffer.append("\n\n");
                                buffer.append(label);
                                buffer.append("\n").append(bundle.getString("updated.no-diff-too-big")).append("\n");
                            }
                        }
                    }

                    if (fields != null) {
                        String fieldsTitle = bundle.getString("common.fields-title");
                        buffer.append("\n\n").append(fieldsTitle);
                        buffer.append("\n").append(TemplateUtil.repeatChar('=', fieldsTitle.length()));

                        Iterator it = fields.iterator();
                        while (it.hasNext()) {
                            Object object = it.next();
                            if (object instanceof AddedFieldInfo) {
                                AddedFieldInfo fieldInfo = (AddedFieldInfo)object;
                                String label = fieldInfo.fieldType.getLabel(locale);
                                String value = FieldHelper.getFormattedValue(fieldInfo.value, fieldInfo.fieldType.getValueType(), locale, repository);
                                buffer.append("\n").append(label).append(": ").append(value).append(" (").append(bundle.getString("updated.new-field")).append(')');
                            } else if (object instanceof RemovedFieldInfo) {
                                RemovedFieldInfo fieldInfo = (RemovedFieldInfo)object;
                                String label = fieldInfo.fieldType.getLabel(locale);
                                buffer.append("\n").append(label).append(": (").append(bundle.getString("updated.field-removed")).append(")");
                            } else if (object instanceof UpdatedFieldInfo) {
                                UpdatedFieldInfo fieldInfo = (UpdatedFieldInfo)object;
                                String label = fieldInfo.fieldType.getLabel(locale);
                                String value1 = FieldHelper.getFormattedValue(fieldInfo.value, fieldInfo.fieldType.getValueType(), locale, repository);
                                String value2 = FieldHelper.getFormattedValue(fieldInfo.oldValue, fieldInfo.fieldType.getValueType(), locale, repository);
                                buffer.append("\n").append(label).append(": ").append(value1).append(" (").append(bundle.getString("updated.previous-version")).append(": ").append(value2).append(")");
                            }
                        }
                    }

                    if (removedLinks != null || addedLinks != null) {
                        String linksTitle = bundle.getString("common.links-title");
                        buffer.append("\n\n").append(linksTitle);
                        buffer.append("\n").append(TemplateUtil.repeatChar('=', linksTitle.length()));

                        if (removedLinks != null) {
                            Iterator it = removedLinks.iterator();
                            while (it.hasNext()) {
                                LinkInfo linkInfo = (LinkInfo)it.next();
                                buffer.append('\n').append(bundle.getString("updated.removed-link"));
                                buffer.append("\n   ").append(bundle.getString("common.link.title")).append(": ").append(linkInfo.title);
                                buffer.append("\n   ").append(bundle.getString("common.link.target")).append(": ").append(linkInfo.target);
                            }
                        }

                        if (addedLinks != null) {
                            Iterator it = addedLinks.iterator();
                            while (it.hasNext()) {
                                LinkInfo linkInfo = (LinkInfo)it.next();
                                buffer.append('\n').append(bundle.getString("updated.added-link"));
                                buffer.append("\n   ").append(bundle.getString("common.link.title")).append(": ").append(linkInfo.title);
                                buffer.append("\n   ").append(bundle.getString("common.link.target")).append(": ").append(linkInfo.target);
                            }
                        }
                    }
                } else {
                    buffer.append("\n\n").append(bundle.getString("updated.no-new-version"));
                }


                if (removedCustomFields != null || addedCustomFields != null || updatedCustomFields != null) {
                    String customFieldsTitle = bundle.getString("common.customfields-title");
                    buffer.append("\n\n").append(customFieldsTitle);
                    buffer.append('\n').append(TemplateUtil.repeatChar('=', customFieldsTitle.length()));

                    if (removedCustomFields != null) {
                        Iterator it = removedCustomFields.iterator();
                        while (it.hasNext()) {
                            CustomFieldInfo customFieldInfo = (CustomFieldInfo)it.next();
                            buffer.append("\n").append(customFieldInfo.name).append(": (").append(bundle.getString("updated.removed")).append(")");
                        }
                    }
                    if (addedCustomFields != null) {
                        Iterator it = addedCustomFields.iterator();
                        while (it.hasNext()) {
                            CustomFieldInfo customFieldInfo = (CustomFieldInfo)it.next();
                            buffer.append("\n").append(customFieldInfo.name).append(": ").append(customFieldInfo.value).append(" (").append(bundle.getString("updated.new")).append(")");
                        }
                    }
                    if (updatedCustomFields != null) {
                        Iterator it = updatedCustomFields.iterator();
                        while (it.hasNext()) {
                            CustomFieldInfo customFieldInfo = (CustomFieldInfo)it.next();
                            buffer.append("\n").append(customFieldInfo.name).append(": ").append(customFieldInfo.value).append(" (").append(bundle.getString("updated.previously")).append(": ").append(customFieldInfo.oldValue).append(")");
                        }
                    }
                }


                if (removedCollections != null || addedCollections != null) {
                    String collectionsTitle = bundle.getString("common.collections-title");
                    buffer.append("\n\n").append(collectionsTitle);
                    buffer.append('\n').append(TemplateUtil.repeatChar('=', collectionsTitle.length()));

                    if (removedCollections != null) {
                        Iterator it = removedCollections.iterator();
                        while (it.hasNext()) {
                            CollectionInfo collectionInfo = (CollectionInfo)it.next();
                            buffer.append('\n').append(bundle.getString("updated.removed-from-collection")).append(' ').append(collectionInfo.name);
                        }
                    }
                    if (addedCollections != null) {
                        Iterator it = addedCollections.iterator();
                        while (it.hasNext()) {
                            CollectionInfo collectionInfo = (CollectionInfo)it.next();
                            buffer.append('\n').append(bundle.getString("updated.added-to-collection")).append(' ').append(collectionInfo.name);
                        }
                    }
                }

                message = buffer.toString();
                cachedByLocale.put(locale, message);
            }
            return message;
        }

        public void addRemovedPart(RemovedPartInfo removedPartInfo) {
            if (parts == null)
                parts = new ArrayList();
            parts.add(removedPartInfo);
        }

        public void addAddedPart(AddedPartInfo addedPartInfo) {
            if (parts == null)
                parts = new ArrayList();
            parts.add(addedPartInfo);
        }

        public void addUpdatedPart(UpdatedPartInfo updatedPartInfo) {
            if (parts == null)
                parts = new ArrayList();
            parts.add(updatedPartInfo);
        }

        public void addMightBeUpdatedPart(MightBeUpdatedPartInfo partInfo) {
            if (parts == null)
                parts = new ArrayList();
            parts.add(partInfo);
        }

        public void addUnchangedPart(UnchangedPartInfo unchangedPartInfo) {
            /* Disabled -- don't show unchanged parts anymore
            if (parts == null)
                parts = new ArrayList();
            parts.add(unchangedPartInfo);
            */
        }

        public void addAddedField(AddedFieldInfo addedFieldInfo) {
            if (fields == null)
                fields = new ArrayList();
            fields.add(addedFieldInfo);
        }

        public void addRemovedField(RemovedFieldInfo removedFieldInfo) {
            if (fields == null)
                fields = new ArrayList();
            fields.add(removedFieldInfo);
        }

        public void addUpdatedField(UpdatedFieldInfo updatedFieldInfo) {
            if (fields == null)
                fields = new ArrayList();
            fields.add(updatedFieldInfo);
        }

        public void addAddedLink(LinkInfo linkInfo) {
            if (addedLinks == null)
                addedLinks = new ArrayList();
            addedLinks.add(linkInfo);
        }

        public void addRemovedLink(LinkInfo linkInfo) {
            if (removedLinks == null)
                removedLinks = new ArrayList();
            removedLinks.add(linkInfo);
        }

        public void addAddedCustomField(CustomFieldInfo customFieldInfo) {
            if (addedCustomFields == null)
                addedCustomFields = new ArrayList();
            addedCustomFields.add(customFieldInfo);
        }

        public void addRemovedCustomField(CustomFieldInfo customFieldInfo) {
            if (removedCustomFields == null)
                removedCustomFields = new ArrayList();
            removedCustomFields.add(customFieldInfo);
        }

        public void addUpdatedCustomField(CustomFieldInfo customFieldInfo) {
            if (updatedCustomFields == null)
                updatedCustomFields = new ArrayList();
            updatedCustomFields.add(customFieldInfo);
        }

        public void addAddedCollection(CollectionInfo collectionInfo) {
            if (addedCollections == null)
                addedCollections = new ArrayList();
            addedCollections.add(collectionInfo);
        }

        public void addRemovedCollection(CollectionInfo collectionInfo) {
            if (removedCollections == null)
                removedCollections = new ArrayList();
            removedCollections.add(collectionInfo);
        }
    }

    static class RemovedPartInfo {
        public PartType partType;
    }

    static class AddedPartInfo {
        public PartType partType;
        public String mimeType;
        public String fileName;
        public long size;
        public String content;
    }

    static class UpdatedPartInfo {
        public PartType partType;
        public String mimeType;
        public String oldMimeType;
        public String fileName;
        public String oldFileName;
        public long size;
        public long oldSize = -1;
        public String oldContent;
        public String newContent;
    }

    static class UnchangedPartInfo {
        public PartType partType;
    }

    static class MightBeUpdatedPartInfo {
        public PartType partType;
    }

    static class AddedFieldInfo {
        public FieldType fieldType;
        public Object value;
    }

    static class UpdatedFieldInfo {
        public FieldType fieldType;
        public Object value;
        public Object oldValue;
    }

    static class RemovedFieldInfo {
        public FieldType fieldType;
    }

    static class LinkInfo {
        public String title;
        public String target;
    }

    static class CustomFieldInfo {
        public String name;
        public String value;
        public String oldValue;
    }

    static class CollectionInfo {
        public String name;
    }

    static class MyDocDiffOutput implements DocDiffOutput {
        private DocumentVariantUpdatedMailTemplate template;
        private DocDiffOutputHelper outputHelper;

        public MyDocDiffOutput(DocumentVariantUpdatedMailTemplate template, DocDiffOutputHelper outputHelper) {
            this.outputHelper = outputHelper;
            this.template = template;
        }

        public void begin() throws Exception {
        }

        public void end() throws Exception {
        }

        public void beginPartChanges() throws Exception {
        }

        public void partRemoved(Part removedPart) throws Exception {
            RemovedPartInfo removedPartInfo = new RemovedPartInfo();
            removedPartInfo.partType = outputHelper.getPartType(removedPart.getTypeId());
            template.addRemovedPart(removedPartInfo);
        }

        public void partAdded(Part addedPart) throws Exception {
            AddedPartInfo partInfo = new AddedPartInfo();
            partInfo.partType = outputHelper.getPartType(addedPart.getTypeId());
            String mimeType = addedPart.getMimeType();
            partInfo.mimeType = mimeType;
            partInfo.fileName = addedPart.getFileName();
            partInfo.size = addedPart.getSize();

            if (mimeType.startsWith("text/") && addedPart.getSize() < INCLUDE_LIMIT) {
                byte[] data = addedPart.getData();
                String encoding = null;
                if (mimeType.equals("text/xml")) {
                    encoding = XmlEncodingDetector.detectEncoding(data);
                }
                partInfo.content = encoding != null ? new String(addedPart.getData(), encoding) : new String(addedPart.getData());
            }

            template.addAddedPart(partInfo);
        }

        public void partUnchanged(Part unchangedPart) throws Exception {
            UnchangedPartInfo partInfo = new UnchangedPartInfo();
            partInfo.partType = outputHelper.getPartType(unchangedPart.getTypeId());
            template.addUnchangedPart(partInfo);
        }

        public void partUpdated(Part version1Part, Part version2Part, String part1Data, String part2Data) throws Exception {
            UpdatedPartInfo partInfo = new UpdatedPartInfo();
            partInfo.partType = outputHelper.getPartType(version1Part.getTypeId());

            partInfo.mimeType = version2Part.getMimeType();
            if (!version1Part.getMimeType().equals(version2Part.getMimeType())) {
                partInfo.oldMimeType = version1Part.getMimeType();
            }

            String version1FileName = nullToEmpty(version1Part.getFileName());
            String version2FileName = nullToEmpty(version2Part.getFileName());
            partInfo.fileName = version2FileName;
            if (!version1FileName.equals(version2FileName)) {
                partInfo.oldFileName = version1FileName;
            }

            partInfo.size = version2Part.getSize();
            if (version1Part.getSize() != version2Part.getSize()) {
                partInfo.oldSize = version1Part.getSize();
            }

            if (part1Data != null) {
                partInfo.oldContent = part1Data;
                partInfo.newContent = part2Data;
            }

            template.addUpdatedPart(partInfo);
        }

        private String nullToEmpty(String value) {
            if (value == null)
                return "";
            return value;
        }

        public void partMightBeUpdated(Part version2Part) throws Exception {
            MightBeUpdatedPartInfo partInfo = new MightBeUpdatedPartInfo();
            partInfo.partType = outputHelper.getPartType(version2Part.getTypeId());
            template.addMightBeUpdatedPart(partInfo);
        }

        public void endPartChanges() throws Exception {
        }

        public void beginFieldChanges() throws Exception {
        }

        public void endFieldChanges() throws Exception {
        }

        public void fieldAdded(Field addedField) throws Exception {
            AddedFieldInfo fieldInfo = new AddedFieldInfo();
            fieldInfo.fieldType = outputHelper.getFieldType(addedField.getTypeId());
            fieldInfo.value = addedField.getValue();
            template.addAddedField(fieldInfo);
        }

        public void fieldRemoved(Field removedField) throws Exception {
            RemovedFieldInfo fieldInfo = new RemovedFieldInfo();
            fieldInfo.fieldType = outputHelper.getFieldType(removedField.getTypeId());
            template.addRemovedField(fieldInfo);
        }

        public void fieldUpdated(Field version1Field, Field version2Field) throws Exception {
            UpdatedFieldInfo fieldInfo = new UpdatedFieldInfo();
            fieldInfo.fieldType = outputHelper.getFieldType(version1Field.getTypeId());
            fieldInfo.value = version2Field.getValue();
            fieldInfo.oldValue = version1Field.getValue();
            template.addUpdatedField(fieldInfo);
        }

        public void beginLinkChanges() throws Exception {
        }

        public void linkRemoved(Link link) throws Exception {
            LinkInfo linkInfo = new LinkInfo();
            linkInfo.title = link.getTitle();
            linkInfo.target = link.getTarget();
            template.addRemovedLink(linkInfo);
        }

        public void linkAdded(Link link) throws Exception {
            LinkInfo linkInfo = new LinkInfo();
            linkInfo.title = link.getTitle();
            linkInfo.target = link.getTarget();
            template.addAddedLink(linkInfo);
        }

        public void endLinkChanges() throws Exception {
        }
    }
}
