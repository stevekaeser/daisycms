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
package org.outerj.daisy.books.publisher.impl.util;

import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.books.publisher.impl.BookInstanceLayout;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Locale;
import java.util.Date;

/**
 * A logger to which progress and errors happening during the book publication are logged.
 */
public class PublicationLog {
    private PrintWriter pw;
    private DateFormat dateFormat;

    public PublicationLog(BookInstance bookInstance) throws Exception {
        dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, Locale.getDefault());
        OutputStream os = bookInstance.getResourceOutputStream(BookInstanceLayout.getPublicationLogPath());
        pw = new PrintWriter(os);
    }

    public void dispose() {
        if (pw != null)
            pw.close();
    }

    public void info(String message) {
        output(message, "INFO ");
        pw.flush();
    }

    public void error(String message) {
        error(message, null);
    }

    public void error(String message, Throwable t) {
        output(message, "ERROR");
        if (t != null)
            pw.println(getFullStackTrace(t));
        pw.flush();
    }

    /**
     * Modified version of ExceptionUtils.getFullStackTrace, which always
     * prints the stack trace of all exceptions (this might lead to some
     * duplicate stacktraces, but at least we see the whole exception chain then).
     */
    private String getFullStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        Throwable[] ts = ExceptionUtils.getThrowables(throwable);
        for (int i = 0; i < ts.length; i++) {
            ts[i].printStackTrace(pw);
        }
        return sw.getBuffer().toString();
    }

    private void output(String message, String type) {
        String date = dateFormat.format(new Date());
        pw.println("[" + type + "] <" + date + "> " + message);
    }
}
