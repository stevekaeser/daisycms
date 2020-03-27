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
package org.outerj.daisy.repository.commonimpl;

import org.outerj.daisy.repository.AvailableVariant;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.commonimpl.variant.CommonVariantManager;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerx.daisy.x10.AvailableVariantDocument;

public class AvailableVariantImpl implements AvailableVariant {
    private final long branchId;
    private final long languageId;
    private final boolean retired;
    private final long liveVersionId;
    private final long lastVersionId;
    private final CommonVariantManager variantManager;
    private final AuthenticatedUser user;

    public AvailableVariantImpl(long branchId, long languageId, boolean retired, long liveVersionId, long lastVersionId, CommonVariantManager variantManager, AuthenticatedUser user) {
        this.branchId = branchId;
        this.languageId = languageId;
        this.retired = retired;
        this.liveVersionId = liveVersionId;
        this.lastVersionId = lastVersionId;
        this.variantManager = variantManager;
        this.user = user;
    }

    public Branch getBranch() throws RepositoryException {
        return variantManager.getBranch(branchId, false, user);
    }

    public Language getLanguage() throws RepositoryException {
        return variantManager.getLanguage(languageId, false, user);
    }

    public long getBranchId() {
        return branchId;
    }

    public long getLanguageId() {
        return languageId;
    }

    public boolean isRetired() {
        return retired;
    }

    public long getLiveVersionId() {
        return liveVersionId;
    }

    public long getLastVersionId() {
        return lastVersionId;
    }

    public AvailableVariantDocument getXml() {
        try {
            return getXml(false);
        } catch (RepositoryException e) {
            // getXml only throws an exception if includeVariantNames is true
            throw new RuntimeException(e);
        }
    }

    public AvailableVariantDocument getXml(boolean includeVariantNames) throws RepositoryException {
        AvailableVariantDocument document = AvailableVariantDocument.Factory.newInstance();
        AvailableVariantDocument.AvailableVariant availableVariantXml = document.addNewAvailableVariant();
        availableVariantXml.setBranchId(branchId);
        availableVariantXml.setLanguageId(languageId);
        if (includeVariantNames) {
            availableVariantXml.setBranchName(getBranch().getName());
            availableVariantXml.setLanguageName(getLanguage().getName());
        }
        availableVariantXml.setRetired(retired);
        availableVariantXml.setLiveVersionId(liveVersionId);
        availableVariantXml.setLastVersionId(lastVersionId);
        return document;
    }
}
