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
package org.outerj.daisy.publisher.serverimpl.variables.impl;

import org.outerj.daisy.publisher.serverimpl.variables.Variables;
import org.outerj.daisy.xmlutil.SaxBuffer;

import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

public class MultiVariables implements Variables {
    Variables[] variables;

    public MultiVariables(Variables[] variables) {
        this.variables = variables;
    }

    public SaxBuffer resolve(String name) {
        for (Variables variables : this.variables) {
            SaxBuffer value = variables.resolve(name);
            if (value != null)
                return value;
        }
        return null;
    }

    public Map<String, SaxBuffer> getEntries() {
        if (variables.length == 0)
            return Collections.emptyMap();

        if (variables.length == 1)
            return variables[0].getEntries();

        Map<String, SaxBuffer> allVars = new HashMap<String, SaxBuffer>(variables[0].getEntries());

        for (int i = 1 ; i < variables.length; i++) {
            allVars.putAll(variables[i].getEntries());
        }

        return allVars;
    }
}
