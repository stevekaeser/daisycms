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
package org.outerj.daisy.books.publisher.impl.util;

import org.outerx.daisy.x10Publisher.VariablesConfigType;
import org.outerx.daisy.x10Publisher.VariantKeyType;
import org.outerj.daisy.util.Constants;

import java.util.regex.Matcher;
import java.util.Map;

public class BookVariablesConfigBuilder {
    public static VariablesConfigType buildVariablesConfig(Map<String, String> bookMetaData, String defaultBranch, String defaultLanguage) throws Exception {
        VariablesConfigType variablesConfig = VariablesConfigType.Factory.newInstance();
        VariablesConfigType.VariableSources variableSources = variablesConfig.addNewVariableSources();

        String prefix = "variables.source.";
        for (int i = 1; true; i++) {
            String propertyName = prefix + i;
            String location = bookMetaData.get(propertyName);
            if (location == null)
                break;

            Matcher matcher = Constants.DAISY_LINK_PATTERN.matcher(location);
            if (matcher.matches()) {
                String docId = matcher.group(1);
                String branch = matcher.group(2);
                if (branch == null || branch.equals(""))
                    branch = String.valueOf(defaultBranch);
                String language = matcher.group(3);
                if (language == null || language.equals(""))
                    language = String.valueOf(defaultLanguage);

                VariantKeyType variableDocument = variableSources.addNewVariableDocument();
                variableDocument.setId(docId);
                variableDocument.setBranch(branch);
                variableDocument.setLanguage(language);
            } else {
                throw new Exception("Invalid link in " + propertyName + " : " + location);
            }
        }

        VariablesConfigType.VariablesInAttributes varsInAttrs = variablesConfig.addNewVariablesInAttributes();
        String resolveInAttrs = bookMetaData.get("variables.resolve-attributes");
        if (resolveInAttrs == null || resolveInAttrs.equalsIgnoreCase("true"))
            varsInAttrs.setAllAttributes(true);

        return variablesConfig;
    }
}
