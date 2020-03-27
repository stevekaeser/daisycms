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
package org.outerj.daisy.frontend.components.reading;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.components.modules.input.InputModuleHelper;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.forms.formmodel.Upload;
import org.apache.cocoon.reading.Reader;
import org.apache.cocoon.servlet.multipart.Part;
import org.outerj.daisy.frontend.util.ResponseUtil;
import org.xml.sax.SAXException;

/**
 * Read the value of a CForms upload widget.
 * 
 * parameters: src should be in the format inputModule:attributeName and it should point to a cforms Upload instance
 * 
 * @author karel
 */
public class UploadWidgetReader implements LogEnabled, Reader, Disposable, Initializable, Serviceable {
    private OutputStream outputStream;
    private Response response;
    private Upload widget;
    
    private ServiceManager service;
    private InputModuleHelper moduleHelper;

    private String disposition;
    private Logger logger;
    private static final int BUFFER_SIZE = 32768;

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    public void dispose() {
        this.widget = null;
    }

    public void generate() throws IOException, SAXException, ProcessingException {
        if (widget.getValue() == null) {
            throw new RuntimeException("Widget value is null");
        }
        
        Part part = ((Part)widget.getValue());
        response.setIntHeader("Content-Length", part.getSize());

        if (disposition != null) {
            ResponseUtil.addContentDispositionHeader(response, disposition, ((Part)widget.getValue()).getUploadName());
        }
        
        if  (widget.getValue() == null) 
            return;
        
        InputStream is = null;
        try {
            is = part.getInputStream();
            byte[] data = new byte[BUFFER_SIZE];
            int read;
            do {
                read = is.read(data);
                if (read != -1) {
                    outputStream.write(data, 0, read);
                }
            } while (read != -1);
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
        return widget.getAttribute("lastModified") == null ? -1:(Long)widget.getAttribute("lastModified");
    }
    
    public void service(ServiceManager service) throws ServiceException {
        this.service = service; 
    }

    public void initialize() throws Exception {
        moduleHelper = new InputModuleHelper();
        moduleHelper.setup(service);
    }

    public void setup(SourceResolver sourceResolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        try {
            Request request = ObjectModelHelper.getRequest(objectModel);

            widget = (Upload)moduleHelper.getAttribute(objectModel, src.substring(0, src.indexOf(":")), src.substring(src.indexOf(":")+1, src.length()), null);
            disposition = parameters.getParameter("disposition", null);

            this.response = ObjectModelHelper.getResponse(objectModel);
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

    public void setOutputStream(OutputStream outputStream) throws IOException {
        this.outputStream = outputStream;
    }

    public String getMimeType() {
        return ((Part)widget.getValue()).getMimeType();
    }

    public boolean shouldSetContentLength() {
        return false;
    }
}
