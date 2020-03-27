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
package org.outerj.daisy.repository.serverimpl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.xmlbeans.XmlObject;
import org.outerj.daisy.blobstore.BlobIOException;
import org.outerj.daisy.blobstore.NonExistingBlobException;
import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.outerj.daisy.plugin.PluginHandle;
import org.outerj.daisy.repository.AccessException;
import org.outerj.daisy.repository.AvailableVariant;
import org.outerj.daisy.repository.ChangeType;
import org.outerj.daisy.repository.CollectionDeletedException;
import org.outerj.daisy.repository.CollectionNotFoundException;
import org.outerj.daisy.repository.ConcurrentUpdateException;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.DocumentLockedException;
import org.outerj.daisy.repository.DocumentNotFoundException;
import org.outerj.daisy.repository.DocumentVariantNotFoundException;
import org.outerj.daisy.repository.DocumentWriteDeniedException;
import org.outerj.daisy.repository.Field;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.Link;
import org.outerj.daisy.repository.LiveHistoryEntry;
import org.outerj.daisy.repository.LiveStrategy;
import org.outerj.daisy.repository.LockInfo;
import org.outerj.daisy.repository.LockType;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.PartDataSource;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryEventType;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.acl.AccessDetails;
import org.outerj.daisy.repository.acl.AclDetailPermission;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.commonimpl.AbstractDocumentWrapper;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.AvailableVariantImpl;
import org.outerj.daisy.repository.commonimpl.CommonCollectionManager;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerj.daisy.repository.commonimpl.DocumentCollectionImpl;
import org.outerj.daisy.repository.commonimpl.DocumentImpl;
import org.outerj.daisy.repository.commonimpl.DocumentStrategy;
import org.outerj.daisy.repository.commonimpl.DocumentVariantImpl;
import org.outerj.daisy.repository.commonimpl.DocumentWrapper;
import org.outerj.daisy.repository.commonimpl.FieldImpl;
import org.outerj.daisy.repository.commonimpl.LinkImpl;
import org.outerj.daisy.repository.commonimpl.LiveHistoryEntryImpl;
import org.outerj.daisy.repository.commonimpl.LockInfoImpl;
import org.outerj.daisy.repository.commonimpl.PartImpl;
import org.outerj.daisy.repository.commonimpl.RepositoryImpl;
import org.outerj.daisy.repository.commonimpl.TimelineImpl;
import org.outerj.daisy.repository.commonimpl.VersionImpl;
import org.outerj.daisy.repository.commonimpl.DocumentVariantImpl.IntimateAccess;
import org.outerj.daisy.repository.commonimpl.schema.RepositorySchemaImpl;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.serverimpl.linkextraction.LinkExtractorHelper;
import org.outerj.daisy.repository.serverimpl.linkextraction.LinkInfo;
import org.outerj.daisy.repository.serverimpl.model.LocalSchemaStrategy;
import org.outerj.daisy.repository.spi.local.PreSaveHook;
import org.outerj.daisy.util.DateUtil;
import org.outerj.daisy.util.ObjectUtils;
import org.outerx.daisy.x10.DocumentCreatedDocument;
import org.outerx.daisy.x10.DocumentDeletedDocument;
import org.outerx.daisy.x10.DocumentDocument;
import org.outerx.daisy.x10.DocumentUpdatedDocument;
import org.outerx.daisy.x10.DocumentVariantCreatedDocument;
import org.outerx.daisy.x10.DocumentVariantDeletedDocument;
import org.outerx.daisy.x10.DocumentVariantUpdatedDocument;
import org.outerx.daisy.x10.TimelineDocument;
import org.outerx.daisy.x10.TimelineUpdatedDocument;
import org.outerx.daisy.x10.VersionDocument;
import org.outerx.daisy.x10.VersionUpdatedDocument;

// Implementation notes:
//   - Updates on a document are synchronized by selecting the document record from the
//     documents table with a lock clause (e.g. select ... for update)
public class LocalDocumentStrategy extends AbstractLocalStrategy implements DocumentStrategy {

    public LocalDocumentStrategy(LocalRepositoryManager.Context context, AuthenticatedUser systemUser, JdbcHelper jdbcHelper) {
        super(context, systemUser, jdbcHelper);
    }

    public Document load(DocId docId, long branchId, long languageId, AuthenticatedUser user) throws RepositoryException {
        // check here that branchId and languageId are not -1, since the loadDocumentInTransaction actually
        // allows them to be -1 to avoid the variant to be loaded, but we don't want to make this functionality public
        if (branchId == -1 || languageId == -1)
            throw new RepositoryException("branchId and languageId parameters should not be -1");

        List<Runnable> executeAfterCommit = new ArrayList<Runnable>(2);
        Connection conn = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            DocumentImpl document = loadDocumentInTransaction(user, docId, branchId, languageId, conn, executeAfterCommit);

            conn.commit();
            executeRunnables(executeAfterCommit);

            AclResultInfo aclInfo = context.getCommonRepository().getAccessManager().getAclInfoOnLive(systemUser,
                    user.getId(), user.getActiveRoleIds(), document);

            return DocumentAccessUtil.protectDocument(aclInfo, document, docId, true, user, context.getCommonRepository(), this);
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            if (e instanceof DocumentNotFoundException || e instanceof AccessException || e instanceof DocumentVariantNotFoundException)
                throw (RepositoryException)e;
            else
                throw new RepositoryException("Error loading document " + docId.toString() + ".", e);
        } finally {
            jdbcHelper.closeConnection(conn);
        }
    }

    /**
     * Loads a document inside an already started connection / transaction.
     * It also DOES NOT check access rights (on purpose, we rely on this).
     *
     * If either the branchId or languageId is -1, a document object without loaded variant will be returned.
     * This is for internal use only.
     */
    private DocumentImpl loadDocumentInTransaction(AuthenticatedUser user, DocId docId, long branchId, long languageId, Connection conn, List<Runnable> executeAfterCommit) throws RepositoryException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select created, last_modified, last_modifier, owner, private, reference_lang_id, updatecount from documents where id = ? and ns_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            ResultSet rs = stmt.executeQuery();
            if (!rs.next())
                throw new DocumentNotFoundException(docId.toString());

            DocumentImpl document = new DocumentImpl(this, context.getCommonRepository(), user, -1, branchId, languageId);
            DocumentImpl.IntimateAccess documentInt = document.getIntimateAccess(this);
            long referenceLanguageId = jdbcHelper.getNullableIdField(rs, "reference_lang_id");
            documentInt.load(docId, rs.getTimestamp("last_modified"), rs.getLong("last_modifier"),
                    rs.getTimestamp("created"), rs.getLong("owner"), rs.getBoolean("private"), rs.getLong("updatecount"), referenceLanguageId);
            rs.close();
            stmt.close();

            if (branchId != -1 && languageId != -1)
                loadVariant(document, documentInt, conn, executeAfterCommit);

            return document;
        } catch (DocumentNotFoundException e) {
            throw e;
        } catch (DocumentVariantNotFoundException e) {
            throw e;
        } catch (Throwable e) {
            throw new RepositoryException("Error loading document.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private void loadVariant(DocumentImpl document, DocumentImpl.IntimateAccess documentInt, Connection conn, List<Runnable> executeAfterCommit) throws RepositoryException, SQLException {
        DocumentVariantImpl variant = documentInt.getVariant();
        DocumentVariantImpl.IntimateAccess variantInt = variant.getIntimateAccess(this);
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select doctype_id, retired, lastversion_id, liveversion_id, last_modified, last_modifier, updatecount," +
                    " created_from_branch_id, created_from_lang_id, created_from_version_id, last_major_change_version_id, live_major_change_version_id from document_variants where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            DocId docId = documentInt.getDocId();
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, variant.getBranchId());
            stmt.setLong(4, variant.getLanguageId());
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                throw new DocumentVariantNotFoundException(document.getId(), getBranchName(variant.getBranchId()), variant.getBranchId(), getLanguageName(variant.getLanguageId()), variant.getLanguageId());

            long updateCount = rs.getLong("updatecount");
            variantInt.load(rs.getLong("doctype_id"),
                    rs.getBoolean("retired"),
                    jdbcHelper.getNullableIdField(rs, "lastversion_id"),
                    jdbcHelper.getNullableIdField(rs, "liveversion_id"),
                    rs.getTimestamp("last_modified"),
                    rs.getLong("last_modifier"),
                    jdbcHelper.getNullableIdField(rs, "created_from_branch_id"),
                    jdbcHelper.getNullableIdField(rs, "created_from_lang_id"),
                    jdbcHelper.getNullableIdField(rs, "created_from_version_id"),
                    jdbcHelper.getNullableIdField(rs, "last_major_change_version_id"),
                    jdbcHelper.getNullableIdField(rs, "live_major_change_version_id"),
                    updateCount);

            loadName(variant, variantInt, docId, conn);
            loadCustomFields(variant, variantInt, docId, conn);
            loadParts(variant, variantInt, docId, conn);
            loadFields(variant, variantInt, docId, conn);
            loadLinks(variant, variantInt, docId, conn);
            loadCollections(variant, variantInt, docId, conn);
            loadSummary(variant, variantInt, docId, conn);
            LiveHistoryEntry[] liveHistory = loadLiveHistory(variant, variantInt, docId, conn);

            variantInt.setLiveHistory(liveHistory);

            LockInfoImpl lockInfo = loadLock(docId, document.getBranchId(), document.getLanguageId(), conn, executeAfterCommit);
            variantInt.setLockInfo(lockInfo);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private void loadCollections(DocumentVariantImpl variant, DocumentVariantImpl.IntimateAccess variantInt, DocId docId, Connection conn) throws RepositoryException {
        Collection<DocumentCollectionImpl> collections = loadCollections(variant, docId, conn);
        if (collections != null) {
            for (DocumentCollectionImpl collection : collections) {
                variantInt.addCollection(collection);
            }
        }
    }

    private void loadName(DocumentVariantImpl variant, DocumentVariantImpl.IntimateAccess variantInt, DocId docId, Connection conn) throws SQLException, RepositoryException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select name from document_versions where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ? and id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, variant.getBranchId());
            stmt.setLong(4, variant.getLanguageId());
            stmt.setLong(5, variantInt.getLastVersionId());
            ResultSet rs = stmt.executeQuery();
            if (!rs.next())
                throw new RepositoryException("Strange: no version record found for " + getFormattedVariant(variant.getKey(), variantInt.getLastVersionId()));
            variantInt.setName(rs.getString(1));
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private void loadCustomFields(DocumentVariantImpl variant, DocumentVariantImpl.IntimateAccess variantInt, DocId docId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select name, value from customfields where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, variant.getBranchId());
            stmt.setLong(4, variant.getLanguageId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                variantInt.setCustomField(rs.getString(1), rs.getString(2));
            }
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private void loadParts(DocumentVariantImpl variant, DocumentVariantImpl.IntimateAccess variantInt, DocId docId, Connection conn) throws SQLException {
        Collection<PartImpl> parts = loadParts(variant, variantInt, docId, variantInt.getLastVersionId(), conn);
        for (PartImpl part : parts) {
            variantInt.addPart(part);
        }
    }

    private Collection<PartImpl> loadParts(DocumentVariantImpl variant, DocumentVariantImpl.IntimateAccess variantInt,
                                 DocId docId, long versionId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select blob_id, mimetype, filename, parttype_id, blob_size, changed_in_version from parts where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ? and version_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, variant.getBranchId());
            stmt.setLong(4, variant.getLanguageId());
            stmt.setLong(5, versionId);
            ResultSet rs = stmt.executeQuery();

            List<PartImpl> parts = new ArrayList<PartImpl>(5);
            while (rs.next()) {
                PartImpl part = new PartImpl(variantInt, rs.getLong("parttype_id"), versionId, rs.getLong("changed_in_version"));
                PartImpl.IntimateAccess partInt = part.getIntimateAccess(this);
                partInt.setBlobKey(rs.getString("blob_id"));
                partInt.setMimeType(rs.getString("mimetype"));
                partInt.setFileName(rs.getString("filename"));
                partInt.setSize(rs.getLong("blob_size"));
                parts.add(part);
            }
            return parts;
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private void loadFields(DocumentVariantImpl variant, DocumentVariantImpl.IntimateAccess variantInt, DocId docId, Connection conn) throws SQLException {
        Collection<FieldImpl> fields = loadFields(variant, docId, variantInt.getLastVersionId(), conn);
        for (FieldImpl field : fields) {
            variantInt.addField(field);
        }
    }

    private Collection<FieldImpl> loadFields(DocumentVariantImpl variant, DocId docId, long versionId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select fieldtype_id, stringvalue, datevalue, datetimevalue, integervalue, floatvalue, decimalvalue, booleanvalue, link_docid, ns.name_ as link_ns, link_branchid, link_langid, hier_seq from thefields left join daisy_namespaces ns on (thefields.link_nsid = ns.id) where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ? and version_id = ? order by fieldtype_id, value_seq, hier_seq");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, variant.getBranchId());
            stmt.setLong(4, variant.getLanguageId());
            stmt.setLong(5, versionId);
            ResultSet rs = stmt.executeQuery();

            List<FieldImpl> fields = new ArrayList<FieldImpl>(5);
            List<Object> multiValueValues = new ArrayList<Object>(4);
            List<Object> hierPathValues = new ArrayList<Object>(4);
            FieldType previousFieldType = null;
            while (rs.next()) {
                long fieldTypeId = rs.getLong("fieldtype_id");
                FieldType fieldType;
                if (previousFieldType == null || previousFieldType.getId() != fieldTypeId) {
                    try {
                        fieldType = context.getRepositorySchema().getFieldTypeById(fieldTypeId, false, systemUser);
                    } catch (RepositoryException e) {
                        throw new RuntimeException(DocumentImpl.ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
                    }
                } else {
                    fieldType = previousFieldType;
                }

                if (previousFieldType != null && previousFieldType.getId() != fieldTypeId) {
                    finishMultiValueAndHierarchicalFields(previousFieldType, hierPathValues, multiValueValues, fields, variant, versionId);
                }

                ValueType valueType = fieldType.getValueType();
                LocalSchemaStrategy.FieldValueGetter valueGetter = LocalSchemaStrategy.getValueGetter(valueType);
                Object value = valueGetter.getValue(rs);
                if (fieldType.isHierarchical() /* and either multi value or not */) {
                    if (hierPathValues.size() > 0 && rs.getLong("hier_seq") == 1) {
                        // test the impossible
                        if (!fieldType.isMultiValue())
                            throw new RuntimeException("Assertion failure: got restarted hierarchical path sequence for a field which is not multivalue (" + getFormattedVariant(variant.getKey(), versionId) + ", field type " + fieldType.getName() + ")");
                        HierarchyPath hierarchyPath = new HierarchyPath(hierPathValues.toArray());
                        multiValueValues.add(hierarchyPath);
                        hierPathValues.clear();
                    }
                    hierPathValues.add(value);
                } else if (fieldType.isMultiValue()) {
                    multiValueValues.add(value);
                } else {
                    FieldImpl field = new FieldImpl(variant.getIntimateAccess(this), fieldType.getId(), value);
                    fields.add(field);
                }

                previousFieldType = fieldType;
            }

            if (previousFieldType != null)
                finishMultiValueAndHierarchicalFields(previousFieldType, hierPathValues, multiValueValues, fields, variant, versionId);

            return fields;
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private void finishMultiValueAndHierarchicalFields(FieldType fieldType, List<Object> hierPathValues, List<Object> multiValueValues, List<FieldImpl> fields, DocumentVariantImpl variant, long versionId) {
        if (hierPathValues.size() > 0) {
            HierarchyPath hierarchyPath = new HierarchyPath(hierPathValues.toArray());
            if (!fieldType.isMultiValue()) {
                FieldImpl field = new FieldImpl(variant.getIntimateAccess(this), fieldType.getId(), hierarchyPath);
                fields.add(field);
            } else {
                multiValueValues.add(hierarchyPath);
            }
            hierPathValues.clear();
        }
        if (multiValueValues.size() > 0) {
            if (!fieldType.isMultiValue())
                throw new RuntimeException("Assertion failure: retrieved multiple values for a field which is not multivalue (" + getFormattedVariant(variant.getKey(), versionId) + ", field type " + fieldType.getName() + ")");
            Object[] values = multiValueValues.toArray();
            FieldImpl field = new FieldImpl(variant.getIntimateAccess(this), fieldType.getId(), values);
            fields.add(field);
            multiValueValues.clear();
        }

    }

    private void loadLinks(DocumentVariantImpl variant, DocumentVariantImpl.IntimateAccess variantInt, DocId docId, Connection conn) throws SQLException {
        Collection<LinkImpl> links = loadLinks(variant, docId, variantInt.getLastVersionId(), conn);
        for (LinkImpl link : links) {
            variantInt.addLink(link);
        }
    }

    private Collection<LinkImpl> loadLinks(DocumentVariantImpl variant, DocId docId, long versionId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select title, target from links where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ? and version_id = ? order by id");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, variant.getBranchId());
            stmt.setLong(4, variant.getLanguageId());
            stmt.setLong(5, versionId);
            ResultSet rs = stmt.executeQuery();

            List<LinkImpl> links = new ArrayList<LinkImpl>(5);
            while (rs.next()) {
                LinkImpl link = new LinkImpl(rs.getString(1), rs.getString(2));
                links.add(link);
            }
            rs.close();

            return links;
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private void loadSummary(DocumentVariantImpl variant, DocumentVariantImpl.IntimateAccess variantInt, DocId docId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select summary from summaries where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ? and version_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, variant.getBranchId());
            stmt.setLong(4, variant.getLanguageId());
            stmt.setLong(5, variant.getLastVersionId());
            ResultSet rs = stmt.executeQuery();
            String summary = null;
            if (rs.next()) {
                summary = rs.getString(1);
            }
            variantInt.setSummary(summary);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    public void store(DocumentImpl document) throws RepositoryException {
        if (!logger.isDebugEnabled()) {
            logger.debug("storing " + document.getName() + "(" + document.getId() + "@" + document.getBranchId() + ":" + document.getLanguageId() + ":" + document.getLastVersionId() + ")");
        }
        DocumentImpl.IntimateAccess documentInt = document.getIntimateAccess(this);
        DocumentVariantImpl variant = documentInt.getVariant();
        DocumentVariantImpl.IntimateAccess variantInt = variant.getIntimateAccess(this);

        // Execute pre-save hooks
        List<PluginHandle<PreSaveHook>> preSaveHooks = context.getPreSaveHooks();
        if (preSaveHooks.size() > 0) {
            for (PluginHandle<PreSaveHook> preSaveHook : preSaveHooks) {
                Repository userRepository = new RepositoryImpl(context.getCommonRepository(), documentInt.getCurrentUser());
                try {
                    preSaveHook.getPlugin().process(document, userRepository);
                } catch (Throwable e) {
                    // Note: can't use document.getVariantKey() here in log statement since the document might not have an ID yet.
                    StringBuffer msg = new StringBuffer("Error executing pre-save-hook ")
                        .append(preSaveHook.getName())
                        .append(" on document ")
                        .append(document.getId());
                    if (document.getId() == null) {
                        msg.append(" (requested id: ")
                            .append(document.getRequestedId())
                            .append(")");
                    }
                    msg.append(", branch " + getBranchLabel(document.getBranchId()));
                    msg.append(", language " + getLanguageLabel(document.getLanguageId()));
                    logger.error(msg, e);
                }
            }
        }

        AccessDetails writeAccessDetails;
        DocumentImpl blankDoc = null;

        // Check if the user has write access to the previous situation:
        //  - the previous (= existing) version of the document
        //  - or for new documents, a special blank "conceptual" document
        // These previous situations also determine the write access details. This is mainly
        // done so that one can easily know on beforehand what properties are writeable, e.g.
        // in order to adjust a user interface (document editor) to this.
        AclResultInfo aclInfoOldDoc;
        if (!variant.isNew()) {
            // The ACL info is retrieved using the document ID rather than the document
            // object, so it will work on the stored content.
            // BTW, write access to this old situation is required since otherwise someone
            // could modify document properties such that he gains access to the document,
            // even if before they didn't have access.
            aclInfoOldDoc = context.getCommonRepository().getAccessManager().getAclInfoOnLive(systemUser,
                    documentInt.getCurrentUser().getId(), documentInt.getCurrentUser().getActiveRoleIds(),
                   documentInt.getDocId(), document.getBranchId(), document.getLanguageId());
        } else {
            // Variant is new, AccessDetails are determined by evaluating ACL on an empty document.
            aclInfoOldDoc = context.getCommonRepository().getAccessManager().getAclInfoOnLiveForConceptualDocument(
                    systemUser, documentInt.getCurrentUser().getId(), documentInt.getCurrentUser().getActiveRoleIds(),
                    document.getDocumentTypeId(), document.getBranchId(), document.getLanguageId());
            blankDoc = new DocumentImpl(this, context.getCommonRepository(), documentInt.getCurrentUser(),
                    document.getDocumentTypeId(), document.getBranchId(), document.getLanguageId());
        }

        // check general write access
        if (!aclInfoOldDoc.isAllowed(AclPermission.WRITE))
            throw new DocumentWriteDeniedException(document.getId(), getBranchLabel(document.getBranchId()),
                    getLanguageLabel(document.getLanguageId()), labelUtil.getUserLabel(documentInt.getCurrentUser()));
        
        writeAccessDetails = aclInfoOldDoc.getAccessDetails(AclPermission.WRITE);

        // Check that the author can't save or create a document to which he himself has no access
        AclResultInfo aclInfoNewDoc = context.getCommonRepository().getAccessManager().getAclInfoOnLive(systemUser,
                documentInt.getCurrentUser().getId(), documentInt.getCurrentUser().getActiveRoleIds(), document);

        if (!aclInfoNewDoc.isAllowed(AclPermission.READ))
            throw new RepositoryException("The document cannot be saved because the user would not have read access to it anymore.");

        if (!aclInfoNewDoc.isAllowed(AclPermission.WRITE))
            throw new RepositoryException("The document cannot be saved because the user would not have write access to it anymore.");

        // Check new version attributes
        boolean canPublish = aclInfoNewDoc.isAllowed(AclPermission.PUBLISH);
        if (document.getNewVersionState() == VersionState.PUBLISH && !canPublish)
            document.setNewVersionState(VersionState.DRAFT);
        
        if (!canPublish && document.getRequestedLiveVersionId() != 0) {
            document.setRequestedLiveVersionId(0);
        }
        
        AccessDetails publishAccessDetails = aclInfoNewDoc.getAccessDetails(AclPermission.PUBLISH);
        boolean canChangeLiveHistory = aclInfoNewDoc.isAllowed(AclPermission.PUBLISH) && ( publishAccessDetails == null || publishAccessDetails.isGranted(AclDetailPermission.LIVE_HISTORY) );
        String id = document.getId();
        boolean isDocumentNew = document.isNew();
        DocId docId = documentInt.getDocId(); // will be null for new docs
        boolean isVariantNew = variant.isNew();
        List<PartUpdate> partUpdates = null;
        boolean isTimelineChanged = false;

        // start database stuff
        List<Runnable> executeAfterCommit = new ArrayList<Runnable>(2);
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            if (document.getReferenceLanguageId() != -1) {
                try {
                    context.getCommonRepository().getVariantManager().getLanguage(document.getReferenceLanguageId(), false, systemUser);
                } catch (RepositoryException r) {
                    throw new RepositoryException("The specified referenceLanguage does not exist (no language with id " + document.getReferenceLanguageId() + ")");
                }
            }

            // load the old document (if any), needed to generate event later on
            // this is our last chance to do this
            DocumentImpl oldDocument = null;
            if (!isDocumentNew && !variant.isNew()) {
                oldDocument = loadDocumentInTransaction(documentInt.getCurrentUser(), docId, variant.getBranchId(), variant.getLanguageId(), conn, executeAfterCommit);
            } else if (!isDocumentNew && variant.isNew()) {
                oldDocument = loadDocumentInTransaction(documentInt.getCurrentUser(), docId, -1, -1, conn, executeAfterCommit);
            }

            // Check the document does not contain any changes to things that are not allowed to be changed
            WriteAccessDetailHelper.checkAccessDetails(writeAccessDetails, document, isDocumentNew ? blankDoc : oldDocument, this);
            
            // When one has only partial read access, the retrieved document might not contain all parts and fields.
            // Therefore, we first need to load an unrestricted copy of the document, re-apply all changes the user
            // made to that copy, and then use that copy for our further work.
            LiveHistoryEntry[] origHistory;
            if (docId != null) {
                origHistory = loadLiveHistory(variant, variantInt, docId, conn);
            } else {
                origHistory = new LiveHistoryEntry[0]; 
            }
            DocumentImpl origDocument = null;
            if (!isDocumentNew && !writeAccessDetails.isFullAccess()) {
                long branchId = variant.isNew() ? -1 : variant.getBranchId();
                long languageId = variant.isNew() ? -1 : variant.getLanguageId();
                DocumentImpl prevDocument = loadDocumentInTransaction(documentInt.getCurrentUser(), docId, branchId, languageId, conn, executeAfterCommit);

                if (variant.isNew()) {
                    DocumentVariantImpl.IntimateAccess prevVariantInt = prevDocument.getIntimateAccess(this).getVariant().getIntimateAccess(this);
                    prevVariantInt.setBranchId(variant.getBranchId());
                    prevVariantInt.setLanguageId(variant.getLanguageId());
                }
                
                WriteAccessDetailHelper.copyChanges(writeAccessDetails, document, prevDocument, this);
                origDocument = document;

                // switch the document related variables to point to the new 'full access' document
                document = prevDocument;
                documentInt = document.getIntimateAccess(this);
                variant = documentInt.getVariant();
                variantInt = variant.getIntimateAccess(this);
            }

            Date now = new Date();

            //
            // PART 1: Store the document itself
            //

            // Note: documentNewUpdateCount should always contain the correct update count of the document, even if
            // the document itself is not modified, because we need this further on
            long documentNewUpdateCount = document.getUpdateCount();

            if (isDocumentNew) {
                if (documentInt.getRequestedDocId() != null) {
                    docId = documentInt.getRequestedDocId();
                    // The below is also checked by the database by a primary key constraint, but
                    // we check it here on beforehand in order to throw a nicer error message.
                    stmt = conn.prepareStatement("select 1 from documents where id = ? and ns_id = ?");
                    stmt.setLong(1, docId.getSeqId());
                    stmt.setLong(2, docId.getNsId());
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        throw new RepositoryException("It was requested to create a document with ID " + docId.toString() + ", however the repository already contains a document with this ID.");
                    }
                    stmt.close();
                } else {
                    String newDocNamespace = context.getRepositoryNamespace(document);
                    long newDocSeqId = context.getNextDocumentId(newDocNamespace);                    
                    docId = DocId.getDocId(newDocSeqId, newDocNamespace, context.getCommonRepository());
                }
                id = docId.toString();
                documentNewUpdateCount = 1;

                stmt = conn.prepareStatement("insert into documents(id, ns_id, id_search, created, owner, reference_lang_id, private, last_modified, last_modifier, updatecount) values(?,?,?,?,?,?,?,?,?,?)");
                stmt.setLong(1, docId.getSeqId());
                stmt.setLong(2, docId.getNsId());
                stmt.setString(3, docId.getSeqId() + "-" + docId.getNsId());
                stmt.setTimestamp(4, new Timestamp(now.getTime()));
                stmt.setLong(5, document.getOwner());
                jdbcHelper.setNullableIdField(stmt, 6, document.getReferenceLanguageId());
                stmt.setBoolean(7, document.isPrivate());
                stmt.setTimestamp(8, new Timestamp(now.getTime()));
                stmt.setLong(9, documentInt.getCurrentUser().getId()); // same as owner of course
                stmt.setLong(10, documentNewUpdateCount);

                stmt.execute();
                stmt.close();

                // create event record
                XmlObject eventDescription = createNewDocumentEvent(document, docId, now, 1L);
                eventHelper.createEvent(eventDescription, "DocumentCreated", conn);
            } else if (document.needsSaving()) {
                // if there is an original document, only that one will hold the updateCount as received 
                checkDocumentUpdateCountAndLock(origDocument == null ? document : origDocument, docId, conn);

                documentNewUpdateCount = document.getUpdateCount() + 1;

                // update document record
                stmt = conn.prepareStatement("update documents set last_modified = ?, last_modifier = ?, private = ?, owner = ?, reference_lang_id = ?, updatecount = ? where id = ? and ns_id = ?");
                stmt.setTimestamp(1, new Timestamp(now.getTime()));
                stmt.setLong(2, documentInt.getCurrentUser().getId());
                stmt.setBoolean(3, document.isPrivate());
                stmt.setLong(4, document.getOwner());
                if (document.getReferenceLanguageId() == -1L) {
                    stmt.setNull(5, Types.BIGINT);
                } else {
                    stmt.setLong(5, document.getReferenceLanguageId());
                }
                stmt.setLong(6, documentNewUpdateCount);
                stmt.setLong(7, docId.getSeqId());
                stmt.setLong(8, docId.getNsId());
                stmt.executeUpdate();
                stmt.close();

                // create event record
                XmlObject eventDescription = createDocumentUpdatedEvent(oldDocument, document, now, documentNewUpdateCount);
                eventHelper.createEvent(eventDescription, "DocumentUpdated", conn);

                // free document record for others to update (so that they don't have to wait until the variant is
                // stored too.
                conn.commit();
                executeRunnables(executeAfterCommit);

                documentInt.saved(docId, now, document.getCreated(), documentNewUpdateCount);

                context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.DOCUMENT_UPDATED, id, document.getUpdateCount());
            }


            //
            // PART 2: Store the document variant
            //


            checkValidSyncedWith(conn, docId, document.getBranchId(), document.getNewSyncedWith());

            boolean newVersion = false;
            if (variant.isNew()) {
                stmt = conn.prepareStatement("insert into document_variants(doc_id, ns_id, branch_id, lang_id, link_search, variant_search, doctype_id, retired, lastversion_id, liveversion_id, last_modified, last_modifier, updatecount, created_from_branch_id, created_from_lang_id, created_from_version_id) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                stmt.setLong(1, docId.getSeqId());
                stmt.setLong(2, docId.getNamespaceId());
                stmt.setLong(3, document.getBranchId());
                stmt.setLong(4, document.getLanguageId());
                stmt.setString(5, getLinkSearchString(docId.getSeqId(), docId.getNamespaceId(), document.getBranchId(), document.getLanguageId()));
                stmt.setString(6, getVariantSearchString(document.getBranchId(), document.getLanguageId()));
                stmt.setLong(7, variant.getDocumentTypeId());
                stmt.setBoolean(8, variant.isRetired());
                stmt.setLong(9, -1); // -1 because column is not nullable.  Will be updated immediately.  
                jdbcHelper.setNullableIdField(stmt, 10, -1);
                stmt.setTimestamp(11, new Timestamp(now.getTime()));
                stmt.setLong(12, documentInt.getCurrentUser().getId());
                stmt.setLong(13, 1L);
                jdbcHelper.setNullableIdField(stmt, 14, variant.getCreatedFromBranchId());
                jdbcHelper.setNullableIdField(stmt, 15, variant.getCreatedFromLanguageId());
                jdbcHelper.setNullableIdField(stmt, 16, variant.getCreatedFromVersionId());

                stmt.execute();
                stmt.close();
                newVersion = true;
            } else if (variant.needsSaving()) {
                // if there is an original document, only that one will hold the updateCount as received
                
                checkVariantUpdateCountAndLock(origDocument == null ? document : origDocument, docId, conn);
                newVersion = true;

                if (!canChangeLiveHistory) {
                    // revert changes to the timeline
                    if (variantInt.getDocId() == null) {
                        variantInt.getTimeline().getIntimateAccess(this).setLiveHistory(new LiveHistoryEntry[0], variant.getUpdateCount());
                    } else {
                        variantInt.getTimeline().getIntimateAccess(this).setLiveHistory(loadLiveHistory(variant, variantInt, variantInt.getDocId(), conn), variant.getUpdateCount());
                    }
                }
                
                // check that if there is a pessimistic lock on the document variant, it is owned by the
                // current user. Also make that the lock record is selected with a lock so that
                // no one can create a lock while the document variant is being saved.
                LockInfo lockInfo = loadLock(docId, document.getBranchId(), document.getLanguageId(), conn, executeAfterCommit);
                if (lockInfo.hasLock() && lockInfo.getType() == LockType.PESSIMISTIC
                        && lockInfo.getUserId() != documentInt.getCurrentUser().getId()) {
                    String lockOwnerLogin = "";
                    try {
                        lockOwnerLogin = context.getCommonRepository().getUserManager().getUserLogin(lockInfo.getUserId());
                    } catch (Throwable e) {
                        // ignore
                    }
                    throw new DocumentLockedException(id, lockInfo.getUserId(), lockOwnerLogin);
                }
            }

            long lastVersionId = document.getLastVersionId();
            long previousLiveVersionId = document.getLiveVersionId();
            long liveVersionId = previousLiveVersionId;

            String summary = variant.getSummary();
            long variantNewUpdateCount = -1;
            
            TimelineImpl timeline = variantInt.getTimeline();
            TimelineImpl.IntimateAccess timelineInt = timeline.getIntimateAccess(this);

            boolean variantNeedsUpdate = false;
            if (newVersion) {
                variantNeedsUpdate = true;
                
                // store the various changes
                if (variantInt.hasCustomFieldChanges())
                    storeCustomFields(document, docId, conn);

                if (variantInt.hasCollectionChanges())
                    storeCollections(variant, docId, conn);
                
                boolean createNewVersion = variant.needsNewVersion();
                if (createNewVersion) {
                    checkChangeCommentSize(document.getNewChangeComment());

                    lastVersionId = createVersion(document, variantInt, docId, now, conn);
                    summary = storeSummary(conn, document, docId, lastVersionId);

                    // extract links
                    LinkExtractorHelper linkExtractorHelper = new LinkExtractorHelper(document, docId, null, systemUser, context);
                    Collection<LinkInfo> links = linkExtractorHelper.extract();
                    storeExtractedLinks(conn, docId, document.getBranchId(), document.getLanguageId(), lastVersionId, links);

                    partUpdates = storeParts(variant, variantInt, docId, lastVersionId, conn);
                    storeFields(document, docId, lastVersionId, conn);
                    storeLinks(document, docId, lastVersionId, conn);
                } else {
                    lastVersionId = variantInt.getLastVersionId();
                }
                
                if (document.getRequestedLiveVersionId() == 0) {
                    if (createNewVersion && (document.getNewLiveStrategy().equals(LiveStrategy.ALWAYS) ||
                            (document.getNewLiveStrategy().equals(LiveStrategy.DEFAULT) && document.getNewVersionState().equals(VersionState.PUBLISH)))) {
                        fixLiveHistory(timeline, now, lastVersionId);
                    }
                } else if (document.getRequestedLiveVersionId() == -2) {
                    fixLiveHistory(timeline, now, -1);
                } else if (document.getRequestedLiveVersionId() == -1) {
                    fixLiveHistory(timeline, now, lastVersionId);
                } else if (document.getRequestedLiveVersionId() > 0) {
                    if (document.getRequestedLiveVersionId() <= lastVersionId) { // note: the document model doesn't allow setting = lastVersionId if a new version was created
                        fixLiveHistory(timeline, now, document.getRequestedLiveVersionId());
                    }
                }
            }
            
            isTimelineChanged = timelineInt.hasChanges();
            if (isTimelineChanged) {
                variantNeedsUpdate = true;

                LiveHistoryEntry lhe = timeline.getLiveHistoryEntryAt(now);
                if (lhe == null) {
                    liveVersionId = -1;
                } else {
                    liveVersionId = lhe.getVersionId();
                }
                storeTimeline(variant, timeline.getLiveHistory(), docId, conn, now);

            }
            
            if (variantNeedsUpdate) {
                // store live history

                long lastMajorChangeVersionId = getLastMajorChangeVersionId(conn, docId, document.getBranchId(), document.getLanguageId());
                long liveMajorChangeVersionId = getLastPublishedMajorChangeVersionId(conn, docId, document.getBranchId(), document.getLanguageId());

                variantNewUpdateCount = variant.getUpdateCount() + 1;
                
                // update document_variants record
                stmt = conn.prepareStatement("update document_variants set last_modified = ?, last_modifier = ?, lastversion_id = ?, liveversion_id = ?, retired = ?, updatecount = ?, doctype_id = ?,"
                        + " last_major_change_version_id = ?, live_major_change_version_id = ?"
                        + " where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
                stmt.setTimestamp(1, new Timestamp(now.getTime()));
                stmt.setLong(2, documentInt.getCurrentUser().getId());
                jdbcHelper.setNullableIdField(stmt, 3, lastVersionId);
                jdbcHelper.setNullableIdField(stmt, 4, liveVersionId);
                stmt.setBoolean(5, document.isRetired());
                stmt.setLong(6, variantNewUpdateCount);
                stmt.setLong(7, document.getDocumentTypeId());
                jdbcHelper.setNullableIdField(stmt, 8, lastMajorChangeVersionId);
                jdbcHelper.setNullableIdField(stmt, 9, liveMajorChangeVersionId);
                stmt.setLong(10, docId.getSeqId());
                stmt.setLong(11, docId.getNsId());
                stmt.setLong(12, document.getBranchId());
                stmt.setLong(13, document.getLanguageId());
                
                stmt.executeUpdate();
                stmt.close();

                // store event record (this is used for guaranteed, asynchronous events)
                // This is done as part of the transaction to be sure that it is not lost: either
                // the document variant and the event record are stored, or both are not.
                XmlObject eventDescription;
                if (isVariantNew) {
                    eventDescription = createNewVariantEvent(document, docId, now, summary, variantNewUpdateCount, documentNewUpdateCount);
                    eventHelper.createEvent(eventDescription, "DocumentVariantCreated", conn);
                } else {
                    eventDescription = createVariantUpdatedEvent(oldDocument, document, now, summary, lastVersionId, liveVersionId, variantNewUpdateCount, documentNewUpdateCount);
                    eventHelper.createEvent(eventDescription, "DocumentVariantUpdated", conn);
                }
                if (timelineInt.hasChanges()) {
                    eventDescription = createTimelineUpdatedEvent(variant, docId, origHistory, variant.getLiveVersionId(), loadLiveHistory(variant, variantInt, docId, conn), liveVersionId);
                    eventHelper.createEvent(eventDescription, "DocumentVariantTimelineUpdated", conn);
                }
            }

            conn.commit();
            executeRunnables(executeAfterCommit);

            //
            // Now that everything's saved correctly, update document object status.
            // In case of partial write access, we might have been working with a full-access copy
            // of the document, in which case we need to switch back to the original objects.
            //
            if (origDocument != null) {
                // The part blobkeys are set on the object during the document storage
                // process, therefore copy them over to the original document.
                DocumentVariantImpl.IntimateAccess origVariantInt = origDocument.getIntimateAccess(this).getVariant().getIntimateAccess(this);
                for (PartImpl partImpl : variantInt.getPartImpls()) {
                    for (PartImpl origPartImpl : origVariantInt.getPartImpls()) {
                        if (origPartImpl.getTypeId() == partImpl.getTypeId()) {
                            if (!ObjectUtils.safeEquals(origPartImpl.getIntimateAccess(this).getBlobKey(),
                                    partImpl.getIntimateAccess(this).getBlobKey())) {
                                origPartImpl.getIntimateAccess(this).setBlobKey(partImpl.getIntimateAccess(this).getBlobKey());
                            }
                            break;
                        }
                    }
                }

                // switch the document related variables to point to the new 'full access' document
                document = origDocument;
                documentInt = document.getIntimateAccess(this);
                variant = documentInt.getVariant();
                variantInt = variant.getIntimateAccess(this);
            }

            if (variantNeedsUpdate) {
                variantInt.saved(lastVersionId, liveVersionId, now, summary, variantNewUpdateCount, loadLiveHistory(variant, variantInt, docId, conn));
            }
            
            if (isDocumentNew)
                documentInt.saved(docId, now, now, documentNewUpdateCount);
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            if (partUpdates != null) {
                for (PartUpdate partUpdate : partUpdates) {
                    partUpdate.rollback();
                }
            }
            if (e instanceof DocumentLockedException)
                throw (DocumentLockedException)e;
            else if (e instanceof AccessException)
                throw (AccessException)e;
            else
                throw new RepositoryException("Problem storing document.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }

        // fire synchronous events
        if (isDocumentNew)
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.DOCUMENT_CREATED, id, document.getUpdateCount());

        if (isVariantNew)
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.DOCUMENT_VARIANT_CREATED, variant.getKey(), variant.getUpdateCount());
        else
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.DOCUMENT_VARIANT_UPDATED, variant.getKey(), variant.getUpdateCount());
        
        if (isTimelineChanged)
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.DOCUMENT_VARIANT_TIMELINE_UPDATED, variant.getKey(), variant.getUpdateCount());
            
    }

    /**
     * check the document variant is really there and not modified by someone else
     * use lock clause to avoid others updating the document variant while we're at it.
     */
    private void checkVariantUpdateCountAndLock(
            DocumentImpl document, DocId docId,
            Connection conn) throws SQLException, RepositoryException,
            ConcurrentUpdateException {
        PreparedStatement stmt;
        stmt = conn.prepareStatement("select updatecount from document_variants where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ? " + jdbcHelper.getSharedLockClause());
        stmt.setLong(1, docId.getSeqId());
        stmt.setLong(2, docId.getNsId());
        stmt.setLong(3, document.getBranchId());
        stmt.setLong(4, document.getLanguageId());
        ResultSet rs = stmt.executeQuery();
        if (!rs.next()) {
            throw new RepositoryException("The document variant for " + document.getVariantKey() + " does not exist in the repository.");
        } else {
            long dbUpdateCount = rs.getLong(1);
            if (dbUpdateCount != document.getVariantUpdateCount())
                throw new ConcurrentUpdateException(Document.class.getName() + "-variant", document.getId() + "~" + getBranchName(document.getBranchId()) + "~" + getLanguageName(document.getLanguageId()));
        }
        rs.close();
        stmt.close();
    }

    /**
     * check the document is really there and not modified by someone else
     * use lock clause to avoid others updating the document while we're at it.
     */
    private void checkDocumentUpdateCountAndLock(DocumentImpl document,
            DocId docId, Connection conn) throws SQLException,
            RepositoryException, ConcurrentUpdateException {
        PreparedStatement stmt;
        // check the document is really there and not modified by someone else
        // use lock clause to avoid others updating the document while we're at it.
        stmt = conn.prepareStatement("select updatecount from documents where id = ? and ns_id = ? " + jdbcHelper.getSharedLockClause());
        stmt.setLong(1, docId.getSeqId());
        stmt.setLong(2, docId.getNsId());
        ResultSet rs = stmt.executeQuery();
        if (!rs.next()) {
            throw new RepositoryException("The document with ID " + docId + " does not exist in the repository.");
        } else {
            // Note that even if a user has an exclusive editing lock, this lock doesn't apply to the
            // document record and thus it is possible that saving will fail. However, in most frontend
            // applications the editing of a variant should be separate from the editing of shared document
            // properties, so that a user will not run into the aforementioned situation.
            long dbUpdateCount = rs.getLong(1);
            if (dbUpdateCount != document.getUpdateCount())
                throw new ConcurrentUpdateException(Document.class.getName(), document.getId());
        }
        rs.close();
        stmt.close();
    }

    private void fixLiveHistory(TimelineImpl timeline, Date now, long liveVersionId) throws RepositoryException {
        TimelineImpl.IntimateAccess timelineInt = timeline.getIntimateAccess(this);
        
        now = DateUtil.getNormalizedDate(now, true);
        LiveHistoryEntry[] liveHistory = timeline.getLiveHistory();

        int i = liveHistory.length - 1;
        while (i >= 0 && (liveHistory[i].getBeginDate().after(now))) {
            i--;
        }
        
        boolean needsNewEntry = true;
        if (i >= 0) {
            if (liveHistory[i].getEndDate() == null || liveHistory[i].getEndDate().after(now)) { // there is an entry covering 'now'
                if (liveHistory[i].getVersionId() != liveVersionId) { // close it if needed
                    timeline.deleteLiveHistoryEntry(liveHistory[i]);
                    if (now.after(liveHistory[i].getBeginDate())) { // avoid adding 0-length entries
                        timelineInt.addLiveHistoryEntry(liveHistory[i].getBeginDate(), now, liveHistory[i].getVersionId());
                    }
                } else { // the current entry covering 'now' is good
                    needsNewEntry = false;
                }
            }
        }
        
        if (liveVersionId > 0 && needsNewEntry) {
            Date endDate = null;
            if (i + 1 < liveHistory.length) {
                endDate = liveHistory[i + 1].getBeginDate();
            }
            timelineInt.addLiveHistoryEntry(now, endDate, liveVersionId);
        }
    }

    private void storeTimeline(DocumentVariantImpl variant, LiveHistoryEntry[] liveHistory, DocId docId, Connection conn, Date lastModified) throws SQLException, RepositoryException {
        PreparedStatement stmt = null;
        try {
            List<Long> myEntries = new ArrayList<Long>();
            stmt = conn.prepareStatement("select id from live_history where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, variant.getBranchId());
            stmt.setLong(4, variant.getLanguageId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                myEntries.add(rs.getLong("id"));
            }
            
            List<Long> toKeep = new ArrayList<Long>();
            for (LiveHistoryEntry entry: liveHistory) {
                if (entry.getId() != -1L) {
                    toKeep.add(entry.getId());
                }
            }
            
            for (Long id: myEntries) {
                stmt = conn.prepareStatement("delete from live_history where id = ?");
                if (!toKeep.contains(id)) {
                    stmt.setLong(1, id);
                    stmt.executeUpdate();
                }
            }
            
            for (LiveHistoryEntry entry: liveHistory) {
                stmt = conn.prepareStatement("insert into live_history(id, doc_id, ns_id, branch_id, lang_id, version_id, begin_date, end_date, created_on, created_by) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                if (entry.getId() == -1) {
                    Long newId = context.getNextLiveHistoryId();

                    stmt.setLong(1, newId);
                    stmt.setLong(2, docId.getSeqId());
                    stmt.setLong(3, docId.getNsId());
                    stmt.setLong(4, variant.getBranchId());
                    stmt.setLong(5, variant.getLanguageId());
                    stmt.setLong(6, entry.getVersionId());
                    stmt.setTimestamp(7, new Timestamp(entry.getBeginDate().getTime()));
                    if (entry.getEndDate() == null) {
                        stmt.setNull(8, Types.TIMESTAMP);
                    } else {
                        stmt.setTimestamp(8, new Timestamp(entry.getEndDate().getTime()));
                    }
                    stmt.setTimestamp(9, new Timestamp(lastModified.getTime()));
                    stmt.setLong(10, variant.getIntimateAccess(this).getCurrentUser().getId());
                    stmt.executeUpdate();
                }
            }
            
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }
    
    private void checkChangeCommentSize(String changeComment) throws RepositoryException {
        if (changeComment != null && changeComment.length() > 1023) {
            throw new RepositoryException("Version change comments are not allowed to exceed 1023 characters.");
        }
    }

    private void checkValidSyncedWith(Connection conn, DocId docId, long docBranchId, VersionKey newSyncedWith)
            throws RepositoryException, SQLException {
        if (newSyncedWith != null) {
            if (newSyncedWith.getLanguageId() == -1) {
                if (newSyncedWith.getVersionId() != -1)
                    throw new RepositoryException("language and version for syncedWith must both be equal to -1 or both be larger than 0");
            } else {
                long lastSyncedWithVersionId = getLastVersionId(conn, docId, docBranchId, newSyncedWith.getLanguageId());
                if (lastSyncedWithVersionId == 0 || newSyncedWith.getVersionId() < 0 || lastSyncedWithVersionId < newSyncedWith.getVersionId())
                    throw new RepositoryException("Synced with can not be set to non-existing version " + newSyncedWith);
            }
        }
    }

    private long getLastPublishedMajorChangeVersionId(Connection conn, DocId docId, long branchId, long languageId)
    throws SQLException {
        PreparedStatement stmt = null;
        try {
            long liveVersionId = getLastPublishedVersionId(conn, docId, branchId, languageId);
            
            stmt = conn.prepareStatement("select max(id) from document_versions where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ? and change_type = ? and id <= ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.setString(5, ChangeType.MAJOR.getCode());
            stmt.setLong(6, liveVersionId);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            long liveMajorChangeVersionId = jdbcHelper.getNullableIdField(rs, 1);
            return liveMajorChangeVersionId == 0 ? -1 : liveMajorChangeVersionId;
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private long getLastMajorChangeVersionId(Connection conn, DocId docId, long branchId, long languageId)
            throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select max(id) from document_versions where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ? and change_type = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.setString(5, ChangeType.MAJOR.getCode());
            ResultSet rs = stmt.executeQuery();
            rs.next();
            long lastMajorChangeVersionId = jdbcHelper.getNullableIdField(rs, 1);
            return lastMajorChangeVersionId == 0 ? -1 : lastMajorChangeVersionId;
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private void storeExtractedLinks(Connection conn, DocId docId, long branchId, long languageId, long versionId, Collection<LinkInfo> links) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("delete from extracted_links where source_doc_id = ? and source_ns_id = ? and source_branch_id = ? and source_lang_id = ? and source_version_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.setLong(5, versionId);
            stmt.execute();
            stmt.close();

            if (links.size() > 0) {
                stmt = conn.prepareStatement("insert into extracted_links(source_doc_id, source_ns_id, source_branch_id, source_lang_id, source_version_id, source_parttype_id, target_doc_id, target_ns_id, target_branch_id, target_lang_id, target_version_id, linktype) values(?,?,?,?,?,?,?,?,?,?,?,?)");

                for (LinkInfo linkInfo : links) {

                    stmt.setLong(1, linkInfo.sourceDocSeqId);
                    stmt.setLong(2, linkInfo.sourceDocNsId);
                    stmt.setLong(3, linkInfo.sourceBranchId);
                    stmt.setLong(4, linkInfo.sourceLanguageId);
                    stmt.setLong(5, versionId);
                    stmt.setLong(6, linkInfo.sourcePartTypeId);
                    stmt.setLong(7, linkInfo.targetDocSeqId);
                    stmt.setLong(8, linkInfo.targetDocNsId);
                    stmt.setLong(9, linkInfo.targetBranchId);
                    stmt.setLong(10, linkInfo.targetLanguageId);
                    stmt.setLong(11, linkInfo.targetVersionId);
                    stmt.setString(12, linkInfo.linkType.getCode());

                    stmt.addBatch();
                }

                stmt.executeBatch();
                stmt.close();
            }
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    /**
     *
     * @param versionId live version id, or -1 if the live-version-to-be is the data in the document object
     */
    private String storeSummary(Connection conn, DocumentImpl document, DocId docId, long versionId) throws SQLException {
        PreparedStatement stmt = null;
        try {
            String summary = null;
            try {
                AuthenticatedUser user = document.getIntimateAccess(this).getCurrentUser();
                summary = context.getDocumentSummarizer().getSummary(document, -1, new RepositorySchemaImpl(context.getRepositorySchema(), user));
            } catch (Exception e) {
                logger.error("Error creating summary for document ID " + docId + ", branch " + getBranchLabel(document.getBranchId()) + ", language " + getLanguageLabel(document.getLanguageId()), e);
            }

            if (summary != null) {
                stmt = conn.prepareStatement("insert into summaries(doc_id, ns_id, branch_id, lang_id, version_id, summary) values(?,?,?,?,?,?)");
                stmt.setLong(1, docId.getSeqId());
                stmt.setLong(2, docId.getNsId());
                stmt.setLong(3, document.getBranchId());
                stmt.setLong(4, document.getLanguageId());
                stmt.setLong(5, versionId);
                stmt.setString(6, summary);
                stmt.execute();
                stmt.close();
            }

            return summary;
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private void storeCollections(DocumentVariantImpl variant, DocId docId, Connection conn) throws SQLException, RepositoryException {
        // first we need to remove this document variant from all the collections it belongs to
        clearCollections(docId, variant.getBranchId(), variant.getLanguageId(), conn);
        // now we can add the document variant to its updated set of collections
        addDocumentToCollections(variant.getIntimateAccess(this).getDocumentCollectionImpls(), docId, variant.getBranchId(), variant.getLanguageId(), conn);
    }

    private void clearCollections(DocId docId, long branchId, long languageId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement("delete from document_collections where document_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.executeUpdate();
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private long createVersion(DocumentImpl document, DocumentVariantImpl.IntimateAccess variantInt, DocId docId,
                               Date now, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            long newVersionId = 1;
            
            if (!document.isNew()) {
                // get the number of the previous last version
                stmt = conn.prepareStatement("select max(id) from document_versions where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
                stmt.setLong(1, docId.getSeqId());
                stmt.setLong(2, docId.getNsId());
                stmt.setLong(3, document.getBranchId());
                stmt.setLong(4, document.getLanguageId());
                ResultSet rs = stmt.executeQuery();
                rs.next();
                newVersionId = rs.getLong(1) + 1;
                rs.close();
                stmt.close();
            }

            long totalPartSize = 0L;
            PartImpl[] parts = variantInt.getPartImpls();
            for (PartImpl part : parts)
                totalPartSize += part.getSize();
            
            long userId = variantInt.getCurrentUser().getId();
            Timestamp created = new Timestamp(now.getTime());
            stmt = conn.prepareStatement("insert into document_versions(id, doc_id, ns_id, branch_id, lang_id, name, created_on, created_by, state, synced_with_lang_id, synced_with_version_id, synced_with_search, change_type, change_comment, last_modified, last_modifier, total_size_of_parts) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            stmt.setLong(1, newVersionId);
            stmt.setLong(2, docId.getSeqId());
            stmt.setLong(3, docId.getNsId());
            stmt.setLong(4, document.getBranchId());
            stmt.setLong(5, document.getLanguageId());
            stmt.setString(6, document.getName());
            stmt.setTimestamp(7, created);
            stmt.setLong(8, userId);
            stmt.setString(9, document.getNewVersionState().getCode());
            VersionKey syncedWith = document.getNewSyncedWith();
            jdbcHelper.setNullableIdField(stmt, 10, syncedWith == null ? -1 : syncedWith.getLanguageId());
            jdbcHelper.setNullableIdField(stmt, 11, syncedWith == null ? -1 : syncedWith.getVersionId());
            if (syncedWith != null)
                stmt.setString(12, getLinkSearchString(docId.getSeqId(), docId.getNsId(), document.getBranchId(), syncedWith.getLanguageId()));
            else
                stmt.setNull(12, Types.VARCHAR);
            stmt.setString(13, document.getNewChangeType().getCode());
            stmt.setString(14, document.getNewChangeComment());
            stmt.setTimestamp(15, created);
            stmt.setLong(16, userId);
            stmt.setLong(17, totalPartSize);
            stmt.execute();
            stmt.close();

            return newVersionId;
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private void storeCustomFields(DocumentImpl document, DocId docId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            if (!document.isNew()) {
                stmt = conn.prepareStatement("delete from customfields where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
                stmt.setLong(1, docId.getSeqId());
                stmt.setLong(2, docId.getNsId());
                stmt.setLong(3, document.getBranchId());
                stmt.setLong(4, document.getLanguageId());
                stmt.execute();
                stmt.close();
            }

            for (Map.Entry<String, String> entry : document.getCustomFields().entrySet()) {
                stmt = conn.prepareStatement("insert into customfields(doc_id, ns_id, branch_id, lang_id, name, value) values(?,?,?,?,?,?)");
                stmt.setLong(1, docId.getSeqId());
                stmt.setLong(2, docId.getNsId());
                stmt.setLong(3, document.getBranchId());
                stmt.setLong(4, document.getLanguageId());
                stmt.setString(5, entry.getKey());
                stmt.setString(6, entry.getValue());
                stmt.execute();
            }
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private List<PartUpdate> storeParts(DocumentVariantImpl variant, DocumentVariantImpl.IntimateAccess variantInt, DocId docId, long versionId, Connection conn) throws SQLException, RepositoryException {
        PreparedStatement stmt = null;
        try {
            PartImpl[] parts = variantInt.getPartImpls();
            List<PartUpdate> partUpdates = new ArrayList<PartUpdate>(parts.length);
            for (PartImpl part : parts) {
                PartImpl.IntimateAccess partInt = part.getIntimateAccess(this);
                String oldBlobKey = partInt.getBlobKey();
                String blobKey = partInt.getBlobKey();
                long changedInVersion = part.getDataChangedInVersion();
                if (partInt.isDataUpdated()) {
                    // first store the blob
                    try {
                        PartDataSource partDataSource = partInt.getPartDataSource();
                        blobKey = context.getBlobStore().store(partDataSource.createInputStream());
                    } catch (Exception e) {
                        throw new RepositoryException("Error storing part data to blobstore.", e);
                    }
                    partUpdates.add(new PartUpdate(partInt, blobKey, oldBlobKey));
                    // we update the object model here already, even though the transaction is not yet
                    // committed, so that other components like the link extractor and the summarizer can
                    // read out the new data. This change will be rolled back if the transaction changes
                    // (see PartUpdate.rollback())
                    partInt.setBlobKey(blobKey);
                    changedInVersion = versionId;
                }

                // insert a record
                if (stmt == null) {
                    stmt = conn.prepareStatement("insert into parts(doc_id, ns_id, branch_id, lang_id, version_id, blob_id, mimetype, filename, parttype_id, blob_size, changed_in_version) values(?,?,?,?,?,?,?,?,?,?,?)");
                }

                stmt.setLong(1, docId.getSeqId());
                stmt.setLong(2, docId.getNsId());
                stmt.setLong(3, variant.getBranchId());
                stmt.setLong(4, variant.getLanguageId());
                stmt.setLong(5, versionId);
                stmt.setString(6, blobKey);
                stmt.setString(7, part.getMimeType());
                stmt.setString(8, part.getFileName());
                stmt.setLong(9, part.getTypeId());
                stmt.setLong(10, part.getSize());
                stmt.setLong(11, changedInVersion);
                stmt.execute();
            }
            return partUpdates;
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private class PartUpdate {
        private final PartImpl.IntimateAccess partInt;
        private final String blobKey;
        private final String oldBlobKey;
        private final PartDataSource partDataSource;

        public PartUpdate(PartImpl.IntimateAccess partInt, String blobKey, String oldBlobKey) {
            this.partInt = partInt;
            this.blobKey = blobKey;
            this.oldBlobKey = oldBlobKey;
            this.partDataSource = partInt.getPartDataSource();
            partInt.setData(null);
        }

        public void rollback() {
            partInt.setBlobKey(oldBlobKey);
            partInt.setData(partDataSource);
            try {
                context.getBlobStore().delete(blobKey);
            } catch (Throwable e) {
                logger.error("Problem removing blob \"" + blobKey + "\" after failed document update. Ignoring.", e);
            }
        }
    }

    private void storeFields(DocumentImpl document, DocId docId, long versionId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            // insert new records
            stmt = conn.prepareStatement("insert into thefields(doc_id, ns_id, branch_id, lang_id, version_id, fieldtype_id, value_seq, value_count, hier_seq, hier_count, stringvalue, datevalue, datetimevalue, integervalue, floatvalue, decimalvalue, booleanvalue, link_docid, link_nsid, link_searchdocid, link_branchid, link_searchbranchid, link_langid, link_searchlangid, link_search) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, document.getBranchId());
            stmt.setLong(4, document.getLanguageId());
            stmt.setLong(5, versionId);
            Field[] fields = document.getFields().getArray();
            // Run over the fields ...
            for (Field field : fields) {
                stmt.setLong(6, field.getTypeId());

                ValueType fieldType = field.getValueType();
                Object[] multiValues = field.isMultiValue() ? (Object[])field.getValue() : new Object[]{field.getValue()};

                stmt.setLong(8, multiValues.length); // value_count

                // Run over the multi-values ...
                for (int k = 0; k < multiValues.length; k++) {
                    Object valueFromMultiValue = multiValues[k];
                    Object[] pathValues = field.isHierarchical() ? ((HierarchyPath)valueFromMultiValue).getElements() : new Object[]{valueFromMultiValue};

                    stmt.setInt(7, k + 1); // value_seq
                    stmt.setLong(10, pathValues.length); // hier_count

                    // Run over the hierarhical path ...
                    for (int p = 0; p < pathValues.length; p++) {
                        Object value = pathValues[p];

                        String stringValue = (String)(fieldType == ValueType.STRING ? value : null);
                        Date dateValue = (fieldType == ValueType.DATE ? new java.sql.Date(((Date)value).getTime()) : null);
                        Timestamp datetimeValue = (fieldType == ValueType.DATETIME ? new Timestamp(((Date)value).getTime()) : null);
                        Long integerValue = (Long)(fieldType == ValueType.LONG ? value : null);
                        Double floatValue = (Double)(fieldType == ValueType.DOUBLE ? value : null);
                        BigDecimal decimalValue = (BigDecimal)(fieldType == ValueType.DECIMAL ? value : null);
                        Boolean booleanValue = (Boolean)(fieldType == ValueType.BOOLEAN ? value : null);

                        stmt.setInt(9, p + 1); // hier_seq
                        stmt.setString(11, stringValue);
                        stmt.setObject(12, dateValue);
                        stmt.setObject(13, datetimeValue);
                        stmt.setObject(14, integerValue);
                        stmt.setObject(15, floatValue);
                        stmt.setBigDecimal(16, decimalValue);
                        stmt.setObject(17, booleanValue);

                        if (fieldType == ValueType.LINK) {
                            VariantKey variantKey = (VariantKey)value;
                            long branchId = variantKey.getBranchId() == -1 ? document.getBranchId() : variantKey.getBranchId();
                            long languageId = variantKey.getLanguageId() == -1 ? document.getLanguageId() : variantKey.getLanguageId();

                            DocId linkDocId = DocId.parseDocId(variantKey.getDocumentId(), context.getCommonRepository());

                            stmt.setLong(18, linkDocId.getSeqId());
                            stmt.setLong(19, linkDocId.getNsId());
                            stmt.setString(20, linkDocId.getSeqId() + "-" + linkDocId.getNsId());
                            stmt.setLong(21, variantKey.getBranchId());
                            stmt.setLong(22, branchId);
                            stmt.setLong(23, variantKey.getLanguageId());
                            stmt.setLong(24, languageId);
                            stmt.setString(25, getLinkSearchString(linkDocId.getSeqId(), linkDocId.getNsId(), branchId, languageId));
                        } else {
                            stmt.setNull(18, Types.BIGINT);
                            stmt.setNull(19, Types.BIGINT);
                            stmt.setNull(20, Types.VARCHAR);
                            stmt.setNull(21, Types.BIGINT);
                            stmt.setNull(22, Types.BIGINT);
                            stmt.setNull(23, Types.BIGINT);
                            stmt.setNull(24, Types.BIGINT);
                            stmt.setNull(25, Types.VARCHAR);
                        }

                        stmt.execute();
                    }
                }
            }
            stmt.close();
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private String getLinkSearchString(long seqId, long nsId, long branchId, long langId) {
        return seqId + "-" + nsId + "@" + branchId + ":" + langId;
    }

    private String getVariantSearchString(long branchId, long langId) {
        return branchId + ":" + langId;
    }

    private void storeLinks(DocumentImpl document, DocId docId, long versionId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("insert into links(id, doc_id, ns_id, branch_id, lang_id, version_id, title, target) values (?,?,?,?,?,?,?,?)");
            stmt.setLong(2, docId.getSeqId());
            stmt.setLong(3, docId.getNsId());
            stmt.setLong(4, document.getBranchId());
            stmt.setLong(5, document.getLanguageId());
            stmt.setLong(6, versionId);
            Link[] links = document.getLinks().getArray();
            for (int i = 0; i < links.length; i++) {
                stmt.setLong(1, i);
                stmt.setString(7, links[i].getTitle());
                stmt.setString(8, links[i].getTarget());
                stmt.execute();
            }
            stmt.close();
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    public InputStream getBlob(DocId docId, long branchId, long languageId, long versionId, long partTypeId, AuthenticatedUser user) throws RepositoryException {
        Document document = context.getCommonRepository().getDocument(docId, branchId, languageId, false, user);
        return document.getVersion(versionId).getPart(partTypeId).getDataStream();
    }

    public InputStream getBlob(String blobKey) throws RepositoryException {
        try {
            return context.getBlobStore().retrieve(blobKey);
        } catch (BlobIOException e) {
            throw new RepositoryException("Problem retrieving blob data.", e);
        } catch (NonExistingBlobException e) {
            throw new RepositoryException("Problem retrieving blob data.", e);
        }
    }

    public VersionImpl[] loadShallowVersions(DocumentVariantImpl variant) throws RepositoryException {
        DocumentVariantImpl.IntimateAccess variantInt = variant.getIntimateAccess(this);
        DocId docId = variantInt.getDocument().getIntimateAccess(this).getDocId();
        VersionImpl[] versions = new VersionImpl[(int)variantInt.getLastVersionId()];

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            stmt = conn.prepareStatement("select v.id id, created_on, created_by, name, state, synced_with_lang_id, synced_with_version_id, change_type, change_comment, last_modified, last_modifier, total_size_of_parts, s.summary from document_versions v" +
            		" left join summaries s on v.ns_id = s.ns_id and v.doc_id = s.doc_id and v.branch_id = s.branch_id and s.lang_id = v.lang_id and v.id = s.version_id" +
            		" where v.doc_id = ? and v.ns_id = ? and v.branch_id = ? and v.lang_id = ? order by id ASC");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNamespaceId());
            stmt.setLong(3, variant.getBranchId());
            stmt.setLong(4, variant.getLanguageId());
            ResultSet rs = stmt.executeQuery();

            for (int i = 0; i < versions.length; i++) {
                rs.next();
                versions[i] = loadShallowVersionFromResultSet(rs, docId, variant);
            }
            stmt.close();
        } catch (Throwable e) {
            throw new RepositoryException("Error loading versions (document ID: " + variant.getDocumentId() + ", branch: " + getBranchLabel(variant.getBranchId()) + ", language: " + getLanguageLabel(variant.getLanguageId()) + ").", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }

        return versions;
    }

    public VersionImpl loadVersion(DocumentVariantImpl variant, long versionId) throws RepositoryException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            return loadVersionInTransaction(conn, variant, versionId);
        } catch (Throwable e) {
            throw new RepositoryException("Error loading version (" + getFormattedVariant(variant.getKey(), versionId) + ").", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    private VersionImpl loadVersionInTransaction(Connection conn, DocumentVariantImpl variant, long versionId) throws RepositoryException {
        DocumentVariantImpl.IntimateAccess variantInt = variant.getIntimateAccess(this);
        DocId docId = variantInt.getDocument().getIntimateAccess(this).getDocId();

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select v.id, created_on, created_by, name, state, synced_with_lang_id, synced_with_version_id, change_type, change_comment, last_modified, last_modifier, total_size_of_parts, s.summary from document_versions v" +
            		" left join summaries s on v.doc_id = s.doc_id and v.ns_id = s.ns_id and v.branch_id = s.branch_id and v.lang_id = s.lang_id and v.id = s.version_id" +
            		" where v.id = ? and v.doc_id = ? and v.ns_id = ? and v.branch_id = ? and v.lang_id = ?");
            stmt.setLong(1, versionId);
            stmt.setLong(2, docId.getSeqId());
            stmt.setLong(3, docId.getNsId());
            stmt.setLong(4, variant.getBranchId());
            stmt.setLong(5, variant.getLanguageId());

            ResultSet rs = stmt.executeQuery();
            if (!rs.next())
                throw new RepositoryException("Unexpected problem: record for version not found (" + getFormattedVariant(variant.getKey(), versionId) + ").");

            VersionImpl versionImpl = loadShallowVersionFromResultSet(rs, docId, variant);
            stmt.close();

            VersionImpl.IntimateAccess versionInt = versionImpl.getIntimateAccess(this);

            versionInt.setFields(loadFields(variant, docId, versionId, conn).toArray(new FieldImpl[0]));
            versionInt.setParts(loadParts(variant, variantInt, docId, versionId, conn).toArray(new PartImpl[0]));
            versionInt.setLinks(loadLinks(variant, docId, versionId, conn).toArray(new LinkImpl[0]));

            return versionImpl;
        } catch (Throwable e) {
            throw new RepositoryException("Error loading version (" + getFormattedVariant(variant.getKey(), versionId) + ").", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private VersionImpl loadShallowVersionFromResultSet(ResultSet rs, DocId docId, DocumentVariantImpl variant) throws SQLException {
        DocumentVariantImpl.IntimateAccess variantInt = variant.getIntimateAccess(this);

        Date createdOn = rs.getTimestamp("created_on");
        long createdBy = rs.getLong("created_by");
        String documentName = rs.getString("name");
        long versionId = rs.getLong("id");
        VersionState versionState = VersionState.getByCode(rs.getString("state"));

        long syncedWithLanguageId = jdbcHelper.getNullableIdField(rs, "synced_with_lang_id");
        long syncedWithVersionId = jdbcHelper.getNullableIdField(rs, "synced_with_version_id");
        VersionKey syncedWith = null;
        if (syncedWithLanguageId != -1 && syncedWithVersionId != -1)
            syncedWith = new VersionKey(docId.toString(), variant.getBranchId(), syncedWithLanguageId, syncedWithVersionId);

        ChangeType changeType = ChangeType.getByCode(rs.getString("change_type"));
        String changeComment = rs.getString("change_comment");

        Date lastModified = rs.getTimestamp("last_modified");
        long lastModifier = rs.getLong("last_modifier");
        long totalSizeOfParts = rs.getLong("total_size_of_parts");
        
        String summary = rs.getString("summary");

        return new VersionImpl(variantInt, versionId, documentName, createdOn, createdBy,
                versionState, syncedWith, changeType, changeComment, lastModified, lastModifier, totalSizeOfParts, summary);
    }

    public void completeVersion(DocumentVariantImpl variant, VersionImpl version) throws RepositoryException {
        DocumentVariantImpl.IntimateAccess variantInt = variant.getIntimateAccess(this);
        DocId docId = variantInt.getDocument().getIntimateAccess(this).getDocId();

        VersionImpl.IntimateAccess versionInt = version.getIntimateAccess(this);
        Connection conn = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            PartImpl[] parts = loadParts(variant, variantInt, docId, version.getId(), conn).toArray(new PartImpl[0]);
            FieldImpl[] fields = loadFields(variant, docId, version.getId(), conn).toArray(new FieldImpl[0]);
            LinkImpl[] links = loadLinks(variant, docId, version.getId(), conn).toArray(new LinkImpl[0]);

            versionInt.setParts(parts);
            versionInt.setFields(fields);
            versionInt.setLinks(links);
        } catch (Throwable e) {
            throw new RepositoryException("Error loading version (" + getFormattedVariant(variant.getKey(), version.getId()) + ").", e);
        } finally {
            jdbcHelper.closeConnection(conn);
        }
    }
    
    public void reExtractLinks(Document document, Date beginDate, Date endDate) throws RepositoryException {
        Connection conn = null;
        PreparedStatement stmt = null;
        DocumentImpl documentImpl;

        if (document instanceof AbstractDocumentWrapper) {
            documentImpl = ((AbstractDocumentWrapper)document).getWrappedDocument(this);
        } else {
            documentImpl = (DocumentImpl)document;
        }
        
        DocId docId = documentImpl.getIntimateAccess(this).getDocId();

        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement("select doc_id, ns_id, branch_id, lang_id from document_variants " + jdbcHelper.getSharedLockClause());
            stmt.execute();
            
            long branchId = document.getBranchId();
            long languageId = document.getLanguageId();
            
            for (Version version: document.getVersions().getArray()) {
                if (beginDate != null && version.getCreated().before(beginDate))
                    continue;
                
                if (endDate != null && endDate.before(version.getCreated()))
                    continue;
                
                LinkExtractorHelper helper = new LinkExtractorHelper(document, docId, version, systemUser, context);
                Collection<LinkInfo> links = helper.extract();
                
                storeExtractedLinks(conn, docId, branchId, languageId, version.getId(), links);
                
            }
            
            conn.commit();
        } catch (Exception e) {
            jdbcHelper.rollback(conn);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public void storeVersion(DocumentImpl document, VersionImpl version, VersionState versionState, VersionKey syncedWith, ChangeType changeType, String changeComment) throws RepositoryException {
        if (logger.isDebugEnabled()) {
            logger.debug("setting version properties for " + version.getDocumentName() + "(" + version + ")");
        }
        checkChangeCommentSize(changeComment);

        DocumentImpl.IntimateAccess documentInt = document.getIntimateAccess(this);
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            DocId docId = documentInt.getDocId();

            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);
            
            // lock the document variant while we're doing this
            stmt = conn.prepareStatement("select last_modified from document_variants where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ? " + jdbcHelper.getSharedLockClause());
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, document.getBranchId());
            stmt.setLong(4, document.getLanguageId());
            stmt.execute();
            stmt.close();

            // Get the old version, this is our last chance to do it.
            DocumentVariantImpl variant = documentInt.getVariant();
            VersionImpl oldVersion = loadVersionInTransaction(conn, variant, version.getId());

            // Check what has changed, don't forget to update this when new version attributes are introduced!
            boolean stateChanged = oldVersion.getState() != version.getState();
            boolean attributesChanged = !ObjectUtils.safeEquals(oldVersion.getChangeComment(), version.getChangeComment())
                    || !ObjectUtils.safeEquals(oldVersion.getChangeType(), version.getChangeType())
                    || !ObjectUtils.safeEquals(oldVersion.getSyncedWith(), version.getSyncedWith());

            // check ACL.
            AclResultInfo aclInfo = context.getCommonRepository().getAccessManager().getAclInfoOnLive(systemUser,
                    documentInt.getCurrentUser().getId(), documentInt.getCurrentUser().getActiveRoleIds(),
                   documentInt.getDocId(), document.getBranchId(), document.getLanguageId());

            if (stateChanged && !aclInfo.isAllowed(AclPermission.PUBLISH)) {
                throw new AccessException("User " + labelUtil.getUserLabel(documentInt.getCurrentUser())
                        + " is not allowed to change the state of versions of document " + document.getId()
                        + ", branch " + labelUtil.getBranchLabel(document.getBranchId()) + ", language "
                        + labelUtil.getLanguageLabel(document.getLanguageId()));
            }

            if (attributesChanged
                    && (
                        !aclInfo.isAllowed(AclPermission.WRITE)
                        ||
                        (aclInfo.isAllowed(AclPermission.WRITE) && !aclInfo.getAccessDetails(AclPermission.WRITE).isGranted(AclDetailPermission.VERSION_META))
                    )) {
                throw new AccessException("User " + labelUtil.getUserLabel(documentInt.getCurrentUser())
                        + " is not allowed to update the version metadata for document " + document.getId()
                        + ", branch " + labelUtil.getBranchLabel(document.getBranchId()) + ", language "
                        + labelUtil.getLanguageLabel(document.getLanguageId()));
            }

            checkValidSyncedWith(conn, docId, document.getBranchId(), syncedWith);

            Date lastModified = new Date();
            long lastModifier = documentInt.getCurrentUser().getId();
            // update document_versions record
            stmt = conn.prepareStatement("update document_versions set state = ?, synced_with_lang_id = ?, synced_with_version_id = ?, synced_with_search = ?, change_type = ?, change_comment = ?, " +
            		" last_modified = ?, last_modifier = ? where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ? and id = ?");
            stmt.setString(1, versionState.getCode());
            jdbcHelper.setNullableIdField(stmt, 2, syncedWith != null ? syncedWith.getLanguageId() : -1);
            jdbcHelper.setNullableIdField(stmt, 3, syncedWith != null ? syncedWith.getVersionId() : -1);
            if (syncedWith != null)
                stmt.setString(4, getLinkSearchString(docId.getSeqId(), docId.getNsId(), document.getBranchId(), syncedWith.getLanguageId()));
            else
                stmt.setNull(4, Types.VARCHAR);
            stmt.setString(5, changeType.getCode());
            stmt.setString(6, changeComment);
            stmt.setTimestamp(7, new Timestamp(lastModified.getTime()));
            stmt.setLong(8, lastModifier);
            stmt.setLong(9, docId.getSeqId());
            stmt.setLong(10, docId.getNsId());
            stmt.setLong(11, document.getBranchId());
            stmt.setLong(12, document.getLanguageId());
            stmt.setLong(13, version.getId());
            long updateCount = stmt.executeUpdate();
            if (updateCount != 1)
                throw new RepositoryException("Assertion failed: wrong number of rows updated (when updating state):" + updateCount);
            stmt.close();

            long liveVersionId = getCurrentLiveVersionId(conn, docId, document.getBranchId(), document.getLanguageId(), lastModified);
            long lastMajorChangeVersionId = getLastMajorChangeVersionId(conn, docId, document.getBranchId(), document.getLanguageId());
            long liveMajorChangeVersionId = getLastPublishedMajorChangeVersionId(conn, docId, document.getBranchId(), document.getLanguageId());
            
            stmt = conn.prepareStatement("update document_variants set liveversion_id = ?, last_major_change_version_id = ?," +
                    " live_major_change_version_id = ? where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            jdbcHelper.setNullableIdField(stmt, 1, liveVersionId);
            jdbcHelper.setNullableIdField(stmt, 2, lastMajorChangeVersionId);
            jdbcHelper.setNullableIdField(stmt, 3, liveMajorChangeVersionId);
            stmt.setLong(4, docId.getSeqId());
            stmt.setLong(5, docId.getNsId());
            stmt.setLong(6, document.getBranchId());
            stmt.setLong(7, document.getLanguageId());
            updateCount = stmt.executeUpdate();
            if (updateCount != 1)
                throw new RepositoryException("Assertion failed: wrong number of rows updated (when updating live version id): " + updateCount);
            stmt.close();
            
            // insert an event record in the events table describing the change we've done
            VersionUpdatedDocument versionUpdatedDocument = createVersionUpdatedDocument(document, oldVersion, version, lastModified, lastModifier, versionState, syncedWith, changeType, changeComment);
            eventHelper.createEvent(versionUpdatedDocument, "VersionUpdated", conn);

            conn.commit();

            version.getIntimateAccess(this).stateChanged(versionState, syncedWith, changeType, changeComment, lastModified, lastModifier);
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            if (e instanceof AccessException)
                throw (AccessException)e;
            throw new RepositoryException("Error trying to change version state.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.VERSION_UPDATED, document.getVariantKey(), -1);
    }

    private VersionUpdatedDocument createVersionUpdatedDocument(DocumentImpl document, VersionImpl oldVersion,
            VersionImpl version, Date lastModified, long lastModifier, VersionState versionState, 
            VersionKey syncedWith, ChangeType changeType, String changeComment) {
        VersionUpdatedDocument versionUpdatedDocument = VersionUpdatedDocument.Factory.newInstance();
        VersionUpdatedDocument.VersionUpdated versionUpdated = versionUpdatedDocument.addNewVersionUpdated();

        versionUpdated.setDocumentId(document.getId());
        versionUpdated.setBranchId(document.getBranchId());
        versionUpdated.setLanguageId(document.getLanguageId());
        
        versionUpdated.addNewOldVersion().setVersion(oldVersion.getShallowXml().getVersion());
        
        VersionDocument.Version newVersionXml = version.getShallowXml().getVersion();
        updateUpdatedVersionXml(newVersionXml, lastModified, lastModifier, versionState, syncedWith, changeType, changeComment);
        versionUpdated.addNewNewVersion().setVersion(newVersionXml);
        
        return versionUpdatedDocument;
    }

    /**
     * Updates the version XML so as if it would look if we retrieved the XML of the document after saving it.
     */
    private void updateUpdatedVersionXml(VersionDocument.Version versionXml, Date lastModified, long lastModifier, VersionState versionState, 
            VersionKey syncedWith, ChangeType changeType, String changeComment) {
        versionXml.setLastModified(getCalendar(lastModified));
        versionXml.setLastModifier(lastModifier);
        versionXml.setState(versionState.toString());
        if (syncedWith != null) {
            versionXml.setSyncedWithLanguageId(-1);
            versionXml.setSyncedWithVersionId(-1);
        } else {
            // we must explicitly clear it because it has been set before
            if (versionXml.isSetSyncedWithLanguageId()) {
                versionXml.unsetSyncedWithLanguageId();
            }
            if (versionXml.isSetSyncedWithVersionId()) {
                versionXml.unsetSyncedWithVersionId();
            }
        }
        versionXml.setChangeType(changeType.toString());
        if (changeComment != null) {
            versionXml.setChangeComment(changeComment);
        }
    }

    /**
     * @since 2.4
     * @return the live version id according to the new definition of 'live' (the version for which begin_date <= now < end_date) 
     */
    private long getCurrentLiveVersionId(Connection conn, DocId docId, long branchId, long languageId, Date currentDate) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select lh.version_id version_id from live_history lh" +
            		" left join document_versions dv on lh.doc_id = dv.doc_id and lh.ns_id = dv.doc_id and lh.branch_id = dv.branch_id and lh.lang_id = dv.lang_id and lh.version_id = dv.id" +
            		" where lh.doc_id = ? and lh.ns_id = ? and lh.branch_id = ? and lh.lang_id = ? and begin_date <= ? and (end_date is null or end_date > ?)");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.setTimestamp(5, new Timestamp(currentDate.getTime()));
            stmt.setTimestamp(6, new Timestamp(currentDate.getTime()));
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                return -1;
            } else {
                return rs.getLong("version_id");
            }
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    /**
     * @return the live version id according to the old definition of 'live' (newest version with state P)
     */
    private long getLastPublishedVersionId(Connection conn, DocId docId, long branchId, long languageId) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select max(id) from document_versions where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ? and state = 'P'");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return jdbcHelper.getNullableIdField(rs, 1);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private long getLastVersionId(Connection conn, DocId docId, long branchId, long languageId) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select max(id) from document_versions where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return jdbcHelper.getNullableIdField(rs, 1);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    public LockInfoImpl lock(DocumentVariantImpl variant, long duration, LockType lockType) throws RepositoryException {
        DocumentVariantImpl.IntimateAccess variantInt = variant.getIntimateAccess(this);
        DocId docId = variantInt.getDocument().getIntimateAccess(this).getDocId();

        // Check if user has write access or live_history publish access
        AclResultInfo aclInfo = context.getCommonRepository().getAccessManager().getAclInfoOnLive(systemUser,
                variantInt.getCurrentUser().getId(), variantInt.getCurrentUser().getActiveRoleIds(),
                docId, variant.getBranchId(), variant.getLanguageId());
        boolean canWrite = aclInfo.isAllowed(AclPermission.WRITE);
        boolean canPublish = aclInfo.isAllowed(AclPermission.PUBLISH);
        AccessDetails publishDetails = aclInfo.getAccessDetails(AclPermission.PUBLISH);
        if (!canWrite && !(canPublish && (publishDetails == null || publishDetails.isGranted(AclDetailPermission.LIVE_HISTORY))))
            throw new AccessException("Write access denied for user " + variantInt.getCurrentUser().getId() + " to " + getFormattedVariant(variant.getKey()));

        List<Runnable> executeAfterCommit = new ArrayList<Runnable>(2);
        Connection conn = null;
        PreparedStatement stmt = null;
        LockInfoImpl lockInfo = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            // lock the database record for the document variant so that the document variant cannot be saved while
            // a lock is being taken
            stmt = conn.prepareStatement("select doc_id from document_variants where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ? " + jdbcHelper.getSharedLockClause());
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, variant.getBranchId());
            stmt.setLong(4, variant.getLanguageId());
            stmt.execute();
            stmt.close();

            // first check if there is already a lock on this document variant
            lockInfo = loadLock(docId, variant.getBranchId(), variant.getLanguageId(), conn, executeAfterCommit);

            // if there is a pessimistic lock belonging to another user, we cannot override it.
            // In that case information about the current lock is returned.
            if (lockInfo.hasLock() && lockInfo.getType() == LockType.PESSIMISTIC
                    && lockInfo.getUserId() != variantInt.getCurrentUser().getId()) {
                return lockInfo;
            }

            if (lockInfo.hasLock()) {
                // delete current lock record
                stmt = conn.prepareStatement("delete from locks where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
                stmt.setLong(1, docId.getSeqId());
                stmt.setLong(2, docId.getNsId());
                stmt.setLong(3, variant.getBranchId());
                stmt.setLong(4, variant.getLanguageId());
                stmt.execute();
                stmt.close();
            }


            lockInfo = new LockInfoImpl(variantInt.getCurrentUser().getId(), new Date(), duration, lockType);

            // Test lock expire time fits in date storage column
            if (lockInfo.getDuration() >= 0) {
                long rest = Long.MAX_VALUE - lockInfo.getTimeAcquired().getTime();
                if (lockInfo.getDuration() >= rest)
                    throw new RepositoryException("Lock duration too long to fit in storage.");
                GregorianCalendar expires = new GregorianCalendar();
                expires.setTimeInMillis(lockInfo.getTimeAcquired().getTime() + lockInfo.getDuration());
                if (expires.get(Calendar.YEAR) > 9999)
                    throw new RepositoryException("Lock duration too long: expiry date falls after the year 9999");
            }

            // insert the new lock
            stmt = conn.prepareStatement("insert into locks(doc_id, ns_id, branch_id, lang_id, user_id, locktype, time_acquired, duration, time_expires) values(?,?,?,?,?,?,?,?,?)");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, variant.getBranchId());
            stmt.setLong(4, variant.getLanguageId());
            stmt.setLong(5, lockInfo.getUserId());
            stmt.setString(6, lockInfo.getType().getCode());
            stmt.setTimestamp(7, new Timestamp(lockInfo.getTimeAcquired().getTime()));
            stmt.setLong(8, lockInfo.getDuration());
            if (lockInfo.getDuration() >= 0)
                stmt.setTimestamp(9, new Timestamp(lockInfo.getTimeAcquired().getTime() + lockInfo.getDuration()));
            else
                stmt.setNull(9, Types.TIMESTAMP);
            stmt.execute();
            conn.commit();
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            executeAfterCommit.clear();
            throw new RepositoryException("Error getting a lock on " + getFormattedVariant(variant.getKey()), e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
            executeRunnables(executeAfterCommit);
        }

        // fire synchronous events
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.LOCK_CHANGE, variant.getKey(), -1);

        return lockInfo;
    }

    public LockInfoImpl getLockInfo(DocumentVariantImpl variant) throws RepositoryException {
        Connection conn = null;
        try {
            DocId docId = variant.getIntimateAccess(this).getDocument().getIntimateAccess(this).getDocId();
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);
            List<Runnable> executeAfterCommit = new ArrayList<Runnable>(2);
            LockInfoImpl lockInfo = loadLock(docId, variant.getBranchId(), variant.getLanguageId(), conn, executeAfterCommit);
            conn.commit();
            executeRunnables(executeAfterCommit);
            return lockInfo;
        } catch (Exception e) {
            throw new RepositoryException("Error loading lock info.", e);
        } finally {
            jdbcHelper.closeConnection(conn);
        }
    }

    /**
     * Loads current lock info for a document variant. If the lock has meanwhile expired, it is
     * removed by this loading operation.
     *
     * <p>This removal however needs to be notified via an event, since otherwise the cache
     * will not get properly invalidated. However, this event should only be sent if and
     * especially after the connection has been committed (otherwise the cache gets invalidated,
     * and it might get refilled with old info before the commit). Therefore, the caller should
     * execute any Runnables added to the executeAfterCommit list after commit of the running transaction.
     * (Note: pay attention to return statements in the middle of methods).
     *
     * <p>To avoid that another thread modifies the lock information concurrently, access to
     * lock info should be serialized by taking a for-update lock on the document
     * variant record.
     */
    private LockInfoImpl loadLock(final DocId docId, final long branchId, final long languageId, Connection conn, List<Runnable> executeAfterCommit) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select user_id, locktype, time_acquired, duration from locks where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next())
                return new LockInfoImpl();
            long userId = rs.getLong("user_id");
            LockType locktype = LockType.getByCode(rs.getString("locktype"));
            Date timeAcquired = rs.getTimestamp("time_acquired");
            long duration = rs.getLong("duration");
            stmt.close();

            // check if the lock has meanwhile expired, and if so delete it
            if (duration != -1 && timeAcquired.getTime() + duration < System.currentTimeMillis()) {
                if (logger.isDebugEnabled())
                    logger.debug("Removing expired lock for user " + userId + " on " + getFormattedVariant(new VariantKey(docId.toString(), branchId, languageId)));
                // Note that we pass all lock information in the statement, not only the doc_id,
                // to be sure that we're deleting the record only in case nobody else has modified
                // it in the meantime
                stmt = conn.prepareStatement("delete from locks where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ? and user_id = ? and locktype = ? and time_acquired = ? and duration = ?");
                stmt.setLong(1, docId.getSeqId());
                stmt.setLong(2, docId.getNsId());
                stmt.setLong(3, branchId);
                stmt.setLong(4, languageId);
                stmt.setLong(5, userId);
                stmt.setString(6, locktype.getCode());
                stmt.setTimestamp(7, new Timestamp(timeAcquired.getTime()));
                stmt.setLong(8, duration);
                stmt.execute();
                stmt.close();

                executeAfterCommit.add(new Runnable() {
                    public void run() {
                        // fire synchronous events
                        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.LOCK_CHANGE, new VariantKey(docId.toString(), branchId, languageId), -1);
                    }
                });
                return new LockInfoImpl();
            }
            return new LockInfoImpl(userId, timeAcquired, duration, locktype);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    public LockInfoImpl releaseLock(DocumentVariantImpl variant) throws RepositoryException {
        DocumentVariantImpl.IntimateAccess variantInt = variant.getIntimateAccess(this);
        DocId docId = variantInt.getDocument().getIntimateAccess(this).getDocId();

        // Check if user has write access or publish live_history access
        AclResultInfo aclInfo = context.getCommonRepository().getAccessManager().getAclInfoOnLive(systemUser,
                variantInt.getCurrentUser().getId(), variantInt.getCurrentUser().getActiveRoleIds(),
                docId, variant.getBranchId(), variant.getLanguageId());
        boolean canWrite = aclInfo.isAllowed(AclPermission.WRITE);
        boolean canPublish = aclInfo.isAllowed(AclPermission.PUBLISH);
        AccessDetails publishDetails = aclInfo.getAccessDetails(AclPermission.PUBLISH);
        if (!canWrite && !(canPublish && (publishDetails == null || publishDetails.isGranted(AclDetailPermission.LIVE_HISTORY))))
            throw new AccessException("Write access denied for user " + variantInt.getCurrentUser().getId() + " to " + getFormattedVariant(variant.getKey()));

        List<Runnable> executeAfterCommit = new ArrayList<Runnable>(2);
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            // lock the database record for the document variant in order to serialize lock access
            stmt = conn.prepareStatement("select doc_id from document_variants where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ? " + jdbcHelper.getSharedLockClause());
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, variant.getBranchId());
            stmt.setLong(4, variant.getLanguageId());
            stmt.execute();
            stmt.close();

            LockInfoImpl lockInfo = loadLock(docId, variant.getBranchId(), variant.getLanguageId(), conn, executeAfterCommit);
            if (!lockInfo.hasLock())
                return lockInfo;

            if (lockInfo.getUserId() != variantInt.getCurrentUser().getId() && !variantInt.getCurrentUser().isInAdministratorRole())
                return lockInfo;

            stmt = conn.prepareStatement("delete from locks where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, variant.getBranchId());
            stmt.setLong(4, variant.getLanguageId());
            stmt.execute();
            conn.commit();

            // fire synchronous events
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.LOCK_CHANGE,
                    new VariantKey(variant.getDocumentId(), variant.getBranchId(), variant.getLanguageId()), -1);

            return new LockInfoImpl();
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            executeAfterCommit.clear();
            throw new RepositoryException("Error removing lock.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
            executeRunnables(executeAfterCommit);
        }
    }


    /**
     * Loads all the collections to which a specified document belongs.
     * @param variant the document variant we want to load the collections for
     * @return a java.util.Collection the document belongs to, null if no collections can be found
     * (which conceptually means the document only belongs to the <i>root collection</i>.
     */

    private Collection<DocumentCollectionImpl> loadCollections(DocumentVariantImpl variant, DocId docId, Connection conn) throws RepositoryException {
        PreparedStatement stmt = null;
        List<DocumentCollectionImpl> list = new ArrayList<DocumentCollectionImpl>();
        PreparedStatement substmt = null;
        boolean anotherRecordFound;

        try {
            DocumentVariantImpl.IntimateAccess variantInt = variant.getIntimateAccess(this);

            stmt = conn.prepareStatement("select collection_id from document_collections where document_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, variant.getBranchId());
            stmt.setLong(4, variant.getLanguageId());
            ResultSet rs = stmt.executeQuery();

            anotherRecordFound = rs.next();
            if (!anotherRecordFound) {
                return null;
            } else {
                CommonCollectionManager collectionManager = context.getCommonRepository().getCollectionManager();
                AuthenticatedUser user = variantInt.getCurrentUser();
                while (anotherRecordFound) {
                    long collectionId = rs.getLong(1);
                    try {
                        list.add(collectionManager.getCollection(collectionId, false, user));
                    } catch (CollectionNotFoundException e) {
                        // ignore: this would mean the collection has been deleted since the transaction
                        //  that loads this document started
                    }
                    anotherRecordFound = rs.next();
                }
            }

        } catch (Throwable e) {
            logger.debug(e.getMessage());
            throw new RepositoryException("Error loading collections.", e);
        } finally {
            jdbcHelper.closeStatement(substmt);
            jdbcHelper.closeStatement(stmt);
        }

        return list;
    }

    private void addDocumentToCollections(DocumentCollectionImpl[] collArray, DocId docId, long branchId, long languageId, Connection conn) throws SQLException, RepositoryException {
        logger.debug("begin adding document to collection");
        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement("insert into document_collections(document_id, ns_id, branch_id, lang_id, collection_id) values (?,?,?,?,?)");
            for (DocumentCollectionImpl collToAddTo : collArray) {
                try {
                    stmt.setLong(1, docId.getSeqId());
                    stmt.setLong(2, docId.getNsId());
                    stmt.setLong(3, branchId);
                    stmt.setLong(4, languageId);
                    stmt.setLong(5, collToAddTo.getId());
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    /* 
                     * This exception can mean a couple of things.
                     * First: it can be a genuine datastore exception.
                     * if this is the case, we can rethrow it as such.
                     * 
                     * Second: it is possible that an admin has removed
                     * a collection and at the same time, someone was
                     * editing a document. If this document belonged
                     * to the removed collection, this will result in an
                     * exception due to the constraint on the database.
                     * 
                     * In this case we want to throw a specific exception.   
                     */
                    try {
                        context.getCommonRepository().getCollectionManager().getCollection(collToAddTo.getId(), false, systemUser);
                    } catch (RepositoryException e1) {
                        if (e1 instanceof CollectionNotFoundException)
                            throw new CollectionDeletedException(String.valueOf(collToAddTo.getId()));
                            //we're not actually interested in e1, so we just rethrow the previous exception
                            //because it was not caused by collection deletion.
                        else throw e;
                    }
                }
            }
        } finally {
            jdbcHelper.closeStatement(stmt);
            logger.debug("document addition to collection complete");
        }
    }

    private DocumentUpdatedDocument createDocumentUpdatedEvent(DocumentImpl oldDocument, DocumentImpl newDocument, Date lastModified, long newUpdateCount) {
        DocumentUpdatedDocument documentUpdatedDocument = DocumentUpdatedDocument.Factory.newInstance();
        DocumentUpdatedDocument.DocumentUpdated documentUpdated = documentUpdatedDocument.addNewDocumentUpdated();
        documentUpdated.addNewOldDocument().setDocument(oldDocument.getXmlWithoutVariant().getDocument());

        DocumentImpl.IntimateAccess newDocumentInt = newDocument.getIntimateAccess(this);
        DocumentDocument.Document newDocumentXml = newDocument.getXmlWithoutVariant().getDocument();
        updateUpdatedDocumentXml(newDocumentXml, lastModified, newDocumentInt.getCurrentUser().getId(), newUpdateCount);
        documentUpdated.addNewNewDocument().setDocument(newDocumentXml);

        return documentUpdatedDocument;
    }

    /**
     * Updates the document XML so as if it would look if we retrieved the XML of the document after saving it.
     */
    private void updateUpdatedDocumentXml(DocumentDocument.Document documentXml, Date lastModified, long lastModifier, long newUpdateCount) {
        documentXml.setUpdateCount(newUpdateCount);
        documentXml.setLastModified(getCalendar(lastModified));
        documentXml.setLastModifier(lastModifier);
    }

    private DocumentCreatedDocument createNewDocumentEvent(DocumentImpl document, DocId docId, Date lastModified, long newUpdateCount) {
        DocumentImpl.IntimateAccess documentInt = document.getIntimateAccess(this);

        DocumentCreatedDocument documentCreatedDocument = DocumentCreatedDocument.Factory.newInstance();
        DocumentCreatedDocument.DocumentCreated documentCreated = documentCreatedDocument.addNewDocumentCreated();
        DocumentCreatedDocument.DocumentCreated.NewDocument newDocument = documentCreated.addNewNewDocument();

        DocumentDocument.Document documentXml = document.getXmlWithoutVariant().getDocument();
        updateNewDocumentXml(documentXml, docId, lastModified, documentInt.getCurrentUser().getId(), newUpdateCount);
        newDocument.setDocument(documentXml);

        return documentCreatedDocument;
    }

    /**
     * Updates the document XML so as if it would look if we retrieved the XML of the document after saving it.
     */
    private void updateNewDocumentXml(DocumentDocument.Document documentXml, DocId docId, Date lastModified, long lastModifier, long newUpdateCount) {
        documentXml.setUpdateCount(newUpdateCount);
        documentXml.setLastModified(getCalendar(lastModified));
        documentXml.setLastModifier(lastModifier);
        documentXml.setId(docId.toString());
        documentXml.setCreated(getCalendar(lastModified));
    }

    private DocumentVariantUpdatedDocument createVariantUpdatedEvent(DocumentImpl oldDocument, DocumentImpl newDocument, Date lastModified, String summary, long lastVersionId, long liveVersionId, long newVariantUpdateCount, long newDocumentUpdateCount) throws RepositoryException {
        DocumentVariantUpdatedDocument variantUpdatedDocument = DocumentVariantUpdatedDocument.Factory.newInstance();
        DocumentVariantUpdatedDocument.DocumentVariantUpdated variantUpdated = variantUpdatedDocument.addNewDocumentVariantUpdated();
        variantUpdated.addNewOldDocumentVariant().setDocument(oldDocument.getXml().getDocument());

        DocumentImpl.IntimateAccess newDocumentInt = newDocument.getIntimateAccess(this);
        DocumentDocument.Document newDocumentXml = newDocument.getXml().getDocument();
        updateUpdatedDocumentXml(newDocumentXml, lastModified, newDocumentInt.getCurrentUser().getId(), newDocumentUpdateCount);
        // update the XML so as if it would look if we retrieved the XML after the internal document object state
        // is already modified
        newDocumentXml.setVariantUpdateCount(newVariantUpdateCount);
        newDocumentXml.setVariantLastModified(getCalendar(lastModified));
        newDocumentXml.setVariantLastModifier(newDocumentInt.getCurrentUser().getId());
        newDocumentXml.setLastVersionId(lastVersionId);
        newDocumentXml.setLiveVersionId(liveVersionId);
        newDocumentXml.setSummary(summary);
        variantUpdated.addNewNewDocumentVariant().setDocument(newDocumentXml);
        
        return variantUpdatedDocument;
    }

    private DocumentVariantCreatedDocument createNewVariantEvent(DocumentImpl document, DocId docId, Date lastModified, String summary, long newVariantUpdateCount, long newDocumentUpdateCount) throws RepositoryException {
        DocumentImpl.IntimateAccess documentInt = document.getIntimateAccess(this);

        DocumentVariantCreatedDocument variantCreatedDocument = DocumentVariantCreatedDocument.Factory.newInstance();
        DocumentVariantCreatedDocument.DocumentVariantCreated variantCreated = variantCreatedDocument.addNewDocumentVariantCreated();
        DocumentVariantCreatedDocument.DocumentVariantCreated.NewDocumentVariant newVariant = variantCreated.addNewNewDocumentVariant();

        DocumentDocument.Document documentXml = document.getXml().getDocument();
        updateNewDocumentXml(documentXml, docId, lastModified, documentInt.getCurrentUser().getId(), newDocumentUpdateCount);
        // update the XML so as if it would look if we retrieved the XML after the internal document object state
        // is already modified
        documentXml.setVariantUpdateCount(newVariantUpdateCount);
        documentXml.setVariantLastModified(getCalendar(lastModified));
        documentXml.setVariantLastModifier(documentInt.getCurrentUser().getId());
        documentXml.setSummary(summary);
        newVariant.setDocument(documentXml);

        return variantCreatedDocument;
    }

    public void deleteDocument(DocId docId, AuthenticatedUser user) throws RepositoryException {
        PreparedStatement stmt = null;
        Connection conn = null;
        List<Runnable> executeAfterCommit = new ArrayList<Runnable>();
        Lock avoidSuspendLock = context.getBlobStore().getAvoidSuspendLock();
        try {
            if (!avoidSuspendLock.tryLock(0, TimeUnit.MILLISECONDS))
                throw new RepositoryException("The blobstore is currently protected for write access, it is impossible to delete documents right now. Try again later.");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            // take a write lock on the document record
            stmt = conn.prepareStatement("select id from documents where id = ? and ns_id = ? " + jdbcHelper.getSharedLockClause());
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                throw new DocumentNotFoundException(docId.toString());
            }
            stmt.close();

            // for each variant, check the access permissions and the presence of an exclusive lock owned by a different user
            AvailableVariant[] availableVariants = getAvailableVariantsInTransaction(docId, user, conn, true);
            for (AvailableVariant availableVariant : availableVariants) {
                // check if the user is allowed to delete the document variant
                AclResultInfo aclInfo = context.getCommonRepository().getAccessManager().getAclInfoOnLive(systemUser,
                        user.getId(), user.getActiveRoleIds(), docId, availableVariant.getBranchId(), availableVariant.getLanguageId());
                if (!aclInfo.isAllowed(AclPermission.DELETE))
                    throw new AccessException("User " + user.getId() + " (" + user.getLogin() + ") is not allowed to delete document ID " + docId + ", since the user has no delete permission for " + getFormattedVariant(new VariantKey(docId.toString(), availableVariant.getBranchId(), availableVariant.getLanguageId())));

                // take a lock on the document variant record
                stmt = conn.prepareStatement("select doc_id from document_variants where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ? " + jdbcHelper.getSharedLockClause());
                stmt.setLong(1, docId.getSeqId());
                stmt.setLong(2, docId.getNsId());
                stmt.setLong(3, availableVariant.getBranchId());
                stmt.setLong(4, availableVariant.getLanguageId());
                stmt.execute();
                stmt.close();

                // check there's no lock on the document variant owned by another user
                LockInfo lockInfo = loadLock(docId, availableVariant.getBranchId(), availableVariant.getLanguageId(), conn, executeAfterCommit);
                if (lockInfo.hasLock() && lockInfo.getType() == LockType.PESSIMISTIC
                        && lockInfo.getUserId() != user.getId()) {
                    throw new RepositoryException("Cannot delete document " + docId + " because someone else is holding a pessimistic lock on it: user " + lockInfo.getUserId() + " on branch " + getBranchLabel(availableVariant.getBranchId()) + ", language " + getLanguageLabel(availableVariant.getLanguageId()));
                }
            }

            // load the old document (if any), needed to generate event later on
            // this is our last chance to do this
            DocumentImpl deletedDocument = loadDocumentInTransaction(user, docId, -1, -1, conn, executeAfterCommit);

            // delete all variants
            Set<String> blobIds = new HashSet<String>();
            for (AvailableVariant availableVariant : availableVariants) {
                deleteVariantInTransaction(docId, availableVariant.getBranchId(), availableVariant.getLanguageId(), blobIds, user, conn, executeAfterCommit);
            }

            // delete document itself
            stmt = conn.prepareStatement("delete from documents where id = ? and ns_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.execute();
            stmt.close();

            // make async event
            XmlObject eventDescription = createDocumentDeletedEvent(deletedDocument, user);
            eventHelper.createEvent(eventDescription, "DocumentDeleted", conn);

            // commit document deletion
            conn.commit();

            // after succesful commit of database transaction, start deleting blobs
            deleteBlobs(blobIds, conn);
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            executeAfterCommit.clear();
            if (e instanceof AccessException)
                throw (AccessException)e;
            if (e instanceof DocumentNotFoundException)
                throw (DocumentNotFoundException)e;

            throw new RepositoryException("Error deleting document " + docId + ".", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
            avoidSuspendLock.unlock();
            executeRunnables(executeAfterCommit);
        }

        // do sync event
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.DOCUMENT_DELETED, docId.toString(), -1);
    }

    private void deleteBlobs(Set<String> blobIds, Connection conn) throws SQLException {
        if (blobIds.size() > 0) {
            PreparedStatement stmt = null;
            try {
                stmt = conn.prepareStatement("select count(*) from parts where blob_id = ?");
                for (String blobId : blobIds) {
                    stmt.setString(1, blobId);
                    ResultSet rs = stmt.executeQuery();
                    rs.next();
                    if (rs.getLong(1) == 0) {
                        try {
                            context.getBlobStore().delete(blobId);
                        } catch (Throwable e) {
                            logger.error("Error deleting blob with id " + blobId, e);
                        }
                    }
                }
            } finally {
                jdbcHelper.closeStatement(stmt);
            }
        }
    }

    public void deleteVariant(DocId docId, long branchId, long languageId, AuthenticatedUser user) throws RepositoryException {
        List<Runnable> executeAfterCommit = new ArrayList<Runnable>(2);
        PreparedStatement stmt = null;
        Connection conn = null;
        Lock avoidSuspendLock = context.getBlobStore().getAvoidSuspendLock();
        try {
            if (!avoidSuspendLock.tryLock(0, TimeUnit.MILLISECONDS))
                throw new RepositoryException("The blobstore is currently protected for write access, it is impossible to delete document variants right now. Try again later.");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            // take a write lock on the document record
            stmt = conn.prepareStatement("select id from documents where id = ? and ns_id = ? " + jdbcHelper.getSharedLockClause());
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                throw new DocumentNotFoundException(docId.toString());
            }
            stmt.close();

            // check if the user is allowed to delete the document variant
            AclResultInfo aclInfo = context.getCommonRepository().getAccessManager().getAclInfoOnLive(systemUser,
                    user.getId(), user.getActiveRoleIds(), docId, branchId, languageId);
            if (!aclInfo.isAllowed(AclPermission.DELETE))
                throw new AccessException("User " + user.getId() + " (" + user.getLogin() + ") is not allowed to delete the variant: " + getFormattedVariant(new VariantKey(docId.toString(), branchId, languageId)));

            // check there's still more then one variant
            stmt = conn.prepareStatement("select count(*) from document_variants where doc_id = ? and ns_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            rs = stmt.executeQuery();
            rs.next();
            long count = rs.getLong(1);
            if (count <= 1)
                throw new RepositoryException("Deleting the last variant of a document is not possible.");
            stmt.close();

            Set<String> blobIds = new HashSet<String>();
            deleteVariantInTransaction(docId, branchId, languageId, blobIds, user, conn, executeAfterCommit);
            
            removeWeakVariantReferences(docId, branchId, languageId, conn);
            
            // commit document variant deletion
            conn.commit();

            // after succesful commit of database transaction, start deleting blobs
            deleteBlobs(blobIds, conn);
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            executeAfterCommit.clear();
            if (e instanceof AccessException)
                throw (AccessException)e;
            if (e instanceof DocumentNotFoundException)
                throw (DocumentNotFoundException)e;
            if (e instanceof DocumentVariantNotFoundException)
                throw (DocumentVariantNotFoundException)e;

            throw new RepositoryException("Error deleting variant: " + getFormattedVariant(new VariantKey(docId.toString(), branchId, languageId)), e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
            avoidSuspendLock.unlock();
            executeRunnables(executeAfterCommit);
        }
    }

    private void removeWeakVariantReferences(DocId docId, long branchId, long languageId, Connection conn) throws SQLException {
        // make sure document.reference_id and version.synced_with_... no longer point to this variant
        PreparedStatement stmt = null;
        try {
            // clear document_versions.synced_with fields
            stmt = conn.prepareStatement("update document_versions set synced_with_lang_id = null, synced_with_version_id = null, synced_with_search = null" +
                    " where doc_id = ? and ns_id = ? and branch_id = ? and synced_with_lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.execute();
            stmt.close();

        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private void executeRunnables(List<Runnable> runnables) {
        while (runnables.size() > 0) {
            Runnable runnable = runnables.remove(runnables.size() - 1);
            runnable.run();
        }
    }

    /**
     * Deletes a document variant, assumes the necessary pre-condition checks are already done
     * (ie user is allowed to delete it, there's no pessimistic lock by another user, it's not
     * the last variant of the document)
     */
    private void deleteVariantInTransaction(final DocId docId, final long branchId, final long languageId, Set<String> blobIds,
                                            AuthenticatedUser user, Connection conn, List<Runnable> executeAfterCommit) throws SQLException, RepositoryException {
        PreparedStatement stmt = null;
        try {
            DocumentImpl deletedDocument = loadDocumentInTransaction(user, docId, branchId, languageId, conn, executeAfterCommit);

            // delete variant
            stmt = conn.prepareStatement("delete from document_variants where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.execute();
            stmt.close();

            // delete lock
            stmt = conn.prepareStatement("delete from locks where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.execute();
            stmt.close();

            // delete summary
            stmt = conn.prepareStatement("delete from summaries where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.execute();
            stmt.close();

            // delete collection membership
            stmt = conn.prepareStatement("delete from document_collections where document_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.execute();
            stmt.close();

            // delete custom fields
            stmt = conn.prepareStatement("delete from customfields where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.execute();
            stmt.close();

            // delete extracted links
            stmt = conn.prepareStatement("delete from extracted_links where source_doc_id = ? and source_ns_id = ? and source_branch_id = ? and source_lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.execute();
            stmt.close();

            // delete comments
            stmt = conn.prepareStatement("delete from comments where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.execute();
            stmt.close();

            // read all blob ids before deleting the parts
            stmt = conn.prepareStatement("select distinct(blob_id) from parts where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next())
                blobIds.add(rs.getString(1));
            stmt.close();

            // delete parts (of all versions)
            stmt = conn.prepareStatement("delete from parts where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.execute();
            stmt.close();

            // delete fields (of all versions)
            stmt = conn.prepareStatement("delete from thefields where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.execute();
            stmt.close();

            // delete links (of all versions)
            stmt = conn.prepareStatement("delete from links where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.execute();
            stmt.close();

            // delete versions
            stmt = conn.prepareStatement("delete from document_versions where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.execute();
            stmt.close();

            // delete live history
            stmt = conn.prepareStatement("delete from live_history where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.execute();
            stmt.close();

            XmlObject eventDescription = createVariantDeletedEvent(deletedDocument, user);
            eventHelper.createEvent(eventDescription, "DocumentVariantDeleted", conn);

            executeAfterCommit.add(new Runnable() {
                public void run() {
                    context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.DOCUMENT_VARIANT_DELETED, new VariantKey(docId.toString(), branchId, languageId), -1);
                }
            });

        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private DocumentVariantDeletedDocument createVariantDeletedEvent(DocumentImpl document, AuthenticatedUser user) throws RepositoryException {
        DocumentVariantDeletedDocument variantDeletedDocument = DocumentVariantDeletedDocument.Factory.newInstance();
        DocumentVariantDeletedDocument.DocumentVariantDeleted variantDeleted = variantDeletedDocument.addNewDocumentVariantDeleted();
        variantDeleted.addNewDeletedDocumentVariant().setDocument(document.getXml().getDocument());
        variantDeleted.setDeletedTime(new GregorianCalendar());
        variantDeleted.setDeleterId(user.getId());
        return variantDeletedDocument;
    }

    private DocumentDeletedDocument createDocumentDeletedEvent(DocumentImpl document, AuthenticatedUser user) {
        DocumentDeletedDocument documentDeletedDocument = DocumentDeletedDocument.Factory.newInstance();
        DocumentDeletedDocument.DocumentDeleted documentDeleted = documentDeletedDocument.addNewDocumentDeleted();
        documentDeleted.addNewDeletedDocument().setDocument(document.getXmlWithoutVariant().getDocument());
        documentDeleted.setDeletedTime(new GregorianCalendar());
        documentDeleted.setDeleterId(user.getId());
        return documentDeletedDocument;
    }

    public AvailableVariantImpl[] getAvailableVariants(DocId docId, AuthenticatedUser user) throws RepositoryException {
        Connection conn = null;
        try {
            conn = context.getDataSource().getConnection();
            return getAvailableVariantsInTransaction(docId, user, conn, false);
        } catch (DocumentNotFoundException e) {
            // pass this one through
            throw e;
        } catch (Throwable e) {
            throw new RepositoryException("Error getting variants of document " + docId + ".", e);
        } finally {
            jdbcHelper.closeConnection(conn);
        }
    }

    public AvailableVariantImpl[] getAvailableVariantsInTransaction(DocId docId, AuthenticatedUser user, Connection conn, boolean takeLock) throws RepositoryException, SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select branch_id, lang_id, retired, liveversion_id, lastversion_id from document_variants where doc_id = ? and ns_id = ? " + (takeLock ? jdbcHelper.getSharedLockClause() : ""));
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                throw new DocumentNotFoundException(docId.toString());

            List<AvailableVariantImpl> availableVariants = new ArrayList<AvailableVariantImpl>();
            do {
                long branchId = rs.getLong("branch_id");
                long languageId = rs.getLong("lang_id");
                boolean retired = rs.getBoolean("retired");
                long liveVersionId = jdbcHelper.getNullableIdField(rs, "liveversion_id");
                long lastVersionId = jdbcHelper.getNullableIdField(rs, "lastversion_id");

                AvailableVariantImpl availableVariant = new AvailableVariantImpl(branchId, languageId, retired, liveVersionId, lastVersionId, context.getCommonRepository().getVariantManager(), user);
                availableVariants.add(availableVariant);
            } while (rs.next());

            return availableVariants.toArray(new AvailableVariantImpl[availableVariants.size()]);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    public Document createVariant(DocId docId, long startBranchId, long startLanguageId, long startVersionId,
            long newBranchId, long newLanguageId, AuthenticatedUser user) throws RepositoryException {
        try {
            // For the most part, we work with the protected document, so that we only have access to
            // those things the user has read access to. However, for the parts we need to have access
            // to the underlying implementation to make an efficient duplicate, that's why we also get
            // the startVersionImpl (and why we don't use that for everything)
            Document startDocument = load(docId, startBranchId, startLanguageId, user);
            DocumentImpl startDocumentImpl = startDocument instanceof DocumentWrapper ? ((DocumentWrapper)startDocument).getWrappedDocument(this) : (DocumentImpl)startDocument;
            org.outerj.daisy.repository.Version startVersion;
            VersionImpl startVersionImpl;
            if (startVersionId == -1) {
                startVersion = startDocument.getLastVersion();
                startVersionImpl = ((VersionImpl)startDocumentImpl.getLastVersion());
            } else if (startVersionId == -2) {
                startVersion = startDocument.getLiveVersion();
                if (startVersion == null)
                    throw new RepositoryException("Requested to create a variant starting from the live version, but the document does not have a live version. Document: " + getFormattedVariant(new VariantKey(docId.toString(), startBranchId, startLanguageId)));
                startVersionImpl = ((VersionImpl)startDocumentImpl.getLiveVersion());
            } else {
                startVersion = startDocument.getVersion(startVersionId);
                startVersionImpl = ((VersionImpl)startDocumentImpl.getVersion(startVersionId));
            }

            DocumentImpl newDocument = new DocumentImpl(this, context.getCommonRepository(), user, startDocument.getDocumentTypeId(), newBranchId, newLanguageId);
            DocumentImpl.IntimateAccess newDocumentInt = newDocument.getIntimateAccess(this);
            DocumentVariantImpl newVariant = newDocumentInt.getVariant();
            DocumentVariantImpl.IntimateAccess newVariantInt = newVariant.getIntimateAccess(this);

            newVariantInt.setCreatedFrom(startBranchId, startLanguageId, startVersion.getId());

            newDocumentInt.load(docId, startDocument.getLastModified(), startDocument.getLastModifier(),
                    startDocument.getCreated(), startDocument.getOwner(), startDocument.isPrivate(), startDocument.getUpdateCount(), startDocument.getReferenceLanguageId());

            newDocument.setRetired(startDocument.isRetired());

            AclResultInfo aclInfo = context.getCommonRepository().getAccessManager().getAclInfoOnLiveForConceptualDocument(
                    systemUser, user.getId(), user.getActiveRoleIds(),
                    startDocument.getDocumentTypeId(), startDocument.getBranchId(), startDocument.getLanguageId());
            AccessDetails accessDetails = aclInfo.getAccessDetails(AclPermission.WRITE);

            // copy document name
            newDocument.setName(startDocument.getName());

            // copy parts -- efficiently, thus reusing existing blob key
            Part[] parts = startVersion.getParts().getArray();
            PartImpl[] partImpls = startVersionImpl.getIntimateAccess(this).getPartImpls();
            for (Part part : parts) {
                if (accessDetails.isGranted(AclDetailPermission.ALL_PARTS) || accessDetails.canAccessPart(part.getTypeName())) {
                    // Search corresponding partImpl
                    PartImpl correspondingPartImpl = null;
                    for (PartImpl partImpl : partImpls) {
                        if (partImpl.getTypeId() == part.getTypeId()) {
                            correspondingPartImpl = partImpl;
                            break;
                        }
                    }
                    newVariantInt.addPart(correspondingPartImpl.getIntimateAccess(this).internalDuplicate(newVariantInt));
                }
            }

            // copy fields
            Field[] fields = startVersion.getFields().getArray();
            for (Field field : fields) {
                if (accessDetails.isGranted(AclDetailPermission.ALL_FIELDS) || accessDetails.canAccessField(field.getTypeName()))
                    newDocument.setField(field.getTypeId(), field.getValue());
            }

            // copy links
            Link[] links = startVersion.getLinks().getArray();
            for (Link link : links) {
                newDocument.addLink(link.getTitle(), link.getTarget());
            }

            newDocument.setNewVersionState(startVersion.getState());

            // copy custom fields
            Map<String, String> customFields = startDocument.getCustomFields();
            for (Map.Entry<String, String> customField : customFields.entrySet()) {
                String name = customField.getKey();
                String value = customField.getValue();
                newDocument.setCustomField(name, value);
            }

            // copy collections
            DocumentCollection[] collections = startDocument.getCollections().getArray();
            for (DocumentCollection collection : collections) {
                newDocument.addToCollection(collection);
            }

            newDocument.save(false);

            return newDocument;
        } catch (Exception e) {
            throw new RepositoryException("Error while creating new variant based on existing variant.", e);
        }
    }
    
    private LiveHistoryEntry[] loadLiveHistory(DocumentVariantImpl variant, DocumentVariantImpl.IntimateAccess variantInt, DocId docId, Connection conn) throws SQLException, RepositoryException {
        List<LiveHistoryEntry> liveHistory = new ArrayList<LiveHistoryEntry>();
        
        PreparedStatement stmt = null;
        stmt = conn.prepareStatement("select id, begin_date, end_date, version_id, created_by from live_history where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ? order by begin_date");
        stmt.setLong(1, docId.getSeqId());
        stmt.setLong(2, docId.getNsId());
        stmt.setLong(3, variant.getBranchId());
        stmt.setLong(4, variant.getLanguageId());
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            liveHistory.add(new LiveHistoryEntryImpl(rs.getLong("id"), rs.getTimestamp("begin_date"), rs.getTimestamp("end_date"), rs.getLong("version_id"), rs.getLong("created_by")));
        }
        
        return liveHistory.toArray(new LiveHistoryEntry[liveHistory.size()]);
    }
    
    public LiveHistoryEntry[] loadLiveHistory(DocumentVariantImpl variant) throws RepositoryException {
        IntimateAccess variantInt = variant.getIntimateAccess(this);
        DocId docId = variantInt.getDocId();
        Connection conn = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);
            return loadLiveHistory(variant, variantInt, docId, conn);
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new RepositoryException("Error loading live history for document " + docId.toString() + ".", e);
        } finally {
            jdbcHelper.closeConnection(conn);
        }
        
    }

    public void storeTimeline(DocumentVariantImpl variant,
            TimelineImpl timeline) throws RepositoryException {
        DocumentVariantImpl.IntimateAccess variantInt = variant.getIntimateAccess(this);
        DocumentImpl document = variantInt.getDocument();
        DocId docId = variantInt.getDocId();
        Connection conn = null;
        PreparedStatement stmt = null;
        
        AclResultInfo aclInfoOldDoc = context.getCommonRepository().getAccessManager().getAclInfoOnLive(systemUser,
                variantInt.getCurrentUser().getId(), variantInt.getCurrentUser().getActiveRoleIds(),
               variantInt.getDocId(), document.getBranchId(), document.getLanguageId());

        boolean canChangeTimeline = false;
        AccessDetails publishAccessDetails = aclInfoOldDoc.getAccessDetails(AclPermission.PUBLISH);
        if (aclInfoOldDoc.isAllowed(AclPermission.PUBLISH)) { 
            canChangeTimeline = ( publishAccessDetails == null || publishAccessDetails.isGranted(AclDetailPermission.LIVE_HISTORY) );
        }
        
        if (!canChangeTimeline) {
            String branch = context.getCommonRepository().getVariantManager().getBranch(document.getBranchId(), false, systemUser).getName();
            String language = context.getCommonRepository().getVariantManager().getLanguage(document.getLanguageId(), false, systemUser).getName();
            throw new DocumentWriteDeniedException(document.getId(), branch, language, variantInt.getCurrentUser().getLogin());
        }
        
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);
            
            checkVariantUpdateCountAndLock(document, docId, conn);
            Date now = new Date();

            LiveHistoryEntry[] oldLiveHistory = loadLiveHistory(variant);
            storeTimeline(variant, variant.getTimeline().getLiveHistory(), docId, conn, now);
            long oldLiveVersionId = variant.getLiveMajorChangeVersionId(); 
            long liveVersionId = getCurrentLiveVersionId(conn, docId, variant.getBranchId(), variant.getLanguageId(), now);
            
            // update the variant record
            long variantUpdateCount = variant.getUpdateCount() + 1;
            stmt = conn.prepareStatement("update document_variants set last_modified = ?, last_modifier = ?, liveversion_id = ?, updatecount = ?");
            stmt.setTimestamp(1, new Timestamp(now.getTime()));
            stmt.setLong(2, variantInt.getCurrentUser().getId());
            jdbcHelper.setNullableIdField(stmt, 3, liveVersionId);
            stmt.setLong(4, variantUpdateCount);
            stmt.executeUpdate();

            // reload live history to make sure we have the newly generated ids
            LiveHistoryEntry[] newLiveHistory = loadLiveHistory(variant, variantInt, docId, conn);

            XmlObject event = createTimelineUpdatedEvent(variant, docId, oldLiveHistory, oldLiveVersionId, newLiveHistory, variant.getLiveVersionId());
            eventHelper.createEvent(event, "DocumentVariantTimelineUpdated", conn);

            conn.commit();

            variantInt.timelineSaved(now, variantInt.getCurrentUser().getId(), variantUpdateCount, liveVersionId, newLiveHistory);

            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.DOCUMENT_VARIANT_TIMELINE_UPDATED, variant.getKey(), variantUpdateCount);

        } catch (Throwable e) {
            jdbcHelper.rollback(conn);

            if (e instanceof DocumentLockedException || e instanceof AccessException)
                throw (RepositoryException)e;
            else
                throw new RepositoryException("Problem storing timeline.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    private TimelineUpdatedDocument createTimelineUpdatedEvent(DocumentVariantImpl variantImpl, DocId docId, LiveHistoryEntry[] oldHistory, long oldLiveVersionId, LiveHistoryEntry[] newHistory, long newLiveVersionId) throws RepositoryException {
        TimelineUpdatedDocument updateDoc = TimelineUpdatedDocument.Factory.newInstance();
        TimelineUpdatedDocument.TimelineUpdated updateXml = updateDoc.addNewTimelineUpdated();
        
        updateXml.setDocumentId(docId.toString());
        updateXml.setBranchId(variantImpl.getBranchId());
        updateXml.setLanguageId(variantImpl.getLanguageId());
        
        TimelineDocument.Timeline oldTimeline = updateXml.addNewOldTimeline().addNewTimeline();
        oldTimeline.setLiveVersionId(oldLiveVersionId);
        for (LiveHistoryEntry entry: oldHistory) {
            entry.toXml(oldTimeline.addNewLiveHistoryEntry());
        }
        
        TimelineDocument.Timeline newTimeline = updateXml.addNewNewTimeline().addNewTimeline();
        newTimeline.setLiveVersionId(newLiveVersionId);
        for (LiveHistoryEntry entry: newHistory) {
            entry.toXml(newTimeline.addNewLiveHistoryEntry());
        }
        
        return updateDoc;
    }

}
