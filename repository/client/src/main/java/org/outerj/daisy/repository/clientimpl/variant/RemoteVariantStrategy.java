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
package org.outerj.daisy.repository.clientimpl.variant;

import org.outerj.daisy.repository.commonimpl.variant.VariantStrategy;
import org.outerj.daisy.repository.commonimpl.variant.BranchImpl;
import org.outerj.daisy.repository.commonimpl.variant.LanguageImpl;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryEventType;
import org.outerj.daisy.repository.clientimpl.infrastructure.AbstractRemoteStrategy;
import org.outerj.daisy.repository.clientimpl.infrastructure.DaisyHttpClient;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.outerx.daisy.x10.BranchDocument;
import org.outerx.daisy.x10.BranchesDocument;
import org.outerx.daisy.x10.LanguageDocument;
import org.outerx.daisy.x10.LanguagesDocument;

import java.util.List;

public class RemoteVariantStrategy extends AbstractRemoteStrategy implements VariantStrategy {
    public RemoteVariantStrategy(RemoteRepositoryManager.Context context) {
        super(context);
    }

    public BranchImpl getBranch(long id, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new GetMethod("/repository/branch/" + id);

        BranchDocument branchDocument = (BranchDocument)httpClient.executeMethod(method, BranchDocument.class, true);
        BranchDocument.Branch branchXml = branchDocument.getBranch();
        BranchImpl branch = instantiateBranchFromXml(branchXml, user);
        return branch;
    }

    private BranchImpl instantiateBranchFromXml(BranchDocument.Branch branchXml, AuthenticatedUser user) {
        BranchImpl branch = new BranchImpl(this, branchXml.getName(), user);
        branch.setDescription(branchXml.getDescription());
        BranchImpl.IntimateAccess branchInt = branch.getIntimateAccess(this);
        branchInt.setLastModified(branchXml.getLastModified().getTime());
        branchInt.setLastModifier(branchXml.getLastModifier());
        branchInt.setUpdateCount(branchXml.getUpdateCount());
        branchInt.setId(branchXml.getId());
        return branch;
    }

    public BranchImpl getBranchByName(String name, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        String encodedName = encodeNameForUseInPath("branch", name);
        HttpMethod method = new GetMethod("/repository/branchByName/" + encodedName);

        BranchDocument branchDocument = (BranchDocument)httpClient.executeMethod(method, BranchDocument.class, true);
        BranchDocument.Branch branchXml = branchDocument.getBranch();
        BranchImpl branch = instantiateBranchFromXml(branchXml, user);
        return branch;
    }

    public BranchImpl[] getAllBranches(AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new GetMethod("/repository/branch");

        BranchesDocument branchesDocument = (BranchesDocument)httpClient.executeMethod(method, BranchesDocument.class, true);
        List<BranchDocument.Branch> branchesXml = branchesDocument.getBranches().getBranchList();

        BranchImpl[] branches = new BranchImpl[branchesXml.size()];
        for (int i = 0; i < branchesXml.size(); i++) {
            branches[i] = instantiateBranchFromXml(branchesXml.get(i), user);
        }
        return branches;
    }

    public void storeBranch(BranchImpl branch) throws RepositoryException {
        BranchImpl.IntimateAccess branchInt = branch.getIntimateAccess(this);
        DaisyHttpClient httpClient = getClient(branchInt.getCurrentUser());

        String url = "/repository";
        boolean isNew = branch.getId() == -1;
        if (isNew)
            url += "/branch";
        else
            url += "/branch/" + branch.getId();

        PostMethod method = new PostMethod(url);
        method.setRequestEntity(new InputStreamRequestEntity(branch.getXml().newInputStream()));

        BranchDocument branchDocument = (BranchDocument)httpClient.executeMethod(method, BranchDocument.class, true);
        BranchDocument.Branch branchXml = branchDocument.getBranch();
        branchInt.setId(branchXml.getId());
        branchInt.setLastModified(branchXml.getLastModified().getTime());
        branchInt.setLastModifier(branchXml.getLastModifier());
        branchInt.setUpdateCount(branchXml.getUpdateCount());

        if (isNew)
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.BRANCH_CREATED, new Long(branch.getId()), branch.getUpdateCount());
        else
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.BRANCH_UPDATED, new Long(branch.getId()), branch.getUpdateCount());
    }

    public void deleteBranch(long id, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        String url = "/repository/branch/" + id;
        DeleteMethod method = new DeleteMethod(url);
        httpClient.executeMethod(method, null, true);
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.BRANCH_DELETED, new Long(id), -1);
    }

    public LanguageImpl getLanguage(long id, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new GetMethod("/repository/language/" + id);

        LanguageDocument languageDocument = (LanguageDocument)httpClient.executeMethod(method, LanguageDocument.class, true);
        LanguageDocument.Language languageXml = languageDocument.getLanguage();
        LanguageImpl language = instantiateLanguageFromXml(languageXml, user);
        return language;
    }

    private LanguageImpl instantiateLanguageFromXml(LanguageDocument.Language languageXml, AuthenticatedUser user) {
        LanguageImpl language = new LanguageImpl(this, languageXml.getName(), user);
        language.setDescription(languageXml.getDescription());
        LanguageImpl.IntimateAccess languageInt = language.getIntimateAccess(this);
        languageInt.setLastModified(languageXml.getLastModified().getTime());
        languageInt.setLastModifier(languageXml.getLastModifier());
        languageInt.setUpdateCount(languageXml.getUpdateCount());
        languageInt.setId(languageXml.getId());
        return language;
    }

    public LanguageImpl getLanguageByName(String name, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        String encodedName = encodeNameForUseInPath("language", name);
        HttpMethod method = new GetMethod("/repository/languageByName/" + encodedName);

        LanguageDocument languageDocument = (LanguageDocument)httpClient.executeMethod(method, LanguageDocument.class, true);
        LanguageDocument.Language languageXml = languageDocument.getLanguage();
        LanguageImpl language = instantiateLanguageFromXml(languageXml, user);
        return language;
    }

    public LanguageImpl[] getAllLanguages(AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new GetMethod("/repository/language");

        LanguagesDocument languagesDocument = (LanguagesDocument)httpClient.executeMethod(method, LanguagesDocument.class, true);
        List<LanguageDocument.Language> languagesXml = languagesDocument.getLanguages().getLanguageList();

        LanguageImpl[] languages = new LanguageImpl[languagesXml.size()];
        for (int i = 0; i < languagesXml.size(); i++) {
            languages[i] = instantiateLanguageFromXml(languagesXml.get(i), user);
        }
        return languages;
    }

    public void storeLanguage(LanguageImpl language) throws RepositoryException {
        LanguageImpl.IntimateAccess languageInt = language.getIntimateAccess(this);
        DaisyHttpClient httpClient = getClient(languageInt.getCurrentUser());

        String url = "/repository";
        boolean isNew = language.getId() == -1;
        if (isNew)
            url += "/language";
        else
            url += "/language/" + language.getId();

        PostMethod method = new PostMethod(url);
        method.setRequestEntity(new InputStreamRequestEntity(language.getXml().newInputStream()));

        LanguageDocument languageDocument = (LanguageDocument)httpClient.executeMethod(method, LanguageDocument.class, true);
        LanguageDocument.Language languageXml = languageDocument.getLanguage();
        languageInt.setId(languageXml.getId());
        languageInt.setLastModified(languageXml.getLastModified().getTime());
        languageInt.setLastModifier(languageXml.getLastModifier());
        languageInt.setUpdateCount(languageXml.getUpdateCount());

        if (isNew)
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.LANGUAGE_CREATED, new Long(language.getId()), language.getUpdateCount());
        else
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.LANGUAGE_UPDATED, new Long(language.getId()), language.getUpdateCount());
    }

    public void deleteLanguage(long id, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        String url = "/repository/language/" + id;
        DeleteMethod method = new DeleteMethod(url);
        httpClient.executeMethod(method, null, true);
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.LANGUAGE_DELETED, new Long(id), -1);
    }
}
