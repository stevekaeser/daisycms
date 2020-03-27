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
package org.outerj.daisy.tools.importexport.model.collection;

import org.outerj.daisy.tools.importexport.util.XmlProducer;

import java.io.OutputStream;
import java.util.Set;

public class ImpExpCollectionsXmlizer {
    private Set<String> collections;
    private OutputStream outputStream;

    public static void toXml(Set<String> collections, OutputStream outputStream) throws Exception {
        new ImpExpCollectionsXmlizer(collections, outputStream).run();
    }

    private ImpExpCollectionsXmlizer(Set<String> collections, OutputStream outputStream) {
        this.collections = collections;
        this.outputStream = outputStream;
    }

    private void run() throws Exception {
        XmlProducer xmlProducer = new XmlProducer(outputStream);
        xmlProducer.startElement("collections");

        for (String collection : collections) {
            xmlProducer.simpleElement("collection", collection);
        }

        xmlProducer.endElement("collections");
        xmlProducer.flush();
    }

}
