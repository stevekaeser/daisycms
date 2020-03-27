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
package org.outerj.daisy.frontend.util;

import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.cocoon.components.ContextHelper;
import org.apache.cocoon.environment.Request;

/**
 * This class exists to provide Flowscript with access to the
 * various Avalon artifacts.
 */
public class FlowAvalonHelper implements Contextualizable, Serviceable {
    private Context context;
    private ServiceManager serviceManager;

    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    public Context getContext() {
        return context;
    }

    public Request getRequestObject() {
        return ContextHelper.getRequest(context);
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }
}
