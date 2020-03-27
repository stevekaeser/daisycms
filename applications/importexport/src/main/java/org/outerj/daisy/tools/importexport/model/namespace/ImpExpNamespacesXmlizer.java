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
package org.outerj.daisy.tools.importexport.model.namespace;

import org.outerj.daisy.tools.importexport.util.XmlProducer;

import java.io.OutputStream;
import java.util.Map;
import java.util.LinkedHashMap;

public class ImpExpNamespacesXmlizer {
    private ImpExpNamespaces impExpNamespaces;
    private OutputStream outputStream;

    public static void toXml(ImpExpNamespaces namespaces, OutputStream outputStream) throws Exception {
        new ImpExpNamespacesXmlizer(namespaces, outputStream).run();
    }

    private ImpExpNamespacesXmlizer(ImpExpNamespaces impExpNamespaces, OutputStream outputStream) {
        this.impExpNamespaces = impExpNamespaces;
        this.outputStream = outputStream;
    }

    private void run() throws Exception {
        XmlProducer xmlProducer = new XmlProducer(outputStream);
        xmlProducer.startElement("namespaces");

        ImpExpNamespace[] namespaces = impExpNamespaces.getNamespaces();

        for (ImpExpNamespace namespace : namespaces) {
            Map<String, String> attrs = new LinkedHashMap<String, String>();
            attrs.put("name", namespace.getName());
            attrs.put("fingerprint", namespace.getFingerprint());
            attrs.put("required", String.valueOf(namespace.isRequired()));
            xmlProducer.emptyElement("namespace", attrs);
        }

        xmlProducer.endElement("namespaces");
        xmlProducer.flush();
    }
}
