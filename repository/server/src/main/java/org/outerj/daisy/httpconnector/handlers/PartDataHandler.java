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

import org.outerj.daisy.repository.*;
import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.io.OutputStream;
import java.io.InputStream;

public class PartDataHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/document/*/version/*/part/*/data";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        String documentId = (String)matchMap.get("1");
        long branchId = HttpUtil.getBranchId(request, repository);
        long languageId = HttpUtil.getLanguageId(request, repository);

        String versionString = (String)matchMap.get("2");
        Document document = repository.getDocument(documentId, branchId, languageId, false);
        Version version;
        if (versionString.equalsIgnoreCase("last")) {
            version = document.getLastVersion();
        } else if (versionString.equalsIgnoreCase("live")) {
            version = document.getLiveVersion();
            if (version == null)
                throw new RepositoryException("Document " + document.getVariantKey() + " does not have a live version.");
        } else {
            long versionId = HttpUtil.parseId("version", versionString);
            version = document.getVersion(versionId);
        }

        String partType = (String)matchMap.get("3");
        Part part;
        if (!(partType.charAt(0) >= '0' && partType.charAt(0) <= '9')) {
            part = version.getPart(partType);
        } else {
            part = version.getPart(Long.parseLong(partType));
        }

        response.setDateHeader("Last-Modified", version.getCreated().getTime());
        response.setContentType(part.getMimeType());
        response.setHeader("Content-Length", String.valueOf(part.getSize()));

        OutputStream os = response.getOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        InputStream is = part.getDataStream();
        try {
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
        } finally {
            is.close();
        }
    }

    private static final int BUFFER_SIZE = 32768;
}
