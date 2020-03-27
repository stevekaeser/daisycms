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
package org.outerj.daisy.query.model;

import org.outerj.daisy.util.ObjectUtils;

public class EvaluationUtil {
    public static boolean hasAny(Object value, Object[] argumentValues) {
        if (value == null)
            return false;

        Object[] values = value instanceof Object[] ? (Object[])value : null;
        for (Object argumentValue : argumentValues) {
            if (values == null) {
                if (ObjectUtils.safeEquals(argumentValue, value))
                    return true;
            } else {
                for (Object aValue : values)
                    if (ObjectUtils.safeEquals(argumentValue, aValue))
                        return true;
            }
        }
        return false;
    }

    public static boolean hasExactly(Object[] values, Object[] argumentValues) {
        if (values == null)
            return false;

        if (values.length != argumentValues.length)
            return false;

        boolean[] matches = new boolean[values.length];
        for (Object argumentValue : argumentValues) {
            boolean valueFound = false;
            for (int i = 0; i < values.length; i++) {
                if (!matches[i] && values[i].equals(argumentValue)) {
                    valueFound = true;
                    matches[i] = true;
                    break;
                }
            }
            if (!valueFound)
                return false;
        }
        return true;
    }

    public static boolean hasAll(Object[] values, Object[] argumentValues) {
        if (values == null)
            return false;

        for (Object argumentValue : argumentValues) {
            boolean valueFound = false;
            for (Object value : values) {
                if (value.equals(argumentValue)) {
                    valueFound = true;
                    break;
                }
            }
            if (!valueFound)
                return false;
        }
        return true;
    }
}
