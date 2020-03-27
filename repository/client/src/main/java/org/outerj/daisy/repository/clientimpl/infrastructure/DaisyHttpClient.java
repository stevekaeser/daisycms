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

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.AuthenticationFailedException;
import org.outerj.daisy.repository.RepositoryRuntimeException;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10.ErrorDocument;
import org.outerx.daisy.x10.CauseType;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class DaisyHttpClient {
    private static Map<Class, Method> xmlObjectParseMethodCache = new ConcurrentHashMap<Class, Method>(20, .75f, 2);
    private static final boolean validateResponses = false;
    private HttpClient sharedHttpClient;
    private HttpState httpState;
    private HostConfiguration sharedHostConfiguration;
    private String login;

    public DaisyHttpClient(HttpClient sharedHttpClient, HostConfiguration sharedHostConfiguration, HttpState httpState, String login) {
        this.sharedHttpClient = sharedHttpClient;
        this.httpState = httpState;
        this.sharedHostConfiguration = sharedHostConfiguration;
        this.login = login;
    }

    public static HttpState buildHttpState(String login, String password, long[] activeRoleIds) {
        HttpState httpState = new HttpState();
        // @'s in the login should be escaped by doubling them
        login = login.replaceAll("@", "@@");
        if (activeRoleIds != null)
            login += "@" + getActiveRoleString(activeRoleIds);
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(login, password);
        httpState.setCredentials(AuthScope.ANY, credentials);

        return httpState;
    }

    private static String getActiveRoleString(long[] activeRoleIds) {
        StringBuilder buffer = new StringBuilder(activeRoleIds.length * 4);
        for (int i = 0; i < activeRoleIds.length; i++) {
            if (i > 0)
                buffer.append(',');
            buffer.append(activeRoleIds[i]);
        }
        return buffer.toString();
    }

    /**
     * Executes the given method, and handles the response to take care of exceptions
     * or non-OK responses, and optionally parses the response body according to the specified
     * XmlObject class. If this method returns without throwing an exception, one can
     * assume that the execution of the HTTP method was successful.
     *
     * @param xmlObjectResponseClass an Apache XmlBeans generated class (having a Factory inner class).
     * @return the XmlObject resulting from the parsing of the response body, or null if no XmlObject
     *         class was specified.
     */
    public XmlObject executeMethod(HttpMethod method, Class xmlObjectResponseClass, boolean releaseConnection) throws RepositoryException {
        try {
            int statusCode;
            try {
                statusCode = sharedHttpClient.executeMethod(sharedHostConfiguration, method, httpState);
            } catch (Exception e) {
                throw new RepositoryException("Problems connecting to repository server.", e);
            }

            if (statusCode == HttpStatus.SC_OK) {
                if (xmlObjectResponseClass != null) {
                    Method parseMethod = getParseMethod(xmlObjectResponseClass);
                    XmlObject parseResult;
                    try {
                        parseResult = (XmlObject)parseMethod.invoke(null, method.getResponseBodyAsStream());
                        if (validateResponses)
                            parseResult.validate();
                    } catch (Exception e) {
                        throw new RepositoryException("Error parsing reponse from repository server.", e);
                    }

                    return parseResult;
                } else {
                    return null;
                }
            } else {
                handleNotOkResponse(method);
                // handleNotOkResponse always throws an exception, thus...
                throw new RuntimeException("This statement should be unreacheable.");
            }
        } finally {
            if (releaseConnection)
                method.releaseConnection();
        }
    }

    private static Method getParseMethod(Class xmlObjectClass) {
        Object parseMethod = xmlObjectParseMethodCache.get(xmlObjectClass);
        if (parseMethod != null) {
            return (Method)parseMethod;
        } else {
            Class[] classes = xmlObjectClass.getClasses();
            Class factoryClass = null;
            for (Class clazz : classes) {
                if (clazz.getName().equals(xmlObjectClass.getName() + "$Factory")) {
                    factoryClass = clazz;
                    break;
                }
            }

            if (factoryClass == null) {
                throw new RuntimeException("Missing Factory class in class " + xmlObjectClass.getName());
            }

            Method newParseMethod;
            try {
                newParseMethod = factoryClass.getMethod("parse", java.io.InputStream.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Missing parse method on XmlObject Factory class for " + xmlObjectClass.getName(), e);
            }

            xmlObjectParseMethodCache.put(xmlObjectClass, newParseMethod);
            return newParseMethod;
        }
    }

    public static String getContentType(HttpMethod method) throws RepositoryException {
        String contentType = null;
        if (method.getResponseHeader("Content-Type") != null)
            contentType = method.getResponseHeader("Content-Type").getElements()[0].getName();
        return contentType;
    }

    public void handleNotOkResponse(HttpMethod method) throws RepositoryException {
        if ("text/xml".equals(getContentType(method))) {
            // an error occured server side
            ErrorDocument.Error errorXml;
            try {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                ErrorDocument errorDocument = ErrorDocument.Factory.parse(method.getResponseBodyAsStream(), xmlOptions);
                errorXml = errorDocument.getError();
            } catch (Exception e) {
                throw new RepositoryException("Error reading error response from repositoryserver", e);
            }
            if (errorXml.getDescription() != null) {
                throw new RepositoryException("Repository server answered with an error: " + errorXml.getDescription());
            } else {
                CauseType causeXml = errorXml.getCause();
                tryRestoreOriginalExceptionAndThrowIt(causeXml);
                Exception cause = restoreException(causeXml);
                throw new RepositoryException("Received exception from repository server.", cause);
            }
        } else {
            if (method.getStatusCode() == 401) {
                throw new AuthenticationFailedException(this.login);
            } else {
                throw new RepositoryException("Unexpected response from repositoryserver: " + method.getStatusCode() + " : " + HttpStatus.getStatusText(method.getStatusCode()));
            }
        }
    }

    private static Exception restoreException(CauseType causeXml) {
        String message = causeXml.getException().getMessage();
        String className = causeXml.getException().getType();

        List<MyStackTraceElement> stackTrace = new ArrayList<MyStackTraceElement>();
        List<CauseType.StackTrace.StackTraceElement> stackTraceElements = causeXml.getStackTrace().getStackTraceElementList();
        for (CauseType.StackTrace.StackTraceElement stackTraceElement : stackTraceElements) {
            stackTrace.add(new MyStackTraceElement(stackTraceElement.getClassName(), stackTraceElement.getFileName(), stackTraceElement.getLineNumber(), stackTraceElement.getMethodName(), stackTraceElement.getNativeMethod()));
        }
        MyStackTraceElement[] remoteStackTrace = stackTrace.toArray(new MyStackTraceElement[0]);

        DaisyPropagatedException exception = new DaisyPropagatedException(message, className, remoteStackTrace);

        CauseType nestedCauseXml = causeXml.getCause();
        if (nestedCauseXml != null) {
            Exception cause = restoreException(nestedCauseXml);
            exception.initCause(cause);
        }

        return exception;
    }

    /**
     * This method handles exceptions which can be restored, ie RepositoryException's
     * whose getState method returned a Map and have a constructor that takes a Map
     * as argument.
     *
     * <p>If the exception could be restored, this method will throw it immediatelly,
     * otherwise it will simply return. Only call this method if there is actually
     * ExceptionData, otherwise this will throw a NPE.
     */
    private static void tryRestoreOriginalExceptionAndThrowIt(CauseType causeXml) throws RepositoryException {
        RepositoryException restoredException = tryRestoreOriginalException(causeXml);
        if (restoredException != null) {
            throw restoredException;
        }
    }

    private static RepositoryException tryRestoreOriginalException(CauseType causeXml) throws RepositoryException {
        if (causeXml.getExceptionData() == null)
            return null;

        String className = causeXml.getException().getType();
        CauseType.ExceptionData exceptionData = causeXml.getExceptionData();

        Map<String, String> state = new HashMap<String, String>();
        for (CauseType.ExceptionData.Parameter parameter : exceptionData.getParameterList()) {
            state.put(parameter.getName(), parameter.getValue());
        }

        Class clazz ;
        try {
            clazz = DaisyHttpClient.class.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            return null;
        }

        if (!RepositoryException.class.isAssignableFrom(clazz) && !RepositoryRuntimeException.class.isAssignableFrom(clazz))
            return null;

        Constructor constructor;
        try {
            constructor = clazz.getConstructor(Map.class);
        } catch (NoSuchMethodException e) {
            return null;
        }

        RepositoryException restoredException;
        try {
            restoredException = (RepositoryException)constructor.newInstance(state);
        } catch (InstantiationException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        }

        if (causeXml.getCause() != null) {
            Exception cause = null;
            if (restoredException.getClass().getName().equals("org.outerj.daisy.publisher.GlobalPublisherException")) {
                cause = tryRestoreOriginalException(causeXml.getCause());
            }
            if (cause == null) {
                cause = restoreException(causeXml.getCause());
            }
            restoredException.initCause(cause);
        }

        return restoredException;
    }
}
