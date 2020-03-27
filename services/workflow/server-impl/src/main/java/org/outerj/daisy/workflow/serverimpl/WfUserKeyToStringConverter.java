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
package org.outerj.daisy.workflow.serverimpl;

import org.jbpm.context.exe.Converter;
import org.outerj.daisy.workflow.WfUserKey;

/**
 * Converter for JBPM to map WfUserKeys to strings and vice versa
 * for storage resp. retrieval to/from the database.
 *
 * <p>See also jbpm.varmapping.xml and jbpm.converter.properties.
 */
public class WfUserKeyToStringConverter  implements Converter {

    public boolean supports(Object value) {
        if (value == null)
            return true;
        return value.getClass() == WfUserKey.class;
    }

    public Object convert(Object o) {
        WfUserKey userKey = (WfUserKey)o;
        return String.valueOf(userKey.getId());
    }

    public Object revert(Object o) {
        String input = (String)o;
        try {
            long id = Long.parseLong(input);
            return new WfUserKey(id);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid ID for user in string representation from database: " + input);
        }
    }
}
