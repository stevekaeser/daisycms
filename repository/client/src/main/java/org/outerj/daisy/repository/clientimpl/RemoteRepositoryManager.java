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
package org.outerj.daisy.repository.clientimpl;

import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.spi.ExtensionProvider;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.clientimpl.schema.RemoteSchemaStrategy;
import org.outerj.daisy.repository.clientimpl.user.RemoteUserManagementStrategy;
import org.outerj.daisy.repository.clientimpl.acl.RemoteAclStrategy;
import org.outerj.daisy.repository.clientimpl.variant.RemoteVariantStrategy;
import org.outerj.daisy.repository.clientimpl.comment.RemoteCommentStrategy;
import org.outerj.daisy.repository.commonimpl.*;
import org.outerj.daisy.repository.commonimpl.schema.CommonRepositorySchema;
import org.outerj.daisy.jms.JmsClient;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.logger.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.net.URI;

public class RemoteRepositoryManager implements RepositoryManager, Configurable, Initializable, ThreadSafe, ExtensionRegistrar, Serviceable, Disposable {
    private String baseURL;
    private String jmsTopic;
    private RemoteDocumentStrategy documentStrategy;
    private CommonRepository commonRepository;
    private Credentials cacheUserCredentials;
    private AuthenticatedUser cacheUser;
    private Context context = new Context();
    private Map<String, ExtensionProvider> registeredExtensions = Collections.synchronizedMap(new HashMap<String, ExtensionProvider>());
    private JmsClient jmsClient;
    private MultiThreadedHttpConnectionManager httpConnectionManager;
    private HttpClient httpClient;
    private boolean requireJms = false;
    private HostConfiguration hostConfiguration;
    private static final int DEFAULT_MAX_HTTP_CONNECTIONS = 60;
    private int maxHttpConnections = DEFAULT_MAX_HTTP_CONNECTIONS;
    private final Log logger = LogFactory.getLog(getClass());

    /**
     * Default constructor, only to be used when respecting the Avalon lifecycle
     * interfaces.
     */
    public RemoteRepositoryManager() {
        // default constructor
    }

    public RemoteRepositoryManager(String url, Credentials cacheUserCredentials) throws Exception {
        this(url, cacheUserCredentials, DEFAULT_MAX_HTTP_CONNECTIONS);
    }

    public RemoteRepositoryManager(String url, Credentials cacheUserCredentials, int maxHttpConnections) throws Exception {
        this(url, cacheUserCredentials, null, null, maxHttpConnections);
    }

    /**
     * @deprecated use the constructor without logger argument
     */
    public RemoteRepositoryManager(String url, Credentials cacheUserCredentials, JmsClient jmsClient, String jmsTopic, Logger logger) throws Exception {
        this(url, cacheUserCredentials, jmsClient, jmsTopic, DEFAULT_MAX_HTTP_CONNECTIONS);
    }

    public RemoteRepositoryManager(String url, Credentials cacheUserCredentials, JmsClient jmsClient, String jmsTopic) throws Exception {
        this(url, cacheUserCredentials, jmsClient, jmsTopic, DEFAULT_MAX_HTTP_CONNECTIONS);
    }

    /**
     * @deprecated use the constructor without logger argument
     */
    public RemoteRepositoryManager(String url, Credentials cacheUserCredentials, JmsClient jmsClient, String jmsTopic, Logger logger, int maxHttpConnections) throws Exception {
        this(url, cacheUserCredentials, jmsClient, jmsTopic, maxHttpConnections);
    }

    public RemoteRepositoryManager(String url, Credentials cacheUserCredentials, JmsClient jmsClient, String jmsTopic, int maxHttpConnections) throws Exception {
        if (url == null)
            throw new NullPointerException("baseURL parameter missing.");
        if (cacheUserCredentials == null)
            throw new NullPointerException("cacheUserCredentials parameter missing.");

        this.jmsClient = jmsClient;
        this.jmsTopic = jmsTopic;
        this.baseURL = url;
        this.cacheUserCredentials = cacheUserCredentials;
        this.maxHttpConnections = maxHttpConnections;

        initialize();
    }

    public void service(ServiceManager serviceManager) throws ServiceException {
        if (serviceManager.hasService("org.outerj.daisy.jms.JmsClient")) {
            jmsClient = (JmsClient)serviceManager.lookup("org.outerj.daisy.jms.JmsClient");
        } else {
            logger.error("RemoteRepositoryManager: failed to lookup jms client component, will do without.");            
        }
    }

    public void configure(Configuration configuration) throws ConfigurationException {
        this.baseURL = configuration.getChild("repository-server-base-url").getValue();

        Configuration cacheUserConf = configuration.getChild("cacheUser");
        final String cacheUserLogin = cacheUserConf.getAttribute("login");
        final String cacheUserPassword = cacheUserConf.getAttribute("password");
        cacheUserCredentials = new Credentials(cacheUserLogin, cacheUserPassword);

        Configuration[] extensionConfs = configuration.getChild("extensions").getChildren("extension");
        for (Configuration extensionConf : extensionConfs) {
            String name = extensionConf.getAttribute("name");
            String className = extensionConf.getAttribute("class");
            ExtensionProvider extensionProvider;
            try {
                Class clazz = getClass().getClassLoader().loadClass(className);
                extensionProvider = (ExtensionProvider)clazz.newInstance();
            } catch (Exception e) {
                throw new ConfigurationException("Problem loading repository extension " + name, e);
            }
            registerExtension(name, extensionProvider);
        }

        jmsTopic = configuration.getChild("jmsTopic").getValue("daisy");
        maxHttpConnections = configuration.getChild("maxHttpConnections").getValueAsInteger(DEFAULT_MAX_HTTP_CONNECTIONS);
        requireJms = configuration.getChild("requireJms").getValueAsBoolean(false);
    }

    public void initialize() throws Exception {
        if (requireJms && jmsClient == null) {
            String message = "RemoteRepositoryManager: JmsClient is not available but was configured as required.";
            if (logger != null)
                logger.error(message);
            throw new Exception("");
        } else if (jmsClient == null) {
            logger.warn("RemoteRepositoryManager: JmsClient is not available, will do without.");
        }

        httpConnectionManager = new MultiThreadedHttpConnectionManager();
        httpConnectionManager.getParams().setDefaultMaxConnectionsPerHost(maxHttpConnections);
        httpConnectionManager.getParams().setMaxTotalConnections(maxHttpConnections);
        httpClient = new HttpClient(httpConnectionManager);
        httpClient.getParams().setAuthenticationPreemptive(true);
        httpClient.getParams().setCredentialCharset("ISO-8859-1");
        httpClient.getParams().setContentCharset("UTF-8");
        URI parsedBaseURL = new URI(baseURL);
        hostConfiguration = new HostConfiguration();
        hostConfiguration.setHost(parsedBaseURL.getHost(), parsedBaseURL.getPort(), parsedBaseURL.getScheme());

        documentStrategy = new RemoteDocumentStrategy(context);
        this.cacheUser = documentStrategy.getUser(cacheUserCredentials);
        // If the cache user has the administrator role, put it in that role, otherwise not.
        long[] availableRoles = this.cacheUser.getAvailableRoleIds();
        for (long availableRole : availableRoles) {
            if (availableRole == Role.ADMINISTRATOR) {
                this.cacheUser.setActiveRoleIds(new long[]{Role.ADMINISTRATOR});
                break;
            }
        }

        RemoteRepositoryStrategy repositoryStrategy = new RemoteRepositoryStrategy(context);
        RemoteSchemaStrategy schemaStrategy = new RemoteSchemaStrategy(context);
        RemoteAclStrategy aclStrategy = new RemoteAclStrategy(context);
        RemoteUserManagementStrategy userManagementStrategy = new RemoteUserManagementStrategy(context);
        RemoteVariantStrategy variantStrategy = new RemoteVariantStrategy(context);
        RemoteCollectionStrategy collectionStrategy = new RemoteCollectionStrategy(context);
        RemoteCommentStrategy commentStrategy = new RemoteCommentStrategy(context);
        this.commonRepository = new RemoteCommonRepository(this, repositoryStrategy, documentStrategy, schemaStrategy,
                aclStrategy, userManagementStrategy, variantStrategy, collectionStrategy, commentStrategy,
                context, registeredExtensions, cacheUser);

        if (jmsClient != null) {
            RemoteEventDispatcher remoteEventDispatcher = new RemoteEventDispatcher(jmsClient, jmsTopic);
            // The RemoteEventDispatcher does not simply fire the events on all listeners registered on
            // the Repository and RepositorySchema objects, because those listeners are by contract
            // only informed about local events
            remoteEventDispatcher.addRepositoryListener(commonRepository.getUserManager().getCacheListener());
            remoteEventDispatcher.addRepositoryListener(commonRepository.getCollectionManager().getCacheListener());
            remoteEventDispatcher.addRepositoryListener(commonRepository.getVariantManager().getCacheListener());
            remoteEventDispatcher.addRepositoryListener(commonRepository.getNamespaceManager().getCacheListener());
            remoteEventDispatcher.addRepositorySchemaListener(commonRepository.getRepositorySchema().getCacheListener());
        }
    }

    public void dispose() {
        httpConnectionManager.shutdown();
    }

    public Repository getRepository(final Credentials credentials) throws RepositoryException {
        AuthenticatedUser user = documentStrategy.getUser(credentials);
        return new RemoteRepositoryImpl(commonRepository, user, context);
    }

    public synchronized void registerExtension(String name, ExtensionProvider extensionProvider) {
        if (registeredExtensions.containsKey(name)) {
            throw new RuntimeException("There is already and extension registered using the name " + name);
        }

        if (registeredExtensions.containsValue(extensionProvider)) {
            throw new RuntimeException("The given extension provider is already registered.");
        }

        registeredExtensions.put(name, extensionProvider);
    }

    public synchronized void unregisterExtension(ExtensionProvider extensionProvider) {
        String key = null;
        for (Map.Entry<String, ExtensionProvider> entry : registeredExtensions.entrySet()) {
            if (entry.getValue() == extensionProvider) {
                key = entry.getKey();
                break;
            }
        }
        if (key != null)
            registeredExtensions.remove(key);
    }

    public class Context {
        private Context() {
            // private constructor to make sure no-one else can create this
        }

        public CommonRepositorySchema getCommonRepositorySchema() {
            return commonRepository.getRepositorySchema();
        }

        public CommonRepository getCommonRepository() {
            return commonRepository;
        }

        public String getBaseURL() {
            return baseURL;
        }

        public HttpClient getSharedHttpClient() {
            return httpClient;
        }

        public HostConfiguration getSharedHostConfiguration() {
            return hostConfiguration;
        }
    }

    public String getRepositoryServerVersion() {
        return commonRepository.getServerVersion(cacheUser);
    }

    public Repository getRepositoryAsUser(User user) throws RepositoryException {
        throw new RepositoryException("Obtaining an unauthenticated repository is not available in the remote implementation.");
    }
}
