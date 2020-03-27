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
package org.outerj.daisy.books.frontend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.service.ServiceManager;
import org.apache.cocoon.forms.datatype.Datatype;
import org.apache.cocoon.forms.datatype.SelectionList;
import org.apache.cocoon.forms.datatype.StaticSelectionList;
import org.apache.cocoon.forms.event.ActionEvent;
import org.apache.cocoon.forms.event.ActionListener;
import org.apache.cocoon.forms.formmodel.Action;
import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.forms.formmodel.Group;
import org.apache.cocoon.forms.formmodel.Repeater;
import org.apache.cocoon.forms.formmodel.Union;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.formmodel.Repeater.RepeaterRow;
import org.apache.cocoon.forms.util.StringMessage;
import org.outerj.daisy.books.publisher.BookPublisher;
import org.outerj.daisy.books.publisher.PublicationSpec;
import org.outerj.daisy.books.publisher.PublicationSpecProperty;
import org.outerj.daisy.books.publisher.PublicationTypeInfo;
import org.outerx.daisy.x10Bookpubspecs.PublicationSpecsDocument;

public class PublicationTypesFormHelper {
    private static class AddPublicationTypeAction implements ActionListener {
        private final PublicationTypeInfo[] publicationTypeInfos;
        private final ServiceManager serviceManager;

        public AddPublicationTypeAction(PublicationTypeInfo[] publicationTypeInfos, ServiceManager serviceManager) {
            this.publicationTypeInfos = publicationTypeInfos;
            this.serviceManager = serviceManager;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            Widget widget = actionEvent.getSourceWidget();
            String typeName = (String)widget.lookupWidget("../availablePublicationTypes").getValue();
            Repeater publicationsRepeater = (Repeater)widget.lookupWidget("../publications");
            Repeater.RepeaterRow row = publicationsRepeater.addRow();
            String typeLabel = getPublicationTypeLabel(typeName, publicationTypeInfos);
            row.getChild("typeLabel").setValue(typeLabel);
            row.getChild("typeName").setValue(typeName);
            row.getChild("outputName").setValue(typeName);
            row.getChild("outputLabel").setValue(typeLabel);

            loadDefaultProperties(row, typeName, serviceManager);
        }
    }

    private static String getPublicationTypeLabel(String name, PublicationTypeInfo[] publicationTypeInfos) {
        for (PublicationTypeInfo publicationTypeInfo : publicationTypeInfos) {
            if (publicationTypeInfo.getName().equals(name)) {
                return publicationTypeInfo.getLabel();
            }
        }
        return "(error: specified publication type unknown)";
    }

    private static void loadDefaultProperties(Repeater.RepeaterRow row, String publicationTypeName, ServiceManager serviceManager) {
    	Map<String, String> properties;
        BookPublisher bookPublisher = null;
        try {
            bookPublisher = (BookPublisher)serviceManager.lookup(BookPublisher.ROLE);
            properties = bookPublisher.getDefaultProperties(publicationTypeName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (bookPublisher != null)
                serviceManager.release(bookPublisher);
        }
        Field defaultPropertiesField = (Field)row.getChild("defaultProperties");
        defaultPropertiesField.setSelectionList(buildDefaultPropertiesSelectionList(properties, defaultPropertiesField.getDatatype()));
        defaultPropertiesField.setAttribute("properties", properties);
    }

    
    private static SelectionList buildDefaultPropertiesSelectionList(Map<String, String>  properties, Datatype datatype) {
        StaticSelectionList selectionList = new StaticSelectionList(datatype);
        for (Map.Entry<String, String> entry : properties.entrySet()) { 
            selectionList.addItem(entry.getKey(), new StringMessage(entry.getKey() + " = " + entry.getValue()));
        }
        return selectionList;
    }

    public static PublicationTypeInfo[] getPublicationTypeInfos(ServiceManager serviceManager) throws Exception {
        BookPublisher bookPublisher = null;
        try {
            bookPublisher = (BookPublisher)serviceManager.lookup(BookPublisher.ROLE);
            return bookPublisher.getAvailablePublicationTypes();
        } finally {
            if (bookPublisher != null)
                serviceManager.release(bookPublisher);
        }
    }

    public static void initPublicationsForm(Widget widget, ServiceManager serviceManager) throws Exception { 
        PublicationTypeInfo[] publicationTypeInfos = getPublicationTypeInfos(serviceManager);
        Arrays.sort(publicationTypeInfos);
        
        Field availablePublicationTypesField = (Field)widget.lookupWidget("editors/gui/availablePublicationTypes");
        availablePublicationTypesField.setSelectionList(publicationTypeInfos, "name", "label");
        ((Action)widget.lookupWidget("editors/gui/addPublicationType")).addActionListener(new AddPublicationTypeAction(publicationTypeInfos, serviceManager));
    }

    public static void loadPublicationSpecs(Widget widget, PublicationSpec[] specs,
                                            ServiceManager serviceManager) throws Exception {
        PublicationTypeInfo[] publicationTypeInfos = getPublicationTypeInfos(serviceManager);
        Repeater repeater = (Repeater)widget.lookupWidget("editors/gui/publications");
        repeater.clear(); // clear in case this is called on an existing form
        for (PublicationSpec spec : specs) {
            Repeater.RepeaterRow row = repeater.addRow();
            row.getChild("typeLabel").setValue(getPublicationTypeLabel(spec.getPublicationTypeName(), publicationTypeInfos));
            row.getChild("typeName").setValue(spec.getPublicationTypeName());
            row.getChild("outputName").setValue(spec.getPublicationOutputName());
            row.getChild("outputLabel").setValue(spec.getPublicationOutputLabel());
            Repeater propertiesRepeater = (Repeater)row.getChild("properties");
            for (Map.Entry<String, String> entry : spec.getPublicationProperties().entrySet()) {
                Repeater.RepeaterRow propRow = propertiesRepeater.addRow();
                propRow.getChild("name").setValue(entry.getKey());
                propRow.getChild("value").setValue(entry.getValue());
            }
            loadDefaultProperties(row, spec.getPublicationTypeName(), serviceManager);
        }
    }

    public static PublicationSpecsDocument getXml(Widget widget) {
        PublicationSpecsDocument publicationSpecsDocument = PublicationSpecsDocument.Factory.newInstance();
        PublicationSpecsDocument.PublicationSpecs publicationSpecsXml = publicationSpecsDocument.addNewPublicationSpecs();

        Repeater repeater = (Repeater)widget.lookupWidget("editors/gui/publications");
        for (int i = 0; i < repeater.getSize(); i++) {
            Repeater.RepeaterRow row = repeater.getRow(i);
            String typeName = (String)row.getChild("typeName").getValue();
            String outputName = (String)row.getChild("outputName").getValue();
            String outputLabel = (String)row.getChild("outputLabel").getValue();

            PublicationSpecsDocument.PublicationSpecs.PublicationSpec publicationSpecXml = publicationSpecsXml.addNewPublicationSpec();
            publicationSpecXml.setName(outputName);
            publicationSpecXml.setType(typeName);
            publicationSpecXml.setLabel(outputLabel);

            Repeater propertiesRepeater = (Repeater)row.getChild("properties");
            if (propertiesRepeater.getSize() > 0) {
                PublicationSpecsDocument.PublicationSpecs.PublicationSpec.Properties propertiesXml = publicationSpecXml.addNewProperties();
                for (int k = 0; k < propertiesRepeater.getSize(); k++) {
                    Repeater.RepeaterRow propRow = propertiesRepeater.getRow(k);
                    String name = (String)propRow.getChild("name").getValue();
                    String value = (String)propRow.getChild("value").getValue();
                    if (name != null) {
                        PublicationSpecsDocument.PublicationSpecs.PublicationSpec.Properties.Entry entryXml = propertiesXml.addNewEntry();
                        entryXml.setKey(name);
                        if (value != null)
                            entryXml.setStringValue(value);
                    }
                }
            }
        }

        return publicationSpecsDocument;
    }
}
