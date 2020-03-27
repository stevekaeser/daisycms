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
package org.outerj.daisy.repository.schema;

import java.util.Locale;

public interface DescriptionEnabled {
    String getDescription(Locale locale);

    String getDescriptionExact(Locale locale);

    /**
     *
     * @param description if null, the description for this locale will be removed.
     */
    void setDescription(Locale locale, String description);

    void clearDescriptions();

    /**
     * Returns the locales for which a description is set.
     */
    Locale[] getDescriptionLocales();
}
