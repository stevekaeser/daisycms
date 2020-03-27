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
/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Apache Cocoon" and  "Apache Software Foundation" must  not  be
    used to  endorse or promote  products derived from  this software without
    prior written permission. For written permission, please contact
    apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation and was  originally created by
 Stefano Mazzocchi  <stefano@apache.org>. For more  information on the Apache
 Software Foundation, please see <http://www.apache.org/>.

*/
package org.outerj.daisy.util;

import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Map using Locale objects as keys.
 *
 * <b>This class is based on code from Apache Cocoon.</b>
 *
 * <p>This map should be filled once using calls to {@link #put}, before any calls
 * are made to {@link #get}.
 *
 */
public class LocaleMap {
    /** Contains the original values the have been put in the map. */
    protected ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<String, Object>();
    /** Contains 'resolved' locales for quick lookup. */
    protected ConcurrentHashMap<String, Object> searchMap = new ConcurrentHashMap<String, Object>();
    private static final String NO_RESULT = "(no result: you should never see this)";

    /**
     * Gets an object based on the given locale. An automatic fallback mechanism is used:
     * if nothing is found for language-COUNTRY-variant, then language-COUNTRY is searched,
     * the language, and finally "" (empty string). If nothing is found null is returned.
     */
    public Object get(Locale locale) {
        if (map.size() == 0)
            return null;

        String full = getFullKey(locale);

        if (!searchMap.containsKey(full)) {
            if (map.containsKey(full)) {
                Object object = map.get(full);
                searchMap.put(full, object);
                return object;
            }

            String altKey = locale.getLanguage() + '-' + locale.getCountry();
            Object object = map.get(altKey);
            if (object != null) {
                searchMap.put(full, object);
                return object;
            }

            altKey = locale.getLanguage();
            object = map.get(altKey);
            if (object != null) {
                searchMap.put(full, object);
                return object;
            }

            object = map.get("");
            if (object != null) {
                searchMap.put(full, object);
                return object;
            }

            searchMap.put(full, NO_RESULT);
        }

        Object result = searchMap.get(full);
        return result == NO_RESULT ? null : result;
    }

    public Object getExact(Locale locale) {
        return map.get(getString(locale));
    }

    public void clear() {
        map.clear();
        searchMap.clear();
    }

    public void remove(Locale locale) {
        put(locale, null);
    }

    public Locale[] getLocales() {
        String[] localeNames = map.keySet().toArray(new String[0]);
        Locale[] locales = new Locale[localeNames.length];
        for (int i = 0; i < locales.length; i++)
            locales[i] = parseLocale(localeNames[i]);
        return locales;
    }

    public Set entrySet() {
        return map.entrySet();
    }

    private String getFullKey(Locale locale) {
        return locale.getLanguage() + '-' + locale.getCountry() + '-' + locale.getVariant();
    }

    public void put(Locale locale, Object object) {
        if (object == null)
            map.remove(getString(locale));
        else
            map.put(getString(locale), object);
        searchMap.clear();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof LocaleMap))
            return false;

        LocaleMap other = (LocaleMap)obj;
        // map can do equals
        return map.equals(other.map);
    }

    public static Locale parseLocale(String localeString) {
        StringTokenizer localeParser = new StringTokenizer(localeString, "-_");

        String lang = null, country = null, variant = null;

        if (localeParser.hasMoreTokens())
            lang = localeParser.nextToken();
        if (localeParser.hasMoreTokens())
            country = localeParser.nextToken();
        if (localeParser.hasMoreTokens())
            variant = localeParser.nextToken();

        if (lang != null && country != null && variant != null)
            return new Locale(lang, country, variant);
        else if (lang != null && country != null)
            return new Locale(lang, country);
        else if (lang != null)
            return new Locale(lang);
        else
            return new Locale("");
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
}
