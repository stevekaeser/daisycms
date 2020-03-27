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
package org.outerj.daisy.navigation.impl;

import org.outerj.daisy.repository.query.QueryHelper;
import org.outerj.daisy.repository.VariantKey;

import java.util.*;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

/**
 * Resolves ${...} expressions that can be used in the attribute
 * values of some navigation nodes. These expressions are used
 * to pull values from the {@link ContextValue}s which are provided
 * by query nodes.
 */
public class ContextValuesResolver {
    public static enum Format { QUERY, URL, STRING }

    public static String resolve(String input, List<Map<String, ContextValue>> contextValues, Format format) {
        StringBuilder result = new StringBuilder(input.length());
        StringBuilder propertyBuffer = null;

        boolean inExpr = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (inExpr) {
                switch (c) {
                    case '\\':
                        if (i + 1 < input.length()) {
                            propertyBuffer.append(input.charAt(i + 1));
                            i++;
                        } else {
                            propertyBuffer.append('\\');
                        }
                        break;
                    case '}':
                        String expr = propertyBuffer.toString();
                        inExpr = false;
                        propertyBuffer = null;
                        String value = getValue(expr, contextValues, format);
                        if (value == null) {
                            result.append("${").append(expr).append("}");
                        } else {
                            result.append(value);
                        }
                        break;
                    default:
                        propertyBuffer.append(c);
                }
            } else {
                switch (c) {
                    case '\\':
                        if (i + 2 < input.length() && input.charAt(i + 1) == '$' && input.charAt(i + 2) == '{') {
                            result.append("${");
                            i += 2;
                        } else {
                            result.append(c);
                        }
                        break;
                    case '$':
                        if (i + 1 < input.length() && input.charAt(i + 1) == '{') {
                            inExpr = true;
                            propertyBuffer = new StringBuilder();
                            i++;
                        } else {
                            result.append(c);
                        }
                        break;
                    default:
                        result.append(c);
                }

            }
        }

        if (inExpr) {
            result.append("${").append(propertyBuffer);
        }

        return result.toString();
    }

    private static final String MOUNTEXPR = "../";

    private static String getValue(String expression, List<Map<String, ContextValue>> contextValues, Format format) {
        int mount = 0;
        while (expression.startsWith(MOUNTEXPR)) {
            mount++;
            expression = expression.substring(MOUNTEXPR.length());
        }

        String result;
        int index = contextValues.size() - 1 - mount;
        if (index < 0 || index >= contextValues.size()) {
            result = null;
        } else {
            ContextValue value = contextValues.get(index).get(expression);
            if (value == null) {
                result = null;
            } else {
                switch (format) {
                    case STRING:
                        result = value.getValue().toString();
                        break;
                    case QUERY:
                        switch (value.getValueType()) {
                            case DATE:
                                result = QueryHelper.formatDate((Date)value.getValue());
                                break;
                            case DATETIME:
                                result = QueryHelper.formatDateTime((Date)value.getValue());
                                break;
                            case LINK:
                                VariantKey key = (VariantKey)value.getValue();
                                result = QueryHelper.formatString("daisy:" + key.getDocumentId() + "@" + key.getBranchId() + ":" + key.getLanguageId());
                                break;
                            default:
                                result = QueryHelper.formatString(value.getValue().toString());
                        }
                        break;
                    case URL:
                        try {
                            result = URLEncoder.encode(value.getValue().toString(), "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            // UTF-8 should always be available
                            throw new RuntimeException("Unexpected UnsupportedEncodingException", e);
                        }
                        break;
                    default:
                        throw new RuntimeException("Unexpected format: " + format);
                }
            }
        }
        return result;
    }
}
