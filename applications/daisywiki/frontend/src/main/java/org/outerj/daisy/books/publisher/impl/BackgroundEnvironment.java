/*
 * Copyright 1999-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.outerj.daisy.books.publisher.impl;

import org.apache.cocoon.environment.AbstractEnvironment;
import org.apache.cocoon.environment.Context;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.commandline.CommandLineRequest;
import org.apache.cocoon.environment.commandline.CommandLineResponse;
import org.apache.cocoon.environment.commandline.CommandLineContext;
import org.apache.cocoon.util.NullOutputStream;

import java.net.MalformedURLException;
import java.io.File;
import java.io.OutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Collections;


import org.apache.avalon.framework.logger.Logger;

/**
 * A simple implementation of <code>org.apache.cocoon.environment.Environment</code>
 * for pipeline calls which are not externally triggered.
 *
 * <p>NOTE: this class was copied from the Cocoon cron block.
 *
 * @author <a href="http://apache.org/~reinhard">Reinhard Poetz</a>
 * @version CVS $Id: BackgroundEnvironment.java 37181 2004-08-29 17:47:03Z vgritsenko $
 *
 * @since 2.1.4
 */
public class BackgroundEnvironment extends AbstractEnvironment {

    public BackgroundEnvironment(Logger logger, Context ctx) throws MalformedURLException {
        super("", null, new File(ctx.getRealPath("/")), null);
        enableLogging(logger);

        this.outputStream = new NullOutputStream();

        // TODO Would special Background*-objects have advantages?
        Request request = new CommandLineRequest(
                this,                  // environment
                "",                    // context path
                "",                    // servlet path
                "",                    // path info
                new HashMap(),         // attributes
                new HashMap(), // parameters
                Collections.EMPTY_MAP  // headers
        );
        this.objectModel.put(ObjectModelHelper.REQUEST_OBJECT, request);
        this.objectModel.put(ObjectModelHelper.RESPONSE_OBJECT,
                             new CommandLineResponse());
        this.objectModel.put(ObjectModelHelper.CONTEXT_OBJECT, ctx);
    }

    /**
     * @param uri
     * @param view
     * @param context
     * @param stream
     * @param log
     * @throws MalformedURLException
     */
    public BackgroundEnvironment(String uri, String view, File context, OutputStream stream, Logger log)
    throws MalformedURLException {

        super(uri, view, context);
        this.enableLogging(log);
        this.outputStream = stream;

        // TODO Would special Background*-objects have advantages?
        Request request = new CommandLineRequest(this, "", uri, null, null, null);
        this.objectModel.put(ObjectModelHelper.REQUEST_OBJECT, request);
        this.objectModel.put(ObjectModelHelper.RESPONSE_OBJECT,
                             new CommandLineResponse());
        this.objectModel.put(ObjectModelHelper.CONTEXT_OBJECT,
                             new CommandLineContext(context.getAbsolutePath()));
    }

    /**
     * @see org.apache.cocoon.environment.AbstractEnvironment#redirect(boolean, java.lang.String)
     */
    public void redirect(boolean sessionmode, String newURL) throws IOException {
    }

    /**
     * @see org.apache.cocoon.environment.Environment#setContentType(java.lang.String)
     */
    public void setContentType(String mimeType) {
    }

    /**
     * @see org.apache.cocoon.environment.Environment#getContentType()
     */
    public String getContentType() {
        return null;
    }

    /**
     * @see org.apache.cocoon.environment.Environment#setContentLength(int)
     */
    public void setContentLength(int length) {
    }

    /**
     * Always return false
     *
     * @see org.apache.cocoon.environment.Environment#isExternal()
     */
    public boolean isExternal() {
        return false;
    }
}