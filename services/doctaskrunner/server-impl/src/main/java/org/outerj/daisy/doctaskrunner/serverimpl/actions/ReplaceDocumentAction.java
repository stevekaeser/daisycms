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
package org.outerj.daisy.doctaskrunner.serverimpl.actions;

import java.util.regex.Pattern;

import org.outerj.daisy.doctaskrunner.DocumentExecutionResult;
import org.outerj.daisy.doctaskrunner.DocumentExecutionState;
import org.outerj.daisy.doctaskrunner.DocumentSelection;
import org.outerj.daisy.doctaskrunner.TaskContext;
import org.outerj.daisy.doctaskrunner.TaskSpecification;
import org.outerj.daisy.doctaskrunner.serverimpl.CaseHandling;
import org.outerj.daisy.doctaskrunner.serverimpl.actions.serp.DocumentMatches;
import org.outerj.daisy.doctaskrunner.spi.AbstractDocumentAction;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.LockInfo;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerx.daisy.x10DocumentActions.ReplaceParametersDocument;
import org.outerx.daisy.x10DocumentActions.ReplaceParametersDocument.ReplaceParameters;

public class ReplaceDocumentAction extends AbstractDocumentAction {
    
    private Pattern pattern;
    private CaseHandling caseHandling = CaseHandling.INSENSITIVE; // this is the default behaviour
    private String replacement;
    
    @Override
    public void setup(VariantKey[] variantKeys,
            TaskSpecification taskSpecifiation, TaskContext taskContext,
            Repository repository) throws Exception {
        super.setup(variantKeys, taskSpecifiation, taskContext, repository);
        ReplaceParameters paramsXml = ReplaceParametersDocument.Factory.parse(taskSpecifiation.getParameters()).getReplaceParameters();

        String regexp = paramsXml.getRegexp();
        
        if (paramsXml.isSetCaseHandling()) {
            caseHandling = CaseHandling.fromString(paramsXml.getCaseHandling().toString());
        }
        
        // note: both CaseHandling.INSENSITIVE and CaseHandling.SENSIBLE need a case insensitive java.util.regexp.Pattern 
        boolean caseSensitive = caseHandling.equals(CaseHandling.SENSITIVE);
        this.pattern = Pattern.compile(regexp, caseSensitive?0:Pattern.CASE_INSENSITIVE);
        replacement = paramsXml.getReplacement();
    }

    
    public void execute(VariantKey variantKey, DocumentExecutionResult result) throws Exception {
        Document doc = repository.getDocument(variantKey, true);
        
        LockInfo lockInfo = doc.getLockInfo(true);
        if (lockInfo != null) {
            if (lockInfo.getUserId() == repository.getUserId()) {
                String message = new StringBuffer("skipped (document is currently locked by ")
                    .append(repository.getUserManager().getPublicUserInfo(lockInfo.getUserId()).getDisplayName())
                    .append("(").append(lockInfo.getUserId())
                    .append("))").toString();
                result.setMessage(message);
                result.setState(DocumentExecutionState.ERROR);
                return;
            }
        }
        
        DocumentMatches m = new DocumentMatches(repository, doc, pattern);
        
        int replacements = m.replaceMatches(replacement, caseHandling.equals(CaseHandling.SENSIBLE));

        if (replacements > 0) {
            doc.save(false);
        }
        
        StringBuffer message = new StringBuffer("replaced ")
            .append(replacements)
            .append(" occurrence(s).");
        if (m.isSkippedDocumentName() || m.isSkippedParts()) {
            message.append(" Some occurrences may have been skipped because you do not have full write access.");
        }
        
        result.setMessage(message.toString()); 
    }
    
    @Override
    public boolean requiresAdministratorRole() {
        return false;
    }

}