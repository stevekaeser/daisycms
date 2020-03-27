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
package org.outerj.daisy.repository.clientimpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.PartSource;
import org.outerj.daisy.repository.ChangeType;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.FieldHelper;
import org.outerj.daisy.repository.LiveHistoryEntry;
import org.outerj.daisy.repository.LockType;
import org.outerj.daisy.repository.PartDataSource;
import org.outerj.daisy.repository.RepositoryEventType;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.acl.AccessDetails;
import org.outerj.daisy.repository.acl.AclActionType;
import org.outerj.daisy.repository.acl.AclDetailPermission;
import org.outerj.daisy.repository.clientimpl.infrastructure.AbstractRemoteStrategy;
import org.outerj.daisy.repository.clientimpl.infrastructure.DaisyHttpClient;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUserImpl;
import org.outerj.daisy.repository.commonimpl.AvailableVariantImpl;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerj.daisy.repository.commonimpl.DocumentCollectionImpl;
import org.outerj.daisy.repository.commonimpl.DocumentImpl;
import org.outerj.daisy.repository.commonimpl.DocumentReadAccessWrapper;
import org.outerj.daisy.repository.commonimpl.DocumentStrategy;
import org.outerj.daisy.repository.commonimpl.DocumentVariantImpl;
import org.outerj.daisy.repository.commonimpl.FieldImpl;
import org.outerj.daisy.repository.commonimpl.LinkImpl;
import org.outerj.daisy.repository.commonimpl.LiveHistoryEntryImpl;
import org.outerj.daisy.repository.commonimpl.LockInfoImpl;
import org.outerj.daisy.repository.commonimpl.PartImpl;
import org.outerj.daisy.repository.commonimpl.TimelineImpl;
import org.outerj.daisy.repository.commonimpl.VersionImpl;
import org.outerj.daisy.repository.commonimpl.acl.AccessDetailsImpl;
import org.outerj.daisy.repository.commonimpl.schema.CommonRepositorySchema;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.util.ListUtil;
import org.outerx.daisy.x10.AvailableVariantDocument;
import org.outerx.daisy.x10.AvailableVariantsDocument;
import org.outerx.daisy.x10.DocumentDocument;
import org.outerx.daisy.x10.FieldDocument;
import org.outerx.daisy.x10.LinksDocument;
import org.outerx.daisy.x10.LiveHistoryEntryDocument;
import org.outerx.daisy.x10.LockInfoDocument;
import org.outerx.daisy.x10.PartDocument;
import org.outerx.daisy.x10.TimelineDocument;
import org.outerx.daisy.x10.UserInfoDocument;
import org.outerx.daisy.x10.VersionDocument;
import org.outerx.daisy.x10.VersionsDocument;
import org.outerx.daisy.x10.TimelineDocument.Timeline;

public class RemoteDocumentStrategy extends AbstractRemoteStrategy implements DocumentStrategy {

    public RemoteDocumentStrategy(RemoteRepositoryManager.Context context) {
        super(context);
    }

    public Document load(DocId docId, long branchId, long languageId, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new GetMethod("/repository/document/" + docId.toString());
        method.setQueryString(getBranchLangParams(branchId, languageId));

        DocumentDocument documentDocument = (DocumentDocument)httpClient.executeMethod(method, DocumentDocument.class, true);
        DocumentDocument.Document documentXml = documentDocument.getDocument();
        Document document = instantiateDocumentFromXml(documentXml, user);
        return document;
    }

    private Document instantiateDocumentFromXml(DocumentDocument.Document documentXml, AuthenticatedUser user) throws RepositoryException {
        DocumentImpl document = new DocumentImpl(this, context.getCommonRepository(), user, documentXml.getTypeId(), documentXml.getBranchId(), documentXml.getLanguageId());
        DocumentImpl.IntimateAccess documentInt = document.getIntimateAccess(this);
        DocId docId = DocId.parseDocId(documentXml.getId(), context.getCommonRepository());
        documentInt.load(docId, documentXml.getLastModified().getTime(), documentXml.getLastModifier(), documentXml.getCreated().getTime(), documentXml.getOwner(), documentXml.getPrivate(), documentXml.getUpdateCount(), documentXml.getReferenceLanguageId());

        DocumentVariantImpl variant = documentInt.getVariant();
        DocumentVariantImpl.IntimateAccess variantInt = variant.getIntimateAccess(this);

        variantInt.load(documentXml.getTypeId(),
                documentXml.getRetired(),
                documentXml.getLastVersionId(),
                documentXml.isSetLiveVersionId() ? documentXml.getLiveVersionId() : -1,
                documentXml.getVariantLastModified().getTime(),
                documentXml.getVariantLastModifier(),
                documentXml.getCreatedFromBranchId(),
                documentXml.getCreatedFromLanguageId(),
                documentXml.getCreatedFromVersionId(),
                documentXml.getLastMajorChangeVersionId(),
                documentXml.getLiveMajorChangeVersionId(),
                documentXml.getVariantUpdateCount());

        for (long collectionId : documentXml.getCollectionIds().getCollectionIdList()) {
            // Note: it is possible that a collection is removed since we retrieved the document, or that a new
            // collection has been recently added and isn't in the local collection cache yet, however these
            // are edge-cases that we'll just live with for now.
            DocumentCollectionImpl collection = context.getCommonRepository().getCollectionManager().getCollection(collectionId, false, user);
            variantInt.addCollection(collection);
        }
        
        List<LiveHistoryEntryDocument.LiveHistoryEntry> liveHistoryXml = documentXml.getTimeline().getLiveHistoryEntryList();
        LiveHistoryEntry[] liveHistory = new LiveHistoryEntry[liveHistoryXml.size()];
        for (int i = 0; i < liveHistoryXml.size(); i++) {
            LiveHistoryEntryDocument.LiveHistoryEntry entryXml = liveHistoryXml.get(i);
            Date beginDate = entryXml.getBeginDate().getTime();
            Date endDate = null;
            if (entryXml.isSetEndDate()) {
                endDate = entryXml.getEndDate().getTime();
            }
            long versionId = entryXml.getVersionId();
            long creator = entryXml.getCreator();
            if (entryXml.isSetId()) {
                liveHistory[i] = new LiveHistoryEntryImpl(entryXml.getId(), beginDate, endDate, versionId, creator);
            } else {
                liveHistory[i] = new LiveHistoryEntryImpl(beginDate, endDate, versionId, creator);
            }
        }
        variantInt.getTimeline().getIntimateAccess(this).setLiveHistory(liveHistory, documentXml.getVariantUpdateCount());
        
        for (DocumentDocument.Document.CustomFields.CustomField customFieldXml : documentXml.getCustomFields().getCustomFieldList()) {
            variantInt.setCustomField(customFieldXml.getName(), customFieldXml.getValue());
        }

        variantInt.setLockInfo(instantiateLockInfo(documentXml.getLockInfo()));
        
        if (documentXml.getFullVersionAccess()) { 
            variantInt.setName(documentXml.getName());

            if (documentXml.getSummary() != null)
                variantInt.setSummary(documentXml.getSummary());

            FieldImpl[] fields = instantiateFields(variantInt, documentXml.getFields().getFieldList());
            for (FieldImpl field : fields)
                variantInt.addField(field);

            PartImpl[] parts = instantiateParts(variantInt, documentXml.getParts().getPartList(), variantInt.getLastVersionId());
            for (PartImpl part : parts)
                variantInt.addPart(part);

            LinkImpl[] links = instantiateLinks(documentXml.getLinks().getLinkList());
            for (LinkImpl link : links)
                variantInt.addLink(link);

            return document;
        } else {
            // the user doesn't have full access.
            AccessDetails accessDetails = new AccessDetailsImpl(null, org.outerj.daisy.repository.acl.AclPermission.READ, AclActionType.GRANT);
            accessDetails.set(AclDetailPermission.NON_LIVE, AclActionType.DENY);
            if (!documentXml.isSetDataVersionId()) {
                // dataVersionId is not set, this means only the current live version can be accessed
                accessDetails.set(AclDetailPermission.LIVE_HISTORY, AclActionType.DENY);
            } else {
                accessDetails.set(AclDetailPermission.LIVE_HISTORY, AclActionType.GRANT);
            }
            return new DocumentReadAccessWrapper(document, accessDetails, context.getCommonRepository(), user, this);
        }
    }

    private FieldImpl[] instantiateFields(DocumentVariantImpl.IntimateAccess variantInt, List<FieldDocument.Field> fieldsXml) {
        CommonRepositorySchema repositorySchema = context.getCommonRepositorySchema();
        List<FieldImpl> fields = new ArrayList<FieldImpl>();
        for (FieldDocument.Field fieldXml : fieldsXml) {
            long typeId = fieldXml.getTypeId();
            FieldType fieldType;
            try {
                fieldType = repositorySchema.getFieldTypeById(typeId, false, variantInt.getCurrentUser());
            } catch (RepositoryException e) {
                throw new RuntimeException(DocumentImpl.ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
            }
            ValueType valueType = fieldType.getValueType();
            Object value = FieldHelper.getFieldValueFromXml(valueType, fieldType.isMultiValue(), fieldType.isHierarchical(), fieldXml);
            FieldImpl field = new FieldImpl(variantInt, typeId, value);
            fields.add(field);
        }
        return fields.toArray(new FieldImpl[0]);
    }

    private PartImpl[] instantiateParts(DocumentVariantImpl.IntimateAccess variantInt, List<PartDocument.Part> partsXml, long versionId) {
        List<PartImpl> parts = new ArrayList<PartImpl>();
        for (PartDocument.Part aPartsXml : partsXml) {
            PartImpl part = new PartImpl(variantInt, aPartsXml.getTypeId(), versionId, aPartsXml.getDataChangedInVersion());
            PartImpl.IntimateAccess partInt = part.getIntimateAccess(this);
            partInt.setMimeType(aPartsXml.getMimeType());
            partInt.setFileName(aPartsXml.getFileName());
            partInt.setSize(aPartsXml.getSize());
            parts.add(part);
        }
        return parts.toArray(new PartImpl[0]);
    }

    private LinkImpl[] instantiateLinks(List<LinksDocument.Links.Link> linksXml) {
        List<LinkImpl> links = new ArrayList<LinkImpl>();
        for (LinksDocument.Links.Link linkXml : linksXml) {
            LinkImpl link = new LinkImpl(linkXml.getTitle(), linkXml.getTarget());
            links.add(link);
        }
        return links.toArray(new LinkImpl[0]);
    }

    private LockInfoImpl instantiateLockInfo(LockInfoDocument.LockInfo lockInfoXml) {
        if (!lockInfoXml.getHasLock())
            return new LockInfoImpl();

        return new LockInfoImpl(lockInfoXml.getUserId(), lockInfoXml.getTimeAcquired().getTime(),
                lockInfoXml.getDuration(), LockType.fromString(lockInfoXml.getType().toString()));
    }

    public void store(DocumentImpl document) throws RepositoryException {
        DocumentImpl.IntimateAccess documentInt = document.getIntimateAccess(this);
        DocumentVariantImpl.IntimateAccess variantInt = documentInt.getVariant().getIntimateAccess(this);
        DaisyHttpClient httpClient = getClient(documentInt.getCurrentUser());
        String url;
        if (document.getId() == null)
            url = "/repository/document";
        else
            url = "/repository/document/" + document.getId();


        PostMethod method = new PostMethod(url);

        if (document.isVariantNew()) {
            method.setQueryString(new NameValuePair[] {
                new NameValuePair("createVariant", "yes"),
                new NameValuePair("startBranch", String.valueOf(variantInt.getStartBranchId())),
                new NameValuePair("startLanguage", String.valueOf(variantInt.getStartLanguageId()))
            });
        }

        // Add data for the parts
        DocumentDocument documentDocument = document.getXml();
        List<PartDocument.Part> partsXml = documentDocument.getDocument().getParts().getPartList();
        PartImpl[] parts = variantInt.getPartImpls();
        List<org.apache.commons.httpclient.methods.multipart.Part> postParts = new ArrayList<org.apache.commons.httpclient.methods.multipart.Part>();
        for (int i = 0; i < parts.length; i++) {
            PartImpl.IntimateAccess partInt = parts[i].getIntimateAccess(this);
            if (partInt.isDataUpdated()) {
                String uploadPartName = String.valueOf(i);
                PartDataSource partDataSource = partInt.getPartDataSource();
                postParts.add(new FilePart(uploadPartName, new PartPartSource(partDataSource, uploadPartName)));
                // note: corresponding parts in the parts and partsXml arrays are not necessarily at the same index
                for (PartDocument.Part partXml : partsXml) {
                    if (partXml.getTypeId() == parts[i].getTypeId())
                        partXml.setDataRef(uploadPartName);
                }
            }
        }

        // Add the XML of document
        ByteArrayOutputStream xmlOS = new ByteArrayOutputStream(5000);
        try {
            documentDocument.save(xmlOS);
        } catch (IOException e) {
            throw new RepositoryException("Error serializing document XML.", e);
        }
        postParts.add(new FilePart("xml", new ByteArrayPartSource(xmlOS.toByteArray(), "xml")));
        
        method.setRequestEntity(new MultipartRequestEntity(postParts.toArray(new org.apache.commons.httpclient.methods.multipart.Part[0]), method.getParams()));

        // and send the request
        DocumentDocument responseDocumentDocument = (DocumentDocument)httpClient.executeMethod(method, DocumentDocument.class, true);

        DocumentDocument.Document documentXml = responseDocumentDocument.getDocument();
        DocId docId = DocId.parseDocId(documentXml.getId(), context.getCommonRepository());
        if (documentXml.getUpdateCount() != document.getUpdateCount())
            documentInt.saved(docId, documentXml.getLastModified().getTime(), documentXml.getCreated().getTime(), documentXml.getUpdateCount());

        
        if (documentXml.getVariantUpdateCount() != document.getVariantUpdateCount()) {
            List<LiveHistoryEntryDocument.LiveHistoryEntry> liveHistoryXml = documentXml.getTimeline().getLiveHistoryEntryList();
            LiveHistoryEntry[] liveHistory = new LiveHistoryEntry[liveHistoryXml.size()];
            for (int i = 0; i < liveHistoryXml.size(); i++) {
                LiveHistoryEntryDocument.LiveHistoryEntry entryXml = liveHistoryXml.get(i);
                Date beginDate = entryXml.getBeginDate().getTime();
                Date endDate = null;
                if ( entryXml.isSetEndDate() ) {
                    endDate = entryXml.getEndDate().getTime();
                }
                liveHistory[i] = new LiveHistoryEntryImpl(beginDate, endDate, entryXml.getVersionId(), entryXml.getCreator());
            }

            variantInt.saved(documentXml.getLastVersionId(),
                    documentXml.isSetLiveVersionId() ? documentXml.getLiveVersionId() : -1,
                    documentXml.getVariantLastModified().getTime(),
                    documentXml.getSummary(), documentXml.getVariantUpdateCount(), liveHistory);
        }
    }

    public void deleteDocument(DocId docId, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);

        String url = "/repository/document/" + docId.toString();

        DeleteMethod method = new DeleteMethod(url);
        httpClient.executeMethod(method, null, true);
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.DOCUMENT_DELETED, docId, -1);
    }

    public InputStream getBlob(DocId docId, long branchId, long languageId, long versionId, long partTypeId, AuthenticatedUser user) throws RepositoryException {
        return getBlob(docId, branchId, languageId, String.valueOf(versionId), String.valueOf(partTypeId), user);
    }

    public InputStream getBlob(String blobKey) throws RepositoryException {
        throw new RepositoryException("This method is not supported.");
    }

    public InputStream getBlob(DocId docId, long branchId, long languageId, String version, String partType, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new GetMethod("/repository/document/" + docId + "/version/" + version + "/part/" + partType + "/data");
        method.setQueryString(getBranchLangParams(branchId, languageId));

        httpClient.executeMethod(method, null, false);
        try {
            InputStream is = method.getResponseBodyAsStream();
            return new ReleaseHttpConnectionOnStreamCloseInputStream(is, method);
        } catch (IOException e) {
            method.releaseConnection();
            throw new RepositoryException("Error getting response input stream.", e);
        }
    }

    static class ReleaseHttpConnectionOnStreamCloseInputStream extends InputStream {
        private final InputStream delegate;
        private final HttpMethod method;

        public ReleaseHttpConnectionOnStreamCloseInputStream(InputStream delegate, HttpMethod method) {
            this.delegate = delegate;
            this.method = method;
        }

        public int available() throws IOException {
            return delegate.available();
        }


        public void close() throws IOException {
            method.releaseConnection();
            delegate.close();
        }

        public synchronized void reset() throws IOException {
            delegate.reset();
        }

        public boolean markSupported() {
            return delegate.markSupported();
        }

        public synchronized void mark(int readlimit) {
            delegate.mark(readlimit);
        }

        public long skip(long n) throws IOException {
            return delegate.skip(n);
        }

        public int read(byte b[]) throws IOException {
            return delegate.read(b);
        }

        public int read(byte b[], int off, int len) throws IOException {
            return delegate.read(b, off, len);
        }

        public int read() throws IOException {
            return delegate.read();
        }
    }

    public Document createVariant(DocId docId, long startBranchId, long startLanguageId, long startVersionId, long newBranchId, long newLanguageId, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        String url = "/repository/document/" + docId;
        PostMethod method = new PostMethod(url);

        NameValuePair[] queryString = {
            new NameValuePair("action", "createVariant"),
            new NameValuePair("startBranch", String.valueOf(startBranchId)),
            new NameValuePair("startLanguage", String.valueOf(startLanguageId)),
            new NameValuePair("startVersion", String.valueOf(startVersionId)),
            new NameValuePair("newBranch", String.valueOf(newBranchId)),
            new NameValuePair("newLanguage", String.valueOf(newLanguageId))
        };

        method.setQueryString(queryString);
        DocumentDocument responseDocumentDocument = (DocumentDocument)httpClient.executeMethod(method, DocumentDocument.class, true);
        DocumentDocument.Document documentXml = responseDocumentDocument.getDocument();
        return instantiateDocumentFromXml(documentXml, user);
    }

    public AvailableVariantImpl[] getAvailableVariants(DocId docId, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        String url = "/repository/document/" + docId + "/availableVariants";
        GetMethod method = new GetMethod(url);
        AvailableVariantsDocument availableVariantsDocument = (AvailableVariantsDocument)httpClient.executeMethod(method, AvailableVariantsDocument.class, true);
        List<AvailableVariantDocument.AvailableVariant> availableVariantsXml = availableVariantsDocument.getAvailableVariants().getAvailableVariantList();

        AvailableVariantImpl[] availableVariants = new AvailableVariantImpl[availableVariantsXml.size()];
        for (int i = 0; i < availableVariantsXml.size(); i++) {
            availableVariants[i] = instantiateAvailableVariantFromXml(availableVariantsXml.get(i), user);
        }
        return availableVariants;
    }

    private AvailableVariantImpl instantiateAvailableVariantFromXml(AvailableVariantDocument.AvailableVariant availableVariantXml, AuthenticatedUser user) {
        return new AvailableVariantImpl(availableVariantXml.getBranchId(), availableVariantXml.getLanguageId(),
                availableVariantXml.getRetired(), availableVariantXml.getLiveVersionId(), availableVariantXml.getLastVersionId(),
                context.getCommonRepository().getVariantManager(), user);
    }

    public void deleteVariant(DocId docId, long branchId, long languageId, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);

        String url = "/repository/document/" + docId;

        DeleteMethod method = new DeleteMethod(url);
        method.setQueryString(getBranchLangParams(branchId, languageId));
        httpClient.executeMethod(method, null, true);
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.DOCUMENT_VARIANT_DELETED, new VariantKey(docId.toString(), branchId, languageId), -1);
    }

    public VersionImpl loadVersion(DocumentVariantImpl variant, long versionId) throws RepositoryException {
        DocumentVariantImpl.IntimateAccess variantInt = variant.getIntimateAccess(this);
        DaisyHttpClient httpClient = getClient(variantInt.getCurrentUser());
        HttpMethod method = new GetMethod("/repository/document/" + variant.getDocumentId() + "/version/" + versionId);
        method.setQueryString(getBranchLangParams(variant.getBranchId(), variant.getLanguageId()));

        VersionDocument versionDocument = (VersionDocument)httpClient.executeMethod(method, VersionDocument.class, true);
        VersionDocument.Version versionXml = versionDocument.getVersion();
        return instantiateVersion(variantInt, versionXml);
    }

    public void completeVersion(DocumentVariantImpl variant, VersionImpl version) throws RepositoryException {
        VersionImpl loadedVersion = loadVersion(variant, version.getId());
        VersionImpl.IntimateAccess versionInt = version.getIntimateAccess(this);
        versionInt.setFields((FieldImpl[])loadedVersion.getFields().getArray());
        versionInt.setParts((PartImpl[])loadedVersion.getParts().getArray());
        versionInt.setLinks((LinkImpl[])loadedVersion.getLinks().getArray());
    }

    public VersionImpl[] loadShallowVersions(DocumentVariantImpl variant) throws RepositoryException {
        DocumentVariantImpl.IntimateAccess variantInt = variant.getIntimateAccess(this);
        DaisyHttpClient httpClient = getClient(variantInt.getCurrentUser());
        HttpMethod method = new GetMethod("/repository/document/" + variant.getDocumentId() + "/version");
        method.setQueryString(getBranchLangParams(variant.getBranchId(), variant.getLanguageId()));

        VersionsDocument versionsDocument = (VersionsDocument)httpClient.executeMethod(method, VersionsDocument.class, true);
        List<VersionDocument.Version> versionsXml = versionsDocument.getVersions().getVersionList();
        VersionImpl[] versions = new VersionImpl[versionsXml.size()];
        for (int i = 0; i < versionsXml.size(); i++) {
            versions[i] = instantiateVersion(variantInt, versionsXml.get(i));
        }
        return versions;
    }
    
    public void storeVersion(DocumentImpl document, VersionImpl version, VersionState versionState, VersionKey syncedWith, ChangeType changeType, String changeComment) throws RepositoryException {
        DocumentImpl.IntimateAccess documentInt = document.getIntimateAccess(this);
        VersionImpl.IntimateAccess versionInt = version.getIntimateAccess(this);
        DaisyHttpClient httpClient = getClient(documentInt.getCurrentUser());
        PostMethod method = new PostMethod("/repository/document/" + document.getId() + "/version/" + version.getId());

        method.setQueryString(new NameValuePair[] {
            new NameValuePair("branch", String.valueOf(document.getBranchId())),
            new NameValuePair("language", String.valueOf(document.getLanguageId()))
        });

        // Create version document
        VersionDocument versionDocument = VersionDocument.Factory.newInstance();
        VersionDocument.Version versionXml = versionDocument.addNewVersion();
        versionXml.setState(versionState.toString());
        if (syncedWith != null) {
            versionXml.setSyncedWithLanguageId(syncedWith.getLanguageId());
            versionXml.setSyncedWithVersionId(syncedWith.getVersionId());
        }
        versionXml.setChangeType(changeType.toString());
        versionXml.setChangeComment(changeComment);

        // Add the XML of document
        method.setRequestEntity(new InputStreamRequestEntity(versionDocument.newInputStream()));

        VersionDocument versionResponseDocument = (VersionDocument)httpClient.executeMethod(method, VersionDocument.class, true);
        versionXml = versionResponseDocument.getVersion();
        VersionState newVersionState = VersionState.fromString(versionXml.getState());
        VersionKey newSyncedWith = null;
        if (versionXml.isSetSyncedWithLanguageId() && versionXml.isSetSyncedWithVersionId())
            newSyncedWith = new VersionKey(document.getId(), document.getBranchId(), versionXml.getSyncedWithLanguageId(), versionXml.getSyncedWithVersionId());
        ChangeType newChangeType = ChangeType.fromString(versionXml.getChangeType());
        String newChangeComment = versionXml.getChangeComment();
        versionInt.stateChanged(newVersionState, newSyncedWith, newChangeType, newChangeComment, versionXml.getLastModified().getTime(),
                versionXml.getLastModifier());
    }

    private VersionImpl instantiateVersion(DocumentVariantImpl.IntimateAccess variantInt, VersionDocument.Version versionXml) {
        VersionKey syncedWith = null;
        if (versionXml.isSetSyncedWithLanguageId() && versionXml.isSetSyncedWithVersionId()) {
            syncedWith = new VersionKey(variantInt.getDocId().toString(), variantInt.getDocument().getBranchId(),
                        versionXml.getSyncedWithLanguageId(), versionXml.getSyncedWithVersionId());
        }

        VersionImpl version = new VersionImpl(variantInt, versionXml.getId(), versionXml.getDocumentName(),
                versionXml.getCreated().getTime(), versionXml.getCreator(), VersionState.fromString(versionXml.getState()),
                syncedWith, ChangeType.fromString(versionXml.getChangeType()), versionXml.getChangeComment(), versionXml.getLastModified().getTime(),
                versionXml.getLastModifier(), versionXml.getTotalSizeOfParts(), versionXml.getSummary());

        VersionImpl.IntimateAccess versionInt = version.getIntimateAccess(this);

        if (versionXml.getParts() != null)
            versionInt.setParts(instantiateParts(variantInt, versionXml.getParts().getPartList(), version.getId()));

        if (versionXml.getFields() != null)
            versionInt.setFields(instantiateFields(variantInt, versionXml.getFields().getFieldList()));

        if (versionXml.getLinks() != null)
            versionInt.setLinks(instantiateLinks(versionXml.getLinks().getLinkList()));
        
        return version;
    }

    public LockInfoImpl lock(DocumentVariantImpl variant, long duration, LockType lockType) throws RepositoryException {
        DocumentVariantImpl.IntimateAccess variantInt = variant.getIntimateAccess(this);
        DaisyHttpClient httpClient = getClient(variantInt.getCurrentUser());
        PostMethod method = new PostMethod("/repository/document/" + variant.getDocumentId() + "/lock");
        method.setQueryString(getBranchLangParams(variant.getBranchId(), variant.getLanguageId()));

        {
            LockInfoDocument lockInfoDocument = LockInfoDocument.Factory.newInstance();
            LockInfoDocument.LockInfo lockInfoXml = lockInfoDocument.addNewLockInfo();
            lockInfoXml.setDuration(duration);
            lockInfoXml.setType(LockInfoDocument.LockInfo.Type.Enum.forString(lockType.toString()));
            method.setRequestEntity(new InputStreamRequestEntity(lockInfoDocument.newInputStream()));
        }

        LockInfoDocument lockInfoDocument = (LockInfoDocument)httpClient.executeMethod(method, LockInfoDocument.class, true);
        LockInfoDocument.LockInfo lockInfoXml = lockInfoDocument.getLockInfo();
        return instantiateLockInfo(lockInfoXml);
    }

    public LockInfoImpl getLockInfo(DocumentVariantImpl variant) throws RepositoryException {
        DocumentVariantImpl.IntimateAccess variantInt = variant.getIntimateAccess(this);
        DaisyHttpClient httpClient = getClient(variantInt.getCurrentUser());
        GetMethod method = new GetMethod("/repository/document/" + variant.getDocumentId() + "/lock");
        method.setQueryString(getBranchLangParams(variant.getBranchId(), variant.getLanguageId()));

        LockInfoDocument lockInfoDocument = (LockInfoDocument)httpClient.executeMethod(method, LockInfoDocument.class, true);
        LockInfoDocument.LockInfo lockInfoXml = lockInfoDocument.getLockInfo();
        return instantiateLockInfo(lockInfoXml);
    }

    public LockInfoImpl releaseLock(DocumentVariantImpl variant) throws RepositoryException {
        DocumentVariantImpl.IntimateAccess variantInt = variant.getIntimateAccess(this);
        DaisyHttpClient httpClient = getClient(variantInt.getCurrentUser());
        DeleteMethod method = new DeleteMethod("/repository/document/" + variant.getDocumentId() + "/lock");
        method.setQueryString(getBranchLangParams(variant.getBranchId(), variant.getLanguageId()));

        LockInfoDocument lockInfoDocument = (LockInfoDocument)httpClient.executeMethod(method, LockInfoDocument.class, true);
        LockInfoDocument.LockInfo lockInfoXml = lockInfoDocument.getLockInfo();
        return instantiateLockInfo(lockInfoXml);
    }

    static class ByteArrayPartSource implements PartSource {
        private final byte[] data;
        private final String fileName;

        public ByteArrayPartSource(byte[] data, String fileName) {
            this.data = data;
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }

        public long getLength() {
            return data.length;
        }

        public InputStream createInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }
    }

    static class PartPartSource implements PartSource {
        private final PartDataSource partDataSource;
        private final String fileName;

        public PartPartSource(PartDataSource partDataSource, String fileName) {
            this.partDataSource = partDataSource;
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }

        public long getLength() {
            return partDataSource.getSize();
        }

        public InputStream createInputStream() throws IOException {
            try {
                return partDataSource.createInputStream();
            } catch (RepositoryException e) {
                throw new IOException("RepositoryException: " + e.toString());
            }
        }
    }

    /**
     * Check username/password and retrieves user info from server.
     */
    public AuthenticatedUser getUser(Credentials credentials) throws RepositoryException {
        HttpState httpState = DaisyHttpClient.buildHttpState(credentials.getLogin(), credentials.getPassword(), null);
        DaisyHttpClient httpClient = new DaisyHttpClient(context.getSharedHttpClient(), context.getSharedHostConfiguration(), httpState, credentials.getLogin());

        HttpMethod method = new GetMethod("/repository/userinfo");
        UserInfoDocument userInfoDocument = (UserInfoDocument)httpClient.executeMethod(method, UserInfoDocument.class, true);
        UserInfoDocument.UserInfo userInfoXml = userInfoDocument.getUserInfo();
        AuthenticatedUserImpl user = new AuthenticatedUserImpl(userInfoXml.getUserId(), credentials.getPassword(), ListUtil.toArray(userInfoXml.getActiveRoleIds().getRoleIdList()), ListUtil.toArray(userInfoXml.getAvailableRoleIds().getRoleIdList()), credentials.getLogin());
        return user;
    }
    
    public LiveHistoryEntry[] loadLiveHistory(DocumentVariantImpl variant) throws RepositoryException {
        DocumentVariantImpl.IntimateAccess variantInt = variant.getIntimateAccess(this);
        DaisyHttpClient httpClient = getClient(variantInt.getCurrentUser());
        HttpMethod method = new GetMethod("/repository/document/" + variant.getDocumentId() + "/timeline");
        method.setQueryString(getBranchLangParams(variant.getBranchId(), variant.getLanguageId()));

        TimelineDocument TimelineDocument = (TimelineDocument)httpClient.executeMethod(method, TimelineDocument.class, true);
        List<LiveHistoryEntryDocument.LiveHistoryEntry> liveHistoryXml = TimelineDocument.getTimeline().getLiveHistoryEntryList();
        LiveHistoryEntry[] entries = new LiveHistoryEntry[liveHistoryXml.size()];
        for (int i = 0; i < liveHistoryXml.size(); i++) {
            entries[i] = instantiateLiveHistoryEntry(liveHistoryXml.get(i));
        }
        return entries;
    }
    
    public LiveHistoryEntryImpl instantiateLiveHistoryEntry(LiveHistoryEntryDocument.LiveHistoryEntry entryXml) {
        Date beginDate = entryXml.getBeginDate().getTime();
        Date endDate = null;
        if (entryXml.isSetEndDate()) {
            endDate = entryXml.getEndDate().getTime();
        }
        return new LiveHistoryEntryImpl(entryXml.getId(), beginDate, endDate, entryXml.getVersionId(), entryXml.getCreator());
    }

    public void storeTimeline(DocumentVariantImpl variant,
            TimelineImpl timeline) throws RepositoryException {
        PostMethod method = new PostMethod("/repository/document/" + variant.getDocumentId() + "/timeline");
        DaisyHttpClient httpClient = getClient(variant.getIntimateAccess(this).getCurrentUser());
        
        method.setQueryString(new NameValuePair[] {
            new NameValuePair("branch", String.valueOf(variant.getBranchId())),
            new NameValuePair("language", String.valueOf(variant.getLanguageId()))
        });

        TimelineDocument historyDocument = timeline.getXml();

        // Add the XML of document
        method.setRequestEntity(new InputStreamRequestEntity(historyDocument.newInputStream()));
        
        TimelineDocument timelineDoc = (TimelineDocument)httpClient.executeMethod(method, TimelineDocument.class, true);
        Timeline timelineXml = timelineDoc.getTimeline();
        
        List<LiveHistoryEntryDocument.LiveHistoryEntry> historyXml = timelineXml.getLiveHistoryEntryList();
        LiveHistoryEntry[] liveHistory = new LiveHistoryEntry[historyXml.size()];
        for (int i = 0; i < historyXml.size(); i++) {
            LiveHistoryEntryDocument.LiveHistoryEntry entryXml = historyXml.get(i);
            liveHistory[i] = LiveHistoryEntryImpl.fromXml(entryXml);
        }
        
        variant.getIntimateAccess(this).timelineSaved(timelineXml.getVariantLastModified().getTime(), timelineXml.getVariantLastModifier(), timelineXml.getVariantUpdateCount(), timelineXml.getLiveVersionId(), liveHistory);
    }
    
}
