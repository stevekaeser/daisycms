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
package org.outerj.daisy.frontend.editor;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.logger.Logger;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceResolver;
import org.apache.excalibur.store.Store;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.frontend.util.CacheHelper;
import org.outerj.daisy.repository.schema.PartTypeUse;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10Parteditor.PartEditorDocument;

public class PartEditorManager {
    private static final String STORE_PREFIX = PartEditorManager.class.getName();

    public static PartEditor getPartEditor(PartTypeUse partTypeUse, DocumentEditorContext documentEditorContext) throws Exception {
//        if (!partTypeUse.isEditable()) {
//            return new NonEditablePartEditor.Factory().getPartEditor(partTypeUse, Collections.EMPTY_MAP, documentEditorContext);
//        }
        SourceResolver sourceResolver = null;
        Source source = null;
        Store store = null;
        try {
            sourceResolver = (SourceResolver)documentEditorContext.getServiceManager().lookup(SourceResolver.ROLE);
            source = sourceResolver.resolveURI("wikidata:/resources/parteditors/" + partTypeUse.getPartType().getName() + ".xml");
            if (!source.exists()) {
                if (partTypeUse.getPartType().isDaisyHtml()) {
                    return new HtmlPartEditor.Factory().getPartEditor(partTypeUse, Collections.<String,String>emptyMap(), documentEditorContext);
                } else {
                    return new UploadPartEditor.Factory().getPartEditor(partTypeUse, Collections.<String,String>emptyMap(), documentEditorContext);
                }
            } else {
                store = (Store)documentEditorContext.getServiceManager().lookup(Store.TRANSIENT_STORE);
                PartEditorInfo partEditorInfo = (PartEditorInfo)CacheHelper.getFromCache(store, source, STORE_PREFIX);
                if (partEditorInfo == null) {
                    partEditorInfo = buildPartEditorInfo(source, documentEditorContext.getLogger());
                    CacheHelper.setInCache(store, partEditorInfo, source, STORE_PREFIX);
                }

                if (partEditorInfo.isValid()) {
                    return partEditorInfo.getBuilder().getPartEditor(partTypeUse, partEditorInfo.getProperties(), documentEditorContext);
                }
            }
        } finally {
            if (source != null)
                sourceResolver.release(source);
            if (sourceResolver != null)
                documentEditorContext.getServiceManager().release(sourceResolver);
            if (store != null)
                documentEditorContext.getServiceManager().release(store);
        }
        
        return null;
    }

    private static PartEditorInfo buildPartEditorInfo(Source source, Logger logger) {
        PartEditorDocument partEditorDocument;
        try {
            InputStream is = null;
            try {
                is = source.getInputStream();
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                partEditorDocument = PartEditorDocument.Factory.parse(is, xmlOptions);
            } finally {
                if (is != null)
                    is.close();
            }

            String className = partEditorDocument.getPartEditor().getClass1();
            if (className == null)
                throw new Exception("Missing class attribute for partEditor specification in " + source.getURI());

            Class clazz;
            try {
                clazz = PartEditorManager.class.getClassLoader().loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new Exception("Class for partEditor not found: " + className + " specified in " + source.getURI());
            }

            if (!PartEditorFactory.class.isAssignableFrom(clazz)) {
                throw new Exception("Class specified for part editor is not a " + PartEditorFactory.class.getName() + " at " + source.getURI());
            }

            PartEditorFactory factory = (PartEditorFactory)clazz.newInstance();

            Map<String, String> properties = new HashMap<String, String>();
            PartEditorDocument.PartEditor.Properties propertiesXml = partEditorDocument.getPartEditor().getProperties();
            if (propertiesXml != null) {
                for (PartEditorDocument.PartEditor.Properties.Entry entry : propertiesXml.getEntryList()) {
                    String key = entry.getKey();
                    if (key != null) {
                        properties.put(key, entry.getStringValue());
                    }
                }
            }

            return new PartEditorInfo(factory, Collections.unmodifiableMap(properties), true);
        } catch (Throwable e) {
            logger.error("Error in part editor specification at " + source.getURI(), e);
            return new PartEditorInfo(null, null, false);
        }
    }

    static class PartEditorInfo {
        private PartEditorFactory factory;

        private Map<String, String> properties;

        private boolean valid;

        public PartEditorInfo(PartEditorFactory factory, Map<String, String> properties, boolean valid) {
            this.factory = factory;
            this.properties = properties;
            this.valid = valid;

            if (valid && (factory == null || properties == null)) {
                throw new IllegalArgumentException("if valid argument is true, the other arguments cannot be null");
            }
        }

        public PartEditorFactory getBuilder() {
            return factory;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public boolean isValid() {
            return valid;
        }
    }
}