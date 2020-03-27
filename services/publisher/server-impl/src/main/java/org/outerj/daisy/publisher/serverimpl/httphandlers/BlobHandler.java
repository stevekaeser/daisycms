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
package org.outerj.daisy.publisher.serverimpl.httphandlers;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.publisher.BlobInfo;
import org.outerj.daisy.publisher.Publisher;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.util.HttpConstants;

public class BlobHandler extends AbstractPublisherRequestHandler {
    private static final int BUFFER_SIZE = 32768;

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        if (request.getMethod().equals(HttpConstants.GET)) {
            String documentId = HttpUtil.getStringParam(request, "documentId");
            long branchId = HttpUtil.getBranchId(request, repository);
            long languageId = HttpUtil.getLanguageId(request, repository);
            VariantKey variantKey = new VariantKey(documentId, branchId, languageId);
            String version = HttpUtil.getStringParam(request, "version");
            String partType = HttpUtil.getStringParam(request, "partType");

            Publisher publisher = (Publisher)repository.getExtension("Publisher");
            BlobInfo blobInfo = publisher.getBlobInfo(variantKey, version, partType);

            response.setDateHeader("Last-Modified", blobInfo.getLastModified().getTime());
            response.setContentType(blobInfo.getMimeType());
            response.setHeader("Content-Length", String.valueOf(blobInfo.getSize()));
            response.setHeader("X-Daisy-Filename", blobInfo.getFilename());

            OutputStream os = response.getOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            InputStream is = blobInfo.getInputStream();
            try {
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
            } finally {
                is.close();
            }
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }

    public String getPathPattern() {
        return "/blob";
    }
}
