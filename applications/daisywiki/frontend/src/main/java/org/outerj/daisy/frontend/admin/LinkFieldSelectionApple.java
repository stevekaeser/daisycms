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
package org.outerj.daisy.frontend.admin;

import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.schema.FieldType;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Apple to show list of link fields.
 */
public class LinkFieldSelectionApple extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Repository repository = frontEndContext.getRepository();
        FieldType[] fieldTypes = repository.getRepositorySchema().getAllFieldTypes(false).getArray();
        List<FieldType> hierarchicalFieldTypes = new ArrayList<FieldType>();
        for (FieldType fieldType : fieldTypes) {
            if (fieldType.getValueType() == ValueType.LINK)
                hierarchicalFieldTypes.add(fieldType);
        }

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("linkFieldTypes", hierarchicalFieldTypes);
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("locale", frontEndContext.getLocale());
        viewData.put("localeAsString", frontEndContext.getLocaleAsString());

        appleResponse.sendPage("LinkFieldsPopupPipe", viewData);
    }
}
