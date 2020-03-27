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
package org.outerj.daisy.publisher.serverimpl.requestmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.QNameSet;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlLineNumber;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.navigation.NavigationParams;
import org.outerj.daisy.publisher.PublisherException;
import org.outerj.daisy.publisher.serverimpl.docpreparation.LinkAnnotationConfig;
import org.outerj.daisy.publisher.serverimpl.requestmodel.PerformFacetedQueryRequest.FacetDefinition;
import org.outerj.daisy.publisher.serverimpl.requestmodel.PerformWorkflowQueryRequest.WfQueryConditions;
import org.outerj.daisy.repository.LocaleHelper;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.query.PredicateExpression;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.repository.query.SortOrder;
import org.outerj.daisy.repository.query.ValueExpression;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.workflow.QueryOrderByItem;
import org.outerj.daisy.workflow.QuerySelectItem;
import org.outerj.daisy.workflow.QueryValueSource;
import org.outerj.daisy.workflow.WfActorKey;
import org.outerj.daisy.workflow.WfUserKey;
import org.outerj.daisy.workflow.WfValueType;
import org.outerx.daisy.x10Publisher.AclInfoDocument;
import org.outerx.daisy.x10Publisher.AnnotatedDocumentDocument1;
import org.outerx.daisy.x10Publisher.AnnotatedVersionListDocument;
import org.outerx.daisy.x10Publisher.AvailableVariantsDocument;
import org.outerx.daisy.x10Publisher.ChooseDocument;
import org.outerx.daisy.x10Publisher.CommentsDocument;
import org.outerx.daisy.x10Publisher.DiffDocument;
import org.outerx.daisy.x10Publisher.DocumentDocument;
import org.outerx.daisy.x10Publisher.DocumentTypeDocument;
import org.outerx.daisy.x10Publisher.ForEachDocument;
import org.outerx.daisy.x10Publisher.GroupDocument;
import org.outerx.daisy.x10Publisher.IdsDocument;
import org.outerx.daisy.x10Publisher.IfDocument;
import org.outerx.daisy.x10Publisher.MyCommentsDocument;
import org.outerx.daisy.x10Publisher.NavigationTreeDocument;
import org.outerx.daisy.x10Publisher.PerformFacetedQueryDocument;
import org.outerx.daisy.x10Publisher.PerformQueryDocument;
import org.outerx.daisy.x10Publisher.PerformWorkflowQueryDocument;
import org.outerx.daisy.x10Publisher.PrepareDocumentDocument1;
import org.outerx.daisy.x10Publisher.PreparedDocumentsDocument;
import org.outerx.daisy.x10Publisher.PublisherRequestDocument;
import org.outerx.daisy.x10Publisher.ResolveDocumentIdsDocument;
import org.outerx.daisy.x10Publisher.ResolveVariablesDocument;
import org.outerx.daisy.x10Publisher.SelectionListDocument;
import org.outerx.daisy.x10Publisher.ShallowAnnotatedVersionDocument;
import org.outerx.daisy.x10Publisher.SubscriptionInfoDocument;
import org.outerx.daisy.x10Publisher.VariablesConfigType;
import org.outerx.daisy.x10Publisher.VariablesListDocument;
import org.outerx.daisy.x10Publisher.VariantKeyType;
import org.outerx.daisy.x10Publisher.WfCondition;
import org.outerx.daisy.x10Publisher.WfVariableValuesType;
import org.outerx.daisy.x10Publisher.PerformFacetedQueryDocument.PerformFacetedQuery.Facets.Facet;
import org.outerx.daisy.x10Publisher.PerformFacetedQueryDocument.PerformFacetedQuery.Facets.Facet.Properties.Property;
import org.outerx.daisy.x10Publisher.PerformFacetedQueryDocument.PerformFacetedQuery.Options.QueryOptions.QueryOption;
import org.outerx.daisy.x10Publisher.PerformWorkflowQueryDocument.PerformWorkflowQuery.Query;
import org.outerx.daisy.x10Publisher.PerformWorkflowQueryDocument.PerformWorkflowQuery.Query.Conditions;
import org.outerx.daisy.x10Publisher.PerformWorkflowQueryDocument.PerformWorkflowQuery.Query.Conditions.SpecialCondition;
import org.outerx.daisy.x10Publisher.PerformWorkflowQueryDocument.PerformWorkflowQuery.Query.OrderByClause.OrderBy;
import org.outerx.daisy.x10Publisher.PerformWorkflowQueryDocument.PerformWorkflowQuery.Query.SelectClause.Select;
import org.outerx.daisy.x10Publisher.WfVariableValuesType.Actor;
import org.outerx.daisy.x10Publisher.WfVariableValuesType.DaisyLink;

public class PublisherRequestBuilder {
    public static PublisherRequest build(PublisherRequestDocument.PublisherRequest publisherRequestXml,
            Repository repository) throws Exception {

        Locale locale = publisherRequestXml.isSetLocale() ? LocaleHelper.parseLocale(publisherRequestXml.getLocale()) : null;
        String styleHint = publisherRequestXml.getStyleHint();
        boolean inlineExceptions = publisherRequestXml.isSetExceptions() && "inline".equals(publisherRequestXml.getExceptions().toString());

        VersionMode versionMode = null;
        if (publisherRequestXml.isSetVersionMode())
            versionMode = VersionMode.get(publisherRequestXml.getVersionMode());

        VariablesConfig variablesConfig = buildVariablesConfig(publisherRequestXml, repository);
        LocationInfo locationInfo = PublisherRequestBuilder.getLocationInfo(publisherRequestXml);
        PublisherRequest publisherRequest = new PublisherRequest(locale, styleHint, inlineExceptions, versionMode, variablesConfig, locationInfo);
        buildChildren(publisherRequestXml, publisherRequest, repository);

        return publisherRequest;
    }

    private static VariablesConfig buildVariablesConfig(PublisherRequestDocument.PublisherRequest publisherRequestXml, Repository repository) throws RepositoryException {
        VariablesConfigType variablesConfigXml = publisherRequestXml.getVariablesConfig();
        if (variablesConfigXml == null)
            return null;

        VariantKey[] variableDocs = buildVariableDocs(repository, variablesConfigXml);

        Map<String, Set<String>> variablesElementAttr = Collections.emptyMap();
        if (variablesConfigXml.isSetVariablesInAttributes()) {
            VariablesConfigType.VariablesInAttributes variablesInAttributes = variablesConfigXml.getVariablesInAttributes();
            if (variablesInAttributes.getAllAttributes()) {
                variablesElementAttr = null; // null means: search all attributes (thus: important not to make it null by default)
            } else {
                variablesElementAttr = new HashMap<String, Set<String>>();
                for (VariablesConfigType.VariablesInAttributes.Element element : variablesInAttributes.getElementList()) {
                    Set<String> attributesSet = new HashSet<String>();
                    String attributes = element.getAttributes();
                    for (String attribute : attributes.split(",")) {
                        attributesSet.add(attribute);
                    }

                    variablesElementAttr.put(element.getName(), attributesSet);
                }
            }
        }

        return new VariablesConfig(variableDocs, variablesElementAttr);

    }

    private static VariantKey[] buildVariableDocs(Repository repository, VariablesConfigType variablesConfigXml) throws RepositoryException {
        if (!variablesConfigXml.isSetVariableSources())
            return null;

        VariantManager variantManager = repository.getVariantManager();
        List<VariantKeyType> xmlKeys = variablesConfigXml.getVariableSources().getVariableDocumentList();

        // optimize for the case the user would have put an empty variables tag in the publisher request,
        // returning null will avoid variable search for queries & navtrees
        if (xmlKeys.size() == 0)
            return null;

        VariantKey[] variableDocs = new VariantKey[xmlKeys.size()];
        for (int i = 0; i < xmlKeys.size(); i++) {
            variableDocs[i] = buildVariantKey(xmlKeys.get(i), repository, variantManager);
        }
        return variableDocs;
    }

    private static void buildChildren(XmlObject container, ParentPublisherRequest request, Repository repository) throws Exception {
        XmlObject[] requestsXml = container.selectChildren(QNameSet.ALL);
        for (XmlObject requestXml : requestsXml) {
            RequestBuilder requestBuilder = REQUEST_BUILDERS.get(requestXml.schemaType());
            if (requestBuilder == null) {
                // special treatment to skip the VariablesConfig
                if (requestXml instanceof VariablesConfigType) {
                    continue;
                }
                XmlOptions xmlOptions = new XmlOptions();
                xmlOptions.setSaveOuter();
                throw new Exception("Unknown type of request: " + requestXml.xmlText(xmlOptions));
            } else {
                request.addRequest(requestBuilder.build(requestXml, repository));
            }
        }
    }

    interface RequestBuilder {
        Request build(XmlObject xmlObject, Repository repository) throws Exception;
    }

    private static RequestBuilder ACL_INFO_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) {
            return new AclInfoRequest(PublisherRequestBuilder.getLocationInfo(xmlObject));
        }
    };

    private static RequestBuilder DOCUMENT_TYPE_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) {
            return new DocumentTypeRequest(PublisherRequestBuilder.getLocationInfo(xmlObject));
        }
    };

    private static RequestBuilder SUBSCRIPTION_INFO_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) {
            return new SubscriptionInfoRequest(PublisherRequestBuilder.getLocationInfo(xmlObject));
        }
    };

    private static RequestBuilder COMMENTS_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) {
            return new CommentsRequest(PublisherRequestBuilder.getLocationInfo(xmlObject));
        }
    };

    private static RequestBuilder AVAILABLE_VARIANTS_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) {
            return new AvailableVariantsRequest(PublisherRequestBuilder.getLocationInfo(xmlObject));
        }
    };

    private static RequestBuilder SHALLOW_ANN_VERSION_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) {
            return new ShallowAnnotatedVersionRequest(PublisherRequestBuilder.getLocationInfo(xmlObject));
        }
    };

    private static RequestBuilder ANNOTATED_DOC_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) {
            AnnotatedDocumentDocument1.AnnotatedDocument annotatedDocXml = (AnnotatedDocumentDocument1.AnnotatedDocument)xmlObject;
            AnnotatedDocumentRequest.PartContentInclusionType includeType = AnnotatedDocumentRequest.PartContentInclusionType.NONE;
            if (annotatedDocXml.isSetInlineParts()) {
                if (annotatedDocXml.getInlineParts().equals("#daisyHtml")) {
                    includeType = AnnotatedDocumentRequest.PartContentInclusionType.DAISY_HTML;
                } else if (annotatedDocXml.getInlineParts().equals("#all")) {
                    includeType = AnnotatedDocumentRequest.PartContentInclusionType.ALL;
                }
            }
            return new AnnotatedDocumentRequest(includeType, PublisherRequestBuilder.getLocationInfo(xmlObject));
        }
    };

    private static RequestBuilder ANNOTATED_VERSIONLIST_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) {
            return new AnnotatedVersionListRequest(PublisherRequestBuilder.getLocationInfo(xmlObject));
        }
    };

    private static RequestBuilder PREPDOC_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) {
            PrepareDocumentDocument1.PrepareDocument preparedDoc = (PrepareDocumentDocument1.PrepareDocument)xmlObject;
            Set<String> inlineParts;
            
            if (preparedDoc.isSetInlineParts()) {
                inlineParts = new HashSet<String>();
                String inlinePartsParam = preparedDoc.getInlineParts();
                StringTokenizer tokenizer = new StringTokenizer(inlinePartsParam, ",");
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    token = token.trim();
                    if (token.length() > 0) {
                        inlineParts.add(token);
                    }
                }
                inlineParts = Collections.unmodifiableSet(inlineParts);
            } else {
                inlineParts = Collections.emptySet();
            }

            Map<QName, LinkAnnotationConfig> linkAnnotationConfig = new HashMap<QName, LinkAnnotationConfig>();

            if (preparedDoc.isSetLinkAnnotation()) {
                List<PrepareDocumentDocument1.PrepareDocument.LinkAnnotation.Element> linkAnnElements = preparedDoc.getLinkAnnotation().getElementList();
                for (PrepareDocumentDocument1.PrepareDocument.LinkAnnotation.Element element : linkAnnElements) {
                    boolean navigationPath = element.isSetNavigationPath() && element.getNavigationPath();
                    boolean imageInfo = element.isSetImageAnnotations() && element.getImageAnnotations();

                    Map<String, ValueExpression> customAnnotationMap = new HashMap<String, ValueExpression>();
                    List<PrepareDocumentDocument1.PrepareDocument.LinkAnnotation.Element.CustomAnnotation> customAnnotations = element.getCustomAnnotationList();
                    for (PrepareDocumentDocument1.PrepareDocument.LinkAnnotation.Element.CustomAnnotation customAnn: customAnnotations) {
                        ValueExpression expression;
                        try {
                            expression = repository.getQueryManager().parseValueExpression(customAnn.getExpression());
                        } catch (QueryException e) {
                            throw new RuntimeException("Failed to build publisher request expression: could not parse custom annotation expression: " + customAnn.getExpression(), e);
                        }
                        customAnnotationMap.put(customAnn.getName(), expression);   
                    }
                    
                    LinkAnnotationConfig linkAnnConfig = new LinkAnnotationConfig(element.getAttribute(), navigationPath, imageInfo, customAnnotationMap);
                    linkAnnotationConfig.put(element.getName(), linkAnnConfig);
                    
                }
            }

            return new PrepareDocumentRequest(inlineParts, linkAnnotationConfig, PublisherRequestBuilder.getLocationInfo(xmlObject));
        }
    };

    private static RequestBuilder DIFF_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) throws Exception {
            DiffDocument.Diff diff = (DiffDocument.Diff)xmlObject;

            ContentDiffType diffType = diff.isSetContentDiffType() ? ContentDiffType.fromString(diff.getContentDiffType()) : ContentDiffType.TEXT;
            LocationInfo locationInfo = PublisherRequestBuilder.getLocationInfo(xmlObject);

            if (diff.isSetOtherDocument()) {
                DiffDocument.Diff.OtherDocument otherDocXml = diff.getOtherDocument();

                PubReqExpr documentIdExpr = PubReqExpr.compile(otherDocXml.isSetId() ? repository.normalizeDocumentId(otherDocXml.getId().trim()) : null, repository);
                PubReqExpr branchExpr = PubReqExpr.compile(otherDocXml.getBranch(), repository);
                PubReqExpr languageExpr = PubReqExpr.compile(otherDocXml.getLanguage(), repository);
                PubReqExpr versionExpr = PubReqExpr.compile(otherDocXml.getVersion(), repository);
                return new DiffRequest(documentIdExpr, branchExpr, languageExpr, versionExpr, diffType, locationInfo);
            } else {
                return new DiffRequest(null, null, null, null, diffType, locationInfo);
            }
        }
    };

    private static RequestBuilder PREPDOCS_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) throws Exception {
            PreparedDocumentsDocument.PreparedDocuments preparedDocuments = (PreparedDocumentsDocument.PreparedDocuments)xmlObject;
            VariantManager variantManager = repository.getVariantManager();
            VariantKey navigationDoc = null;

            if (preparedDocuments.isSetNavigationDocument()) {
                VariantKeyType navigationDocXml = preparedDocuments.getNavigationDocument();
                navigationDoc = buildVariantKey(navigationDocXml, repository, variantManager);
            }

            String displayContext = preparedDocuments.getDisplayContext();
            boolean applyDocumentTypeStyling = preparedDocuments.getApplyDocumentTypeStyling();
            String pubReqSet = "default";
            if (preparedDocuments.isSetPublisherRequestSet() && preparedDocuments.getPublisherRequestSet().trim().length() != 0) {
                pubReqSet = preparedDocuments.getPublisherRequestSet();
            }
            boolean doDiff = preparedDocuments.getDoDiff();
            String diffList = null;
            if (preparedDocuments.isSetDiffList() && preparedDocuments.getDiffList().trim().length() != 0) {
                diffList = preparedDocuments.getDiffList();
            }
            LocationInfo locationInfo = PublisherRequestBuilder.getLocationInfo(xmlObject);
            return new PreparedDocumentsRequest(navigationDoc, pubReqSet, applyDocumentTypeStyling, doDiff, diffList, displayContext, locationInfo);
        }
    };

    private static VariantKey buildVariantKey(VariantKeyType xmlVariantKey, Repository repository, VariantManager variantManager) throws RepositoryException {
        long branchId = variantManager.getBranch(xmlVariantKey.getBranch(), false).getId();
        long languageId = variantManager.getLanguage(xmlVariantKey.getLanguage(), false).getId();
        return new VariantKey(repository.normalizeDocumentId(xmlVariantKey.getId().trim()), branchId, languageId);
    }

    private static RequestBuilder NAVTREE_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) throws Exception {
            NavigationTreeDocument.NavigationTree navigationRequestXml = (NavigationTreeDocument.NavigationTree)xmlObject;

            VariantKeyType navigationDocXml = navigationRequestXml.getNavigationDocument();
            PubReqExpr navDocIdExpr = PubReqExpr.compile(navigationDocXml.getId().trim(), repository);
            PubReqExpr navDocBranchExpr = PubReqExpr.compile(navigationDocXml.getBranch().trim(), repository);
            PubReqExpr navDocLangExpr = PubReqExpr.compile(navigationDocXml.getLanguage().trim(), repository);

            PubReqExpr activeDocIdExpr = null;
            PubReqExpr activeDocBranchExpr = null;
            PubReqExpr activeDocLangExpr = null;
            if (navigationRequestXml.isSetActiveDocument()) {
                VariantKeyType activeDocXml = navigationRequestXml.getActiveDocument();
                activeDocIdExpr = PubReqExpr.compile(activeDocXml.getId().trim(), repository);
                activeDocBranchExpr = PubReqExpr.compile(activeDocXml.getBranch().trim(), repository);
                activeDocLangExpr = PubReqExpr.compile(activeDocXml.getLanguage().trim(), repository);
            }

            PubReqExpr activePathExpr = PubReqExpr.compile(navigationRequestXml.getActivePath(), repository);

            boolean contextualized = navigationRequestXml.getContextualized();
            VersionMode versionMode = navigationRequestXml.isSetVersionMode() ?
                    VersionMode.get(navigationRequestXml.getVersionMode()) : null;

            int depth;
            if (navigationRequestXml.isSetDepth()) {
                depth = navigationRequestXml.getDepth();
            } else {
                if (contextualized)
                    depth = NavigationParams.DEFAULT_CONTEXTUALIZED_DEPTH;
                else
                    depth = NavigationParams.DEFAULT_NONCONTEXTUALIZED_DEPTH;
            }

            PubReqExpr addChildCountsExpr = PubReqExpr.compile(navigationRequestXml.getAddChildCounts(), repository);

            DocumentRequest documentRequest = null;
            if (navigationRequestXml.isSetDocument()) {
                documentRequest = (DocumentRequest)DOCUMENT_BUILDER.build(navigationRequestXml.getDocument(), repository);
            }

            LocationInfo locationInfo = PublisherRequestBuilder.getLocationInfo(xmlObject);
            return new NavigationTreeRequest(navDocIdExpr, navDocBranchExpr, navDocLangExpr,
                    activeDocIdExpr, activeDocBranchExpr, activeDocLangExpr, activePathExpr,
                    contextualized, depth, versionMode, addChildCountsExpr, documentRequest, locationInfo);
        }
    };

    private static RequestBuilder MYCOMMENTS_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) throws Exception {
            return new MyCommentsRequest(PublisherRequestBuilder.getLocationInfo(xmlObject));
        }
    };

    private static RequestBuilder PERFORMQUERY_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) throws Exception {
            PerformQueryDocument.PerformQuery performQueryXml = (PerformQueryDocument.PerformQuery)xmlObject;
            String query = performQueryXml.getQuery();
            String extraConditions = performQueryXml.isSetExtraConditions() ? performQueryXml.getExtraConditions() : null;

            DocumentRequest documentRequest = null;
            if (performQueryXml.isSetDocument()) {
                documentRequest = (DocumentRequest)DOCUMENT_BUILDER.build(performQueryXml.getDocument(), repository);
            }

            LocationInfo locationInfo = PublisherRequestBuilder.getLocationInfo(xmlObject);
            return new PerformQueryRequest(query, extraConditions, documentRequest, locationInfo);
        }
    };

    private static RequestBuilder GROUP_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) throws Exception {
            GroupDocument.Group groupXml = (GroupDocument.Group)xmlObject;
            PubReqExpr idExpr = PubReqExpr.compile(groupXml.getId(), repository);
            boolean catchErrors = groupXml.isSetCatchErrors() ? groupXml.getCatchErrors() : false;
            LocationInfo locationInfo = PublisherRequestBuilder.getLocationInfo(xmlObject);
            GroupRequest groupRequest = new GroupRequest(idExpr, catchErrors, locationInfo);
            buildChildren(groupXml, groupRequest, repository);
            return groupRequest;
        }
    };

    private static RequestBuilder FOREACH_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) throws Exception {
            ForEachDocument.ForEach forEachXml = (ForEachDocument.ForEach)xmlObject;
            DocumentRequest documentRequest = (DocumentRequest)DOCUMENT_BUILDER.build(forEachXml.getDocument(), repository);
            LocationInfo locationInfo = PublisherRequestBuilder.getLocationInfo(xmlObject);
            if (forEachXml.isSetQuery()) {
                String query = forEachXml.getQuery();
                boolean useLastVersion = forEachXml.isSetUseLastVersion() && forEachXml.getUseLastVersion();
                return new QueryForEachRequest(query, useLastVersion, documentRequest, locationInfo);
            } else if (forEachXml.isSetExpression()) {
                ForEachDocument.ForEach.Expression exprXml = forEachXml.getExpression();
                String expression = exprXml.getStringValue();
                boolean precompile = exprXml.isSetPrecompile() ? exprXml.getPrecompile() : true;
                int hierarchyElement = 0;
                if (exprXml.isSetHierarchyElement() && !exprXml.getHierarchyElement().equals("all")) {
                    hierarchyElement = Integer.parseInt(exprXml.getHierarchyElement());
                }
                if (precompile) {
                    ValueExpression compiledExpression;
                    try {
                        compiledExpression = repository.getQueryManager().parseValueExpression(expression);
                    } catch (QueryException e) {
                        throw new PublisherException("Error compiling forEach expression: " + expression, e);
                    }
                    if (compiledExpression.getValueType() != ValueType.LINK) {
                        throw new PublisherException("forEach expression should evaluate to a link value: " + expression);
                    }
                    return new ExpressionForEachRequest(compiledExpression, hierarchyElement, documentRequest, locationInfo);
                } else {
                    return new ExpressionForEachRequest(expression, hierarchyElement, documentRequest, locationInfo);
                }
            } else {
                throw new PublisherException("Unexpected error: forEach doesn't contain a query or expression.");
            }
        }
    };

    private static RequestBuilder RESOLVE_DOCIDS_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) throws Exception {
            ResolveDocumentIdsDocument.ResolveDocumentIds resolveDocIdsXml = (ResolveDocumentIdsDocument.ResolveDocumentIds)xmlObject;
            VariantManager variantManager = repository.getVariantManager();
            long defaultBranchId = resolveDocIdsXml.isSetBranch() ? variantManager.getBranch(resolveDocIdsXml.getBranch(), false).getId() : Branch.MAIN_BRANCH_ID;
            long defaultLanguageId = resolveDocIdsXml.isSetLanguage() ? variantManager.getLanguage(resolveDocIdsXml.getLanguage(), false).getId() : Language.DEFAULT_LANGUAGE_ID;
            LocationInfo locationInfo = PublisherRequestBuilder.getLocationInfo(xmlObject);
            return new ResolveDocumentIdsRequest(resolveDocIdsXml.getDocumentList(), defaultBranchId, defaultLanguageId, locationInfo);
        }
    };

    private static RequestBuilder RESOLVE_VARS_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) throws Exception {
            ResolveVariablesDocument.ResolveVariables resolveVarsXml = (ResolveVariablesDocument.ResolveVariables)xmlObject;
            LocationInfo locationInfo = PublisherRequestBuilder.getLocationInfo(xmlObject);
            return new ResolveVariablesRequest(resolveVarsXml.getTextList(), locationInfo);
        }
    };

    private static RequestBuilder VARS_LIST_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) throws Exception {
            LocationInfo locationInfo = PublisherRequestBuilder.getLocationInfo(xmlObject);
            return new VariablesListRequest(locationInfo);
        }
    };

    private static RequestBuilder DOCUMENT_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) throws Exception {
            LocationInfo location = PublisherRequestBuilder.getLocationInfo(xmlObject);
            DocumentDocument.Document documentRequestXml = (DocumentDocument.Document)xmlObject;
            DocumentRequest documentRequest;
            if (documentRequestXml.isSetId()) {
                String documentId = documentRequestXml.getId().trim();
                String branch = documentRequestXml.getBranch();
                String language = documentRequestXml.getLanguage();
                String version = documentRequestXml.getVersion();
                documentRequest = new DocumentRequest(documentId, branch, language, version, location);
            } else if (documentRequestXml.isSetField() && documentRequestXml.getField().length() > 0) {
                // Note: the field attribute on documentRequest is a somewhat older syntax from before
                //       forEach supported expressions. We translate/delegate this request here to a
                //       forEach request.
                String fieldTypeParam = documentRequestXml.getField();
                FieldType fieldType;
                try {
                    if (Character.isDigit(fieldTypeParam.charAt(0))) {
                        fieldType = repository.getRepositorySchema().getFieldTypeById(Long.parseLong(fieldTypeParam), false);
                    } else {
                        fieldType = repository.getRepositorySchema().getFieldTypeByName(fieldTypeParam, false);
                    }
                } catch (Exception e) {
                    throw new Exception("Problem getting the field type specified in the field attribute on a document publisher request.", e);
                }
                if (fieldType.getValueType() != ValueType.LINK)
                    throw new Exception("The field type specified in the field attribute on a document publisher request does not specify a field of type link.");

                int hierarchyElement = 0;
                if (documentRequestXml.isSetHierarchyElement() && !documentRequestXml.getHierarchyElement().equals("all")) {
                    hierarchyElement = Integer.parseInt(documentRequestXml.getHierarchyElement());
                }

                ValueExpression expression;
                try {
                    expression = repository.getQueryManager().parseValueExpression("$" + fieldType.getName());
                } catch (QueryException e) {
                    throw new PublisherException("Error compiling forEach expression (created from a p:document request).", e);
                }
                documentRequest = new DocumentRequest(location);
                buildChildren(documentRequestXml, documentRequest, repository);
                LocationInfo locationInfo = PublisherRequestBuilder.getLocationInfo(xmlObject);
                return new ExpressionForEachRequest(expression, hierarchyElement, documentRequest, locationInfo);
            } else {
                documentRequest = new DocumentRequest(location);
            }
            buildChildren(documentRequestXml, documentRequest, repository);
            return documentRequest;
        }
    };

    private static RequestBuilder IF_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) throws Exception {
            IfDocument.If ifXml = (IfDocument.If)xmlObject;
            String test = ifXml.getTest();
            PredicateExpression expression = repository.getQueryManager().parsePredicateExpression(test);
            LocationInfo locationInfo = PublisherRequestBuilder.getLocationInfo(xmlObject);
            IfRequest ifRequest = new IfRequest(expression, locationInfo);
            buildChildren(ifXml, ifRequest, repository);
            return ifRequest;
        }
    };

    private static RequestBuilder CHOOSE_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) throws Exception {
            ChooseDocument.Choose chooseXml = (ChooseDocument.Choose)xmlObject;
            ChooseRequest chooseRequest = new ChooseRequest(PublisherRequestBuilder.getLocationInfo(xmlObject));
            for (ChooseDocument.Choose.When whenXml : chooseXml.getWhenList()) {
                PredicateExpression expression = repository.getQueryManager().parsePredicateExpression(whenXml.getTest());
                ChooseWhen chooseWhen = new ChooseWhen(expression, PublisherRequestBuilder.getLocationInfo(whenXml));
                buildChildren(whenXml, chooseWhen, repository);
                chooseRequest.addWhen(chooseWhen);
            }
            if (chooseXml.isSetOtherwise()) {
                ChooseOtherwise chooseOtherwise = new ChooseOtherwise(PublisherRequestBuilder.getLocationInfo(chooseXml));
                buildChildren(chooseXml.getOtherwise(), chooseOtherwise, repository);
                chooseRequest.setOtherwise(chooseOtherwise);
            }
            return chooseRequest;
        }
    };

    private static RequestBuilder SELECTIONLIST_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) throws Exception {
            SelectionListDocument.SelectionList selListXml = (SelectionListDocument.SelectionList)xmlObject;
            PubReqExpr branchExpr = PubReqExpr.compile(selListXml.getBranch(), repository);
            PubReqExpr languageExpr = PubReqExpr.compile(selListXml.getLanguage(), repository);
            LocationInfo locationInfo = PublisherRequestBuilder.getLocationInfo(xmlObject);
            return new SelectionListRequest(selListXml.getFieldType(), branchExpr, languageExpr, locationInfo);
        }
    };

    private static RequestBuilder IDS_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) throws Exception {
            return new IdsRequest(PublisherRequestBuilder.getLocationInfo(xmlObject));
        }
    };
    
    private static RequestBuilder PERFORMFACETEDQUERY_BUILDER = new RequestBuilder() {
        public Request build(XmlObject xmlObject, Repository repository) throws Exception {
            PerformFacetedQueryDocument.PerformFacetedQuery pfqXml = (PerformFacetedQueryDocument.PerformFacetedQuery) xmlObject;
            
            List<String> auxilaryExpressions = Collections.emptyList();
            if (pfqXml.getOptions().isSetAdditionalSelects())
                auxilaryExpressions = pfqXml.getOptions().getAdditionalSelects().getExpressionList();
            
            List<Facet> facetXmls = pfqXml.getFacets().getFacetList();
            FacetDefinition[] facets = new PerformFacetedQueryRequest.FacetDefinition[auxilaryExpressions.size() + facetXmls.size()];
            for (int i = 0; i < auxilaryExpressions.size(); i++) {
                facets[i] = new PerformFacetedQueryRequest.FacetDefinition(auxilaryExpressions.get(i), false, -1, true, false, "default");
            }            
            for (int i = 0; i < facetXmls.size(); i++) {
                Facet facetXml = facetXmls.get(i);
                int maxValues =  facetXml.isSetMaxValues() ? facetXml.getMaxValues() : -1;
                boolean sortOnValue = facetXml.isSetSortOnValue() ? facetXml.getSortOnValue() : true;
                boolean sortAscending = facetXml.isSetSortAscending() ? facetXml.getSortAscending() : true;
                String type = facetXml.isSetType() ? facetXml.getType() : "default";
                facets[auxilaryExpressions.size() + i] = new PerformFacetedQueryRequest.FacetDefinition(facetXml.getExpression(), true, maxValues, sortOnValue, sortAscending, type);
                Map<String,String> props = facets[auxilaryExpressions.size() + i].getProperties();
                if (facetXml.isSetProperties()) {
                    for (Property propXml : facetXml.getProperties().getPropertyList()) {
                        props.put(propXml.getName(), propXml.getValue());
                    }
                }
            }
            
            Map<String,String> queryOptions = new HashMap<String, String>();
            if (pfqXml.getOptions().isSetQueryOptions()) {
                for (QueryOption queryOption : pfqXml.getOptions().getQueryOptions().getQueryOptionList()) {
                    queryOptions.put(queryOption.getName(), queryOption.getValue());
                }
            }
            
            String conditions = pfqXml.getOptions().isSetDefaultConditions() ? pfqXml.getOptions().getDefaultConditions() : "true";
            
            return new PerformFacetedQueryRequest(facets, conditions, pfqXml.getOptions().getDefaultSortOrder(), 
                    queryOptions, PublisherRequestBuilder.getLocationInfo(xmlObject));
        }
    };
    
    private static RequestBuilder PERFORMWORKFLOWQUERY_BUILDER = new RequestBuilder () {

        public Request build(XmlObject xmlObject, Repository repository) throws Exception {
            PerformWorkflowQueryDocument.PerformWorkflowQuery wfqXml = (PerformWorkflowQueryDocument.PerformWorkflowQuery) xmlObject;
            Query queryXml = wfqXml.getQuery();
            PerformWorkflowQueryRequest.QuerySubject subject = PerformWorkflowQueryRequest.QuerySubject.valueOf(wfqXml.getType().toString().toUpperCase());
            
            List<QuerySelectItem> selectItems = new ArrayList<QuerySelectItem>();
            if (queryXml.isSetSelectClause()) {
                List<Select> selectsXml = queryXml.getSelectClause().getSelectList();
                for (Select selectXml : selectsXml) {                
                    selectItems.add(new QuerySelectItem(selectXml.getName(), QueryValueSource.fromString(selectXml.getType()) ));
                }
            }

            WfQueryConditions conditions = new WfQueryConditions();
            if (queryXml.isSetConditions()) {
                Conditions conditionsXml = queryXml.getConditions();
                conditions.setMeetAllCriteria(conditionsXml.getMeetAllCriteria());
                for (WfCondition conditionXml : conditionsXml.getProcessVariableConditionList() ) {
                    WfValueType valueType = WfValueType.fromString(conditionXml.getValueType());
                    List<Object> values = getVariableConditionValues(conditionXml, valueType, repository);
                    conditions.addProcessVariableCondition(conditionXml.getName(), valueType, conditionXml.getOperator(), values);
                }
                for (WfCondition conditionXml : conditionsXml.getPropertyConditionList() ) {
                    WfValueType valueType = WfValueType.fromString(conditionXml.getValueType());
                    List<Object> values = getVariableConditionValues(conditionXml, valueType, repository);
                    conditions.addPropertyCondition(conditionXml.getName(), valueType, conditionXml.getOperator(), values);
                }
                for (WfCondition conditionXml : conditionsXml.getTaskVariableConditionList()) {
                    WfValueType valueType = WfValueType.fromString(conditionXml.getValueType());
                    List<Object> values = getVariableConditionValues(conditionXml, valueType, repository);
                    conditions.addTaskVariableCondition(conditionXml.getName(), valueType, conditionXml.getOperator(), values);
                }
                for (SpecialCondition conditionXml : conditionsXml.getSpecialConditionList()) {
                    List<WfVariableValuesType> valuesXml = conditionXml.getValueList();
                    List<Object> values = new ArrayList<Object>(valuesXml.size());
                    List<WfValueType> types = new ArrayList<WfValueType>(valuesXml.size());
                    
                    for (WfVariableValuesType valueXml : valuesXml) {
                        Object[] data = getValueAndType(valueXml, " ", repository);
                        values.add(data[0]);
                        types.add((WfValueType)data[1]);
                    }
                    
                    conditions.addSpecialCondition(conditionXml.getName(), types, values);
                }        
            }
            
            List<QueryOrderByItem> orderByItems = new ArrayList<QueryOrderByItem>();
            if (queryXml.isSetOrderByClause()) {
                for (OrderBy orderByXml : queryXml.getOrderByClause().getOrderByList()) {
                    orderByItems.add(new QueryOrderByItem(orderByXml.getName(), QueryValueSource.fromString(orderByXml.getType()), SortOrder.fromString(orderByXml.getSortOrder())));
                }
            }
            return new PerformWorkflowQueryRequest(selectItems, conditions, orderByItems, subject, PublisherRequestBuilder.getLocationInfo(xmlObject));
        }
        
        private List<Object> getVariableConditionValues(WfCondition cond, WfValueType type, Repository repository) throws Exception {
            List<Object> values = new ArrayList<Object>(2);
            for (WfVariableValuesType value : cond.getValueList()) {
                Object[] result = getValueAndType(value, "query condition value", repository);
                if (result[1] != type)
                    throw new PublisherException("Specified value does not correspond to type defined in valueType attribute for condition on " + cond.getName());
                values.add(result[0]);
            }
            return values;
        }
        
        private Object[] getValueAndType(WfVariableValuesType xml, String what, Repository repository) throws Exception{
            WfValueType valueType;
            Object value;
            if (xml.isSetString()) {
                valueType = WfValueType.STRING;
                value = xml.getString();
            } else if (xml.isSetLong()) {
                valueType = WfValueType.LONG;
                value = xml.getLong();
            } else if (xml.isSetDate()) {
                valueType = WfValueType.DATE;
                value = xml.getDate().getTime();
            } else if (xml.isSetDateTime()) {
                valueType = WfValueType.DATETIME;
                value = xml.getDateTime().getTime();
            } else if (xml.isSetDaisyLink()) {
                valueType = WfValueType.DAISY_LINK;
                DaisyLink link = xml.getDaisyLink();
                value = new PerformWorkflowQueryRequest.DaisyLinkExpression (
                        PubReqExpr.compile(link.getDocumentId(), repository),
                        link.isSetBranch() ? PubReqExpr.compile(link.getBranch(), repository) : null,
                        link.isSetLanguage() ? PubReqExpr.compile(link.getLanguage(), repository) : null,
                        link.isSetVersion() ? PubReqExpr.compile(link.getVersion(), repository) : null);
                
            } else if (xml.isSetActor()) {
                valueType = WfValueType.ACTOR;
                Actor actor = xml.getActor();
                if (actor.getPool()) {
                    if (actor.isSetId2()) {
                        List<Long> poolIds = new ArrayList<Long>(1);
                        poolIds.add(actor.getId2());
                        value = new WfActorKey(poolIds);
                    } else {
                        List<Long> poolIds = actor.getIdList();
                        List<Long> poolIdsList = new ArrayList<Long>(poolIds.size());
                        for (long poolId : poolIds) {
                            poolIdsList.add(poolId);
                        }
                        value = new WfActorKey(poolIdsList);
                    }
                } else {
                    value = new WfActorKey(actor.getId2());
                }
            } else if (xml.isSetBoolean()) {
                valueType = WfValueType.BOOLEAN;
                value = xml.getBoolean() ? Boolean.TRUE : Boolean.FALSE;
            } else if (xml.isSetUser()) {
                valueType = WfValueType.USER;
                value = new WfUserKey(xml.getUser());
            } else if (xml.isSetId()) {
                valueType = WfValueType.ID;
                value = xml.getId();
            } else {
                throw new RuntimeException("Missing tag for the value in XML representation of " + what);
            }
            
            Object[] data = new Object[2];
            data[0] = value;
            data[1] = valueType;
            
            return data;
        }
        
    };

    private static Map<org.apache.xmlbeans.SchemaType, RequestBuilder> REQUEST_BUILDERS = new HashMap<org.apache.xmlbeans.SchemaType, RequestBuilder>();
    static {
        REQUEST_BUILDERS.put(AclInfoDocument.AclInfo.type, ACL_INFO_BUILDER);
        REQUEST_BUILDERS.put(DocumentTypeDocument.DocumentType.type, DOCUMENT_TYPE_BUILDER);
        REQUEST_BUILDERS.put(SubscriptionInfoDocument.SubscriptionInfo.type, SUBSCRIPTION_INFO_BUILDER);
        REQUEST_BUILDERS.put(CommentsDocument.Comments.type, COMMENTS_BUILDER);
        REQUEST_BUILDERS.put(AvailableVariantsDocument.AvailableVariants.type, AVAILABLE_VARIANTS_BUILDER);
        REQUEST_BUILDERS.put(ShallowAnnotatedVersionDocument.ShallowAnnotatedVersion.type, SHALLOW_ANN_VERSION_BUILDER);
        REQUEST_BUILDERS.put(AnnotatedDocumentDocument1.AnnotatedDocument.type, ANNOTATED_DOC_BUILDER);
        REQUEST_BUILDERS.put(AnnotatedVersionListDocument.AnnotatedVersionList.type, ANNOTATED_VERSIONLIST_BUILDER);
        REQUEST_BUILDERS.put(DiffDocument.Diff.type, DIFF_BUILDER);
        REQUEST_BUILDERS.put(PreparedDocumentsDocument.PreparedDocuments.type, PREPDOCS_BUILDER);
        REQUEST_BUILDERS.put(DocumentDocument.Document.type, DOCUMENT_BUILDER);
        REQUEST_BUILDERS.put(NavigationTreeDocument.NavigationTree.type, NAVTREE_BUILDER);
        REQUEST_BUILDERS.put(MyCommentsDocument.MyComments.type, MYCOMMENTS_BUILDER);
        REQUEST_BUILDERS.put(PerformQueryDocument.PerformQuery.type, PERFORMQUERY_BUILDER);
        REQUEST_BUILDERS.put(PerformWorkflowQueryDocument.PerformWorkflowQuery.type, PERFORMWORKFLOWQUERY_BUILDER);
        REQUEST_BUILDERS.put(GroupDocument.Group.type, GROUP_BUILDER);
        REQUEST_BUILDERS.put(ForEachDocument.ForEach.type, FOREACH_BUILDER);
        REQUEST_BUILDERS.put(ResolveDocumentIdsDocument.ResolveDocumentIds.type, RESOLVE_DOCIDS_BUILDER);
        REQUEST_BUILDERS.put(IfDocument.If.type,  IF_BUILDER);
        REQUEST_BUILDERS.put(ChooseDocument.Choose.type, CHOOSE_BUILDER);
        REQUEST_BUILDERS.put(PrepareDocumentDocument1.PrepareDocument.type, PREPDOC_BUILDER);
        REQUEST_BUILDERS.put(SelectionListDocument.SelectionList.type, SELECTIONLIST_BUILDER);
        REQUEST_BUILDERS.put(IdsDocument.Ids.type, IDS_BUILDER);
        REQUEST_BUILDERS.put(PerformFacetedQueryDocument.PerformFacetedQuery.type, PERFORMFACETEDQUERY_BUILDER);
        REQUEST_BUILDERS.put(ResolveVariablesDocument.ResolveVariables.type, RESOLVE_VARS_BUILDER);
        REQUEST_BUILDERS.put(VariablesListDocument.VariablesList.type, VARS_LIST_BUILDER);
    }

    private static LocationInfo getLocationInfo(XmlObject xmlObject) {
        XmlCursor cursor = xmlObject.newCursor();
        XmlLineNumber lineNumber = (XmlLineNumber)cursor.getBookmark(XmlLineNumber.class);
        String source = cursor.documentProperties().getSourceName();
        QName name = cursor.getName();
        cursor.dispose();

        int line = -1;
        int column = -1;
        if (lineNumber != null) {
            line = lineNumber.getLine();
            column = lineNumber.getColumn();
        }


        String prefix = name.getPrefix();
        String localName = name.getLocalPart();
        String formattedName = prefix != null ? prefix + ":" + localName : localName;

        return new LocationInfo(source, line, column, formattedName);
    }
}
