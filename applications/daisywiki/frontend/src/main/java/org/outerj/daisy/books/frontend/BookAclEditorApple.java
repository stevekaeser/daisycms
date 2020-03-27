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
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.frontend.util.EncodingUtil;
import org.outerj.daisy.frontend.FrontEndContext;
import org.outerj.daisy.books.store.*;
import org.outerj.daisy.books.store.impl.AclResult;
import org.outerj.daisy.books.store.impl.BookAclEvaluator;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.user.UserManager;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.forms.formmodel.*;
import org.apache.cocoon.forms.validation.WidgetValidator;
import org.apache.cocoon.forms.util.I18nMessage;
import org.apache.cocoon.forms.FormContext;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

public class BookAclEditorApple extends AbstractDaisyApple implements Serviceable {
    private ServiceManager serviceManager;
    private boolean init = false;
    private Form form;
    private BookInstance bookInstance;
    private Locale locale;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        if (!init) {
            if (!request.getMethod().equals("POST"))
                throw new HttpMethodNotAllowedException(request.getMethod());

            String bookInstanceName = appleRequest.getSitemapParameter("bookInstanceName");
            Repository repository = frontEndContext.getRepository();
            BookStore bookStore = (BookStore)repository.getExtension("BookStore");
            bookInstance = bookStore.getBookInstance(bookInstanceName);
            BookAcl bookAcl = bookInstance.getAcl();

            form = FormHelper.createForm(serviceManager, "resources/form/bookacl_definition.xml");
            form.getChild("editmode").setValue("gui");
            form.getChild("editmode").setState(WidgetState.INVISIBLE);
            initForm(form, repository);
            load(form, bookAcl);
            annotateAclSubjectValues(form, repository);

            locale = frontEndContext.getLocale();

            init = true;
            appleResponse.redirectTo(EncodingUtil.encodePath(getMountPoint() + "/books/" + bookInstanceName + "/acl/" + getContinuationId()));
            return;
        }

        if (request.getMethod().equals("GET")) {
            showForm(frontEndContext, appleResponse);
        } else if (request.getMethod().equals("POST")) {
            boolean finished = form.process(new FormContext(request, locale));
            if (finished) {
                BookAcl bookAcl = getBookAcl(form);
                bookInstance.lock();
                try {
                    bookInstance.setAcl(bookAcl);
                } finally {
                    bookInstance.unlock();
                }
                appleResponse.redirectTo(EncodingUtil.encodePath(getMountPoint() + "/books"));
            } else {
                showForm(frontEndContext, appleResponse);
            }
        } else {
            throw new HttpMethodNotAllowedException(request.getMethod());
        }

    }

    private void showForm(FrontEndContext frontEndContext, AppleResponse appleResponse) throws Exception {
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("CocoonFormsInstance", form);
        viewData.put("locale", locale);
        viewData.put("pageContext", frontEndContext.getPageContext());
        appleResponse.sendPage("Form-bookacl-Pipe", viewData);
    }

    public static void load(Widget widget, BookAcl bookAcl) {
        Repeater entriesRepeater = (Repeater)widget.lookupWidget("editors/gui/entries");
        entriesRepeater.clear(); // in case load is called on an already loaded form
        BookAclEntry[] entries = bookAcl.getEntries();
        for (BookAclEntry entry : entries) {
            Repeater.RepeaterRow row = entriesRepeater.addRow();
            row.getChild("subjectType").setValue(entry.getSubjectType());
            row.getChild("subjectValue").setValue(new Long(entry.getSubjectValue()));
            row.getChild("readPerm").setValue(entry.getReadPermission());
            row.getChild("managePerm").setValue(entry.getManagePermission());
        }
    }

    public static BookAcl getBookAcl(Widget widget) {
        Repeater entriesRepeater = (Repeater)widget.lookupWidget("editors/gui/entries");
        BookAclEntry[] entries = new BookAclEntry[entriesRepeater.getSize()];
        for (int i = 0; i < entries.length; i++) {
            Repeater.RepeaterRow row = entriesRepeater.getRow(i);
            BookAclSubjectType subjectType = (BookAclSubjectType)row.getChild("subjectType").getValue();
            long subjectValue = ((Long)row.getChild("subjectValue").getValue()).longValue();
            BookAclActionType readPerm = (BookAclActionType)row.getChild("readPerm").getValue();
            BookAclActionType managePerm = (BookAclActionType)row.getChild("managePerm").getValue();
            entries[i] = new BookAclEntry(subjectType, subjectValue, readPerm, managePerm);
        }
        return new BookAcl(entries);
    }

    public static void annotateAclSubjectValues(Widget widget, Repository repository) {
        UserManager userManager = repository.getUserManager();
        Repeater entriesRepeater = (Repeater)widget.lookupWidget("editors/gui/entries");
        for (int k = 0; k < entriesRepeater.getSize(); k++) {
            Repeater.RepeaterRow entry = entriesRepeater.getRow(k);
            BookAclSubjectType subjectType = (BookAclSubjectType)entry.getChild("subjectType").getValue();
            long subjectValue;
            subjectValue = ((Long)entry.getChild("subjectValue").getValue()).longValue();
            if (subjectType == BookAclSubjectType.ROLE) {
                String roleName;
                try {
                    roleName = userManager.getRole(subjectValue, false).getName();
                } catch (Exception e) {
                    roleName = "(error)";
                }
                entry.getChild("subjectValueLabel").setValue(roleName);
            } else if (subjectType == BookAclSubjectType.USER) {
                String userName;
                try {
                    userName = userManager.getUserLogin(subjectValue);
                } catch (Exception e) {
                    userName = "(error)";
                }
                entry.getChild("subjectValueLabel").setValue(userName);
            }
        }
    }

    public static void initForm(Form form, Repository repository) {
        form.lookupWidget("editors/gui/entries").addValidator(new AclFormValidator(repository));
    }

    public static class AclFormValidator implements WidgetValidator {
        private Repository repository;

        public AclFormValidator(Repository repository) {
            this.repository = repository;
        }

        public boolean validate(Widget widget) {
            Form form = widget.getForm();
            AclResult result = BookAclEvaluator.evaluate(getBookAcl(form), repository.getUserId(), repository.getActiveRoleIds());
            if (!result.canManage()) {
                ((Messages)form.lookupWidget("editors/gui/messages")).addMessage(new I18nMessage("bookacl.error-cannot-exclude-yourself"));
                return false;
            } else {
                return true;
            }
        }
    }

}
