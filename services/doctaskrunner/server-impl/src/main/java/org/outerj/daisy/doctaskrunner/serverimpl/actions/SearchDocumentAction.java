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

import org.apache.xmlbeans.XmlObject;
import org.outerj.daisy.doctaskrunner.DocumentExecutionResult;
import org.outerj.daisy.doctaskrunner.DocumentSelection;
import org.outerj.daisy.doctaskrunner.SearchActionMatch;
import org.outerj.daisy.doctaskrunner.TaskContext;
import org.outerj.daisy.doctaskrunner.TaskSpecification;
import org.outerj.daisy.doctaskrunner.commonimpl.SearchActionMatchImpl;
import org.outerj.daisy.doctaskrunner.commonimpl.SearchActionResultImpl;
import org.outerj.daisy.doctaskrunner.serverimpl.actions.serp.DocumentMatches;
import org.outerj.daisy.doctaskrunner.serverimpl.actions.serp.SearchAndReplaceUtil;
import org.outerj.daisy.doctaskrunner.serverimpl.actions.serp.TextFragment;
import org.outerj.daisy.doctaskrunner.spi.AbstractDocumentAction;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.LockInfo;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.acl.AccessManager;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerx.daisy.x10DocumentActions.MatchesDocument;
import org.outerx.daisy.x10DocumentActions.SearchParametersDocument;
import org.outerx.daisy.x10DocumentActions.CaseHandlingAttribute.CaseHandling;
import org.outerx.daisy.x10DocumentActions.MatchesDocument.Matches.Match;
import org.outerx.daisy.x10DocumentActions.SearchParametersDocument.SearchParameters;

public class SearchDocumentAction extends AbstractDocumentAction {
    
    private AccessManager accessManager;
    private Pattern pattern;

    @Override
    public void setup(VariantKey[] variantKeys, TaskSpecification taskSpecifiation, TaskContext taskContext, Repository repository) throws Exception {
        super.setup(variantKeys, taskSpecifiation, taskContext, repository);
        accessManager = repository.getAccessManager();
        SearchParameters paramsXml = SearchParametersDocument.Factory.parse(taskSpecifiation.getParameters()).getSearchParameters();

        String regexp = paramsXml.getRegexp();
        boolean caseSensitive = false;
        if (paramsXml.isSetCaseHandling()) {
            caseSensitive = paramsXml.getCaseHandling().equals(CaseHandling.SENSITIVE);
        }
        this.pattern = Pattern.compile(regexp, caseSensitive?0:Pattern.CASE_INSENSITIVE);
    }

    public void execute(VariantKey variantKey, DocumentExecutionResult result) throws Exception {
        Document doc = repository.getDocument(variantKey, false);
        
        LockInfo lockInfo = doc.getLockInfo(true);
        AclResultInfo aclResultInfo = accessManager.getAclInfoOnLive(repository.getUserId(), repository.getActiveRoleIds(), variantKey);

        SearchActionResultImpl actionResult = new SearchActionResultImpl(lockInfo, aclResultInfo);
        
        DocumentMatches m = new DocumentMatches(repository, doc, pattern);

        if (m.getDocumentNameFragment() != null) {
            SearchActionMatch match = createMatch(m.getDocumentNameFragment());
            actionResult.addMatch(match);
        }
        for (TextFragment frag: m.getPartContentFragments()) {
            SearchActionMatch match = createMatch(frag);
            actionResult.addMatch(match);
        }
        
        result.setMessage(actionResult.getXml().xmlText());
    }

    private SearchActionMatch createMatch(TextFragment fragment) {
        Match dummyMatchXml = MatchesDocument.Factory.newInstance().addNewMatches().addNewMatch();
        XmlObject fragmentXml = dummyMatchXml.addNewFragment();
        SearchAndReplaceUtil.addFragmentXml(fragmentXml.getDomNode(), fragment.getOriginalText(), pattern);
        
        SearchActionMatchImpl match = new SearchActionMatchImpl(fragmentXml);
        match.addAttributes(fragment.getAttributes());
        return match;
    }

    @Override
    public boolean requiresAdministratorRole() {
        return false;
    }
    
    

}