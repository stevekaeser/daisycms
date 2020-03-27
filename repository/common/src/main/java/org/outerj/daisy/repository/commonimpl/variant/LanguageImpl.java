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
package org.outerj.daisy.repository.commonimpl.variant;

import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.Util;
import org.outerj.daisy.repository.RepositoryException;
import org.outerx.daisy.x10.LanguageDocument;

import java.util.Date;
import java.util.GregorianCalendar;

public class LanguageImpl implements Language {
    private long id = -1;
    private String name;
    private String description;
    private boolean readOnly = false;
    private long lastModifier;
    private Date lastModified;
    private long updateCount = 0;
    private AuthenticatedUser currentUser;
    private VariantStrategy strategy;
    private IntimateAccess intimateAccess = new IntimateAccess();
    private static final String READ_ONLY_MESSAGE = "This Language object is read-only.";

    public LanguageImpl(VariantStrategy strategy, String name, AuthenticatedUser currentUser) {
        Util.checkName(name);
        this.strategy = strategy;
        this.name = name;
        this.currentUser = currentUser;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        Util.checkName(name);
        this.name = name;
    }

    public void setDescription(String description) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        this.description = description;
    }

    public void save() throws RepositoryException {
        strategy.storeLanguage(this);
    }

    public long getLastModifier() {
        return lastModifier;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public long getUpdateCount() {
        return updateCount;
    }

    public LanguageDocument getXml() {
        LanguageDocument languageDocument = LanguageDocument.Factory.newInstance();
        LanguageDocument.Language language = languageDocument.addNewLanguage();

        language.setName(name);
        if (description != null)
            language.setDescription(description);

        if (id != -1) {
            language.setId(id);

            GregorianCalendar lastModifiedCalendar = new GregorianCalendar();
            lastModifiedCalendar.setTime(lastModified);
            language.setLastModified(lastModifiedCalendar);

            language.setLastModifier(lastModifier);
            language.setUpdateCount(updateCount);
        }

        return languageDocument;
    }

    public void setAllFromXml(LanguageDocument.Language languageXml) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        setName(languageXml.getName());
        setDescription(languageXml.getDescription());
    }

    public void makeReadOnly() {
        this.readOnly = true;
    }

    public IntimateAccess getIntimateAccess(VariantStrategy strategy) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (this.strategy == strategy)
            return intimateAccess;
        else
            return null;
    }

    public class IntimateAccess {
        private IntimateAccess() {
        }

        public AuthenticatedUser getCurrentUser() {
            return currentUser;
        }

        public void setId(long id) {
            LanguageImpl.this.id = id;
        }

        public void setLastModified(Date lastModified) {
            LanguageImpl.this.lastModified = lastModified;
        }

        public void setLastModifier(long lastModifier) {
            LanguageImpl.this.lastModifier = lastModifier;
        }

        public void setUpdateCount(long updateCount) {
            LanguageImpl.this.updateCount = updateCount;
        }
    }
}
