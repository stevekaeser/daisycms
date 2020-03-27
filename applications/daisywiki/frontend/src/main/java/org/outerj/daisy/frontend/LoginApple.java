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

import java.util.HashMap;
import java.util.Map;

import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.frontend.util.ResponseUtil;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.user.Role;

public class LoginApple extends AbstractDaisyApple {
    private boolean init = false;
    private String returnTo;

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Repository repository = frontEndContext.getRepository();

        if (!init) {
            returnTo = request.getParameter("returnTo");
            init = true;
        }

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("pipeConf", GenericPipeConfig.stylesheetPipe("daisyskin:xslt/login.xsl"));

        if (request.getMethod().equals("GET") || request.getMethod().equals("HEAD")) {
            appleResponse.sendPage("internal/genericPipe", viewData);
        } else if (request.getMethod().equals("POST")) {
            String action = request.getParameter("action");
            if (action == null) {
                throw new Exception("Missing action request parameter.");
            } else if (action.equals("changeRole")) {
                String newRoleParam = request.getParameter("newrole");
                long[] newActiveRoles;
                if (newRoleParam.equals("all")) {
                    // create a list of all roles the user has, without the admin role
                    long[] allRoles = repository.getAvailableRoles();
                    boolean hasAdminRole = false;
                    for (long role : allRoles) {
                        if (role == Role.ADMINISTRATOR) {
                            hasAdminRole = true;
                            break;
                        }
                    }
                    if (hasAdminRole) {
                        newActiveRoles = new long[allRoles.length - 1];
                        int p = 0;
                        for (long role : allRoles) {
                            if (role != Role.ADMINISTRATOR) {
                                newActiveRoles[p] = role;
                                p++;
                            }
                        }
                    } else {
                        newActiveRoles = allRoles;
                    }
                } else {
                    long newRole = Long.parseLong(newRoleParam);
                    newActiveRoles = new long[] {newRole};
                }
                repository.setActiveRoleIds(newActiveRoles);
            } else if (action.equals("login")) {
                String login = request.getParameter("username");
                String password = request.getParameter("password");
                frontEndContext.login(login, password);
            } else {
                throw new Exception("Unexpected action parameter value: " + action);
            }

            if (returnTo == null || returnTo.equals(""))
                ResponseUtil.safeRedirect(appleRequest, appleResponse, getMountPoint() + "/");
            else
                ResponseUtil.safeRedirect(appleRequest, appleResponse, returnTo);
        } else {
            throw new HttpMethodNotAllowedException(request.getMethod());
        }
    }
}
