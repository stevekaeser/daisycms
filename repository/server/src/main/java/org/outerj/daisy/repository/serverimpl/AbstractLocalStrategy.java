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
package org.outerj.daisy.repository.serverimpl;

import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.apache.commons.logging.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public abstract class AbstractLocalStrategy {
    protected Log logger;
    protected AuthenticatedUser systemUser;
    protected LocalRepositoryManager.Context context;
    protected JdbcHelper jdbcHelper;
    protected EventHelper eventHelper;
    protected LabelUtil labelUtil;

    public AbstractLocalStrategy(LocalRepositoryManager.Context context, AuthenticatedUser systemUser, JdbcHelper jdbcHelper) {
        this.context = context;
        this.logger = context.getLogger();
        this.systemUser = systemUser;
        this.jdbcHelper = jdbcHelper;
        this.eventHelper = new EventHelper(context, jdbcHelper);
        this.labelUtil = new LabelUtil(context.getCommonRepository(), systemUser);
    }

    public String getFormattedVariant(VariantKey variantKey) {
        return "document " + variantKey.getDocumentId() + ", branch " + getBranchLabel(variantKey.getBranchId()) + ", language " + getLanguageLabel(variantKey.getLanguageId());
    }

    public String getFormattedVariant(VariantKey variantKey, long versionId) {
        return labelUtil.getFormattedVariant(variantKey, versionId);
    }

    public String getBranchLabel(long branchId) {
        return labelUtil.getBranchLabel(branchId);
    }

    public String getLanguageLabel(long languageId) {
        return labelUtil.getLanguageLabel(languageId);
    }

    public String getBranchName(long branchId) {
        return labelUtil.getBranchName(branchId);
    }

    public String getLanguageName(long languageId) {
        return labelUtil.getLanguageName(languageId);
    }

    protected Calendar getCalendar(Date date) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        return calendar;
    }
}
