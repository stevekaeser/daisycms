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
package org.outerj.daisy.doctaskrunner.commonimpl;

import org.outerj.daisy.doctaskrunner.SearchActionMatch;
import org.outerj.daisy.doctaskrunner.SearchActionMatches;
import org.outerj.daisy.doctaskrunner.SearchActionResult;
import org.outerj.daisy.repository.LockInfo;
import org.outerj.daisy.repository.LockType;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.commonimpl.LockInfoImpl;
import org.outerj.daisy.repository.commonimpl.acl.AclResultInfoImpl;
import org.outerj.daisy.util.ListUtil;
import org.outerx.daisy.x10.LockInfoDocument;
import org.outerx.daisy.x10.AclResultDocument.AclResult;
import org.outerx.daisy.x10DocumentActions.MatchesDocument;
import org.outerx.daisy.x10DocumentActions.SearchActionResultDocument;

public class SearchActionResultImpl implements SearchActionResult {
    
    private LockInfo lockInfo;
    private AclResultInfo aclResultInfo;
    private SearchActionMatchesImpl matches = new SearchActionMatchesImpl();
    
    public SearchActionResultImpl() {
        
    }
    
    public SearchActionResultImpl(LockInfo lockInfo, AclResultInfo aclResultInfo) {
        if (lockInfo == null)
            throw new NullPointerException("lockInfo should not be null");
        if (aclResultInfo == null)
            throw new NullPointerException("aclResultInfo should not be null");

        this.lockInfo = lockInfo;
        this.aclResultInfo = aclResultInfo;
    }
    
    public LockInfo getLockInfo() {
        return this.lockInfo;
    }

    public AclResultInfo getAclResultInfo() {
        return this.aclResultInfo;
    }
    
    public SearchActionMatches getMatches() {
        return matches;
    }
    
    public void addMatch(SearchActionMatch match) {
        matches.addMatch(match);
    }

    public SearchActionResultDocument getXml() {
        SearchActionResultDocument result = SearchActionResultDocument.Factory.newInstance();
        SearchActionResultDocument.SearchActionResult resultXml = result.addNewSearchActionResult();
        resultXml.setLockInfo(lockInfo.getXml().getLockInfo());
        resultXml.setAclResult(aclResultInfo.getXml().getAclResult());
        resultXml.setMatches(matches.getXml().getMatches());
        return result;
    }
    
    public void setFromXml(SearchActionResultDocument xmlDoc) {
        SearchActionResultDocument.SearchActionResult xml = xmlDoc.getSearchActionResult();

        lockInfo = instantiateLockInfo(xml.getLockInfo());

        AclResult aclResultXml = xml.getAclResult();
        this.aclResultInfo = new AclResultInfoImpl(aclResultXml.getUser().getId(), ListUtil.toArray(aclResultXml.getUser().getRoles().getRoleIdList()), aclResultXml.getDocumentId(), aclResultXml.getBranchId(), aclResultXml.getLanguageId());
        aclResultInfo.setFromXml(aclResultXml);
        
        for (MatchesDocument.Matches.Match matchXml: xml.getMatches().getMatchList()) {
            SearchActionMatchImpl matchImpl = new SearchActionMatchImpl();
            matchImpl.setFromXml(matchXml);
            
            addMatch(matchImpl);
        }
    }

    /**
     * Note: code duplicated from RemoteDocumentStrategy.  Perhaps this should be moved as a static method of LockInfoImpl.
     */
    private LockInfoImpl instantiateLockInfo(LockInfoDocument.LockInfo lockInfoXml) {
        if (!lockInfoXml.getHasLock())
            return new LockInfoImpl();

        return new LockInfoImpl(lockInfoXml.getUserId(), lockInfoXml.getTimeAcquired().getTime(),
                lockInfoXml.getDuration(), LockType.fromString(lockInfoXml.getType().toString()));
    }

}
