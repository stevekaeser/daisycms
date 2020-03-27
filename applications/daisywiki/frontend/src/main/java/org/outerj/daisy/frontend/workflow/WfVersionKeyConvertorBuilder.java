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

import org.apache.cocoon.forms.datatype.convertor.ConvertorBuilder;
import org.apache.cocoon.forms.datatype.convertor.Convertor;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.w3c.dom.Element;

public class WfVersionKeyConvertorBuilder implements ConvertorBuilder, Contextualizable {
    private Context context;

    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

    public Convertor build(Element element) throws Exception {
        return new WfVersionKeyConvertor(context);
    }
}
