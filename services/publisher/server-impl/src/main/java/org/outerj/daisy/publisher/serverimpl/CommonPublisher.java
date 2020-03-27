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
package org.outerj.daisy.publisher.serverimpl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.configutil.PropertyResolver;
import org.outerj.daisy.credentialsprovider.CredentialsProvider;
import org.outerj.daisy.plugin.PluginHandle;
import org.outerj.daisy.plugin.PluginRegistry;
import org.outerj.daisy.plugin.PluginUser;
import org.outerj.daisy.publisher.PublisherException;
import org.outerj.daisy.publisher.serverimpl.requestmodel.PublisherContext;
import org.outerj.daisy.publisher.serverimpl.requestmodel.PublisherRequest;
import org.outerj.daisy.publisher.serverimpl.requestmodel.PublisherRequestBuilder;
import org.outerj.daisy.publisher.serverimpl.resolving.PublisherRequestResolver;
import org.outerj.daisy.publisher.serverimpl.variables.VariablesManager;
import org.outerj.daisy.publisher.serverimpl.variables.impl.VariablesManagerImpl;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.spi.ExtensionProvider;
import org.outerx.daisy.x10Publisher.PublisherRequestDocument;
import org.xml.sax.SAXException;

/**
 * This component provides no services of its own, but registers an extension called
 * "Publisher" with the repository.
 *
 */
public class CommonPublisher {
    private PluginRegistry pluginRegistry;
    private ExtensionProvider extensionProvider = new MyExtensionProvider();
    private PublisherRequestResolver publisherRequestResolver;
    private String repositoryKey;
    private CredentialsProvider credentialsProvider;
    private RepositoryManager repositoryManager;
    private Repository repository;
    private File pubRequestsRoot;
    private VariablesManagerImpl variablesManager;
    private final Log log = LogFactory.getLog(getClass());
    private final String EXTENSION_NAME = "Publisher";

    public CommonPublisher(Configuration configuration, RepositoryManager repositoryManager,
            PluginRegistry pluginRegistry, CredentialsProvider credentialsProvider) throws Exception {
        this.repositoryManager = repositoryManager;
        this.pluginRegistry = pluginRegistry;
        this.credentialsProvider = credentialsProvider;
        configure(configuration);
        initialize();
    }

    @PreDestroy
    public void destroy() {
        pluginRegistry.removePlugin(ExtensionProvider.class, EXTENSION_NAME, extensionProvider);
        variablesManager.destroy();
    }

    private void configure(Configuration configuration) throws ConfigurationException {
        repositoryKey = configuration.getChild("repositoryKey").getValue("internal");
        String pubRequestDir = PropertyResolver.resolveProperties(configuration.getChild("publisherRequestDirectory").getValue());
        pubRequestsRoot = new File(pubRequestDir);
        // Note: the existance of the pubRequestsRoot directory is not check on purpose. It is optional
        // and this avoids the need to configure it for e.g. development installations.
    }

    private void initialize() throws Exception {
        try {
            repository = repositoryManager.getRepository(credentialsProvider.getCredentials(repositoryKey));
        } catch (Throwable e) {
            throw new Exception("Problem getting repository.", e);
        }
        this.variablesManager = new VariablesManagerImpl(repository);
        this.publisherRequestResolver = new PublisherRequestResolver(pubRequestsRoot, repository, this);

        pluginRegistry.addPlugin(ExtensionProvider.class, EXTENSION_NAME, extensionProvider);
    }

    public PublisherRequest buildPublisherRequest(PublisherRequestDocument publisherRequestDocument) throws SAXException, RepositoryException {
        List errors = new ArrayList();
        XmlOptions xmlOptions = new XmlOptions();
        xmlOptions.setErrorListener(errors);
        boolean valid = publisherRequestDocument.validate(xmlOptions);

        if (!valid) {
            StringBuilder errorsAsString = new StringBuilder();
            errorsAsString.append("The supplied publisher request does not validate against the publisher request schema: ");
            for (int i = 0; i < errors.size(); i++) {
                if (i > 0)
                    errorsAsString.append(", ");
                XmlError error = (XmlError)errors.get(i);
                errorsAsString.append(error.toString());
            }
            throw new PublisherException(errorsAsString.toString());
        }

        try {
            return PublisherRequestBuilder.build(publisherRequestDocument.getPublisherRequest(), repository);
        } catch (Throwable e) {
            if (e instanceof RepositoryException) {
                throw (RepositoryException)e;
            } else if (e instanceof SAXException) {
                throw (SAXException)e;
            } else {
                throw new PublisherException("Error while building publisher request.", e);
            }
        }
    }

    public PublisherRequest lookupPublisherRequest(String pubReqSetName, Document document, Version version, PublisherContext publisherContext)
            throws SAXException, RepositoryException {
        return publisherRequestResolver.lookupPublisherRequest(pubReqSetName, document, version, publisherContext);
    }

    public VariablesManager getVariablesManager() {
        return variablesManager;
    }

    private class MyExtensionProvider implements ExtensionProvider {
        public Object createExtension(Repository repository) {
            return new PublisherImpl(repository, CommonPublisher.this, log);
        }
    }
}
