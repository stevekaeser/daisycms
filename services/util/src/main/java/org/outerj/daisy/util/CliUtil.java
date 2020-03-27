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
package org.outerj.daisy.util;

public class CliUtil {
    public static String promptPassword(String message, String defaultPassword) throws Exception {
        char[] pwdChars = CliPasswordField.getPassword(System.in, message);
        if (pwdChars == null)
            return defaultPassword;
        else
            return new String(pwdChars);
    }

    public static String promptPassword(String message, boolean required) throws Exception {
        char[] pwdChars = null;
        if (required) {
            while (pwdChars == null) {
                pwdChars = CliPasswordField.getPassword(System.in, message);
            }
            return new String(pwdChars);
        } else {
            pwdChars = CliPasswordField.getPassword(System.in, message);
            if (pwdChars == null)
                return null;
            else
                return new String(pwdChars);
        }
    }
}
