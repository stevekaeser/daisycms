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
package org.outerj.daisy.frontend.workflow;

import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.PartTypeUse;
import org.outerx.daisy.x10.PartTypeUseDocument;

public class PartTypeUseWrapper implements PartTypeUse {
    
    private PartTypeUse delegate;
    private boolean required;
    private boolean editable;
    
    public PartTypeUseWrapper(PartTypeUse partTypeUse, boolean required, boolean editable) {
        this.delegate = partTypeUse;
        this.required = required;
        this.editable = editable;
    }

    public PartTypeUseDocument getExtendedXml() {
        PartTypeUseDocument result = delegate.getExtendedXml();
        result.getPartTypeUse().setRequired(required);
        return result;
    }

    public PartType getPartType() {
        return delegate.getPartType();
    }

    public PartTypeUseDocument getXml() {
        PartTypeUseDocument result = delegate.getXml();
        result.getPartTypeUse().setRequired(required);
        result.getPartTypeUse().setEditable(editable);
        return result;
    }

    public boolean isEditable() {
        return editable;
    }

    public boolean isRequired() {
        return required;
    }

    public void setEditable(boolean editable) {
        throw new UnsupportedOperationException("This is a read-only PartTypeUse representation");
    }

    public void setRequired(boolean required) {
        throw new UnsupportedOperationException("This is a read-only PartTypeUse representation");
    }

}
