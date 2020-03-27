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
package org.outerj.daisy.frontend.editor;

import org.apache.cocoon.forms.datatype.Datatype;
import org.apache.cocoon.forms.datatype.convertor.Convertor;
import org.apache.cocoon.forms.datatype.convertor.DefaultFormatCache;
import org.apache.cocoon.forms.FormsConstants;
import org.apache.cocoon.xml.XMLUtils;
import org.apache.cocoon.xml.AttributesImpl;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.outerj.daisy.repository.schema.SelectionList;
import org.outerj.daisy.repository.schema.ListItem;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.variant.VariantManager;

import java.util.Locale;
import java.util.List;

/**
 * Adapts a Daisy selection list to a CForms selection list.
 */
public class SelectionListAdapter implements org.apache.cocoon.forms.datatype.SelectionList {
    private Datatype datatype;
    private SelectionList selectionList;
    private boolean includeEmpty;
    private ValueType fieldValueType;
    private boolean hierarchicalFieldType;
    private VariantManager variantManager;
    private long documentBranchId;
    private long documentLanguageId;

    public SelectionListAdapter(Datatype datatype, SelectionList selectionList, boolean includeEmpty,
            ValueType fieldValueType, boolean hierarchicalFieldType, VariantManager variantManager,
            long documentBranchId, long documentLanguageId) {
        this.datatype = datatype;
        this.selectionList = selectionList;
        this.includeEmpty = includeEmpty;
        this.fieldValueType = fieldValueType;
        this.hierarchicalFieldType = hierarchicalFieldType;
        this.variantManager = variantManager;
        this.documentBranchId = documentBranchId;
        this.documentLanguageId = documentLanguageId;
    }

    public Datatype getDatatype() {
        return datatype;
    }

    public void generateSaxFragment(ContentHandler contentHandler, Locale locale) throws SAXException {
        generateSaxFragment(contentHandler, locale, false);
    }

    /**
     *
     * @param hierarchical should a hierarchical list be generated, that is, a list in which items can again contain
     *                     items. Note that a Daisy hierarchical selection list can be generated as either a flat
     *                     or hierarchical CForms list.
     */
    public void generateSaxFragment(ContentHandler contentHandler, Locale locale, boolean hierarchical) throws SAXException {
        List<? extends ListItem> items = selectionList.getItems(documentBranchId, documentLanguageId, locale);
        Convertor.FormatCache formatCache = new DefaultFormatCache();
        AttributesImpl rootAttrs = new AttributesImpl();
        rootAttrs.addCDATAAttribute("hierarchical", String.valueOf(hierarchical));
        contentHandler.startElement(FormsConstants.INSTANCE_NS, SELECTION_LIST_EL, FormsConstants.INSTANCE_PREFIX_COLON + SELECTION_LIST_EL, rootAttrs);

        if (includeEmpty) {
            AttributesImpl itemAttrs = new AttributesImpl();
            itemAttrs.addCDATAAttribute("value", "");
            contentHandler.startElement(FormsConstants.INSTANCE_NS, ITEM_EL, FormsConstants.INSTANCE_PREFIX_COLON + ITEM_EL, itemAttrs);
            contentHandler.startElement(FormsConstants.INSTANCE_NS, LABEL_EL, FormsConstants.INSTANCE_PREFIX_COLON + LABEL_EL, XMLUtils.EMPTY_ATTRIBUTES);
            contentHandler.endElement(FormsConstants.INSTANCE_NS, LABEL_EL, FormsConstants.INSTANCE_PREFIX_COLON + LABEL_EL);
            contentHandler.endElement(FormsConstants.INSTANCE_NS, ITEM_EL, FormsConstants.INSTANCE_PREFIX_COLON + ITEM_EL);
        }

        generateSaxFragment(items, contentHandler, locale, hierarchical, null, null, null, formatCache);

        contentHandler.endElement(FormsConstants.INSTANCE_NS, SELECTION_LIST_EL, FormsConstants.INSTANCE_PREFIX_COLON + SELECTION_LIST_EL);
    }

    /**
     *
     * @param hierarchical indicates whether the list should be generated hierarchically, not whether the field type
     *                     itself is hierarchical
     */
    private void generateSaxFragment(List<? extends ListItem> items, ContentHandler contentHandler, Locale locale, boolean hierarchical, String parentValue, String parentLabelPrefix, String parentHierarchicalLabel, Convertor.FormatCache formatCache) throws SAXException {
        for (ListItem item : items) {
            // convert value to string
            String stringValue;
            if (fieldValueType == ValueType.LINK)
                stringValue = variantKeyToString((VariantKey)item.getValue());
            else
                stringValue = datatype.getConvertor().convertToString(item.getValue(), locale, formatCache);

            // make value hierarchical if needed
            String fullStringValue = parentValue != null && hierarchicalFieldType ? parentValue + " / " + stringValue : stringValue;

            List<? extends ListItem> children = item.getItems();

            // Determine label
            String label = item.getLabel(locale);
            if (label == null)
                label = stringValue;
            if (label == null) // label for null value: empty string
                label = "";

            String hierarchicalLabel = parentHierarchicalLabel == null ? label : parentHierarchicalLabel + " / " + label;

            if (!hierarchical) {
                // if a hierarchical list is generated as flat list, the labels are indented a bit
                label = parentLabelPrefix == null ? label : parentLabelPrefix + label;
            }

            String labelPrefix = null;
            if (children.size() > 0) {
                labelPrefix = parentLabelPrefix == null ? "\u00A0\u00A0" : parentLabelPrefix + "\u00A0\u00A0";
            }

            AttributesImpl itemAttrs = new AttributesImpl();
            itemAttrs.addCDATAAttribute("value", fullStringValue);
            if (hierarchicalLabel != null)
                itemAttrs.addCDATAAttribute("hierarchicalLabel", hierarchicalLabel);
            contentHandler.startElement(FormsConstants.INSTANCE_NS, ITEM_EL, FormsConstants.INSTANCE_PREFIX_COLON + ITEM_EL, itemAttrs);
            contentHandler.startElement(FormsConstants.INSTANCE_NS, LABEL_EL, FormsConstants.INSTANCE_PREFIX_COLON + LABEL_EL, XMLUtils.EMPTY_ATTRIBUTES);

            contentHandler.characters(label.toCharArray(), 0, label.length());

            contentHandler.endElement(FormsConstants.INSTANCE_NS, LABEL_EL, FormsConstants.INSTANCE_PREFIX_COLON + LABEL_EL);

            if (hierarchical) {
                generateSaxFragment(children, contentHandler, locale, hierarchical, fullStringValue, labelPrefix, hierarchicalLabel, formatCache);
            }

            contentHandler.endElement(FormsConstants.INSTANCE_NS, ITEM_EL, FormsConstants.INSTANCE_PREFIX_COLON + ITEM_EL);

            if (!hierarchical) {
                generateSaxFragment(children, contentHandler, locale, hierarchical, fullStringValue, labelPrefix, hierarchicalLabel, formatCache);
            }
        }
    }

    /**
     * Converts a variant key to string representation, but does not add the branch and language if they
     * are the same as the document that is being edited.
     */
    private String variantKeyToString(VariantKey variantKey) {
        StringBuilder text = new StringBuilder(20);
        text.append("daisy:");
        text.append(variantKey.getDocumentId());
        long branchId = variantKey.getBranchId() == -1 ? documentBranchId : variantKey.getBranchId();
        long languageId = variantKey.getLanguageId() == -1 ? documentLanguageId : variantKey.getLanguageId();
        if (branchId != documentBranchId || languageId != documentLanguageId) {
            text.append("@");
            if (branchId != documentBranchId) {
                String branchName;
                try {
                    branchName = variantManager.getBranch(variantKey.getBranchId(), false).getName();
                } catch (RepositoryException e) {
                    branchName = String.valueOf(variantKey.getBranchId());
                }
                text.append(branchName);
            }
            if (languageId != documentLanguageId) {
                text.append(":");
                String languageName;
                try {
                    languageName = variantManager.getLanguage(variantKey.getLanguageId(), false).getName();
                } catch (RepositoryException e) {
                    languageName = String.valueOf(variantKey.getLanguageId());
                }
                text.append(languageName);
            }
        }
        return text.toString();
    }
}
