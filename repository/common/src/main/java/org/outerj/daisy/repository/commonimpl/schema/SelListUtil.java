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

import org.outerj.daisy.repository.schema.ListItem;
import org.outerj.daisy.repository.VariantKey;
import org.outerx.daisy.x10.ExpSelectionListDocument;
import org.outerx.daisy.x10.ExpListItemDocument;
import org.outerx.daisy.x10.LinkDocument;

import java.util.List;
import java.util.ArrayList;

public class SelListUtil {
    protected static List<ListItem> buildListFromXml(ExpSelectionListDocument listDocument) {
        List<ExpListItemDocument.ExpListItem> itemsXml = listDocument.getExpSelectionList().getExpListItemList();
        List<ListItem> items = new ArrayList<ListItem>(itemsXml.size());

        for (ExpListItemDocument.ExpListItem itemXml : itemsXml) {
            items.add(buildFromXml(itemXml));
        }

        return items;
    }

    protected static LinkListItem buildFromXml(ExpListItemDocument.ExpListItem itemXml) {
        LinkDocument.Link linkXml = itemXml.getValue().getLinkArray(0);
        VariantKey variantKey = new VariantKey(linkXml.getDocumentId(), linkXml.getBranchId(), linkXml.getLanguageId());

        List<LinkListItem> children = new ArrayList<LinkListItem>();
        for (ExpListItemDocument.ExpListItem childXml : itemXml.getExpListItemList()) {
            children.add(buildFromXml(childXml));
        }

        String label = itemXml.getLabel();
        return new LinkListItem(variantKey, label, children);
    }
}
