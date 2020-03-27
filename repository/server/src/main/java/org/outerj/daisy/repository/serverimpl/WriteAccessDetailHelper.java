/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.repository.serverimpl;

import static org.outerj.daisy.repository.acl.AclDetailPermission.ALL_FIELDS;
import static org.outerj.daisy.repository.acl.AclDetailPermission.ALL_PARTS;
import static org.outerj.daisy.repository.acl.AclDetailPermission.CHANGE_COMMENT;
import static org.outerj.daisy.repository.acl.AclDetailPermission.CHANGE_TYPE;
import static org.outerj.daisy.repository.acl.AclDetailPermission.COLLECTIONS;
import static org.outerj.daisy.repository.acl.AclDetailPermission.CUSTOM_FIELDS;
import static org.outerj.daisy.repository.acl.AclDetailPermission.DOCUMENT_NAME;
import static org.outerj.daisy.repository.acl.AclDetailPermission.DOCUMENT_TYPE;
import static org.outerj.daisy.repository.acl.AclDetailPermission.LINKS;
import static org.outerj.daisy.repository.acl.AclDetailPermission.PRIVATE;
import static org.outerj.daisy.repository.acl.AclDetailPermission.REFERENCE_LANGUAGE;
import static org.outerj.daisy.repository.acl.AclDetailPermission.RETIRED;
import static org.outerj.daisy.repository.acl.AclDetailPermission.SYNCED_WITH;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.outerj.daisy.repository.AccessDetailViolationException;
import org.outerj.daisy.repository.ChangeType;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.Field;
import org.outerj.daisy.repository.Link;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.PartPartDataSource;
import org.outerj.daisy.repository.acl.AccessDetails;
import org.outerj.daisy.repository.commonimpl.PartImpl;
import org.outerj.daisy.util.ObjectUtils;

/**
 * Some write access detail related code.
 */
public class WriteAccessDetailHelper {
    /**
     *
     * @param accessDetails Should be AccessDetails of the write permission!
     */
    public static void checkAccessDetails(AccessDetails accessDetails, Document newDocument, Document oldDocument,
            LocalDocumentStrategy strategy) throws AccessDetailViolationException {
        if (accessDetails.isFullAccess())
            return;

        AccessDetailViolationException violations = new AccessDetailViolationException();

        if (!accessDetails.isGranted(CHANGE_COMMENT) && newDocument.getNewChangeComment() != null) {
            violations.addViolation(CHANGE_COMMENT, null);
        }

        if (!accessDetails.isGranted(CHANGE_TYPE) && newDocument.getNewChangeType() != ChangeType.MAJOR) {
            violations.addViolation(CHANGE_TYPE, null);
        }

        if (!accessDetails.isGranted(SYNCED_WITH) && newDocument.getNewSyncedWith() != null) {
            violations.addViolation(SYNCED_WITH, null);
        }

        if (!accessDetails.isGranted(DOCUMENT_NAME)
                && !ObjectUtils.safeEquals(newDocument.getName(), oldDocument.getName())) {
            violations.addViolation(DOCUMENT_NAME, null);
        }

        if (!accessDetails.isGranted(DOCUMENT_TYPE)
                && newDocument.getDocumentTypeId() != oldDocument.getDocumentTypeId()) {
            violations.addViolation(DOCUMENT_TYPE, null);
        }

        if (!accessDetails.isGranted(RETIRED)
                && newDocument.isRetired() != oldDocument.isRetired()) {
            violations.addViolation(RETIRED, null);
        }

        if (!accessDetails.isGranted(PRIVATE)
                && newDocument.isPrivate() != oldDocument.isPrivate()) {
            violations.addViolation(PRIVATE, null);
        }

        if (!accessDetails.isGranted(COLLECTIONS)
                && collectionsChanged(newDocument, oldDocument)) {
            violations.addViolation(COLLECTIONS, null);
        }

        if (!accessDetails.isGranted(CUSTOM_FIELDS)
                && !newDocument.getCustomFields().equals(oldDocument.getCustomFields())) {
            violations.addViolation(CUSTOM_FIELDS, null);
        }

        if (!accessDetails.isGranted(LINKS)
                && !Arrays.equals(newDocument.getLinks().getArray(), oldDocument.getLinks().getArray())) {
            violations.addViolation(LINKS, null);
        }

        if (!accessDetails.isGranted(REFERENCE_LANGUAGE)
                && newDocument.getReferenceLanguageId() != oldDocument.getReferenceLanguageId()) {
            violations.addViolation(REFERENCE_LANGUAGE, null);
        }

        if (!accessDetails.isGranted(ALL_FIELDS)) {
            Collection<Field> fields = getConflictFields(newDocument, oldDocument, accessDetails.getAccessibleFields());
            for (Field field : fields)
                violations.addViolation(ALL_FIELDS, field.getTypeName());
        }

        if (!accessDetails.isGranted(ALL_PARTS)) {
            Collection<Part> parts = getConflictParts(newDocument, oldDocument, accessDetails.getAccessibleParts(), strategy);
            for (Part part : parts)
                violations.addViolation(ALL_PARTS, part.getTypeName());
        }
        
        if (!violations.isEmpty())
            throw violations;
    }

    private static boolean collectionsChanged(Document doc1, Document doc2) {
        DocumentCollection[] doc1Collections = doc1.getCollections().getArray();
        DocumentCollection[] doc2Collections = doc2.getCollections().getArray();

        boolean changed = false;
        if (doc1Collections.length == doc2Collections.length) {
            for (int i = 0; i < doc1Collections.length; i++) {
                if (doc1Collections[i].getId() != doc2Collections[i].getId()) {
                    changed = true;
                    break;
                }
            }
        } else {
            changed = true;
        }

        return changed;
    }

    private static Collection<Field> getConflictFields(Document newDocument, Document oldDocument, Set<String> accessibleFields) {
        Collection<Field> alteredFields = new ArrayList<Field>();

        Field[] oldFields = oldDocument.getFields().getArray();
        Field[] newFields = newDocument.getFields().getArray();

        // Removed fields: we don't care: either you have write access to them, and you
        // are allowed to remove them, or you don't, and then it will be ignored that
        // they are missing.

        // Search for new or updated fields
        for (Field newField : newFields) {
            Field oldField = findField(oldFields, newField.getTypeId());
            if (oldField == null) {
                if (!accessibleFields.contains(newField.getTypeName()))
                    alteredFields.add(newField);
            } else {
                boolean updated = (oldField.isMultiValue() && !Arrays.equals((Object[])oldField.getValue(), (Object[])newField.getValue()))
                        || (!oldField.isMultiValue() && !oldField.getValue().equals(newField.getValue()));
                if (updated && !accessibleFields.contains(newField.getTypeName()))
                    alteredFields.add(newField);
            }
        }

        return alteredFields;
    }

    private static Field findField(Field[] fields, long typeId) {
        for (Field field : fields) {                                  
            if (field.getTypeId() == typeId) {
                return field;
            }
        }
        return null;
    }

    private static Collection<Part> getConflictParts(Document newDocument, Document oldDocument, Set<String> accessibleParts,
            LocalDocumentStrategy strategy) {
        Collection<Part> alteredParts = new ArrayList<Part>();

        Part[] oldParts = oldDocument.getParts().getArray();
        Part[] newParts = newDocument.getParts().getArray();

        // Removed parts: we don't care: either you have write access to them, and you
        // are allowed to remove them, or you don't, and then it will be ignored that
        // they are missing.

        // Search for new or updated parts
        for (Part newPart : newParts) {
            Part oldPart = findPart(oldParts, newPart.getTypeId());
            if (oldPart == null) {
                if (!accessibleParts.contains(newPart.getTypeName()))
                    alteredParts.add(newPart);
            } else {
                if (((PartImpl)newPart).getIntimateAccess(strategy).isDataUpdated()
                        && !accessibleParts.contains(newPart.getTypeName()))
                    alteredParts.add(newPart);
            }
        }

        return alteredParts;
    }

    private static Part findPart(Part[] parts, long typeId) {
        for (Part part : parts) {
            if (part.getTypeId() == typeId) {
                return part;
            }
        }
        return null;
    }

    /**
     * Applies all changes between srcDoc and destDoc to destDoc, taking into account that
     * srcDoc might be missing certain data because of access restrictions.
     *
     * <p>Implementation note: it is of course very important that no properties are forgotten,
     * so that no changes are lost. Obviously, this method is only supposed to make changes
     * to the destDoc, never to the srcDoc.
     */
    public static void copyChanges(AccessDetails accessDetails, Document srcDoc, Document destDoc,
            LocalDocumentStrategy strategy) throws Exception {
        destDoc.setDocumentTypeChecksEnabled(false);

        //
        // Temporary state
        //
        if (srcDoc.getRequestedId() != null)
            destDoc.setRequestedId(srcDoc.getRequestedId());
        
        if (srcDoc.getRequestedLiveVersionId() != 0)
            destDoc.setRequestedLiveVersionId(srcDoc.getRequestedLiveVersionId());

        if (srcDoc.getNewVersionState() != null)
            destDoc.setNewVersionState(srcDoc.getNewVersionState());

        if (srcDoc.getNewChangeComment() != null)
            destDoc.setNewChangeComment(srcDoc.getNewChangeComment());

        if (srcDoc.getNewChangeType() != null)
            destDoc.setNewChangeType(srcDoc.getNewChangeType());

        if (srcDoc.getNewSyncedWith() != null)
            destDoc.setNewSyncedWith(srcDoc.getNewSyncedWith());
        
        //
        // Document-level properties
        //
        if (srcDoc.getOwner() != destDoc.getOwner()) {
            destDoc.setOwner(srcDoc.getOwner());
        }

        if (srcDoc.isPrivate() != destDoc.isPrivate()) {
            destDoc.setPrivate(srcDoc.isPrivate());
        }

        if (srcDoc.getReferenceLanguageId() != destDoc.getReferenceLanguageId()) {
            destDoc.setReferenceLanguageId(srcDoc.getReferenceLanguageId());
        }

        //
        // Non-versioned variant properties
        //

        if (srcDoc.getDocumentTypeId() != destDoc.getDocumentTypeId()) {
            destDoc.changeDocumentType(srcDoc.getDocumentTypeId());
        }

        // Custom fields
        if (!srcDoc.getCustomFields().equals(destDoc.getCustomFields())) {
            destDoc.clearCustomFields();
            for (Map.Entry<String, String> customField : srcDoc.getCustomFields().entrySet()) {
                destDoc.setCustomField(customField.getKey(), customField.getValue());
            }
        }

        // Collections
        if (collectionsChanged(srcDoc, destDoc)) {
            destDoc.clearCollections();
            for (DocumentCollection collection : srcDoc.getCollections().getArray()) {
                destDoc.addToCollection(collection);
            }
        }

        if (srcDoc.isRetired() != destDoc.isRetired()) {
            destDoc.setRetired(srcDoc.isRetired());
        }

        //
        // Versioned properties
        //

        if (!srcDoc.getName().equals(destDoc.getName())) {
            destDoc.setName(srcDoc.getName());
        }

        // Links
        if (!Arrays.equals(srcDoc.getLinks().getArray(), destDoc.getLinks().getArray())) {
            destDoc.clearLinks();
            for (Link link : srcDoc.getLinks().getArray()) {
                destDoc.addLink(link.getTitle(), link.getTarget());
            }
        }

        //
        // Apply field changes
        //
        Field[] destFields = destDoc.getFields().getArray();
        Field[] srcFields = srcDoc.getFields().getArray();

        // Check if there are fields in destDoc which have been removed in srcDoc, if so, if they
        // are writeable, also remove them in destDoc. If they are not writeable, they are allowed
        // to be missing in srcDoc. Same holds for non-readable, or readable-but-not-writeable, but
        // those cases are automatically covered by the not-writeable test.
        for (Field destField : destFields) {
            if (findField(srcFields, destField.getTypeId()) == null && isFieldWritable(destField.getTypeName(), accessDetails)) {
                destDoc.deleteField(destField.getTypeId());
            }
        }

        // All fields in srcDoc, which are writeable: update corresponding field in destDoc
        for (Field srcField : srcFields) {
            if (isFieldWritable(srcField.getTypeName(), accessDetails)) {
                destDoc.setField(srcField.getTypeId(), srcField.getValue());
            }
        }

        //
        // Apply part changes
        //
        Part[] destParts = destDoc.getParts().getArray();
        Part[] srcParts = srcDoc.getParts().getArray();

        //
        for (Part destPart : destParts) {
            if (findPart(srcParts, destPart.getTypeId()) == null && isPartWritable(destPart.getTypeName(), accessDetails)) {
                destDoc.deletePart(destPart.getTypeId());
            }
        }

        for (Part srcPart : srcParts) {
            if (isPartWritable(srcPart.getTypeName(), accessDetails)) {
                // Unlike for fields, parts can be large and therefore the Document object will
                // simply accept any setPart-calls as changes, so we should only call the setPart
                // method if really necessary.
                Part destPart = findPart(destParts, srcPart.getTypeId());
                if (destPart == null) {
                    destDoc.setPart(srcPart.getTypeId(), srcPart.getMimeType(), new PartPartDataSource(srcPart));
                } else {
                    if (((PartImpl)srcPart).getIntimateAccess(strategy).isDataUpdated())
                        destDoc.setPart(srcPart.getTypeId(), srcPart.getMimeType(), new PartPartDataSource(srcPart));
                }
            }
        }
    }

    private static boolean isFieldWritable(String fieldName, AccessDetails accessDetails) {
        return accessDetails.isGranted(ALL_FIELDS) || accessDetails.canAccessField(fieldName);
    }

    private static boolean isPartWritable(String partName, AccessDetails accessDetails) {
        return accessDetails.isGranted(ALL_PARTS) || accessDetails.canAccessPart(partName);
    }
}
