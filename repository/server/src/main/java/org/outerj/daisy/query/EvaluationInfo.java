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
package org.outerj.daisy.query;

import org.outerj.daisy.ftindex.Hits;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.query.EvaluationContext;
import org.outerj.daisy.repository.query.QueryException;

public class EvaluationInfo {
    private EvaluationContext evaluationContext;
    private QueryContext queryContext;
    private Hits hits;
    private VersionMode versionMode;

    public EvaluationInfo(QueryContext queryContext) {
        this.queryContext = queryContext;
        this.evaluationContext = new EvaluationContext();
    }

    public EvaluationInfo(QueryContext queryContext, EvaluationContext evaluationContext) {
        this.queryContext = queryContext;
        this.evaluationContext = evaluationContext;
    }

    public EvaluationContext getEvaluationContext() {
        return evaluationContext;
    }

    public void setEvaluationContext(EvaluationContext evaluationContext) {
        this.evaluationContext = evaluationContext;
    }

    public Hits getHits() {
        return hits;
    }

    public VersionMode getVersionMode() {
        if (versionMode == null) {
            throw new RuntimeException("Tried to access versionMode flag, but it is not set.");
        }
        return versionMode;
    }

    public void setVersionMode(VersionMode versionMode) {
        this.versionMode = versionMode;
    }

    public void setHits(Hits hits) {
        this.hits = hits;
    }

    public QueryContext getQueryContext() {
        return queryContext;
    }

    public void dispose() throws QueryException {
        if (this.hits != null)
            this.hits.dispose();
    }
}
