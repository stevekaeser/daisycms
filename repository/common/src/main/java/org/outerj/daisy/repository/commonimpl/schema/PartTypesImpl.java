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

import org.outerj.daisy.repository.schema.PartTypes;
import org.outerj.daisy.repository.schema.PartType;
import org.outerx.daisy.x10.PartTypesDocument;
import org.outerx.daisy.x10.PartTypeDocument;

public class PartTypesImpl implements PartTypes {
    private PartType[] partTypes;

    public PartTypesImpl(PartType[] partTypes) {
        this.partTypes = partTypes;
    }

    public PartType[] getArray() {
        return partTypes;
    }

    public PartTypesDocument getXml() {
        PartTypeDocument.PartType[] partTypeXml = new PartTypeDocument.PartType[partTypes.length];
        for (int i = 0; i < partTypes.length; i++) {
            partTypeXml[i] = partTypes[i].getXml().getPartType();
        }

        PartTypesDocument partTypesDocument = PartTypesDocument.Factory.newInstance();
        PartTypesDocument.PartTypes partTypesXml = partTypesDocument.addNewPartTypes();
        partTypesXml.setPartTypeArray(partTypeXml);
        return partTypesDocument;
    }

    public int size() {
        return partTypes.length;
    }
}
