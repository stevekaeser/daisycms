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

import org.apache.cocoon.forms.datatype.convertor.Convertor;
import org.apache.cocoon.forms.datatype.convertor.ConversionResult;
import org.apache.cocoon.components.ContextHelper;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.service.ServiceManager;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.frontend.WikiHelper;
import org.outerj.daisy.frontend.FrontEndContext;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.workflow.WfVersionKey;

import java.util.Locale;

public class WfVersionKeyConvertor implements Convertor {
    private Context context;

    public WfVersionKeyConvertor(Context context) {
        this.context = context;
    }

    public ConversionResult convertFromString(String value, Locale locale, FormatCache formatCache) {
        FrontEndContext frontEndContext = FrontEndContext.get(ContextHelper.getRequest(context));

        Repository repository;
        try {
            repository = frontEndContext.getRepository();
        } catch (Exception e) {
            throw new RuntimeException("Error getting access to the repository in " + this.getClass().getName(), e);
        }
        SiteConf siteConf = frontEndContext.getSiteConf();

        WfVersionKey versionKey = null;
        try {
            versionKey = WfVersionKeyUtil.parseWfVersionKey(value, repository, siteConf);
        } catch (Throwable e) {
            /* ignore */
        }

        if (versionKey != null) {
            try {
                value = WfVersionKeyUtil.versionKeyToString(versionKey, repository);
            } catch (Throwable e) {
                /* ignore */
            }
        }

        return new ConversionResult(value);
    }

    public String convertToString(Object object, Locale locale, FormatCache formatCache) {
        return (String)object;
    }

    public Class getTypeClass() {
        return java.lang.String.class;
    }

    public void generateSaxFragment(ContentHandler contentHandler, Locale locale) throws SAXException {
        // nothing to say about myself
    }
}
