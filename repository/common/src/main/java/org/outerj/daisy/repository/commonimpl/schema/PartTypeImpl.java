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
package org.outerj.daisy.repository.commonimpl.schema;

import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.Util;
import org.outerj.daisy.util.LocaleMap;
import org.outerx.daisy.x10.PartTypeDocument;

import java.util.*;

// IMPORTANT:
//  When adding/changing properties to a part type, or any of the objects used by it
//  be sure to update the equals method if needed

public class PartTypeImpl implements PartType {
    private long id = -1;
    private String name;
    private SchemaLocaleMap label = new SchemaLocaleMap();
    private long labelId = -1;
    private SchemaLocaleMap description = new SchemaLocaleMap();
    private long descriptionId = -1;
    private String mimeTypes;
    private boolean daisyHtml = false;
    private String linkExtractor;
    private SchemaStrategy schemaStrategy;
    private Date lastModified;
    private long lastModifier=-1;
    private AuthenticatedUser currentModifier;
    private boolean deprecated = false;
    private boolean readOnly = false;
    private long updateCount = 0;
    private IntimateAccess intimateAccess = new IntimateAccess();
    private static final String READ_ONLY_MESSAGE = "This PartType is read-only.";

    public PartTypeImpl(String name, String mimeTypes, SchemaStrategy schemaStrategy, AuthenticatedUser user) {
        Util.checkName(name);
        this.name = name;
        setMimeTypes(mimeTypes);
        this.schemaStrategy = schemaStrategy;
        this.currentModifier = user;
    }

    public IntimateAccess getIntimateAccess(SchemaStrategy schemaStrategy) {
        if (this.schemaStrategy == schemaStrategy)
            return intimateAccess;
        else
            return null;
    }

    public void setAllFromXml(PartTypeDocument.PartType partTypeXml) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        this.name = partTypeXml.getName();
        setMimeTypes(partTypeXml.getMimeTypes());
        this.deprecated = partTypeXml.getDeprecated();
        this.label.clear();
        this.label.readFromLabelsXml(partTypeXml.getLabels());
        this.description.clear();
        this.description.readFromDescriptionsXml(partTypeXml.getDescriptions());
        this.daisyHtml = partTypeXml.getDaisyHtml();
        this.linkExtractor = partTypeXml.getLinkExtractor();
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        Util.checkName(name);
        this.name = name;
    }

    public String getLabel(Locale locale) {
        String result = (String)label.get(locale);
        return result == null ? getName() : result;
    }

    public String getLabelExact(Locale locale) {
        return (String)label.getExact(locale);
    }

    public void setLabel(Locale locale, String label) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        this.label.put(locale, label);
    }

    public void clearLabels() {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        label.clear();
    }

    public Locale[] getLabelLocales() {
        return label.getLocales();
    }

    public String getDescription(Locale locale) {
        return (String)description.get(locale);
    }

    public String getDescriptionExact(Locale locale) {
        return (String)description.getExact(locale);
    }

    public void setDescription(Locale locale, String description) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        this.description.put(locale, description);
    }

    public void clearDescriptions() {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        description.clear();
    }

    public Locale[] getDescriptionLocales() {
        return description.getLocales();
    }

    public String getMimeTypes() {
        return mimeTypes;
    }

    public void setMimeTypes(String mimeTypes) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (mimeTypes == null)
            throw new NullPointerException("mimeTypes cannot be null.");

        this.mimeTypes = mimeTypes;
    }

    public boolean mimeTypeAllowed(String mimeType) {
        if (this.mimeTypes.equals(""))
            return true;

        StringTokenizer mimeTypesTokenizer = new StringTokenizer(mimeTypes, ",");
        while (mimeTypesTokenizer.hasMoreTokens()) {
            if (mimeType.equals(mimeTypesTokenizer.nextToken()))
                return true;
        }
        return false;
    }

    public boolean isDaisyHtml() {
        return daisyHtml;
    }

    public void setDaisyHtml(boolean daisyHtml) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        this.daisyHtml = daisyHtml;
        if (daisyHtml) {
            // for backwards compatibility, set the link extractor to daisy-html
            this.linkExtractor = "daisy-html";
        }
    }

    public void setLinkExtractor(String name) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        this.linkExtractor = name;
    }

    public String getLinkExtractor() {
        return linkExtractor;
    }

    public Date getLastModified() {
        if (lastModified != null)
            return (Date)lastModified.clone();
        else
            return null;
    }

    public long getLastModifier() {
        return lastModifier;
    }

    public PartTypeDocument getXml() {
        PartTypeDocument partTypeDocument = PartTypeDocument.Factory.newInstance();
        PartTypeDocument.PartType partType = partTypeDocument.addNewPartType();
        partType.setName(name);
        partType.setMimeTypes(mimeTypes);
        partType.setDaisyHtml(daisyHtml);
        if (linkExtractor != null)
            partType.setLinkExtractor(linkExtractor);
        partType.setDeprecated(deprecated);
        partType.setLabels(label.getAsLabelsXml());
        partType.setDescriptions(description.getAsDescriptionsXml());
        partType.setUpdateCount(updateCount);

        if (id != -1) {
            partType.setId(id);
            GregorianCalendar lastModified = new GregorianCalendar();
            lastModified.setTime(this.lastModified);
            partType.setLastModified(lastModified);
            partType.setLastModifier(lastModifier);
        }

        return partTypeDocument;
    }

    public void save() throws RepositoryException {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        schemaStrategy.store(this);
    }

    public void setDeprecated(boolean deprecated) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        this.deprecated = deprecated;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public long getUpdateCount() {
        return updateCount;
    }

    /**
     * Disables all operations that can change the state of this PartType.
     */
    public void makeReadOnly() {
        this.readOnly = true;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof PartTypeImpl))
            return false;

        PartTypeImpl other = (PartTypeImpl)obj;

        if (daisyHtml != other.daisyHtml)
            return false;

        if (deprecated != other.deprecated)
            return false;

        if (mimeTypes == null ? other.mimeTypes != null : !mimeTypes.equals(other.mimeTypes))
            return false;

        if (linkExtractor == null ? other.linkExtractor != null : !linkExtractor.equals(other.linkExtractor))
            return false;

        if (!description.equals(other.description))
            return false;

        if (!label.equals(other.label))
            return false;

        return name.equals(other.name);
    }
    

    public class IntimateAccess {
        private IntimateAccess() {
        }

        public long getLabelId() {
            return labelId;
        }

        public void setLabelId(long labelId) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            PartTypeImpl.this.labelId = labelId;
        }

        public long getDescriptionId() {
            return descriptionId;
        }

        public void setDescriptionId(long descriptionId) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            PartTypeImpl.this.descriptionId = descriptionId;
        }

        public LocaleMap getLabels() {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            return label;
        }

        public LocaleMap getDescriptions() {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            return description;
        }

        public void setLastModified(Date lastModified) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            PartTypeImpl.this.lastModified = lastModified;
        }

        public void setLastModifier(long lastModifier) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            PartTypeImpl.this.lastModifier = lastModifier;
        }

        public AuthenticatedUser getCurrentModifier() {
            return currentModifier;
        }

        public void setId(long id) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            PartTypeImpl.this.id = id;
        }

        public void setUpdateCount(long updateCount) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            PartTypeImpl.this.updateCount = updateCount;
        }
    }
}
