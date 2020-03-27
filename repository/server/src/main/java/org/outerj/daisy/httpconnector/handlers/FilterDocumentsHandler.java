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
package org.outerj.daisy.httpconnector.handlers;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VariantKeys;
import org.outerj.daisy.repository.acl.AccessManager;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerx.daisy.x10.VariantKeysDocument;
import org.apache.xmlbeans.XmlOptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class FilterDocumentsHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/filterDocuments";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
        VariantKeysDocument variantKeysDocument = VariantKeysDocument.Factory.parse(request.getInputStream(), xmlOptions);
        VariantKey[] variantKeys = VariantKeys.fromXml(variantKeysDocument).getArray();

        // No permission parameter defaults to READ permission (for backwards compatibility)
        String permissionParam = request.getParameter("permission");
        AclPermission permission = AclPermission.READ;
        if (permissionParam != null)
            permission = AclPermission.fromString(permissionParam);

        // No "nonLive" param defaults to false (= only live versions)
        String nonLiveParam = request.getParameter("nonLive");
        boolean nonLive = "true".equals(nonLiveParam);

        AccessManager accessManager = repository.getAccessManager();
        VariantKey[] filteredVariantKeys = accessManager.filterDocuments(variantKeys, permission, nonLive);

        new VariantKeys(filteredVariantKeys).getXml().save(response.getOutputStream());
    }
}
