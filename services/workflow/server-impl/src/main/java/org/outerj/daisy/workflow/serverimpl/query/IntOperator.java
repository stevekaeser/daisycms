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
package org.outerj.daisy.workflow.serverimpl.query;

import org.outerj.daisy.i18n.I18nMessage;
import org.outerj.daisy.i18n.impl.StringI18nMessage;
import org.outerj.daisy.workflow.WfValueType;

import java.util.Set;
import java.util.HashSet;
import java.util.List;

/**
 * Internal representation of an operator.
 */
public abstract class IntOperator {
    private final int argumentCount;
    private final String name;
    private final I18nMessage label;
    private Set<WfValueType> supportedTypes;

    public IntOperator(int argumentCount, String name, I18nMessage label, Set<WfValueType> supportedTypes) {
        this.argumentCount = argumentCount;
        this.name = name;
        this.label = label;
        this.supportedTypes = supportedTypes;
    }

    public int getArgumentCount() {
        return argumentCount;
    }

    public String getName() {
        return name;
    }

    public I18nMessage getLabel() {
        return label;
    }

    public boolean supportsType(WfValueType type) {
        return supportedTypes.contains(type);
    }

    public abstract void generateHql(StringBuilder builder, WfValueType type, List<Object> values, Binder binder);

    public static class EqOperator extends IntOperator {
        public EqOperator() {
            super(1, "eq", new StringI18nMessage("equals"), getSupportedTypes());
        }

        private static Set<WfValueType> getSupportedTypes() {
            Set<WfValueType> supportedTypes = new HashSet<WfValueType>();
            supportedTypes.add(WfValueType.ACTOR);
            supportedTypes.add(WfValueType.DATE);
            supportedTypes.add(WfValueType.DATETIME);
            supportedTypes.add(WfValueType.LONG);
            supportedTypes.add(WfValueType.STRING);
            supportedTypes.add(WfValueType.BOOLEAN);
            supportedTypes.add(WfValueType.USER);
            supportedTypes.add(WfValueType.ID);
            return supportedTypes;
        }

        public void generateHql(StringBuilder builder, WfValueType type, List<Object> values, Binder binder) {
            String bindName = binder.getUniqueBindName();
            builder.append(" = :").append(bindName).append(" ");
            binder.addBind(bindName, type, values.get(0));
        }
    }

    public static class LtOperator extends IntOperator {
        public LtOperator() {
            super(1, "lt", new StringI18nMessage("less than"), getSupportedTypes());
        }

        private static Set<WfValueType> getSupportedTypes() {
            Set<WfValueType> supportedTypes = new HashSet<WfValueType>();
            supportedTypes.add(WfValueType.DATE);
            supportedTypes.add(WfValueType.DATETIME);
            supportedTypes.add(WfValueType.LONG);
            supportedTypes.add(WfValueType.STRING);
            return supportedTypes;
        }

        public void generateHql(StringBuilder builder, WfValueType type, List<Object> values, Binder binder) {
            String bindName = binder.getUniqueBindName();
            builder.append(" < :").append(bindName).append(" ");
            binder.addBind(bindName, type, values.get(0));
        }
    }

    public static class GtOperator extends IntOperator {
        public GtOperator() {
            super(1, "gt", new StringI18nMessage("greater than"), getSupportedTypes());
        }

        private static Set<WfValueType> getSupportedTypes() {
            Set<WfValueType> supportedTypes = new HashSet<WfValueType>();
            supportedTypes.add(WfValueType.DATE);
            supportedTypes.add(WfValueType.DATETIME);
            supportedTypes.add(WfValueType.LONG);
            supportedTypes.add(WfValueType.STRING);
            return supportedTypes;
        }

        public void generateHql(StringBuilder builder, WfValueType type, List<Object> values, Binder binder) {
            String bindName = binder.getUniqueBindName();
            builder.append(" > :").append(bindName).append(" ");
            binder.addBind(bindName, type, values.get(0));
        }
    }

    public static class LtEqOperator extends IntOperator {
        public LtEqOperator() {
            super(1, "lt_eq", new StringI18nMessage("less than or equal"), getSupportedTypes());
        }

        private static Set<WfValueType> getSupportedTypes() {
            Set<WfValueType> supportedTypes = new HashSet<WfValueType>();
            supportedTypes.add(WfValueType.DATE);
            supportedTypes.add(WfValueType.DATETIME);
            supportedTypes.add(WfValueType.LONG);
            supportedTypes.add(WfValueType.STRING);
            return supportedTypes;
        }

        public void generateHql(StringBuilder builder, WfValueType type, List<Object> values, Binder binder) {
            String bindName = binder.getUniqueBindName();
            builder.append(" <= :").append(bindName).append(" ");
            binder.addBind(bindName, type, values.get(0));
        }
    }

    public static class GtEqOperator extends IntOperator {
        public GtEqOperator() {
            super(1, "gt_eq", new StringI18nMessage("greater than or equal"), getSupportedTypes());
        }

        private static Set<WfValueType> getSupportedTypes() {
            Set<WfValueType> supportedTypes = new HashSet<WfValueType>();
            supportedTypes.add(WfValueType.DATE);
            supportedTypes.add(WfValueType.DATETIME);
            supportedTypes.add(WfValueType.LONG);
            supportedTypes.add(WfValueType.STRING);
            return supportedTypes;
        }

        public void generateHql(StringBuilder builder, WfValueType type, List<Object> values, Binder binder) {
            String bindName = binder.getUniqueBindName();
            builder.append(" >= :").append(bindName).append(" ");
            binder.addBind(bindName, type, values.get(0));
        }
    }

    public static class BetweenOperator extends IntOperator {
        public BetweenOperator() {
            super(2, "between", new StringI18nMessage("between"), getSupportedTypes());
        }

        private static Set<WfValueType> getSupportedTypes() {
            Set<WfValueType> supportedTypes = new HashSet<WfValueType>();
            supportedTypes.add(WfValueType.DATE);
            supportedTypes.add(WfValueType.DATETIME);
            supportedTypes.add(WfValueType.LONG);
            supportedTypes.add(WfValueType.STRING);
            return supportedTypes;
        }

        public void generateHql(StringBuilder builder, WfValueType type, List<Object> values, Binder binder) {
            String bindName1 = binder.getUniqueBindName();
            String bindName2 = binder.getUniqueBindName();
            builder.append(" between :").append(bindName1).append(" and :").append(bindName2).append(" ");
            binder.addBind(bindName1, type, values.get(0));
            binder.addBind(bindName2, type, values.get(1));
        }
    }

    public static class IsNullOperator extends IntOperator {
        public IsNullOperator() {
            super(0, "is_null", new StringI18nMessage("is null"), getSupportedTypes());
        }

        private static Set<WfValueType> getSupportedTypes() {
            Set<WfValueType> supportedTypes = new HashSet<WfValueType>();
            supportedTypes.add(WfValueType.ACTOR);
            supportedTypes.add(WfValueType.DATE);
            supportedTypes.add(WfValueType.DATETIME);
            supportedTypes.add(WfValueType.LONG);
            supportedTypes.add(WfValueType.STRING);
            supportedTypes.add(WfValueType.BOOLEAN);
            supportedTypes.add(WfValueType.USER);
            supportedTypes.add(WfValueType.DAISY_LINK);
            return supportedTypes;
        }

        public void generateHql(StringBuilder builder, WfValueType type, List<Object> values, Binder binder) {
            builder.append(" is null ");
        }
    }

    public static class IsNotNullOperator extends IntOperator {
        public IsNotNullOperator() {
            super(0, "is_not_null", new StringI18nMessage("is not null"), getSupportedTypes());
        }

        private static Set<WfValueType> getSupportedTypes() {
            Set<WfValueType> supportedTypes = new HashSet<WfValueType>();
            supportedTypes.add(WfValueType.ACTOR);
            supportedTypes.add(WfValueType.DATE);
            supportedTypes.add(WfValueType.DATETIME);
            supportedTypes.add(WfValueType.LONG);
            supportedTypes.add(WfValueType.STRING);
            supportedTypes.add(WfValueType.BOOLEAN);
            supportedTypes.add(WfValueType.USER);
            supportedTypes.add(WfValueType.DAISY_LINK);
            return supportedTypes;
        }

        public void generateHql(StringBuilder builder, WfValueType type, List<Object> values, Binder binder) {
            builder.append(" is not null ");
        }
    }

    public static class LikeOperator extends IntOperator {
        public LikeOperator() {
            super(1, "like", new StringI18nMessage("like"), getSupportedTypes());
        }

        private static Set<WfValueType> getSupportedTypes() {
            Set<WfValueType> supportedTypes = new HashSet<WfValueType>();
            supportedTypes.add(WfValueType.STRING);
            supportedTypes.add(WfValueType.DAISY_LINK);
            return supportedTypes;
        }

        public void generateHql(StringBuilder builder, WfValueType type, List<Object> values, Binder binder) {
            String bindName = binder.getUniqueBindName();
            builder.append(" like :").append(bindName).append(" ");
            binder.addBind(bindName, type, values.get(0));
        }
    }
}
