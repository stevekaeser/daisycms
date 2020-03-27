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
package org.outerj.daisy.repository.commonimpl.schema;

import org.outerj.daisy.repository.schema.PartTypeUse;
import org.outerj.daisy.repository.schema.PartType;
import org.outerx.daisy.x10.PartTypeUseDocument;

public class PartTypeUseImpl implements PartTypeUse {
    private PartType partType;
    private boolean required;
    private boolean editable = true;
    private DocumentTypeImpl owner;
    private static final String READ_ONLY_MESSAGE = "This part-type-use is read-only.";

    public PartTypeUseImpl(DocumentTypeImpl owner, PartType partType, boolean required) {
        this.owner = owner;
        this.partType = partType;
        this.required = required;
    }

    public PartType getPartType() {
        return partType;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        if (owner.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        this.required = required;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        if (owner.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        this.editable = editable;
    }

    public PartTypeUseDocument getXml() {
        return getXml(false);
    }

    public PartTypeUseDocument getExtendedXml() {
        return getXml(true);
    }

    private PartTypeUseDocument getXml(boolean extended) {
        PartTypeUseDocument partTypeUseDoc = PartTypeUseDocument.Factory.newInstance();
        PartTypeUseDocument.PartTypeUse partTypeUseXml = partTypeUseDoc.addNewPartTypeUse();
        partTypeUseXml.setPartTypeId(getPartType().getId());
        partTypeUseXml.setRequired(isRequired());
        partTypeUseXml.setEditable(isEditable());
        if (extended)
            partTypeUseXml.setPartType(getPartType().getXml().getPartType());
        return partTypeUseDoc;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof PartTypeUseImpl))
            return false;

        PartTypeUseImpl other = (PartTypeUseImpl)obj;

        if (partType.getId() != other.partType.getId())
            return false;

        if (editable != other.editable)
            return false;

        return required == other.required;
    }
}
