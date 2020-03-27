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
package org.outerj.daisy.httpconnector.spi;

import org.outerj.daisy.repository.Repository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Handler for HTTP requests.
 *
 * <p>Handlers always deal with URL's of the following structure:
 *
 * <pre>
 * {namespace}/{remainder of URL}
 * </pre>
 *
 * The {namespace} part is a fixed string (should not contain slashes)
 * used to have a bulk division of the URL space among different subsystems.
 *
 * <p>The {remainder of URL} can have any structure whatsoever. The structure
 * handled by this RequestHandler is the pattern returned by {@link #getPathPattern()}.
 *
 * <p>The RequestHandler should be registered as a plugin with the
 * {@link org.outerj.daisy.plugin.PluginRegistry PluginRegistry}
 */
public interface RequestHandler {
    /**
     *
     * @param matchMap contains the content of the matched wildcards from the pattern, if any.
     *                 The keys for retrieving these patterns are "1", "2", etc (as string objects).
     * @param repository Repository object of the authenticated user.
     * @param support
     */
    void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support)
            throws Exception;

    /**
     * Pattern for the URLs that should be handled by this request handler.
     *
     * <p>The pattern should usually start with a slash. It should
     * not include the namespace to which this RequestHandler belongs.
     *
     * <p>The pattern can contain the wildcards * and **.
     * One star matches any character without slash, two stars
     * matches any character including slash.
     *
     * <p>Patterns will be checked in the order by which the request
     * handlers are registered (within a namespace).
     */
    String getPathPattern();

    /**
     * See class description.
     */
    String getNamespace();
}
