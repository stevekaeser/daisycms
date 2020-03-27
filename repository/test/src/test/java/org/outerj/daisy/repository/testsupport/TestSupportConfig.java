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
package org.outerj.daisy.repository.testsupport;

import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;

public class TestSupportConfig extends Properties {
    private static final String DEFAULTPROPS = "testsupport.properties";
    private static final String USERPROPS = "local.testsupport.properties";

    public TestSupportConfig() throws Exception {
        File defaultProps = new File(DEFAULTPROPS);
        if (defaultProps.exists()) {
            load(new FileInputStream(defaultProps));
            System.out.println("Finished reading properties from " + DEFAULTPROPS);
        } else {
            System.out.println("Did not find properties file " + DEFAULTPROPS);
        }

        File userProps = new File(USERPROPS);
        if (userProps.exists()) {
            load(new FileInputStream(userProps));
            System.out.println("Finished reading properties from " + USERPROPS);
        } else {
            System.out.println("Did not find optional properties file " + USERPROPS);
        }
    }

    public String getRequiredProperty(String key) throws Exception {
        String value = getProperty(key);
        if (value == null)
            throw new Exception("Missing property " + key + ".");
        return value;
    }

}
