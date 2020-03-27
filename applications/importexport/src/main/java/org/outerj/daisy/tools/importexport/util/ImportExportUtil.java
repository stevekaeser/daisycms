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
package org.outerj.daisy.tools.importexport.util;

import org.outerj.daisy.util.VersionHelper;
import org.outerj.daisy.xmlutil.DocumentHelper;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.w3c.dom.Element;

import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ImportExportUtil {
    public static Properties getVersionProperties() throws IOException {
        return VersionHelper.getVersionProperties(ImportExportUtil.class.getClassLoader(), "org/outerj/daisy/tools/importexport/versioninfo.properties");
    }

    public static void logError(Throwable throwable, String baseFileName, PrintStream out) {
        out.println();
        out.println("*** An error occured ***");
        out.println();

        Throwable current = throwable;
        do {
            out.println(current.getMessage());
            current = current.getCause();
        } while (current != null);
        out.println();

        try {
            File logFile;
            SimpleDateFormat dateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance();
            dateFormat.applyPattern("yyyyMMddHHmmss");
            String baseName = baseFileName + "_" + dateFormat.format(new Date());
            int c = 1;
            while (true) {
                File file = new File(baseName + c + ".txt");
                if (file.createNewFile()) {
                    logFile = file;
                    break;
                }
                c++;
            }

            FileOutputStream fos = new FileOutputStream(logFile);
            PrintWriter writer = new PrintWriter(fos);
            throwable.printStackTrace(writer);
            writer.close();
            fos.close();
            out.println("Error details written to " + logFile.getAbsolutePath());
            out.println();
        } catch (Throwable e) {
            // Something fails writing the error to a file, just dump it on standard out then
            throwable.printStackTrace(System.out);
        }
    }

    public static Object useFactory(Element element, Repository repository) throws Exception {
        String factoryClassName = DocumentHelper.getAttribute(element, "factoryClass", true);
        Class factoryClass = ImportExportUtil.class.getClassLoader().loadClass(factoryClassName);
        Method createMethod = factoryClass.getMethod("create", Element.class, Repository.class);
        if (!Modifier.isStatic(createMethod.getModifiers())) {
            throw new Exception("The create method of class " + factoryClassName + " is not static.");
        }
        return createMethod.invoke(null, element, repository);
    }

    public static long[] parseRoles(String roles, Repository repository) throws RepositoryException {
        List<Long> roleIdList = new ArrayList<Long>(5);
        StringTokenizer tokenizer = new StringTokenizer(roles, ",");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();
            if (token.length() > 0) {
                roleIdList.add(repository.getUserManager().getRole(token, false).getId());
            }
        }
        
        long[] roleIds = new long[roleIdList.size()];
        for (int i = 0; i < roleIds.length; i++) {
            roleIds[i] = roleIdList.get(i);
        }
        return roleIds;
    }
}
