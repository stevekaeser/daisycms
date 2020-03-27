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
package org.outerj.daisy.frontend.admin;

import org.apache.cocoon.environment.Request;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.xmlbeans.XmlCursor;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.acl.AccessManager;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerx.daisy.x10.AclDocument;
import org.outerx.daisy.x10.AclObjectDocument;
import org.outerx.daisy.x10.AclEntryDocument;

import java.util.Map;
import java.util.HashMap;

public class ManageAclApple extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        String aclName = appleRequest.getSitemapParameter("name");

        if (!"live".equals(aclName) && !"staging".equals(aclName))
            throw new org.apache.cocoon.ResourceNotFoundException("ACL does not exist: " + aclName);

        Repository repository = frontEndContext.getRepository();
        AccessManager accessManager = repository.getAccessManager();

        String action = request.getParameter("action");
        if (action == null) {
            // show the ACL
            AclDocument aclXml;
            if (aclName.equals("live"))
                aclXml = accessManager.getLiveAcl().getXml();
            else
                aclXml = accessManager.getStagingAcl().getXml();

            annotateAcl(aclXml, repository);

            Map<String, Object> viewData = new HashMap<String, Object>();
            viewData.put("pageXml", new XmlObjectXMLizable(aclXml));
            viewData.put("pageContext", frontEndContext.getPageContext());

            appleResponse.sendPage("ShowAclPipe", viewData);
        } else if (action.equals("putLive") && aclName.equals("staging")) {
            if (isConfirmed(request)) {
                accessManager.copyStagingToLive();

                Map<String, Object> viewData = new HashMap<String, Object>();
                viewData.put("title", "Done");
                viewData.put("message", "The staging ACL has been put live.");
                viewData.put("linkTitle", "Administration Home");
                viewData.put("link", getMountPoint() + "/admin");
                viewData.put("pageContext", frontEndContext.getPageContext());

                appleResponse.sendPage("MessagePagePipe", viewData);
            } else {
                Map<String, Object> viewData = new HashMap<String, Object>();
                viewData.put("title", "Are you sure?");
                viewData.put("message", "Are you sure you want to put the current staging ACL live?");
                viewData.put("confirmURL", getMountPoint() + "/admin/acl/staging?action=putLive&confirmed=true");
                viewData.put("cancelURL", getMountPoint() + "/admin");
                viewData.put("pageContext", frontEndContext.getPageContext());
                appleResponse.sendPage("ConfirmationPagePipe", viewData);
            }
        } else if (action.equals("revertChanges") && aclName.equals("staging")) {
            if (isConfirmed(request)) {
                accessManager.copyLiveToStaging();

                Map<String, Object> viewData = new HashMap<String, Object>();
                viewData.put("title", "Done");
                viewData.put("message", "The staging ACL has been overwritten with the contents of the live ACL.");
                viewData.put("linkTitle", "Administration Home");
                viewData.put("link", getMountPoint() + "/admin");
                viewData.put("pageContext", frontEndContext.getPageContext());

                appleResponse.sendPage("MessagePagePipe", viewData);
            } else {
                Map<String, Object> viewData = new HashMap<String, Object>();
                viewData.put("title", "Are you sure?");
                viewData.put("message", "Are you sure you want to overwrite the staging ACL with the live ACL?");
                viewData.put("confirmURL", getMountPoint() + "/admin/acl/staging?action=revertChanges&confirmed=true");
                viewData.put("cancelURL", getMountPoint() + "/admin");
                viewData.put("pageContext", frontEndContext.getPageContext());
                appleResponse.sendPage("ConfirmationPagePipe", viewData);
            }
        } else {
            throw new java.lang.Exception("Illegal request with action parameter = " + action);
        }
    }

    private boolean isConfirmed(Request request) {
        String confirmed = request.getParameter("confirmed");
        return confirmed != null && confirmed.equals("true");
    }

    private void annotateAcl(AclDocument aclDocument, Repository repository) {
        UserManager userManager = repository.getUserManager();
        for (AclObjectDocument.AclObject aclObject : aclDocument.getAcl().getAclObjectList()) {
            for (AclEntryDocument.AclEntry entry : aclObject.getAclEntryList()) {
                String subjectType = entry.getSubjectType().toString();
                long subjectValue = entry.getSubjectValue();
                String label = "";
                if (subjectType.equals("role")) {
                    try {
                        label = userManager.getRoleDisplayName(subjectValue);
                    } catch (Exception e) {
                        label = "(error)";
                    }
                } else if (subjectType.equals("user")) {
                    try {
                        label = userManager.getUserDisplayName(subjectValue);
                    } catch (Exception e) {
                        label = "(error)";
                    }
                }

                XmlCursor cursor = entry.newCursor();
                cursor.toNextToken();
                cursor.insertAttributeWithValue("subjectValueLabel", label);
                cursor.dispose();
            }
        }
    }
}
