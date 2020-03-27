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
import org.outerj.daisy.repository.ConcurrentUpdateException;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.util.HttpConstants;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.outerx.daisy.x10.BranchDocument;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class BranchHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/branch/*";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        long id = HttpUtil.parseId("branch", (String)matchMap.get("1"));

        VariantManager variantManager = repository.getVariantManager();

        if (request.getMethod().equals(HttpConstants.GET)) {
            Branch branch = variantManager.getBranch(id, true);
            XmlObject branchDocument = branch.getXml();
            branchDocument.save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.POST)) {
            // A POST updates the Branch
            Branch branch = variantManager.getBranch(id, true);
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            BranchDocument branchDocument = BranchDocument.Factory.parse(request.getInputStream(), xmlOptions);
            BranchDocument.Branch branchXml = branchDocument.getBranch();
            if (branch.getUpdateCount() != branchXml.getUpdateCount())
                throw new ConcurrentUpdateException(Branch.class.getName(), String.valueOf(branch.getId()));
            branch.setAllFromXml(branchXml);
            branch.save();

            branchDocument = branch.getXml();
            branchDocument.save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.DELETE)) {
            variantManager.deleteBranch(id);
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }
}
