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
package org.outerj.daisy.repository;

import org.outerx.daisy.x10.VariantKeysDocument;
import org.outerx.daisy.x10.VariantKeyDocument;

import java.util.List;

public final class VariantKeys {
    private final VariantKey[] variantKeys;

    public VariantKeys(VariantKey[] variantKeys) {
        this.variantKeys = variantKeys;
    }

    public VariantKey[] getArray() {
        return variantKeys;
    }

    public VariantKeysDocument getXml() {
        VariantKeyDocument.VariantKey[] variantKeysXml = new VariantKeyDocument.VariantKey[variantKeys.length];
        for (int i = 0; i < variantKeys.length; i++)
            variantKeysXml[i] = variantKeys[i].getXml();

        VariantKeysDocument variantKeysDocument = VariantKeysDocument.Factory.newInstance();
        variantKeysDocument.addNewVariantKeys().setVariantKeyArray(variantKeysXml);

        return variantKeysDocument;
    }

    public static VariantKeys fromXml(VariantKeysDocument variantKeysDocument) {
        List<VariantKeyDocument.VariantKey> variantKeysXml = variantKeysDocument.getVariantKeys().getVariantKeyList();
        VariantKey[] variantKeys = new VariantKey[variantKeysXml.size()];

        for (int i = 0; i < variantKeysXml.size(); i++) {
            VariantKeyDocument.VariantKey variantKeyXml = variantKeysXml.get(i);
            variantKeys[i] = new VariantKey(variantKeyXml.getDocumentId(), variantKeyXml.getBranchId(), variantKeyXml.getLanguageId());
        }

        return new VariantKeys(variantKeys);
    }
}
