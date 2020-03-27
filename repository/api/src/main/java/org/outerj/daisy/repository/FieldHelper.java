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
package org.outerj.daisy.repository;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.xmlbeans.GDateBuilder;
import org.outerx.daisy.x10.FieldValuesType;
import org.outerx.daisy.x10.LinkDocument;

/**
 * Some helper methods that can be useful when working with {@link Field}s.
 */
public final class FieldHelper {
    /**
     * Convenience method to get a String representation of a value, taking into
     * account the locale (thus nicer then calling toString() on the value).
     */
    public static String getFormattedValue(Object value, ValueType valueType, Locale locale, Repository repository) {
        if (value instanceof Object[]) {
            Object[] values = (Object[])value;
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                if (i > 0)
                    result.append(", ");
                result.append(getFormattedValueIntHier(values[i], valueType, locale, repository));
            }
            return result.toString();
        } else {
            return getFormattedValueIntHier(value, valueType, locale, repository);
        }
    }

    private static String getFormattedValueIntHier(Object value, ValueType valueType, Locale locale, Repository repository) {
        if (value instanceof HierarchyPath) {
            Object[] elements = ((HierarchyPath)value).getElements();
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < elements.length; i++) {
                if (i > 0)
                    result.append("/");
                result.append(getFormattedValueInt(elements[i], valueType, locale, repository));
            }
            return result.toString();
        } else {
            return getFormattedValueInt(value, valueType, locale, repository);
        }
    }

    private static String getFormattedValueInt(Object value, ValueType valueType, Locale locale, Repository repository) {
        if (value == null) {
            return "";
        } else if (valueType == ValueType.DATE) {
            DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale);
            return dateFormat.format((Date)value);
        } else if (valueType == ValueType.DATETIME) {
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
            return dateFormat.format((Date)value);
        } else if (valueType == ValueType.DECIMAL || valueType == ValueType.DOUBLE) {
            NumberFormat decimalFormat = NumberFormat.getNumberInstance(locale);
            return decimalFormat.format(value);
        } else if (valueType == ValueType.LINK) {
            VariantKey link = (VariantKey)value;
            StringBuilder linkFormatted = new StringBuilder(20);
            linkFormatted.append("daisy:");
            linkFormatted.append(link.getDocumentId());
            if (link.getBranchId() != -1 || link.getLanguageId() != -1) {
                linkFormatted.append("@");
                if (link.getBranchId() != -1) {
                    String branchName;
                    try {
                        branchName = repository.getVariantManager().getBranch(link.getBranchId(), false).getName();
                    } catch (RepositoryException e) {
                        branchName = String.valueOf(link.getBranchId());
                    }
                    linkFormatted.append(branchName);
                }
                if (link.getLanguageId() != -1) {
                    String languageName;
                    try {
                        languageName = repository.getVariantManager().getLanguage(link.getLanguageId(), false).getName();
                    } catch (RepositoryException e) {
                        languageName = String.valueOf(link.getLanguageId());
                    }
                    linkFormatted.append(":");
                    linkFormatted.append(languageName);
                }
            }
            return linkFormatted.toString();
        } else {
            return value.toString();
        }
    }

    /**
     * Convenience method to get a value of a certain ValueType from a Field-as-XML.
     * Useful if you're working with the XML representation of a field.
     */
    public static Object getFieldValueFromXml(ValueType valueType, boolean multiValue, boolean hierarchical, FieldValuesType xml) {
        return getXmlFieldValueGetter(valueType).getValue(xml, multiValue, hierarchical);
    }

    public static Object getFieldValueFromXml(ValueType valueType, boolean multiValue, FieldValuesType xml) {
        return getXmlFieldValueGetter(valueType).getValue(xml, multiValue);
    }

    public static Object[] getFieldValuesFromXml(ValueType valueType, FieldValuesType xml) {
        return (Object[])getXmlFieldValueGetter(valueType).getValue(xml, true);
    }

    public static XmlFieldValueSetter getXmlFieldValueSetter(ValueType valueType) {
        if (valueType == ValueType.STRING)
            return STRING_DISTINCT_SETTER;
        else if (valueType == ValueType.LONG)
            return LONG_DISTINCT_SETTER;
        else if (valueType == ValueType.DECIMAL)
            return DECIMAL_DISTINCT_SETTER;
        else if (valueType == ValueType.DOUBLE)
            return DOUBLE_DISTINCT_SETTER;
        else if (valueType == ValueType.BOOLEAN)
            return BOOLEAN_DISTINCT_SETTER;
        else if (valueType == ValueType.DATE)
            return DATE_DISTINCT_SETTER;
        else if (valueType == ValueType.DATETIME)
            return DATETIME_DISTINCT_SETTER;
        else if (valueType == ValueType.LINK)
            return LINK_DISTINCT_SETTER;
        else
            throw new RuntimeException("Unexpected ValueType: " + valueType.toString());
    }

    public static interface XmlFieldValueSetter {
        void addValue(Object value, FieldValuesType xml);
    }

    private static XmlFieldValueSetter STRING_DISTINCT_SETTER = new XmlFieldValueSetter() {
        public void addValue(Object value, FieldValuesType xml) {
            xml.addString((String)value);
        }
    };

    private static XmlFieldValueSetter LONG_DISTINCT_SETTER = new XmlFieldValueSetter() {
        public void addValue(Object value, FieldValuesType xml) {
            xml.addLong((Long)value);
        }
    };

    private static XmlFieldValueSetter DECIMAL_DISTINCT_SETTER = new XmlFieldValueSetter() {
        public void addValue(Object value, FieldValuesType xml) {
            xml.addDecimal((BigDecimal)value);
        }
    };

    private static XmlFieldValueSetter DOUBLE_DISTINCT_SETTER = new XmlFieldValueSetter() {
        public void addValue(Object value, FieldValuesType xml) {
            xml.addDouble((Double)value);
        }
    };

    private static XmlFieldValueSetter BOOLEAN_DISTINCT_SETTER = new XmlFieldValueSetter() {
        public void addValue(Object value, FieldValuesType xml) {
            xml.addBoolean((Boolean)value);
        }
    };

    private static XmlFieldValueSetter DATE_DISTINCT_SETTER = new XmlFieldValueSetter() {
        public void addValue(Object value, FieldValuesType xml) {
            GDateBuilder gdb = new GDateBuilder((Date)value);
            gdb.clearTime();
            gdb.clearTimeZone();
            xml.addDate(gdb.getCalendar());
        }
    };

    private static XmlFieldValueSetter DATETIME_DISTINCT_SETTER = new XmlFieldValueSetter() {
        public void addValue(Object value, FieldValuesType xml) {
            GDateBuilder gdb = new GDateBuilder((Date)value);
            gdb.normalizeToTimeZone(0);
            xml.addDateTime(gdb.getCalendar());
        }
    };

    private static XmlFieldValueSetter LINK_DISTINCT_SETTER = new XmlFieldValueSetter() {
        public void addValue(Object value, FieldValuesType xml) {
            LinkDocument.Link link = xml.addNewLink();
            VariantKey variantKey = (VariantKey)value;
            link.setDocumentId(variantKey.getDocumentId());
            link.setBranchId(variantKey.getBranchId());
            link.setLanguageId(variantKey.getLanguageId());
        }
    };

    public static XmlFieldValueGetter getXmlFieldValueGetter(ValueType valueType) {
        if (valueType == ValueType.STRING)
            return STRING_VALUE_GETTER;
        else if (valueType == ValueType.BOOLEAN)
            return BOOLEAN_VALUE_GETTER;
        else if (valueType == ValueType.LONG)
            return LONG_VALUE_GETTER;
        else if (valueType == ValueType.DECIMAL)
            return DECIMAL_VALUE_GETTER;
        else if (valueType == ValueType.DOUBLE)
            return DOUBLE_VALUE_GETTER;
        else if (valueType == ValueType.DATE)
            return DATE_VALUE_GETTER;
        else if (valueType == ValueType.DATETIME)
            return DATETIME_VALUE_GETTER;
        else if (valueType == ValueType.LINK)
            return LINK_VALUE_GETTER;
        else
            throw new RuntimeException("Unexpected ValueType: " + valueType.toString());
    }

    public static interface XmlFieldValueGetter {
        public Object getValue(FieldValuesType xml, boolean multiValue);

        /**
         *
         * @param multiValue if true, an Object[] will be returned.
         */
        public Object getValue(FieldValuesType xml, boolean multiValue, boolean hierarchical);
    }

    private static abstract class AbstractFieldValueGetter implements XmlFieldValueGetter {
        public Object getValue(FieldValuesType xml, boolean multiValue, boolean hierarchical) {
            if (multiValue && hierarchical) {
                List<FieldValuesType.HierarchyPath> hierarchyPathsXml = xml.getHierarchyPathList();
                HierarchyPath[] hierarchyPaths = new HierarchyPath[hierarchyPathsXml.size()];
                for (int i = 0; i < hierarchyPaths.length; i++) {
                    hierarchyPaths[i] = new HierarchyPath((Object[])getValue(hierarchyPathsXml.get(i), true));
                }
                return hierarchyPaths;
            } else if (hierarchical) {
                FieldValuesType.HierarchyPath hierarchyPathXml = xml.getHierarchyPathArray(0);
                return new HierarchyPath((Object[])getValue(hierarchyPathXml, true));
            } else {
                return getValue(xml, multiValue);
            }
        }
    }

    private static XmlFieldValueGetter STRING_VALUE_GETTER = new AbstractFieldValueGetter() {
        public Object getValue(FieldValuesType xml, boolean multiValue) {
            if (multiValue) {
                return xml.getStringList().toArray(new String[0]);
            } else {
                return xml.getStringArray(0);
            }
        }
    };

    private static XmlFieldValueGetter BOOLEAN_VALUE_GETTER = new AbstractFieldValueGetter() {
        public Object getValue(FieldValuesType xml, boolean multiValue) {
            if (multiValue) {
                return xml.getBooleanList().toArray(new Boolean[0]);
            } else {
                return xml.getBooleanArray(0);
            }
        }
    };

    private static XmlFieldValueGetter LONG_VALUE_GETTER = new AbstractFieldValueGetter() {
        public Object getValue(FieldValuesType xml, boolean multiValue) {
            if (multiValue) {
                return xml.getLongList().toArray(new Long[0]);
            } else {
                return xml.getLongArray(0);
            }
        }
    };

    private static XmlFieldValueGetter DECIMAL_VALUE_GETTER = new AbstractFieldValueGetter() {
        public Object getValue(FieldValuesType xml, boolean multiValue) {
            if (multiValue) {
                return xml.getDecimalList().toArray(new BigDecimal[0]);
            } else {
                return xml.getDecimalArray(0);
            }
        }
    };

    private static XmlFieldValueGetter DOUBLE_VALUE_GETTER = new AbstractFieldValueGetter() {
        public Object getValue(FieldValuesType xml, boolean multiValue) {
            if (multiValue) {
                return xml.getDoubleList().toArray(new Double[0]);
            } else {
                return xml.getDoubleArray(0);
            }
        }
    };

    private static XmlFieldValueGetter DATE_VALUE_GETTER = new AbstractFieldValueGetter() {
        public Object getValue(FieldValuesType xml, boolean multiValue) {
            if (multiValue) {
                List<Calendar> origValues = xml.getDateList();
                Date[] values = new Date[origValues.size()];
                for (int i = 0; i < origValues.size(); i++)
                    values[i] = origValues.get(i).getTime();
                return values;
            } else {
                return xml.getDateArray(0).getTime();
            }
        }
    };

    private static XmlFieldValueGetter DATETIME_VALUE_GETTER = new AbstractFieldValueGetter() {
        public Object getValue(FieldValuesType xml, boolean multiValue) {
            if (multiValue) {
                List<Calendar> origValues = xml.getDateTimeList();
                Date[] values = new Date[origValues.size()];
                for (int i = 0; i < origValues.size(); i++)
                    values[i] = origValues.get(i).getTime();
                return values;
            } else {
                return xml.getDateTimeArray(0).getTime();
            }
        }
    };

    private static XmlFieldValueGetter LINK_VALUE_GETTER = new AbstractFieldValueGetter() {
        public Object getValue(FieldValuesType xml, boolean multiValue) {
            if (multiValue) {
                List<LinkDocument.Link> origValues = xml.getLinkList();
                VariantKey[] values = new VariantKey[origValues.size()];
                for (int i = 0; i < origValues.size(); i++) {
                    values[i] = getVariantKey(origValues.get(i));
                }
                return values;
            } else {
                return getVariantKey(xml.getLinkArray(0));
            }
        }

        private VariantKey getVariantKey(LinkDocument.Link xml) {
            String documentId = xml.getDocumentId();
            long branchId = xml.isSetBranchId() ? xml.getBranchId() : -1;
            long languageId = xml.isSetLanguageId() ? xml.getLanguageId() : -1;
            return new VariantKey(documentId, branchId, languageId);
        }
    };
}
