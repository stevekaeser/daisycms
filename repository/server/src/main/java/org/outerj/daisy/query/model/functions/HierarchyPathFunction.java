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

import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.model.*;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.repository.HierarchyPath;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;

/**
 * Converts a literal representation of a hierarchy path to a HierarchyPath object.
 */
public class HierarchyPathFunction extends AbstractFunction {
    private String pathSpec;
    private Literal[] literals;
    public final static String NAME = "Path";

    public String getFunctionName() {
        return NAME;
    }

    public void prepare(QueryContext context) throws QueryException {
        super.prepare(context);
        if (params.size() != 1) {
            throw new QueryException("Function " + NAME + " expects exactly 1 parameter.");
        }

        ValueExpr param = getParam(0);
        if (!(param instanceof Literal))
            throw new QueryException("Function " + NAME + " requires a string literal as parameter.");

        pathSpec = (String)((Literal)param).evaluate(QValueType.STRING, null);

        String[] pathParts = pathSpec.split("/");
        List<Literal> literalsList = new ArrayList<Literal>();
        for (String pathPart : pathParts) {
            pathPart = pathPart.trim();
            if (pathPart.length() > 0) {
                Literal literal = new Literal(pathPart, pathPart);
                literal.prepare(context);
                literalsList.add(literal);
            }
        }
        literals = literalsList.toArray(new Literal[0]);
    }

    public Object evaluate(QValueType valueType, ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        Object[] elements = new Object[literals.length];
        for (int i = 0; i < literals.length; i++) {
            elements[i] = literals[i].evaluate(valueType, evaluationInfo);
        }
        return new HierarchyPath(elements);
    }

    public QValueType getValueType() {
        return QValueType.UNDEFINED;
    }

    public boolean isHierarchical() {
        return true;
    }

    public String getSqlPreConditions(SqlGenerationContext context) throws QueryException {
        return null;
    }

    public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        throw new QueryException("Hierarchy path expression can't generate SQL value expression.");
    }

    public int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        return bindPos;
    }

    public int bindValueExpr(PreparedStatement stmt, int bindPos, QValueType valueType, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        return bindPos;
    }

    public String getTitle(Locale locale) {
        return "(literal hierarchy path)";
    }

    public QValueType getOutputValueType() {
        return QValueType.STRING;
    }

    public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        return pathSpec;
    }

    public ValueExpr clone() {
        HierarchyPathFunction clone = new HierarchyPathFunction();
        super.fillInClone(clone);
        clone.literals = literals;
        clone.pathSpec = pathSpec;
        return clone;
    }
}
