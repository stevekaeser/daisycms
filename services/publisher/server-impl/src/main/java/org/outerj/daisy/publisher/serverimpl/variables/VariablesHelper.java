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
package org.outerj.daisy.publisher.serverimpl.variables;

import org.outerj.daisy.xmlutil.SaxBuffer;

public class VariablesHelper {

    public static String substituteVariables(String text, Variables variables) {
        // This code should not fail on "errors" such as unclosed variable declarations,
        // since the variables are entered by users and shouldn't cause document display
        // to fail

        boolean inProp = false;
        boolean didSubstitution = false;

        StringBuilder result = new StringBuilder(text.length());
        StringBuilder varName = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '$':
                    if (inProp) {
                        varName.append(c);
                    } else if (i + 1 < text.length() && text.charAt(i + 1) == '{') {
                        inProp = true;
                        i++;
                    } else if (i + 2 < text.length() && text.charAt(i + 1) == '$' && text.charAt(i + 2) == '{') {
                        // escaped ${ using $${
                        result.append(c);
                        i += 2;
                    } else {
                        result.append(c);
                    }
                    break;
                case '}':
                    if (inProp) {
                        inProp = false;
                        SaxBuffer varValue = variables.resolve(varName.toString());
                        if (varValue != null) {
                            result.append(varValue.toString());
                        } else {
                            result.append("${").append(varName).append("}");
                        }
                        varName.setLength(0);
                        didSubstitution = true;
                    } else {
                        result.append(c);
                    }
                    break;
                default:
                    if (inProp) {
                        varName.append(c);
                    } else {
                        result.append(c);
                    }
            }
        }

        if (!didSubstitution)
            return null;

        if (inProp) {
            result.append("${").append(varName);
        }

        return result.toString();
    }
}
