#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
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
package ${package};

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.workflow.WfVersionKey;

/**
 * Sample workflow action
 */
public class SampleAction implements ActionHandler {

    /**
     * Custom field to set
     */
    public String customField;

    /**
     * New value to set.
     */
    public String value;

    public void execute(ExecutionContext context) throws Exception {
        ContextInstance contextInstance = context.getContextInstance();
        WfVersionKey key = (WfVersionKey)contextInstance.getVariable("daisy_document");
        Repository wfRepository = (Repository)contextInstance.getTransientVariable("wfRepository");
        Document doc = wfRepository.getDocument(key.getVariantKey(), true);
        doc.setCustomField(customField, value);
        doc.save();
    }

}
