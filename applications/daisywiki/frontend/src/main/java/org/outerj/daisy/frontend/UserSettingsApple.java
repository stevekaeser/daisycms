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
package org.outerj.daisy.frontend;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.forms.FormContext;
import org.apache.cocoon.forms.FormsConstants;
import org.apache.cocoon.forms.binding.Binding;
import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.util.I18nMessage;
import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.cocoon.forms.validation.WidgetValidator;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.outerj.daisy.emailnotifier.CollectionSubscriptionKey;
import org.outerj.daisy.emailnotifier.EmailSubscriptionManager;
import org.outerj.daisy.emailnotifier.Subscription;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.ResponseUtil;
import org.outerj.daisy.repository.CollectionManager;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryImpl;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.variant.VariantManager;

public class UserSettingsApple extends AbstractDaisyApple implements Serviceable, LogEnabled {
    private ServiceManager serviceManager;
    private boolean init = false;
    private Locale locale;
    private Form form;
    private Binding userBinding;
    private Binding subscriptionBinding;
    private Repository repository;
    private User user;
    private Subscription subscription;
    private Map<String, Object> viewDataTemplate;
    private String returnTo;
    private Logger logger;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        EmailSubscriptionManager subscriptionManager;

        if (!init) {
            repository = frontEndContext.getRepository();
            user = repository.getUserManager().getUser(repository.getUserId(), true);
            if (!user.isUpdateableByUser()) {
                throw new Exception("You do not have access to this function.");
            }

            returnTo = request.getParameter("returnTo");
            if (returnTo == null || returnTo.equals(""))
                returnTo = getMountPoint() + "/";
            locale = frontEndContext.getLocale();
            form = FormHelper.createForm(serviceManager, "resources/form/usersettings_definition.xml");
            userBinding = FormHelper.createBinding(serviceManager, "resources/form/usersettings_userbinding.xml");
            subscriptionBinding = FormHelper.createBinding(serviceManager, "resources/form/usersettings_subscriptionbinding.xml");

            subscriptionManager = (EmailSubscriptionManager)repository.getExtension("EmailSubscriptionManager");
            subscription = subscriptionManager.getSubscription();

            form.addValidator(new CurrentPasswordRequiredValidator());
            form.getChild("currentPassword").addValidator(new CurrentPasswordValidator());
            form.getChild("subscribedDocuments").addValidator(KeySetParser.getVariantKeysWidgetValidator(repository.getVariantManager(), true, false));
            form.getChild("subscribedCollections").addValidator(KeySetParser.getCollectionSubscriptionKeysWidgetValidator(repository, true, false));

            userBinding.loadFormFromModel(form, user);
            subscriptionBinding.loadFormFromModel(form, subscription);
            subscriptionAdditionalLoad();

            String path = getMountPoint() + "/usersettings/" + getContinuationId();

            viewDataTemplate = new HashMap<String, Object>();
            viewDataTemplate.put("submitPath", path);
            viewDataTemplate.put("locale", locale);
            viewDataTemplate.put("returnTo", returnTo);
            viewDataTemplate.put("collectionsArray", repository.getCollectionManager().getCollections(false).getArray());
            viewDataTemplate.put("branchesArray", repository.getVariantManager().getAllBranches(false).getArray());
            viewDataTemplate.put("languagesArray", repository.getVariantManager().getAllLanguages(false).getArray());
            viewDataTemplate.put("locales", new AvailableLocales(serviceManager, logger).getLocales());
            viewDataTemplate.put("user", user);
            viewDataTemplate.put("CocoonFormsInstance", form);

            init = true;

            appleResponse.redirectTo(path);
        } else {
            String methodName = appleRequest.getCocoonRequest().getMethod();
            if (methodName.equals("GET")) {
                // display the form
                appleResponse.sendPage("Form-usersettings-Pipe", getViewData(frontEndContext));
            } else if (methodName.equals("POST")) {
                // handle a form submit
                boolean endProcessing = form.process(new FormContext(appleRequest.getCocoonRequest(), locale));

                if (!endProcessing) {
                    appleResponse.sendPage("Form-usersettings-Pipe", getViewData(frontEndContext));
                } else {
                    subscriptionBinding.saveFormToModel(form, subscription);
                    subscriptionAdditionalStore();
                    subscription.save();

                    userBinding.saveFormToModel(form, user);
                    user.save();

                    Field newPassword = (Field)form.getChild("newPassword");
                    if (newPassword.getValue() != null) {
                        WikiHelper.login(user.getLogin(), (String)newPassword.getValue(), appleRequest.getCocoonRequest(), serviceManager);
                    }

                    // if the user would go back, we would need to re-initialize since the repository object might have changed
                    init = false;
                    repository = null;
                    viewDataTemplate = null;
                    subscription = null;
                    user = null;

                    ResponseUtil.safeRedirect(appleRequest, appleResponse, returnTo);
                }
            } else {
                throw new Exception("Unspported HTTP method: " + methodName);
            }
        }
    }

    private Map<String, Object> getViewData(FrontEndContext frontEndContext) {
        Map<String, Object> viewData = new HashMap<String, Object>(viewDataTemplate);
        viewData.put("pageContext", frontEndContext.getPageContext());
        return viewData;
    }

    class CurrentPasswordRequiredValidator implements WidgetValidator {
        public boolean validate(Widget widget) {
            if (form.getChild("newPassword").getValue() != null && form.getChild("currentPassword").getValue() == null) {
                Field currentPasswordField = (Field)form.getChild("currentPassword");
                if (currentPasswordField.getValidationError() == null) {
                    currentPasswordField.setValidationError(new ValidationError(new I18nMessage("general.field-required", FormsConstants.I18N_CATALOGUE)));
                    return false;
                }
            }
            return true;
        }
    }

    class CurrentPasswordValidator implements WidgetValidator {
        public boolean validate(Widget widget) {
            boolean success = true;

            Field currentPasswordField = (Field)widget;
            String currentPassword = (String)currentPasswordField.getValue();
            if (currentPassword != null) {
                MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
                try {
                    HttpClient httpClient = new HttpClient(connectionManager);
                    httpClient.getParams().setAuthenticationPreemptive(true);
                    httpClient.getParams().setCredentialCharset("ISO-8859-1");
                    String login = user.getLogin().replaceAll("@", "@@");
                    UsernamePasswordCredentials credentials =
                            new UsernamePasswordCredentials(login, currentPassword);
                    httpClient.getState().setCredentials(AuthScope.ANY, credentials);

                    String errorMessage = null;

                    String basePath = ((RemoteRepositoryImpl)repository).getBaseURL();
                    GetMethod getMethod = new GetMethod(basePath + "/repository/userinfo");
                    try {
                        httpClient.executeMethod(getMethod);
                    } catch (IOException e) {
                        errorMessage = "Error checking password: " + e.getMessage();
                    } finally {
                        getMethod.releaseConnection();
                    }

                    if (getMethod.getStatusCode() == 401) {
                        errorMessage = "Incorrect password.";
                    } else if (getMethod.getStatusCode() != 200) {
                        errorMessage = "Error checking password, unexpected response code: " + getMethod.getStatusCode();
                    }

                    if (errorMessage != null) {
                        currentPasswordField.setValidationError(new ValidationError(errorMessage, false));
                        success = false;
                    }
                } finally {
                    connectionManager.shutdown();
                }
            }

            return success;
        }
    }

    private void subscriptionAdditionalLoad() {
        VariantManager variantManager = repository.getVariantManager();
        {
            VariantKey[] variantKeys = subscription.getSubscribedVariantKeys();
            StringBuilder variantKeyBuffer = new StringBuilder(variantKeys.length * 25);
            for (VariantKey variantKey : variantKeys) {
                String branch = getBranchString(variantKey.getBranchId(), variantManager);
                String language = getLanguageString(variantKey.getLanguageId(), variantManager);
                String document;
                if (variantKey.getDocumentId() == null)
                    document = "*";
                else
                    document = String.valueOf(variantKey.getDocumentId());
                variantKeyBuffer.append(document).append(",").append(branch).append(",").append(language).append("\n");
            }
            form.getChild("subscribedDocuments").setValue(variantKeyBuffer.toString());
        }

        {
            CollectionManager collectionManager = repository.getCollectionManager();
            CollectionSubscriptionKey[] collectionKeys = subscription.getSubscribedCollectionKeys();
            StringBuilder collectionKeyBuffer = new StringBuilder(collectionKeys.length * 40);
            for (CollectionSubscriptionKey collectionKey : collectionKeys) {
                String branch = getBranchString(collectionKey.getBranchId(), variantManager);
                String language = getLanguageString(collectionKey.getLanguageId(), variantManager);
                String collection;
                if (collectionKey.getCollectionId() == -1) {
                    collection = "*";
                } else {
                    try {
                        collection = collectionManager.getCollection(collectionKey.getCollectionId(), false).getName();
                    } catch (RepositoryException e) {
                        collection = String.valueOf(collectionKey.getCollectionId());
                    }
                }
                collectionKeyBuffer.append(collection).append(",").append(branch).append(",").append(language).append("\n");
            }
            form.getChild("subscribedCollections").setValue(collectionKeyBuffer.toString());
        }
    }

    private String getBranchString(long branchId, VariantManager variantManager) {
        String branch;
        if (branchId == -1) {
            branch = "*";
        } else {
            try {
                branch = variantManager.getBranch(branchId, false).getName();
            } catch (RepositoryException e) {
                branch = String.valueOf(branchId);
            }
        }
        return branch;
    }

    private String getLanguageString(long languageId, VariantManager variantManager) {
        String language;
        if (languageId == -1) {
            language = "*";
        } else {
            try {
                language = variantManager.getLanguage(languageId, false).getName();
            } catch (RepositoryException e) {
                language = String.valueOf(languageId);
            }
        }
        return language;
    }

    private void subscriptionAdditionalStore() throws Exception {
        String input = (String)form.getChild("subscribedDocuments").getValue();
        VariantKey[] variantKeys = KeySetParser.parseVariantKeys(input, repository.getVariantManager(), true, false);
        subscription.setSubscribedVariantKeys(variantKeys);

        input = (String)form.getChild("subscribedCollections").getValue();
        CollectionSubscriptionKey[] collectionKeys = KeySetParser.parseCollectionKeys(input, repository);
        subscription.setSubscribedCollectionKeys(collectionKeys);
    }

}
