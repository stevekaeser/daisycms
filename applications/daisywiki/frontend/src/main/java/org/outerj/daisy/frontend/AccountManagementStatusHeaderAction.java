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
package org.outerj.daisy.frontend;

import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.cocoon.acting.Action;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.Session;
import org.apache.cocoon.environment.SourceResolver;
import org.outerj.daisy.repository.Repository;

public class AccountManagementStatusHeaderAction implements Action, ThreadSafe {

    public Map act(Redirector redirector, SourceResolver resolver,
            Map objectModel, String source, Parameters parameters)
            throws Exception {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Response response = ObjectModelHelper.getResponse(objectModel);
        Session session = request.getSession(false);
        StringBuffer status = new StringBuffer();
        Repository repository = null;
        if (session != null) {
            repository = (Repository)session.getAttribute("daisy-repository");
        }
        if (repository != null) {
            status.append("active; name=\"")
                .append(repository.getUserDisplayName())
                .append("\"; id=\"")
                .append(repository.getUserLogin())
                .append("\"");
        } else {
            status.append("none");
        }

        String scheme = request.getScheme();
        String hostname = request.getServerName();
        int port = request.getServerPort(); 
        StringBuffer link = new StringBuffer("\"");
        link.append(scheme).append("://").append(hostname).append(":").append(port).append("/amcd.json").append(">; rel='acct-mgmt';");
        response.addHeader("Link", link.toString());
        response.addHeader("X-Account-Management-Status", status.toString());
        return null;
    }

}
