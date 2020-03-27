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
package org.outerj.daisy.repository.query;

import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionMode;

/**
 * A precompiled expression.
 *
 * <p>Obtained via {@link QueryManager#parseValueExpression}.
 */
public interface ValueExpression {
    /**
     * The type of value that will be returned by the evaluate methods.
     */
    ValueType getValueType();

    /**
     * The version argument is optional, if not present (null) version-dependent information
     * will be taken from the document object.
     *
     * <p>This is the same as calling evaluate(document, version, version == null ? VersionMode.LAST : VersionMode.LIVE, null).
     *
     * <p>The type of object returned by this method corresponds to the value type
     * as returned by {@link #getValueType()}, though in case the expression returns
     * a multivalue or hierarchical expression, it will be an array of such objects
     * respectively an {@link org.outerj.daisy.repository.HierarchyPath}.
     *
     * <p>The result of evaluation can also be null.
     *
     */
    Object evaluate(Document document, Version version) throws QueryException;

    /**
     * Same as the other evaluate method, but allows to specify if, when traversing
     * links (e.g. using the dereference operator "=>"), the last or live version
     * should be used of the linked-to documents.
     */
    Object evaluate(Document document, Version version, VersionMode versionMode) throws QueryException;

    Object evaluate(Document document, Version version, VersionMode versionMode, EvaluationContext evaluationContext) throws QueryException;

    /**
     * This evaluate allows to specify a specific repository to use during evaluation.
     *
     * <p>This can be useful if you want to compile an expression just once, but then
     * evaluate it on multiple occasions for different users.
     *
     * <p>Note that many expressions don't need access to a repository at all, it is only
     * some expressions (like dereferencing) which might need access to the repository.
     */
    Object evaluate(Document document, Version version, VersionMode versionMode, Repository repository) throws QueryException;

    Object evaluate(Document document, Version version, VersionMode versionMode, EvaluationContext evaluationContext, Repository repository) throws QueryException;
}
