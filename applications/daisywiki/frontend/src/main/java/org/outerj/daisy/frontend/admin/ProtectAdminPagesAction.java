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

import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.Action;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.frontend.FrontEndContext;

import java.util.Map;

public class ProtectAdminPagesAction implements Action, ThreadSafe {

    public Map act(Redirector redirector, SourceResolver sourceResolver, Map objectModel, String s, Parameters parameters) throws Exception {
        Repository repository = FrontEndContext.get(ObjectModelHelper.getRequest(objectModel)).getRepository();
        if (!repository.isInRole(Role.ADMINISTRATOR))
            throw new Exception("Admin pages are only accessible for users acting in the Administrator role.");
        return null;
    }
}
