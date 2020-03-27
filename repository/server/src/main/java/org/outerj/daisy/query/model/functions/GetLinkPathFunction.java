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
package org.outerj.daisy.query.model.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.query.model.ExprDocData;
import org.outerj.daisy.query.model.QValueType;
import org.outerj.daisy.query.model.ValueExpr;
import org.outerj.daisy.query.model.ValueExprUtil;
import org.outerj.daisy.repository.AccessException;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentNotFoundException;
import org.outerj.daisy.repository.DocumentVariantNotFoundException;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionNotFoundException;
import org.outerj.daisy.repository.VersionedData;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.repository.schema.FieldType;

/**
 * Returns a HierarchyPath object obtained by recursively following a link field
 * (until null or recursion), the elements of the path are the link field values.
 */
public class GetLinkPathFunction extends AbstractNonBindableFunction {
    public static final String NAME = "GetLinkPath";

    public String getFunctionName() {
        return NAME;
    }

    public void prepare(QueryContext context) throws QueryException {
        super.prepare(context);

        if (params.size() < 1 || params.size() > 3)
            throw new QueryException("\"" + getFunctionName() + "\" takes at least one and at most three parameters, at " + getLocation());

        if (!ValueExprUtil.isPrimitiveValue(QValueType.STRING, getParam(0)))
            throw new QueryException("First argument of " + getFunctionName() + " should evaluate to a string, at " + getParam(0).getLocation());

        if (params.size() >= 2 && !ValueExprUtil.isPrimitiveValue(QValueType.BOOLEAN, getParam(1)))
            throw new QueryException("Second argument of " + getFunctionName() + " should evaluate to a boolean, at " + getParam(1).getLocation());

        if (params.size() >= 3 && !ValueExprUtil.isPrimitiveValue(QValueType.LINK, getParam(2)))
            throw new QueryException("Second argument of " + getFunctionName() + " should evaluate to a link, at " + getParam(2).getLocation());
    }

    public String getTitle(Locale locale) {
        return getExpression();
    }

    public QValueType getValueType() {
        return QValueType.LINK;
    }

    public QValueType getOutputValueType() {
        return getValueType();
    }

    public boolean isHierarchical() {
        return true;
    }

    public Object evaluate(QValueType valueType, ExprDocData data, final EvaluationInfo evaluationInfo) throws QueryException {
        String fieldTypeName = (String)getParam(0).evaluate(QValueType.STRING, data, evaluationInfo);
        FieldType fieldType;
        try {
            fieldType = evaluationInfo.getQueryContext().getFieldTypeByName(fieldTypeName);
        } catch (RepositoryException e) {
            throw new QueryException("Error getting field type named \"" + fieldTypeName + "\" specified as argument of function " + getFunctionName(), e);
        }

        if (fieldType.getValueType() != ValueType.LINK || !fieldType.isPrimitive())
            throw new QueryException("Field type \"" + fieldTypeName + "\" used as argument of function " + getFunctionName() + " is not a primitive link type field.");


        boolean includeStartDoc = false;
        if (params.size() >= 2) {
            includeStartDoc = (Boolean)getParam(1).evaluate(QValueType.BOOLEAN, data, evaluationInfo);
        }

        Document currentDocument = null;
        Version currentVersion = null;
        if (params.size() >= 3) {
            VariantKey variantKey = (VariantKey)getParam(2).evaluate(QValueType.LINK, data, evaluationInfo);
            if (variantKey != null) {
                Object[] docAndVersion = getDocAndVersion(variantKey, evaluationInfo);
                if (docAndVersion != null) {
                    currentDocument = (Document)docAndVersion[0];
                    currentVersion = (Version)docAndVersion[1];
                }
            }
        } else if (data != null) {
            currentDocument = data.document;
            currentVersion = data.version;
        }
        VersionedData currentVersionedData = currentVersion != null ? currentVersion : currentDocument;

        List<VariantKey> items = new ArrayList<VariantKey>();

        if (includeStartDoc && currentDocument != null) {
            items.add(currentDocument.getVariantKey());
        }

        while (currentVersionedData != null && currentVersionedData.hasField(fieldType.getId())) {
            VariantKey value = (VariantKey)currentVersionedData.getField(fieldType.getId()).getValue();
            // absolutize variant key
            value = new VariantKey(value.getDocumentId(),
                    value.getBranchId() == -1 ? currentDocument.getBranchId() : value.getBranchId(),
                    value.getLanguageId() == -1 ? currentDocument.getLanguageId() : value.getLanguageId());

            if (items.contains(value)) {
                // recursion detected
                // We could throw an exception, but for this case it seems more convenient to avoid
                // that the query fails.
                // Add this value anyway so that the recursion is visible.
                items.add(value);
                break;
            }

            items.add(value);

            Object[] docAndVersion = getDocAndVersion(value, evaluationInfo);
            if (docAndVersion != null) {
                currentDocument = (Document)docAndVersion[0];
                currentVersion = (Version)docAndVersion[1];
                currentVersionedData = currentVersion != null ? currentVersion : currentDocument;
            } else {
                currentVersionedData = null;
            }
        }

        return items.isEmpty() ? null : new HierarchyPath(items.toArray());
    }

    private Object[] getDocAndVersion(VariantKey variantKey, EvaluationInfo evaluationInfo) throws QueryException {
        Object[] result = null;
        try {
            Document document = evaluationInfo.getQueryContext().getDocument(variantKey);
            Version version = document.getVersion(evaluationInfo.getVersionMode());
            result = new Object[] { document, version };
        } catch (DocumentNotFoundException e) {
            // ok
        } catch (DocumentVariantNotFoundException e) {
            // ok
        } catch (AccessException e) {
            // ok
        } catch (VersionNotFoundException e) {
            // ok
        } catch (RepositoryException e) {
            throw new QueryException("Unexpected error retrieving document in function " + getFunctionName(), e);
        }
        return result;
    }

    public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        return evaluate(null, data, evaluationInfo);
    }


    public ValueExpr clone() {
        GetLinkPathFunction clone = new GetLinkPathFunction();
        super.fillInClone(clone);
        return null;
    }
}
