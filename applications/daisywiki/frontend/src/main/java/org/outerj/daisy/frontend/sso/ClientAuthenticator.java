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
package org.outerj.daisy.frontend.sso;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;

public interface ClientAuthenticator extends Configurable {
    
    public static final String ROLE = ClientAuthenticator.class.getName();

    public String authenticateClient(Request request, Response response) throws Exception;

}
