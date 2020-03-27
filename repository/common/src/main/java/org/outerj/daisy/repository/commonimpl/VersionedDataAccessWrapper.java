/*
 * Copyright 2008 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.repository.commonimpl;

import static org.outerj.daisy.repository.acl.AclDetailPermission.ALL_FIELDS;
import static org.outerj.daisy.repository.acl.AclDetailPermission.ALL_PARTS;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.outerj.daisy.repository.Field;
import org.outerj.daisy.repository.FieldNotFoundException;
import org.outerj.daisy.repository.Fields;
import org.outerj.daisy.repository.Links;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.PartNotFoundException;
import org.outerj.daisy.repository.Parts;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VersionedData;
import org.outerj.daisy.repository.acl.AccessDetails;

public abstract class VersionedDataAccessWrapper implements VersionedData {
    protected VersionedData delegate;
    protected AccessDetails accessDetails;
    protected CommonRepository repository;
    protected AuthenticatedUser currentUser;
    protected static final String UNMODIFIABLE_MESSAGE = "You are not allowed to update this document.";
    protected static final String NONLIVE_ACCESS = "You are not allowed to access non-live information of this document.";
    protected static final String LIVE_HISTORY_ACCESS = "You are not allowed to historic live information of this document.";
    protected static final String ERROR_ACCESSING_REPOSITORY_SCHEMA = "Error accessing repository schema information.";

    public VersionedDataAccessWrapper(VersionedData delegate, AccessDetails accessDetails, CommonRepository repository,
            AuthenticatedUser currentUser) {
        this.delegate = delegate;
        this.accessDetails = accessDetails;
        this.repository = repository;
        this.currentUser = currentUser;
    }

    protected abstract void checkLiveAccess();
        
    public String getDocumentName() {
        return delegate.getDocumentName();
    }

    public Parts getParts() {
        checkLiveAccess();
        
        if (!accessDetails.isGranted(ALL_PARTS))
            return filterParts(delegate.getParts());

        return delegate.getParts();
    }

    public Parts getPartsInOrder() {
        checkLiveAccess();
        
        if (!accessDetails.isGranted(ALL_PARTS))
            return filterParts(delegate.getPartsInOrder());

        return delegate.getPartsInOrder();
    }

    private Parts filterParts(Parts parts) {
        return filterParts(parts, accessDetails);
    }

    public static Parts filterParts(Parts parts, AccessDetails accessDetails) {
        if (accessDetails == null || accessDetails.isGranted(ALL_PARTS))
            return parts;
        
        Set<String> readableParts = accessDetails.getAccessibleParts();
        if (readableParts.isEmpty())
            return new PartsImpl(new Part[0]);

        List<Part> filteredParts = new ArrayList<Part>(parts.getArray().length);
        for (Part part : parts.getArray()) {
            if (readableParts.contains(part.getTypeName())) {
                filteredParts.add(part);
            }
        }
        return new PartsImpl(filteredParts.toArray(new Part[0]));
    }

    public Part getPart(long partTypeId) throws PartNotFoundException {
        checkLiveAccess();
        
        if (!accessDetails.canAccessPart(getPartTypeName(partTypeId)))
            throw new PartNotFoundException(partTypeId);

        return delegate.getPart(partTypeId);
    }

    private String getPartTypeName(long partTypeId) {
        try {
            return repository.getRepositorySchema().getPartTypeById(partTypeId, false, currentUser).getName();
        } catch (RepositoryException e) {
            throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }
    }

    public boolean hasPart(long partTypeId) {
        checkLiveAccess();
        
        if (!accessDetails.canAccessPart(getPartTypeName(partTypeId)))
            return false;

        return delegate.hasPart(partTypeId);
    }

    public boolean hasPart(String name) {
        checkLiveAccess();
        
        if (!accessDetails.canAccessPart(name))
            return false;

        return delegate.hasPart(name);
    }

    public Part getPart(String name) throws PartNotFoundException {
        checkLiveAccess();
        
        if (!accessDetails.canAccessPart(name))
            throw new PartNotFoundException(name);

        return delegate.getPart(name);
    }

    public Fields getFields() {
        checkLiveAccess();
        
        if (!accessDetails.isGranted(ALL_FIELDS))
            return filterFields(delegate.getFields());

        return delegate.getFields();
    }

    public Fields getFieldsInOrder() {
        checkLiveAccess();
        
        if (!accessDetails.isGranted(ALL_FIELDS))
            return filterFields(delegate.getFieldsInOrder());

        return delegate.getFieldsInOrder();
    }

    private Fields filterFields(Fields fields) {
        return filterFields(fields, accessDetails);
    }

    public static Fields filterFields(Fields fields, AccessDetails accessDetails) {
        if (accessDetails == null || accessDetails.isGranted(ALL_FIELDS))
            return fields;

        Set<String> readableFields = accessDetails.getAccessibleFields();
        if (readableFields.isEmpty())
            return new FieldsImpl(new Field[0]);

        List<Field> filteredFields = new ArrayList<Field>(fields.getArray().length);
        for (Field field : fields.getArray()) {
            if (readableFields.contains(field.getTypeName())) {
                filteredFields.add(field);
            }
        }
        return new FieldsImpl(filteredFields.toArray(new Field[0]));
    }

    public Field getField(long fieldTypeId) throws FieldNotFoundException {
        checkLiveAccess();
        
        if (!accessDetails.canAccessField(getFieldTypeName(fieldTypeId)))
            throw new FieldNotFoundException(fieldTypeId);

        return delegate.getField(fieldTypeId);
    }

    public Field getField(String fieldTypeName) throws FieldNotFoundException {
        checkLiveAccess();
        
        if (!accessDetails.canAccessField(fieldTypeName))
            throw new FieldNotFoundException(fieldTypeName);

        return delegate.getField(fieldTypeName);
    }

    private String getFieldTypeName(long fieldTypeId) {
        try {
            return repository.getRepositorySchema().getFieldTypeById(fieldTypeId, false, currentUser).getName();
        } catch (RepositoryException e) {
            throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }
    }

    public boolean hasField(long fieldTypeId) {
        checkLiveAccess();
        
        if (!accessDetails.canAccessField(getFieldTypeName(fieldTypeId)))
            return false;

        return delegate.hasField(fieldTypeId);
    }

    public boolean hasField(String fieldTypeName) {
        checkLiveAccess();
        
        if (!accessDetails.canAccessField(fieldTypeName))
            return false;

        return delegate.hasField(fieldTypeName);
    }

    public Links getLinks() {
        checkLiveAccess();
        
        return delegate.getLinks();
    }
    
    public String getSummary() {
        checkLiveAccess();
        
        return delegate.getSummary();
    }


}
