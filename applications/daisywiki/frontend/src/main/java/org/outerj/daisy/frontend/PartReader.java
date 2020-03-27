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
package org.outerj.daisy.frontend;

import org.apache.cocoon.reading.Reader;
import org.apache.cocoon.servlet.multipart.Part;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.activity.Disposable;
import org.xml.sax.SAXException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.frontend.util.ResponseUtil;
import org.outerj.daisy.publisher.Publisher;
import org.outerj.daisy.publisher.BlobInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Map;

public class PartReader implements LogEnabled, Reader, Disposable {
    private OutputStream outputStream;
    private Response response;
    private BlobInfo blobInfo;
    private String filename;
    private String disposition;
    private Logger logger;
    private static final int BUFFER_SIZE = 32768;

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    public void dispose() {
        if (this.blobInfo != null)
            blobInfo.dispose();
    }

    public void generate() throws IOException, SAXException, ProcessingException {
        response.setIntHeader("Content-Length", (int)blobInfo.getSize());

        if (!StringUtils.isEmpty(disposition)) {
            ResponseUtil.addContentDispositionHeader(response, disposition, filename != null && !filename.equals("") ? filename : blobInfo.getFilename());
        }
        
        InputStream is = null;
        try {
            is = blobInfo.getInputStream();
            byte[] data = new byte[BUFFER_SIZE];
            int read;
            do {
                read = is.read(data);
                if (read != -1) {
                    outputStream.write(data, 0, read);
                }
            } while (read != -1);
        } catch (RepositoryException e) {
            throw new ProcessingException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Throwable e) {
                    logger.error("Error closing input stream in PartReader.", e);
                }
            }
        }
    }

    public long getLastModified() {
        return blobInfo.getLastModified().getTime();
    }

    public void setup(SourceResolver sourceResolver, Map objectModel, String s, Parameters parameters) throws ProcessingException, SAXException, IOException {
        try {
            Request request = ObjectModelHelper.getRequest(objectModel);
            FrontEndContext frontEndContext = FrontEndContext.get(request);

            String documentId = parameters.getParameter("documentId");
            String version = parameters.getParameter("version");
            if (version.equalsIgnoreCase("default")) {
                version = frontEndContext.getVersionMode().toString();
            }
            String partType = parameters.getParameter("partType");
            String branch = parameters.getParameter("branch");
            String language = parameters.getParameter("language");
            long defaultBranchId = parameters.getParameterAsLong("defaultBranchId");
            long defaultLanguageId = parameters.getParameterAsLong("defaultLanguageId");
            filename = parameters.getParameter("filename");
            disposition = parameters.getParameter("disposition", null);

            Repository repository = frontEndContext.getRepository();
            long branchId = RequestUtil.getBranchId(branch, defaultBranchId, repository);
            long languageId = RequestUtil.getLanguageId(language, defaultLanguageId, repository);

            Publisher publisher = (Publisher)repository.getExtension("Publisher");
            this.blobInfo = publisher.getBlobInfo(new VariantKey(documentId, branchId, languageId), version, partType);

            this.response = ObjectModelHelper.getResponse(objectModel);
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

    public void setOutputStream(OutputStream outputStream) throws IOException {
        this.outputStream = outputStream;
    }

    public String getMimeType() {
        return blobInfo.getMimeType();
    }

    public boolean shouldSetContentLength() {
        return false;
    }
}
