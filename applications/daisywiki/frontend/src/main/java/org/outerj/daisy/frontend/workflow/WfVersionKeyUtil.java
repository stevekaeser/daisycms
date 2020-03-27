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
package org.outerj.daisy.frontend.workflow;

import org.outerj.daisy.workflow.WfVersionKey;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WfVersionKeyUtil {
    public static final Pattern VERSIONKEY_PATTERN = Pattern.compile("^daisy:([^@]*)(?:@([^:]*)(?::([^:]*)(?::([^:]*))?)?)?$");

    public static String versionKeyToString(WfVersionKey versionKey, Repository repository) {
        StringBuilder link = new StringBuilder(40);
        link.append("daisy:");
        link.append(versionKey.getDocumentId());
        link.append("@");

        VariantManager variantManager = repository.getVariantManager();
        String branchName;
        try {
            branchName = variantManager.getBranch(versionKey.getBranchId(), false).getName();
        } catch (RepositoryException e) {
            branchName = String.valueOf(versionKey.getBranchId());
        }
        link.append(branchName);

        String languageName;
        try {
            languageName = variantManager.getLanguage(versionKey.getLanguageId(), false).getName();
        } catch (RepositoryException e) {
            languageName = String.valueOf(versionKey.getLanguageId());
        }
        link.append(":").append(languageName);

        if (versionKey.getVersion() != null)
            link.append(":").append(versionKey.getVersion());

        return link.toString();
    }

    /**
     * Parses a version key, error handling is rough since it should already
     * have been validated by the widget validator.
     */
    public static WfVersionKey parseWfVersionKey(String link, Repository repository, SiteConf siteConf) {
        Matcher matcher = WfVersionKeyUtil.VERSIONKEY_PATTERN.matcher(link);
        if (matcher.matches()) {
            String documentId = matcher.group(1);
            String branch = matcher.group(2);
            String language = matcher.group(3);
            String version = matcher.group(4);

            documentId = repository.normalizeDocumentId(documentId);

            VariantManager variantManager = repository.getVariantManager();
            long branchId;
            if (branch != null) {
                try {
                    branchId = variantManager.getBranch(branch, false).getId();
                } catch (RepositoryException e) {
                    throw new RuntimeException("Error with branch in link " + link, e);
                }
            } else {
                branchId = siteConf.getBranchId();
            }

            long languageId;
            if (language != null) {
                try {
                    languageId = variantManager.getLanguage(language, false).getId();
                } catch (RepositoryException e) {
                    throw new RuntimeException("Error with language in link " + link, e);
                }
            } else {
                languageId = siteConf.getLanguageId();
            }

            return new WfVersionKey(documentId, branchId, languageId, version);
        } else {
            throw new RuntimeException("Invalid version link: " + link);
        }
    }
}
