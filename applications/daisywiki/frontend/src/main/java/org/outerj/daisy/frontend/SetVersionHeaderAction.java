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

import org.apache.cocoon.acting.Action;
import org.apache.cocoon.environment.*;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.activity.Initializable;
import org.outerj.daisy.util.VersionHelper;

import java.util.Map;

/**
 * Sets a HTTP header containing the Daisy Version.
 * This action should typically be put somewhere at the start of the sitemap.
 */
public class SetVersionHeaderAction implements Action, ThreadSafe, Initializable {
    private static String versionString;

    public void initialize() throws Exception {
        versionString = VersionHelper.getVersionString(SetVersionHeaderAction.class.getClassLoader(),
                "org/outerj/daisy/frontend/versioninfo.properties");
    }

    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        Response response = ObjectModelHelper.getResponse(objectModel);
        response.setHeader("X-Daisy-Version", versionString);
        return null;
    }
}
