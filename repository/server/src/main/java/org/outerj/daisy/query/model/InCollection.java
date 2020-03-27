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

import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InCollection extends AbstractPredicateExpr {
    private final List<Literal> literals = new ArrayList<Literal>();
    private DocumentCollection[] collections;

    public void add(Literal literal) {
        literals.add(literal);
    }

    public void prepare(QueryContext context) throws QueryException {
        collections = new DocumentCollection[literals.size()];
        int i = 0;
        for (Literal literal : literals) {
            String collectionName = (String)literal.evaluate(QValueType.STRING, null);
            try {
                collections[i] = context.getCollection(collectionName);
            } catch (CollectionNotFoundException e) {
                throw new QueryException("There is no collection with ID or name \"" + collectionName + "\".", e);
            } catch (RepositoryException e) {
                throw new QueryException("Error consulting document collection information.", e);
            }
            i++;
        }
    }

    public boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        if (data == null)
            throw new ExprDocDataMissingException("InCollection", getLocation());

        for (DocumentCollection collection : collections)
            if (data.document.inCollection(collection))
                return true;
        return false;
    }

    public Tristate appliesTo(ExprDocData data, EvaluationInfo evaluationInfo) {
        DocumentCollection[] documentCollections = data.document.getCollections().getArray();

        if (documentCollections.length == 0) {
            return Tristate.MAYBE;
        } else if (documentCollections.length != 1) {
            // Note: we assume the supplied document belongs to zero or one collections, which
            // will alwyas be the case with the current usage of appliesTo
            throw new RepositoryRuntimeException("Unexpected situation (bug): InCollection.appliesTo: given document belongs to more than one collection.");
        }

        for (DocumentCollection collection : collections)
            if (collection.getId() == documentCollections[0].getId())
                return Tristate.YES;

        // the document might be added other collections too
        return Tristate.MAYBE;
    }

    public void generateSql(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        // Note: for each InCollection condition, a new join with the document_collections table
        // is created, otherwise and conditions wouldn't work.
        String alias = context.getNewCollectionsTable().getName();
        sql.append(" ");
        sql.append(alias);
        sql.append(".");
        sql.append(SqlGenerationContext.DocsCollectionsTable.COLLECTION_ID);
        sql.append(" IN(");
        for (int i = 0; i < collections.length; i++) {
            if (i == collections.length - 1)
                sql.append("?");
            else
                sql.append("?,");
        }
        sql.append(")");
    }

    public int bindSql(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException {
        for (DocumentCollection collection : collections) {
            stmt.setLong(bindPos, collection.getId());
            bindPos++;
        }
        return bindPos;
    }

    public AclConditionViolation isAclAllowed() {
        return null;
    }

    public void collectAccessRestrictions(AccessRestrictions restrictions) {
        for (Expression expr : literals) {
            expr.collectAccessRestrictions(restrictions);
        }
    }
}
