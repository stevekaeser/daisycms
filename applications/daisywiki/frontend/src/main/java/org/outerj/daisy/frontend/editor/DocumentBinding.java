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
package org.outerj.daisy.frontend.editor;

import org.apache.cocoon.forms.formmodel.*;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.acl.AccessDetails;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.acl.AclDetailPermission;
import org.outerj.daisy.repository.schema.RepositorySchema;

import java.util.*;

public class DocumentBinding {
    private static final String FIELD_PREFIX = "field_";

    /**
     * This load should be executed only once on a form, since it registers validators too.
     * @param documentEditorContext TODO
     */
    public static void load(DocumentEditorForm form, DocumentEditorContext documentEditorContext, Document document, Repository repository, Locale locale) throws Exception {
        if (document.getLastVersion() != null && document.getLastVersion().getSyncedWith() != null) {
            form.setSyncedWithLanguageId(document.getLastVersion().getSyncedWith().getLanguageId());
            form.setSyncedWithVersionId(document.getLastVersion().getSyncedWith().getVersionId());
        } else {
            form.setSyncedWithLanguageId(-1);
            form.setSyncedWithVersionId(-1);
        }

        RepositorySchema repositorySchema = repository.getRepositorySchema();
        // first load part and field data
        Form additionalPartsAndFieldsForm = form.getAdditionalPartsAndFieldsForm();
        Repeater additionalParts = (Repeater)additionalPartsAndFieldsForm.getChild("additionalParts");
        Part[] parts = document.getParts().getArray();
        for (int i = 0; i < parts.length; i++) {
            Part part = parts[i];
            Form partForm = form.getPartForm(part.getTypeName());
            if (partForm == null) {
                // it is a part that is present in the document but not in the document type
                Repeater.RepeaterRow row = additionalParts.addRow();
                row.getChild("typeId").setValue(new Long(part.getTypeId()));
                row.getChild("label").setValue(repositorySchema.getPartTypeById(part.getTypeId(), false).getLabel(locale));
                row.getChild("size").setValue(new Long(part.getSize()));
                row.getChild("mimeType").setValue(part.getMimeType());
            }
        }

        Repeater additionalFields = (Repeater)additionalPartsAndFieldsForm.getChild("additionalFields");
        org.outerj.daisy.repository.Field[] fields = document.getFields().getArray();
        Form fieldsForm = form.getFieldsForm();
        for (int i = 0; i < fields.length; i++) {
            org.outerj.daisy.repository.Field field = fields[i];
            String widgetId = FIELD_PREFIX + field.getTypeId();
            Widget widget = fieldsForm != null ? fieldsForm.getChild(widgetId) : null;
            if (widget == null) {
                // it is a field that exists in the document but not in the document type
                Repeater.RepeaterRow row = additionalFields.addRow();
                row.getChild("typeId").setValue(new Long(field.getTypeId()));
                row.getChild("label").setValue(repositorySchema.getFieldTypeById(field.getTypeId(), false).getLabel(locale));
                row.getChild("value").setValue(FieldHelper.getFormattedValue(field.getValue(), field.getValueType(), locale, repository));
            }
        }

        AclResultInfo aclInfo = documentEditorContext.getAclInfo(document);
        form.setAclInfo(aclInfo);
        
        documentEditorContext.setupEditors(document, aclInfo);

        form.setDocumentName(document.getName());

        Form linksForm = form.getLinksForm();
        Repeater linksRepeater = (Repeater)linksForm.getChild("links");
        Link[] links = document.getLinks().getArray();
        for (int i = 0; i < links.length; i++) {
            Repeater.RepeaterRow row = linksRepeater.addRow();
            row.getChild("title").setValue(links[i].getTitle());
            row.getChild("target").setValue(links[i].getTarget());
        }

        Form miscForm = form.getMiscForm();
        miscForm.getChild("private").setValue(Boolean.valueOf(document.isPrivate()));
        miscForm.getChild("retired").setValue(Boolean.valueOf(document.isRetired()));
        miscForm.getChild("referenceLanguageId").setValue(document.getReferenceLanguageId());

        Repeater customFieldsRepeater = (Repeater)miscForm.getChild("customFields");
        Iterator customFieldsIt = document.getCustomFields().entrySet().iterator();
        while (customFieldsIt.hasNext()) {
            Repeater.RepeaterRow row = customFieldsRepeater.addRow();
            Map.Entry entry = (Map.Entry)customFieldsIt.next();
            row.getChild("name").setValue(entry.getKey());
            row.getChild("value").setValue(entry.getValue());
        }

        DocumentCollection[] collections = document.getCollections().getArray();
        Long[] collectionIds = new Long[collections.length];
        for (int i = 0; i < collections.length; i++) {
            collectionIds[i] = new Long(collections[i].getId());
        }
        miscForm.getChild("collections").setValue(collectionIds);

        applyAclInfo(aclInfo, form);
    }

    private static void applyAclInfo(AclResultInfo aclInfo, DocumentEditorForm form) {
        AccessDetails details = aclInfo.getAccessDetails(AclPermission.WRITE);
        if (details.isFullAccess())
            return;

        Form linksForm = form.getLinksForm();
        linksForm.getChild("links").setState(getWidgetState(details, AclDetailPermission.LINKS));
        linksForm.getChild("addLink").setState(getWidgetState(details, AclDetailPermission.LINKS));

        Form miscForm = form.getMiscForm();
        miscForm.getChild("private").setState(getWidgetState(details, AclDetailPermission.PRIVATE));
        miscForm.getChild("retired").setState(getWidgetState(details, AclDetailPermission.RETIRED));
        miscForm.getChild("referenceLanguageId").setState(getWidgetState(details, AclDetailPermission.REFERENCE_LANGUAGE));
        miscForm.getChild("customFields").setState(getWidgetState(details, AclDetailPermission.CUSTOM_FIELDS));
        miscForm.getChild("addCustomField").setState(getWidgetState(details, AclDetailPermission.CUSTOM_FIELDS));
        miscForm.getChild("collections").setState(getWidgetState(details, AclDetailPermission.COLLECTIONS));
    }

    private static WidgetState getWidgetState(AccessDetails details, AclDetailPermission permission) {
        return details.isGranted(permission) ? WidgetState.ACTIVE : WidgetState.DISABLED;
    }

    public static void save(DocumentEditorForm form, DocumentEditorContext documentEditorContext, Document document, Repository repository) throws Exception {
        // save part and field data
        documentEditorContext.saveEditors(document);

        // remove obsolete parts and fields
        Form additionalPartsAndFieldsForm = form.getAdditionalPartsAndFieldsForm();
        Repeater additionalParts = (Repeater)additionalPartsAndFieldsForm.getChild("additionalParts");
        for (int i = 0; i < additionalParts.getSize(); i++) {
            Repeater.RepeaterRow row = additionalParts.getRow(i);
            if (row.getChild("delete").getValue().equals(Boolean.TRUE)) {
                document.deletePart(((Long)row.getChild("typeId").getValue()).longValue());
            }
        }
        Repeater additionalFields = (Repeater)additionalPartsAndFieldsForm.getChild("additionalFields");
        for (int i = 0; i < additionalFields.getSize(); i++) {
            Repeater.RepeaterRow row = additionalFields.getRow(i);
            if (row.getChild("delete").getValue().equals(Boolean.TRUE)) {
                document.deleteField(((Long)row.getChild("typeId").getValue()).longValue());
            }
        }

        document.setName(form.getDocumentName());
        if (form.getRequestedDocumentId() != null && form.getRequestedDocumentId() != "") {
            document.setRequestedId(form.getRequestedDocumentId());
        }
        document.setNewSyncedWith(form.getSyncedWithLanguageId(), form.getSyncedWithVersionId());
        document.setNewChangeType(form.getMajorChange() ? ChangeType.MAJOR : ChangeType.MINOR);
        document.setNewChangeComment(form.getChangeComment());

        //
        // Links
        //

        // first check if any links changed
        boolean linksNeedUpdating = false;
        Form linksForm = form.getLinksForm();
        Repeater linksRepeater = (Repeater)linksForm.getChild("links");
        Link[] links = document.getLinks().getArray();
        if (linksRepeater.getSize() != links.length) {
            linksNeedUpdating = true;
        } else {
            for (int i = 0; i < linksRepeater.getSize(); i++) {
                if (!links[i].getTitle().equals(linksRepeater.getWidget(i, "title").getValue())) {
                    linksNeedUpdating = true;
                    break;
                }
                if (!links[i].getTarget().equals(linksRepeater.getWidget(i, "target").getValue())) {
                    linksNeedUpdating = true;
                    break;
                }
            }
        }

        // if there were any link changes, re-add all links
        if (linksNeedUpdating) {
            document.clearLinks();
            for (int i = 0; i < linksRepeater.getSize(); i++) {
                String title = (String)linksRepeater.getWidget(i, "title").getValue();
                String target = (String)linksRepeater.getWidget(i, "target").getValue();
                document.addLink(title, target);
            }
        }


        Form miscForm = form.getMiscForm();
        document.setPrivate(((Boolean)miscForm.getChild("private").getValue()).booleanValue());
        document.setRetired(((Boolean)miscForm.getChild("retired").getValue()).booleanValue());
        document.setReferenceLanguageId((Long)miscForm.getChild("referenceLanguageId").getValue());

        //
        // User fields
        //

        // first check if any user fields changed
        boolean customFieldsNeedUpdating = false;
        Repeater customFieldRepeater = (Repeater)miscForm.getChild("customFields");
        Map customFields = document.getCustomFields();
        if (customFields.size() != customFieldRepeater.getSize()) {
            customFieldsNeedUpdating = true;
        } else {
            for (int i = 0; i < customFieldRepeater.getSize(); i++) {
                String name = (String)customFieldRepeater.getWidget(i, "name").getValue();
                String value = (String)customFieldRepeater.getWidget(i, "value").getValue();
                if (!value.equals(customFields.get(name))) {
                    customFieldsNeedUpdating = true;
                    break;
                }
            }
        }

        // if there were any user field changes, re-add them all
        if (customFieldsNeedUpdating) {
            document.clearCustomFields();
            for (int i = 0; i < customFieldRepeater.getSize(); i++) {
                String name = (String)customFieldRepeater.getWidget(i, "name").getValue();
                String value = (String)customFieldRepeater.getWidget(i, "value").getValue();
                document.setCustomField(name, value);
            }
        }

        //
        // Collections
        //

        // first check if there were any collections changes
        boolean collectionsNeedUpdating = false;
        Object[] collectionIds = (Object[])miscForm.getChild("collections").getValue();
        DocumentCollection[] collections = document.getCollections().getArray();
        if (collectionIds.length != collections.length) {
            collectionsNeedUpdating = true;
        } else {
            for (int i = 0; i < collections.length; i++) {
                boolean found = false;
                for (int j = 0; j < collectionIds.length; j++) {
                    if (((Long)collectionIds[j]).longValue() == collections[i].getId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    collectionsNeedUpdating = true;
                    break;
                }
            }
        }

        // if there were any collection changes, re-add them all
        if (collectionsNeedUpdating) {
            CollectionManager collectionManager = repository.getCollectionManager();
            document.clearCollections();
            for (int i = 0; i < collectionIds.length; i++) {
                document.addToCollection(collectionManager.getCollection(((Long)collectionIds[i]).longValue(), false));
            }
        }

        // New version state
        if (form.getPublishImmediately()) {
            document.setNewVersionState(VersionState.PUBLISH);
        } else {
            document.setNewVersionState(VersionState.DRAFT);
        }
        
        if (form.getMajorChange()) {
            document.setNewChangeType(ChangeType.MAJOR);
        } else {
            document.setNewChangeType(ChangeType.MINOR);
        }
        
        document.setNewChangeComment(form.getChangeComment());

    }

}
