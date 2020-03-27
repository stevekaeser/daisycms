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
package org.outerj.daisy.runtime.component;

import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.outerj.daisy.runtime.DaisyRTException;

public class DaisyRuntimeNamespaceHandler implements NamespaceHandler {

    public void init() {
    }

    public BeanDefinition parse(Element element, ParserContext parserContext) {
        if (element.getLocalName().equals("import-service")) {
            try {
                String service = element.getAttribute("service");
                Class serviceClass = parserContext.getReaderContext().getBeanClassLoader().loadClass(service);
                Object component = ContainerConfigImpl.BUILD_CONTEXT_RUNTIME.get().getService(serviceClass);

                String id = element.getAttribute("id");
                RootBeanDefinition def = new RootBeanDefinition(ServiceImportFactoryBean.class);

                ConstructorArgumentValues args = new ConstructorArgumentValues();
                args.addIndexedArgumentValue(0, serviceClass);
                args.addIndexedArgumentValue(1, component);

                def.setConstructorArgumentValues(args);
                def.setLazyInit(false);
                parserContext.getRegistry().registerBeanDefinition(id, def);
            } catch (Throwable e) {
                throw new DaisyRTException("Error handling import-service directive.", e);
            }
        } else if (element.getLocalName().equals("export-service")) {
            try {
                String service = element.getAttribute("service");
                Class serviceClass = parserContext.getReaderContext().getBeanClassLoader().loadClass(service);

                String beanName = element.getAttribute("ref");

                ContainerConfigImpl.BUILD_CONTEXT_EXPORTS.get().put(serviceClass, beanName);
            } catch (Throwable e) {
                throw new DaisyRTException("Error handling import-service directive.", e);
            }
        }
        return null;
    }

    public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder beanDefinitionHolder, ParserContext parserContext) {
        return null;
    }
}
