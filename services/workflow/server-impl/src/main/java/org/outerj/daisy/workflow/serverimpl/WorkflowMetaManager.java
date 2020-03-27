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
package org.outerj.daisy.workflow.serverimpl;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.file.def.FileDefinition;
import org.outerj.daisy.workflow.WorkflowException;
import org.outerj.daisy.i18n.impl.AggregateResourceBundle;
import org.outerj.daisy.i18n.impl.DResourceBundleFactory;
import org.outerj.daisy.i18n.DResourceBundle;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlError;
import org.outerx.daisy.x10Workflowmeta.WorkflowMetaDocument;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class WorkflowMetaManager {
    public static final String DAISY_PROCESS_META_FILENAME = "daisy-process-meta.xml";
    private static final Pattern I18N_FILE_PATTERN = Pattern.compile("^i18n/([^.]+)\\.xml$");

    /**
     * Throws an exception if the metadata is not valid (not parseable,
     * schema error, io error, ...).
     */
    public WfMetaWrapper getWorkflowMeta(ProcessDefinition definition) throws WorkflowException {
        try {
            Map<String, byte[]> files;
            FileDefinition fileDefinition = definition.getFileDefinition();
            if (fileDefinition != null) {
                files = fileDefinition.getBytesMap();
            } else {
                files = Collections.emptyMap();
            }

            byte[] daisyProcessMetaBytes = files.get(DAISY_PROCESS_META_FILENAME);

            WorkflowMetaDocument workflowMetaDocument;
            if (daisyProcessMetaBytes != null) {
                workflowMetaDocument = WorkflowMetaDocument.Factory.parse(new ByteArrayInputStream(daisyProcessMetaBytes));
                XmlOptions xmlOptions = new XmlOptions();
                List<XmlError> errors = new ArrayList<XmlError>();
                xmlOptions.setErrorListener(errors);
                boolean valid = workflowMetaDocument.validate(xmlOptions);
                if (!valid) {
                    StringBuilder errorMsg = new StringBuilder();
                    for (XmlError xmlError : errors) {
                        if (errorMsg.length() > 0)
                            errorMsg.append(", ");
                        errorMsg.append(xmlError.getMessage());
                    }
                    throw new WorkflowException("The Daisy workflow metadata does not conform to the XML schema, the following errors occured: " + errorMsg);
                }
            } else {
                // No Daisy meta data available in process definition, create an empty dummy meta data document
                workflowMetaDocument = WorkflowMetaDocument.Factory.newInstance();
                workflowMetaDocument.addNewWorkflowMeta();
            }

            AggregateResourceBundle i18nBundle = new AggregateResourceBundle(getBundleNames(workflowMetaDocument));
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                String fileName = entry.getKey();
                Matcher i18nFileMatcher = I18N_FILE_PATTERN.matcher(fileName);
                if (i18nFileMatcher.matches()) {
                    String i18nFileName = i18nFileMatcher.group(1);
                    Object[] baseNameAndLocale = getBaseNameAndLocale(i18nFileName);
                    String baseName = (String)baseNameAndLocale[0];
                    Locale locale = (Locale)baseNameAndLocale[1];
                    DResourceBundle resourceBundle = DResourceBundleFactory.build(new InputSource(new ByteArrayInputStream(entry.getValue())));
                    i18nBundle.addBundle(baseName, locale, resourceBundle);
                }
            }

            return new WfMetaWrapper(workflowMetaDocument, i18nBundle);
        } catch (Exception e) {
            throw new WorkflowException("There is an error with the " + DAISY_PROCESS_META_FILENAME + " or one of the i18n bundles embedded in the process archive.", e);
        }
    }

    private Object[] getBaseNameAndLocale(String fileName) {
        String[] suffix1, suffix2 = null, suffix3 = null;
        suffix1 = getSuffix(fileName);
        if (suffix1 != null) {
            suffix2 = getSuffix(suffix1[0]);
            if (suffix2 != null) {
                suffix3 = getSuffix(suffix2[0]);
            }
        }

        Locale locale;
        String baseName;
        if (suffix1 != null && suffix2 != null && suffix3 != null) {
            locale = new Locale(suffix3[1], suffix2[1], suffix1[1]);
            baseName = suffix3[0];
        } else if (suffix1 != null && suffix2 != null) {
            locale = new Locale(suffix2[1], suffix1[1]);
            baseName = suffix2[0];
        } else if (suffix1 != null) {
            locale = new Locale(suffix1[1]);
            baseName = suffix1[0];
        } else {
            locale = new Locale("");
            baseName = fileName;
        }

        return new Object[] {baseName, locale};
    }

    private String[] getSuffix(String fileName) {
        int up = fileName.lastIndexOf('_');
        if (up != -1) {
            String suff = fileName.substring(up + 1, fileName.length());
            return new String[] {fileName.substring(0, up), suff};
        }
        return null;
    }

    private String[] getBundleNames(WorkflowMetaDocument workflowMetaDocument) {
        WorkflowMetaDocument.WorkflowMeta workflowMeta = workflowMetaDocument.getWorkflowMeta();
        if (workflowMeta.isSetResourceBundles()) {
            return workflowMeta.getResourceBundles().getResourceBundleList().toArray(new String[0]);
        }
        return new String[0];
    }
}
