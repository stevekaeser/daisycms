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

import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.commonimpl.namespace.NamespaceImpl;
import org.outerj.daisy.repository.namespace.NamespaceManager;
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.util.HttpConstants;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10.NamespaceDocument;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class NamespaceHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/namespace/*";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        long id = HttpUtil.parseId("namespace", (String)matchMap.get("1"));

        NamespaceManager nsManager = repository.getNamespaceManager();

        if (request.getMethod().equals(HttpConstants.GET)) {
            Namespace namespace = nsManager.getNamespace(id);
            namespace.getXml().save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.POST)) {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            org.outerx.daisy.x10.NamespaceDocument.Namespace nsXml = NamespaceDocument.Factory.parse(request.getInputStream(), xmlOptions).getNamespace();
            
            Namespace namespace = new NamespaceImpl(
                    id,
                    nsXml.getName(),
                    nsXml.getFingerprint(),
                    nsXml.getRegisteredBy(),
                    nsXml.getRegisteredOn().getTime(),
                    nsXml.getDocumentCount(),
                    nsXml.getIsManaged()
                    );
            
            namespace = nsManager.updateNamespace(namespace);
            namespace.getXml().save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.DELETE)) {
            Namespace namespace = nsManager.unregisterNamespace(id);
            namespace.getXml().save(response.getOutputStream());
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }
}


