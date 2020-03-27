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

import org.outerj.daisy.repository.schema.ListItem;
import org.outerj.daisy.repository.VariantKey;

import java.util.Locale;
import java.util.List;
import java.util.Collections;

public class LinkListItem implements ListItem {
    private final VariantKey variantKey;
    private final String documentName;
    private final List<LinkListItem> children;

    protected LinkListItem(final VariantKey variantKey, final String documentName) {
        this.variantKey = variantKey;
        this.documentName = documentName;
        this.children = null;
    }

    protected LinkListItem(final VariantKey variantKey, final String documentName, List<LinkListItem> children) {
        this.variantKey = variantKey;
        this.documentName = documentName;
        this.children = children != null ? Collections.unmodifiableList(children) : null;
    }

    public Object getValue() {
        return variantKey;
    }

    public String getLabel(Locale locale) {
        return documentName;
    }

    public List<? extends ListItem> getItems() {
        if (children == null)
            return Collections.emptyList();
        else
            return children;
    }
}
