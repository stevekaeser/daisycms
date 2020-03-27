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
package org.outerj.daisy.httpconnector.spi;

import org.outerx.daisy.x10.ErrorDocument;
import org.outerx.daisy.x10.CauseType;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.schema.DocumentTypeNotFoundException;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.repository.variant.BranchNotFoundException;
import org.outerj.daisy.repository.variant.LanguageNotFoundException;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.lang.reflect.Method;

public class HttpUtil {
    public static void sendCustomError(String message, int code, HttpServletResponse response)
        throws IOException
    {
        response.setStatus(code);
        response.setContentType("text/xml");
        ErrorDocument errorDocument = ErrorDocument.Factory.newInstance();
        ErrorDocument.Error errorXml = errorDocument.addNewError();
        errorXml.setDescription(message);
        errorDocument.save(response.getOutputStream());
    }

    public static void sendCustomError(Throwable throwable, int code, HttpServletResponse response)
        throws IOException
    {
        response.setStatus(code);
        response.setContentType("text/xml");

        ErrorDocument errorDocument = buildErrorXml(throwable);
        errorDocument.save(response.getOutputStream());
    }

    public static ErrorDocument buildErrorXml(Throwable throwable) {
        ErrorDocument errorDocument = ErrorDocument.Factory.newInstance();
        ErrorDocument.Error errorXml = errorDocument.addNewError();
        CauseType causeXml = errorXml.addNewCause();
        createCause(throwable, causeXml);
        return errorDocument;
    }

    private static CauseType createCause(Throwable throwable, CauseType causeXml) {
        CauseType.Exception exceptionXml = causeXml.addNewException();
        exceptionXml.setType(throwable.getClass().getName());

        String message = null;
        // Special message handling for certain exceptions
        if (throwable.getClass().getName().startsWith("freemarker.template.")) {
            // Freemarker exceptions support nicely formatted error location information,
            // here we try to include it in the message
            try {
                // Note: reflection is not too fast of course, but errors in templates
                // should be exceptional exceptions
                Method method = throwable.getClass().getMethod("getFTLInstructionStack");
                Object result = method.invoke(throwable);
                if (result != null) {
                    message = throwable.getMessage() + " The problematic instruction: " + result;
                }
            } catch (Throwable e) {
                // ignore
            }
        }
        if (message == null)
            message = throwable.getMessage();

        exceptionXml.setMessage(message);

        if (throwable instanceof RepositoryException) {
            RepositoryException repositoryException = (RepositoryException)throwable;
            Map<String, String> state = repositoryException.getState();
            if (state != null) {
                CauseType.ExceptionData exceptionData = causeXml.addNewExceptionData();

                for (Map.Entry<String, String> entry : state.entrySet()) {
                    CauseType.ExceptionData.Parameter parameter = exceptionData.addNewParameter();
                    parameter.setName(entry.getKey());
                    parameter.setValue(entry.getValue());
                }
            }
        }

        CauseType.StackTrace stackTraceXml = causeXml.addNewStackTrace();
        StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        for (int i = 0; i < stackTraceElements.length; i++) {
            StackTraceElement stackTraceElement = stackTraceElements[i];
            CauseType.StackTrace.StackTraceElement stackTraceElementXml = stackTraceXml.addNewStackTraceElement();
            stackTraceElementXml.setClassName(stackTraceElement.getClassName());
            stackTraceElementXml.setFileName(stackTraceElement.getFileName());
            stackTraceElementXml.setLineNumber(stackTraceElement.getLineNumber());
            stackTraceElementXml.setMethodName(stackTraceElement.getMethodName());
            stackTraceElementXml.setNativeMethod(stackTraceElement.isNativeMethod());
        }

        Throwable cause = throwable.getCause();
        if (cause != null) {
            CauseType causeXml2 = causeXml.addNewCause();
            createCause(cause, causeXml2);
        }

        return causeXml;
    }

    public static String getStringParam(ServletRequest request, String name) throws Exception {
        String stringValue = request.getParameter(name);
        if (stringValue == null || stringValue.equals(""))
            throw new BadRequestException("Missing request parameter: " + name);

        return stringValue;
    }

    public static String getStringParam(ServletRequest request, String name, String defaultValue) throws Exception {
        String stringValue = request.getParameter(name);
        if (stringValue == null || stringValue.equals(""))
            return defaultValue;

        return stringValue;
    }

    public static int getIntParam(ServletRequest request, String name, int defaultValue) throws Exception {
        String stringValue = request.getParameter(name);
        if (stringValue == null || stringValue.equals(""))
            return defaultValue;

        try {
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException e) {
            throw new BadRequestException("The value of the request parameter \"" + name + "\" should be an integer value, got: " + stringValue);
        }
    }

    public static long getLongParam(ServletRequest request, String name) throws Exception {
        String stringValue = request.getParameter(name);
        if (stringValue == null || stringValue.equals(""))
            throw new BadRequestException("Missing request parameter: " + name);

        try {
            long longValue = Long.parseLong(stringValue);
            return longValue;
        } catch (NumberFormatException e) {
            throw new BadRequestException("The value of the request parameter \"" + name + "\" should be an integer value, got: " + stringValue);
        }
    }

    public static boolean getBooleanParam(ServletRequest request, String name) throws Exception {
        String stringValue = request.getParameter(name);
        if (stringValue == null || stringValue.equals(""))
            throw new BadRequestException("Missing request parameter: " + name);

        return stringValue.equalsIgnoreCase("true");
    }

    public static boolean getBooleanParam(ServletRequest request, String name, boolean defaultValue) throws Exception {
        String stringValue = request.getParameter(name);
        if (stringValue == null || stringValue.equals(""))
            return defaultValue;

        return stringValue.equalsIgnoreCase("true");
    }

    public static long getBranchId(ServletRequest request, Repository repository) throws BadRequestException, RepositoryException {
        return getBranchId(request, repository, "branch");
    }

    public static long getBranchId(ServletRequest request, Repository repository, String paramName) throws BadRequestException, RepositoryException {
        String branchParam = request.getParameter(paramName);
        if (branchParam == null || branchParam.length() == 0) {
            return Branch.MAIN_BRANCH_ID;
        } else if (Character.isDigit(branchParam.charAt(0))) {
            try {
                return Long.parseLong(branchParam);
            } catch (NumberFormatException e) {
                throw new BadRequestException("Invalid value in \"" + paramName + "\" request parameter: " + branchParam);
            }
        } else {
            try {
                return repository.getVariantManager().getBranch(branchParam, false).getId();
            } catch (BranchNotFoundException e) {
                throw new BadRequestException("Non-existing branch in \"" + paramName + "\" request parameter: " + branchParam);
            }
        }
    }

    public static long getLanguageId(ServletRequest request, Repository repository) throws BadRequestException, RepositoryException {
        return getLanguageId(request, repository, "language");
    }

    public static long getLanguageId(ServletRequest request, Repository repository, String paramName) throws BadRequestException, RepositoryException {
        String languageParam = request.getParameter(paramName);
        if (languageParam == null || languageParam.length() == 0) {
            return Language.DEFAULT_LANGUAGE_ID;
        } else if (Character.isDigit(languageParam.charAt(0))) {
            try {
                return Long.parseLong(languageParam);
            } catch (NumberFormatException e) {
                throw new BadRequestException("Invalid value in \"" + paramName + "\" request parameter: " + languageParam);
            }
        } else {
            try {
                return repository.getVariantManager().getLanguage(languageParam, false).getId();
            } catch (LanguageNotFoundException e) {
                throw new BadRequestException("Non-existing language in \"" + paramName + "\" request parameter: " + languageParam);
            }
        }
    }

    public static long getDocumentTypeId(ServletRequest request, Repository repository, String paramName) throws BadRequestException, RepositoryException {
        String param = request.getParameter(paramName);
        if (param == null || param.length() == 0) {
            throw new BadRequestException("Missing request parameter: " + paramName);
        } else if (Character.isDigit(param.charAt(0))) {
            try {
                return Long.parseLong(param);
            } catch (NumberFormatException e) {
                throw new BadRequestException("Invalid value in \"" + paramName + "\" request parameter: " + param);
            }
        } else {
            try {
                return repository.getRepositorySchema().getDocumentTypeByName(param, false).getId();
            } catch (DocumentTypeNotFoundException e) {
                throw new BadRequestException("Non-existing document type in \"" + paramName + "\" request parameter: " + param);
            }
        }
    }

    public static long parseId(String name, String value) throws BadRequestException {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid " + name + " ID: " + value);
        }
    }
}
