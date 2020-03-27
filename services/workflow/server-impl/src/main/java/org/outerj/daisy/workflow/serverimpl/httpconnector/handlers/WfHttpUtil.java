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
package org.outerj.daisy.workflow.serverimpl.httpconnector.handlers;

import org.outerj.daisy.repository.LocaleHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

public class WfHttpUtil {
    public static Locale getLocale(HttpServletRequest request) {
        String locale = request.getParameter("locale");
        if (locale == null)
            return Locale.getDefault();
        else
            return LocaleHelper.parseLocale(locale);
    }
}
