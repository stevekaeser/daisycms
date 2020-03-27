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
package org.outerj.daisy.books.frontend;

import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.books.publisher.BookPublisher;
import org.outerj.daisy.books.publisher.PublishTaskInfo;
import org.outerj.daisy.repository.Repository;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.activity.Disposable;

import java.util.Map;
import java.util.HashMap;

public class ListPublishTasksApple extends AbstractDaisyApple implements StatelessAppleController, Serviceable, Disposable {
    private ServiceManager serviceManager;
    private BookPublisher bookPublisher;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
        this.bookPublisher = (BookPublisher)serviceManager.lookup(BookPublisher.ROLE);
    }

    public void dispose() {
        serviceManager.release(bookPublisher);
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Repository repository = frontEndContext.getRepository();

        if (repository.isInRole("guest") && repository.getActiveRoleIds().length == 1) {
            throw new Exception("Users in the guest role are not allowed to retrieve the list of book publish tasks.");
        }

        PublishTaskInfo[] taskInfos = bookPublisher.getTaskOverview(frontEndContext.getLocale());
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("publishTasks", taskInfos);

        appleResponse.sendPage("PublishTasksPipe", viewData);
    }
}
