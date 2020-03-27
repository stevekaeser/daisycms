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
package org.outerj.daisy.query.model.functions;

import java.util.Locale;

import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.query.model.*;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.acl.AclDetailPermission;
import org.outerj.daisy.repository.acl.AccessDetails;
import org.outerj.daisy.repository.query.QueryException;

public class FullTextFragmentFunction extends AbstractFunction {
    public final static String NAME="FullTextFragment"; 

    public String getFunctionName() {
        return NAME;
    }
    
    public void prepare(QueryContext context) throws QueryException {        
        super.prepare(context);
        if (params.size() > 1)
            throw new QueryException(NAME + " function expects one parameter at the most.");
    }

    public Object evaluate(QValueType valueType, ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        return getOutputValue(data, evaluationInfo);
    }

    public QValueType getValueType() {        
        return QValueType.XML;
    }

    public String getTitle(Locale locale) {
        return getExpression();
    }

    public QValueType getOutputValueType() {
        return QValueType.XML;
    }

    public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        if (data == null)
            throw new ExprDocDataMissingException(getFunctionName(), getLocation());

        if (data.aclResultInfo == null) // can't check whether user is allowed access
            return null;

        AccessDetails accessDetails = data.aclResultInfo.getAccessDetails(AclPermission.READ);
        if (accessDetails != null && !accessDetails.isGranted(AclDetailPermission.FULLTEXT_FRAGMENTS))
            return null;
        
        VariantKey key = data.document.getVariantKey();
        Object fragments = null;
        int fragmentAmount = 1;

        // default fragment amount = 1
        if (params.size() == 1)
            fragmentAmount = Integer.parseInt((String)getParam(0).evaluate(QValueType.STRING, data, evaluationInfo));
        
        try {            
            if (evaluationInfo.getHits() != null)
                fragments = evaluationInfo.getHits().contextFragments(key, fragmentAmount);
        } catch (Exception e) {
            throw new QueryException("Could not retrieve context fragments for document variant : " + key, e);
        }
        return fragments;
    }

    public ValueExpr clone() {
        FullTextFragmentFunction clone = new FullTextFragmentFunction();
        super.fillInClone(clone);
        return clone;
    }
}
