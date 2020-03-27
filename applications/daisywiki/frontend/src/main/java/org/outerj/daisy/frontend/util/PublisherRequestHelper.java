/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.frontend.util;

import org.outerx.daisy.x10Publisher.VariablesConfigType;
import org.outerx.daisy.x10Publisher.VariantKeyType;
import org.outerx.daisy.x10Publisher.NavigationTreeDocument;
import org.outerx.daisy.x10Publisher.PublisherRequestDocument;
import org.outerj.daisy.frontend.FrontEndContext;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.components.config.ConfigurationManager;
import org.apache.avalon.framework.configuration.Configuration;

public class PublisherRequestHelper {
    public static PublisherRequestDocument createTemplateRequest(FrontEndContext frontEndContext) {
        PublisherRequestDocument pubReqDoc = PublisherRequestDocument.Factory.newInstance();
        PublisherRequestDocument.PublisherRequest pubReq = pubReqDoc.addNewPublisherRequest();
        pubReq.setVersionMode(frontEndContext.getVersionMode().toString());
        pubReq.setLocale(frontEndContext.getLocaleAsString());
        pubReq.setVariablesConfig(PublisherRequestHelper.getVariablesConfig(frontEndContext));
        return pubReqDoc;
    }

    /**
     * Creates the variables configuration to be inserted in publisher requests.
     */
    public static VariablesConfigType getVariablesConfig(FrontEndContext frontEndContext) {
        VariablesConfigType variablesConfig = VariablesConfigType.Factory.newInstance();

        if (!frontEndContext.inSite())
            return variablesConfig;

        SiteConf siteConf = frontEndContext.getSiteConf();
        ConfigurationManager configurationManager = frontEndContext.getConfigurationManager();
        Configuration conf = configurationManager.getConfiguration(siteConf.getName(), "variables");

        if (conf == null)
            return variablesConfig;

        Configuration variableSources = conf.getChild("variableSources", false);
        if (variableSources != null) {
            Configuration[] variableDocs = variableSources.getChildren("variableDocument");
            VariablesConfigType.VariableSources variableSourcesXml = null;
            for (Configuration doc : variableDocs) {
                String id = doc.getAttribute("id", null);
                if (id == null) {
                    frontEndContext.getLog().error("Error in variables configuration: missing id attribute at " + doc.getLocation());
                    continue;
                }

                String branch = doc.getAttribute("branch", null);
                String language = doc.getAttribute("language", null);
                if (branch == null)
                    branch = String.valueOf(siteConf.getBranchId());
                if (language == null)
                    language = String.valueOf(siteConf.getLanguageId());

                if (variableSourcesXml == null) {
                    variableSourcesXml = variablesConfig.addNewVariableSources();
                }

                VariantKeyType variableDocument = variableSourcesXml.addNewVariableDocument();
                variableDocument.setId(id);
                variableDocument.setBranch(branch);
                variableDocument.setLanguage(language);
            }
        }

        Configuration variablesInAttrs = conf.getChild("variablesInAttributes", false);
        if (variablesInAttrs != null) {
            boolean allAttributes = variablesInAttrs.getAttribute("allAttributes", "false").equals("true");

            VariablesConfigType.VariablesInAttributes varsInAttrsXml = variablesConfig.addNewVariablesInAttributes();
            varsInAttrsXml.setAllAttributes(allAttributes);


            Configuration[] elements = variablesInAttrs.getChildren("element");
            for (Configuration element : elements) {
                String name = element.getAttribute("name", null);
                String attributes = element.getAttribute("attributes", null);
                if (name != null && attributes != null) {
                    VariablesConfigType.VariablesInAttributes.Element elementXml = varsInAttrsXml.addNewElement();
                    elementXml.setName(name);
                    elementXml.setAttributes(attributes);
                }
            }

        }

        return variablesConfig;
    }

    public static NavigationTreeDocument.NavigationTree getNavigationTree(FrontEndContext frontEndContext) {
        SiteConf siteConf = frontEndContext.getSiteConf();

        NavigationTreeDocument.NavigationTree navTree = NavigationTreeDocument.NavigationTree.Factory.newInstance();

        VariantKeyType navDoc = navTree.addNewNavigationDocument();
        navDoc.setId(siteConf.getNavigationDocId());
        navDoc.setBranch(String.valueOf(siteConf.getBranchId()));
        navDoc.setLanguage(String.valueOf(siteConf.getLanguageId()));

        navTree.setContextualized(siteConf.contextualizedTree());
        navTree.setDepth(siteConf.getNavigationDepth());

        return navTree;
    }

    public static NavigationTreeDocument.NavigationTree[] getNavigationTreeArray(FrontEndContext frontEndContext) {
        return new NavigationTreeDocument.NavigationTree[] { getNavigationTree(frontEndContext) };
    }
}
