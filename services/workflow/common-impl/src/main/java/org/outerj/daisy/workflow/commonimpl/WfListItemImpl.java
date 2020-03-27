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
package org.outerj.daisy.workflow.commonimpl;

import org.outerj.daisy.workflow.WfListItem;
import org.outerj.daisy.i18n.I18nMessage;

public class WfListItemImpl implements WfListItem {
    private Object value;
    private I18nMessage label;

    public WfListItemImpl(Object value, I18nMessage label) {
        this.value = value;
        this.label = label;
    }

    public Object getValue() {
        return value;
    }

    public I18nMessage getLabel() {
        return label;
    }
}
