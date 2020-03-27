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
package org.outerj.daisy.frontend;

import org.apache.cocoon.acting.Action;
import org.apache.cocoon.environment.*;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.avalon.framework.parameters.Parameters;

import java.util.Map;

/**
 * This actions prevents proxies from caching when there is a session.
 * At the time of this writing, the Daisy Wiki does not create a session
 * as long as the user is not logged in (i.e. is the guest user).
 */
public class SetCacheHeaderAction  implements Action, ThreadSafe {

    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        Request request = ObjectModelHelper.getRequest(objectModel);

        if (request.getSession(false) != null) {
            Response response = ObjectModelHelper.getResponse(objectModel);
            response.setHeader("Cache-Control", "private");
        }

        return null;
    }
}

