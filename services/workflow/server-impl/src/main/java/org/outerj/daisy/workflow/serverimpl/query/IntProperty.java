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

import org.outerj.daisy.workflow.WfValueType;
import org.outerj.daisy.i18n.I18nMessage;
import org.outerj.daisy.i18n.impl.StringI18nMessage;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Internal representation of a property.
 */
public class IntProperty {
    public static enum PropertyParent { PROCESS, TASK, TIMER }
    private final String name;
    private final WfValueType type;
    private final PropertyParent parent;
    private final String hqlName;
    private final ValueGetter getter;

    public IntProperty(String name, WfValueType type, PropertyParent parent, String hqlName, ValueGetter getter) {
        this.name = name;
        this.type = type;
        this.parent = parent;
        this.hqlName = hqlName;
        this.getter = getter;
    }

    public String getName() {
        return name;
    }

    public WfValueType getType() {
        return type;
    }

    public I18nMessage getLabel(Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("org/outerj/daisy/workflow/serverimpl/messages", locale);
        return new StringI18nMessage(bundle.getString(name + ".label"));
    }

    public PropertyParent getParent() {
        return parent;
    }

    public String getHqlName() {
        return hqlName;
    }

    public ValueGetter getPropertyGetter() {
        return getter;
    }
}
