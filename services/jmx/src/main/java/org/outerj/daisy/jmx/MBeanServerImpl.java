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
package org.outerj.daisy.jmx;

import java.io.ObjectInputStream;
import java.util.Set;
import java.util.List;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.loading.ClassLoaderRepository;
import javax.annotation.PreDestroy;

import mx4j.log.Log;
import mx4j.log.CommonsLogger;
import mx4j.tools.adaptor.http.HttpAdaptor;
import mx4j.tools.adaptor.http.XSLTProcessor;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

public class MBeanServerImpl implements MBeanServer {
    private MBeanServer delegate;

    private int httpAdaptorPort;
    private String httpAdaptorHost;
    private String httpAdaptorAuthMethod;
    private String httpAdaptorUser;
    private String httpAdaptorPassword;

    private int xmlHttpAdaptorPort;
    private String xmlHttpAdaptorHost;
    private String xmlHttpAdaptorAuthMethod;
    private String xmlHttpAdaptorUser;
    private String xmlHttpAdaptorPassword;

    private ObjectName httpAdaptorName = new ObjectName("Server:name=HttpAdaptor");
    private ObjectName xmlHttpAdaptorName = new ObjectName("Server:name=XmlHttpAdaptor");
    private ObjectName xsltProcessorName = new ObjectName("Server:name=XSLTProcessor");
    private HttpAdaptor httpAdaptor;
    private HttpAdaptor xmlHttpAdaptor;

    public MBeanServerImpl(Configuration configuration) throws Exception {
        this.configure(configuration);
        this.initialize();
        this.start();
    }

    @PreDestroy
    public void destroy() throws Exception {
        httpAdaptor.stop();
        xmlHttpAdaptor.stop();
        unregisterMBean(httpAdaptorName);
        unregisterMBean(xmlHttpAdaptorName);
        unregisterMBean(xsltProcessorName);
    }

    private void configure(Configuration configuration) throws ConfigurationException {
        Configuration httpAdaptorConf = configuration.getChild("httpAdaptor");
        httpAdaptorPort = httpAdaptorConf.getAttributeAsInteger("port");
        httpAdaptorHost = httpAdaptorConf.getAttribute("host", "localhost");
        httpAdaptorAuthMethod = httpAdaptorConf.getAttribute("authenticationMethod", "none");
        httpAdaptorUser = httpAdaptorConf.getAttribute("username", null);
        httpAdaptorPassword = httpAdaptorConf.getAttribute("password", null);

        Configuration xmlHttpAdaptorConf = configuration.getChild("xmlHttpAdaptor");
        xmlHttpAdaptorPort = xmlHttpAdaptorConf.getAttributeAsInteger("port");
        xmlHttpAdaptorHost = xmlHttpAdaptorConf.getAttribute("host", "localhost");
        xmlHttpAdaptorAuthMethod = xmlHttpAdaptorConf.getAttribute("authenticationMethod", "none");
        xmlHttpAdaptorUser = xmlHttpAdaptorConf.getAttribute("username", null);
        xmlHttpAdaptorPassword = xmlHttpAdaptorConf.getAttribute("password", null);
    }

    private void initialize() throws Exception {
        Log.redirectTo(new CommonsLogger());

        // Try to reuse an existing MBeanServer if available.
        List list = MBeanServerFactory.findMBeanServer(null);
        if (list != null && list.size() > 0) {
            delegate = (MBeanServer)list.get(0);
        } else {
            delegate = MBeanServerFactory.createMBeanServer("daisy");
        }

        {
            httpAdaptor = new HttpAdaptor();
            registerMBean(httpAdaptor, httpAdaptorName);
            configureHttpAdaptor(httpAdaptor, httpAdaptorPort, httpAdaptorHost, httpAdaptorAuthMethod,
                    httpAdaptorUser, httpAdaptorPassword);
        }

        {
            xmlHttpAdaptor = new HttpAdaptor();
            registerMBean(xmlHttpAdaptor, xmlHttpAdaptorName);
            configureHttpAdaptor(xmlHttpAdaptor, xmlHttpAdaptorPort, xmlHttpAdaptorHost, xmlHttpAdaptorAuthMethod,
                    xmlHttpAdaptorUser, xmlHttpAdaptorPassword);
        }

        XSLTProcessor xsltProcessor = new XSLTProcessor();
        registerMBean(xsltProcessor, xsltProcessorName);
        httpAdaptor.setProcessorName(xsltProcessorName);      
    }

    private void configureHttpAdaptor(HttpAdaptor httpAdaptor, int port, String host, String authMethod, String user, String password) {
        httpAdaptor.setPort(port);
        httpAdaptor.setHost(host);
        httpAdaptor.setAuthenticationMethod(authMethod);
        if (user != null) {
            httpAdaptor.addAuthorization(user, password);
        }
    }

    private void start() throws Exception {
        httpAdaptor.start();
        xmlHttpAdaptor.start();
    }

    public void addNotificationListener(ObjectName objectName, NotificationListener notificationListener, NotificationFilter notificationFilter, Object o) throws InstanceNotFoundException {
        delegate.addNotificationListener(objectName, notificationListener, notificationFilter, o);
    }

    public void addNotificationListener(ObjectName objectName, ObjectName objectName1, NotificationFilter notificationFilter, Object o) throws InstanceNotFoundException {
        delegate.addNotificationListener(objectName, objectName1, notificationFilter, o);
    }

    public void removeNotificationListener(ObjectName objectName, ObjectName objectName1) throws InstanceNotFoundException, ListenerNotFoundException {
        delegate.removeNotificationListener(objectName, objectName1);
    }

    public void removeNotificationListener(ObjectName objectName, NotificationListener notificationListener) throws InstanceNotFoundException, ListenerNotFoundException {
        delegate.removeNotificationListener(objectName, notificationListener);
    }

    public void removeNotificationListener(ObjectName objectName, ObjectName objectName1, NotificationFilter notificationFilter, Object o) throws InstanceNotFoundException, ListenerNotFoundException {
        delegate.removeNotificationListener(objectName, objectName1, notificationFilter, o);
    }

    public void removeNotificationListener(ObjectName objectName, NotificationListener notificationListener, NotificationFilter notificationFilter, Object o) throws InstanceNotFoundException, ListenerNotFoundException {
        delegate.removeNotificationListener(objectName, notificationListener, notificationFilter, o);
    }

    public MBeanInfo getMBeanInfo(ObjectName objectName) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        return delegate.getMBeanInfo(objectName);
    }

    public boolean isInstanceOf(ObjectName objectName, String s) throws InstanceNotFoundException {
        return delegate.isInstanceOf(objectName, s);
    }

    public String[] getDomains() {
        return delegate.getDomains();
    }

    public String getDefaultDomain() {
        return delegate.getDefaultDomain();
    }

    public ObjectInstance createMBean(String s, ObjectName objectName) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
        return delegate.createMBean(s, objectName);
    }

    public ObjectInstance createMBean(String s, ObjectName objectName, ObjectName objectName1) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        return delegate.createMBean(s, objectName, objectName1);
    }

    public ObjectInstance createMBean(String s, ObjectName objectName, Object[] objects, String[] strings) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
        return delegate.createMBean(s, objectName, objects, strings);
    }

    public ObjectInstance createMBean(String s, ObjectName objectName, ObjectName objectName1, Object[] objects, String[] strings) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        return delegate.createMBean(s, objectName, objectName1, objects, strings);
    }

    public void unregisterMBean(ObjectName objectName) throws InstanceNotFoundException, MBeanRegistrationException {
        delegate.unregisterMBean(objectName);
    }

    public Object getAttribute(ObjectName objectName, String s) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        return delegate.getAttribute(objectName, s);
    }

    public void setAttribute(ObjectName objectName, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        delegate.setAttribute(objectName, attribute);
    }

    public AttributeList getAttributes(ObjectName objectName, String[] strings) throws InstanceNotFoundException, ReflectionException {
        return delegate.getAttributes(objectName, strings);
    }

    public AttributeList setAttributes(ObjectName objectName, AttributeList attributeList) throws InstanceNotFoundException, ReflectionException {
        return delegate.setAttributes(objectName, attributeList);
    }

    public Object invoke(ObjectName objectName, String s, Object[] objects, String[] strings) throws InstanceNotFoundException, MBeanException, ReflectionException {
        return delegate.invoke(objectName, s, objects, strings);
    }

    public Integer getMBeanCount() {
        return delegate.getMBeanCount();
    }

    public boolean isRegistered(ObjectName objectName) {
        return delegate.isRegistered(objectName);
    }

    public ObjectInstance getObjectInstance(ObjectName objectName) throws InstanceNotFoundException {
        return delegate.getObjectInstance(objectName);
    }

    public Set queryMBeans(ObjectName objectName, QueryExp queryExp) {
        return delegate.queryMBeans(objectName, queryExp);
    }

    public Set queryNames(ObjectName objectName, QueryExp queryExp) {
        return delegate.queryNames(objectName, queryExp);
    }

    public Object instantiate(String s) throws ReflectionException, MBeanException {
        return delegate.instantiate(s);
    }

    public Object instantiate(String s, ObjectName objectName) throws ReflectionException, MBeanException, InstanceNotFoundException {
        return delegate.instantiate(s, objectName);
    }

    public Object instantiate(String s, Object[] objects, String[] strings) throws ReflectionException, MBeanException {
        return delegate.instantiate(s, objects, strings);
    }

    public Object instantiate(String s, ObjectName objectName, Object[] objects, String[] strings) throws ReflectionException, MBeanException, InstanceNotFoundException {
        return delegate.instantiate(s, objectName, objects, strings);
    }

    public ObjectInstance registerMBean(Object o, ObjectName objectName) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        return delegate.registerMBean(o, objectName);
    }

    public ObjectInputStream deserialize(String s, ObjectName objectName, byte[] bytes) throws InstanceNotFoundException, OperationsException, ReflectionException {
        return delegate.deserialize(s, objectName, bytes);
    }

    public ObjectInputStream deserialize(String s, byte[] bytes) throws OperationsException, ReflectionException {
        return delegate.deserialize(s, bytes);
    }

    public ObjectInputStream deserialize(ObjectName objectName, byte[] bytes) throws InstanceNotFoundException, OperationsException {
        return delegate.deserialize(objectName, bytes);
    }

    public ClassLoader getClassLoaderFor(ObjectName objectName) throws InstanceNotFoundException {
        return delegate.getClassLoaderFor(objectName);
    }

    public ClassLoader getClassLoader(ObjectName objectName) throws InstanceNotFoundException {
        return delegate.getClassLoader(objectName);
    }

    public ClassLoaderRepository getClassLoaderRepository() {
        return delegate.getClassLoaderRepository();
    }

}
