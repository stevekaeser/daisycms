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
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceResolver;
import org.apache.excalibur.store.Store;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.frontend.util.CacheHelper;
import org.outerj.daisy.repository.schema.FieldTypeUse;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10Fieldeditor.FieldEditorDocument;

public class FieldEditorManager {
    private static final String STORE_PREFIX = FieldEditorManager.class.getName();

    public static FieldEditor getFieldEditor(FieldTypeUse fieldTypeUse, DocumentEditorContext documentEditorContext) throws Exception {
        ServiceManager serviceManager = documentEditorContext.getServiceManager();
        SourceResolver sourceResolver = null;
        Source source = null;
        Store store = null;
        try {
            sourceResolver = (SourceResolver)serviceManager.lookup(SourceResolver.ROLE);
            source = sourceResolver.resolveURI("wikidata:/resources/fieldeditors/" + fieldTypeUse.getFieldType().getName() + ".xml");
            if (!source.exists()) {
                return new DefaultFieldEditor.Factory().getFieldEditor(fieldTypeUse, Collections.EMPTY_MAP, documentEditorContext);
            } else {
                store = (Store)documentEditorContext.getServiceManager().lookup(Store.TRANSIENT_STORE);
                FieldEditorInfo fieldEditorInfo = (FieldEditorInfo)CacheHelper.getFromCache(store, source, STORE_PREFIX);
                if (fieldEditorInfo == null) {
                    fieldEditorInfo = buildFieldEditorInfo(source, documentEditorContext.getLogger());
                    CacheHelper.setInCache(store, fieldEditorInfo, source, STORE_PREFIX);
                }

                if (fieldEditorInfo.isValid()) {
                    return fieldEditorInfo.getBuilder().getFieldEditor(fieldTypeUse, fieldEditorInfo.getProperties(), documentEditorContext);
                }
            }
        } finally {
            if (source != null)
                sourceResolver.release(source);
            if (sourceResolver != null)
                serviceManager.release(sourceResolver);
            if (store != null)
                serviceManager.release(store);
        }
        return null;
    }

    private static FieldEditorInfo buildFieldEditorInfo(Source source, Logger logger) {
        FieldEditorDocument fieldEditorDocument;
        try {
            InputStream is = null;
            try {
                is = source.getInputStream();
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                fieldEditorDocument = FieldEditorDocument.Factory.parse(is, xmlOptions);
            } finally {
                if (is != null)
                    is.close();
            }

            String className = fieldEditorDocument.getFieldEditor().getClass1();
            if (className == null)
                throw new Exception("Missing class attribute for fieldEditor specification in " + source.getURI());

            Class clazz;
            try {
                clazz = FieldEditorManager.class.getClassLoader().loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new Exception("Class for fieldEditor not found: " + className + " specified in " + source.getURI());
            }

            if (!FieldEditorFactory.class.isAssignableFrom(clazz)) {
                throw new Exception("Class specified for field editor is not a " + FieldEditorFactory.class.getName() + " at " + source.getURI());
            }

            FieldEditorFactory factory = (FieldEditorFactory)clazz.newInstance();

            Map<String, String> properties = new HashMap<String, String>();
            FieldEditorDocument.FieldEditor.Properties propertiesXml = fieldEditorDocument.getFieldEditor().getProperties();
            if (propertiesXml != null) {
                for (FieldEditorDocument.FieldEditor.Properties.Entry entry : propertiesXml.getEntryList()) {
                    String key = entry.getKey();
                    if (key != null) {
                        properties.put(key, entry.getStringValue());
                    }
                }
            }

            return new FieldEditorInfo(factory, Collections.unmodifiableMap(properties), true);
        } catch (Throwable e) {
            logger.error("Error in field editor specification at " + source.getURI(), e);
            return new FieldEditorInfo(null, null, false);
        }
    }

    static class FieldEditorInfo {
        private FieldEditorFactory factory;

        private Map<String, String> properties;

        private boolean valid;

        public FieldEditorInfo(FieldEditorFactory factory, Map<String, String> properties, boolean valid) {
            this.factory = factory;
            this.properties = properties;
            this.valid = valid;

            if (valid && (factory == null || properties == null)) {
                throw new IllegalArgumentException("if valid argument is true, the other arguments cannot be null");
            }
        }

        public FieldEditorFactory getBuilder() {
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