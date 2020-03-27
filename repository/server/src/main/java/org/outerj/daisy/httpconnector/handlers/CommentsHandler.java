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
import org.outerj.daisy.repository.comment.Comment;
import org.outerj.daisy.repository.comment.CommentVisibility;
import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.util.HttpConstants;
import org.outerx.daisy.x10.CommentDocument;
import org.apache.xmlbeans.XmlOptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class CommentsHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/document/*/comment";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        String id = (String)matchMap.get("1");

        if (request.getMethod().equals(HttpConstants.GET)) {
            long branchId = HttpUtil.getBranchId(request, repository);
            long languageId = HttpUtil.getLanguageId(request, repository);
            response.setContentType("text/xml");
            repository.getCommentManager().getComments(id, branchId, languageId).getXml().save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.POST)) {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            CommentDocument commentDocument = CommentDocument.Factory.parse(request.getInputStream(), xmlOptions);
            CommentDocument.Comment commentXml = commentDocument.getComment();
            Comment comment = repository.getCommentManager().addComment(id, commentXml.getBranchId(), commentXml.getLanguageId(), CommentVisibility.fromString(commentXml.getVisibility().toString()), commentXml.getContent());

            comment.getXml().save(response.getOutputStream());
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }
}
