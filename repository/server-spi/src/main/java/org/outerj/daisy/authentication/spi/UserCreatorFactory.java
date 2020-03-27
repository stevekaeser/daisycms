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
package org.outerj.daisy.authentication.spi;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

/**
 * Helper class to support auto-user-creation.
 */
public class UserCreatorFactory {
    public static UserCreator createUser(Configuration config, String scheme) throws ConfigurationException {
        Configuration createUserConf = config.getChild("autoCreateUser", false);
        if (createUserConf != null) {
            Configuration[] rolesConf = createUserConf.getChild("roles").getChildren("role");
            String[] roles = new String[rolesConf.length];
            for (int i = 0; i < rolesConf.length; i++) {
                roles[i] = rolesConf[i].getValue();
            }
            String defaultRole = createUserConf.getChild("defaultRole").getValue(null);
            boolean updateableByUser = createUserConf.getChild("updateableByUser").getValueAsBoolean();
            return new UserCreator(roles, defaultRole, updateableByUser, scheme);
        }
        return null;
    }
}
