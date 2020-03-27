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
package org.outerj.daisy.frontend.util;

import org.apache.cocoon.acting.Action;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.components.flow.FlowHelper;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.CompiledExpression;

import java.util.Map;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class JXTestAction implements Action, ThreadSafe {
    Map<String, CompiledExpression> expressionCache = new ConcurrentHashMap<String, CompiledExpression>(16, .75f, 2);

    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        String testExpr = parameters.getParameter("test");

        CompiledExpression compiledExpr = expressionCache.get(testExpr);
        if (compiledExpr == null) {
            // multiple threads might do this at the same time, but that's not really a problem
            compiledExpr = JXPathContext.compile(testExpr);
            expressionCache.put(testExpr, compiledExpr);
        }

        JXPathContext jxContext = JXPathContext.newContext(FlowHelper.getContextObject(objectModel));

        Object value = compiledExpr.getValue(jxContext);
        if (value != null && value instanceof Boolean && value.equals(Boolean.TRUE)) {
            return Collections.emptyMap();
        } else {
            return null;
        }
    }
}
