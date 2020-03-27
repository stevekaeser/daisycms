/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.backupTool.dbDump;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DbDumperFactory {
    public static DbDumper createDbDumper(String dburl, String username, String password) throws Exception {
        Pattern urlPattern = Pattern.compile("jdbc:(.*)://(([^\\?:]+)(:(\\d*))?)/([^\\?:]+)(\\?.+)?");
        Matcher urlMatcher = urlPattern.matcher(dburl);

        if (!urlMatcher.matches())
            throw new Exception("Please verify that the database url is correct " + dburl);

        String dbType = urlMatcher.group(1);
        String dbName = urlMatcher.group(6);
        Integer port = new Integer(urlMatcher.group(5) == null ? "0" : urlMatcher.group(5));
        String hostName = urlMatcher.group(3);

        dbType = dbType.substring(0, 1).toUpperCase() + dbType.substring(1);

        Class dumperClazz = Class.forName(MysqlDbDumper.class.getPackage().getName() + "." + dbType + "DbDumper");

        Object[] valueParams = new Object[] { dbName, hostName, port, password, username };
        Class[] classParams = new Class[] { String.class, String.class, Integer.class, String.class, String.class };

        AbstractDbDumper dbDumper = (AbstractDbDumper) dumperClazz.getConstructor(classParams).newInstance(valueParams);

        return dbDumper;
    }
}