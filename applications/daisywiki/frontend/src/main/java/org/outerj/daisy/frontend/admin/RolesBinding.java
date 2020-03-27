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

import org.apache.cocoon.forms.binding.AbstractCustomBinding;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.MultiValueField;
import org.apache.commons.jxpath.JXPathContext;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.UserManager;

public class RolesBinding extends AbstractCustomBinding {
    protected void doLoad(Widget widget, JXPathContext jxPathContext) throws Exception {
        Form form = (Form)widget;
        User user = (User)jxPathContext.getValue(".");

        if (user.getDefaultRole() != null)
            form.getChild("defaultRole").setValue(new Long(user.getDefaultRole().getId()));

        Role[] roles = user.getAllRoles().getArray();
        Long[] roleIds = new Long[roles.length];
        for (int i = 0; i < roleIds.length; i++)
            roleIds[i] = new Long(roles[i].getId());
        form.getChild("roles").setValue(roleIds);
    }

    protected void doSave(Widget widget, JXPathContext jxPathContext) throws Exception {
        Form form = (Form)widget;
        User user = (User)jxPathContext.getValue(".");

        UserManager userManager = (UserManager)form.getAttribute("UserManager");

        Role roles[] = user.getAllRoles().getArray();

        // sync role additions
        MultiValueField rolesField = (MultiValueField)form.getChild("roles");
        Long[] roleIds = (Long[])rolesField.getValue();
        for (int i = 0; i < roleIds.length; i++) {
            if (getRole(roles, roleIds[i].longValue()) == null)
                user.addToRole(userManager.getRole(roleIds[i].longValue(), false));
        }

        // sync role removals
        for (int i = 0; i < roles.length; i++) {
            if (!contains(roleIds, roles[i].getId()))
                user.removeFromRole(roles[i]);
        }

        if (form.getChild("defaultRole").getValue() != null) {
            long defaultRoleId = ((Long)form.getChild("defaultRole").getValue()).longValue();
            Role defaultRole = userManager.getRole(defaultRoleId, false);
            user.setDefaultRole(defaultRole);
        } else {
            user.setDefaultRole(null);
        }
    }

    private Role getRole(Role[] roles, long id) {
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].getId() == id)
                return roles[i];
        }
        return null;
    }

    private boolean contains(Long[] ids, long id) {
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].longValue() == id)
                return true;
        }
        return false;
    }
}
