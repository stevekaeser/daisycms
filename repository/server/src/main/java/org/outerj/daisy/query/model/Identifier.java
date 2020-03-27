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

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.repository.AvailableVariant;
import org.outerj.daisy.repository.ChangeType;
import org.outerj.daisy.repository.CollectionNotFoundException;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.LockInfo;
import org.outerj.daisy.repository.LockType;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryRuntimeException;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.acl.AccessDetails;
import org.outerj.daisy.repository.acl.AclDetailPermission;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.DocumentTypeNotFoundException;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.BranchNotFoundException;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.repository.variant.LanguageNotFoundException;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;

public final class Identifier extends AbstractExpression implements ValueExpr, Cloneable {
    private final String id;
    private final String subId;
    private final ValueExpr multiValueIndexExpr;
    private final ValueExpr hierarchyIndexExpr;
    private DelegateIdentifier delegate;
    private QueryContext prepareOnlyQueryContext;
    private static final Map<String, Constructor> delegateClasses = new HashMap<String, Constructor>();
    static {
        try {
            delegateClasses.put(DocumentTypeIdentifier.NAME, DocumentTypeIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(DocumentNameIdentifier.NAME, DocumentNameIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(CreationTimeIdentifier.NAME, CreationTimeIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(DocumentIdIdentifier.NAME, DocumentIdIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(DocumentNamespaceIdentifier.NAME, DocumentNamespaceIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(DocumentLinkIdentifier.NAME, DocumentLinkIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(SummaryIdentifier.NAME, SummaryIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(VersionCreationTimeIdentifier.NAME, VersionCreationTimeIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(VersionCreatorIdIdentifier.NAME, VersionCreatorIdIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(VersionCreatorNameIdentifier.NAME, VersionCreatorNameIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(VersionCreatorLoginIdentifier.NAME, VersionCreatorLoginIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(VersionStateIdentifier.NAME, VersionStateIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(TotalSizeOfPartsIdentifier.NAME, TotalSizeOfPartsIdentifier.class.getConstructor(Identifier.class));
            // note: the old (pre 2.2 name) of this property is deprecated and will be removed in the future
            delegateClasses.put(VersionLastModifiedIdentifier.PRE_2_2_NAME, VersionLastModifiedIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(VersionLastModifiedIdentifier.NAME, VersionLastModifiedIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(RetiredIdentifier.NAME, RetiredIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(PrivateIdentifier.NAME, PrivateIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(LockTypeIdentifier.NAME, LockTypeIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(LockOwnerIdIdentifier.NAME, LockOwnerIdIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(LockOwnerLoginIdentifier.NAME, LockOwnerLoginIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(LockOwnerNameIdentifier.NAME, LockOwnerNameIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(LockDurationIdentifier.NAME, LockDurationIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(LockTimeAcquiredIdentifier.NAME, LockTimeAcquiredIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(OwnerIdIdentifier.NAME, OwnerIdIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(OwnerLoginIdentifier.NAME, OwnerLoginIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(OwnerNameIdentifier.NAME, OwnerNameIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(LastModifierIdIdentifier.NAME, LastModifierIdIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(LastModifierLoginIdentifier.NAME, LastModifierLoginIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(LastModifierNameIdentifier.NAME, LastModifierNameIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(LastModifiedIdentifier.NAME, LastModifiedIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(BranchIdIdentifier.NAME, BranchIdIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(BranchNameIdentifier.NAME, BranchNameIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(LanguageIdIdentifier.NAME, LanguageIdIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(LanguageNameIdentifier.NAME, LanguageNameIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(VariantLastModifiedIdentifier.NAME, VariantLastModifiedIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(VariantLastModifierIdIdentifier.NAME, VariantLastModifierIdIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(VariantLastModifierLoginIdentifier.NAME, VariantLastModifierLoginIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(VariantLastModifierNameIdentifier.NAME, VariantLastModifierNameIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(CollectionsIdentifier.NAME, CollectionsIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(CollectionsValueCountIdentifier.NAME, CollectionsValueCountIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(VariantsIdentifier.NAME, VariantsIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(VariantsValueCountIdentifier.NAME, VariantsValueCountIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(VersionIdIdentifier.NAME, VersionIdIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(ScoreIdentifier.NAME, ScoreIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(ReferenceLanguageIdIdentifier.NAME, ReferenceLanguageIdIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(ReferenceLanguageNameIdentifier.NAME, ReferenceLanguageNameIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(VersionChangeTypeIdentifier.NAME, VersionChangeTypeIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(VersionCommentIdentifier.NAME, VersionCommentIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(LastMajorChangeVersionIdIdentifier.NAME, LastMajorChangeVersionIdIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(LiveMajorChangeVersionIdIdentifier.NAME, LiveMajorChangeVersionIdIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(SyncedWithIdentifier.NAME, SyncedWithIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(SyncedWithVersionIdIdentifier.NAME, SyncedWithVersionIdIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(SyncedWithLanguageIdIdentifier.NAME, SyncedWithLanguageIdIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(SyncedWithLanguageNameIdentifier.NAME, SyncedWithLanguageNameIdentifier.class.getConstructor(Identifier.class));
            delegateClasses.put(ConceptualIdentifier.NAME, ConceptualIdentifier.class.getConstructor(Identifier.class));
        } catch (Exception e) {
            throw new RuntimeException("Error initializing delegate identifier map.", e);
        }
    }
    private static final QValueType[] valueTypeToOutputValueType;
    static {
        valueTypeToOutputValueType = new QValueType[9];
        valueTypeToOutputValueType[ValueType.STRING.getCode()] = QValueType.STRING;
        valueTypeToOutputValueType[ValueType.DATE.getCode()] = QValueType.DATE;
        valueTypeToOutputValueType[ValueType.DATETIME.getCode()] = QValueType.DATETIME;
        valueTypeToOutputValueType[ValueType.LONG.getCode()] = QValueType.LONG;
        valueTypeToOutputValueType[ValueType.DOUBLE.getCode()] = QValueType.DOUBLE;
        valueTypeToOutputValueType[ValueType.DECIMAL.getCode()] = QValueType.DECIMAL;
        valueTypeToOutputValueType[ValueType.BOOLEAN.getCode()] = QValueType.BOOLEAN;
        valueTypeToOutputValueType[ValueType.LINK.getCode()] = QValueType.LINK;
    }
    private static final long CONTENT_INCLUDE_LIMIT = 200000; // about 200k

    public Identifier(String id, String subId, ValueExpr multiValueIndexExpr, ValueExpr hierarchyIndexExpr) {
        this.id = id;
        this.subId = subId;
        this.multiValueIndexExpr = multiValueIndexExpr;
        this.hierarchyIndexExpr = hierarchyIndexExpr;
    }

    protected Identifier(String id, String subId, QueryContext context, DelegateIdentifier delegate) {
        this.id = id;
        this.subId = subId;
        this.multiValueIndexExpr = null;
        this.hierarchyIndexExpr = null;
        this.prepareOnlyQueryContext = context;
        this.delegate = delegate;
    }

    public ValueExpr clone() {
        Identifier clone = new Identifier(this.id, this.subId, this.multiValueIndexExpr, this.hierarchyIndexExpr);
        if (this.prepareOnlyQueryContext == null)
            throw new RuntimeException("Cannot clone non-prepared identifier");
        try {
            // easier to re-prepare then to implement cloning of the delegates too
            clone.prepare(this.prepareOnlyQueryContext);
        } catch (QueryException e) {
            throw new RuntimeException("Unexpected exception while cloning identifier", e);
        }
        return clone;
    }

    public void prepare(QueryContext context) throws QueryException {
        this.prepareOnlyQueryContext = context;

        if ((multiValueIndexExpr != null || hierarchyIndexExpr != null) && id.charAt(0) != '$')
            throw new QueryException("Index-expressions ([..]) can only be used with field identifiers, at " + getLocation());

        if (id.charAt(0) == '$') { // id's starting with $ represent fields
            String fieldName = id.substring(1);

            FieldType fieldType;
            try {
                fieldType = context.getFieldTypeByName(fieldName);
            } catch (RepositoryException e) {
                throw new QueryException("Error with identifier \"" + id + "\".", e);
            }

            if (multiValueIndexExpr != null) {
                if (!fieldType.isMultiValue())
                    throw new QueryException("A multivalue index ([..]) is specified for a non-multivalue field, at " + multiValueIndexExpr.getLocation());
                multiValueIndexExpr.prepare(context);
                if (!ValueExprUtil.isCompatPrimitiveValue(QValueType.LONG, multiValueIndexExpr)) {
                    throw new QueryException("Multivalue index should evaluate to a long value, at " + multiValueIndexExpr.getLocation());
                }
            }

            if (hierarchyIndexExpr != null) {
                if (!fieldType.isHierarchical())
                    throw new QueryException("A hierarchy path index ([..][..]) is specified for a non-hierarchical field, at " + hierarchyIndexExpr.getLocation());
                hierarchyIndexExpr.prepare(context);
                if (!ValueExprUtil.isCompatPrimitiveValue(QValueType.LONG, hierarchyIndexExpr)) {
                    throw new QueryException("Hierarchy path index should evaluate to a long value, at " + hierarchyIndexExpr.getLocation());
                }
            }

            if (subId == null) {
                this.delegate = new FieldIdentifier(fieldType);
            } else if (subId.equals(FieldValueCountIdentifier.NAME)) {
                this.delegate = new FieldValueCountIdentifier(fieldType);
            } else if (subId.equals(LinkFieldDocumentIdIdentifier.NAME)) {
                if (fieldType.getValueType() != ValueType.LINK)
                    throw new QueryException("Sub-field identifier " + LinkFieldDocumentIdIdentifier.NAME + " can only be used with link-type fields.");
                this.delegate = new LinkFieldDocumentIdIdentifier(fieldType);
            } else if (subId.equals(LinkFieldBranchIdIdentifier.NAME)) {
                if (fieldType.getValueType() != ValueType.LINK)
                    throw new QueryException("Sub-field identifier " + LinkFieldBranchIdIdentifier.NAME + " can only be used with link-type fields.");
                this.delegate = new LinkFieldBranchIdIdentifier(fieldType);
            } else if (subId.equals(LinkFieldLanguageIdIdentifier.NAME)) {
                if (fieldType.getValueType() != ValueType.LINK)
                    throw new QueryException("Sub-field identifier " + LinkFieldLanguageIdIdentifier.NAME + " can only be used with link-type fields.");
                this.delegate = new LinkFieldLanguageIdIdentifier(fieldType);
            } else if (subId.equals(LinkFieldBranchIdentifier.NAME)) {
                if (fieldType.getValueType() != ValueType.LINK)
                    throw new QueryException("Sub-field identifier " + LinkFieldBranchIdentifier.NAME + " can only be used with link-type fields.");
                this.delegate = new LinkFieldBranchIdentifier(fieldType);
            } else if (subId.equals(LinkFieldLanguageIdentifier.NAME)) {
                if (fieldType.getValueType() != ValueType.LINK)
                    throw new QueryException("Sub-field identifier " + LinkFieldLanguageIdentifier.NAME + " can only be used with link-type fields.");
                this.delegate = new LinkFieldLanguageIdentifier(fieldType);
            } else if (subId.equals(LinkFieldNamespaceIdentifier.NAME)) {
                if (fieldType.getValueType() != ValueType.LINK)
                    throw new QueryException("Sub-field identifier " + LinkFieldNamespaceIdentifier.NAME + " can only be used with link-type fields.");
                this.delegate = new LinkFieldNamespaceIdentifier(fieldType);
            } else {
                throw new QueryException("Invalid sub-field identifier: " + subId);
            }
        } else if (id.charAt(0) == '%') { // id's starting with % represent parts
            String partId = id.substring(1);
            if (subId == null)
                throw new QueryException("Identifier \"" + id + "\": missing sub-part identifier (ie a dot followed by what information of the part to use).");

            PartType partType;
            try {
                partType = context.getPartTypeByName(partId);
            } catch (RepositoryException e) {
                throw new QueryException("Error with identifier \"" + id + "\".", e);
            }

            if (subId.equals("content")) {
                this.delegate = new PartContentIdentifier(id, partType);
            } else if (subId.equals("mimeType")) {
                this.delegate = new PartMimeTypeIdentifier(id, partType);
            } else if (subId.equals("size")) {
                this.delegate = new PartSizeIdentifier(id, partType);
            } else {
                throw new QueryException("Identifier \"" + id + "\": invalid subpart id \"" + subId + "\".");
            }
        } else if (id.charAt(0) == '#') {
            String customFieldName = id.substring(1);
            this.delegate = new CustomFieldIdentifier(customFieldName);
        } else {
            String fullId = subId == null ? id : id + "." + subId;
            Constructor constructor = delegateClasses.get(fullId);
            if (constructor == null) {
                throw new QueryException("Unknown identifier: \"" + fullId + "\".");
            } else {
                try {
                    this.delegate = (DelegateIdentifier)constructor.newInstance(this);
                } catch (Exception e) {
                    throw new QueryException("Error instantiating identifier class.", e);
                }
            }
        }
    }

    public Object evaluate(QValueType valueType, ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        if (data == null)
            throw new ExprDocDataMissingException(getExpression(), getLocation());
        return delegate.evaluate(data, evaluationInfo);
    }

    public String getSqlPreConditions(SqlGenerationContext context) throws QueryException {
        return delegate.getSqlPreConditions(context);
    }

    public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        delegate.generateSqlValueExpr(sql, context);
    }

    public int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        return delegate.bindPreConditions(stmt, bindPos, evaluationInfo);
    }

    public int bindValueExpr(PreparedStatement stmt, int bindPos, QValueType valueType,
            EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        return delegate.bindValueExpr(stmt, bindPos, valueType);
    }

    public QValueType getValueType() {
        return delegate.getValueType();
    }

    public boolean isMultiValue() {
        return delegate.isMultiValue();
    }

    public boolean isHierarchical() {
        return delegate.isHierarchical();
    }

    public QValueType getOutputValueType() {
        return delegate.getOutputValueType();
    }

    public final Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        if (data == null)
            throw new ExprDocDataMissingException(getExpression(), getLocation());
        return delegate.getOutputValue(data, evaluationInfo);
    }

    public String getTitle(Locale locale) {
        return delegate.getTitle(locale);
    }

    public String getExpression() {
        StringBuilder expr = new StringBuilder();
        expr.append(id);
        if (multiValueIndexExpr != null) {
            expr.append("[").append(multiValueIndexExpr.getExpression()).append("]");
        }
        if (hierarchyIndexExpr != null) {
            if (multiValueIndexExpr == null)
                expr.append("[*]");
            expr.append("[").append(hierarchyIndexExpr.getExpression()).append("]");
        }
        if (subId != null)
            expr.append(".").append(subId);
        return expr.toString();
    }

    /**
     * May this identifier be used in ACL evaluation expressions?
     */
    public AclConditionViolation isAclAllowed() {
        return delegate.isAclAllowed();
    }

    /**
     * Must only be implemented by classes for which
     * isAclAllowed returns null.
     */
    public boolean canTestAppliesTo() {
        return delegate.canTestappliesTo();
    }

    public boolean isSymbolicIdentifier() {
        return delegate.isSymbolic();
    }

    public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
        return delegate.translateSymbolic(valueExpr, evaluationInfo);
    }

    /**
     * If true, then this identifier does not present a field on which can be searched,
     * and only the method getOutputValue should be called on it.
     */
    public boolean isOutputOnly() {
        return delegate.isOutputOnly();
    }

    public void collectAccessRestrictions(AccessRestrictions restrictions) {
        delegate.collectAccessRestrictions(restrictions);
    }

    public void doInContext(SqlGenerationContext context, ContextualizedRunnable runnable) throws QueryException {
        runnable.run(context);
    }

    public DelegateIdentifier getDelegate() {
        return delegate;
    }

    /**
     * If isMultiValue() returns true, this method returns the corresponding value count
     * identifier.
     */
    Identifier getValueCountIdentifier() {
        return delegate.getValueCountIdentifier();
    }

    interface DelegateIdentifier {
        void prepare(QueryContext context);
        QValueType getValueType();
        boolean isMultiValue();
        boolean isHierarchical();
        Identifier getValueCountIdentifier();
        Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException;
        AclConditionViolation isAclAllowed();
        String getSqlPreConditions(SqlGenerationContext context) throws QueryException;
        void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException;
        int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException;
        public int bindValueExpr(PreparedStatement stmt, int bindPos, QValueType valueType) throws SQLException, QueryException;
        boolean isSymbolic();
        Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException;
        QValueType getOutputValueType();
        Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException;
        boolean canTestappliesTo();
        boolean isOutputOnly();
        String getName();
        String getTitle(Locale locale);
        void collectAccessRestrictions(AccessRestrictions restrictions);
    }

    public abstract class AbstractIdentifier implements DelegateIdentifier {
        public void prepare(QueryContext context) {
        }

        public boolean isMultiValue() {
            return false;
        }

        public boolean isHierarchical() {
            return false;
        }

        public Identifier getValueCountIdentifier() {
            return null;
        }

        public AclConditionViolation isAclAllowed() {
            return null;
        }

        public String getTitle(Locale locale) {
            return getLocalizedString(getName(), locale);
        }

        public boolean isSymbolic() {
            return false;
        }

        public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
            throw new QueryException("translateSymbolic should not be called if isSymbolic returns false");
        }

        public boolean canTestappliesTo() {
            return false;
        }

        public String getSqlPreConditions(SqlGenerationContext context) throws QueryException {
            return null;
        }

        public int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
            return bindPos;
        }

        public int bindValueExpr(PreparedStatement stmt, int bindPos, QValueType valueType) throws SQLException, QueryException {
            return bindPos;
        }

        public boolean isOutputOnly() {
            return false;
        }

        public void collectAccessRestrictions(AccessRestrictions restrictions) {
            // do nothing
        }
    }

    public abstract class AbstractNonAclIdentifier extends AbstractIdentifier {
        public AclConditionViolation isAclAllowed() {
            return new AclConditionViolation("Identifier '" + getName() + "' is not allowed in ACL conditions.");
        }
    }

    public abstract class AbstractFieldIdentifier extends AbstractIdentifier {
        protected FieldType fieldType;

        protected AbstractFieldIdentifier(FieldType fieldType) {
            this.fieldType = fieldType;
        }

        public boolean isMultiValue() {
            return fieldType.isMultiValue() && multiValueIndexExpr == null;
        }

        public boolean isHierarchical() {
            return fieldType.isHierarchical() && hierarchyIndexExpr == null;
        }

        public String getSqlPreConditions(SqlGenerationContext context) throws QueryException {
            if (multiValueIndexExpr == null && hierarchyIndexExpr == null)
                return null;

            StringBuilder builder = new StringBuilder();

            builder.append(" (");
            if (multiValueIndexExpr != null) {
                String mvPreCond = multiValueIndexExpr.getSqlPreConditions(context);
                if (mvPreCond != null)
                    builder.append(mvPreCond);

                builder.append(getTableAlias()).append(".value_seq").append(" = ");
                multiValueIndexExpr.generateSqlValueExpr(builder, context);

                // support negative index counting from the end
                builder.append(" or ");
                builder.append(getTableAlias()).append(".value_seq").append(" = ");
                builder.append(getTableAlias()).append(".value_count").append(" + 1 + ");
                multiValueIndexExpr.generateSqlValueExpr(builder, context);
            }

            if (hierarchyIndexExpr != null) {
                if (multiValueIndexExpr != null)
                    builder.append(" and ");
                String hierPreCond = hierarchyIndexExpr.getSqlPreConditions(context);
                if (hierPreCond != null)
                    builder.append(hierPreCond);

                builder.append(getTableAlias()).append(".hier_seq").append(" = ");
                hierarchyIndexExpr.generateSqlValueExpr(builder, context);

                // support negative index counting from the end
                builder.append("or ");
                builder.append(getTableAlias()).append(".hier_seq").append(" = ");
                builder.append(getTableAlias()).append(".hier_count").append(" + 1 + ");
                hierarchyIndexExpr.generateSqlValueExpr(builder, context);
            }
            builder.append(") ");

            return builder.toString();
        }

        public int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
            if (multiValueIndexExpr != null) {
                bindPos = multiValueIndexExpr.bindPreConditions(stmt, bindPos, evaluationInfo);
                bindPos = multiValueIndexExpr.bindValueExpr(stmt, bindPos, QValueType.LONG, evaluationInfo);
                bindPos = multiValueIndexExpr.bindValueExpr(stmt, bindPos, QValueType.LONG, evaluationInfo);
            }

            if (hierarchyIndexExpr != null) {
                bindPos = hierarchyIndexExpr.bindPreConditions(stmt, bindPos, evaluationInfo);
                bindPos = hierarchyIndexExpr.bindValueExpr(stmt, bindPos, QValueType.LONG, evaluationInfo);
                bindPos = hierarchyIndexExpr.bindValueExpr(stmt, bindPos, QValueType.LONG, evaluationInfo);
            }

            return bindPos;
        }

        public abstract String getTableAlias();


        public Object processValue(Object object, ExprDocData data, EvaluationInfo evaluationInfo, ValueExtractor valueExtractor) throws QueryException {
            if (object instanceof Object[]) {
                return processMultiValue((Object[])object, data, evaluationInfo, valueExtractor);
            } else if (object instanceof HierarchyPath) {
                return processHierarchyPath((HierarchyPath)object, data, evaluationInfo, valueExtractor);
            } else {
                return valueExtractor.extract(object);
            }
        }

        private Object processMultiValue(Object[] values, ExprDocData data, EvaluationInfo evaluationInfo, ValueExtractor valueExtractor) throws QueryException {
            if (multiValueIndexExpr != null) {
                Number number = (Number)multiValueIndexExpr.evaluate(QValueType.LONG, data, evaluationInfo);
                if (number == null)
                    return null;
                int index = number.intValue();
                index = index < 0 ? values.length + index : index - 1;
                if (index >= 0 && index < values.length)
                    return processValue(values[index], data, evaluationInfo, valueExtractor);
                return null;
            } else {
                List<Object> result = new ArrayList<Object>(values.length);
                for (Object value : values) {
                    value = processValue(value, data, evaluationInfo, valueExtractor);
                    if (value != null)
                        result.add(value);
                }
                return result.isEmpty() ? null : result.toArray();
            }
        }

        private Object processHierarchyPath(HierarchyPath path, ExprDocData data, EvaluationInfo evaluationInfo, ValueExtractor valueExtractor) throws QueryException {
            Object[] elements = path.getElements();
            if (hierarchyIndexExpr != null) {
                Number number = (Number)hierarchyIndexExpr.evaluate(QValueType.LONG, data, evaluationInfo);
                if (number == null)
                    return null;
                int index = number.intValue();
                index = index < 0 ? elements.length + index : index - 1;
                if (index >= 0 && index < elements.length)
                    return valueExtractor.extract(elements[index]);
                return null;
            } else {
                Object[] newElements = new Object[elements.length];
                for (int i = 0; i < elements.length; i++) {
                    newElements[i] = valueExtractor.extract(elements[i]);
                }
                return new HierarchyPath(newElements);
            }
        }

        public void collectAccessRestrictions(AccessRestrictions resrestrictions) {
            resrestrictions.addFieldReference(fieldType.getName());
        }
    }

    private static interface ValueExtractor {
        public Object extract(Object object);
    }

    public final class FieldIdentifier extends AbstractFieldIdentifier {
        private String alias;
        private QValueType valueType;

        public FieldIdentifier(FieldType fieldType) {
            super(fieldType);
        }

        public String getName() {
            return "$" + fieldType.getName();
        }

        public long getfieldTypeId() {
            return fieldType.getId();
        }

        public QValueType getValueType() {
            if (valueType == null) {
                ValueType fieldValueType = fieldType.getValueType();
                if (fieldValueType == ValueType.STRING)
                    valueType = QValueType.STRING;
                else if (fieldValueType == ValueType.DATE)
                    valueType = QValueType.DATE;
                else if (fieldValueType == ValueType.DATETIME)
                    valueType = QValueType.DATETIME;
                else if (fieldValueType == ValueType.LONG)
                    valueType = QValueType.LONG;
                else if (fieldValueType == ValueType.DOUBLE)
                    valueType = QValueType.DOUBLE;
                else if (fieldValueType == ValueType.DECIMAL)
                    valueType = QValueType.DECIMAL;
                else if (fieldValueType == ValueType.BOOLEAN)
                    valueType = QValueType.BOOLEAN;
                else if (fieldValueType == ValueType.LINK)
                    valueType = QValueType.LINK;
                else
                    throw new RuntimeException("Unrecognized field value type: " + fieldValueType);
            }
            return valueType;
        }

        public Identifier getValueCountIdentifier() {
            return new Identifier(getName(), "valueCount", prepareOnlyQueryContext, new FieldValueCountIdentifier(fieldType));
        }

        public Object evaluate(final ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
            if (data.versionedData.hasField(fieldType.getId())) {
                Object value = data.versionedData.getField(fieldType.getId()).getValue();
                return super.processValue(value, data, evaluationInfo, new ValueExtractor() {
                    public Object extract(Object object) {
                        if (fieldType.getValueType() == ValueType.LINK) {
                            return absolutizeVariantKey((VariantKey)object, data.document);
                        } else {
                            return object;
                        }
                    }
                });
            } else {
                return null;
            }
        }

        private VariantKey absolutizeVariantKey(VariantKey key, Document document) {
            if (key.getBranchId() == -1 || key.getLanguageId() == -1)
                return new VariantKey(key.getDocumentId(),
                        key.getBranchId() == -1 ? document.getBranchId() : key.getBranchId(),
                        key.getLanguageId() == -1 ? document.getLanguageId() : key.getLanguageId());
            else
                return key;
        }

        public QValueType getOutputValueType() {
            return valueTypeToOutputValueType[fieldType.getValueType().getCode()];
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
            return evaluate(data, evaluationInfo);
        }

        public AclConditionViolation isAclAllowed() {
            if (!fieldType.isAclAllowed()) {
                return new AclConditionViolation("Field \"" + fieldType.getName() + "\" (ID: " + fieldType.getId() + ") may not be used in ACL object expressions.");
            } else {
                return null;
            }
        }

        public boolean canTestappliesTo() {
            return false;
        }

        public String getSqlPreConditions(SqlGenerationContext context) throws QueryException {
            // the alias/join is [also] created here for some special conditions (like IsNull) that never make use of the value expr
            alias = context.getNewFieldsTable(fieldType.getId()).getName();
            return super.getSqlPreConditions(context);
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            String valueColumn = SqlGenerationContext.FieldsTable.getValueColumn(getValueType());
            sql.append(alias).append(".").append(valueColumn);
        }

        /**
         * For exceptional uses only.
         */
        public String getTableAlias() {
            return alias;
        }

        public String getTitle(Locale locale) {
            return fieldType.getLabel(locale);
        }
    }

    public final class FieldValueCountIdentifier extends AbstractNonAclIdentifier {
        static final String NAME = "valueCount";
        private final FieldType fieldType;
        private String alias;

        public FieldValueCountIdentifier(FieldType fieldType) {
            this.fieldType = fieldType;
        }

        public String getTitle(Locale locale) {
            String fieldLabel = fieldType.getLabel(locale);
            String valueCount = getLocalizedString("fieldValueCount", locale);
            return fieldLabel + ": " + valueCount;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            if (data.versionedData.hasField(fieldType.getId())) {
                if (fieldType.isMultiValue()) {
                    return new Long(((Object[])data.versionedData.getField(fieldType.getId()).getValue()).length);
                } else {
                    return new Long(1);
                }
            } else {
                return new Long(0);
            }
        }

        public String getSqlPreConditions(SqlGenerationContext context) {
            // the alias/join is created here for some special conditions (like IsNull) that never make use of the value expr
            alias = context.getNewFieldsTable(fieldType.getId()).getName();
            return null;
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(alias).append('.').append("value_count");
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }

        public String getName() {
            return "$" + fieldType.getName() + "." + NAME;
        }

        public int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) {
            return bindPos;
        }
    }

    public final class LinkFieldDocumentIdIdentifier extends AbstractFieldIdentifier {
        public static final String NAME = "documentId";
        private String alias;

        public LinkFieldDocumentIdIdentifier(FieldType fieldType) {
            super(fieldType);
        }

        public String getName() {
            return "$" + fieldType.getName() + "." + NAME;
        }

        public QValueType getValueType() {
            return QValueType.DOCID;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
            return evaluate(data, false, evaluationInfo);
        }

        public boolean isSymbolic() {
            return true;
        }

        public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
            String documentId = (String)valueExpr.evaluate(QValueType.STRING, null, evaluationInfo);
            return SqlUtils.parseDocId(documentId, evaluationInfo.getQueryContext());
        }

        public Object evaluate(ExprDocData data, final boolean returnStrings, final EvaluationInfo evaluationInfo) throws QueryException {
            if (data.versionedData.hasField(fieldType.getId())) {
                Object value = data.versionedData.getField(fieldType.getId()).getValue();
                return super.processValue(value, data, evaluationInfo, new ValueExtractor() {
                    public Object extract(Object object) {
                        if (returnStrings) {
                            return ((VariantKey)object).getDocumentId();
                        } else {
                            return evaluationInfo.getQueryContext().parseDocId(((VariantKey)object).getDocumentId());
                        }
                    }
                });
            } else {
                return null;
            }
        }

        public String getSqlPreConditions(SqlGenerationContext context) throws QueryException {
            // the alias/join is [also] created here for some special conditions (like IsNull) that never make use of the value expr
            alias = context.getNewFieldsTable(fieldType.getId()).getName();
            return super.getSqlPreConditions(context);
        }

        public int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
            return super.bindPreConditions(stmt, bindPos, evaluationInfo);
        }

        public String getTableAlias() {
            return alias;
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(alias).append('.').append(SqlGenerationContext.FieldsTable.LINK_SEARCHDOCID);
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
            return evaluate(data, true, evaluationInfo);
        }

        public String getTitle(Locale locale) {
            String fieldLabel = fieldType.getLabel(locale);
            String branchId = getLocalizedString("id", locale);
            return fieldLabel + ": " + branchId;
        }
    }

    public final class LinkFieldNamespaceIdentifier extends AbstractFieldIdentifier {
        public static final String NAME = "namespace";
        private String alias;

        public LinkFieldNamespaceIdentifier(FieldType fieldType) {
            super(fieldType);
        }

        public String getName() {
            return "$" + fieldType.getName() + "." + NAME;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
            return evaluate(data, false, evaluationInfo);
        }

        public Object evaluate(ExprDocData data, final boolean returnStrings, final EvaluationInfo evaluationInfo) throws QueryException {
            if (data.versionedData.hasField(fieldType.getId())) {
                Object value = data.versionedData.getField(fieldType.getId()).getValue();
                return super.processValue(value, data, evaluationInfo, new ValueExtractor() {
                    public Object extract(Object object) {
                        if (returnStrings) {
                            return getNamespace(((VariantKey)object).getDocumentId());
                        } else {
                            return new Long(getNamespaceId(((VariantKey)object).getDocumentId(), evaluationInfo.getQueryContext()));
                        }
                    }
                });
            } else {
                return null;
            }
        }

        private String getNamespace(String documentId) {
            int dashPos = documentId.indexOf('-');
            if (dashPos == -1)
                throw new RepositoryRuntimeException("Unexpected error: no dash in document ID " + documentId + " in link field namespace identifier.");
            return documentId.substring(dashPos + 1);
        }

        private long getNamespaceId(String documentId, QueryContext queryContext) {
            int dashPos = documentId.indexOf('-');
            if (dashPos == -1)
                throw new RepositoryRuntimeException("Unexpected error: no dash in document ID " + documentId + " in link field namespace identifier.");
            String ns = documentId.substring(dashPos + 1);
            try {
                return queryContext.getNamespace(ns).getId();
            } catch (NamespaceNotFoundException e) {
                throw new RepositoryRuntimeException("Unexpected error: namespace in document ID " + documentId + " selected in link field namespace identifier for " + fieldType.getName() + " does not exist.");
            }
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
            return evaluate(data, true, evaluationInfo);
        }

        public boolean isSymbolic() {
            return true;
        }

        public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
            String namespaceName = (String)valueExpr.evaluate(QValueType.STRING, null, evaluationInfo);
            Namespace namespace;
            try {
                namespace = evaluationInfo.getQueryContext().getNamespace(namespaceName);
            } catch (NamespaceNotFoundException e) {
                // it is inconvenient if searching on a non-registered namespace would fail
                // there return here an ID that does not exist in the database.
                return new Long(-1);
            } catch (RepositoryException e) {
                throw new QueryException("Error consulting repository namespace information.", e);
            }
            return new Long(namespace.getId());
        }

        public String getSqlPreConditions(SqlGenerationContext context) throws QueryException {
            // the alias/join is created here for some special conditions (like IsNull) that never make use of the value expr
            alias = context.getNewFieldsTable(fieldType.getId()).getName();
            return super.getSqlPreConditions(context);
        }

        public int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
            return super.bindPreConditions(stmt, bindPos, evaluationInfo);
        }

        public String getTableAlias() {
            return alias;
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(alias).append('.').append(SqlGenerationContext.FieldsTable.LINK_NSID);
        }

        public String getTitle(Locale locale) {
            String fieldLabel = fieldType.getLabel(locale);
            String namespace = getLocalizedString("namespace", locale);
            return fieldLabel + ": " + namespace;
        }

    }

    public class LinkFieldBranchIdIdentifier extends AbstractFieldIdentifier {
        public static final String NAME = "branchId";
        private String alias;

        public LinkFieldBranchIdIdentifier(FieldType fieldType) {
            super(fieldType);
        }

        public String getName() {
            return "$" + fieldType.getName() + "." + NAME;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
            if (data.versionedData.hasField(fieldType.getId())) {
                final long documentBranchId = data.document.getBranchId();
                Object value = data.versionedData.getField(fieldType.getId()).getValue();
                return super.processValue(value, data, evaluationInfo, new ValueExtractor() {
                    public Object extract(Object object) {
                        VariantKey key = (VariantKey)object;
                        return new Long(key.getBranchId() != -1 ? key.getBranchId() : documentBranchId);
                    }
                });
            } else {
                return null;
            }
        }

        public String getSqlPreConditions(SqlGenerationContext context) throws QueryException {
            alias = context.getNewFieldsTable(fieldType.getId()).getName();
            return super.getSqlPreConditions(context);
        }

        public int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
            return super.bindPreConditions(stmt, bindPos, evaluationInfo);
        }

        public String getTableAlias() {
            return alias;
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(alias).append('.').append(SqlGenerationContext.FieldsTable.LINK_SEARCHBRANCHID);
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
            return evaluate(data, evaluationInfo);
        }

        public String getTitle(Locale locale) {
            String fieldLabel = fieldType.getLabel(locale);
            String branchId = getLocalizedString("branchId", locale);
            return fieldLabel + ": " + branchId;
        }
    }

    public class LinkFieldLanguageIdIdentifier extends AbstractFieldIdentifier {
        public static final String NAME = "languageId";
        private String alias;

        public LinkFieldLanguageIdIdentifier(FieldType fieldType) {
            super(fieldType);
        }

        public String getName() {
            return "$" + fieldType.getName() + "." + NAME;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
            final long documentLanguageId = data.document.getLanguageId();
            if (data.versionedData.hasField(fieldType.getId())) {
                Object value = data.versionedData.getField(fieldType.getId()).getValue();
                return super.processValue(value, data, evaluationInfo, new ValueExtractor() {
                    public Object extract(Object object) {
                        VariantKey key = (VariantKey)object;
                        return new Long(key.getLanguageId() != -1 ? key.getLanguageId() : documentLanguageId);
                    }
                });
            } else {
                return null;
            }
        }

        public String getSqlPreConditions(SqlGenerationContext context) throws QueryException {
            // the alias/join is created here for some special conditions (like IsNull) that never make use of the value expr
            alias = context.getNewFieldsTable(fieldType.getId()).getName();
            return super.getSqlPreConditions(context);
        }

        public int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
            return super.bindPreConditions(stmt, bindPos, evaluationInfo);
        }

        public String getTableAlias() {
            return alias;
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(alias).append('.').append(SqlGenerationContext.FieldsTable.LINK_SEARCHLANGID);
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
            return evaluate(data, evaluationInfo);
        }

        public String getTitle(Locale locale) {
            String fieldLabel = fieldType.getLabel(locale);
            String languageId = getLocalizedString("languageId", locale);
            return fieldLabel + ": " + languageId;
        }
    }

    public final class LinkFieldBranchIdentifier extends LinkFieldBranchIdIdentifier {
        public static final String NAME = "branch";

        public LinkFieldBranchIdentifier(FieldType fieldType) {
            super(fieldType);
        }

        public String getName() {
            return "$" + fieldType.getName() + "." + NAME;
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, final EvaluationInfo evaluationInfo) throws QueryException {
            if (data.versionedData.hasField(fieldType.getId())) {
                final long documentBranchId = data.document.getBranchId();
                Object value = data.versionedData.getField(fieldType.getId()).getValue();
                return super.processValue(value, data, evaluationInfo, new ValueExtractor() {
                    public Object extract(Object object) {
                        VariantKey key = (VariantKey)object;
                        long branchId = key.getBranchId() != -1 ? key.getBranchId() : documentBranchId;
                        try {
                            return evaluationInfo.getQueryContext().getBranch(branchId).getName();
                        } catch (RepositoryException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } else {
                return null;
            }
        }

        public boolean isSymbolic() {
            return true;
        }

        public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
            String branchName = (String)valueExpr.evaluate(QValueType.STRING, null, evaluationInfo);
            Branch branch;
            try {
                branch = evaluationInfo.getQueryContext().getBranchByName(branchName);
            } catch (RepositoryException e) {
                throw new QueryException("Error with branch name \"" + branchName + "\".", e);
            }
            return new Long(branch.getId());
        }

        public String getTitle(Locale locale) {
            String fieldLabel = fieldType.getLabel(locale);
            String branch = getLocalizedString("branch", locale);
            return fieldLabel + ": " + branch;
        }
    }

    public final class LinkFieldLanguageIdentifier extends LinkFieldLanguageIdIdentifier {
        public static final String NAME = "language";

        public LinkFieldLanguageIdentifier(FieldType fieldType) {
            super(fieldType);
        }

        public String getName() {
            return "$" + fieldType.getName() + "." + NAME;
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, final EvaluationInfo evaluationInfo) throws QueryException {
            final long documentLanguageId = data.document.getLanguageId();
            if (data.versionedData.hasField(fieldType.getId())) {
                Object value = data.versionedData.getField(fieldType.getId()).getValue();
                return super.processValue(value, data, evaluationInfo, new ValueExtractor() {
                    public Object extract(Object object) {
                        VariantKey key = (VariantKey)object;
                        long languageId = key.getLanguageId() != -1 ? key.getLanguageId() : documentLanguageId;
                        try {
                            return evaluationInfo.getQueryContext().getLanguage(languageId).getName();
                        } catch (RepositoryException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } else {
                return null;
            }
        }

        public boolean isSymbolic() {
            return true;
        }

        public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
            String languageName = (String)valueExpr.evaluate(QValueType.STRING, null, evaluationInfo);
            Language language;
            try {
                language = evaluationInfo.getQueryContext().getLanguageByName(languageName);
            } catch (RepositoryException e) {
                throw new QueryException("Error with language name \"" + languageName + "\".", e);
            }
            return new Long(language.getId());
        }

        public String getTitle(Locale locale) {
            String fieldLabel = fieldType.getLabel(locale);
            String language = getLocalizedString("language", locale);
            return fieldLabel + ": " + language;
        }
    }

    /**
     * Exposes the collections a document belongs to as a multi-value identifier.
     */
    public final class CollectionsIdentifier extends AbstractIdentifier {
        public static final String NAME = "collections";

        public CollectionsIdentifier() {
        }

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public boolean isMultiValue() {
            return true;
        }

        public Identifier getValueCountIdentifier() {
            return new Identifier(CollectionsValueCountIdentifier.NAME, null, prepareOnlyQueryContext, new CollectionsValueCountIdentifier());
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            DocumentCollection[] collections = data.document.getCollections().getArray();
            if (collections.length == 0)
                return null;
            Long[] collectionIds = new Long[collections.length];
            for (int i = 0; i < collectionIds.length; i++)
                collectionIds[i] = new Long(collections[i].getId());
            return collectionIds;
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            DocumentCollection[] collections = data.document.getCollections().getArray();
            if (collections.length == 0)
                return null;
            String[] collectionNames = new String[collections.length];
            for (int i = 0; i < collectionNames.length; i++)
                collectionNames[i] = collections[i].getName();
            return collectionNames;
        }

        public boolean canTestappliesTo() {
            return true;
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            String alias = context.getNewCollectionsTable().getName();
            sql.append(alias).append(".collection_id");
        }

        public boolean isSymbolic() {
            return true;
        }

        public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
            String collectionName = (String)valueExpr.evaluate(QValueType.STRING, null, evaluationInfo);
            DocumentCollection collection;
            try {
                collection = evaluationInfo.getQueryContext().getCollection(collectionName);
            } catch (CollectionNotFoundException e) {
                throw new QueryException("\"" + collectionName + "\" is not an existing collection.");
            } catch (RepositoryException e) {
                throw new QueryException("Error consulting collection information.", e);
            }
            return new Long(collection.getId());
        }
    }

    private static final ParamString COLL_VALUE_COUNT_EXPR = new ParamString(" (select count(*) from document_collections where document_id = {document_variants}.doc_id and branch_id = {document_variants}.branch_id and lang_id = {document_variants}.lang_id) ");

    public final class CollectionsValueCountIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "collections.valueCount";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return new Long(data.document.getCollections().getArray().length);
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            Map<String, String> params = new HashMap<String, String>();
            params.put("document_variants", context.getDocumentVariantsTable().getName());
            sql.append(COLL_VALUE_COUNT_EXPR.toString(params));
        }

        public boolean isSymbolic() {
            return false;
        }

        public boolean isOutputOnly() {
            return false;
        }

        public String getTitle(Locale locale) {
            return getLocalizedString(getName(), locale);
        }
    }

    private static final Pattern VARIANT_SEARCH_PATTERN = Pattern.compile("^([^:]*):([^:]*)$");
    private static final ParamString VARIANT_SEARCH_JOIN_EXPR = new ParamString(" left join document_variants {docVariantsAlias} on ({document_variants}.doc_id = {docVariantsAlias}.doc_id and {document_variants}.ns_id = {docVariantsAlias}.ns_id) ");

    /**
     * Exposes the variants of a document as a multi-value identifier.
     */
    public final class VariantsIdentifier extends AbstractIdentifier {
        public static final String NAME = "variants";

        public VariantsIdentifier() {
        }

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.STRING;
        }

        public boolean isMultiValue() {
            return true;
        }

        public Identifier getValueCountIdentifier() {
            return new Identifier(VariantsValueCountIdentifier.NAME, null, prepareOnlyQueryContext, new VariantsValueCountIdentifier());
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            AvailableVariant[] variants;
            try {
                variants = data.document.getAvailableVariants().getArray();
            } catch (RepositoryException e) {
                return null;
            }

            if (variants.length == 0) // can't normally occur
                return null;

            String[] keys = new String[variants.length];
            for (int i = 0; i < variants.length; i++)
                keys[i] = variants[i].getBranchId() + ":" + variants[i].getLanguageId();
            return keys;
        }

        public QValueType getOutputValueType() {
            return QValueType.LINK;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            AvailableVariant[] variants;
            try {
                variants = data.document.getAvailableVariants().getArray();
            } catch (RepositoryException e) {
                return null;
            }

            if (variants.length == 0) // can't normally occur
                return null;

            VariantKey[] keys = new VariantKey[variants.length];
            String documentId = data.document.getId();
            for (int i = 0; i < variants.length; i++)
                keys[i] = new VariantKey(documentId, variants[i].getBranchId(), variants[i].getLanguageId());
            return keys;
        }

        public boolean canTestappliesTo() {
            return true;
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            final String alias = "variantsearch" + context.getNewAliasCounter();
            final String currentDocVariantsAlias = context.getDocumentVariantsTable().getName();
            context.needsJoinWithTable(new SqlGenerationContext.Table() {

                public String getName() {
                    return null;
                }

                public String getJoinExpression() throws QueryException {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("docVariantsAlias", alias);
                    params.put("document_variants", currentDocVariantsAlias);
                    return VARIANT_SEARCH_JOIN_EXPR.toString(params);
                }

                public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
                    return bindPos;
                }
            });
            sql.append(alias).append(".variant_search");
        }

        public boolean isSymbolic() {
            return true;
        }

        public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
            String variant = (String)valueExpr.evaluate(QValueType.STRING, null, evaluationInfo);
            Matcher matcher = VARIANT_SEARCH_PATTERN.matcher(variant);
            if (matcher.matches()) {
                String branch = matcher.group(1);
                String language = matcher.group(2);
                long branchId;
                long languageId;
                try {
                    branchId = evaluationInfo.getQueryContext().getBranch(branch).getId();
                    languageId = evaluationInfo.getQueryContext().getLanguage(language).getId();
                } catch (BranchNotFoundException e) {
                    throw new QueryException("Branch specified in \"" + variant + "\" does not exist.");
                } catch (LanguageNotFoundException e) {
                    throw new QueryException("Language specified in \"" + variant + "\" does not exist.");
                } catch (RepositoryException e) {
                    throw new QueryException("Problem parsing branch:language specification \"" + variant + "\".");
                }
                return branchId + ":" + languageId;
            } else {
                throw new QueryException("Invalid branch:language specification: \"" + variant + "\".");
            }
        }
    }

    private static final ParamString VARIANTS_VALUE_COUNT_EXPR = new ParamString(" (select count(*) from document_variants vvc where vvc.doc_id = {document_variants}.doc_id and vvc.ns_id = {document_variants}.ns_id) ");

    public final class VariantsValueCountIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "variants.valueCount";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            try {
                return new Long(data.document.getAvailableVariants().getArray().length);
            } catch (RepositoryException e) {
                // either this or throwing an exception
                return new Long(-1);
            }
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            Map<String, String> params = new HashMap<String, String>();
            params.put("document_variants", context.getDocumentVariantsTable().getName());
            sql.append(VARIANTS_VALUE_COUNT_EXPR.toString(params));
        }

        public boolean isSymbolic() {
            return false;
        }

        public boolean isOutputOnly() {
            return false;
        }

        public String getTitle(Locale locale) {
            return getLocalizedString(getName(), locale);
        }
    }

    private String getUserDisplayName(long userId, QueryContext queryContext) {
        try {
            return queryContext.getUserDisplayName(userId);
        } catch (RepositoryException e) {
            throw new RuntimeException("Error getting display name for user " + userId, e);
        }
    }

    private String getUserLogin(long userId, QueryContext queryContext) {
        try {
            return queryContext.getUserLogin(userId);
        } catch (RepositoryException e) {
            throw new RuntimeException("Error getting login for user " + userId, e);
        }
    }

    private static String getLocalizedString(String name, Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("org/outerj/daisy/query/model/messages", locale);
        return bundle.getString(name);
    }

    public final class DocumentTypeIdentifier extends AbstractIdentifier {
        public static final String NAME = "documentType";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return new Long(data.document.getDocumentTypeId());
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            try {
                return evaluationInfo.getQueryContext().getDocumentTypeById(data.document.getDocumentTypeId()).getName();
            } catch (RepositoryException e) {
                return "<ERROR>";
            }
        }

        public boolean canTestappliesTo() {
            return true;
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentVariantsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentVariantsTable.DOCTYPE_ID);
        }

        public boolean isSymbolic() {
            return true;
        }

        public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
            String documentTypeName = (String)valueExpr.evaluate(QValueType.STRING, null, evaluationInfo);
            DocumentType documentType;
            try {
                documentType = evaluationInfo.getQueryContext().getDocumentTypeByName(documentTypeName);
            } catch (DocumentTypeNotFoundException e) {
                throw new QueryException("\"" + documentTypeName + "\" is not a valid document type name.");
            } catch (RepositoryException e) {
                throw new QueryException("Error consulting repository schema information.", e);
            }
            return new Long(documentType.getId());
        }
    }

    public final class DocumentNameIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "name";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.STRING;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return data.versionedData.getDocumentName();
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getVersionsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.VersionsTable.NAME);
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }
    }

    public final class CreationTimeIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "creationTime";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.DATETIME;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return data.document.getCreated();
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentsTable.CREATED);
        }

        public QValueType getOutputValueType() {
            return QValueType.DATETIME;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }
    }

    public final class DocumentIdIdentifier extends AbstractIdentifier {
        public static final String NAME = "id";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.DOCID;
        }

        public boolean isSymbolic() {
            return true;
        }

        public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
            String documentId = (String)valueExpr.evaluate(QValueType.STRING, null, evaluationInfo);
            return SqlUtils.parseDocId(documentId, evaluationInfo.getQueryContext());
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            String id = data.document.getId();
            return id == null ? null : evaluationInfo.getQueryContext().parseDocId(id);
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            // The document ID as known to the external world is internally stored
            // in two database columns: the numeric ID and the namespace ID. This mismatch
            // leads to some difficulties for searching, especially considering that this
            // not only needs to work for the '=' operator but also for others such as 'IN'.
            // Therefore a special id_search column is maintained that allows to treat
            // the document ID again as one atomic value.
            // (Note: this same technique is used for searching on link type fields where
            // the doc id, ns id, branch and lang are stored in multiple columns)
            // The format of the ID_SEARCH column is docSeqId + '-' + internal namespace ID (not name) 
            sql.append(context.getDocumentsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentsTable.ID_SEARCH);
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return data.document.getId();
        }
    }

    public final class DocumentNamespaceIdentifier extends AbstractIdentifier {
        public static final String NAME = "namespace";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public boolean isSymbolic() {
            return true;
        }

        public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
            String namespaceName = (String)valueExpr.evaluate(QValueType.STRING, null, evaluationInfo);
            Namespace namespace;
            try {
                namespace = evaluationInfo.getQueryContext().getNamespace(namespaceName);
            } catch (NamespaceNotFoundException e) {
                // it is inconvenient if searching on a non-registered namespace would fail
                // there return here an ID that does not exist in the database.
                return new Long(-1);
            } catch (RepositoryException e) {
                throw new QueryException("Error consulting repository namespace information.", e);
            }
            return new Long(namespace.getId());
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            String namespaceName = data.document.getNamespace();
            if (namespaceName == null) // namespace can be null on not-yet saved documents
                return null;

            try {
                Namespace namespace = evaluationInfo.getQueryContext().getNamespace(namespaceName);
                return new Long(namespace.getId());
            } catch (RepositoryException e) {
                throw new RepositoryRuntimeException("Error consulting repository namespace information.", e);
            }
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentVariantsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentsTable.NS_ID);
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return data.document.getNamespace();
        }
    }

    public final class DocumentLinkIdentifier extends AbstractIdentifier {
        public static final String NAME = "link";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.LINK;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return data.document.getVariantKey();
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentVariantsTable().getName());
            sql.append(".");
            sql.append(SqlGenerationContext.DocumentVariantsTable.LINK_SEARCH);
        }

        public QValueType getOutputValueType() {
            return QValueType.LINK;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }
    }

    public final class BranchIdIdentifier extends AbstractIdentifier {
        public static final String NAME = "branchId";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return new Long(data.document.getBranchId());
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentVariantsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentVariantsTable.BRANCH_ID);
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }

        @Override
        public boolean canTestappliesTo() {
            return true;
        }
    }

    public final class BranchNameIdentifier extends AbstractIdentifier {
        public static final String NAME = "branch";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return new Long(data.document.getBranchId());
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentVariantsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentVariantsTable.BRANCH_ID);
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            try {
                return evaluationInfo.getQueryContext().getBranch(data.document.getBranchId()).getName();
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }

        public boolean isSymbolic() {
            return true;
        }

        public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
            String branchName = (String)valueExpr.evaluate(QValueType.STRING, null, evaluationInfo);
            Branch branch;
            try {
                branch = evaluationInfo.getQueryContext().getBranchByName(branchName);
            } catch (RepositoryException e) {
                throw new QueryException("Error with branch name \"" + branchName + "\".", e);
            }
            return new Long(branch.getId());
        }

        @Override
        public boolean canTestappliesTo() {
            return true;
        }
    }

    public final class LanguageIdIdentifier extends AbstractIdentifier {
        public static final String NAME = "languageId";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return new Long(data.document.getLanguageId());
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentVariantsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentVariantsTable.LANG_ID);
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return new Long(data.document.getLanguageId());
        }

        @Override
        public boolean canTestappliesTo() {
            return true;
        }
    }

    public final class LanguageNameIdentifier extends AbstractIdentifier {
        public static final String NAME = "language";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return new Long(data.document.getLanguageId());
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentVariantsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentVariantsTable.LANG_ID);
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            try {
                return evaluationInfo.getQueryContext().getLanguage(data.document.getLanguageId()).getName();
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }

        public boolean isSymbolic() {
            return true;
        }

        public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
            String languageName = (String)valueExpr.evaluate(QValueType.STRING, null, evaluationInfo);
            Language language;
            try {
                language = evaluationInfo.getQueryContext().getLanguageByName(languageName);
            } catch (RepositoryException e) {
                throw new QueryException("Error with language name \"" + languageName + "\".", e);
            }
            return new Long(language.getId());
        }

        @Override
        public boolean canTestappliesTo() {
            return true;
        }
    }

    public final class VersionCreationTimeIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "versionCreationTime";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.DATETIME;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            if (data.version != null) {
                return data.version.getCreated();
            } else {
                return null;
            }
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getVersionsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.VersionsTable.CREATED_ON);
        }

        public QValueType getOutputValueType() {
            return QValueType.DATETIME;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }
    }

    public final class TotalSizeOfPartsIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "totalSizeOfParts";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            if (data.version != null) {
                return new Long(data.version.getTotalSizeOfParts());
            } else {
                return null;
            }
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getVersionsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.VersionsTable.TOTAL_SIZE_OF_PARTS);
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }
    }

    public final class VersionLastModifiedIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "versionLastModified";
        public static final String PRE_2_2_NAME = "versionStateLastModified";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.DATETIME;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            if (data.version != null) {
                return data.version.getLastModified();
            } else {
                return null;
            }
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getVersionsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.VersionsTable.LAST_MODIFIED);
        }

        public QValueType getOutputValueType() {
            return QValueType.DATETIME;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }
    }

    public final class VersionCreatorIdIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "versionCreatorId";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            if (data.version != null) {
                return new Long(data.version.getCreator());
            } else {
                return null;
            }
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getVersionsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.VersionsTable.CREATED_BY);
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }
    }

    public abstract class AbstractLoginIdentifier extends AbstractNonAclIdentifier {
        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public boolean isSymbolic() {
            return true;
        }

        public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
            String login = (String)valueExpr.evaluate(QValueType.STRING, null, evaluationInfo);
            long userId;
            try {
                userId = evaluationInfo.getQueryContext().getUserId(login);
            } catch (RepositoryException e) {
                throw new QueryException("Error getting user information for user with login \"" + login + "\".", e);
            }
            return new Long(userId);
        }
    }

    public final class VersionCreatorLoginIdentifier extends AbstractLoginIdentifier {
        public static final String NAME = "versionCreatorLogin";

        public String getName() {
            return NAME;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            if (data.version != null) {
                return new Long(data.version.getCreator());
            } else {
                return null;
            }
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getVersionsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.VersionsTable.CREATED_BY);
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            Long creatorId = (Long)evaluate(data, null);
            return creatorId != null ? getUserLogin(creatorId.longValue(), evaluationInfo.getQueryContext()) : null;
        }
    }

    public final class PrivateIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "private";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.BOOLEAN;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return data.document.isPrivate() ? Boolean.TRUE : Boolean.FALSE;
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentsTable.PRIVATE);
        }

        public QValueType getOutputValueType() {
            return QValueType.BOOLEAN;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }
    }

    public final class RetiredIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "retired";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.BOOLEAN;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return data.document.isRetired() ? Boolean.TRUE : Boolean.FALSE;
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentVariantsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentVariantsTable.RETIRED);
        }

        public QValueType getOutputValueType() {
            return QValueType.BOOLEAN;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }
    }

    public final class VersionCreatorNameIdentifier extends AbstractOutputIdentifier {
        public static final String NAME = "versionCreatorName";

        public QValueType getValueType() {
            return QValueType.STRING;
        }

        public String getName() {
            return NAME;
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return data.version != null ? getUserDisplayName(data.version.getCreator(), evaluationInfo.getQueryContext()) : null;
        }
    }

    public abstract class AbstractOutputIdentifier extends AbstractIdentifier {
        public boolean isOutputOnly() {
            return true;
        }

        public QValueType getValueType() {
            return getOutputValueType();
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
            return getOutputValue(data, evaluationInfo);
        }

        public AclConditionViolation isAclAllowed() {
            return new AclConditionViolation("Identifier \"" + getName() + "\" is not allowed in ACL conditions.");
        }

        public boolean canTestappliesTo() {
            return false;
        }

        public String getSqlPreConditions(SqlGenerationContext context) throws QueryException {
            throw new QueryException("It is not possible to search on identifier " + getName());
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
            throw new QueryException("It is not possible to search on identifier " + getName());
        }

        public int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
            // This will never be reached, as the SQL generation methods throw exceptions
            throw new IllegalStateException();
        }

        public int bindValueExpr(PreparedStatement stmt, int bindPos, QValueType valueType) throws SQLException, QueryException {
            // This will never be reached, as the SQL generation methods throw exceptions
            throw new IllegalStateException();
        }
    }

    public final class SummaryIdentifier extends AbstractOutputIdentifier {
        public static final String NAME = "summary";

        public String getName() {
            return NAME;
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            Version version = null;
            try {
                version = data.document.getVersion(evaluationInfo.getVersionMode());
            } catch (RepositoryException re) {
                throw new RuntimeException(re);
            }
            if (version == null)
                return null;

            return version.getSummary();
        }
    }

    public final class VersionStateIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "versionState";

        public String getName() {
            return NAME;
        }

        public QValueType getOutputValueType() {
            return QValueType.VERSION_STATE;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            if (data.version != null) {
                return data.version.getState();
            } else {
                return null;
            }
        }

        public QValueType getValueType() {
            return QValueType.STRING;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            if (data.version != null) {
                return data.version.getState().getCode();
            } else {
                return null;
            }
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getVersionsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.VersionsTable.STATE);
        }

        public boolean isSymbolic() {
            return true;
        }

        public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
            String versionStateName = (String)valueExpr.evaluate(QValueType.STRING, null, evaluationInfo);
            VersionState versionState = VersionState.fromString(versionStateName);
            return versionState.getCode();
        }
    }

    public final class PartContentIdentifier extends AbstractOutputIdentifier {
        private final String name;
        private final PartType partType;

        public PartContentIdentifier(String name, PartType partType) {
            this.name = name;
            this.partType = partType;
        }

        public QValueType getOutputValueType() {
            return QValueType.XML;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            if (data.versionedData.hasPart(partType.getId())) {
                try {
                    Part part = data.versionedData.getPart(partType.getId());
                    if (part.getMimeType().equals("text/xml") && part.getSize() < CONTENT_INCLUDE_LIMIT) {
                        XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                        return XmlObject.Factory.parse(new ByteArrayInputStream(part.getData()), xmlOptions);
                    }
                } catch (Exception e) {
                    XmlObject error = XmlObject.Factory.newInstance();
                    XmlCursor cursor = error.newCursor();
                    cursor.toNextToken();
                    cursor.beginElement("p");
                    cursor.insertAttributeWithValue("class", "daisy-error");
                    cursor.insertChars("Error getting part data for part " + partType.getId() + " of document " + data.document.getVariantKey() + ", error message: " + e.getMessage());
                    cursor.dispose();
                    return error;
                }
            }
            return null;
        }

        public String getName() {
            return name;
        }

        public String getTitle(Locale locale) {
            return partType.getLabel(locale);
        }

        public void collectAccessRestrictions(AccessRestrictions restrictions) {
            restrictions.addPartReference(partType.getName());
        }
    }

    public final class PartMimeTypeIdentifier extends AbstractNonAclIdentifier {
        private final String name;
        private final PartType partType;
        private String alias;

        public PartMimeTypeIdentifier(String name, PartType partType) {
            this.name = name;
            this.partType = partType;
        }

        public QValueType getValueType() {
            return QValueType.STRING;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            if (data.versionedData.hasPart(partType.getId())) {
                return data.versionedData.getPart(partType.getId()).getMimeType();
            } else {
                return null;
            }
        }

        public String getSqlPreConditions(SqlGenerationContext context) {
            alias = context.getNewPartsTable().getName();
            return alias + '.' + SqlGenerationContext.PartsTable.PARTTYPE_ID + " = ? ";
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(alias).append('.').append(SqlGenerationContext.PartsTable.MIMETYPE);
        }

        public int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException {
            stmt.setLong(bindPos, partType.getId());
            return ++bindPos;
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }

        public String getName() {
            return name;
        }

        public String getTitle(Locale locale) {
            return partType.getLabel(locale) + " (" + getLocalizedString("part.mimetype", locale) + ")";
        }

        public void collectAccessRestrictions(AccessRestrictions restrictions) {
            restrictions.addPartReference(partType.getName());
        }
    }

    public final class PartSizeIdentifier extends AbstractNonAclIdentifier {
        private final String name;
        private final PartType partType;
        private String alias;

        public PartSizeIdentifier(String name, PartType partType) {
            this.name = name;
            this.partType = partType;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            if (data.versionedData.hasPart(partType.getId())) {
                return new Long(data.versionedData.getPart(partType.getId()).getSize());
            } else {
                return null;
            }
        }

        public String getSqlPreConditions(SqlGenerationContext context) {
            alias = context.getNewPartsTable().getName();
            return alias + '.' + SqlGenerationContext.PartsTable.PARTTYPE_ID + " = ? ";
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(alias).append('.').append(SqlGenerationContext.PartsTable.SIZE);
        }

        public int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException {
            stmt.setLong(bindPos, partType.getId());
            return ++bindPos;
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }

        public String getName() {
            return name;
        }

        public String getTitle(Locale locale) {
            return partType.getLabel(locale) + " (" + getLocalizedString("part.size", locale) + ")";
        }

        public void collectAccessRestrictions(AccessRestrictions restrictions) {
            restrictions.addPartReference(partType.getName());
        }
    }

    public final class LockTypeIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "lockType";

        public QValueType getValueType() {
            return QValueType.STRING;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            LockInfo lockInfo;
            try {
                lockInfo = data.document.getLockInfo(false);
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
            if (lockInfo.hasLock()) {
                return lockInfo.getType().getCode();
            } else {
                return null;
            }
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getLocksTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.LocksTable.LOCKTYPE);
        }

        public boolean isSymbolic() {
            return true;
        }

        public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
            String lockTypeName = (String)valueExpr.evaluate(QValueType.STRING, null, evaluationInfo);
            LockType lockType = LockType.fromString(lockTypeName);
            return lockType.getCode();
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            LockInfo lockInfo;
            try {
                lockInfo = data.document.getLockInfo(false);
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
            if (lockInfo.hasLock()) {
                return lockInfo.getType().toString();
            } else {
                return null;
            }
        }

        public String getName() {
            return NAME;
        }
    }

    public final class LockOwnerIdIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "lockOwnerId";

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            LockInfo lockInfo;
            try {
                lockInfo = data.document.getLockInfo(false);
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
            if (lockInfo.hasLock()) {
                return new Long(lockInfo.getUserId());
            } else {
                return null;
            }
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getLocksTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.LocksTable.OWNER_ID);
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }

        public String getName() {
            return NAME;
        }
    }

    public final class LockOwnerLoginIdentifier extends AbstractLoginIdentifier {
        public static final String NAME = "lockOwnerLogin";

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            LockInfo lockInfo;
            try {
                lockInfo = data.document.getLockInfo(false);
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
            if (lockInfo.hasLock()) {
                return new Long(lockInfo.getUserId());
            } else {
                return null;
            }
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getLocksTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.LocksTable.OWNER_ID);
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            LockInfo lockInfo;
            try {
                lockInfo = data.document.getLockInfo(false);
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
            if (lockInfo.hasLock()) {
                return getUserLogin(lockInfo.getUserId(), evaluationInfo.getQueryContext());
            } else {
                return null;
            }
        }

        public String getName() {
            return NAME;
        }
    }

    public final class LockOwnerNameIdentifier extends AbstractOutputIdentifier {
        public static final String NAME = "lockOwnerName";

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            LockInfo lockInfo;
            try {
                lockInfo = data.document.getLockInfo(false);
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
            if (lockInfo.hasLock()) {
                return getUserDisplayName(lockInfo.getUserId(), evaluationInfo.getQueryContext());
            } else {
                return null;
            }
        }

        public String getName() {
            return NAME;
        }
    }

    public final class LockTimeAcquiredIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "lockTimeAcquired";

        public QValueType getValueType() {
            return QValueType.DATETIME;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            LockInfo lockInfo;
            try {
                lockInfo = data.document.getLockInfo(false);
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
            if (lockInfo.hasLock()) {
                return lockInfo.getTimeAcquired();
            } else {
                return null;
            }
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getLocksTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.LocksTable.TIME_ACQUIRED);
        }

        public QValueType getOutputValueType() {
            return QValueType.DATETIME;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }

        public String getName() {
            return NAME;
        }
    }

    public final class LockDurationIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "lockDuration";

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            LockInfo lockInfo;
            try {
                lockInfo = data.document.getLockInfo(false);
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
            if (lockInfo.hasLock()) {
                return new Long(lockInfo.getDuration());
            } else {
                return null;
            }
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getLocksTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.LocksTable.DURATION);
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }

        public String getName() {
            return NAME;
        }
    }

    public final class OwnerIdIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "ownerId";

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return new Long(data.document.getOwner());
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentsTable.OWNER);
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }

        public String getName() {
            return NAME;
        }
    }

    public final class OwnerLoginIdentifier extends AbstractLoginIdentifier {
        public static final String NAME = "ownerLogin";

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return new Long(data.document.getOwner());
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentsTable.OWNER);
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return getUserLogin(data.document.getOwner(), evaluationInfo.getQueryContext());
        }

        public String getName() {
            return NAME;
        }
    }

    public final class OwnerNameIdentifier extends AbstractOutputIdentifier {
        public static final String NAME = "ownerName";

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return getUserDisplayName(data.document.getOwner(), evaluationInfo.getQueryContext());
        }

        public String getName() {
            return NAME;
        }
    }

    public final class LastModifierIdIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "lastModifierId";

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return new Long(data.document.getLastModifier());
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentsTable.LAST_MODIFIER);
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }

        public String getName() {
            return NAME;
        }
    }

    public final class LastModifierLoginIdentifier extends AbstractLoginIdentifier {
        public static final String NAME = "lastModifierLogin";

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return new Long(data.document.getLastModifier());
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentsTable.LAST_MODIFIER);
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            long lastModifier = data.document.getLastModifier();
            return lastModifier != -1 ? getUserLogin(lastModifier, evaluationInfo.getQueryContext()) : null;
        }

        public String getName() {
            return NAME;
        }
    }

    public final class LastModifierNameIdentifier extends AbstractOutputIdentifier {
        public static final String NAME = "lastModifierName";

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            long lastModifier = data.document.getLastModifier();
            return lastModifier != -1 ? getUserDisplayName(lastModifier, evaluationInfo.getQueryContext()) : null;
        }

        public String getName() {
            return NAME;
        }
    }

    public final class VariantLastModifierIdIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "variantLastModifierId";

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return new Long(data.document.getVariantLastModifier());
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentVariantsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentVariantsTable.LAST_MODIFIER);
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }

        public String getName() {
            return NAME;
        }
    }

    public final class VariantLastModifierLoginIdentifier extends AbstractLoginIdentifier {
        public static final String NAME = "variantLastModifierLogin";

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return new Long(data.document.getLastModifier());
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentVariantsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentVariantsTable.LAST_MODIFIER);
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            long lastModifier = data.document.getVariantLastModifier();
            return lastModifier != -1 ? getUserLogin(lastModifier, evaluationInfo.getQueryContext()) : null;
        }

        public String getName() {
            return NAME;
        }
    }

    public final class VariantLastModifierNameIdentifier extends AbstractOutputIdentifier {
        public static final String NAME = "variantLastModifierName";

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            long lastModifier = data.document.getVariantLastModifier();
            return lastModifier != -1 ? getUserDisplayName(lastModifier, evaluationInfo.getQueryContext()) : null;
        }

        public String getName() {
            return NAME;
        }
    }

    public final class LastModifiedIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "lastModified";

        public QValueType getValueType() {
            return QValueType.DATETIME;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return data.document.getLastModified();
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentsTable.LAST_MODIFIED);
        }

        public QValueType getOutputValueType() {
            return QValueType.DATETIME;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }

        public String getName() {
            return NAME;
        }
    }

    public final class VariantLastModifiedIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "variantLastModified";

        public QValueType getValueType() {
            return QValueType.DATETIME;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return data.document.getVariantLastModified();
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentVariantsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentVariantsTable.LAST_MODIFIED);
        }

        public QValueType getOutputValueType() {
            return QValueType.DATETIME;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }

        public String getName() {
            return NAME;
        }
    }

    public final class VersionIdIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "versionId";

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            if (data.version != null) {
                return new Long(data.version.getId());
            } else {
                return null;
            }
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getVersionsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.VersionsTable.ID);
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }

        public String getName() {
            return NAME;
        }
    }

    public class CustomFieldIdentifier extends AbstractNonAclIdentifier {
        private final String name;
        private String alias;

        public CustomFieldIdentifier(String name) {
            this.name = name;
        }

        public QValueType getValueType() {
            return QValueType.STRING;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return data.document.getCustomField(name);
        }

        public String getSqlPreConditions(SqlGenerationContext context) throws QueryException {
            // the alias/join is created here for some special conditions (like IsNull) that never make use of the value expr
            alias = context.getNewCustomFieldsTable(name).getName();
            return null;
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(alias);
            sql.append('.');
            sql.append(SqlGenerationContext.CustomFieldsTable.VALUE);
        }

        public int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
            return bindPos;
        }

        public String getTableAlias() {
            return alias;
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }

        public String getName() {
            return '#' + name;
        }

        public String getTitle(Locale locale) {
            return name;
        }
    }
    
    public final class ScoreIdentifier extends AbstractOutputIdentifier {
        public static final String NAME = "score";

        public String getName() {
            return NAME;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            Document document = data.document;
            VariantKey key = new VariantKey(document.getId(), document.getBranchId(), document.getLanguageId());
            float score = 0;            
            
            if (evaluationInfo.getHits() != null)
                score = evaluationInfo.getHits().score(key);
            return new Double(score);
        }

        public QValueType getOutputValueType() {
            return QValueType.DOUBLE;
        }
    }

    public final class ReferenceLanguageIdIdentifier extends AbstractIdentifier {
        public static final String NAME = "referenceLanguageId";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            long refLangId = data.document.getReferenceLanguageId();
            return refLangId == -1 ? null : new Long(refLangId);
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentsTable.REFERENCE_LANGUAGE);
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, evaluationInfo);
        }
    }

    public final class ReferenceLanguageNameIdentifier extends AbstractIdentifier {
        public static final String NAME = "referenceLanguage";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            long refLangId = data.document.getReferenceLanguageId();
            return refLangId == -1 ? null : new Long(refLangId);
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentsTable.REFERENCE_LANGUAGE);
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            long refLangId = data.document.getReferenceLanguageId();
            if (refLangId == -1)
                return null;
            try {
                return evaluationInfo.getQueryContext().getLanguage(refLangId).getName();
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }

        public boolean isSymbolic() {
            return true;
        }

        public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
            String languageName = (String)valueExpr.evaluate(QValueType.STRING, null, evaluationInfo);
            Language language;
            try {
                language = evaluationInfo.getQueryContext().getLanguageByName(languageName);
            } catch (RepositoryException e) {
                throw new QueryException("Error with language name \"" + languageName + "\".", e);
            }
            return new Long(language.getId());
        }
    }

    public final class VersionCommentIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "versionComment";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.STRING;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return data.version != null ? data.version.getChangeComment() : null;
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getVersionsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.VersionsTable.CHANGE_COMMENT);
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }
    }

    public final class VersionChangeTypeIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "versionChangeType";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.STRING;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return data.version != null ? data.version.getChangeType().getCode() : null;
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return data.version.getChangeType().toString();
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getVersionsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.VersionsTable.CHANGE_TYPE);
        }

        public boolean isSymbolic() {
            return true;
        }

        public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
            String changeTypeName = (String)valueExpr.evaluate(QValueType.STRING, null, evaluationInfo);
            return ChangeType.fromString(changeTypeName).getCode();
        }
    }

    public final class LastMajorChangeVersionIdIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "lastMajorChangeVersionId";

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            long id = data.document.getLastMajorChangeVersionId();
            return id == -1 ? null : new Long(id);
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentVariantsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentVariantsTable.LAST_MAJOR_CHANGE_VERSION);
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }

        public String getName() {
            return NAME;
        }
    }

    public final class LiveMajorChangeVersionIdIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "liveMajorChangeVersionId";

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            long id = data.document.getLiveMajorChangeVersionId();
            return id == -1 ? null : new Long(id);
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getDocumentVariantsTable().getName());
            sql.append('.');
            sql.append(SqlGenerationContext.DocumentVariantsTable.LIVE_MAJOR_CHANGE_VERSION);
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }

        public String getName() {
            return NAME;
        }
    }

    public final class SyncedWithIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "syncedWith";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.LINK;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            if (data.version != null) {
                VersionKey versionKey = data.version.getSyncedWith();
                if (versionKey != null)
                    return new VariantKey(versionKey.getDocumentId(), versionKey.getBranchId(), versionKey.getLanguageId());
            }
            return null;
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getVersionsTable().getName());
            sql.append(".");
            sql.append(SqlGenerationContext.VersionsTable.SYNCED_WITH_SEARCH);
        }

        public QValueType getOutputValueType() {
            return QValueType.LINK;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }
    }

    public final class SyncedWithVersionIdIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "syncedWith.versionId";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            if (data.version != null) {
                VersionKey versionKey = data.version.getSyncedWith();
                if (versionKey != null)
                    return new Long(versionKey.getVersionId());
            }
            return null;
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getVersionsTable().getName());
            sql.append(".");
            sql.append(SqlGenerationContext.VersionsTable.SYNCED_WITH_VERSION_ID);
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }
    }

    public class SyncedWithLanguageIdIdentifier extends AbstractNonAclIdentifier {
        public static final String NAME = "syncedWith.languageId";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.LONG;
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            if (data.version != null) {
                VersionKey versionKey = data.version.getSyncedWith();
                if (versionKey != null)
                    return new Long(versionKey.getLanguageId());
            }
            return null;
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getVersionsTable().getName());
            sql.append(".");
            sql.append(SqlGenerationContext.VersionsTable.SYNCED_WITH_LANG_ID);
        }

        public QValueType getOutputValueType() {
            return QValueType.LONG;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, null);
        }
    }

    public final class SyncedWithLanguageNameIdentifier extends SyncedWithLanguageIdIdentifier {
        public static final String NAME = "syncedWith.language";

        public String getName() {
            return NAME;
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
            sql.append(context.getVersionsTable().getName());
            sql.append(".");
            sql.append(SqlGenerationContext.VersionsTable.SYNCED_WITH_LANG_ID);
        }

        public QValueType getOutputValueType() {
            return QValueType.STRING;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            if (data.version == null)
                return null;

            VersionKey versionKey = data.version.getSyncedWith();
            if (versionKey == null)
                return null;

            try {
                return evaluationInfo.getQueryContext().getLanguage(versionKey.getLanguageId()).getName();
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }

        public boolean isSymbolic() {
            return true;
        }

        public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
            String languageName = (String)valueExpr.evaluate(QValueType.STRING, null, evaluationInfo);
            Language language;
            try {
                language = evaluationInfo.getQueryContext().getLanguageByName(languageName);
            } catch (RepositoryException e) {
                throw new QueryException("Error with language name \"" + languageName + "\".", e);
            }
            return new Long(language.getId());
        }
    }

    /**
     * This is a special identifier for use in ACL expressions to determine
     * the "previous ACL state" in case of new documents.
     */
    public final class ConceptualIdentifier extends AbstractIdentifier {
        public static final String NAME = "conceptual";

        public String getName() {
            return NAME;
        }

        public QValueType getValueType() {
            return QValueType.BOOLEAN;
        }

        public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
            // No document stored in the repository will ever be 'conceptual'
            // (using conceptual to search simply does not make sense)
            sql.append(" 1 = 0");
        }

        public Object evaluate(ExprDocData data, EvaluationInfo evaluationInfo) {
            return data.conceptual ? Boolean.TRUE : Boolean.FALSE;
        }

        public QValueType getOutputValueType() {
            return QValueType.BOOLEAN;
        }

        public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
            return evaluate(data, evaluationInfo);
        }

        @Override
        public boolean canTestappliesTo() {
            return true;
        }
    }
}
