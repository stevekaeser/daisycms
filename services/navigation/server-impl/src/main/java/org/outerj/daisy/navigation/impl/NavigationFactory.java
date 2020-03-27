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
package org.outerj.daisy.navigation.impl;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.QNameSet;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.navigation.NavigationException;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;

/**
 * Builds a tree of navigation {Node}s based on an XML description.
 *
 * <p>This is a use-once, non-reusable (and non-threadsafe) class.
 */
public class NavigationFactory {
    private static String[] PART_NAMES = {"NavigationDescription", "BookDefinitionDescription"};

    private static Map<QName, Constructor> NAVIGATION_BUILDERS;
    static {
        NAVIGATION_BUILDERS = new HashMap<QName, Constructor>();
        try {
            NAVIGATION_BUILDERS.put(new QName("http://outerx.org/daisy/1.0#navigationspec", "navigationTree"), DefaultNavigationBuilder.class.getConstructor());
            NAVIGATION_BUILDERS.put(new QName("http://outerx.org/daisy/1.0#bookdef", "book"), BookNavigationBuilder.class.getConstructor());
        } catch (Exception e) {
            throw new RuntimeException("Error initializing navigation builders map.", e);
        }
    }

    static interface NavigationBuilder {
        void buildTree(Node parentNode, XmlObject xmlObject, BuildContext buildContext) throws RepositoryException;

        SchemaType getSchemaType();
    }

    /**
     * Builds a navigation tree from an XML navigation description provided as a string argument.
     */
    public static void build(Node parentNode, String navigationXml, long branchId, long languageId, BuildContext buildContext) throws NavigationException {
        try {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            XmlObject navTreeXml = XmlObject.Factory.parse(new StringReader(navigationXml), xmlOptions);
            buildContext.pushContextVariant(branchId, languageId);
            try {
                build(parentNode, navTreeXml, buildContext);
            } finally {
                buildContext.popContextVariant();
            }
        } catch (Throwable e) {
            throw new NavigationException("Error building navigation tree.", e);
        }
    }

    public static void build(Node parentNode, VariantKey navigationDoc, BuildContext buildContext) throws NavigationException {
        try {
            // normalize the navigationDoc VariantKey, since it is added to the dependency information of the RootNode (see further on)
            navigationDoc = new VariantKey(buildContext.getNavContext().getRepository().normalizeDocumentId(navigationDoc.getDocumentId()),
                    navigationDoc.getBranchId(), navigationDoc.getLanguageId());

            Document document = buildContext.getNavContext().getRepository().getDocument(navigationDoc, false);

            Version version;
            VersionMode versionMode = buildContext.getVersionMode();
            if (versionMode.isLive()) {
                version = document.getLiveVersion();
                if (version == null)
                    throw new NavigationException(navigationDoc + " does not have a live version.");
            } else if (versionMode.isLast()) {
                version = document.getLastVersion();
            } else if (versionMode.getDate() != null){
                version = document.getVersion(versionMode);
                if (version == null)
                    throw new NavigationException(navigationDoc + " does not have a live version at " + versionMode);
            } else {
                throw new NavigationException("Unexepected VersionMode: " + versionMode);
            }

            String partName = null;
            for (int i = 0; i < PART_NAMES.length; i++) {
                if (version.hasPart(PART_NAMES[i])) {
                    partName = PART_NAMES[i];
                    break;
                }
            }

            if (partName == null)
                throw new NavigationException(navigationDoc + " (version mode: " + versionMode + ") does not contain a part from which a navigation tree can be build.");

            Part part = version.getPart(partName);

            XmlObject xmlObject;
            InputStream is = part.getDataStream();
            try {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                xmlObject = XmlObject.Factory.parse(new BufferedInputStream(is), xmlOptions);
            } finally {
                is.close();
            }

            buildContext.pushContextVariant(navigationDoc.getBranchId(), navigationDoc.getLanguageId());
            try {
                build(parentNode, xmlObject, buildContext);
            } finally {
                buildContext.popContextVariant();
            }

            buildContext.getRootNode().addDependency(navigationDoc);
        } catch (Throwable e) {
            throw new NavigationException("Error building navigation tree.", e);
        }
    }

    private static void build(Node parentNode, XmlObject xmlObject, BuildContext buildContext) throws NavigationException {
        // XMLBeans type inference doesn't seem to work, so do our own detection based on the root element
        XmlObject[] result = xmlObject.selectChildren(QNameSet.ALL);
        QName rootEl = result[0].newCursor().getName();

        Constructor buildConstructor = NAVIGATION_BUILDERS.get(rootEl);
        if (buildConstructor == null) {
            throw new NavigationException("Provided XML does not contain a recognized navigation format.");
        }

        NavigationBuilder navigationBuilder;
        try {
            navigationBuilder = (NavigationBuilder)buildConstructor.newInstance();
        } catch (IllegalAccessException e) {
            throw new NavigationException("Unexpected IllegalAccessException.", e);
        } catch (InvocationTargetException e) {
            throw new NavigationException("Error building navigation tree.", e.getTargetException());
        } catch (InstantiationException e) {
            throw new NavigationException("Error instantiating navigation tree builder.", e);
        }

        XmlObject typedXmlObject = xmlObject.changeType(navigationBuilder.getSchemaType());
        if (!typedXmlObject.validate()) {
            throw new NavigationException("Provided XML is not valid.");
        }

        try {
            navigationBuilder.buildTree(parentNode, typedXmlObject, buildContext);
        } catch (RepositoryException e) {
            throw new NavigationException("Error building navigation tree.", e);
        }
    }
}
