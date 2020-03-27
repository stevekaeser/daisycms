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

import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.repository.query.QueryException;

/**
 * Base interface for predicate and value expression.
 */
public interface Expression {
    /**
     * Prepare is called after building up the expression tree but before
     * using it for evaluation or SQL generation.
     *
     * <p>Expressions should recursively call prepare on any of their
     * child-expressions.
     *
     * <p>During prepare, various initialization and checks can be done, such
     * as checking whether the number and type of the arguments is correct.
     *
     * <p>The provided {@link QueryContext} object should only be used during
     * the preparation step, and not be stored for later use. This is because
     * an expression might be parsed + prepared using one user's repository,
     * but then evaluated for other users. This is especially important for
     * expressions which might retrieve documents.
     */
    void prepare(QueryContext context) throws QueryException;

    /**
     * Runs through the whole expression hierarchy to collect things that need extra access checking.
     */
    void collectAccessRestrictions(AccessRestrictions restrictions);

    public void setLocation(int line, int column);

    public String getLocation();

    public int getLine();

    public int getColumn();
}
