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
package org.outerj.daisy.repository.commonimpl.schema;

import org.outerx.daisy.x10.LabelsDocument;
import org.outerx.daisy.x10.DescriptionsDocument;
import org.outerj.daisy.util.LocaleMap;

import java.util.Map;

public class SchemaLocaleMap extends LocaleMap {
    public LabelsDocument.Labels getAsLabelsXml() {
        LabelsDocument.Labels labels = LabelsDocument.Factory.newInstance().addNewLabels();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            LabelsDocument.Labels.Label label = labels.addNewLabel();
            label.setLocale(entry.getKey());
            label.setStringValue((String)entry.getValue());
        }
        return labels;
    }

    public DescriptionsDocument.Descriptions getAsDescriptionsXml() {
        DescriptionsDocument.Descriptions descriptions = DescriptionsDocument.Factory.newInstance().addNewDescriptions();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            DescriptionsDocument.Descriptions.Description description = descriptions.addNewDescription();
            description.setLocale(entry.getKey());
            description.setStringValue((String)entry.getValue());
        }
        return descriptions;
    }

    public void readFromLabelsXml(LabelsDocument.Labels labelsXml) {
        for (LabelsDocument.Labels.Label label : labelsXml.getLabelList()) {
            String locale = label.getLocale();
            String value = label.getStringValue();
            map.put(locale, value);
        }
        searchMap.clear();
    }

    public void readFromDescriptionsXml(DescriptionsDocument.Descriptions descriptionsAsXml) {
        for (DescriptionsDocument.Descriptions.Description description : descriptionsAsXml.getDescriptionList()) {
            String locale = description.getLocale();
            String value = description.getStringValue();
            map.put(locale, value);
        }
        searchMap.clear();
    }
}
