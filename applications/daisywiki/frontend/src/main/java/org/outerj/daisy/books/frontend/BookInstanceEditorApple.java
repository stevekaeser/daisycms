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
import org.outerj.daisy.frontend.util.XslUtil;
import org.outerj.daisy.frontend.PageContext;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.books.store.BookStore;
import org.outerj.daisy.books.store.BookInstance;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;

import java.util.Map;
import java.util.HashMap;

public class BookInstanceEditorApple extends AbstractDaisyApple implements StatelessAppleController, Serviceable {
    private ServiceManager serviceManager;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Repository repository = frontEndContext.getRepository();
        BookStore bookStore = (BookStore)repository.getExtension("BookStore");
        String bookInstanceName = appleRequest.getSitemapParameter("bookInstanceName");
        BookInstance bookInstance = bookStore.getBookInstance(bookInstanceName);

        PageContext pageContext = frontEndContext.getPageContext();
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageContext", pageContext);
        viewData.put("bookInstanceName", bookInstanceName);
        viewData.put("metaData", bookInstance.getMetaData());
        viewData.put("publicationInfos", bookInstance.getPublicationsInfo().getInfos());
        viewData.put("daisyutil", new XslUtil()); // ugly

        appleResponse.sendPage("BookInstanceEditorPipe", viewData);
    }
}
