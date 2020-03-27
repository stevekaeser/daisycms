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

import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.Repeater;
import org.apache.cocoon.forms.FormContext;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.frontend.util.EncodingUtil;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.AvailableVariants;
import org.outerj.daisy.repository.query.QueryHelper;
import org.outerx.daisy.x10.SearchResultDocument;

import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * This apple allows the user to create a new document variant for one document.
 */
public class CreateDocumentVariantApple extends AbstractDocumentApple {
    private boolean init = false;
    private Form form;
    private Map<String, Object> viewDataTemplate;
    private Locale locale;

    protected void processDocumentRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        if (!init) {
            locale = frontEndContext.getLocale();
            form = FormHelper.createForm(getServiceManager(), "resources/form/createdocvariant_definition.xml");
            form.getChild("copyContent").setValue(Boolean.TRUE);
            form.getChild("goToEditor").setValue(Boolean.TRUE);
            form.getChild("startVersion").setValue("last");

            String imagesQuery = "select name where LinksFromVariant(" + QueryHelper.formatString(getDocumentId()) + "," + getBranchId() + "," + getLanguageId() + ",'image') and branchId = " + getBranchId() + " and languageId = " + getLanguageId() + " option point_in_time='last'"; 
            List<SearchResultDocument.SearchResult.Rows.Row> images = getRepository().getQueryManager().performQuery(imagesQuery, locale).getSearchResult().getRows().getRowList();
            Repeater repeater = (Repeater)form.getChild("resources");
            for (SearchResultDocument.SearchResult.Rows.Row image : images) {
                Repeater.RepeaterRow row = repeater.addRow();
                row.getChild("id").setValue(image.getDocumentId());
                row.getChild("name").setValue(image.getValueArray(0));
                row.getChild("createvariant").setValue(Boolean.TRUE);
            }

            VariantManager variantManager = getRepository().getVariantManager();
            viewDataTemplate = new HashMap<String, Object>();
            viewDataTemplate.put("branchesArray", variantManager.getAllBranches(false).getArray());
            viewDataTemplate.put("languagesArray", variantManager.getAllLanguages(false).getArray());
            viewDataTemplate.put("startBranchName", variantManager.getBranch(getBranchId(), false).getName());
            viewDataTemplate.put("startLanguageName", variantManager.getLanguage(getLanguageId(), false).getName());
            viewDataTemplate.put("availableVariants", getRepository().getAvailableVariants(getDocumentId()).getArray());
            viewDataTemplate.put("documentPath", getMountPoint() + "/" + getSiteConf().getName() + "/" + getRequestedNavigationPath());
            viewDataTemplate.put("CocoonFormsInstance", form);
            viewDataTemplate.put("variantParams", getVariantParams());
            viewDataTemplate.put("variantQueryString", getVariantQueryString());
            viewDataTemplate.put("locale", locale);

            init = true;

            appleResponse.redirectTo(EncodingUtil.encodePath(getMountPoint() + "/" + getSiteConf().getName() + "/" + getRequestedNavigationPath() + "/createVariant/" + getContinuationId()));
        } else {
            if (request.getMethod().equals("POST")) {
                FormContext formContext = new FormContext(request, locale);
                boolean finished = form.process(formContext);
                if (finished) {
                    long newBranchId = ((Long)form.getChild("newBranchId").getValue()).longValue();
                    long newLanguageId = ((Long)form.getChild("newLanguageId").getValue()).longValue();
                    createImageVariants(newBranchId, newLanguageId);
                    boolean copyContent = ((Boolean)form.getChild("copyContent").getValue()).booleanValue();
                    if (!copyContent) {
                        String redirectURL = getMountPoint() + "/" + getSiteConf().getName()
                                + "/new/edit?variantOf=" + getDocumentId() + "&startBranchId=" + getBranchId()
                                + "&startLanguageId=" + getLanguageId() + "&newBranchId=" + newBranchId
                                + "&newLanguageId=" + newLanguageId + "&startWithGet=true";
                        appleResponse.redirectTo(EncodingUtil.encodePathQuery(redirectURL));
                    } else {
                        String startVersion = (String)form.getChild("startVersion").getValue();
                        long startVersionId;
                        if (startVersion.equals("last"))
                            startVersionId = -1;
                        else if (startVersion.equals("live"))
                            startVersionId = -2;
                        else
                            startVersionId = Long.parseLong(startVersion);
                        getRepository().createVariant(getDocumentId(), getBranchId(), getLanguageId(), startVersionId, newBranchId, newLanguageId, true);
                        boolean goToEditor = ((Boolean)form.getChild("goToEditor").getValue()).booleanValue();
                        if (goToEditor) {
                            String redirectURL = getMountPoint() + "/" + getSiteConf().getName() + "/" + getDocumentId() + "/edit"
                                    + "?branch=" + newBranchId + "&language=" + newLanguageId + "&startWithGet=true";
                            appleResponse.redirectTo(EncodingUtil.encodePathQuery(redirectURL));
                        } else {
                            setBranchAndLanguage(newBranchId, newLanguageId);
                            String redirectURL = getMountPoint() + "/" + getSiteConf().getName() + "/" + getDocumentId() + ".html" + getVariantQueryString();
                            appleResponse.redirectTo(EncodingUtil.encodePathQuery(redirectURL));
                        }
                    }
                } else {
                    appleResponse.sendPage("Form-createdocvariant-Pipe", getViewData(frontEndContext));
                }
            } else if (request.getMethod().equals("GET")) {
                appleResponse.sendPage("Form-createdocvariant-Pipe", getViewData(frontEndContext));
            } else {
                throw new HttpMethodNotAllowedException(request.getMethod());
            }
        }
    }

    protected boolean needsInitialisation() {
        return !init;
    }

    private void createImageVariants(long newBranchId, long newLanguageId) throws Exception {
        Repeater repeater = (Repeater)form.getChild("resources");
        for (int i = 0; i < repeater.getSize(); i++) {
            Repeater.RepeaterRow row = repeater.getRow(i);
            if (Boolean.TRUE.equals(row.getChild("createvariant").getValue())) {
                String id = (String)row.getChild("id").getValue();
                try {
                    AvailableVariants availableVariants = getRepository().getAvailableVariants(id);
                    if (!availableVariants.hasVariant(newBranchId, newLanguageId))
                        getRepository().createVariant(id, getBranchId(), getLanguageId(), -1, newBranchId, newLanguageId, true);
                } catch (RepositoryException e) {
                    throw new Exception("Error branching embedded image " + id);
                }
            }
        }
    }

    private Map<String, Object> getViewData(FrontEndContext frontEndContext) {
        Map<String, Object> viewData = new HashMap<String, Object>(viewDataTemplate);
        viewData.put("pageContext", frontEndContext.getPageContext());
        return viewData;
    }
}
