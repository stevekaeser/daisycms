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
package org.outerj.daisy.doctaskrunner.commonimpl;

import org.outerj.daisy.doctaskrunner.DocumentExecutionState;
import org.outerj.daisy.doctaskrunner.TaskDocDetail;
import org.outerj.daisy.repository.VariantKey;
import org.outerx.daisy.x10Doctaskrunner.TaskDocDetailDocument;

public class TaskDocDetailImpl implements TaskDocDetail {
    private final VariantKey variantKey;
    private final DocumentExecutionState state;
    private final String details;
    private final int tryCount;

    public TaskDocDetailImpl(VariantKey variantKey, DocumentExecutionState state, String details, int tryCount) {
        this.variantKey = variantKey;
        this.state = state;
        this.details = details;
        this.tryCount = tryCount;
    }

    public VariantKey getVariantKey() {
        return variantKey;
    }

    public DocumentExecutionState getState() {
        return state;
    }

    public String getDetails() {
        return details;
    }
    
    public int getTryCount() {
        return tryCount;
    }

    public TaskDocDetailDocument getXml() {
        TaskDocDetailDocument taskDocDetailDocument = TaskDocDetailDocument.Factory.newInstance();
        TaskDocDetailDocument.TaskDocDetail taskDocDetailXml = taskDocDetailDocument.addNewTaskDocDetail();

        taskDocDetailXml.setDocumentId(variantKey.getDocumentId());
        taskDocDetailXml.setBranchId(variantKey.getBranchId());
        taskDocDetailXml.setLanguageId(variantKey.getLanguageId());        
        taskDocDetailXml.setState(TaskDocDetailDocument.TaskDocDetail.State.Enum.forString(state.toString()));
        taskDocDetailXml.setTryCount(this.tryCount);
        if (details != null)
            taskDocDetailXml.setDetails(details);

        return taskDocDetailDocument;
    }
}
