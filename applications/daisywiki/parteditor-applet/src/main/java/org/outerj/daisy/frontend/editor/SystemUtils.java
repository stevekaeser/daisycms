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
package org.outerj.daisy.frontend.editor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that uses the Java 6 Desktop API by using reflection (so that it can be compiled using Java 5)  
 */
public class SystemUtils {
    
    private static final Pattern envVar = Pattern.compile("%([^%]*)%");
    
    public static String replaceEnvironmentVariables(String input) {
        Matcher m = envVar.matcher(input);
        StringBuffer newPath = new StringBuffer();
        
        while (m.find()) {
            String replacement = System.getenv(m.group(1));
            
            if (replacement == null) {
                m.appendReplacement(newPath, Matcher.quoteReplacement(m.group()));
            } else {
                m.appendReplacement(newPath, Matcher.quoteReplacement(replacement));
            }
        }

        m.appendTail(newPath);

        return newPath.toString();
    }

}
