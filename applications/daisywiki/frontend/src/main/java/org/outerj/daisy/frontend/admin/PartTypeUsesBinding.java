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
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.formmodel.Repeater;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.PartTypeUse;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.Repository;

public class PartTypeUsesBinding extends AbstractCustomBinding {
    protected void doLoad(Widget widget, org.apache.commons.jxpath.JXPathContext jxPathContext) throws Exception {
        Repeater repeater = (Repeater)widget;

        DocumentType documentType = (DocumentType)jxPathContext.getValue(".");
        PartTypeUse[] partTypeUses = documentType.getPartTypeUses();
        for (PartTypeUse partTypeUse : partTypeUses) {
            Repeater.RepeaterRow row = repeater.addRow();
            row.getChild("id").setValue(partTypeUse.getPartType().getId());
            row.getChild("name").setValue(partTypeUse.getPartType().getName());
            row.getChild("required").setValue(partTypeUse.isRequired());
            row.getChild("editable").setValue(partTypeUse.isEditable());
        }
    }

    protected void doSave(Widget widget, org.apache.commons.jxpath.JXPathContext jxPathContext) throws Exception {
        Repeater repeater = (Repeater)widget;

        DocumentType documentType = (DocumentType)jxPathContext.getValue(".");
        documentType.clearPartTypeUses();
        Repository repository = (Repository)widget.getForm().getAttribute("DaisyRepository");
        RepositorySchema repositorySchema = repository.getRepositorySchema();
        for (int i = 0; i < repeater.getSize(); i++) {
            Long partTypeId = (Long)repeater.getRow(i).getChild("id").getValue();
            PartType partType = repositorySchema.getPartTypeById(partTypeId.longValue(), false);
            PartTypeUse partTypeUse = documentType.addPartType(partType, (Boolean)repeater.getRow(i).getChild("required").getValue());
            partTypeUse.setEditable((Boolean)repeater.getRow(i).getChild("editable").getValue());
        }
    }
}
