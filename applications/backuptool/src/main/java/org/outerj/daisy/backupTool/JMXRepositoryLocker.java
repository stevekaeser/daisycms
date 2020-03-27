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
package org.outerj.daisy.backupTool;

import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Document;

public class JMXRepositoryLocker {

    private final static long SUSPEND_GRACE_TIME = 60000; // one minute

    private final static String LOCK_QUERY = "operation=lock&objectname=Daisy:name=BackupLocker&value0=" + SUSPEND_GRACE_TIME + "&type0=long";

    private final static String UNLOCK_QUERY = "operation=unlock&objectname=Daisy:name=BackupLocker";

    private final static String STATUS_QUERY = "objectname=Daisy:name=BackupLocker";
    
    private final static String VERSION_QUERY = "objectname=Daisy:name=SystemInfo";

    private final static String XPATH_ISLOCKED = "/MBean[@objectname = 'Daisy:name=BackupLocker']/Attribute[@name = 'Locked']/@value";

    private final static String XPATH_METHOD_STATUS = "/MBeanOperation/Operation/@result";

    private final static String XPATH_METHOD_ERRORMSG = "/MBeanOperation/Operation/@errorMsg";
    
    private final static String XPATH_VERSION = "/MBean[@objectname = 'Daisy:name=SystemInfo']/Attribute[@name = 'ServerVersion']/@value";

    private String username;

    private String password;

    private String host;

    private int port;

    private Date startLock;

    private Date stopLock;

    private DocumentBuilder documentbuilder;

    public JMXRepositoryLocker(String host, int port, String username, String password) throws Exception {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentbuilder = documentBuilderFactory.newDocumentBuilder();
    }

    public void lock() throws Exception {
        startLock = new Date();
        stopLock = null;
        Document statusDocument = queryJmx("invoke", LOCK_QUERY);
        if (!successfulInvocation(statusDocument))
            throw createException(statusDocument);
    }

    public void unlock() throws Exception {
        stopLock = new Date();
        Document statusDocument = queryJmx("invoke", UNLOCK_QUERY);
        if (!successfulInvocation(statusDocument))
            throw createException(statusDocument);
    }

    public boolean isLocked() throws Exception {
        Document resultDocument = queryJmx("mbean", STATUS_QUERY);
        String result = BackupHelper.getStringFromDom(resultDocument, XPATH_ISLOCKED);
        return Boolean.valueOf(result).booleanValue();
    }

    public long getLockTime() {
        long time = -1;
        if (stopLock != null && startLock != null)
            time = stopLock.getTime() - startLock.getTime();
        return time;
    }
    
    public String getServerVersion() throws Exception{
        Document result = queryJmx("mbean", VERSION_QUERY);
        return BackupHelper.getStringFromDom(result, XPATH_VERSION);        
    }

    private Exception createException(Document doc) throws Exception {
        return new Exception(BackupHelper.getStringFromDom(doc, XPATH_METHOD_ERRORMSG));
    }

    private Document queryJmx(String method, String query) throws Exception {
        Document document = null;

        HttpClient httpClient = new HttpClient();
        httpClient.getParams().setCredentialCharset("ISO-8859-1");
        HttpState state = new HttpState();
        state.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        httpClient.setState(state);
        HostConfiguration hostConfiguration = new HostConfiguration();
        hostConfiguration.setHost(host, port);
        httpClient.setHostConfiguration(hostConfiguration);

        HttpMethod getMethod = new GetMethod("/" + method);
        getMethod.setQueryString(query);
        try {
            httpClient.executeMethod(getMethod);
            if (getMethod.getStatusCode() != 200)
                throw new Exception("Failed to connect to JMX: HTTP response code : " + getMethod.getStatusCode() + " : " + getMethod.getStatusText());
            document = documentbuilder.parse(getMethod.getResponseBodyAsStream());
        } finally {
            getMethod.releaseConnection();
        }
        return document;
    }

    private boolean successfulInvocation(Document doc) throws Exception {
        return BackupHelper.getStringFromDom(doc, XPATH_METHOD_STATUS).equals("success");
    }

    public String getRepositoryHost() {
        return host;
    }

    public int getRepositoryPort() {
        return port;
    }
}