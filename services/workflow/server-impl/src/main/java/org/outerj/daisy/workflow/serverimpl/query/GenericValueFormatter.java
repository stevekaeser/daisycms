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

import org.outerj.daisy.workflow.WfVersionKey;
import org.outerj.daisy.workflow.WfUserKey;
import org.outerj.daisy.workflow.serverimpl.IntWfContext;

import java.util.Date;
import java.text.DateFormat;

public class GenericValueFormatter {
    public static String getLabel(Object value, DateFormat dateTimeFormat, IntWfContext context) {
        if (value instanceof Date) {
            return dateTimeFormat.format((Date)value);
        } else if (value instanceof WfVersionKey) {
            try {
                return ((WfVersionKey)value).getVersion(context.getRepository()).getDocumentName();
            } catch (Throwable e) {
                // couldn't retrieve document name, ignore
            }
        } else if (value instanceof WfUserKey) {
            try {
                return context.getRepository().getUserManager().getUserDisplayName(((WfUserKey)value).getId());
            } catch (Throwable e) {
                // coudn't retrieve user name, ignore
            }
        }
        return null;
    }

    public static String getLabelForSorting(Object value, IntWfContext context) {
        if (value instanceof WfVersionKey) {
            try {
                return ((WfVersionKey)value).getVersion(context.getRepository()).getDocumentName();
            } catch (Throwable e) {
                // couldn't retrieve document name, ignore
            }
        } else if (value instanceof WfUserKey) {
            try {
                return context.getRepository().getUserManager().getUserDisplayName(((WfUserKey)value).getId());
            } catch (Throwable e) {
                // coudn't retrieve user name, ignore
            }
        }
        return null;
    }
}
