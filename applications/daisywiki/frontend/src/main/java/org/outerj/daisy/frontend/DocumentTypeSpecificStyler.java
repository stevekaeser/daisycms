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

import org.apache.cocoon.xml.SaxBuffer;
import org.apache.cocoon.components.flow.util.PipelineUtil;
import org.apache.cocoon.components.LifecycleHelper;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.service.ServiceManager;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.util.Constants;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * This class is concerned with applying document type specific styling (by using
 * Cocoon pipelines) to the documents stored in a {@link PreparedDocuments} object.
 */
public class DocumentTypeSpecificStyler {
    private final String publishType;
    private final String basePath;
    private final String daisyCocoonPath;
    private final StylesheetProvider stylesheetProvider;
    private final PageContext pageContext;
    private final Repository repository;
    private final Context context;
    private final ServiceManager serviceManager;

    /**
     *
     * @param publishType 'html' or 'xslfo' or anything for which a pipeline is defined
     * @param basePath the base path for addressing documents (should end on a slash).
     *                 This is simply passed as a parameter to the stylesheets, who can use it if they need.
     * @param stylesheetProvider
     */
    public DocumentTypeSpecificStyler(String publishType, String basePath, String daisyCocoonPath,
            StylesheetProvider stylesheetProvider, PageContext pageContext,
            Repository repository, Context context, ServiceManager serviceManager) {
        this.publishType = publishType;
        this.basePath = basePath;
        this.daisyCocoonPath = daisyCocoonPath;
        this.stylesheetProvider = stylesheetProvider;
        this.repository = repository;
        this.context = context;
        this.serviceManager = serviceManager;
        this.pageContext = pageContext;
    }

    public static interface StylesheetProvider {
        String getStylesheet(String documentTypeName) throws Exception;
        String getDefaultStylesheet() throws Exception;
        String getStylesheetByHint(String styleHint) throws Exception;
    }

    public void transformPreparedDocuments(PreparedDocuments preparedDocuments, String displayContext) throws Exception {
        int[] ids = preparedDocuments.getPreparedDocumentIds();
        for (int id : ids) {
            PreparedDocuments.PreparedDocument preparedDocument = preparedDocuments.getPreparedDocument(id);
            SaxBuffer buffer = preparedDocument.getSaxBuffer();

            Object styleInfo = searchStyleInfo(buffer);
            String stylesheet;
            if (styleInfo != null) {
                if (styleInfo instanceof Long) {
                    long documentTypeId = ((Long)styleInfo).longValue();
                    String documentTypeName = repository.getRepositorySchema().getDocumentTypeById(documentTypeId, false).getName();
                    stylesheet = stylesheetProvider.getStylesheet(documentTypeName);
                } else {
                    stylesheet = stylesheetProvider.getStylesheetByHint((String)styleInfo);
                }
            } else {
                stylesheet = stylesheetProvider.getDefaultStylesheet();
            }

            // Calculate variantQueryString and variantParams (supplied to stylesheets for convenience)
            VariantKey documentKey = preparedDocument.getDocumentKey();
            SiteConf siteConf = pageContext.getSiteConf();
            String variantParams = "";
            String variantQueryString = "";
            if (documentKey.getBranchId() != siteConf.getBranchId() || documentKey.getLanguageId() != siteConf.getLanguageId()) {
                String branch = repository.getVariantManager().getBranch(documentKey.getBranchId(), false).getName();
                String language = repository.getVariantManager().getLanguage(documentKey.getLanguageId(), false).getName();
                variantParams = "&branch=" + branch + "&language=" + language;
                variantQueryString = "?branch=" + branch + "&language=" + language;
            }

            Map<String, Object> viewData = new HashMap<String, Object>();
            viewData.put("stylesheet", stylesheet);
            viewData.put("pageContext", pageContext);
            viewData.put("repository", repository);
            viewData.put("document", buffer);
            viewData.put("documentBasePath", basePath);
            viewData.put("displayContext", displayContext != null ? displayContext : "");
            viewData.put("isIncluded", String.valueOf(id != 1)); // root document has ID 1 in prep-docs
            viewData.put("documentKey", documentKey);
            viewData.put("documentBranch", repository.getVariantManager().getBranch(documentKey.getBranchId(), false).getName());
            viewData.put("documentLanguage", repository.getVariantManager().getLanguage(documentKey.getLanguageId(), false).getName());
            viewData.put("variantQueryString", variantQueryString);
            viewData.put("variantParams", variantParams);

            SaxBuffer resultBuffer = executePipeline(daisyCocoonPath + "/" + publishType + "-StyleDocumentPipe", viewData);
            preparedDocuments.putPreparedDocument(preparedDocument.getId(), preparedDocument.getDocumentKey(), resultBuffer);
        }
    }

    /**
     * Searches the supplied buffer and returns:
     * <ul>
     *  <li>a String containing the value of the styleHint attribute on the p:publisherResponse element, if found
     *  <li>otherwise a Long containing the typeId attribute of the first d:document element encountered
     *  <li>otherwise null
     * </ul>
     *
     * @param buffer containing the XML of the publisher request for the document.
     */
    public static Object searchStyleInfo(SaxBuffer buffer) {
        List bits = buffer.getBits();
        boolean publisherResponseElementFound = false;
        for (Object bit : bits) {
            if (bit instanceof SaxBuffer.StartElement) {
                SaxBuffer.StartElement startElement = (SaxBuffer.StartElement)bit;
                if (!publisherResponseElementFound
                        && startElement.namespaceURI.equals("http://outerx.org/daisy/1.0#publisher")
                        && startElement.localName.equals("publisherResponse")) {
                    String styleHint = startElement.attrs.getValue("styleHint");
                    if (styleHint != null && styleHint.length() != 0)
                        return styleHint;
                    publisherResponseElementFound = true;
                } else
                if (startElement.namespaceURI.equals(Constants.DAISY_NAMESPACE) && startElement.localName.equals("document"))
                {
                    return new Long(Long.parseLong(startElement.attrs.getValue("typeId")));
                }
            }
        }
        return null;
    }

    /**
     * Executes a Cocoon pipeline and store the result in a SaxBuffer instance.
     */
    private SaxBuffer executePipeline(String pipe, Map viewData) throws Exception {
        PipelineUtil pipelineUtil = new PipelineUtil();
        try {
            LifecycleHelper.setupComponent(pipelineUtil, null, context, serviceManager, null, false);

            SaxBuffer buffer = new SaxBuffer();
            pipelineUtil.processToSAX(pipe, viewData, buffer);

            return buffer;
        } finally {
            LifecycleHelper.dispose(pipelineUtil);
        }
    }
}
