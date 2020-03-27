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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.cocoon.components.flow.apples.AppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.outerj.daisy.frontend.FrontEndContext;

/**
 * Base class for Apple controllers in the Daisy Wiki.
 */
public abstract class AbstractDaisyApple implements AppleController, Contextualizable {
    private Lock lock = new ReentrantLock();
    private String mountPoint;
    private String layoutType;
    private String continuationId;
    private Context context;
    /** Import: this is a request-scope variable! */
    protected Request request;
    protected Response response;
    /** Import: this is a request-scope variable! */
    protected FrontEndContext frontEndContext;

    public final void contextualize(Context context) throws ContextException {
        this.context = context;
        if (!(this instanceof StatelessAppleController))
            continuationId = (String)context.get("continuation-id");
    }

    public final void process(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        if (needsLock())
            lock.tryLock(10000, TimeUnit.MILLISECONDS);
        try {
            request = appleRequest.getCocoonRequest();
            response = appleResponse.getCocoonResponse();
            frontEndContext = FrontEndContext.get(request);
            mountPoint = frontEndContext.getMountPoint();
            layoutType = frontEndContext.getLayoutType();

            processRequest(appleRequest, appleResponse);
        } finally {
            this.request = null;
            this.frontEndContext = null;
            if (needsLock())
                lock.unlock();
        }
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        // override this to do something useful

        // default: call processInternal for backwards compatibility
        processInternal(appleRequest, appleResponse);
    }


    /**
     * @deprecated use {@link #processRequest} instead.
     */
    protected void processInternal(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
    }

    protected final String getMountPoint() {
        return mountPoint;
    }

    protected final String getLayoutType() {
        return layoutType;
    }

    protected final String getLayoutType(String defaultLayoutType) {
        if (layoutType == null)
            return defaultLayoutType;
        else
            return layoutType;
    }

    protected final String getContinuationId() {
        return continuationId;
    }

    protected boolean needsLock() {
        return !(this instanceof StatelessAppleController);
    }

    protected final Context getContext() {
        return context;
    }
    
}
