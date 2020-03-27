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
package org.outerj.daisy.doctaskrunner.serverimpl.actions;

import org.outerj.daisy.doctaskrunner.TaskContext;
import org.outerj.daisy.doctaskrunner.TaskSpecification;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;

public class JavascriptDocumentAction extends AbstractJavascriptDocumentAction {
    
    @Override
    public String getScriptCode() {
        return taskSpecification.getParameters();
    }

    @Override
    public void onSetup(VariantKey[] variantKeys,
            TaskSpecification taskSpecification, TaskContext taskContext,
            Repository repository) throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void onTearDown() throws Exception {
        // TODO Auto-generated method stub
    }

    public boolean requiresAdministratorRole() {
        return true;
    }

}