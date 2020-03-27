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
package org.outerj.daisy.query.model;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.query.QueryException;

/**
 * Context object used during the generation of SQL from a daisy query.
 *
 * <p>The main goal of this class is to keep track of which tables are
 * required by the conditions, thus which tables will need to be joined in
 * the "from" clause of the SQL statement. Next to that, it also contains
 * constants for table and field names so that those aren't scattered
 * around everywhere in the code.
 */
public class SqlGenerationContext {
    private List<Table> joinTables = new ArrayList<Table>();
    private int aliasCounter = 0;
    private final JdbcHelper jdbcHelper;
    private VersionMode versionMode;
    private boolean includeRetired;
    private Map<String, DereferenceContext> derefContexts = new HashMap<String, DereferenceContext>();
    private Stack<DereferenceContext> derefContextStack = new Stack<DereferenceContext>();
    private List<String> extraWhereConditions = new ArrayList<String>();
    private List<SqlBinder> extraWhereConditionBinds = new ArrayList<SqlBinder>();
    private final EvaluationInfo evaluationInfo;

    public SqlGenerationContext(JdbcHelper jdbcHelper, VersionMode versionMode, boolean includeRetired, EvaluationInfo evaluationInfo) {
        this.jdbcHelper = jdbcHelper;
        this.versionMode = versionMode;
        this.includeRetired = includeRetired;
        this.evaluationInfo = evaluationInfo;

        DocumentVariantsTable docVariantsTable = new DocumentVariantsTable();
        DereferenceContext rootDerefContext = new DereferenceContext(docVariantsTable, "root");
        derefContextStack.push(rootDerefContext);
        
        if (!includeRetired) {
            addExtraWhereCondition("document_variants.retired = ?", new SqlBinder() {
                public int doBind(int bindPos, PreparedStatement stmt) throws SQLException {
                    stmt.setBoolean(bindPos++, false);
                    return bindPos;
                }
            });
        }
        if (versionMode.isLast()) {
            // intentionally empty
        } else if (versionMode.isLive()) {
            addExtraWhereCondition("document_variants.liveversion_id is not null", null);
        } else {
            addExtraWhereCondition(getVersionFieldTable() + "." + getVersionField() + " is not null", null);
        }

    }

    public EvaluationInfo getEvaluationInfo() {
        return evaluationInfo;
    }
    
    public String getNewAliasCounter() {
        aliasCounter++;
        return String.valueOf(aliasCounter);
    }

    public JdbcHelper getJdbcHelper() {
        return jdbcHelper;
    }

    public void pushDereference(ValueExpr refValueExpr) throws QueryException {
        String refKey = getRefKey(refValueExpr);
        DereferenceContext derefContext = derefContexts.get(refKey);
        boolean newContext = derefContext == null;
        if (newContext) {
            DocumentVariantsTable table = new JoinedDocumentVariantsTable(refValueExpr, this);

            derefContext = new DereferenceContext(table, refKey);
            derefContexts.put(refKey, derefContext);
            
        }
        derefContextStack.push(derefContext);
        if (newContext) {
            if (!includeRetired)
                addExtraWhereCondition(getDocumentVariantsTable().getName()+".retired = ? ", new SqlBinder() {
                    public int doBind(int bindPos, PreparedStatement stmt) throws SQLException {
                        stmt.setBoolean(bindPos++, false);
                        return bindPos;
                    }
                });
    
            if (versionMode.isLast()) {
                //intentionally empty
            } else if (versionMode.isLive()) {
                addExtraWhereCondition(getVersionFieldTable()+".liveversion_id is not null", null);
            } else {
                addExtraWhereCondition(getVersionFieldTable()+"."+getVersionField() + " is not null", null);
            }
        }

    }

    private void addExtraWhereCondition(String condition, SqlBinder sqlBinder) {
        extraWhereConditions.add(condition);
        if (sqlBinder != null) {
            extraWhereConditionBinds.add(sqlBinder);
        }
    }

    /**
     * Returns a string that uniquely identifies the current dereference hierarchy.
     */
    private String getRefKey(ValueExpr valueExpr) {
        return derefContextStack.peek().getKey() + "=>" + valueExpr.getExpression();
    }

    public void popDereference() {
        derefContextStack.pop();
    }

    public class DereferenceContext {
        private Map<Class<? extends Table>, Table> tables = new HashMap<Class<? extends Table>, Table>();
        private String key;
        
        public DereferenceContext(DocumentVariantsTable table, String key) {
            tables.put(DocumentVariantsTable.class, table);
            needsJoinWithTable(table);
            this.key = key;
        }

        public Table getTable(Class<? extends Table> clazz) {
            Table table = tables.get(clazz);
            if (table == null) {
                try {
                    table = clazz.getConstructor(SqlGenerationContext.class).newInstance(SqlGenerationContext.this);
                } catch (Exception e) {
                    throw new RuntimeException("Unexpected error trying to instantiate table descriptor class.", e);
                }
                tables.put(clazz, table);
                needsJoinWithTable(table);
            }
            return table;
        }

        public String getKey() {
            return key;
        }
    }

    public DocumentsTable getDocumentsTable() {
        return (DocumentsTable)derefContextStack.peek().getTable(DocumentsTable.class);
    }

    public DocumentVariantsTable getDocumentVariantsTable() {
        return (DocumentVariantsTable)derefContextStack.peek().getTable(DocumentVariantsTable.class);
    }

    public VersionsTable getVersionsTable() {
        return (VersionsTable)derefContextStack.peek().getTable(VersionsTable.class);
    }

    public NamespacesTable getNamespacesTable() {
        return (NamespacesTable)derefContextStack.peek().getTable(NamespacesTable.class);
    }

    public LocksTable getLocksTable() {
        return (LocksTable)derefContextStack.peek().getTable(LocksTable.class);
    }

    public SummariesTable getSummariesTable() {
        return (SummariesTable)derefContextStack.peek().getTable(SummariesTable.class);
    }

    public ExtractedLinksTable getExtractedLinksTable() {
        return (ExtractedLinksTable)derefContextStack.peek().getTable(ExtractedLinksTable.class);
    }

    public InverseExtractedLinksTable getInverseExtractedLinksTable() {
        return (InverseExtractedLinksTable)derefContextStack.peek().getTable(InverseExtractedLinksTable.class);
    }

    public PartsTable getNewPartsTable() {
        PartsTable partsTable = new PartsTable(this);
        needsJoinWithTable(partsTable);
        return partsTable;
    }

    public FieldsTable getNewFieldsTable(long fieldTypeId) {
        FieldsTable fieldsTable = new FieldsTable(this, fieldTypeId);
        needsJoinWithTable(fieldsTable);
        return fieldsTable;
    }

    public CollectionsTable getNewCollectionsTable() {
        CollectionsTable table = new CollectionsTable(this);
        needsJoinWithTable(table);
        return table;
    }

    public CustomFieldsTable getNewCustomFieldsTable(String customFieldName) {
        CustomFieldsTable table = new CustomFieldsTable(this, customFieldName);
        needsJoinWithTable(table);
        return table;
    }
    
    /**
     * Adds a table to join with. If the same Table object is added a second
     * time, it is ignored. The joins will be performed in the order as added
     * here.
     */
    public void needsJoinWithTable(Table table) {
        if (!joinTables.contains(table)) // don't join twice on the same table
            joinTables.add(table);
    }

    private static final ParamString SELECT_EXPR = new ParamString("select distinct {documents}.id as id, {daisy_namespaces}.name_ as ns_name, {document_variants}.branch_id as branch_id, {document_variants}.lang_id as lang_id");
    public void appendDocIdSelectClause(StringBuilder sql) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("documents", getDocumentsTable().getName());
        params.put("document_variants", getDocumentVariantsTable().getName());
        params.put("daisy_namespaces", getNamespacesTable().getName());
        sql.append(SELECT_EXPR.toString(params));
    }

    public int bindJoins(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        for (Table table : joinTables) {
            bindPos = table.bindJoin(stmt, bindPos, evaluationInfo);
        }
        return bindPos;
    }

    /**
     * Generates the SQL "from" expression with all the required joins
     * (as added via {@link #needsJoinWithTable}.
     */
    public void appendFromClause(StringBuilder sql) throws QueryException {
        getRequiredTables();
        
        sql.append(" from document_variants ");

        for (Table table : joinTables) {
            String joinExpr = table.getJoinExpression();
            if (joinExpr != null) // joinExpr is null for the (unjoined, root) document_variants table, as that is base table in the from clause
                sql.append(joinExpr);
        }
    }

    /**
     * When doing a point-in-time search the join with the versions table is required
     */
    private void getRequiredTables() {
        if (!versionMode.isLast() && !versionMode.isLive()) {
            getVersionsTable();
        }
        
    }

    public void appendExtraWhereConditions(StringBuilder sql) {
        for (String condition: extraWhereConditions) {
            sql.append(" and ").append(condition);
        }
    }

    public int bindExtraWhereConditions(PreparedStatement stmt, int bindPos) throws SQLException {
        for (SqlBinder binder: extraWhereConditionBinds) {
            bindPos = binder.doBind(bindPos, stmt);
        }
        return bindPos;
    }
    
    public String getVersionField() {
        if (versionMode.isLast())
            return "lastversion_id";
        else if (versionMode.isLive())
            return "liveversion_id";
        else
            return "id";
    }

    public String getVersionFieldTable() {
        if (versionMode.isLast() || versionMode.isLive()) {
            return getDocumentVariantsTable().getName();
        } else {
            return getVersionsTable().getName();
        }
    }

    public interface Table {
        public String getName();

        public String getJoinExpression() throws QueryException;

        public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException;
    }

    /**
     * Meta information about the association table between documents and collections.
     */
    public static final class DocsCollectionsTable {
        public static final String COLLECTION_ID = "collection_id";
    }

    /**
     * Meta information about the table containing version information.
     */
    public static final class VersionsTable implements Table {
        private static final ParamString DEFAULT_VERSIONS_ALIAS_JOIN = new ParamString(" left outer join document_versions {alias} on ({document_variants}.doc_id = {alias}.doc_id and {document_variants}.ns_id = {alias}.ns_id and {document_variants}.branch_id = {alias}.branch_id and {document_variants}.lang_id = {alias}.lang_id and {document_variants}.{version_field} = {alias}.id)");
        // for point in time searches we need two joins to get the versions table
        private static final ParamString POINT_IN_TIME_VERSIONS_ALIAS_JOIN = new ParamString(" left outer join live_history {lh_alias} on ({document_variants}.doc_id = {lh_alias}.doc_id and {document_variants}.ns_id = {lh_alias}.ns_id and {document_variants}.branch_id = {lh_alias}.branch_id and {document_variants}.lang_id = {lh_alias}.lang_id and {lh_alias}.begin_date <= ? and ({lh_alias}.end_date is null or {lh_alias}.end_date > ?)) "
                                                                                    +" left outer join document_versions {alias} on ({lh_alias}.doc_id = {alias}.doc_id and {lh_alias}.ns_id = {alias}.ns_id and {lh_alias}.branch_id = {alias}.branch_id and {lh_alias}.lang_id = {alias}.lang_id and {lh_alias}.version_id = {alias}.{version_field})");
        private Map<String, String> params;
        private SqlGenerationContext context;

        public VersionsTable(SqlGenerationContext context) {
            params = new HashMap<String, String>();
            
            this.context = context;
            
            String alias = "versions" + context.getNewAliasCounter();
            context.addExtraWhereCondition(alias + ".id is not null", null);

            params.put("alias", alias);
            params.put("document_variants", context.getDocumentVariantsTable().getName());
            
            if (!context.versionMode.isLast() && !context.versionMode.isLive()) {
                String lh_alias = "live_history" + context.getNewAliasCounter();
                params.put("lh_alias", lh_alias);
            }

        }

        public final String getName() {
            return params.get("alias");
        }

        public final String getJoinExpression() {
            params.put("version_field", context.getVersionField());
            params.put("version_field_table", context.getVersionFieldTable());
            
            if (context.versionMode.isLive() || context.versionMode.isLast()) {
                return DEFAULT_VERSIONS_ALIAS_JOIN.toString(params);
            } else {
                return POINT_IN_TIME_VERSIONS_ALIAS_JOIN.toString(params);
            }
        }

        public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException {
            if (!context.versionMode.isLive() && !context.versionMode.isLast()) {
                stmt.setTimestamp(bindPos++, new Timestamp(context.versionMode.getDate().getTime()));
                stmt.setTimestamp(bindPos++, new Timestamp(context.versionMode.getDate().getTime()));
            }
            return bindPos;
        }

        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String CREATED_ON = "created_on";
        public static final String CREATED_BY = "created_by";
        public static final String STATE = "state";
        public static final String TOTAL_SIZE_OF_PARTS = "total_size_of_parts";
        public static final String LAST_MODIFIED = "last_modified";
        public static final String CHANGE_COMMENT = "change_comment";
        public static final String CHANGE_TYPE = "change_type";
        public static final String SYNCED_WITH_VERSION_ID = "synced_with_version_id";
        public static final String SYNCED_WITH_LANG_ID = "synced_with_lang_id";
        public static final String SYNCED_WITH_SEARCH = "synced_with_search";
    }

    public static class ExtractedLinksTable implements Table {
        private static final ParamString DEFAULT_JOIN_EXPR = new ParamString(" left outer join extracted_links {alias} on ({document_variants}.doc_id = {alias}.source_doc_id and {document_variants}.ns_id = {alias}.source_ns_id and {document_variants}.branch_id = {alias}.source_branch_id and {document_variants}.lang_id = {alias}.source_lang_id)"
                + " left outer join document_variants {dv_alias} on ({alias}.target_doc_id = {dv_alias}.doc_id and {alias}.target_ns_id = {dv_alias}.ns_id and {alias}.target_branch_id = {dv_alias}.branch_id and {alias}.target_lang_id = {dv_alias}.lang_id" +
                        " and {alias}.target_version_id = {dv_alias}.{mode}version_id)");
        private static final ParamString DATE_JOIN_EXPR = new ParamString(" left outer join extracted_links {alias} on ({document_variants}.doc_id = {alias}.source_doc_id and {document_variants}.ns_id = {alias}.source_ns_id and {document_variants}.branch_id = {alias}.source_branch_id and {document_variants}.lang_id = {alias}.source_lang_id)"
                + " left outer join live_history {lh_alias} on ({alias}.target_doc_id = {lh_alias}.doc_id and {alias}.target_ns_id = {lh_alias}.ns_id and {alias}.target_branch_id = {lh_alias}.branch_id and {alias}.target_lang_id = {lh_alias}.lang_id"
                      + " and {lh_alias}.begin_date =< ? and {lh_alias}.end_date > ?)"
                + " left outer join document_versions {ver_alias} on ({lh_alias}.doc_id = {ver_alias}.doc_id and {lh_alias}.ns_id = {ver_alias}.ns_id and {lh_alias}.branch_id = {ver_alias}.branch_id and {lh_alias}.lang_id = {ver_alias}.lang_id"
                      +" and {lh_alias}.version_id = {ver_alias}.id)");

        private Map<String, String> params;
        protected SqlGenerationContext context;
        protected VersionMode mode;
        protected String dvAlias;
        protected String verAlias;

        public ExtractedLinksTable(SqlGenerationContext context) {
            this.context = context;
            this.mode = context.getEvaluationInfo().getVersionMode();
            params = new HashMap<String, String>();
            params.put("alias", getAliasPrefix() + context.getNewAliasCounter());

            mode = context.getEvaluationInfo().getVersionMode();
            params.put("mode", String.valueOf(mode));
            dvAlias = "document_variants" + context.getNewAliasCounter();
            params.put("dv_alias", dvAlias);
            params.put("lh_alias", "live_history" + context.getNewAliasCounter());
            verAlias = "document_versions" + context;
            params.put("ver_alias", verAlias);

            params.put("document_variants", context.getDocumentVariantsTable().getName());
        }
        public final String getName() {
            return params.get("alias");
        }

        public String getJoinExpression() {
            return getJoinExpr().toString(params);
        }

        protected ParamString getJoinExpr() {
            if (mode.isLast() || mode.isLive()) {
                return DEFAULT_JOIN_EXPR;
            } else {
                return DATE_JOIN_EXPR;
            }
        }

        protected String getAliasPrefix() {
            return "extracted_links";
        }

        public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException {
            if (mode.isLast() || mode.isLive()) {
                return bindPos;
            }
            stmt.setTimestamp(bindPos++, new Timestamp(mode.getDate().getTime()));
            stmt.setTimestamp(bindPos++, new Timestamp(mode.getDate().getTime()));
            return bindPos;
        }

        public static final String TARGET_DOC_ID = "target_doc_id";
        public static final String TARGET_NS_ID = "target_ns_id";
        public static final String TARGET_BRANCH_ID = "target_branch_id";
        public static final String TARGET_LANG_ID = "target_lang_id";
        public static final String SOURCE_DOC_ID = "source_doc_id";
        public static final String SOURCE_NS_ID = "source_ns_id";
        public static final String SOURCE_BRANCH_ID = "source_branch_id";
        public static final String SOURCE_LANG_ID = "source_lang_id";
        public static final String SOURCE_VERSION_ID = "source_version_id";
        public static final String IN_LAST_VERSION = "in_last_version";
        public static final String IN_LIVE_VERSION = "in_live_version";
        public static final String LINKTYPE = "linktype";

        public String getSourceVersionField() { // source version refers to the version field table 
            return context.getVersionFieldTable().concat(".").concat(context.getVersionField());
        }
    }

    public static final class InverseExtractedLinksTable extends ExtractedLinksTable {
        private static final ParamString DEFAULT_JOIN_EXPR = new ParamString(" left outer join extracted_links {alias} on ({document_variants}.doc_id = {alias}.target_doc_id and {document_variants}.ns_id = {alias}.target_ns_id and {document_variants}.branch_id = {alias}.target_branch_id and {document_variants}.lang_id = {alias}.target_lang_id)"
                            + " left outer join document_variants {dv_alias} on ({alias}.source_doc_id = {dv_alias}.doc_id and {alias}.source_ns_id = {dv_alias}.ns_id and {alias}.source_branch_id = {dv_alias}.branch_id and {alias}.source_lang_id = {dv_alias}.lang_id" +
                            		" and {alias}.source_version_id = {dv_alias}.{mode}version_id)");
        private static final ParamString DATE_JOIN_EXPR = new ParamString(" left outer join extracted_links {alias} on ({document_variants}.doc_id = {alias}.target_doc_id and {document_variants}.ns_id = {alias}.target_ns_id and {document_variants}.branch_id = {alias}.target_branch_id and {document_variants}.lang_id = {alias}.target_lang_id)"
                            + " left outer join live_history {lh_alias} on ({alias}.source_doc_id = {lh_alias}.doc_id and {alias}.source_ns_id = {lh_alias}.ns_id and {alias}.source_branch_id = {lh_alias}.branch_id and {alias}.source_lang_id = {lh_alias}.lang_id"
                                  + " and {lh_alias}.begin_date =< ? and {lh_alias}.end_date > ?)"
                            + " left outer join document_versions {ver_alias} on ({lh_alias}.doc_id = {ver_alias}.doc_id and {lh_alias}.ns_id = {ver_alias}.ns_id and {lh_alias}.branch_id = {ver_alias}.branch_id and {lh_alias}.lang_id = {ver_alias}.lang_id"
                                  +" and {lh_alias}.version_id = {ver_alias}.id)");
        
        public InverseExtractedLinksTable(SqlGenerationContext context) {
            super(context);
        }

        protected ParamString getJoinExpr() {
            if (mode.isLast()  || mode.isLive()) {
                return DEFAULT_JOIN_EXPR;
            } else {
                return DATE_JOIN_EXPR;
            }
        }

        protected String getAliasPrefix() {
            return "inv_extracted_links";
        }
        
        public String getSourceVersionField() { // source version refers to the version field table
            if (mode.isLast() || mode.isLive()) {
                return dvAlias.concat(".").concat(String.valueOf(mode)).concat("version_id");
            } else {
                return verAlias.concat(".id");
            }
        }

    }

    public static final class SummariesTable implements Table {
        private static final ParamString JOIN_EXPR = new ParamString(" left outer join summaries {alias} on ({document_variants}.doc_id = {alias}.doc_id and {document_variants}.ns_id = {alias}.ns_id and {document_variants}.branch_id = {alias}.branch_id and {document_variants}.lang_id = {alias}.lang_id)");
        private Map<String, String> params;

        public SummariesTable(SqlGenerationContext context) {
            this.params = new HashMap<String, String>();
            params.put("alias", "summaries" + context.getNewAliasCounter());
            params.put("document_variants", context.getDocumentVariantsTable().getName());
        }

        public final String getName() {
            return params.get("alias");
        }

        public String getJoinExpression() {
            return JOIN_EXPR.toString(params);
        }

        public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) {
            return bindPos;
        }

        public static final String SUMMARY = "summary";
    }

    public static final class PartsTable implements Table {
        private static final ParamString JOIN_EXPR = new ParamString(" left outer join parts {alias} on ({versionFieldTable}.doc_id = {alias}.doc_id and {versionFieldTable}.ns_id = {alias}.ns_id and {versionFieldTable}.branch_id = {alias}.branch_id and {versionFieldTable}.lang_id = {alias}.lang_id and {versionFieldTable}.{versionField} = {alias}.version_id)");
        private Map<String, String> params;
        private SqlGenerationContext context;

        public PartsTable(SqlGenerationContext context) {
            this.context = context;
            this.params = new HashMap<String, String>();
            params.put("alias", "parts" + context.getNewAliasCounter());
            params.put("document_variants", context.getDocumentVariantsTable().getName());
            params.put("versionField", context.getVersionField());
            params.put("versionFieldTable", context.getVersionFieldTable());
        }

        public final String getName() {
            return params.get("alias");
        }

        public String getJoinExpression() {
            return JOIN_EXPR.toString(params);
        }

        public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) {
            return bindPos;
        }

        public static final String MIMETYPE = "mimetype";
        public static final String SIZE = "blob_size";
        public static final String PARTTYPE_ID = "parttype_id";
    }

    public static final class LocksTable implements Table {
        private static final ParamString JOIN_EXPR = new ParamString(" left outer join locks {alias} on ({document_variants}.doc_id = {alias}.doc_id and {document_variants}.ns_id = {alias}.ns_id and {document_variants}.branch_id = {alias}.branch_id and {document_variants}.lang_id = {alias}.lang_id)");
        Map<String, String> params;

        public LocksTable(SqlGenerationContext context) {
            params = new HashMap<String, String>();
            params.put("alias", "locks" + context.getNewAliasCounter());
            params.put("document_variants", context.getDocumentVariantsTable().getName());
        }

        public String getName() {
            return params.get("alias");
        }

        public String getJoinExpression() {
            return JOIN_EXPR.toString(params);
        }

        public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) {
            return bindPos;
        }

        public static final String LOCKTYPE = "locktype";
        public static final String OWNER_ID = "user_id";
        public static final String TIME_ACQUIRED = "time_acquired";
        public static final String DURATION = "duration";
    }

    public static final class FieldsTable implements Table {
        // Note on this JOIN_EXPR: setting the condition on fieldtype_id ususally doesn't have to be in the
        // on-clause (could as well be in the where clause), except for some special operators like IS NULL,
        // and since it doesn't do any harm for the rest it's easier to make it the same for everything
        private static final ParamString JOIN_EXPR = new ParamString(" left outer join thefields {alias} on ({versionFieldTable}.doc_id = {alias}.doc_id and {versionFieldTable}.ns_id = {alias}.ns_id and {versionFieldTable}.branch_id = {alias}.branch_id and {versionFieldTable}.lang_id = {alias}.lang_id and {versionFieldTable}.{versionField} = {alias}.version_id and {alias}.fieldtype_id = ?)");
        private Map<String, String> params;
        private long fieldTypeId;
        private SqlGenerationContext context;

        public FieldsTable(SqlGenerationContext context, long fieldTypeId) {
            this.context = context;
            params = new HashMap<String, String>();
            params.put("alias", "fields" + context.getNewAliasCounter());
            params.put("document_variants", context.getDocumentVariantsTable().getName());
            params.put("versionField", context.getVersionField());
            params.put("versionFieldTable", context.getVersionFieldTable()); // side-effect: joins with VersionsTable in case of point-in-time searches. This join is added *before* this one (as needed). 
            this.fieldTypeId = fieldTypeId;
        }

        public String getName() {
            return params.get("alias");
        }

        public String getJoinExpression() {
            return JOIN_EXPR.toString(params);
        }

        public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException {
            stmt.setLong(bindPos++, fieldTypeId);
            return bindPos;
        }

        public static final String FIELDTYPE_ID = "fieldtype_id";
        public static final String STRINGVALUE = "stringvalue";
        public static final String DATEVALUE = "datevalue";
        public static final String DATETIMEVALUE = "datetimevalue";
        public static final String INTEGERVALUE = "integervalue";
        public static final String FLOATVALUE = "floatvalue";
        public static final String DECIMALVALUE = "decimalvalue";
        public static final String BOOLEANVALUE = "booleanvalue";
        public static final String LINK_DOCID = "link_docid";
        public static final String LINK_NSID = "link_nsid";
        public static final String LINK_SEARCHDOCID = "link_searchdocid";
        public static final String LINK_SEARCHBRANCHID = "link_searchbranchid";
        public static final String LINK_SEARCHLANGID = "link_searchlangid";
        public static final String LINK_SEARCH = "link_search";

        public static String getValueColumn(QValueType valueType) {
            String valueColumn;
            if (valueType == QValueType.STRING)
                valueColumn = STRINGVALUE;
            else if (valueType == QValueType.DATE)
                valueColumn = DATEVALUE;
            else if (valueType == QValueType.DATETIME)
                valueColumn = DATETIMEVALUE;
            else if (valueType == QValueType.DECIMAL)
                valueColumn = DECIMALVALUE;
            else if (valueType == QValueType.DOUBLE)
                valueColumn = FLOATVALUE;
            else if (valueType == QValueType.LONG)
                valueColumn = INTEGERVALUE;
            else if (valueType == QValueType.BOOLEAN)
                valueColumn = BOOLEANVALUE;
            else if (valueType == QValueType.LINK)
                valueColumn = LINK_SEARCH;
            else
                throw new RuntimeException("Not a valid field value type: " + valueType);
            return valueColumn;
        }
    }

    public static class DocumentVariantsTable implements Table {
        
        public String getName() {
            return "document_variants";
        }

        public String getJoinExpression() {
            return null;
        }

        public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
            return bindPos;
        }

        public static final String DOCTYPE_ID = "doctype_id";
        public static final String RETIRED = "retired";
        public static final String LAST_MODIFIER = "last_modifier";
        public static final String LAST_MODIFIED = "last_modified";
        public static final String BRANCH_ID = "branch_id";
        public static final String LANG_ID = "lang_id";
        public static final String LINK_SEARCH = "link_search";
        public static final String LAST_MAJOR_CHANGE_VERSION = "last_major_change_version_id";
        public static final String LIVE_MAJOR_CHANGE_VERSION = "live_major_change_version_id";
    }

    /**
     * This table class is used for dereference expressions, which is the only case where a new
     * join with the document variants table is needed.
     */
    public static class JoinedDocumentVariantsTable extends DocumentVariantsTable {
        private static final ParamString JOIN_EXPR = new ParamString(" left outer join document_variants {alias} on ({valueExpr} = {alias}.link_search)");
        private Map<String, String> params;
        private ValueExpr refValueExpr;

        public JoinedDocumentVariantsTable(ValueExpr refValueExpr, SqlGenerationContext context) throws QueryException {
            this.params = new HashMap<String, String>();
            String alias = "variants" + context.getNewAliasCounter();
            params.put("alias", alias);
            this.refValueExpr = refValueExpr;

            String refPreCond = refValueExpr.getSqlPreConditions(context);
            StringBuilder valueExprBuffer = new StringBuilder();
            if (refPreCond != null) {
                valueExprBuffer.append(refPreCond);
                valueExprBuffer.append(" and ");
            }
            refValueExpr.generateSqlValueExpr(valueExprBuffer, context);
            params.put("valueExpr", valueExprBuffer.toString());
        }

        public String getName() {
            return params.get("alias");
        }

        public String getJoinExpression() {
            return JOIN_EXPR.toString(params);
        }

        public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
            bindPos = refValueExpr.bindPreConditions(stmt, bindPos, evaluationInfo);
            bindPos = refValueExpr.bindValueExpr(stmt, bindPos, QValueType.LINK, evaluationInfo);
            return bindPos;
        }
    }

    public static final class DocumentsTable implements Table {
        private Map<String, String> params;
        private final ParamString JOIN_EXPR = new ParamString(" left outer join documents {alias} on ({document_variants}.doc_id = {alias}.id and {document_variants}.ns_id = {alias}.ns_id)");

        public DocumentsTable(SqlGenerationContext context) {
            this.params = new HashMap<String, String>();
            params.put("alias", "documents" + context.getNewAliasCounter());
            params.put("document_variants", context.getDocumentVariantsTable().getName());
        }

        public String getName() {
            return params.get("alias");
        }

        public String getJoinExpression() {
            return JOIN_EXPR.toString(params);
        }

        public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) {
            return bindPos;
        }

        public static final String CREATED = "created";
        public static final String ID = "id";
        public static final String NS_ID = "ns_id";
        public static final String ID_SEARCH = "id_search";
        public static final String PRIVATE = "private";
        public static final String OWNER = "owner";
        public static final String LAST_MODIFIER = "last_modifier";
        public static final String LAST_MODIFIED = "last_modified";
        public static final String REFERENCE_LANGUAGE = "reference_lang_id";
    }

    public static final class NamespacesTable implements Table {
        private Map<String, String> params;
        private final ParamString JOIN_EXPR = new ParamString(" left outer join daisy_namespaces {alias} on ({document_variants}.ns_id = {alias}.id)");

        public NamespacesTable(SqlGenerationContext context) {
            this.params = new HashMap<String, String>();
            params.put("alias", "namespaces" + context.getNewAliasCounter());
            params.put("document_variants", context.getDocumentVariantsTable().getName());
        }

        public String getName() {
            return params.get("alias");
        }

        public String getJoinExpression() {
            return JOIN_EXPR.toString(params);
        }

        public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) {
            return bindPos;
        }
    }

    public static class CollectionsTable implements Table {
        private static final ParamString JOIN_EXPR = new ParamString(" left outer join document_collections {alias} on ({document_variants}.doc_id = {alias}.document_id and {document_variants}.ns_id = {alias}.ns_id and {document_variants}.branch_id = {alias}.branch_id and {document_variants}.lang_id = {alias}.lang_id)");
        private Map<String, String> params;

        public CollectionsTable(SqlGenerationContext context) {
            params = new HashMap<String, String>();
            params.put("alias", "doc_colls" + context.getNewAliasCounter());
            params.put("document_variants", context.getDocumentVariantsTable().getName());
        }

        public String getName() {
            return params.get("alias");
        }

        public String getJoinExpression() {
            return JOIN_EXPR.toString(params);
        }

        public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) {
            return bindPos;
        }
    }

    public static class CustomFieldsTable implements Table {
        // See the note at the FieldsTable.JOIN_EXPR for why the condition on name is in the on-clause
        private static final ParamString JOIN_EXPR = new ParamString(" left outer join customfields {alias} on ({document_variants}.doc_id = {alias}.doc_id and {document_variants}.ns_id = {alias}.ns_id and {document_variants}.branch_id = {alias}.branch_id and {document_variants}.lang_id = {alias}.lang_id and {alias}.name = ?)");
        private Map<String, String> params;
        private String customFieldName;

        public CustomFieldsTable(SqlGenerationContext context, String customFieldName) {
            params = new HashMap<String, String>();
            params.put("alias", "customfields" + context.getNewAliasCounter());
            params.put("document_variants", context.getDocumentVariantsTable().getName());
            this.customFieldName = customFieldName;
        }

        public String getName() {
            return params.get("alias");
        }

        public String getJoinExpression() {
            return JOIN_EXPR.toString(params);
        }

        public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException {
            stmt.setString(bindPos++, customFieldName);
            return bindPos;
        }

        public static final String NAME = "name";
        public static final String VALUE = "value";
    }
    
    private interface SqlBinder {
        int doBind(int bindPos, PreparedStatement stmt) throws SQLException;
    }
}
