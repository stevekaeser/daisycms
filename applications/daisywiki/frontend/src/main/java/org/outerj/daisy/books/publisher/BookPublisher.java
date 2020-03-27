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
package org.outerj.daisy.books.publisher;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.books.store.BookAcl;

import java.util.Locale;
import java.util.Map;

public interface BookPublisher {
    public static final String ROLE = BookPublisher.class.getName();

    /**
     * @return an array containing two elements: the task ID and the (normalized) book instance name
     */
    String[] publishBook(Repository repository, VariantKey bookDefinition, long dataBranchId, long dataLanguageId,
                     VersionMode dataVersion, Locale locale, String bookInstanceName, String bookInstanceLabel,
                     String daisyCocoonPath, String daisyContextPath, PublicationSpec[] specs, BookAcl acl) throws Exception;

    PublicationTypeInfo[] getAvailablePublicationTypes() throws Exception;

    Map<String, String> getDefaultProperties(String publicationTypeName) throws Exception ;

    /**
     * @return null if the task with the given ID does not exist (or is finished, which is the same)
     */
    String[] getTaskState(String taskId);

    /**
     * @return a list of all running book publish tasks, of all users.
     */
    PublishTaskInfo[] getTaskOverview(Locale locale);
}
