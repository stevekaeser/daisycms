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
package org.outerj.daisy.repository.serverimpl;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.commonimpl.CommonRepository;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;

public class LabelUtil {
    private CommonRepository repository;
    private AuthenticatedUser user;

    public LabelUtil(CommonRepository repository, AuthenticatedUser user) {
        this.repository = repository;
        this.user = user;
    }

    public String getFormattedVariant(VariantKey variantKey) {
        return "document " + variantKey.getDocumentId() + ", branch " + getBranchLabel(variantKey.getBranchId()) + ", language " + getLanguageLabel(variantKey.getLanguageId());
    }

    public String getFormattedVariant(VariantKey variantKey, long versionId) {
        return getFormattedVariant(variantKey) + ", version " + versionId;
    }

    public String getBranchLabel(long branchId) {
        try {
            Branch branch = repository.getVariantManager().getBranch(branchId, false, user);
            return branch.getName() + " (ID " + branchId + ")";
        } catch (Throwable e) {
            return String.valueOf(branchId);
        }
    }

    public String getLanguageLabel(long languageId) {
        try {
            Language language = repository.getVariantManager().getLanguage(languageId, false, user);
            return language.getName() + " (ID " + languageId + ")";
        } catch (Throwable e) {
            return String.valueOf(languageId);
        }
    }

    public String getUserLabel(AuthenticatedUser user) {
        return user.getLogin() + " (ID " + user.getId() + ")";
    }

    public String getBranchName(long branchId) {
        try {
            Branch branch = repository.getVariantManager().getBranch(branchId, false, user);
            return branch.getName();
        } catch (Throwable e) {
            return null;
        }
    }

    public String getLanguageName(long languageId) {
        try {
            Language language = repository.getVariantManager().getLanguage(languageId, false, user);
            return language.getName();
        } catch (Throwable e) {
            return null;
        }
    }
}
