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
package org.outerj.daisy.i18n.impl;

import org.outerj.daisy.i18n.DResourceBundles;
import org.outerj.daisy.i18n.DResourceBundle;
import org.outerj.daisy.i18n.I18nMessage;

import java.util.*;

/**
 * Manages a set of resource bundles with fallback between languages and different bundles.
 */
public class AggregateResourceBundle implements DResourceBundles {
    private String orderedBundleNames[];
    private Map<String, Map<String, DResourceBundle>> bundlesByName = new HashMap<String, Map<String, DResourceBundle>>();

    /**
     * @param names the names of the resource bundles, in the order in which they should be searched
     */
    public AggregateResourceBundle(String[] names) {
        if (names == null)
            throw new IllegalArgumentException("Null argument: names");

        this.orderedBundleNames = names;
    }

    public void addBundle(String name, Locale locale, DResourceBundle bundle) {
        Map<String, DResourceBundle> bundlesByLocale = bundlesByName.get(name);
        if (bundlesByLocale == null) {
            bundlesByLocale = new HashMap<String, DResourceBundle>();
            bundlesByName.put(name, bundlesByLocale);
        }

        bundlesByLocale.put(getString(locale), bundle);
    }

    public static String getString(Locale locale) {
        boolean hasLanguage = !locale.getLanguage().equals("");
        boolean hasCountry = !locale.getCountry().equals("");
        boolean hasVariant = !locale.getVariant().equals("");

        if (hasLanguage && hasCountry && hasVariant)
            return locale.getLanguage() + '-' + locale.getCountry() + '-' + locale.getVariant();
        else if (hasLanguage && hasCountry)
            return locale.getLanguage() + '-' + locale.getCountry();
        else if (hasLanguage)
            return locale.getLanguage();
        else
            return "";
    }

    public I18nMessage get(Locale locale, String key) {
        for (String name : orderedBundleNames) {
            Map<String, DResourceBundle> bundlesByLocale = bundlesByName.get(name);
            if (bundlesByLocale == null)
                continue;

            String[] localeNames = getSearchLocaleNames(locale);
            for (String localeName : localeNames) {
                DResourceBundle bundle = bundlesByLocale.get(localeName);
                if (bundle != null) {
                    I18nMessage message = bundle.get(key);
                    if (message != null)
                        return message;
                }
            }
        }

        return null;
    }

    private String[] getSearchLocaleNames(Locale locale) {
        boolean hasLanguage = !locale.getLanguage().equals("");
        boolean hasCountry = !locale.getCountry().equals("");
        boolean hasVariant = !locale.getVariant().equals("");

        List<String> localeNames = new ArrayList<String>(4);
        if (hasLanguage && hasCountry && hasVariant)
            localeNames.add(locale.getLanguage() + '-' + locale.getCountry() + '-' + locale.getVariant());

        if (hasLanguage && hasCountry)
            localeNames.add(locale.getLanguage() + '-' + locale.getCountry());

        if (hasLanguage)
            localeNames.add(locale.getLanguage());

        localeNames.add("");

        return localeNames.toArray(new String[0]);
    }
}
