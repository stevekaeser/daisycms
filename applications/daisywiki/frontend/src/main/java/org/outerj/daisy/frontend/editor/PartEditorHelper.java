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

import java.util.Arrays;

import org.apache.cocoon.forms.formmodel.Widget;
import org.outerj.daisy.frontend.HtmlHelper;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.schema.PartType;

public class PartEditorHelper {

    public static void load(Widget widget, PartType partType, Document document) throws Exception {
        widget.setValue(new String(document.getPart(partType.getId()).getData(), "UTF-8"));
    }

    /**
     *
     * @param data alternative data instead of the field value, useful if the widget's value has to be postprocessed
     */
    public static void save(Widget widget, PartType partType, Document document, String mimeType, byte[] data) throws Exception {
        if (widget == null && data == null) {
            throw new IllegalArgumentException("Either field or data argument should be non-null");
        } else if (widget != null && (widget.getValue() == null || HtmlHelper.isEmpty((String)widget.getValue()))) {
            document.deletePart(partType.getId());
        } else {
            byte[] newData = data != null ? data : ((String)widget.getValue()).getBytes("UTF-8");

            if (newData == null) {
                document.deletePart(partType.getId());
                return;
            }

            boolean mimeTypeChanged = false;
            if (!document.isVariantNew()) {
                // Compare new data with previous data, so that we don't store new data
                // if it hasn't changed.
                Version lastVersion =  document.getLastVersion();
                if (lastVersion.hasPart(partType.getId())) {
                    Part part = lastVersion.getPart(partType.getId());
                    mimeTypeChanged = !part.getMimeType().equals(mimeType);
                    byte[] oldData = part.getData();
                    if (Arrays.equals(newData, oldData))
                        newData = null;
                }
            }

            if (newData != null)
                document.setPart(partType.getId(), mimeType, newData);
            else if (mimeTypeChanged)
                document.setPartMimeType(partType.getId(), mimeType);
        }
    }

}
