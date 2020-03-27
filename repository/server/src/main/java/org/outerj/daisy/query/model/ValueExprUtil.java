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
package org.outerj.daisy.query.model;

public class ValueExprUtil {
    public static boolean isPrimitiveValue(ValueExpr valueExpr) {
        return !(valueExpr.isSymbolicIdentifier() || valueExpr.isMultiValue() || valueExpr.isHierarchical());
    }

    /**
     * Returns true if the given ValueExpr could potentially return a value of the specified
     * QValueType, and is non-symbolic, non-multivalue, and non-hierarhical.
     */
    public static boolean isPrimitiveValue(QValueType valueType, ValueExpr valueExpr) {
        QValueType exprValueType = valueExpr.getValueType();
        return (exprValueType == QValueType.UNDEFINED || exprValueType == valueType)
                && isPrimitiveValue(valueExpr);
    }

    public static boolean isCompatPrimitiveValue(QValueType valueType, ValueExpr valueExpr) {
        QValueType exprValueType = valueExpr.getValueType();
        return (exprValueType == QValueType.UNDEFINED || exprValueType.isCompatible(valueType))
                && isPrimitiveValue(valueExpr);
    }

    public static boolean isComparable(ValueExpr valueExpr) {
        QValueType valueType = valueExpr.getValueType();
        return isPrimitiveValue(valueExpr) && valueType != QValueType.BOOLEAN && valueType != QValueType.UNDEFINED;
    }

    /**
     * If the valueExpr is an identifier (possibly via dereference), return that
     * identifier, otherwise returns null.
     */
    public static Identifier getIdentifier(ValueExpr valueExpr) {
        if (valueExpr instanceof Dereference)
            valueExpr = ((Dereference)valueExpr).getFinalValueExpr();
        if (valueExpr instanceof Identifier)
            return (Identifier)valueExpr;
        return null;
    }
}
