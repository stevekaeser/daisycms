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

import org.outerj.daisy.repository.variant.Languages;
import org.outerj.daisy.repository.variant.Language;
import org.outerx.daisy.x10.LanguagesDocument;
import org.outerx.daisy.x10.LanguageDocument;

public class LanguagesImpl implements Languages {
    private Language[] languages;

    public LanguagesImpl(Language[] languages) {
        this.languages = languages;
    }

    public Language[] getArray() {
        return languages;
    }

    public LanguagesDocument getXml() {
        LanguagesDocument languagesDocument = LanguagesDocument.Factory.newInstance();
        LanguageDocument.Language[] languagesXml = new LanguageDocument.Language[languages.length];

        for (int i = 0; i < languages.length; i++) {
            languagesXml[i] = languages[i].getXml().getLanguage();
        }

        languagesDocument.addNewLanguages().setLanguageArray(languagesXml);

        return languagesDocument;
    }

    public int size() {
        return languages.length;
    }
}
