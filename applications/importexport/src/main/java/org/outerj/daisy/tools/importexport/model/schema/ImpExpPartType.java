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
package org.outerj.daisy.tools.importexport.model.schema;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

public class ImpExpPartType implements ImpExpLabelEnabled, ImpExpDescriptionEnabled, Comparable {
    private String name;
    private String mimeTypes = "";
    private boolean daisyHtml = false;
    private String linkExtractor;
    private boolean deprecated;
    private Map<Locale, String> labels = new HashMap<Locale, String>();
    private Map<Locale, String> descriptions = new HashMap<Locale, String>();

    public ImpExpPartType(String name) {
        if (name == null || name.trim().length() == 0)
            throw new IllegalArgumentException("Empty or null argument: name");
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getMimeTypes() {
        return mimeTypes;
    }

    public void setMimeTypes(String mimeTypes) {
        if (mimeTypes == null)
            throw new IllegalArgumentException("Null argument: mimeTypes");
        this.mimeTypes = mimeTypes;
    }

    public boolean isDaisyHtml() {
        return daisyHtml;
    }

    public void setDaisyHtml(boolean daisyHtml) {
        this.daisyHtml = daisyHtml;
    }

    public String getLinkExtractor() {
        return linkExtractor;
    }

    public void setLinkExtractor(String linkExtractor) {
        this.linkExtractor = linkExtractor;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public void addLabel(Locale locale, String label) {
        labels.put(locale, label);
    }

    public void clearLabels() {
        labels.clear();
    }

    public Map<Locale, String> getLabels() {
        return new HashMap<Locale, String>(labels);
    }

    public void addDescription(Locale locale, String description) {
        descriptions.put(locale, description);
    }

    public void clearDescriptions() {
        descriptions.clear();
    }

    public Map<Locale, String> getDescriptions() {
        return new HashMap<Locale, String>(descriptions);
    }

    public int compareTo(Object o) {
        return name.compareTo(((ImpExpPartType)o).getName());
    }
}
