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

import java.util.Properties;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.http.HttpResponse;
import org.apache.commons.codec.binary.Base64;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.Oid;

public class NegotiateClientAuthenticator extends AbstractLogEnabled implements ClientAuthenticator, ThreadSafe, Configurable {
    
    private Properties properties = new Properties();
    
    public NegotiateClientAuthenticator() {}

    public void configure(Configuration configuration) throws ConfigurationException {
        if (configuration == null) {
            return;
        }
        Configuration env = configuration.getChild("environment");
        for (Configuration prop: env.getChildren("property")) {
            String name = prop.getAttribute("name");
            String value = prop.getAttribute("value");
            
            properties.put(name, value);
        }
    }

    public String authenticateClient(Request request, Response response) throws Exception {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            getLogger().debug("No authorization header present");
            requestAuthentication(request, response, "Negotiate");
            return null; 
        }
        
        if (!authHeader.startsWith("Negotiate ")) {
            getLogger().debug("Not using 'Negotiate' authentication");
            return null;
        }

        String base64token = authHeader.substring("Negotiate ".length());
        byte[] token = Base64.decodeBase64(base64token.getBytes());
        
        Oid spnegoOid = new Oid("1.3.6.1.5.5.2");
        GSSManager manager = GSSManager.getInstance();

        GSSCredential myCred = manager.createCredential(null, GSSCredential.DEFAULT_LIFETIME, spnegoOid, GSSCredential.ACCEPT_ONLY);
        GSSContext context = manager.createContext(myCred);
        
        byte[] tokenForPeer = context.acceptSecContext(token, 0, token.length);
        
        
        if (!context.isEstablished()) {
            getLogger().debug("GSSContext is not established");
            return null;
        }
        if (tokenForPeer != null) {
            getLogger().info("There was a token to send back (ignored)");
	    
	    // Sending it back makes IE show an error page instead of the 401
	    // response's body.
	    // requestAuthentication(request, response, "Negotiate ".concat(new String(Base64.encodeBase64(tokenForPeer))));
        }
        
        getLogger().debug("Client principal is " + context.getSrcName());  
        getLogger().debug("Server principal is " + context.getTargName());
        
        if (!context.getCredDelegState()) {
            getLogger().debug("Credentials can not be delegated (This is only a problem is the repository requires SSO auth, which is not the case with the TrustedAuthenticationScheme.  (To delegate credentials in firefox, add the wiki hostname to 'network.negotiate-auth.delegation-uris' in about:config");
        }

        request.setAttribute("gss-manager", manager);
        request.setAttribute("gss-context", context);
        request.setAttribute("gss-peertoken", tokenForPeer);
        
        return context.getSrcName().toString();
    }

	private void requestAuthentication(Request request, Response response, String authString) {
        // the setAttribute is required because the headers are cleared in some cases (only for some exceptions it seems)
		// (in that case the attribute is picked up by the ChallengeAction and the header is restored)
		request.setAttribute("WWW-Authenticate-header", authString);
		response.setHeader("WWW-Authenticate", authString);
		((HttpResponse)response).setStatus(401);
		
	}
}
