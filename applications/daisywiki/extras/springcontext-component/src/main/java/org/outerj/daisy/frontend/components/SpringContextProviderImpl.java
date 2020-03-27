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
package org.outerj.daisy.frontend.components;

import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.outerj.daisy.configutil.PropertyResolver;
import org.outerj.daisy.frontend.util.WikiPropertiesHelper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class SpringContextProviderImpl extends AbstractLogEnabled implements Configurable, ThreadSafe, SpringContextProvider, Serviceable, Contextualizable, Disposable {
    private Map<String, AbstractApplicationContext> contextMap;
    private ServiceManager serviceManager;
    private Context context;

    public void service(ServiceManager serviceManager) {
		this.serviceManager = serviceManager;
	}
    
    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

	public void configure(Configuration configuration) throws ConfigurationException {
        contextMap = new HashMap<String, AbstractApplicationContext>();
        Configuration[] configs = configuration.getChildren();
        ServiceManagerHolder.set(serviceManager);
        for (Configuration config : configs) {
            try {
                String path = PropertyResolver.resolveProperties(config.getAttribute("src"), WikiPropertiesHelper.getResolveProperties(context));
                if (config.getName().equals("fileContext")) {                     
                    AbstractApplicationContext ctx = new FileSystemXmlApplicationContext(path);
                    String key = config.getAttribute("name");
                    contextMap.put(key, ctx);
                } else if (config.getName().equals("classpathContext")) {
                    AbstractApplicationContext ctx = new ClassPathXmlApplicationContext(path);
                    String key = config.getAttribute("name");
                    contextMap.put(key, ctx);
                } else {
                    this.getLogger().warn("Can not handle configuration of type " + config.getName());
                }
            } catch (Exception e) {
                this.getLogger().warn("Could not load spring application context:" + e.getMessage(), e);
            }
        }
        ServiceManagerHolder.set(null);
    }

	public void dispose() {
	    for (AbstractApplicationContext ctx : contextMap.values()) {
            ctx.close();
        }
	}
	
    public Object getBean(String contextName, String beanName) throws Exception{
        ApplicationContext ctx = contextMap.get(contextName);
        if (ctx == null)
            throw new Exception("Could not find a context with name " + contextName);
        
        Object bean = ctx.getBean(beanName);
        if (bean instanceof Serviceable) {
        	((Serviceable)bean).service(serviceManager);
        }
		return bean;
    }

}
