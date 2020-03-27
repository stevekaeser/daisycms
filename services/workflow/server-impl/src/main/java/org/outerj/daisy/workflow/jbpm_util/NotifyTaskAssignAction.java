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
package org.outerj.daisy.workflow.jbpm_util;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;


/**
 * jBPM action for use in process definitions to sent notifications on task assignment.
 * It should be associated with the "task-assign" event of a task. 
 */
public class NotifyTaskAssignAction implements ActionHandler {
    private TaskMailerAction delegate;
    
    public NotifyTaskAssignAction () {
        Document doc = DocumentHelper.createDocument();        
        Element element = doc.addElement("mailTemplate");
        element.setText("taskassigned.ftl");
        
        this.delegate = new TaskMailerAction();
        this.delegate.mailTemplate = element;
    }
    
    public void execute(ExecutionContext executionContext) throws Exception {
        this.delegate.execute(executionContext);
    }
}
