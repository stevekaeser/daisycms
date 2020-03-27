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
import java.util.Locale;

import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.repository.AccessException;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentNotFoundException;
import org.outerj.daisy.repository.DocumentVariantNotFoundException;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;
import org.outerj.daisy.repository.query.QueryException;

/**
 * A dereference expression allows to follow through links, in order to evaluate identifiers on
 * the linked-to document.
 *
 * <p>The dereference operator pushes the traversed document on the evaluation context stack,
 * so that it modifies the behaviour of the ContextDoc function. This makes it possible for
 * further dereferences to refer to values of fields of documents higher in the stack, though at the
 * time of this writing there is no value expression which can make use of this (I think).
 */
public class Dereference extends AbstractExpression implements ValueExpr {
    private final ValueExpr refValueExpr;
    private ValueExpr derefValueExpr;

    public Dereference(ValueExpr refValueExpr, ValueExpr derefValueExpr) {
        this.refValueExpr = refValueExpr;
        this.derefValueExpr = derefValueExpr;
    }

    public void prepare(QueryContext context) throws QueryException {
        refValueExpr.prepare(context);
        derefValueExpr.prepare(context);

        if (!ValueExprUtil.isPrimitiveValue(QValueType.LINK, refValueExpr))
            throw new QueryException("Left-hand side of the dereference operator (=>) should be a link value, at " + refValueExpr.getLocation());

        // Any sort of ValueExpr could be made allowed for the derefValueExpr, but in fact
        // that currently doesn't add anything (doesn't make much sense for literals or functions),
        // so rather start restrictive
        if (!(derefValueExpr instanceof Identifier || derefValueExpr instanceof Dereference))
            throw new QueryException("Right-hand side of the dereference operator (=>) should be an identifier or another dereference operator.");
    }

    public Object evaluate(final QValueType valueType, final ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        if (data == null)
            throw new ExprDocDataMissingException(getExpression(), getLocation());

        return evalDeref(data, evaluationInfo, new DerefEvalCallback() {
            public Object derefEval(ExprDocData data, EvaluationInfo evalutionInfo) throws QueryException {
                return derefValueExpr.evaluate(valueType, data, evalutionInfo);
            }
        });
    }

    //
    // Implementation insight note:
    //  in case there is a chain of dereference operators, all these methods will simply
    //  "fall through" until the last element in the chain is reached (a derefValueExpr
    //  which is not a Dereference itself)
    //

    public QValueType getValueType() {
        return derefValueExpr.getValueType();
    }

    public boolean isSymbolicIdentifier() {
        return derefValueExpr.isSymbolicIdentifier();
    }

    public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
        return derefValueExpr.translateSymbolic(valueExpr, evaluationInfo);
    }

    public boolean isMultiValue() {
        return derefValueExpr.isMultiValue();
    }

    public boolean isHierarchical() {
        return derefValueExpr.isHierarchical();
    }

    public boolean isOutputOnly() {
        return derefValueExpr.isOutputOnly();
    }

    public String getSqlPreConditions(SqlGenerationContext context) throws QueryException {
        // Note: it is pushDereference which will create the appropriate join and
        // puts the pre-conditions and the sql-value-expr of the refValueExpr to use
        context.pushDereference(refValueExpr);
        String preCond = derefValueExpr.getSqlPreConditions(context);
        context.popDereference();
        return preCond;
    }

    public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        context.pushDereference(refValueExpr);
        derefValueExpr.generateSqlValueExpr(sql, context);
        context.popDereference();
    }

    public int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        return derefValueExpr.bindPreConditions(stmt, bindPos, evaluationInfo);
    }

    public int bindValueExpr(PreparedStatement stmt, int bindPos, QValueType valueType, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        return derefValueExpr.bindValueExpr(stmt, bindPos, valueType, evaluationInfo);
    }

    public String getTitle(Locale locale) {
        return refValueExpr.getTitle(locale) + " => " + derefValueExpr.getTitle(locale);
    }

    public String getExpression() {
        return refValueExpr.getExpression() + "=>" + derefValueExpr.getExpression();
    }

    public QValueType getOutputValueType() {
        return derefValueExpr.getOutputValueType();
    }

    public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        if (data == null)
            throw new ExprDocDataMissingException(getExpression(), getLocation());
        
        return evalDeref(data, evaluationInfo, new DerefEvalCallback() {
            public Object derefEval(ExprDocData data, EvaluationInfo evalutionInfo) throws QueryException {
                return derefValueExpr.getOutputValue(data, evalutionInfo);
            }
        });
    }

    private Object evalDeref(ExprDocData data, EvaluationInfo evaluationInfo, DerefEvalCallback derefEval) throws QueryException {
        VariantKey link = (VariantKey)refValueExpr.evaluate(QValueType.LINK, data, evaluationInfo);
        if (link == null)
            return null;

        Document linkedDoc;
        Version linkedVersion = null;
        try {
            linkedDoc = evaluationInfo.getQueryContext().getDocument(link);
            linkedVersion = linkedDoc.getVersion(evaluationInfo.getVersionMode());
            if (linkedVersion == null)
                return null;
        } catch (DocumentNotFoundException e) {
            return null;
        } catch (DocumentVariantNotFoundException e) {
            return null;
        } catch (NamespaceNotFoundException e) {
            return null;
        } catch (AccessException e) {
            return null;
        } catch (RepositoryException e) {
            throw new QueryException("Error retrieving document or document version in query dereference operator for link value " + link);
        }

        evaluationInfo.getEvaluationContext().pushContextDocument(linkedDoc, linkedVersion);
        try {
            return derefEval.derefEval(new ExprDocData(linkedDoc, linkedVersion), evaluationInfo);
        } finally {
            evaluationInfo.getEvaluationContext().popContextDocument();
        }
    }

    private static interface DerefEvalCallback {
        Object derefEval(ExprDocData data, EvaluationInfo evalutionInfo) throws QueryException;
    }

    public void collectAccessRestrictions(AccessRestrictions restrictions) {
        restrictions.startRefExpr(refValueExpr);
        derefValueExpr.collectAccessRestrictions(restrictions);
        restrictions.end();
    }

    public boolean canTestAppliesTo() {
        return derefValueExpr.canTestAppliesTo();
    }

    public AclConditionViolation isAclAllowed() {
        return derefValueExpr.isAclAllowed();
    }

    /**
     * Gets the value expression at the end of a dereference chain.
     */
    public ValueExpr getFinalValueExpr() {
        if (derefValueExpr instanceof Dereference) {
            return ((Dereference)derefValueExpr).getFinalValueExpr();
        } else {
            return derefValueExpr;
        }
    }

    public ValueExpr clone() {
        Dereference clone = new Dereference(refValueExpr.clone(), derefValueExpr.clone());
        return clone;
    }

    /**
     * Changes the deref expression at the end of the dereference chain.
     * The newExpr should already be 'prepared'.
     * Intended for very special internal use cases.
     */
    public void changeDerefValueExpr(ValueExpr newExpr) {
        if (!(newExpr instanceof Identifier || newExpr instanceof Dereference))
            throw new RuntimeException("Right-hand side of the dereference operator (=>) should be an identifier or another dereference operator.");

        if (derefValueExpr instanceof Dereference) {
            ((Dereference)derefValueExpr).changeDerefValueExpr(newExpr);
        } else  {
            derefValueExpr = newExpr;
        }
    }

    public void doInContext(SqlGenerationContext context, ContextualizedRunnable runnable) throws QueryException {
        context.pushDereference(refValueExpr);
        derefValueExpr.doInContext(context, runnable);
        context.popDereference();
    }
}
