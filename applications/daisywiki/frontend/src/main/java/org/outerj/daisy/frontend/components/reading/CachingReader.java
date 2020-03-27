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

import org.apache.cocoon.reading.Reader;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.Cache;
import org.apache.cocoon.caching.CachedResponse;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.SourceParameters;
import org.apache.excalibur.source.impl.validity.ExpiresValidity;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * The stupid solution to forced caching, without resolving the "cocoon:"
 * pipeline since that costs too much energy.
 */
public class CachingReader implements Reader, Serviceable {
    private ServiceManager serviceManager;
    private MyCachedResponse cachedResponse;
    private OutputStream outputStream;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    public void generate() throws IOException, SAXException, ProcessingException {
        outputStream.write(cachedResponse.getResponse());
        outputStream.flush();
    }

    public long getLastModified() {
        return System.currentTimeMillis();
    }

    public void setup(SourceResolver sourceResolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        SourceParameters sourceParameters = new SourceParameters(request.getQueryString());
        long expires;
        try {
            expires = parameters.getParameterAsLong("expires") * 1000;
        } catch (ParameterException e) {
            throw new ProcessingException(e);
        }

        // This will be good enough for our needs
        String cacheKey = "----DAISY-CACHING-READER----" + request.getRequestURI() + "?" + sourceParameters.getQueryString() + "~~" + src;

        Cache cache;
        try {
            cache = (Cache) this.serviceManager.lookup(Cache.ROLE);
        } catch (ServiceException e) {
            throw new ProcessingException(e);
        }
        try {
            cachedResponse = (MyCachedResponse)cache.get(cacheKey);

            if (cachedResponse != null) {
                SourceValidity[] validities = cachedResponse.getValidityObjects();
                ExpiresValidity validity = (ExpiresValidity)validities[0];
                if (validity.isValid() != SourceValidity.VALID)
                    cachedResponse = null;
            }

            if (cachedResponse == null) {
                ExpiresValidity validity = new ExpiresValidity(expires);
                Source source = sourceResolver.resolveURI(src);
                byte[] data = readSource(source);
                cachedResponse = new MyCachedResponse(new SourceValidity[] { validity }, data, source.getMimeType());
                cache.store(cacheKey, cachedResponse);
            }
        } finally {
            serviceManager.release(cache);
        }
    }

    private byte[] readSource(Source source) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        InputStream inputStream = source.getInputStream();
        int length;
        while ((length = inputStream.read(buffer)) > -1) {
            os.write(buffer, 0, length);
        }
        inputStream.close();
        return os.toByteArray();
    }


    public void setOutputStream(OutputStream outputStream) throws IOException {
        this.outputStream = outputStream;
    }

    public String getMimeType() {
        return cachedResponse.getMimeType();
    }

    public boolean shouldSetContentLength() {
        return false;
    }

    static class MyCachedResponse extends CachedResponse {
        private String mimeType;
        public MyCachedResponse(SourceValidity[] sourceValidities, byte[] bytes, String mimeType) {
            super(sourceValidities, bytes);
            this.mimeType = mimeType;
        }

        public String getMimeType() {
            return mimeType;
        }
    }
}
