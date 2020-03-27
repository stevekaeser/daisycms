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
package org.outerj.daisy.tools.importexport.util;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.xmlbeans.GDate;
import org.apache.xmlbeans.GDateBuilder;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.util.Constants;

public class XmlizerUtil {
    public static String escape(String text) {
        StringBuilder result = new StringBuilder(text.length() + 10);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '<':
                    result.append("&lt;");
                    break;
                case '&':
                    result.append("&amp;");
                    break;
                default:
                    result.append(c);
            }
        }
        return result.toString();
    }

    public static String escapeAttr(String text) {
        StringBuilder result = new StringBuilder(text.length() + 10);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '<':
                    result.append("&lt;");
                    break;
                case '"':
                    result.append("&quot;");
                    break;
                case '&':
                    result.append("&amp;");
                    break;
                default:
                    result.append(c);
            }
        }
        return result.toString();
    }

    public static String spaces(int count) {
        if (count == 2) {
            return "  ";
        } else if (count == 4) {
            return "    ";
        } else if (count == 6) {
            return "      ";
        } else if (count == 8) {
            return "        ";
        } else {
            StringBuilder spaces = new StringBuilder(count);
            for (int i = 0; i < count; i++)
                spaces.append(' ');
            return spaces.toString();
        }
    }

    public static String formatValue(Object value, ValueType valueType, Repository repository) {
        ValueFormatter formatter = VALUE_FORMATTERS.get(valueType);
        if (formatter == null)
            throw new RuntimeException("No value formatter found for value type " + valueType);
        return formatter.format(value, repository);
    }

    static interface ValueFormatter {
        String format(Object value, Repository repository);
    }

    private static Map<ValueType, ValueFormatter> VALUE_FORMATTERS = new HashMap<ValueType, ValueFormatter>();
    static {
        VALUE_FORMATTERS.put(ValueType.STRING,
                new ValueFormatter() {
                    public String format(Object value, Repository repository) {
                        return (String)value;
                    }
                });
        VALUE_FORMATTERS.put(ValueType.BOOLEAN,
                new ValueFormatter() {
                    public String format(Object value, Repository repository) {
                        return value.toString();
                    }
                });
        VALUE_FORMATTERS.put(ValueType.LONG,
                new ValueFormatter() {
                    public String format(Object value, Repository repository) {
                        return value.toString();
                    }
                });
        VALUE_FORMATTERS.put(ValueType.DOUBLE,
                new ValueFormatter() {
                    public String format(Object value, Repository repository) {
                        return value.toString();
                    }
                });
        VALUE_FORMATTERS.put(ValueType.DECIMAL,
                new ValueFormatter() {
                    public String format(Object value, Repository repository) {
                        return value.toString();
                    }
                });
        VALUE_FORMATTERS.put(ValueType.DATE,
                new ValueFormatter() {
                    public String format(Object value, Repository repository) {
                        GDateBuilder gdb = new GDateBuilder((Date)value);
                        gdb.clearTime();
                        gdb.clearTimeZone();
                        return gdb.toString();
                    }
                });
        VALUE_FORMATTERS.put(ValueType.DATETIME,
                new ValueFormatter() {
                    public String format(Object value, Repository repository) {
                        GDateBuilder gdb = new GDateBuilder((Date)value);
                        gdb.normalizeToTimeZone(0);
                        return gdb.toString();
                    }
                });
        VALUE_FORMATTERS.put(ValueType.LINK,
                new ValueFormatter() {
                    public String format(Object value, Repository repository) {
                        VariantKey variantKey = (VariantKey)value;
                        StringBuilder link = new StringBuilder(20);
                        link.append("daisy:").append(variantKey.getDocumentId());
                        try {
                            if (variantKey.getBranchId() != -1) {
                                String branch = repository.getVariantManager().getBranch(variantKey.getBranchId(), false).getName();
                                link.append("@").append(branch);
                            }
                            if (variantKey.getLanguageId() != -1) {
                                link.append(variantKey.getBranchId() == -1 ? "@:" : ":");
                                String language = repository.getVariantManager().getLanguage(variantKey.getLanguageId(), false).getName();
                                link.append(language);
                            }
                        } catch (RepositoryException e) {
                            throw new RuntimeException("Error formatting link for " + variantKey, e);
                        }
                        return link.toString();
                    }
                });
    }

    public static Object parseValue(ValueType valueType, String value, Repository repository) {
        ValueParser parser = VALUE_PARSERS.get(valueType);
        if (parser == null)
            throw new RuntimeException("No value parser found for value type " + valueType);
        return parser.parse(value, repository);
    }

    interface ValueParser {
        Object parse(String value, Repository repository);
    }

    private static Map<ValueType, ValueParser> VALUE_PARSERS = new HashMap<ValueType, ValueParser>();
    static {
        VALUE_PARSERS.put(ValueType.STRING,
                new ValueParser() {
                    public Object parse(String value, Repository repository) {
                        return value;
                    }
                });
        VALUE_PARSERS.put(ValueType.BOOLEAN,
                new ValueParser() {
                    public Object parse(String value, Repository repository) {
                        return Boolean.valueOf(value);
                    }
                });
        VALUE_PARSERS.put(ValueType.LONG,
                new ValueParser() {
                    public Object parse(String value, Repository repository) {
                        return new Long(Long.parseLong(value));
                    }
                });
        VALUE_PARSERS.put(ValueType.DOUBLE,
                new ValueParser() {
                    public Object parse(String value, Repository repository) {
                        return new Double(Double.parseDouble(value));
                    }
                });
        VALUE_PARSERS.put(ValueType.DECIMAL,
                new ValueParser() {
                    public Object parse(String value, Repository repository) {
                        return new BigDecimal(value);
                    }
                });
        VALUE_PARSERS.put(ValueType.DATE,
                new ValueParser() {
                    public Object parse(String value, Repository repository) {
                        return new GDate(value).getDate();
                    }
                });
        VALUE_PARSERS.put(ValueType.DATETIME,
                new ValueParser() {
                    public Object parse(String value, Repository repository) {
                        return new GDate(value).getDate();
                    }
                });
        VALUE_PARSERS.put(ValueType.LINK,
                new ValueParser() {
                    public Object parse(String value, Repository repository) {
                        Matcher matcher = Constants.DAISY_LINK_PATTERN.matcher(value);
                        if (matcher.matches()) {
                            String documentId = matcher.group(1);
                            String branch = matcher.group(2);
                            String lang = matcher.group(3);
                            long branchId = -1;
                            long langId = -1;
                            try {
                                if (branch != null && branch.length() > 0)
                                    branchId = repository.getVariantManager().getBranch(branch, false).getId();
                                if (lang != null && lang.length() > 0)
                                    langId = repository.getVariantManager().getLanguage(lang, false).getId();
                            } catch (RepositoryException e) {
                                throw new RuntimeException("Invalid branch or language in link " + value, e);
                            }
                            return new VariantKey(documentId, branchId, langId);
                        } else {
                            throw new RuntimeException("Invalid link: " + value);
                        }
                    }
                });
    }
    
}
