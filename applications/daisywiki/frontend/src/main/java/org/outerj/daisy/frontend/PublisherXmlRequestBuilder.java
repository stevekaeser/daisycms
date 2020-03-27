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

import org.outerx.daisy.x10Publisher.PublisherRequestDocument;
import org.apache.xmlbeans.XmlSaxHandler;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlException;
import org.apache.cocoon.components.flow.util.PipelineUtil;
import org.apache.cocoon.components.LifecycleHelper;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.service.ServiceManager;
import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;

/**
 * Utility code to build a PublisherRequestDocument from the output of a Cocoon pipeline.
 */
public class PublisherXmlRequestBuilder {
    public static PublisherRequestDocument loadPublisherRequest(String name, Object params, ServiceManager serviceManager, Context context) throws Exception {
        XmlSaxHandler xmlSaxHandler = XmlObject.Factory.newXmlSaxHandler();
        try {
            executePipeline(name, params, xmlSaxHandler.getContentHandler(), serviceManager, context);
        } catch (Throwable e) {
            throw new Exception("Error building publisher request for \"" + name + "\".", e);
        }

        XmlObject requestAsXmlObject;
        try {
            requestAsXmlObject = xmlSaxHandler.getObject();
        } catch (XmlException e) {
            throw new SAXException("Error retrieving publisher request as XmlObject.", e);
        }

        PublisherRequestDocument publisherRequestDocument = (PublisherRequestDocument)requestAsXmlObject.changeType(PublisherRequestDocument.type);
        if (publisherRequestDocument == null)
            throw new SAXException("Could not change the type of the XmlObject to PublisherRequestDocument.");

        return publisherRequestDocument;
    }

    private static void executePipeline(String pipe, Object viewData, ContentHandler contentHandler, ServiceManager serviceManager, Context context) throws Exception {
        PipelineUtil pipelineUtil = new PipelineUtil();
        try {
            LifecycleHelper.setupComponent(pipelineUtil, null, context, serviceManager, null, false);
            pipelineUtil.processToSAX(pipe, viewData, contentHandler);
        } finally {
            LifecycleHelper.dispose(pipelineUtil);
        }
    }
}
