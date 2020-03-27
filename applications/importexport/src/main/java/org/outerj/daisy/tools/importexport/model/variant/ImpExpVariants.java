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
package org.outerj.daisy.tools.importexport.model.variant;

import java.util.List;
import java.util.ArrayList;

public class ImpExpVariants {
    private List<ImpExpVariant> branches = new ArrayList<ImpExpVariant>();
    private List<ImpExpVariant> languages = new ArrayList<ImpExpVariant>();

    public void addBranch(String name, String description, boolean required) {
        addVariant(name, description, required, branches);
    }

    public void addLanguage(String name, String description, boolean required) {
        addVariant(name, description, required, languages);
    }

    public ImpExpVariant[] getBranches() {
        return branches.toArray(new ImpExpVariant[0]);
    }

    public ImpExpVariant[] getLanguages() {
        return languages.toArray(new ImpExpVariant[0]);
    }

    private void addVariant(String name, String description, boolean required, List<ImpExpVariant> variants) {
        if (name == null || name.trim().length() == 0)
            throw new IllegalArgumentException("Null or empty argument: name");

        // check if variant is already present, upgrade required setting if needed
        // (assume description stays the same)
        for (ImpExpVariant variant : variants) {
            if (variant.getName().equals(name)) {
                if (required && !variant.isRequired())
                    variant.setRequired(true);
                return;
            }
        }

        // not yet present, add
        variants.add(new ImpExpVariant(name, description, required));
    }
}
