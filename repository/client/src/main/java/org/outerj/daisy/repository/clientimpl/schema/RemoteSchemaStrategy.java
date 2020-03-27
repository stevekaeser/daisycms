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
package org.outerj.daisy.repository.clientimpl.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager;
import org.outerj.daisy.repository.clientimpl.infrastructure.AbstractRemoteStrategy;
import org.outerj.daisy.repository.clientimpl.infrastructure.DaisyHttpClient;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.LinkExtractorInfoImpl;
import org.outerj.daisy.repository.commonimpl.LinkExtractorInfosImpl;
import org.outerj.daisy.repository.commonimpl.schema.DocumentTypeImpl;
import org.outerj.daisy.repository.commonimpl.schema.FieldTypeImpl;
import org.outerj.daisy.repository.commonimpl.schema.PartTypeImpl;
import org.outerj.daisy.repository.commonimpl.schema.SchemaStrategy;
import org.outerj.daisy.repository.schema.RepositorySchemaEventType;
import org.outerx.daisy.x10.*;

public class RemoteSchemaStrategy extends AbstractRemoteStrategy implements SchemaStrategy {
    public RemoteSchemaStrategy(RemoteRepositoryManager.Context context) {
        super(context);
    }

    public DocumentTypeImpl getDocumentTypeById(long id, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new GetMethod("/repository/schema/documentType/" + id);

        DocumentTypeDocument documentTypeDocument = (DocumentTypeDocument)httpClient.executeMethod(method, DocumentTypeDocument.class, true);
        DocumentTypeDocument.DocumentType documentTypeXml = documentTypeDocument.getDocumentType();
        DocumentTypeImpl documentType = instantiateDocumentTypeFromXml(documentTypeXml, user);
        return documentType;
    }

    public DocumentTypeImpl getDocumentTypeByName(String name, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        String encodedName = encodeNameForUseInPath("document type", name);
        HttpMethod method = new GetMethod("/repository/schema/documentTypeByName/" + encodedName);

        DocumentTypeDocument documentTypeDocument = (DocumentTypeDocument)httpClient.executeMethod(method, DocumentTypeDocument.class, true);
        DocumentTypeDocument.DocumentType documentTypeXml = documentTypeDocument.getDocumentType();
        DocumentTypeImpl documentType = instantiateDocumentTypeFromXml(documentTypeXml, user);
        return documentType;
    }

    public void store(DocumentTypeImpl documentType) throws RepositoryException {
        DocumentTypeImpl.IntimateAccess documentTypeInt = documentType.getIntimateAccess(this);
        DaisyHttpClient httpClient = getClient(documentTypeInt.getCurrentModifier());

        String url = "/repository";
        boolean isNew = documentType.getId() == -1;
        if (isNew)
            url += "/schema/documentType";
        else
            url += "/schema/documentType/" + documentType.getId();

        PostMethod method = new PostMethod(url);
        method.setRequestEntity(new InputStreamRequestEntity(documentType.getXml().newInputStream()));

        DocumentTypeDocument documentTypeDocument = (DocumentTypeDocument)httpClient.executeMethod(method, DocumentTypeDocument.class, true);
        DocumentTypeDocument.DocumentType documentTypeXml = documentTypeDocument.getDocumentType();
        documentTypeInt.setId(documentTypeXml.getId());
        documentTypeInt.setLastModified(documentTypeXml.getLastModified().getTime());
        documentTypeInt.setLastModifier(documentTypeXml.getLastModifier());
        documentTypeInt.setUpdateCount(documentTypeXml.getUpdateCount());

        if (isNew)
            context.getCommonRepositorySchema().fireSchemaEvent(RepositorySchemaEventType.DOCUMENT_TYPE_CREATED, documentType.getId(), documentType.getUpdateCount());
        else
            context.getCommonRepositorySchema().fireSchemaEvent(RepositorySchemaEventType.DOCUMENT_TYPE_UPDATED, documentType.getId(), documentType.getUpdateCount());
    }

    public void deleteDocumentType(long documentTypeId, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);

        String url = "/repository/schema/documentType/" + documentTypeId;

        DeleteMethod method = new DeleteMethod(url);
        httpClient.executeMethod(method, null, true);
        context.getCommonRepositorySchema().fireSchemaEvent(RepositorySchemaEventType.DOCUMENT_TYPE_DELETED, documentTypeId, -1);
    }

    public Collection<DocumentTypeImpl> getAllDocumentTypes(AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new GetMethod("/repository/schema/documentType");

        DocumentTypesDocument documentTypesDocument = (DocumentTypesDocument)httpClient.executeMethod(method, DocumentTypesDocument.class, true);
        DocumentTypesDocument.DocumentTypes documentTypesElement = documentTypesDocument.getDocumentTypes();

        List<DocumentTypeDocument.DocumentType> documentTypesXml = documentTypesElement.getDocumentTypeList();
        List<DocumentTypeImpl> documentTypes = new ArrayList<DocumentTypeImpl>(documentTypesXml.size());
        for (DocumentTypeDocument.DocumentType documentTypeXml : documentTypesXml) {
            DocumentTypeImpl documentType = instantiateDocumentTypeFromXml(documentTypeXml, user);
            documentTypes.add(documentType);
        }
        return documentTypes;
    }

    private DocumentTypeImpl instantiateDocumentTypeFromXml(DocumentTypeDocument.DocumentType documentTypeXml, AuthenticatedUser user) {
        DocumentTypeImpl documentType = new DocumentTypeImpl(documentTypeXml.getName(), this,
                context.getCommonRepository(), user);
        DocumentTypeImpl.IntimateAccess documentTypeInt = documentType.getIntimateAccess(this);
        documentType.setAllFromXml(documentTypeXml);
        documentTypeInt.setLastModified(documentTypeXml.getLastModified().getTime());
        documentTypeInt.setLastModifier(documentTypeXml.getLastModifier());
        documentTypeInt.setId(documentTypeXml.getId());
        documentTypeInt.setUpdateCount(documentTypeXml.getUpdateCount());
        return documentType;
    }

    public PartTypeImpl getPartTypeById(long id, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new GetMethod("/repository/schema/partType/" + id);

        PartTypeDocument partTypeDocument = (PartTypeDocument)httpClient.executeMethod(method, PartTypeDocument.class, true);
        PartTypeDocument.PartType partTypeXml = partTypeDocument.getPartType();
        PartTypeImpl partType = instantiatePartTypeFromXml(partTypeXml, user);
        return partType;
    }

    public PartTypeImpl getPartTypeByName(String name, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        String encodedName = encodeNameForUseInPath("part type", name);
        HttpMethod method = new GetMethod("/repository/schema/partTypeByName/" + encodedName);

        PartTypeDocument partTypeDocument = (PartTypeDocument)httpClient.executeMethod(method, PartTypeDocument.class, true);
        PartTypeDocument.PartType partTypeXml = partTypeDocument.getPartType();
        PartTypeImpl partType = instantiatePartTypeFromXml(partTypeXml, user);
        return partType;
    }

    public void store(PartTypeImpl partType) throws RepositoryException {
        PartTypeImpl.IntimateAccess partTypeInt = partType.getIntimateAccess(this);
        DaisyHttpClient httpClient = getClient(partTypeInt.getCurrentModifier());

        String url = "/repository";
        boolean isNew = partType.getId() == -1;
        if (isNew)
            url += "/schema/partType";
        else
            url += "/schema/partType/" + partType.getId();

        PostMethod method = new PostMethod(url);
        method.setRequestEntity(new InputStreamRequestEntity(partType.getXml().newInputStream()));

        PartTypeDocument partTypeDocument = (PartTypeDocument)httpClient.executeMethod(method, PartTypeDocument.class, true);
        PartTypeDocument.PartType partTypeXml = partTypeDocument.getPartType();
        partTypeInt.setId(partTypeXml.getId());
        partTypeInt.setLastModified(partTypeXml.getLastModified().getTime());
        partTypeInt.setLastModifier(partTypeXml.getLastModifier());
        partTypeInt.setUpdateCount(partTypeXml.getUpdateCount());

        if (isNew)
            context.getCommonRepositorySchema().fireSchemaEvent(RepositorySchemaEventType.PART_TYPE_CREATED, partType.getId(), partType.getUpdateCount());
        else
            context.getCommonRepositorySchema().fireSchemaEvent(RepositorySchemaEventType.PART_TYPE_UPDATED, partType.getId(), partType.getUpdateCount());
    }

    public void deletePartType(long partTypeId, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);

        String url = "/repository/schema/partType/" + partTypeId;

        DeleteMethod method = new DeleteMethod(url);

        httpClient.executeMethod(method, null, true);
        context.getCommonRepositorySchema().fireSchemaEvent(RepositorySchemaEventType.PART_TYPE_DELETED, partTypeId, -1);
    }

    public Collection<PartTypeImpl> getAllPartTypes(AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new GetMethod("/repository/schema/partType");

        PartTypesDocument partTypesDocument = (PartTypesDocument)httpClient.executeMethod(method, PartTypesDocument.class, true);
        PartTypesDocument.PartTypes partTypesElement = partTypesDocument.getPartTypes();
        List<PartTypeDocument.PartType> partTypesXml = partTypesElement.getPartTypeList();
        List<PartTypeImpl> partTypes = new ArrayList<PartTypeImpl>(partTypesXml.size());
        for (PartTypeDocument.PartType partTypeXml : partTypesXml) {
            PartTypeImpl partType = instantiatePartTypeFromXml(partTypeXml, user);
            partTypes.add(partType);
        }
        return partTypes;
    }

    private PartTypeImpl instantiatePartTypeFromXml(PartTypeDocument.PartType partTypeXml, AuthenticatedUser user) {
        PartTypeImpl partType = new PartTypeImpl(partTypeXml.getName(), partTypeXml.getMimeTypes(),
                this, user);
        PartTypeImpl.IntimateAccess partTypeInt = partType.getIntimateAccess(this);
        partType.setAllFromXml(partTypeXml);
        partTypeInt.setId(partTypeXml.getId());
        partTypeInt.setLastModified(partTypeXml.getLastModified().getTime());
        partTypeInt.setLastModifier(partTypeXml.getLastModifier());
        partTypeInt.setUpdateCount(partTypeXml.getUpdateCount());
        return partType;
    }

    public FieldTypeImpl getFieldTypeById(long id, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new GetMethod("/repository/schema/fieldType/" + id);

        FieldTypeDocument fieldTypeDocument = (FieldTypeDocument)httpClient.executeMethod(method, FieldTypeDocument.class, true);
        FieldTypeDocument.FieldType fieldTypeXml = fieldTypeDocument.getFieldType();
        FieldTypeImpl fieldType = instantiateFieldTypeFromXml(fieldTypeXml, user);
        return fieldType;
    }

    public FieldTypeImpl getFieldTypeByName(String name, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        String encodedName = encodeNameForUseInPath("field type", name);
        HttpMethod method = new GetMethod("/repository/schema/fieldTypeByName/" + encodedName);

        FieldTypeDocument fieldTypeDocument = (FieldTypeDocument)httpClient.executeMethod(method, FieldTypeDocument.class, true);
        FieldTypeDocument.FieldType fieldTypeXml = fieldTypeDocument.getFieldType();
        FieldTypeImpl fieldType = instantiateFieldTypeFromXml(fieldTypeXml, user);
        return fieldType;
    }

    public void store(FieldTypeImpl fieldType) throws RepositoryException {
        FieldTypeImpl.IntimateAccess fieldTypeInt = fieldType.getIntimateAccess(this);
        DaisyHttpClient httpClient = getClient(fieldTypeInt.getCurrentModifier());

        String url = "/repository";
        boolean isNew = fieldType.getId() == -1;
        if (isNew)
            url += "/schema/fieldType";
        else
            url += "/schema/fieldType/" + fieldType.getId();

        PostMethod method = new PostMethod(url);
        method.setRequestEntity(new InputStreamRequestEntity(fieldType.getXml().newInputStream()));

        FieldTypeDocument fieldTypeDocument = (FieldTypeDocument)httpClient.executeMethod(method, FieldTypeDocument.class, true);
        FieldTypeDocument.FieldType fieldTypeXml = fieldTypeDocument.getFieldType();
        fieldTypeInt.setId(fieldTypeXml.getId());
        fieldTypeInt.setLastModified(fieldTypeXml.getLastModified().getTime());
        fieldTypeInt.setLastModifier(fieldTypeXml.getLastModifier());
        fieldTypeInt.setUpdateCount(fieldTypeXml.getUpdateCount());

        if (isNew)
            context.getCommonRepositorySchema().fireSchemaEvent(RepositorySchemaEventType.FIELD_TYPE_CREATED, fieldType.getId(), fieldType.getUpdateCount());
        else
            context.getCommonRepositorySchema().fireSchemaEvent(RepositorySchemaEventType.FIELD_TYPE_UPDATED, fieldType.getId(), fieldType.getUpdateCount());
    }

    public void deleteFieldType(long fieldTypeId, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);

        String url = "/repository/schema/fieldType/" + fieldTypeId;

        DeleteMethod method = new DeleteMethod(url);
        httpClient.executeMethod(method, null, true);
        context.getCommonRepositorySchema().fireSchemaEvent(RepositorySchemaEventType.FIELD_TYPE_DELETED, fieldTypeId, -1);
    }

    public Collection<FieldTypeImpl> getAllFieldTypes(AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new GetMethod("/repository/schema/fieldType");

        FieldTypesDocument fieldTypesDocument = (FieldTypesDocument)httpClient.executeMethod(method, FieldTypesDocument.class, true);
        FieldTypesDocument.FieldTypes fieldTypesElement = fieldTypesDocument.getFieldTypes();
        List<FieldTypeDocument.FieldType> fieldTypesXml = fieldTypesElement.getFieldTypeList();
        List<FieldTypeImpl> fieldTypes = new ArrayList<FieldTypeImpl>(fieldTypesXml.size());
        for (FieldTypeDocument.FieldType fieldTypeXml : fieldTypesXml) {
            FieldTypeImpl partType = instantiateFieldTypeFromXml(fieldTypeXml, user);
            fieldTypes.add(partType);
        }
        return fieldTypes;
    }

    private FieldTypeImpl instantiateFieldTypeFromXml(FieldTypeDocument.FieldType fieldTypeXml, AuthenticatedUser user) {
        FieldTypeImpl fieldType = new FieldTypeImpl(fieldTypeXml.getName(),
                ValueType.fromString(fieldTypeXml.getValueType()), fieldTypeXml.getMultiValue(),
                fieldTypeXml.getHierarchical(), this, user);
        FieldTypeImpl.IntimateAccess fieldTypeInt = fieldType.getIntimateAccess(this);
        fieldType.setAllFromXml(fieldTypeXml);
        fieldTypeInt.setId(fieldTypeXml.getId());
        fieldTypeInt.setLastModified(fieldTypeXml.getLastModified().getTime());
        fieldTypeInt.setLastModifier(fieldTypeXml.getLastModifier());
        fieldTypeInt.setUpdateCount(fieldTypeXml.getUpdateCount());
        return fieldType;
    }

    public LinkExtractorInfos getLinkExtractors(AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new GetMethod("/repository/linkExtractors");

        LinkExtractorsDocument linkExtractorsDocument = (LinkExtractorsDocument)httpClient.executeMethod(method, LinkExtractorsDocument.class, true);
        List<LinkExtractorDocument.LinkExtractor> linkExtractorsXml = linkExtractorsDocument.getLinkExtractors().getLinkExtractorList();
        LinkExtractorInfo[] linkExtractorInfos = new LinkExtractorInfo[linkExtractorsXml.size()];
        for (int i = 0; i < linkExtractorsXml.size(); i++) {
            linkExtractorInfos[i] = new LinkExtractorInfoImpl(linkExtractorsXml.get(i).getName(), linkExtractorsXml.get(i).getDescription());
        }
        return new LinkExtractorInfosImpl(linkExtractorInfos);
    }

    public ExpSelectionListDocument getExpandedSelectionListData(long fieldTypeId, long branchId, long languageId, Locale locale, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new GetMethod("/repository/schema/fieldType/" + fieldTypeId + "/selectionListData");


        List<NameValuePair> queryString = new ArrayList<NameValuePair>();
        if (branchId != -1)
            queryString.add(new NameValuePair("branch", String.valueOf(branchId)));
        if (languageId != -1)
            queryString.add(new NameValuePair("language", String.valueOf(languageId)));
        queryString.add(new NameValuePair("locale", LocaleHelper.getString(locale)));

        method.setQueryString(queryString.toArray(new NameValuePair[0]));

        return (ExpSelectionListDocument)httpClient.executeMethod(method, ExpSelectionListDocument.class, true);
    }
}
