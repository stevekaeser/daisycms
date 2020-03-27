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
package org.outerj.daisy.repository.clientimpl.infrastructure;

/**
 * This is the default exception class used to restore exceptions
 * that happened in the remote Daisy repository in the client.
 */
public class DaisyPropagatedException extends Exception {
    private String message;
    private String remoteExceptionClassName;
    private MyStackTraceElement[] remoteStackTrace;

    public DaisyPropagatedException(String message, String remoteExceptionClassName, MyStackTraceElement[] remoteStackTrace) {
        this.message = message;
        this.remoteExceptionClassName = remoteExceptionClassName;
        this.remoteStackTrace = remoteStackTrace;

        setStackTrace(new StackTraceElement[0]);
    }

    public String getMessage() {
        return "[" + remoteExceptionClassName + "] " + message;
    }

    public MyStackTraceElement[] getRemoteStackTrace() {
        return remoteStackTrace;
    }

    public String getRemoteClassName() {
        return remoteExceptionClassName;
    }

    public String getUserMessage() {
        return message;
    }
}
