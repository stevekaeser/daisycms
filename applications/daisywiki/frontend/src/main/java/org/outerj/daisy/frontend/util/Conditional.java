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
package org.outerj.daisy.frontend.util;

/**
 * This is an utility class to enable conditional expressions in JXTemplate.
 */
public class Conditional {
    public static String when(Object expr, Object trueValue, Object falseValue) {
        if (expr != null && expr instanceof Boolean) {
            return Boolean.TRUE.equals(expr) ? trueValue.toString() : falseValue.toString();
        } else if (expr != null && expr instanceof String) {
            return "true".equalsIgnoreCase((String)expr) ? trueValue.toString() : falseValue.toString();
        } else {
            return falseValue.toString();
        }
    }
}
