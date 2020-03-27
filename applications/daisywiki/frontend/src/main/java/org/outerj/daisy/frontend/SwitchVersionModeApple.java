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

import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.frontend.util.ResponseUtil;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VersionMode;

public class SwitchVersionModeApple extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Repository repository = frontEndContext.getRepository();

        if (request.getMethod().equals("POST")) {
            String versionModeParam = RequestUtil.getStringParameter(request, "versionMode");
            VersionMode versionMode = VersionMode.get(versionModeParam);

            WikiHelper.setVersionMode(request, versionMode);

            String returnTo = request.getParameter("returnTo");
            if (returnTo != null)
                ResponseUtil.safeRedirect(appleRequest, appleResponse, returnTo);
            else
                ResponseUtil.safeRedirect(appleRequest, appleResponse, getMountPoint() + "/");
        } else {
            throw new HttpMethodNotAllowedException(request.getMethod());
        }
    }
}
