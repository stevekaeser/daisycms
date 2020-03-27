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
import org.outerj.daisy.workflow.WfActorKey;

import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;

/**
 * Converter for JBPM to map WfActorKeys to strings and vice versa
 * for storage resp. retrieval to/from the database.
 *
 * <p>See also jbpm.varmapping.xml and jbpm.converter.properties.
 */
public class WfActorKeyToStringConverter  implements Converter {
    private static final String USER_PREFIX = "user-";
    private static final String POOL_PREFIX = "pool-";

    public boolean supports(Object value) {
        if (value == null)
            return true;
        return value.getClass() == WfActorKey.class;
    }

    public Object convert(Object o) {
        WfActorKey actorKey = (WfActorKey)o;

        if (actorKey.isPool()) {
            // Since the result is stored in a limited-length database column,
            // this will only work for list of pools to a certain size, though
            // for normal use-cases these limits should never be reached.
            StringBuilder result = new StringBuilder(20);
            result.append(POOL_PREFIX);
            List<Long> poolIds = actorKey.getPoolIds();
            for (int i = 0; i < poolIds.size(); i++) {
                if (i > 0)
                    result.append(",");
                result.append(poolIds.get(i));
            }
            return result.toString();
        } else {
            return USER_PREFIX + actorKey.getUserId();
        }
    }

    public Object revert(Object o) {
        String input = (String)o;
        long id;
        if (input.startsWith(USER_PREFIX)) {
            try {
                id = Long.parseLong(input.substring(USER_PREFIX.length()));
                return new WfActorKey(id);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid ID in actor key in string representation from database: " + input);
            }
        } else if (input.startsWith(POOL_PREFIX)) {
            String poolIdsString = input.substring(POOL_PREFIX.length());
            List<Long> poolIdsList = new ArrayList<Long>(3);
            StringTokenizer tokenizer = new StringTokenizer(poolIdsString, ",");
            try {
                while (tokenizer.hasMoreTokens()) {
                    poolIdsList.add(Long.parseLong(tokenizer.nextToken()));
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid ID in actor key in string representation from database: " + input);
            }
            return new WfActorKey(poolIdsList);
        } else {
            throw new RuntimeException("Invalid actor key in string representation from database: " + input);
        }
    }
}

