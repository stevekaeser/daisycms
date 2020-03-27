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
package org.outerj.daisy.frontend.sso;

import java.util.Map;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.cocoon.acting.Action;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.outerj.daisy.frontend.FrontEndContext;

/**
 * Action that establishes SSO authentication 
 */
public class SSOAction extends AbstractLogEnabled implements Action, ThreadSafe, Serviceable {
    
    private ServiceManager service;
    
    public void service(ServiceManager service) {
        this.service = service;
    }

    public Map act(Redirector redirector, SourceResolver sourceResolver, Map objectModel, String s, Parameters parameters) throws Exception {
        if (!service.hasService(ClientAuthenticator.ROLE)) {
            return null;
        }
        
        Request request = ObjectModelHelper.getRequest(objectModel);
        Response response = ObjectModelHelper.getResponse(objectModel);
        FrontEndContext ctx = FrontEndContext.get(request);
        
        if (ctx.getGuestRepository().getUserId() != ctx.getRepository().getUserId()) {
        	// already logged in
        	return null;
        }
        
        ClientAuthenticator ca = null;
        IdentityMapper im = null;
        RepositoryAuthenticator ra = null;

        try {
            ca = (ClientAuthenticator) service.lookup(ClientAuthenticator.ROLE);
            im = (IdentityMapper) service.lookup(IdentityMapper.ROLE);
            ra = (RepositoryAuthenticator) service.lookup(RepositoryAuthenticator.ROLE);
            
            String username = ca.authenticateClient(request, response);
            if (username == null) {
                return null;
            }
            
            if (im != null) {
                username = im.mapIdentity(username, request, response);
            }
            
            ra.performLogin(username, ctx, request, response);
        } finally {
            if (ca != null) try { service.release(ca); } catch (Throwable t) {};
            if (im != null) try { service.release(im); } catch (Throwable t) {};
            if (ra != null) try { service.release(ra); } catch (Throwable t) {};
        }
        return null;
    }
    
}