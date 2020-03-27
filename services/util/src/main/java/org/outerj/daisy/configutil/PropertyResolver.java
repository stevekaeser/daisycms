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
package org.outerj.daisy.configutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PropertyResolver {
    
    private static final String DAISY_ENV_PROP = "daisy.env";
    
    private static final Properties baseProperties = new Properties();
    
    private static final Log log = LogFactory.getLog(PropertyResolver.class);
    
    static {
        baseProperties.putAll(System.getProperties());
        String envFile = baseProperties.getProperty(DAISY_ENV_PROP);
        if (envFile != null) {
            try {
                Properties envProps = new Properties();
                envProps.load(new FileInputStream(envFile));
                baseProperties.putAll(envProps);
            } catch (FileNotFoundException e) {
                log.error("Environment properties were specified, but the property file was not found ("+DAISY_ENV_PROP+"="+envFile);
            } catch (IOException e) {
                log.error("Failed to read environment properties file ("+DAISY_ENV_PROP+"="+envFile);
            }
        }
    }
    
    public static Properties getBaseProperties() {
        return baseProperties;
    }
    
    public static String resolveProperties(String input) {
        return resolveProperties(input, baseProperties);
    }

    /**
     * Resolves properties in the input string referenced using
     * ${property} syntax. Special 'function' properties are also
     * supported, which use a syntax like ${url-encode:something}.
     * Nested syntax is also supported, e.g.
     * ${property and ${anotherproperty}}, which is mostly useful
     * in combination with the function properties like 'url-encode:'.
     * Recursive property evaluation is also supported, but will
     * stop resolving when a cycle is detected:
     * Given a=${b}x, b=${c}y and c=${a}z, resolving "${a}" will result in "${a}zyx"
     */
    public static String resolveProperties(String input, Properties properties) {
        return resolveProperties(input, properties, new Stack<String>());
    }
    
    public static String resolveProperties(String input, Properties properties, Stack<String> propNameStack) {
        if (input == null) return null;
        StringBuilder result = new StringBuilder(input.length());
        StringBuilder propertyBuffer = null;
        Stack<StringBuilder> openPropertyBuffers = new Stack<StringBuilder>();

        final int STATE_DEFAULT = 0;
        final int STATE_IN_PROP = 1;
        int state = STATE_DEFAULT;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '$':
                    if (i + 1 < input.length() && input.charAt(i + 1) == '{') {
                        if (state == STATE_IN_PROP) {
                            openPropertyBuffers.push(propertyBuffer);
                        }
                        i++;
                        state = STATE_IN_PROP;
                        propertyBuffer = new StringBuilder();
                    } else {
                        (state == STATE_IN_PROP ? propertyBuffer : result).append("$");
                    }
                    break;
                case '}':
                    if (state == STATE_IN_PROP) {
                        String propName = propertyBuffer.toString();
                        String propValue = null;
                        if (!propNameStack.contains(propName)) {
                            // recursive property names detected -> stop recursive resolving.
                            propNameStack.push(propName);
                            propValue = evaluateProperty(propName, properties);
                            propValue = resolveProperties(propValue, properties, propNameStack);
                            propNameStack.pop();
                        }
                        String propEvalResult = propValue != null ? propValue : "${" + propName + "}";
                        if (!openPropertyBuffers.empty()) {
                            propertyBuffer = (StringBuilder)openPropertyBuffers.pop();
                            propertyBuffer.append(propEvalResult);
                            // stay in STATE_IN_PROP
                        } else {
                            result.append(propEvalResult);
                            state = STATE_DEFAULT;
                        }
                    } else {
                        result.append(c);
                    }
                    break;
                default:
                    (state == STATE_IN_PROP ? propertyBuffer : result).append(c);
            }
        }

        if (state == STATE_IN_PROP) {
            // process any property buffers still open
            do {
                if (!openPropertyBuffers.empty()) {
                    propertyBuffer = ((StringBuilder)openPropertyBuffers.pop()).append("${").append(propertyBuffer);
                } else {
                    result.append("${").append(propertyBuffer);
                    propertyBuffer = null;
                }
            } while (propertyBuffer != null);
        }

        return result.toString();
    }

    private static Pattern PROP_PATTERN = Pattern.compile("^([^:]+):(.+)$");

    /**
     * Evaluates properties containing special syntax.
     *
     * <p>Allows for things like ${url-encode:propname}.
     */
    private static String evaluateProperty(String input, Properties properties) {
        Matcher matcher = PROP_PATTERN.matcher(input);
        if (matcher.matches()) {
            String action = matcher.group(1);
            String value = matcher.group(2);

            try {
                if (action.equals("url-encode")) {
                    return URLEncoder.encode(value, "UTF-8");
                } else if (action.equals("double-url-encode")) {
                    return URLEncoder.encode(URLEncoder.encode(value, "UTF-8"), "UTF-8");
                } else if (action.equals("tripple-url-encode")) {
                    return URLEncoder.encode(URLEncoder.encode(URLEncoder.encode(value, "UTF-8"), "UTF-8"), "UTF-8");
                } else if (action.equals("uri-path-encode")) {
                    try {
                        return new URI(null, null, value, null).getRawSchemeSpecificPart();
                    } catch (URISyntaxException e) {
                        throw new RuntimeException("Error in uri-path-encode function of property resolver.", e);
                    }
                } else if (action.equals("file-to-uri")) {
                    return new File(value).toURI().toString();
                } else {
                    return null;
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        } else {
            return properties.getProperty(input);
        }
    }
}
