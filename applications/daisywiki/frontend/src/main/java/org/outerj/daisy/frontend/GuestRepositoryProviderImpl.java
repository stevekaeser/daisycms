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

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryImpl;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.thread.ThreadSafe;

/**
 * This component serves as a quick factory for guest repository objects.
 * The Guest Repository object is requested only once from the RepositoryManager,
 * after which it is cloned each time needed.
 */
public class GuestRepositoryProviderImpl implements GuestRepositoryProvider, Serviceable, Initializable, Configurable, ThreadSafe {
    
    private final static String defaultGuestLogin = "guest";
    private final static String defaultGuestPassword = "guest"; 
    
    private ServiceManager serviceManager;
    private Repository templateRepository;
    private Credentials credentials;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }
    
    public void configure(Configuration configuration) throws ConfigurationException {
        Configuration guestUser = configuration.getChild("guestUser", true);
        credentials = new Credentials(guestUser.getAttribute("login", defaultGuestLogin),
                guestUser.getAttribute("password", defaultGuestPassword));
    }

    public void initialize() throws Exception {
        RepositoryManager repositoryManager = (RepositoryManager)serviceManager.lookup("daisy-repository-manager");
        try {
            templateRepository = repositoryManager.getRepository(credentials);
        } finally {
            serviceManager.release(repositoryManager);
        }
    }

    public Repository getGuestRepository() {
        return (Repository)((RemoteRepositoryImpl)templateRepository).clone();
    }
}
