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
package org.outerj.daisy.repository.serverimpl.query;

import java.util.Date;
import java.util.Locale;

import org.outerj.daisy.query.model.QValueType;
import org.outerj.daisy.query.model.ValueExpr;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.query.QueryHelper;

public class QueryFormatter implements Formatter {

    final Locale locale;

    public QueryFormatter(Locale locale) {
        this.locale = locale;
    }

    public String format(ValueExpr valueExpr, Object value) {
        String returnValue;
        QValueType outputValueType = valueExpr.getOutputValueType(); 

        if (value instanceof HierarchyPath) {
            HierarchyPath path = (HierarchyPath)value;
            StringBuilder result = new StringBuilder("matchesPath('");
            for (int i = 0; i < path.getElements().length; i++) {
                result.append("/");
                String elementValue = format(valueExpr, path.getElements()[i]);
                result.append(elementValue.replaceAll("^'|'$", ""));
            }
            result.append("')");
            returnValue = result.toString();
        } else {

            if (outputValueType == QValueType.STRING) {
                returnValue = QueryHelper.formatString(value.toString());
            } else if (outputValueType == QValueType.LONG || outputValueType == QValueType.DOUBLE || outputValueType == QValueType.DECIMAL) {
                returnValue = value.toString();
            } else if (outputValueType == QValueType.DATE) {
                returnValue = QueryHelper.formatDate((Date)value);
            } else if (outputValueType == QValueType.DATETIME) {
                returnValue = QueryHelper.formatDate((Date)value);
            } else if (outputValueType == QValueType.LINK) {
                VariantKey key = (VariantKey)value;
                returnValue = "'daisy:" + key.getDocumentId() + '@' + key.getBranchId() + ':' + key.getLanguageId() + '\'';
            } else {
                returnValue = "'" + value.toString() + "'";
            }
        }

        return returnValue;
    }

    public Locale getLocale() {
        return locale;
    }

}
