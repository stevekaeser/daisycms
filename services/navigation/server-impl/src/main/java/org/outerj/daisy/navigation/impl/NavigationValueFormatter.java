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
package org.outerj.daisy.navigation.impl;

import org.outerj.daisy.repository.ValueType;

import java.text.NumberFormat;
import java.text.DateFormat;
import java.util.Locale;
import java.util.Date;

public class NavigationValueFormatter {
    private final DateFormat dateFormat;
    private final DateFormat dateTimeFormat;
    private final NumberFormat decimalFormat;

    public NavigationValueFormatter(Locale locale) {
        dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale);
        dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
        decimalFormat = NumberFormat.getNumberInstance(locale);
    }

    public String format(ValueType valueType, Object value) {
        if (value == null)
            return "(blank)";

        String formattedValue;
        switch (valueType) {
            case DATE:
                formattedValue = dateFormat.format((Date)value);
                break;
            case DATETIME:
                formattedValue = dateTimeFormat.format((Date)value);
                break;
            case DECIMAL:
                formattedValue = decimalFormat.format(value);
                break;
            case DOUBLE:
                formattedValue = decimalFormat.format(value);
                break;
            default:
                formattedValue = value.toString();
        }
        return formattedValue;
    }
}
