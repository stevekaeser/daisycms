/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.sync;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.web.servlet.DispatcherServlet;

public class Main {

    /**
     * @param args
     */
    public static void main(String[] args) {
        Properties props;

        String syncConf = System.getProperty("sync.conf", ".");
        File syncConfDir = new File(syncConf);
        File appProperties = new File(syncConfDir, "sync.properties");
        if (!appProperties.exists()) {
            System.out.println(appProperties.getAbsolutePath()+"/sync.properties not found -- did you set sync.conf?");
        } else if (!appProperties.canRead()){
            throw new RuntimeException("Cannot read " + appProperties.getAbsolutePath());
        }
        try {
            props = PropertiesLoaderUtils.loadProperties(new FileSystemResource(appProperties));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int port = Integer.parseInt(props.getProperty("sync.port", "8488"));
        String host = props.getProperty("sync.host", "localhost");
        Server server = new Server();
        Connector connector = new SocketConnector();
        connector.setHost(host);
        connector.setPort(port);
        server.addConnector(connector);

        Context root = new Context(server, "/", Context.SESSIONS);
        ServletHolder holder = new ServletHolder(new DispatcherServlet());
        root.addServlet(holder, "/*");

        holder.setInitParameter("contextConfigLocation", "classpath:/org/outerj/daisy/sync/applicationContext.xml");

        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException("Could not start sync server", e);
        }
    }

}
