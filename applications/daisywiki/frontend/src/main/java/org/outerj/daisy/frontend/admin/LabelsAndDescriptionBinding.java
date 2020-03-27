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
package org.outerj.daisy.frontend.admin;

import org.apache.cocoon.forms.binding.AbstractCustomBinding;
import org.apache.cocoon.forms.binding.BindingException;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.formmodel.Repeater;
import org.apache.commons.jxpath.JXPathContext;
import org.outerj.daisy.repository.schema.*;
import org.outerj.daisy.repository.LocaleHelper;

import java.util.*;

public class LabelsAndDescriptionBinding extends AbstractCustomBinding {

    protected void doLoad(Widget widget, JXPathContext jxPathContext) throws BindingException {
        Repeater repeater = (Repeater)widget;

        Locale[] adminLocales = (Locale[])repeater.getAttribute("adminLocales");
        Object object = jxPathContext.getValue(".");
        LabelEnabled labelEnabled = (LabelEnabled)object;
        DescriptionEnabled descriptionEnabled = (DescriptionEnabled)object;

        // Merge list of all available locales
        Set<Locale> result = new HashSet<Locale>();
        result.addAll(Arrays.asList(adminLocales));
        result.addAll(Arrays.asList(labelEnabled.getLabelLocales()));
        result.addAll(Arrays.asList(descriptionEnabled.getDescriptionLocales()));
        Locale[] locales = result.toArray(new Locale[0]);
        Arrays.sort(locales, LOCALE_COMPARATOR);
        String[] localeStrings = localesAsStrings(locales);

        // Fill up the repeater
        repeater.clear();
        for (int i = 0; i < locales.length; i++) {
            Repeater.RepeaterRow row = repeater.addRow();
            row.getChild("locale").setValue(localeStrings[i]);
            row.getChild("label").setValue(labelEnabled.getLabelExact(locales[i]));
            row.getChild("description").setValue(descriptionEnabled.getDescriptionExact(locales[i]));
        }
    }

    public static void initRepeater(Repeater repeater) {
        Locale[] locales = (Locale[])repeater.getAttribute("adminLocales");
        Arrays.sort(locales, LOCALE_COMPARATOR);
        String[] localeStrings = localesAsStrings(locales);
        for (int i = 0; i < locales.length; i++) {
            Repeater.RepeaterRow row = repeater.addRow();
            row.getChild("locale").setValue(localeStrings[i]);
        }
    }

    private static String[] localesAsStrings(Locale[] locales) {
        String[] result = new String[locales.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = LocaleHelper.getString(locales[i]);
        }
        return result;
    }

    protected void doSave(Widget widget, JXPathContext jxPathContext) throws BindingException {
        Repeater repeater = (Repeater)widget;
        Object object = jxPathContext.getValue(".");
        LabelEnabled labelEnabled = (LabelEnabled)object;
        DescriptionEnabled descriptionEnabled = (DescriptionEnabled)object;

        for (int i = 0; i < repeater.getSize(); i++) {
            Locale locale = LocaleHelper.parseLocale((String)repeater.getRow(i).getChild("locale").getValue());
            String label = (String)repeater.getRow(i).getChild("label").getValue();
            String description = (String)repeater.getRow(i).getChild("description").getValue();
            labelEnabled.setLabel(locale, label);
            descriptionEnabled.setDescription(locale, description);
        }
    }

    private static final LocaleComparator LOCALE_COMPARATOR = new LocaleComparator();

    static class LocaleComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            Locale locale1 = (Locale)o1;
            Locale locale2 = (Locale)o2;
            return LocaleHelper.getString(locale1).compareTo(LocaleHelper.getString(locale2));
        }
    }

}
