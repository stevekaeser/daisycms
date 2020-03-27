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

import org.apache.cocoon.environment.Request;
import org.outerj.daisy.repository.Repository;

public class RequestUtil {
    public static String getStringParameter(Request request, String paramName) throws Exception {
        return getStringParameter(request, paramName, null);
    }

    public static String getStringParameter(Request request, String paramName, String defaultValue) throws Exception {
        String value = request.getParameter(paramName);

        if (value == null && defaultValue == null) {
            throw new Exception("Invalid request: parameter \"" + paramName + "\" is missing.");
        } else if (value == null) {
            return defaultValue;
        } else {
            return value;
        }
    }

    public static int getIntParameter(Request request, String paramName) throws Exception {
        String value = request.getParameter(paramName);

        if (value == null) {
            throw new Exception("Invalid request: parameter \"" + paramName + "\" is missing.");
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new Exception("Invalid request: parameter \"" + paramName + "\" does not contain a valid integer value: \"" + value + "\".");
        }
    }

    public static int getIntParameter(Request request, String paramName, int defaultValue) throws Exception {
        String value = request.getParameter(paramName);

        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new Exception("Invalid request: parameter \"" + paramName + "\" does not contain a valid integer value: \"" + value + "\".");
        }
    }

    public static long getLongParameter(Request request, String paramName) throws Exception {
        String value = request.getParameter(paramName);

        if (value == null) {
            throw new Exception("Invalid request: parameter \"" + paramName + "\" is missing.");
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new Exception("Invalid request: parameter \"" + paramName + "\" does not contain a valid long value: \"" + value + "\".");
        }
    }

    public static long getLongParameter(Request request, String paramName, long defaultValue) throws Exception {
        String value = request.getParameter(paramName);

        if (value == null) {
            return defaultValue;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new Exception("Invalid request: parameter \"" + paramName + "\" does not contain a valid long value: \"" + value + "\".");
        }
    }

    public static boolean getBooleanParameter(Request request, String paramName, boolean defaultValue) throws Exception {
        String value = request.getParameter(paramName);

        if (value == null) {
            return defaultValue;
        } else {
            return value.equalsIgnoreCase("true");
        }
    }

    public static String getServer(Request request) {
        String server = request.getScheme() + "://" + request.getServerName();
        if (request.getServerPort() != 80)
            server += ":" + request.getServerPort();
        return server;
    }

    public static long getBranchId(Request request, long defaultBranchId, Repository repository) throws Exception {
        String branchParam = request.getParameter("branch");
        return getBranchId(branchParam, defaultBranchId, repository);
    }

    public static long getBranchId(String branch, long defaultBranchId, Repository repository) throws Exception {
        if (branch != null && branch.length() > 0) {
            if (Character.isDigit(branch.charAt(0))) {
                try {
                    return Long.parseLong(branch);
                } catch (NumberFormatException e) {
                    throw new Exception("Invalid branch specification: \"" + branch + "\".");
                }
            } else {
                return repository.getVariantManager().getBranch(branch, false).getId();
            }
        } else {
            return defaultBranchId;
        }
    }

    public static long getLanguageId(Request request, long defaultLanguageId, Repository repository) throws Exception {
        String languageParam = request.getParameter("language");
        return getLanguageId(languageParam, defaultLanguageId, repository);
    }

    public static long getLanguageId(String language, long defaultLanguageId, Repository repository) throws Exception {
        if (language != null && language.length() > 0) {
            if (Character.isDigit(language.charAt(0))) {
                try {
                    return Long.parseLong(language);
                } catch (NumberFormatException e) {
                    throw new Exception("Invalid language specification: \"" + language + "\".");
                }
            } else {
                return repository.getVariantManager().getLanguage(language, false).getId();
            }
        } else {
            return defaultLanguageId;
        }
    }

    /**
     * Some browser (Internet Explorer is one) provide the complete file path in
     * the upload file name, this method strips that.
     */
    public static String removePathFromUploadFileName(String fileName) {
        int pos = fileName.lastIndexOf('\\');
        if (pos == -1)
            pos = fileName.lastIndexOf('/');
        if (pos != -1)
            fileName = fileName.substring(pos + 1);
        return fileName;
    }

}
