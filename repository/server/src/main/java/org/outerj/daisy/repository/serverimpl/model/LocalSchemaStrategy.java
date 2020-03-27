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
package org.outerj.daisy.repository.serverimpl.model;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.collections.primitives.ArrayLongList;
import org.apache.commons.collections.primitives.LongIterator;
import org.apache.commons.logging.Log;
import org.apache.xmlbeans.XmlObject;
import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.outerj.daisy.repository.ConcurrentUpdateException;
import org.outerj.daisy.repository.LinkExtractorInfos;
import org.outerj.daisy.repository.LocaleHelper;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerj.daisy.repository.commonimpl.LinkExtractorInfosImpl;
import org.outerj.daisy.repository.commonimpl.schema.DocumentTypeImpl;
import org.outerj.daisy.repository.commonimpl.schema.FieldTypeImpl;
import org.outerj.daisy.repository.commonimpl.schema.PartTypeImpl;
import org.outerj.daisy.repository.commonimpl.schema.SchemaStrategy;
import org.outerj.daisy.repository.commonimpl.schema.StaticListItemImpl;
import org.outerj.daisy.repository.commonimpl.schema.StaticSelectionListImpl;
import org.outerj.daisy.repository.query.SortOrder;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.DocumentTypeNotFoundException;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.FieldTypeNotFoundException;
import org.outerj.daisy.repository.schema.FieldTypeUse;
import org.outerj.daisy.repository.schema.HierarchicalQuerySelectionList;
import org.outerj.daisy.repository.schema.LinkQuerySelectionList;
import org.outerj.daisy.repository.schema.ParentLinkedSelectionList;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.PartTypeNotFoundException;
import org.outerj.daisy.repository.schema.PartTypeUse;
import org.outerj.daisy.repository.schema.QuerySelectionList;
import org.outerj.daisy.repository.schema.RepositorySchemaEventType;
import org.outerj.daisy.repository.schema.StaticSelectionList;
import org.outerj.daisy.repository.serverimpl.EventHelper;
import org.outerj.daisy.repository.serverimpl.LocalRepositoryManager;
import org.outerj.daisy.util.DateUtil;
import org.outerj.daisy.util.LocaleMap;
import org.outerx.daisy.x10.DocumentTypeCreatedDocument;
import org.outerx.daisy.x10.DocumentTypeDeletedDocument;
import org.outerx.daisy.x10.DocumentTypeDocument;
import org.outerx.daisy.x10.DocumentTypeUpdatedDocument;
import org.outerx.daisy.x10.ExpSelectionListDocument;
import org.outerx.daisy.x10.FieldTypeCreatedDocument;
import org.outerx.daisy.x10.FieldTypeDeletedDocument;
import org.outerx.daisy.x10.FieldTypeDocument;
import org.outerx.daisy.x10.FieldTypeUpdatedDocument;
import org.outerx.daisy.x10.PartTypeCreatedDocument;
import org.outerx.daisy.x10.PartTypeDeletedDocument;
import org.outerx.daisy.x10.PartTypeDocument;
import org.outerx.daisy.x10.PartTypeUpdatedDocument;

public class LocalSchemaStrategy implements SchemaStrategy {
    // the code of a static selection list as it will be stored in the database
    private static final char STATIC_SELECTIONLIST_TYPE = 'S';
    private static final char LINKQUERY_SELECTIONLIST_TYPE = 'L';
    private static final char QUERY_SELECTIONLIST_TYPE = 'Q';
    private static final char HIERQUERY_SELECTIONLIST_TYPE = 'H';
    private static final char PARENTLINKED_SELECTIONLIST_TYPE = 'P';
    // the code that is stored in the database if no selection list is present for a field type
    private static final char NO_SELECTIONLIST_TYPE = 'N';
    private LocalRepositoryManager.Context context;
    private Log logger;
    private JdbcHelper jdbcHelper;
    private EventHelper eventHelper;

    public LocalSchemaStrategy(LocalRepositoryManager.Context context, JdbcHelper jdbcHelper) {
        this.context = context;
        this.logger = context.getLogger();
        this.jdbcHelper = jdbcHelper;
        this.eventHelper = new EventHelper(context, jdbcHelper);
    }

    public void store(FieldTypeImpl fieldType) throws RepositoryException {
        FieldTypeImpl.IntimateAccess fieldTypeInt = fieldType.getIntimateAccess(this);

        if (!fieldTypeInt.getCurrentModifier().isInAdministratorRole())
            throw new RepositoryException("Only Administrators can create or update field types.");

        // do some validity checks
        if (fieldType.getName() == null || fieldType.getName().length() < 1)
            throw new RepositoryException("A field type must have a non-null and non-empty name assigned before saving it.");

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            // check the name isn't already in use
            stmt = conn.prepareStatement("select id from field_types where name = ?");
            stmt.setString(1, fieldType.getName());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                if (id != fieldType.getId())
                    throw new RepositoryException("There is already a field type with the following name: \"" + fieldType.getName() + "\", used by the field type with id " + id);
            }
            rs.close();
            stmt.close();

            boolean isNew = false;
            long id = fieldType.getId();
            long labelId = fieldTypeInt.getLabelId();
            long descriptionId = fieldTypeInt.getDescriptionId();
            java.util.Date lastModified = new java.util.Date();
            long lastModifier = fieldTypeInt.getCurrentModifier().getId();

            char selectionListType = NO_SELECTIONLIST_TYPE;
            if (fieldType.getSelectionList() != null) {
                if (fieldType.getSelectionList() instanceof StaticSelectionList) {
                    selectionListType = STATIC_SELECTIONLIST_TYPE;
                } else if (fieldType.getSelectionList() instanceof LinkQuerySelectionList) {
                    selectionListType = LINKQUERY_SELECTIONLIST_TYPE;
                } else if (fieldType.getSelectionList() instanceof QuerySelectionList) {
                    selectionListType = QUERY_SELECTIONLIST_TYPE;
                } else if (fieldType.getSelectionList() instanceof HierarchicalQuerySelectionList) {
                    selectionListType = HIERQUERY_SELECTIONLIST_TYPE;
                } else if (fieldType.getSelectionList() instanceof ParentLinkedSelectionList) {
                    selectionListType = PARENTLINKED_SELECTIONLIST_TYPE;
                }
            }
            
            XmlObject eventDescription;
            if (id == -1) {
                logger.debug("a new fieldtype will be inserted in the repository");
                isNew = true;
                // insert new record
                id = context.getNextFieldTypeId();

                labelId = context.getNextLocalizedStringId();
                descriptionId = context.getNextLocalizedStringId();

                stmt = conn.prepareStatement("insert into field_types(id, name, label_id, description_id, size, valuetype, deprecated, acl_allowed, multivalue, hierarchical, selectionlist_type, selectlist_free_entry, selectlist_load_async, last_modified, last_modifier, updatecount) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                stmt.setLong(1, id);
                stmt.setString(2, fieldType.getName());
                stmt.setLong(3, labelId);
                stmt.setLong(4, descriptionId);
                stmt.setInt(5, fieldType.getSize());
                stmt.setInt(6, fieldType.getValueType().getCode());
                stmt.setBoolean(7, fieldType.isDeprecated());
                stmt.setBoolean(8, fieldType.isAclAllowed());
                stmt.setBoolean(9, fieldType.isMultiValue());
                stmt.setBoolean(10, fieldType.isHierarchical());
                stmt.setString(11, String.valueOf(selectionListType));
                stmt.setBoolean(12, fieldType.getAllowFreeEntry());
                stmt.setBoolean(13, fieldType.getLoadSelectionListAsync());
                stmt.setTimestamp(14, new Timestamp(lastModified.getTime()));
                stmt.setLong(15, lastModifier);
                stmt.setLong(16, 1L);
                stmt.execute();
                stmt.close();
                
                // store selectionlist
                storeSelectionList(selectionListType, fieldType, id, conn);

                eventDescription = createFieldTypeCreatedEvent(fieldType, id, lastModified);
            } else {
                logger.debug("update existing field type");

                // check if nobody else changed it in the meantime
                stmt = conn.prepareStatement("select updatecount from field_types where id = ? " + jdbcHelper.getSharedLockClause());
                stmt.setLong(1, id);
                rs = stmt.executeQuery();
                if (!rs.next()) {
                    throw new FieldTypeNotFoundException(id);
                } else {
                    long dbUpdateCount = rs.getLong(1);
                    if (dbUpdateCount != fieldType.getUpdateCount())
                        throw new ConcurrentUpdateException(FieldType.class.getName(), String.valueOf(fieldType.getId()));
                }
                stmt.close(); // closes resultset too

                FieldTypeImpl oldFieldType = getFieldTypeById(fieldType.getId(), fieldTypeInt.getCurrentModifier());
                long newUpdateCount = fieldType.getUpdateCount() + 1;

                // update the record
                stmt = conn.prepareStatement("update field_types set name=?, deprecated=?, acl_allowed=?, last_modified=?, last_modifier=?, updatecount = ?, selectionlist_type = ?, selectlist_free_entry = ?, selectlist_load_async = ?, size = ?, multivalue = ?, hierarchical = ? where id=?");
                stmt.setString(1, fieldType.getName());
                stmt.setBoolean(2, fieldType.isDeprecated());
                stmt.setBoolean(3, fieldType.isAclAllowed());
                stmt.setTimestamp(4, new Timestamp(lastModified.getTime()));
                stmt.setLong(5, lastModifier);
                stmt.setLong(6, newUpdateCount);
                stmt.setString(7, String.valueOf(selectionListType));
                stmt.setBoolean(8, fieldType.getAllowFreeEntry());
                stmt.setBoolean(9, fieldType.getLoadSelectionListAsync());
                stmt.setInt(10, fieldType.getSize());
                stmt.setBoolean(11, fieldType.isMultiValue());
                stmt.setBoolean(12, fieldType.isHierarchical());
                stmt.setLong(13, id);
                stmt.execute();
                stmt.close();
                
                // remove old selection list, if any
                deleteSelectionList(id, conn);

                // store the new selection list, if any
                storeSelectionList(selectionListType, fieldType, id, conn);

                eventDescription = createFieldTypeUpdatedEvent(oldFieldType, fieldType, lastModified, newUpdateCount);
            }

            // store labels and descriptions, do this by deleting all entries and re-inserting them
            storeLocalizedStrings(labelId, conn, fieldTypeInt.getLabels());
            storeLocalizedStrings(descriptionId, conn, fieldTypeInt.getDescriptions());

            eventHelper.createEvent(eventDescription, isNew ? "FieldTypeCreated" : "FieldTypeUpdated", conn);

            conn.commit();

            fieldTypeInt.setId(id);
            fieldTypeInt.setLabelId(labelId);
            fieldTypeInt.setDescriptionId(descriptionId);
            fieldTypeInt.setLastModified(lastModified);
            fieldTypeInt.setLastModifier(lastModifier);
            fieldTypeInt.setUpdateCount(fieldType.getUpdateCount() + 1);

            if (isNew)
                context.getRepositorySchema().fireSchemaEvent(RepositorySchemaEventType.FIELD_TYPE_CREATED, id, fieldType.getUpdateCount());
            else
                context.getRepositorySchema().fireSchemaEvent(RepositorySchemaEventType.FIELD_TYPE_UPDATED, id, fieldType.getUpdateCount());
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new RepositoryException("Error storing field type.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }

    }

    private void storeSelectionList(char selectionListType, FieldTypeImpl fieldType, long fieldTypeId, Connection conn) throws SQLException, RepositoryException {
        if (selectionListType == STATIC_SELECTIONLIST_TYPE) {
            storeStaticSelectionList(fieldType, conn, fieldTypeId);
        } else if (selectionListType == LINKQUERY_SELECTIONLIST_TYPE) {
            storeLinkQuerySelectionList(fieldType, conn, fieldTypeId);
        } else if (selectionListType == QUERY_SELECTIONLIST_TYPE) {
            storeQuerySelectionList(fieldType, conn, fieldTypeId);
        } else if (selectionListType == HIERQUERY_SELECTIONLIST_TYPE) {
            storeHierarchicalQuerySelectionList(fieldType, conn, fieldTypeId);
        } else if (selectionListType == PARENTLINKED_SELECTIONLIST_TYPE) {
            storeParentLinkedSelectionList(fieldType, conn, fieldTypeId);
        } else if (selectionListType != NO_SELECTIONLIST_TYPE) {
            throw new RepositoryException("Invalid selection list type: " + selectionListType);
        }
    }

    private void deleteSelectionList(long fieldTypeId, Connection conn) throws SQLException {
        deleteStaticSelectionList(fieldTypeId, conn);
        deleteLinkQuerySelectionList(fieldTypeId, conn);
        deleteQuerySelectionList(fieldTypeId, conn);
        deleteHierarchicalQuerySelectionList(fieldTypeId, conn);
        deleteParentLinkedSelectionList(fieldTypeId, conn);
    }

    private void deleteStaticSelectionList(long fieldTypeId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            /* load previous persisted label_ids from repository and use these
             * to clear their corresponding localized strings, in order to avoid
             * orphaned records in that table (suppose a label_id was created
             * the previous time but is now no longer in the selection list,
             * this would otherwise cause an orphaned record in the repository)
             */
            stmt = conn.prepareStatement("select label_id from selectionlist_data where fieldtype_id=?");
            stmt.setLong(1, fieldTypeId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                long oldLabelId = rs.getLong(1);
                if (oldLabelId != -1)
                    deleteLocalizedString(oldLabelId, conn);
            }
            stmt.close();

            stmt = conn.prepareStatement("delete from selectionlist_data where fieldtype_id=?");
            stmt.setLong(1, fieldTypeId);
            stmt.execute();
            stmt.close();
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private void deleteLinkQuerySelectionList(long fieldTypeId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("delete from linkquerysellist where fieldtype_id = ?");
            stmt.setLong(1, fieldTypeId);
            stmt.execute();
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private void deleteQuerySelectionList(long fieldTypeId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("delete from querysellist where fieldtype_id = ?");
            stmt.setLong(1, fieldTypeId);
            stmt.execute();
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private void deleteHierarchicalQuerySelectionList(long fieldTypeId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("delete from hierquerysellist_fields where fieldtype_id = ?");
            stmt.setLong(1, fieldTypeId);
            stmt.execute();
            stmt.close();
            stmt = conn.prepareStatement("delete from hierquerysellist where fieldtype_id = ?");
            stmt.setLong(1, fieldTypeId);
            stmt.execute();
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private void deleteParentLinkedSelectionList(long fieldTypeId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("delete from parentlinkedsellist where fieldtype_id = ?");
            stmt.setLong(1, fieldTypeId);
            stmt.execute();
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private FieldTypeCreatedDocument createFieldTypeCreatedEvent(FieldTypeImpl fieldType, long newId, Date lastModified) {
        FieldTypeCreatedDocument fieldTypeCreatedDocument = FieldTypeCreatedDocument.Factory.newInstance();
        FieldTypeCreatedDocument.FieldTypeCreated fieldTypeCreated = fieldTypeCreatedDocument.addNewFieldTypeCreated();

        FieldTypeDocument.FieldType fieldTypeXml = fieldType.getXml().getFieldType();
        fieldTypeXml.setLastModified(getCalendar(lastModified));
        fieldTypeXml.setLastModifier(fieldType.getIntimateAccess(this).getCurrentModifier().getId());
        fieldTypeXml.setUpdateCount(1);
        fieldTypeXml.setId(newId);

        fieldTypeCreated.addNewNewFieldType().setFieldType(fieldTypeXml);
        return fieldTypeCreatedDocument;
    }

    private FieldTypeUpdatedDocument createFieldTypeUpdatedEvent(FieldTypeImpl oldFieldType, FieldTypeImpl newFieldType, Date lastModified, long newUpdateCount) {
        FieldTypeUpdatedDocument fieldTypeUpdatedDocument = FieldTypeUpdatedDocument.Factory.newInstance();
        FieldTypeUpdatedDocument.FieldTypeUpdated fieldTypeUpdated = fieldTypeUpdatedDocument.addNewFieldTypeUpdated();

        fieldTypeUpdated.addNewOldFieldType().setFieldType(oldFieldType.getXml().getFieldType());

        FieldTypeDocument.FieldType newFieldTypeXml = newFieldType.getXml().getFieldType();
        newFieldTypeXml.setLastModified(getCalendar(lastModified));
        newFieldTypeXml.setLastModifier(newFieldType.getIntimateAccess(this).getCurrentModifier().getId());
        newFieldTypeXml.setUpdateCount(newUpdateCount);
        fieldTypeUpdated.addNewNewFieldType().setFieldType(newFieldTypeXml);

        return fieldTypeUpdatedDocument;
    }

    private Calendar getCalendar(Date date) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        return calendar;
    }

    private void storeStaticSelectionList(FieldTypeImpl fieldType, Connection conn, long id) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("insert into selectionlist_data(fieldtype_id, sequencenr, depth, stringvalue, datevalue, datetimevalue, integervalue, floatvalue, decimalvalue, booleanvalue, link_docid, link_nsid, link_branchid, link_langid, label_id) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

            StaticSelectionListImpl selectionList = (StaticSelectionListImpl)fieldType.getSelectionList();
            StaticSelectionListImpl.IntimateAccess selectionListInt = selectionList.getIntimateAccess(this);
            StaticListItemImpl root = selectionListInt.getRoot();
            int listItemCounter = 0;

            storeStaticListItemChildren(root, fieldType.getValueType(), conn, stmt, id, listItemCounter, 0);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private int storeStaticListItemChildren(StaticListItemImpl parentListItem, ValueType valueType, Connection conn,
            PreparedStatement stmt, long fieldTypeId, int listItemCounter, int listItemDepth) throws SQLException {
        List<StaticListItemImpl> listItems = (List<StaticListItemImpl>)parentListItem.getItems();
        for (StaticListItemImpl listItem : listItems) {
            listItemCounter++;
            listItemCounter = storeStaticListItem(listItem, valueType, conn, stmt, fieldTypeId, listItemCounter, listItemDepth);
        }
        return listItemCounter;
    }

    private int storeStaticListItem(StaticListItemImpl listItem, ValueType valueType, Connection conn,
            PreparedStatement stmt, long fieldTypeId, int listItemCounter, int listItemDepth) throws SQLException {
        StaticListItemImpl.IntimateAccess itemInt = listItem.getIntimateAccess(this);
        long localizedLabelId = -1;
        if (!itemInt.getLabels().isEmpty())
            localizedLabelId = context.getNextLocalizedStringId();

        Object listItemValue = listItem.getValue();

        String stringValue = (String)(valueType == ValueType.STRING ? listItemValue : null);
        Date dateValue = valueType == ValueType.DATE ? new java.sql.Date(((Date)listItemValue).getTime()) : null;
        Timestamp datetimeValue = valueType == ValueType.DATETIME ? new Timestamp(((Date)listItemValue).getTime()) : null;
        Long integerValue = (Long)(valueType == ValueType.LONG ? listItemValue : null);
        Double floatValue = (Double)(valueType == ValueType.DOUBLE ? listItemValue : null);
        BigDecimal decimalValue = (BigDecimal)(valueType == ValueType.DECIMAL ? listItemValue : null);
        Boolean booleanValue = (Boolean)(valueType == ValueType.BOOLEAN ? listItemValue : null);
        Long linkDocId = null;
        Long linkNsId = null;
        Long linkBranchId = null;
        Long linkLangId = null;
        if (valueType == ValueType.LINK) {
            VariantKey variantKey = (VariantKey)listItemValue;
            DocId docId = DocId.parseDocId(variantKey.getDocumentId(), context.getCommonRepository());
            linkDocId = new Long(docId.getSeqId());
            linkNsId = new Long(docId.getNsId());
            linkBranchId = new Long(variantKey.getBranchId());
            linkLangId = new Long(variantKey.getLanguageId());
        }

        stmt.setLong(1, fieldTypeId);
        stmt.setInt(2, listItemCounter);
        stmt.setInt(3, listItemDepth);
        stmt.setString(4, stringValue);
        stmt.setObject(5, dateValue);
        stmt.setObject(6, datetimeValue);
        stmt.setObject(7, integerValue);
        stmt.setObject(8, floatValue);
        stmt.setBigDecimal(9, decimalValue);
        stmt.setObject(10, booleanValue);
        stmt.setObject(11, linkDocId);
        stmt.setObject(12, linkNsId);
        stmt.setObject(13, linkBranchId);
        stmt.setObject(14, linkLangId);
        stmt.setLong(15, localizedLabelId);

        stmt.execute();

        if (localizedLabelId != -1)
            storeLocalizedStrings(localizedLabelId, conn, itemInt.getLabels());

        listItemDepth++;
        listItemCounter = storeStaticListItemChildren(listItem, valueType, conn, stmt, fieldTypeId, listItemCounter, listItemDepth);
        return listItemCounter;
    }

    private void storeLinkQuerySelectionList(FieldTypeImpl fieldType, Connection conn, long id) throws SQLException {
        PreparedStatement stmt = null;
        try {
            LinkQuerySelectionList list = (LinkQuerySelectionList)fieldType.getSelectionList();
            stmt = conn.prepareStatement("insert into linkquerysellist(fieldtype_id, whereclause, filtervariants) values(?, ?, ?)");
            stmt.setLong(1, id);
            stmt.setString(2, list.getWhereClause());
            stmt.setBoolean(3, list.getFilterVariants());
            stmt.execute();
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private void storeQuerySelectionList(FieldTypeImpl fieldType, Connection conn, long id) throws SQLException {
        PreparedStatement stmt = null;
        try {
            QuerySelectionList list = (QuerySelectionList)fieldType.getSelectionList();
            stmt = conn.prepareStatement("insert into querysellist(fieldtype_id, query, filtervariants, sort_order) values(?, ?, ?, ?)");
            stmt.setLong(1, id);
            stmt.setString(2, list.getQuery());
            stmt.setBoolean(3, list.getFilterVariants());
            stmt.setString(4, String.valueOf(list.getSortOrder().getCode()));
            stmt.execute();
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private void storeHierarchicalQuerySelectionList(FieldTypeImpl fieldType, Connection conn, long id) throws SQLException {
        PreparedStatement stmt = null;
        try {
            HierarchicalQuerySelectionList list = (HierarchicalQuerySelectionList)fieldType.getSelectionList();
            stmt = conn.prepareStatement("insert into hierquerysellist(fieldtype_id, whereclause, filtervariants) values(?, ?, ?)");
            stmt.setLong(1, id);
            stmt.setString(2, list.getWhereClause());
            stmt.setBoolean(3, list.getFilterVariants());
            stmt.execute();
            stmt.close();

            String[] linkFields = list.getLinkFields();
            stmt = conn.prepareStatement("insert into hierquerysellist_fields(fieldtype_id, sequencenr, fieldname) values(?, ?, ?)");
            for (int i = 0; i < linkFields.length; i++) {
                stmt.setLong(1, id);
                stmt.setInt(2, i);
                stmt.setString(3, linkFields[i]);
                stmt.execute();
            }
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private void storeParentLinkedSelectionList(FieldTypeImpl fieldType, Connection conn, long id) throws SQLException {
        PreparedStatement stmt = null;
        try {
            ParentLinkedSelectionList list = (ParentLinkedSelectionList)fieldType.getSelectionList();
            stmt = conn.prepareStatement("insert into parentlinkedsellist(fieldtype_id, whereclause, linkfield, filtervariants) values(?, ?, ?, ?)");
            stmt.setLong(1, id);
            stmt.setString(2, list.getWhereClause());
            stmt.setString(3, list.getParentLinkField());
            stmt.setBoolean(4, list.getFilterVariants());
            stmt.execute();
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    public void deleteFieldType(long fieldTypeId, AuthenticatedUser user) throws RepositoryException {
        if (!user.isInAdministratorRole())
            throw new RepositoryException("Only Administrators can delete field types.");

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            // take a lock on the field type
            stmt = conn.prepareStatement("select id from field_types where id = ? " + jdbcHelper.getSharedLockClause());
            stmt.setLong(1, fieldTypeId);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                throw new FieldTypeNotFoundException(fieldTypeId);
            }
            stmt.close();

            FieldTypeImpl fieldType = loadFieldType(fieldTypeId, user, conn);
            FieldTypeImpl.IntimateAccess fieldTypeInt = fieldType.getIntimateAccess(this);

            deleteLocalizedString(fieldTypeInt.getLabelId(), conn);
            deleteLocalizedString(fieldTypeInt.getDescriptionId(), conn);
            deleteSelectionList(fieldTypeId, conn);

            stmt = conn.prepareStatement("delete from field_types where id = ?");
            stmt.setLong(1, fieldTypeId);
            stmt.execute();

            XmlObject eventDescription = createFieldTypeDeletedEvent(fieldType, user);
            eventHelper.createEvent(eventDescription, "FieldTypeDeleted", conn);

            conn.commit();
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new RepositoryException("Error deleting field type " + fieldTypeId, e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }

        context.getRepositorySchema().fireSchemaEvent(RepositorySchemaEventType.FIELD_TYPE_DELETED, fieldTypeId, -1);
    }

    private FieldTypeDeletedDocument createFieldTypeDeletedEvent(FieldTypeImpl fieldType, AuthenticatedUser deleter) {
        FieldTypeDeletedDocument fieldTypeDeletedDocument = FieldTypeDeletedDocument.Factory.newInstance();
        FieldTypeDeletedDocument.FieldTypeDeleted fieldTypeDeleted = fieldTypeDeletedDocument.addNewFieldTypeDeleted();
        fieldTypeDeleted.addNewDeletedFieldType().setFieldType(fieldType.getXml().getFieldType());
        fieldTypeDeleted.setDeleterId(deleter.getId());
        fieldTypeDeleted.setDeletedTime(new GregorianCalendar());
        return fieldTypeDeletedDocument;
    }

    public void store(DocumentTypeImpl documentType) throws RepositoryException {
        DocumentTypeImpl.IntimateAccess documentTypeInt = documentType.getIntimateAccess(this);

        if (!documentTypeInt.getCurrentModifier().isInAdministratorRole())
            throw new RepositoryException("Only Administrators can create or store Document Types.");

        // do some validity checks on the documentType
        if (documentType.getName() == null || documentType.getName().length() < 1)
            throw new RepositoryException("A document type must have a non-null and non-empty name assigned before saving it.");

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            // check the name isn't already in use
            stmt = conn.prepareStatement("select id from document_types where name = ?");
            stmt.setString(1, documentType.getName());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                if (id != documentType.getId())
                    throw new RepositoryException("There is already a document type with the name \"" + documentType.getName() + "\", used by the document type with id " + id);
            }
            rs.close();
            stmt.close();

            // start creating the new entry
            boolean isNew = false;
            long id = documentType.getId();
            long labelId = documentTypeInt.getLabelId();
            long descriptionId = documentTypeInt.getDescriptionId();
            java.util.Date lastModified = new java.util.Date();
            long lastModifier = documentTypeInt.getCurrentModifier().getId();
            XmlObject eventDescription;

            if (id == -1) {
                isNew = true;
                // insert new record
                id = context.getNextDocumentTypeId();

                labelId = context.getNextLocalizedStringId();
                descriptionId = context.getNextLocalizedStringId();

                stmt = conn.prepareStatement("insert into document_types(id, name, deprecated, label_id, description_id, last_modified, last_modifier, updatecount) values(?,?,?,?,?,?,?,?)");
                stmt.setLong(1, id);
                stmt.setString(2, documentType.getName());
                stmt.setBoolean(3, documentType.isDeprecated());
                stmt.setLong(4, labelId);
                stmt.setLong(5, descriptionId);
                stmt.setTimestamp(6, new Timestamp(lastModified.getTime()));
                stmt.setLong(7, lastModifier);
                stmt.setLong(8, 1L);
                stmt.execute();
                stmt.close();

                eventDescription = createDocumentTypeCreatedEvent(documentType, id, lastModified);
            } else {
                // update existing record

                // check if nobody else changed it in the meantime
                stmt = conn.prepareStatement("select updatecount from document_types where id = ? " + jdbcHelper.getSharedLockClause());
                stmt.setLong(1, id);
                rs = stmt.executeQuery();
                if (!rs.next()) {
                    throw new DocumentTypeNotFoundException(id);
                } else {
                    long dbUpdateCount = rs.getLong(1);
                    if (dbUpdateCount != documentType.getUpdateCount())
                        throw new ConcurrentUpdateException(DocumentType.class.getName(), String.valueOf(documentType.getId()));
                }
                stmt.close(); // closes resultset too

                DocumentTypeImpl oldDocumentType = getDocumentTypeById(documentType.getId(), documentTypeInt.getCurrentModifier());
                long newUpdateCount = documentType.getUpdateCount() + 1;

                // update the record
                stmt = conn.prepareStatement("update document_types set name=?, deprecated=?, last_modified=?, last_modifier=?, updatecount=? where id = ?");
                stmt.setString(1, documentType.getName());
                stmt.setBoolean(2, documentType.isDeprecated());
                stmt.setTimestamp(3, new Timestamp(lastModified.getTime()));
                stmt.setLong(4, lastModifier);
                stmt.setLong(5, newUpdateCount);
                stmt.setLong(6, id);
                stmt.execute();
                stmt.close();

                eventDescription = createDocumentTypeUpdatedEvent(oldDocumentType, documentType, lastModified, newUpdateCount);
            }

            // store labels and descriptions, do this by deleting all entries and re-inserting them
            storeLocalizedStrings(labelId, conn, documentTypeInt.getLabels());
            storeLocalizedStrings(descriptionId, conn, documentTypeInt.getDescriptions());

            deleteDocumentTypeAssociations(id, conn);

            // store association with field types
            stmt = conn.prepareStatement("insert into doctypes_fieldtypes(doctype_id, field_id, required, editable, sequencenr) values(?,?,?,?,?)");
            FieldTypeUse[] fieldTypeUses = documentType.getFieldTypeUses();
            for (int i = 0; i < fieldTypeUses.length; i++) {
                stmt.setLong(1, id);
                stmt.setLong(2, fieldTypeUses[i].getFieldType().getId());
                stmt.setBoolean(3, fieldTypeUses[i].isRequired());
                stmt.setBoolean(4, fieldTypeUses[i].isEditable());
                stmt.setInt(5, i);
                stmt.execute();
            }
            stmt.close();

            // store association with part types
            stmt = conn.prepareStatement("insert into doctype_contentmodel(doctype_id, part_id, required, editable, sequencenr) values(?,?,?,?,?)");
            PartTypeUse[] partTypeUses = documentType.getPartTypeUses();
            for (int i = 0; i < partTypeUses.length; i++) {
                stmt.setLong(1, id);
                stmt.setLong(2, partTypeUses[i].getPartType().getId());
                stmt.setBoolean(3, partTypeUses[i].isRequired());
                stmt.setBoolean(4, partTypeUses[i].isEditable());
                stmt.setInt(5, i);
                stmt.execute();
            }
            stmt.close();

            eventHelper.createEvent(eventDescription, isNew ? "DocumentTypeCreated" : "DocumentTypeUpdated", conn);

            conn.commit();

            documentTypeInt.setId(id);
            documentTypeInt.setLabelId(labelId);
            documentTypeInt.setDescriptionId(descriptionId);
            documentTypeInt.setLastModified(lastModified);
            documentTypeInt.setLastModifier(lastModifier);
            documentTypeInt.setUpdateCount(documentType.getUpdateCount() + 1);

            if (isNew)
                context.getRepositorySchema().fireSchemaEvent(RepositorySchemaEventType.DOCUMENT_TYPE_CREATED, id, documentType.getUpdateCount());
            else
                context.getRepositorySchema().fireSchemaEvent(RepositorySchemaEventType.DOCUMENT_TYPE_UPDATED, id, documentType.getUpdateCount());
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new RepositoryException("Problem storing document type.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }

    }

    private DocumentTypeCreatedDocument createDocumentTypeCreatedEvent(DocumentTypeImpl documentType, long newId, Date lastModified) {
        DocumentTypeCreatedDocument documentTypeCreatedDocument = DocumentTypeCreatedDocument.Factory.newInstance();
        DocumentTypeCreatedDocument.DocumentTypeCreated documentTypeCreated = documentTypeCreatedDocument.addNewDocumentTypeCreated();

        DocumentTypeDocument.DocumentType documentTypeXml = documentType.getXml().getDocumentType();
        documentTypeXml.setLastModified(getCalendar(lastModified));
        documentTypeXml.setLastModifier(documentType.getIntimateAccess(this).getCurrentModifier().getId());
        documentTypeXml.setUpdateCount(1);
        documentTypeXml.setId(newId);

        documentTypeCreated.addNewNewDocumentType().setDocumentType(documentTypeXml);
        return documentTypeCreatedDocument;
    }

    private DocumentTypeUpdatedDocument createDocumentTypeUpdatedEvent(DocumentTypeImpl oldDocumentType, DocumentTypeImpl newDocumentType, Date lastModified, long newUpdateCount) {
        DocumentTypeUpdatedDocument documentTypeUpdatedDocument = DocumentTypeUpdatedDocument.Factory.newInstance();
        DocumentTypeUpdatedDocument.DocumentTypeUpdated documentTypeUpdated = documentTypeUpdatedDocument.addNewDocumentTypeUpdated();

        documentTypeUpdated.addNewOldDocumentType().setDocumentType(oldDocumentType.getXml().getDocumentType());

        DocumentTypeDocument.DocumentType newDocumentTypeXml = newDocumentType.getXml().getDocumentType();
        newDocumentTypeXml.setLastModified(getCalendar(lastModified));
        newDocumentTypeXml.setLastModifier(newDocumentType.getIntimateAccess(this).getCurrentModifier().getId());
        newDocumentTypeXml.setUpdateCount(newUpdateCount);
        documentTypeUpdated.addNewNewDocumentType().setDocumentType(newDocumentTypeXml);

        return documentTypeUpdatedDocument;
    }

    public void deleteDocumentType(long documentTypeId, AuthenticatedUser user) throws RepositoryException {
        if (!user.isInAdministratorRole())
            throw new RepositoryException("Only Administrators can delete document types.");

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            // lock the document type
            stmt = conn.prepareStatement("select updatecount from document_types where id = ? " + jdbcHelper.getSharedLockClause());
            stmt.setLong(1, documentTypeId);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                throw new DocumentTypeNotFoundException(documentTypeId);
            }
            stmt.close();

            DocumentTypeImpl documentType = loadDocumentType(documentTypeId, user, conn);
            DocumentTypeImpl.IntimateAccess documentTypeInt = documentType.getIntimateAccess(this);

            deleteLocalizedString(documentTypeInt.getLabelId(), conn);
            deleteLocalizedString(documentTypeInt.getDescriptionId(), conn);
            deleteDocumentTypeAssociations(documentTypeId, conn);

            stmt = conn.prepareStatement("delete from document_types where id = ?");
            stmt.setLong(1, documentTypeId);
            stmt.execute();

            XmlObject eventDescription = createDocumentTypeDeletedEvent(documentType, user);
            eventHelper.createEvent(eventDescription, "DocumentTypeDeleted", conn);

            conn.commit();
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new RepositoryException("Error deleting document type " + documentTypeId, e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }

        context.getRepositorySchema().fireSchemaEvent(RepositorySchemaEventType.DOCUMENT_TYPE_DELETED, documentTypeId, -1);
    }

    private void deleteDocumentTypeAssociations(long documentTypeId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("delete from doctypes_fieldtypes where doctype_id = ?");
            stmt.setLong(1, documentTypeId);
            stmt.execute();
            stmt.close();

            stmt = conn.prepareStatement("delete from doctype_contentmodel where doctype_id = ?");
            stmt.setLong(1, documentTypeId);
            stmt.execute();
            stmt.close();
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private DocumentTypeDeletedDocument createDocumentTypeDeletedEvent(DocumentTypeImpl documentType, AuthenticatedUser deleter) {
        DocumentTypeDeletedDocument documentTypeDeletedDocument = DocumentTypeDeletedDocument.Factory.newInstance();
        DocumentTypeDeletedDocument.DocumentTypeDeleted documentTypeDeleted = documentTypeDeletedDocument.addNewDocumentTypeDeleted();
        documentTypeDeleted.addNewDeletedDocumentType().setDocumentType(documentType.getXml().getDocumentType());
        documentTypeDeleted.setDeleterId(deleter.getId());
        documentTypeDeleted.setDeletedTime(new GregorianCalendar());
        return documentTypeDeletedDocument;
    }

    public void store(PartTypeImpl partType) throws RepositoryException {
        PartTypeImpl.IntimateAccess partTypeInt = partType.getIntimateAccess(this);

        if (!partTypeInt.getCurrentModifier().isInAdministratorRole())
            throw new RepositoryException("Only Administrators can create or update part types.");

        // do some validity checks on the partType

        if (partType.getMimeTypes() == null)
            throw new RepositoryException("A part type must have a non-null mime-type assigned before saving it.");

        if (partType.getName() == null || partType.getName().length() < 1)
            throw new RepositoryException("A part type must have a non-null and non-empty name assigned before saving it.");

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            // check the name isn't already in use
            stmt = conn.prepareStatement("select part_id from part_types where name = ?");
            stmt.setString(1, partType.getName());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                if (id != partType.getId())
                    throw new RepositoryException("There is already a part type with the following name: \"" + partType.getName() + "\", used by the part type with id " + id);
            }
            rs.close();
            stmt.close();

            // start creating the new entry
            boolean isNew = false;
            long id = partType.getId();
            long labelId = partTypeInt.getLabelId();
            long descriptionId = partTypeInt.getDescriptionId();
            java.util.Date lastModified = new java.util.Date();
            long lastModifier = partTypeInt.getCurrentModifier().getId();
            XmlObject eventDescription;

            if (id == -1) {
                isNew = true;
                // insert new record
                id = context.getNextPartTypeId();

                labelId = context.getNextLocalizedStringId();
                descriptionId = context.getNextLocalizedStringId();

                stmt = conn.prepareStatement("insert into part_types(part_id, name, mimetype, daisy_html, linkextractor, deprecated, label_id, description_id, last_modified, last_modifier, updatecount) values(?,?,?,?,?,?,?,?,?,?,?)");
                stmt.setLong(1, id);
                stmt.setString(2, partType.getName());
                stmt.setString(3, partType.getMimeTypes().equals("") ? " " : partType.getMimeTypes()); // Workaround for Oracle "empty string becomes null" problem
                stmt.setBoolean(4, partType.isDaisyHtml());
                stmt.setString(5, partType.getLinkExtractor());
                stmt.setBoolean(6, partType.isDeprecated());
                stmt.setLong(7, labelId);
                stmt.setLong(8, descriptionId);
                stmt.setTimestamp(9, new Timestamp(lastModified.getTime()));
                stmt.setLong(10, lastModifier);
                stmt.setLong(11, 1L);
                stmt.execute();
                stmt.close();

                eventDescription = createPartTypeCreatedEvent(partType, id, lastModified);
            } else {
                // update existing record

                // check if nobody else changed it in the meantime
                stmt = conn.prepareStatement("select updatecount from part_types where part_id = ? " + jdbcHelper.getSharedLockClause());
                stmt.setLong(1, id);
                rs = stmt.executeQuery();
                if (!rs.next()) {
                    throw new PartTypeNotFoundException(id);
                } else {
                    long dbUpdateCount = rs.getLong(1);
                    if (dbUpdateCount != partType.getUpdateCount())
                        throw new ConcurrentUpdateException(PartType.class.getName(), String.valueOf(partType.getId()));
                }
                stmt.close(); // closes resultset too

                PartTypeImpl oldPartType = getPartTypeById(partType.getId(), partTypeInt.getCurrentModifier());
                long newUpdateCount = partType.getUpdateCount() + 1;

                // update the record
                stmt = conn.prepareStatement("update part_types set name=?, mimetype=?, daisy_html=?, linkextractor=?, deprecated=?, last_modified=?, last_modifier=?, updatecount=? where part_id = ?");
                stmt.setString(1, partType.getName());
                stmt.setString(2, partType.getMimeTypes());
                stmt.setBoolean(3, partType.isDaisyHtml());
                stmt.setString(4, partType.getLinkExtractor());
                stmt.setBoolean(5, partType.isDeprecated());
                stmt.setTimestamp(6, new Timestamp(lastModified.getTime()));
                stmt.setLong(7, lastModifier);
                stmt.setLong(8, partType.getUpdateCount() + 1);
                stmt.setLong(9, id);
                stmt.execute();
                stmt.close();

                eventDescription = createPartTypeUpdatedEvent(oldPartType, partType, lastModified, newUpdateCount);
            }

            // store labels and descriptions, do this by deleting all entries and re-inserting them
            storeLocalizedStrings(labelId, conn, partTypeInt.getLabels());
            storeLocalizedStrings(descriptionId, conn, partTypeInt.getDescriptions());

            eventHelper.createEvent(eventDescription, isNew ? "PartTypeCreated" : "PartTypeUpdated", conn);

            conn.commit();

            partTypeInt.setId(id);
            partTypeInt.setLabelId(labelId);
            partTypeInt.setDescriptionId(descriptionId);
            partTypeInt.setLastModified(lastModified);
            partTypeInt.setLastModifier(lastModifier);
            partTypeInt.setUpdateCount(partType.getUpdateCount() + 1);

            if (isNew)
                context.getRepositorySchema().fireSchemaEvent(RepositorySchemaEventType.PART_TYPE_CREATED, id, partType.getUpdateCount());
            else
                context.getRepositorySchema().fireSchemaEvent(RepositorySchemaEventType.PART_TYPE_UPDATED, id, partType.getUpdateCount());
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new RepositoryException("Problem storing part type", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    private PartTypeCreatedDocument createPartTypeCreatedEvent(PartTypeImpl partType, long newId, Date lastModified) {
        PartTypeCreatedDocument partTypeCreatedDocument = PartTypeCreatedDocument.Factory.newInstance();
        PartTypeCreatedDocument.PartTypeCreated partTypeCreated = partTypeCreatedDocument.addNewPartTypeCreated();

        PartTypeDocument.PartType partTypeXml = partType.getXml().getPartType();
        partTypeXml.setLastModified(getCalendar(lastModified));
        partTypeXml.setLastModifier(partType.getIntimateAccess(this).getCurrentModifier().getId());
        partTypeXml.setUpdateCount(1);
        partTypeXml.setId(newId);

        partTypeCreated.addNewNewPartType().setPartType(partTypeXml);
        return partTypeCreatedDocument;
    }

    private PartTypeUpdatedDocument createPartTypeUpdatedEvent(PartTypeImpl oldPartType, PartTypeImpl newPartType, Date lastModified, long newUpdateCount) {
        PartTypeUpdatedDocument partTypeUpdatedDocument = PartTypeUpdatedDocument.Factory.newInstance();
        PartTypeUpdatedDocument.PartTypeUpdated partTypeUpdated = partTypeUpdatedDocument.addNewPartTypeUpdated();

        partTypeUpdated.addNewOldPartType().setPartType(oldPartType.getXml().getPartType());

        PartTypeDocument.PartType newPartTypeXml = newPartType.getXml().getPartType();
        newPartTypeXml.setLastModified(getCalendar(lastModified));
        newPartTypeXml.setLastModifier(newPartType.getIntimateAccess(this).getCurrentModifier().getId());
        newPartTypeXml.setUpdateCount(newUpdateCount);
        partTypeUpdated.addNewNewPartType().setPartType(newPartTypeXml);

        return partTypeUpdatedDocument;
    }

    public void deletePartType(long partTypeId, AuthenticatedUser user) throws RepositoryException {
        if (!user.isInAdministratorRole())
            throw new RepositoryException("Only Administrators can delete part types.");

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement("select updatecount from part_types where part_id = ? " + jdbcHelper.getSharedLockClause());
            stmt.setLong(1, partTypeId);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                throw new PartTypeNotFoundException(partTypeId);
            }
            stmt.close();

            PartTypeImpl partType = loadPartType(partTypeId, user, conn);
            PartTypeImpl.IntimateAccess partTypeInt = partType.getIntimateAccess(this);

            deleteLocalizedString(partTypeInt.getLabelId(), conn);
            deleteLocalizedString(partTypeInt.getDescriptionId(), conn);

            stmt = conn.prepareStatement("delete from part_types where part_id = ?");
            stmt.setLong(1, partTypeId);
            stmt.execute();

            XmlObject eventDescription = createPartTypeDeletedEvent(partType, user);
            eventHelper.createEvent(eventDescription, "PartTypeDeleted", conn);

            conn.commit();
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new RepositoryException("Error deleting part type " + partTypeId, e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }

        context.getRepositorySchema().fireSchemaEvent(RepositorySchemaEventType.PART_TYPE_DELETED, partTypeId, -1);
    }

    private PartTypeDeletedDocument createPartTypeDeletedEvent(PartTypeImpl partType, AuthenticatedUser deleter) {
        PartTypeDeletedDocument partTypeDeletedDocument = PartTypeDeletedDocument.Factory.newInstance();
        PartTypeDeletedDocument.PartTypeDeleted partTypeDeleted = partTypeDeletedDocument.addNewPartTypeDeleted();
        partTypeDeleted.addNewDeletedPartType().setPartType(partType.getXml().getPartType());
        partTypeDeleted.setDeleterId(deleter.getId());
        partTypeDeleted.setDeletedTime(new GregorianCalendar());
        return partTypeDeletedDocument;
    }

    /**
     * Stores the contents of a LocaleMap in the table containing the localized strings. This
     * is done by first deleting all entries and then reinserting them.
     */
    private void storeLocalizedStrings(long id, Connection conn, LocaleMap localizedStrings) throws SQLException {
        Iterator entryIt = localizedStrings.entrySet().iterator();

        PreparedStatement stmt = null;
        try {
            deleteLocalizedString(id, conn);

            stmt = conn.prepareStatement("insert into localized_strings(id, locale, value) values(?,?,?)");

            while (entryIt.hasNext()) {
                Map.Entry entry = (Map.Entry)entryIt.next();
                String locale = (String)entry.getKey();
                if (locale.equals(""))
                    locale = " ";  // Workaround for Oracle "empty string becomes null" problem
                String label = (String)entry.getValue();
                stmt.setLong(1, id);
                stmt.setString(2, locale);
                stmt.setString(3, label);
                stmt.execute();
            }
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private void deleteLocalizedString(long id, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("delete from localized_strings where id = ?");
            stmt.setLong(1, id);
            stmt.execute();
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    public PartTypeImpl getPartTypeById(long id, AuthenticatedUser user) throws RepositoryException {
        Connection conn = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);
            return loadPartType(id, user, conn);
        } catch (SQLException e) {
            throw new RepositoryException("Error retrieving part type with id " + id + ".", e);
        } finally {
            jdbcHelper.closeConnection(conn);
        }
    }

    private PartTypeImpl loadPartType(long partTypeId, AuthenticatedUser user, Connection conn) throws RepositoryException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select name, label_id, description_id, mimetype, daisy_html, linkextractor, deprecated, last_modified, last_modifier, updatecount from part_types where part_id = ?");
            stmt.setLong(1, partTypeId);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                throw new PartTypeNotFoundException(partTypeId);

            String name = rs.getString("name");
            String mimeTypes = rs.getString("mimetype").trim(); // the trim() balances the effect of the Oracle work-around in the store method

            PartTypeImpl partType = new PartTypeImpl(name, mimeTypes, this, user);
            PartTypeImpl.IntimateAccess partTypeInt = partType.getIntimateAccess(this);

            partType.setDaisyHtml(rs.getBoolean("daisy_html"));
            partTypeInt.setId(partTypeId);
            partType.setLinkExtractor(rs.getString("linkextractor"));
            partType.setDeprecated(rs.getBoolean("deprecated"));
            partTypeInt.setLabelId(rs.getLong("label_id"));
            partTypeInt.setDescriptionId(rs.getLong("description_id"));
            partTypeInt.setLastModified(rs.getTimestamp("last_modified"));
            partTypeInt.setLastModifier(rs.getLong("last_modifier"));
            partTypeInt.setUpdateCount(rs.getLong("updatecount"));

            loadLocalizedStrings(partTypeInt.getLabelId(), conn, partTypeInt.getLabels());
            loadLocalizedStrings(partTypeInt.getDescriptionId(), conn, partTypeInt.getDescriptions());

            return partType;

        } catch (PartTypeNotFoundException e) {
            throw e;
        } catch (Throwable e) {
            throw new RepositoryException("Error retrieving part type with id " + partTypeId + ".", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    public PartTypeImpl getPartTypeByName(String name, AuthenticatedUser user) throws RepositoryException {
        Connection conn = null;
        PreparedStatement stmt = null;
        long id;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement("select part_id from part_types where name = ?");
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                throw new PartTypeNotFoundException(name);

            id = rs.getLong(1);

            return loadPartType(id, user, conn);
        } catch (PartTypeNotFoundException e) {
            throw e;
        } catch (Throwable e) {
            throw new RepositoryException("Error retrieving the part type named " + name, e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public void loadLocalizedStrings(long id, Connection conn, LocaleMap localizedStrings) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select locale, value from localized_strings where id = ?");
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String locale = rs.getString(1).trim();
                String value = rs.getString(2);
                localizedStrings.put(LocaleHelper.parseLocale(locale), value);
            }
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }


    public FieldTypeImpl getFieldTypeById(long id, AuthenticatedUser user) throws RepositoryException {
        Connection conn = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);
            return loadFieldType(id, user, conn);
        } catch (SQLException e) {
            throw new RepositoryException("Error retrieving field type with id " + id + ".", e);
        } finally {
            jdbcHelper.closeConnection(conn);
        }
    }

    private FieldTypeImpl loadFieldType(long fieldTypeId, AuthenticatedUser user, Connection conn) throws RepositoryException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select name, label_id, description_id, valuetype, size, deprecated, acl_allowed, multivalue, hierarchical, selectionlist_type, selectlist_free_entry, selectlist_load_async, last_modified, last_modifier, updatecount from field_types where id=?");
            stmt.setLong(1, fieldTypeId);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                throw new FieldTypeNotFoundException(fieldTypeId);

            String name = rs.getString("name");
            ValueType type = ValueType.getByCode(rs.getInt("valuetype"));
            boolean multiValue = rs.getBoolean("multivalue");
            boolean hierarchical = rs.getBoolean("hierarchical");

            FieldTypeImpl fieldType = new FieldTypeImpl(name, type, multiValue, hierarchical, this, user);
            FieldTypeImpl.IntimateAccess fieldTypeInt = fieldType.getIntimateAccess(this);
            fieldTypeInt.setId(fieldTypeId);
            fieldType.setDeprecated(rs.getBoolean("deprecated"));
            fieldType.setAclAllowed(rs.getBoolean("acl_allowed"));
            fieldTypeInt.setLabelId(rs.getLong("label_id"));
            fieldTypeInt.setDescriptionId(rs.getLong("description_id"));
            fieldTypeInt.setLastModified(rs.getTimestamp("last_modified"));
            fieldTypeInt.setLastModifier(rs.getLong("last_modifier"));
            fieldTypeInt.setUpdateCount(rs.getLong("updatecount"));
            fieldType.setSize(rs.getInt("size"));
            char selectionListType = rs.getString("selectionlist_type").charAt(0);
            fieldType.setAllowFreeEntry(rs.getBoolean("selectlist_free_entry"));
            fieldType.setLoadSelectionListAsync(rs.getBoolean("selectlist_load_async"));
            loadLocalizedStrings(fieldTypeInt.getLabelId(), conn, fieldTypeInt.getLabels());
            loadLocalizedStrings(fieldTypeInt.getDescriptionId(), conn, fieldTypeInt.getDescriptions());

            stmt.close();

            if (selectionListType == NO_SELECTIONLIST_TYPE) {
                // do nothing
            } else if (selectionListType == STATIC_SELECTIONLIST_TYPE) {
                StaticSelectionListImpl selList = (StaticSelectionListImpl)fieldType.createStaticSelectionList();

                stmt = conn.prepareStatement("select sequencenr, depth, stringvalue, datevalue, datetimevalue, integervalue, floatvalue, decimalvalue, booleanvalue, link_docid, ns.name_ as link_ns, link_branchid, link_langid, label_id from selectionlist_data left join daisy_namespaces ns on (selectionlist_data.link_nsid = ns.id) where fieldtype_id=? order by sequencenr");
                stmt.setLong(1, fieldTypeId);
                rs = stmt.executeQuery();
                int depth = 0;
                Stack<StaticListItemImpl> itemStack = new Stack<StaticListItemImpl>();
                StaticListItemImpl parentItem = selList.getIntimateAccess(this).getRoot();
                StaticListItemImpl listItem = null;

                while (rs.next()) {
                    int newDepth = rs.getInt("depth");
                    if (newDepth > depth) {
                        itemStack.push(parentItem);
                        parentItem = listItem;
                        depth = newDepth;
                    } else if (newDepth < depth) {
                        while (depth > newDepth) {
                            parentItem = itemStack.pop();
                            depth--;
                        }
                    }

                    FieldValueGetter fieldValueGetter = getValueGetter(type);
                    listItem = (StaticListItemImpl)parentItem.createItem(fieldValueGetter.getValue(rs));

                    long localizedLabelId = rs.getLong("label_id");
                    if (localizedLabelId != -1) {
                        StaticListItemImpl.IntimateAccess listItemInt = listItem.getIntimateAccess(this);
                        loadLocalizedStrings(localizedLabelId, conn, listItemInt.getLabels());
                    }
                }
                stmt.close();
            } else if (selectionListType == LINKQUERY_SELECTIONLIST_TYPE) {
                stmt = conn.prepareStatement("select whereclause, filtervariants from linkquerysellist where fieldtype_id = ?");
                stmt.setLong(1, fieldTypeId);
                rs = stmt.executeQuery();
                rs.next();
                String whereClause = rs.getString(1);
                boolean filterVariants = rs.getBoolean(2);
                fieldType.createLinkQuerySelectionList(whereClause, filterVariants);
                stmt.close();
            } else if (selectionListType == QUERY_SELECTIONLIST_TYPE) {
                stmt = conn.prepareStatement("select query, filtervariants, sort_order from querysellist where fieldtype_id = ?");
                stmt.setLong(1, fieldTypeId);
                rs = stmt.executeQuery();
                rs.next();
                String query = rs.getString(1);
                boolean filterVariants = rs.getBoolean(2);
                SortOrder sortOrder = SortOrder.fromCode(rs.getString(3).charAt(0));
                fieldType.createQuerySelectionList(query, filterVariants, sortOrder);
                stmt.close();
            } else if (selectionListType == HIERQUERY_SELECTIONLIST_TYPE) {
                stmt = conn.prepareStatement("select fieldname from hierquerysellist_fields where fieldtype_id = ? order by sequencenr");
                stmt.setLong(1, fieldTypeId);
                rs = stmt.executeQuery();
                List<String> fieldTypeNames = new ArrayList<String>();
                while (rs.next())
                    fieldTypeNames.add(rs.getString(1));
                stmt.close();
                stmt = conn.prepareStatement("select whereclause, filtervariants from hierquerysellist where fieldtype_id = ?");
                stmt.setLong(1, fieldTypeId);
                rs = stmt.executeQuery();
                rs.next();
                String whereClause = rs.getString(1);
                boolean filterVariants = rs.getBoolean(2);
                fieldType.createHierarchicalQuerySelectionList(whereClause, fieldTypeNames.toArray(new String[0]), filterVariants);
            } else if (selectionListType == PARENTLINKED_SELECTIONLIST_TYPE) {
                stmt = conn.prepareStatement("select whereclause, linkfield, filtervariants from parentlinkedsellist where fieldtype_id = ?");
                stmt.setLong(1, fieldTypeId);
                rs = stmt.executeQuery();
                rs.next();
                String whereClause = rs.getString(1);
                String linkField = rs.getString(2);
                boolean filterVariants = rs.getBoolean(3);
                fieldType.createParentLinkedSelectionList(whereClause, linkField, filterVariants);
                stmt.close();
            } else {
                throw new Exception("Invalid selection list type in repository for field type " + fieldTypeId + " : " + selectionListType);
            }

            return fieldType;
        } catch (FieldTypeNotFoundException e) {
            throw e;
        } catch (Throwable e) {
            throw new RepositoryException("Error retrieving field type with id " + fieldTypeId + ".", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    public FieldTypeImpl getFieldTypeByName(String name, AuthenticatedUser user) throws RepositoryException {
        Connection conn = null;
        PreparedStatement stmt = null;
        long id;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement("select id from field_types where name = ?");
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                throw new FieldTypeNotFoundException(name);

            id = rs.getLong(1);
            stmt.close();

            return loadFieldType(id, user, conn);
        } catch (FieldTypeNotFoundException e) {
            throw e;
        } catch (Throwable e) {
            throw new RepositoryException("Error retrieving the field type named " + name, e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public DocumentTypeImpl getDocumentTypeById(long id, AuthenticatedUser user) throws RepositoryException {
        Connection conn = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);
            return loadDocumentType(id, user, conn);
        } catch (SQLException e) {
            jdbcHelper.rollback(conn);
            throw new RepositoryException("Error retrieving document type with id " + id + ".", e);
        } finally {
            jdbcHelper.closeConnection(conn);
        }
    }

    private DocumentTypeImpl loadDocumentType(long documentTypeId, AuthenticatedUser user, Connection conn) throws RepositoryException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select name, deprecated, label_id, description_id, last_modified, last_modifier, updatecount from document_types where id = ?");
            stmt.setLong(1, documentTypeId);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                throw new DocumentTypeNotFoundException(documentTypeId);

            String name = rs.getString("name");

            DocumentTypeImpl docType = new DocumentTypeImpl(name, this, context.getCommonRepository(), user);
            DocumentTypeImpl.IntimateAccess docTypeInt = docType.getIntimateAccess(this);
            docTypeInt.setId(documentTypeId);
            docType.setDeprecated(rs.getBoolean("deprecated"));
            docTypeInt.setLabelId(rs.getLong("label_id"));
            docTypeInt.setDescriptionId(rs.getLong("description_id"));
            docTypeInt.setLastModified(rs.getTimestamp("last_modified"));
            docTypeInt.setLastModifier(rs.getLong("last_modifier"));
            docTypeInt.setUpdateCount(rs.getLong("updatecount"));
            stmt.close();

            loadLocalizedStrings(docTypeInt.getLabelId(), conn, docTypeInt.getLabels());
            loadLocalizedStrings(docTypeInt.getDescriptionId(), conn, docTypeInt.getDescriptions());

            // load associations with fields (unmodifiable cache copies)
            stmt = conn.prepareStatement("select field_id, required, editable from doctypes_fieldtypes where doctype_id = ? order by sequencenr");
            stmt.setLong(1, documentTypeId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                long fieldId = rs.getLong(1);
                boolean required = rs.getBoolean(2);
                FieldType fieldType = context.getRepositorySchema().getFieldTypeById(fieldId, false, user);
                FieldTypeUse fieldTypeUse = docType.addFieldType(fieldType, required);
                fieldTypeUse.setEditable(rs.getBoolean(3));
            }
            stmt.close();

            // load associations with part types (unmodifiable cache copies)
            stmt = conn.prepareStatement("select part_id, required, editable from doctype_contentmodel where doctype_id = ? order by sequencenr");
            stmt.setLong(1, documentTypeId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                long partId = rs.getLong(1);
                boolean required = rs.getBoolean(2);
                PartType partType = context.getRepositorySchema().getPartTypeById(partId, false, user);
                PartTypeUse partTypeUse = docType.addPartType(partType, required);
                partTypeUse.setEditable(rs.getBoolean(3));
            }
            stmt.close();

            return docType;
        } catch (DocumentTypeNotFoundException e) {
            throw e;
        } catch (Throwable e) {
            throw new RepositoryException("Error retrieving document type with id " + documentTypeId + ".", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    public DocumentTypeImpl getDocumentTypeByName(String name, AuthenticatedUser user) throws RepositoryException {
        Connection conn = null;
        PreparedStatement stmt = null;
        long id;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement("select id from document_types where name = ?");
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                throw new DocumentTypeNotFoundException(name);

            id = rs.getLong(1);

            return loadDocumentType(id, user, conn);
        } catch (DocumentTypeNotFoundException e) {
            throw e;
        } catch (Throwable e) {
            throw new RepositoryException("Error retrieving the document type named " + name, e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public Collection<PartTypeImpl> getAllPartTypes(final AuthenticatedUser user) throws RepositoryException {
        return getAllOfSomething("part_id", "part_types", "PartTypes", new IdGettable<PartTypeImpl>() {
            public PartTypeImpl getById(long id, Connection conn) throws RepositoryException {
                try {
                    return loadPartType(id, user, conn);
                } catch (PartTypeNotFoundException e) {
                    throw new RepositoryException("Strange situation: the part type with ID " + id + " does not exist.");
                }
            }
        });
    }

    public Collection<FieldTypeImpl> getAllFieldTypes(final AuthenticatedUser user) throws RepositoryException {
        return getAllOfSomething("id", "field_types", "FieldTypes", new IdGettable<FieldTypeImpl>() {
            public FieldTypeImpl getById(long id, Connection conn) throws RepositoryException {
                return loadFieldType(id, user, conn);
            }
        });
    }

    public Collection<DocumentTypeImpl> getAllDocumentTypes(final AuthenticatedUser user) throws RepositoryException {
        return getAllOfSomething("id", "document_types", "DocumentTypes", new IdGettable<DocumentTypeImpl>() {
            public DocumentTypeImpl getById(long id, Connection conn) throws RepositoryException {
                return loadDocumentType(id, user, conn);
            }
        });
    }

    public <T> Collection<T> getAllOfSomething(String idColumnName, String tableName, String name, IdGettable<T> idGettable) throws RepositoryException {
        ArrayLongList ids = new ArrayLongList(50);

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            // retrieve the id's of all the objects
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement("select " + idColumnName + " from " + tableName);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                long id = rs.getLong(1);
                ids.add(id);
            }
            stmt.close();

            // now effectively load the objects
            List<T> objects = new ArrayList<T>();
            LongIterator partIdIt = ids.iterator();
            while (partIdIt.hasNext()) {
                long id = partIdIt.next();
                objects.add(idGettable.getById(id, conn));
            }

            return objects;
        } catch (Throwable e) {
            throw new RepositoryException("Error retrieving " + name, e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public LinkExtractorInfos getLinkExtractors(AuthenticatedUser user) {
        return new LinkExtractorInfosImpl(context.getLinkExtractors());
    }

    public ExpSelectionListDocument getExpandedSelectionListData(long fieldTypeId, long branchId, long languageId, Locale locale, AuthenticatedUser user) throws RepositoryException {
        return null;
    }

    interface IdGettable<T> {
        public T getById(long id, Connection conn) throws RepositoryException;
    }

    public static FieldValueGetter getValueGetter(ValueType valueType) {
        if (valueType == ValueType.STRING)
            return STRING_FIELD_VALUE_GETTER;
        else if (valueType == ValueType.DATE)
            return DATE_FIELD_VALUE_GETTER;
        else if (valueType == ValueType.DATETIME)
            return DATETIME_FIELD_VALUE_GETTER;
        else if (valueType == ValueType.LONG)
            return LONG_FIELD_VALUE_GETTER;
        else if (valueType == ValueType.DOUBLE)
            return DOUBLE_FIELD_VALUE_GETTER;
        else if (valueType == ValueType.DECIMAL)
            return DECIMAL_FIELD_VALUE_GETTER;
        else if (valueType == ValueType.BOOLEAN)
            return BOOLEAN_FIELD_VALUE_GETTER;
        else if (valueType == ValueType.LINK)
            return LINK_FIELD_VALUE_GETTER;
        else
            throw new RuntimeException("Unrecognized ValueType: " + valueType);
    }

    public static interface FieldValueGetter {
        Object getValue(ResultSet rs) throws SQLException;
    }

    private static FieldValueGetter STRING_FIELD_VALUE_GETTER = new FieldValueGetter() {
        public Object getValue(ResultSet rs) throws SQLException {
            return rs.getString("stringvalue");
        }
    };

    private static FieldValueGetter DATE_FIELD_VALUE_GETTER = new FieldValueGetter() {
        public Object getValue(ResultSet rs) throws SQLException {
            return DateUtil.getNormalizedDate(rs.getDate("datevalue"), false);
        }
    };

    private static FieldValueGetter DATETIME_FIELD_VALUE_GETTER = new FieldValueGetter() {
        public Object getValue(ResultSet rs) throws SQLException {
            return DateUtil.getNormalizedDate(rs.getTimestamp("datetimevalue"), true);
        }
    };

    private static FieldValueGetter LONG_FIELD_VALUE_GETTER = new FieldValueGetter() {
        public Object getValue(ResultSet rs) throws SQLException {
            return new Long(rs.getLong("integervalue"));
        }
    };

    private static FieldValueGetter DOUBLE_FIELD_VALUE_GETTER = new FieldValueGetter() {
        public Object getValue(ResultSet rs) throws SQLException {
            return new Double(rs.getDouble("floatvalue"));
        }
    };

    private static FieldValueGetter DECIMAL_FIELD_VALUE_GETTER = new FieldValueGetter() {
        public Object getValue(ResultSet rs) throws SQLException {
            return rs.getBigDecimal("decimalvalue");
        }
    };

    private static FieldValueGetter BOOLEAN_FIELD_VALUE_GETTER = new FieldValueGetter() {
        public Object getValue(ResultSet rs) throws SQLException {
            return rs.getBoolean("booleanvalue") ? Boolean.TRUE : Boolean.FALSE;
        }
    };

    private static FieldValueGetter LINK_FIELD_VALUE_GETTER = new FieldValueGetter() {
        public Object getValue(ResultSet rs) throws SQLException {
            String documentId = rs.getLong("link_docid") + "-"  + rs.getString("link_ns");            
            long branchId = rs.getLong("link_branchid");
            long languageId = rs.getLong("link_langid");
            return new VariantKey(documentId, branchId, languageId);
        }
    };
}
