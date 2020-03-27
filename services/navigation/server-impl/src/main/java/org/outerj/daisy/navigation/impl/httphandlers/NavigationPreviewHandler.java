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
package org.outerj.daisy.navigation.impl.httphandlers;

import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.LocaleHelper;
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.outerj.daisy.navigation.NavigationManager;
import org.xml.sax.ContentHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Locale;
import java.io.ByteArrayOutputStream;

public class NavigationPreviewHandler extends AbstractNavigationRequestHandler {
    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        String navigationXml = HttpUtil.getStringParam(request, "navigationXml");
        long branchId = HttpUtil.getBranchId(request, repository);
        long languageId = HttpUtil.getLanguageId(request, repository);
        Locale locale = Locale.getDefault();
        if (request.getParameter("locale") != null)
            locale = LocaleHelper.parseLocale(request.getParameter("locale"));

        ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
        ContentHandler serializer = new XmlSerializer(bos);
        NavigationManager navigationManager = (NavigationManager)repository.getExtension("NavigationManager");
        navigationManager.generatePreviewNavigationTree(serializer, navigationXml, branchId, languageId, locale);

        bos.writeTo(response.getOutputStream());
    }

    public String getPathPattern() {
        return "/preview";
    }
}
