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
package org.outerj.daisy.httpconnector;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.cocoon.matching.helpers.WildcardHelper;
import org.apache.commons.fileupload.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.httpconnector.handlers.*;
import org.outerj.daisy.httpconnector.spi.*;
import org.outerj.daisy.util.VersionHelper;
import org.outerj.daisy.util.HttpConstants;
import org.outerj.daisy.plugin.PluginRegistry;
import org.outerj.daisy.plugin.PluginUser;
import org.outerj.daisy.plugin.PluginHandle;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.*;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.thread.BoundedThreadPool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.security.Principal;

public class HttpConnector implements RequestHandlerSupport {
    private int port;
    private RepositoryManager repositoryManager;
    private Server server;
    private Log requestErrorLogger = LogFactory.getLog("org.outerj.daisy.request-errors");
    private int uploadThreshold;
    private int uploadMaxSize;
    private String uploadTempdir;
    private Map<String, List<PathHandler>> pathHandlersByNamespace = new HashMap<String, List<PathHandler>>();
    private String versionString;
    private PluginRegistry pluginRegistry;
    private PluginUser<RequestHandler> pluginUser = new MyPluginUser();
    /** The RequestHandler's for the core repository server. */
    private List<RequestHandler> repositoryRequestHandlers = new ArrayList<RequestHandler>();

    public HttpConnector(Configuration configuration, RepositoryManager repositoryManager,
            PluginRegistry pluginRegistry) throws Exception {
        this.repositoryManager = repositoryManager;
        this.pluginRegistry = pluginRegistry;
        this.configure(configuration);
        this.initialize();
        this.start();
    }

    @PreDestroy
    public void destroy() throws Exception {
        this.stop();

        for (RequestHandler handler : repositoryRequestHandlers) {
            pluginRegistry.removePlugin(RequestHandler.class, handler.getNamespace() + handler.getPathPattern(), handler);
        }

        pluginRegistry.unsetPluginUser(RequestHandler.class, pluginUser);
    }

    private void configure(Configuration configuration) throws ConfigurationException
    {
        port = configuration.getChild("port").getValueAsInteger();
        Configuration uploadConf = configuration.getChild("upload");
        uploadThreshold = uploadConf.getChild("threshold").getValueAsInteger(50000);
        uploadMaxSize = uploadConf.getChild("maxsize").getValueAsInteger(-1);
        uploadTempdir = uploadConf.getChild("tempdir").getValue(null);
    }

    private void initialize() throws Exception {
        System.setProperty("org.mortbay.util.URI.charset", "UTF-8");
        System.setProperty("org.mortbay.log.LogFactory.noDiscovery", "true");

        versionString = VersionHelper.getVersionString(getClass().getClassLoader(), "org/outerj/daisy/repository/serverimpl/versioninfo.properties");

        server = new Server();

        // Configure security
        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setAuthenticate(true);
        constraint.setRoles(new String[] { "*" });
        
        ConstraintMapping constraintMapping = new ConstraintMapping();
        constraintMapping.setPathSpec("/*");
        constraintMapping.setConstraint(constraint);

        SecurityHandler securityHandler = new SecurityHandler();
        securityHandler.setUserRealm(new DaisyUserRealm());
        securityHandler.setConstraintMappings(new ConstraintMapping[] { constraintMapping });

        // Configure Daisy servlet
        Context root = new Context(server, "/", Context.NO_SESSIONS);
        root.setSecurityHandler(securityHandler);
        root.addServlet(new ServletHolder(new DaisyServlet()), "/*");

        // Configure threadpool
        org.mortbay.thread.BoundedThreadPool threadPool = new BoundedThreadPool();
        threadPool.setMinThreads(10);
        threadPool.setMaxThreads(255);
        server.setThreadPool(threadPool);

        // Configure connector
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(port);
        String headerBufferSize = System.getProperty("jetty.http.headerbuffersize");
        if (headerBufferSize != null) { // jetty uses 4096 by default, this is insufficient for, say, Kerberos tickets
            connector.setHeaderBufferSize(Integer.parseInt(headerBufferSize));
        }
        server.addConnector(connector);

        pluginRegistry.setPluginUser(RequestHandler.class, pluginUser);

        initializeRepositoryHandlers();
    }

    private void start() throws Exception {
        server.start();
    }

    private void stop() throws Exception {
        server.stop();
    }

    private void initializeRepositoryHandlers() {
        // These are the handlers for the core repository. We could consider moving the
        // registration of this over to the repository itself.
        repositoryRequestHandlers.add(new UserInfoHandler());
        repositoryRequestHandlers.add(new DocumentHandler(requestErrorLogger));
        repositoryRequestHandlers.add(new DocumentsHandler(requestErrorLogger));
        repositoryRequestHandlers.add(new CommentsHandler());
        repositoryRequestHandlers.add(new CommentHandler());
        repositoryRequestHandlers.add(new UserCommentsHandler());
        repositoryRequestHandlers.add(new VersionHandler());
        repositoryRequestHandlers.add(new VersionsHandler());
        repositoryRequestHandlers.add(new TimelineHandler());
        repositoryRequestHandlers.add(new LockHandler());
        repositoryRequestHandlers.add(new PartDataHandler());
        repositoryRequestHandlers.add(new AvailableVariantsHandler());
        repositoryRequestHandlers.add(new QueryHandler());
        repositoryRequestHandlers.add(new FacetedQueryHandler());
        repositoryRequestHandlers.add(new DistinctQueryHandler());
        repositoryRequestHandlers.add(new AclHandler());
        repositoryRequestHandlers.add(new PartTypeHandler());
        repositoryRequestHandlers.add(new PartTypeByNameHandler());
        repositoryRequestHandlers.add(new PartTypesHandler());
        repositoryRequestHandlers.add(new FieldTypeHandler());
        repositoryRequestHandlers.add(new FieldTypeByNameHandler());
        repositoryRequestHandlers.add(new FieldTypesHandler());
        repositoryRequestHandlers.add(new DocumentTypeHandler());
        repositoryRequestHandlers.add(new DocumentTypeByNameHandler());
        repositoryRequestHandlers.add(new DocumentTypesHandler());
        repositoryRequestHandlers.add(new CollectionHandler());
        repositoryRequestHandlers.add(new CollectionByNameHandler());
        repositoryRequestHandlers.add(new CollectionsHandler());
        repositoryRequestHandlers.add(new FilterDocumentTypesHandler());
        repositoryRequestHandlers.add(new FilterDocumentsHandler());
        repositoryRequestHandlers.add(new UsersHandler());
        repositoryRequestHandlers.add(new UserIdsHandler());
        repositoryRequestHandlers.add(new UserHandler());
        repositoryRequestHandlers.add(new UserByLoginHandler());
        repositoryRequestHandlers.add(new UserByEmailHandler());
        repositoryRequestHandlers.add(new PublicUserInfoHandler());
        repositoryRequestHandlers.add(new PublicUserInfoByLoginHandler());
        repositoryRequestHandlers.add(new PublicUserInfosHandler());
        repositoryRequestHandlers.add(new RoleByNameHandler());
        repositoryRequestHandlers.add(new RolesHandler());
        repositoryRequestHandlers.add(new RoleHandler());
        repositoryRequestHandlers.add(new BranchHandler());
        repositoryRequestHandlers.add(new BranchByNameHandler());
        repositoryRequestHandlers.add(new BranchesHandler());
        repositoryRequestHandlers.add(new LanguageHandler());
        repositoryRequestHandlers.add(new LanguageByNameHandler());
        repositoryRequestHandlers.add(new LanguagesHandler());
        repositoryRequestHandlers.add(new AuthenticationSchemesHandler());
        repositoryRequestHandlers.add(new LinkExtractorsHandler());
        repositoryRequestHandlers.add(new NamespacesHandler());
        repositoryRequestHandlers.add(new NamespaceHandler());
        repositoryRequestHandlers.add(new NamespaceByNameHandler());
        repositoryRequestHandlers.add(new SelectionListDataHandler());

        for (RequestHandler handler : repositoryRequestHandlers) {
            pluginRegistry.addPlugin(RequestHandler.class, handler.getNamespace() + handler.getPathPattern(), handler);
        }
    }

    private class MyPluginUser implements PluginUser<RequestHandler> {

        public void pluginAdded(PluginHandle<RequestHandler> pluginHandle) {
            String namespace = pluginHandle.getPlugin().getNamespace();
            RequestHandler requestHandler = pluginHandle.getPlugin();

            List<PathHandler> pathHandlers = pathHandlersByNamespace.get(namespace);
            if (pathHandlers == null) {
                pathHandlers = new ArrayList<PathHandler>();
                pathHandlersByNamespace.put(namespace, pathHandlers);
            }

            pathHandlers.add(new PathHandler(requestHandler));
        }

        public void pluginRemoved(PluginHandle<RequestHandler> pluginHandle) {
            String namespace = pluginHandle.getPlugin().getNamespace();
            RequestHandler requestHandler = pluginHandle.getPlugin();

            List<PathHandler> pathHandlers = pathHandlersByNamespace.get(namespace);
            if (pathHandlers != null) {
                for (PathHandler pathHandler : pathHandlers) {
                    if (pathHandler.getRequestHandler() == requestHandler) {
                        pathHandlers.remove(pathHandler);
                        return;
                    }
                }
            }
        }
    }

    public List<UploadItem> parseMultipartRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, BadRequestException {
        if (!FileUpload.isMultipartContent(request)) {
            HttpUtil.sendCustomError("Expected multipart/form-data encoded content.", HttpConstants._400_Bad_Request, response);
            return null;
        }

        DiskFileUpload upload = new DiskFileUpload();
        upload.setSizeThreshold(uploadThreshold);
        upload.setSizeMax(uploadMaxSize);
        if (uploadTempdir != null)
            upload.setRepositoryPath(uploadTempdir);

        try {
            List fileItems = upload.parseRequest(request);
            List<UploadItem> uploadItems = new ArrayList<UploadItem>(fileItems.size());
            for (Object item : fileItems) {
                uploadItems.add(new UploadItemImpl((FileItem)item));
            }
            return uploadItems;
        } catch (FileUploadBase.SizeLimitExceededException e) {
            throw new BadRequestException("Uploaded data exceeded the maximum allowed size.");
        } catch (FileUploadException e) {
            requestErrorLogger.error("Error handling multipart data.", e);
            throw new BadRequestException("There was a problem handling the multipart data.");
        }
    }

    private class DaisyServlet extends HttpServlet {
        protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            String path = request.getPathInfo();
            response.addHeader("X-Daisy-Version", versionString);

            // Check if the URL path structure conforms to the pattern "/namespace/..."
            int secondSlashPos;
            if (path != null && path.startsWith("/") && path.length() > 1 && (secondSlashPos = path.indexOf('/', 1)) != -1) {
                String namespace = path.substring(1, secondSlashPos);
                path = path.substring(secondSlashPos); // the second slash is left at the start of the path

                try {
                    Repository repository = ((DaisyUserPrincipal)request.getUserPrincipal()).getRepository();
                    HashMap matchMap = new HashMap();

                    List<PathHandler> pathHandlers = pathHandlersByNamespace.get(namespace);
                    if (pathHandlers != null) {
                        for (PathHandler pathHandler : pathHandlers) {
                            if (pathHandler.handle(path, matchMap, request, response, repository, HttpConnector.this))
                                return;
                            matchMap.clear();
                        }
                    }
                } catch (BadRequestException e) {
                    // doesn't need to be logged
                    HttpUtil.sendCustomError(e.getMessage(), HttpConstants._400_Bad_Request, response);
                    return;
                } catch (Exception e) {
                    requestErrorLogger.error("Error processing request " + path, e);
                    HttpUtil.sendCustomError(e, HttpConstants._202_Accepted, response);
                    return;
                } catch (Error e) {
                    requestErrorLogger.error("Error processing request " + path, e);
                    HttpUtil.sendCustomError(e, HttpConstants._202_Accepted, response);
                    return;
                }
            }

            // Nothing matched, send 404
            response.setStatus(HttpConstants._404_Not_Found);
            response.setContentType(HttpConstants.MIMETYPE_TEXT_HTML);

            Writer writer = response.getWriter();
            writer.write("<HTML>\n<HEAD>\n<TITLE>Error 404 - Not Found");
            writer.write("</TITLE>\n<BODY>\n<H2>Error 404 - Not Found.</H2>\n</BODY>\n</HTML>");
            writer.close();
        }
    }

    /**
     * A runtime wrapper around a {@link org.outerj.daisy.httpconnector.spi.RequestHandler}.
     */
    private static class PathHandler {
        private final String path;
        private final boolean isPattern;
        private final RequestHandler requestHandler;
        private final int[] compiledPattern;

        public PathHandler(RequestHandler handler) {
            this.path = handler.getPathPattern();
            this.isPattern = path.indexOf("*") != -1;
            this.requestHandler = handler;
            if (isPattern)
                compiledPattern = WildcardHelper.compilePattern(path);
            else
                compiledPattern = null;
        }

        /**
         * @return true if this handler handled the request
         */
        public boolean handle(String requestPath, HashMap matchMap, HttpServletRequest request,
                HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
            matchMap.clear();
            if ((isPattern && WildcardHelper.match(matchMap, requestPath, compiledPattern)) || (!isPattern && path.equals(requestPath))) {
                requestHandler.handleRequest(matchMap, request, response, repository, support);
                return true;
            }
            return false;
        }

        public RequestHandler getRequestHandler() {
            return requestHandler;
        }
    }

    private class DaisyUserRealm implements UserRealm {
        public String getName() {
            return "daisy";
        }


        public Principal authenticate(String userRoleIdentification, Object credentials, Request request) {
            // userRoleIdentification follows the structure <login>@<roleId> in which the @<roleId> part
            // is optional. Any '@'-symbols occuring in the <login> part must be escaped by doubling them
            // The code below determines the <login> and <roleId> parts
            StringBuilder login = new StringBuilder(userRoleIdentification.length());
            StringBuilder roleIds = new StringBuilder(5);

            final int STATE_IN_AT = 1;
            final int STATE_IN_ROLE = 2;
            final int STATE_IN_NAME = 3;

            int state = STATE_IN_NAME;

            for (int i = 0; i < userRoleIdentification.length(); i++) {
                char c = userRoleIdentification.charAt(i);
                switch (c) {
                    case '@':
                        if (state == STATE_IN_ROLE) {
                            requestErrorLogger.error("Invalid username (user/role identification): " + userRoleIdentification);
                            return null;
                        } else if (state == STATE_IN_AT) {
                            login.append('@');
                            state = STATE_IN_NAME;
                        } else {
                            state = STATE_IN_AT;
                        }
                        break;
                    default:
                        if (state == STATE_IN_AT || state == STATE_IN_ROLE) {
                            state = STATE_IN_ROLE;
                            if ((c >= '0' && c <= '9') || c == ',') {
                                roleIds.append(c);
                            } else {
                                requestErrorLogger.error("Invalid username (user/role identification), encountered non-digit in role ID: " + userRoleIdentification);
                                return null;
                            }
                        } else if (state == STATE_IN_NAME) {
                            login.append(c);
                        }
                }
            }

            Credentials daisyCredentials = new Credentials(login.toString(), (String)credentials);

            try {
                Repository repository = repositoryManager.getRepository(daisyCredentials);
                if (roleIds.length() > 0)  {
                    long[] parsedRoleIds = parseRoleIds(roleIds.toString());
                    if (parsedRoleIds.length > 0)
                        repository.setActiveRoleIds(parsedRoleIds);
                }
                return new DaisyUserPrincipal(repository);
            } catch (RepositoryException e) {
                requestErrorLogger.error("Error authenticating user.", e);
                return null;
            }
        }

        /**
         *
         * @param roleIdSpec a comma-separated list of role IDs
         */
        private long[] parseRoleIds(String roleIdSpec) {
            List<String> roleIdList = new ArrayList<String>();
            StringTokenizer tokenizer = new StringTokenizer(roleIdSpec, ",");
            while (tokenizer.hasMoreTokens()) {
                roleIdList.add(tokenizer.nextToken());
            }
            long[] roleIds = new long[roleIdList.size()];
            for (int i = 0; i < roleIds.length; i++)
                roleIds[i] = Long.parseLong(roleIdList.get(i));
            return roleIds;
        }

        public void disassociate(Principal userPrincipal) {
        }

        public Principal pushRole(Principal userPrincipal, String string) {
            return null;
        }

        public Principal popRole(Principal userPrincipal) {
            return null;
        }

        public void logout(Principal userPrincipal) {
        }

        public Principal getPrincipal(String string) {
            return null;
        }

        public boolean reauthenticate(Principal principal) {
            return false;
        }

        public boolean isUserInRole(Principal principal, String string) {
            return false;
        }
    }

    private static class DaisyUserPrincipal implements Principal {
        private Repository repository;

        public DaisyUserPrincipal(Repository repository) {
            this.repository = repository;
        }

        public String getName() {
            return String.valueOf(repository.getUserId());
        }

        public Repository getRepository() {
            return repository;
        }
    }
}
