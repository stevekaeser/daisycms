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

import org.apache.xmlbeans.XmlCursor;
import org.outerj.daisy.doctaskrunner.TaskDocDetail;
import org.outerj.daisy.doctaskrunner.TaskDocDetails;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerx.daisy.x10Doctaskrunner.TaskDocDetailDocument;
import org.outerx.daisy.x10Doctaskrunner.TaskDocDetailsDocument;

public class TaskDocDetailsImpl implements TaskDocDetails {
    private final TaskDocDetail[] taskDocDetails;

    public TaskDocDetailsImpl(TaskDocDetail[] taskDocDetails) {
        this.taskDocDetails = taskDocDetails;
    }

    public TaskDocDetail[] getArray() {
        return taskDocDetails;
    }

    public TaskDocDetailsDocument getXml() {
        TaskDocDetailsDocument detailsDocument = TaskDocDetailsDocument.Factory.newInstance();
        TaskDocDetailDocument.TaskDocDetail[] detailsXml = new TaskDocDetailDocument.TaskDocDetail[taskDocDetails.length];

        for (int i = 0; i < taskDocDetails.length; i++) {
            detailsXml[i] = taskDocDetails[i].getXml().getTaskDocDetail();
        }

        detailsDocument.addNewTaskDocDetails().setTaskDocDetailArray(detailsXml);
        return detailsDocument;
    }

    public TaskDocDetailsDocument getAnnotatedXml(Repository repository) {
        TaskDocDetailsDocument taskDocDetailsDoc = getXml(); 
        VariantManager variantManager = repository.getVariantManager();
        for (TaskDocDetailDocument.TaskDocDetail taskDocDetailXml : taskDocDetailsDoc.getTaskDocDetails().getTaskDocDetailList()) {
            long branchId = taskDocDetailXml.getBranchId();
            long languageId = taskDocDetailXml.getLanguageId();
            String branch;
            String language;
            try {
                branch = variantManager.getBranch(branchId, false).getName();
            } catch (RepositoryException e) {
                branch = String.valueOf(branchId);
            }
            try {
                language = variantManager.getLanguage(languageId, false).getName();
            } catch (RepositoryException e) {
                language = String.valueOf(languageId);
            }
            XmlCursor cursor = taskDocDetailXml.newCursor();
            cursor.toNextToken();
            cursor.insertAttributeWithValue("branch", branch);
            cursor.insertAttributeWithValue("language", language);
            cursor.dispose();
        }
        
        return taskDocDetailsDoc;
    }
}
