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
package org.outerj.daisy.frontend.admin;

import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.frontend.RequestUtil;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.user.PublicUserInfo;
import org.outerj.daisy.repository.user.UserNotFoundException;
import org.outerj.daisy.workflow.WorkflowManager;
import org.outerj.daisy.workflow.WfPoolManager;
import org.outerj.daisy.workflow.WfPool;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.environment.Request;

import java.util.*;

public class WfPoolMembersApple extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Repository repository = frontEndContext.getRepository();
        Locale locale = frontEndContext.getLocale();
        WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
        WfPoolManager poolManager = workflowManager.getPoolManager();

        String poolIdString = appleRequest.getSitemapParameter("id");
        Long poolId = Long.parseLong(poolIdString);

        if (request.getMethod().equals("GET")) {
            // Show pool membership page
            WfPool pool = poolManager.getPool(poolId);
            List<Long> userIds = poolManager.getUsersForPool(poolId);
            List<MemberInfo> memberInfos = new ArrayList<MemberInfo>(userIds.size());
            UserManager userManager = repository.getUserManager();

            for (long userId : userIds) {
                MemberInfo memberInfo = new MemberInfo();
                memberInfo.userId = userId;
                try {
                    PublicUserInfo publicUserInfo = userManager.getPublicUserInfo(userId);
                    memberInfo.login = publicUserInfo.getLogin();
                    memberInfo.displayName = publicUserInfo.getDisplayName();
                } catch (UserNotFoundException e) {
                    memberInfo.login = "[error]";
                    memberInfo.displayName = "[user does not exist]";
                }
                memberInfos.add(memberInfo);
            }

            Map<String, Object> viewData = new HashMap<String, Object>();
            viewData.put("memberInfos", memberInfos);
            viewData.put("pool", pool);
            viewData.put("poolXml", new XmlObjectXMLizable(pool.getXml()));
            viewData.put("locale", locale);
            viewData.put("mountPoint", getMountPoint());
            viewData.put("pageContext", frontEndContext.getPageContext());

            appleResponse.sendPage("PoolMembersPipe", viewData);
        } else if (request.getMethod().equals("POST")) {
            String action = RequestUtil.getStringParameter(request, "action");

            if (action.equals("add")) {
                poolManager.addUsersToPool(poolId, getUserIds(request));
            } else if (action.equals("remove")) {
                poolManager.removeUsersFromPool(poolId, getUserIds(request));
            } else if (action.equals("clear")) {
                poolManager.clearPool(poolId);
            } else {
                throw new Exception("Invalid value for action parameter: " + action);
            }

            // So that we are there again with a "GET"
            appleResponse.redirectTo(getMountPoint() + "/admin/wfPool/" + poolId + "/members");
        } else {
            throw new HttpMethodNotAllowedException(request.getMethod());
        }
    }

    private List<Long> getUserIds(Request request) throws Exception {
        String[] userIdStrings = request.getParameterValues("userId");
        if (userIdStrings == null)
            return Collections.emptyList();
        List<Long> userIds = new ArrayList<Long>(userIdStrings.length);
        for (String userIdString : userIdStrings) {
            try {
                userIds.add(Long.parseLong(userIdString));
            } catch (NumberFormatException e) {
                throw new Exception("Invalid user ID in userId request parameter: " + userIdString);
            }
        }
        return userIds;
    }

    public static class MemberInfo {
        public long userId;
        public String login;
        public String displayName;

        public long getUserId() {
            return userId;
        }

        public String getLogin() {
            return login;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
