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
package org.outerj.daisy.frontend.editor;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.xml.SaxBuffer;
import org.apache.commons.lang.ObjectUtils;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.outerj.daisy.frontend.FrontEndContext;
import org.outerj.daisy.frontend.WikiPublisherHelper;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.repository.LiveHistoryEntry;
import org.outerj.daisy.repository.Timeline;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * This is the Apple controlling the document editing screen(s).
 */
public class LiveHistoryEditorApple extends DocumentEditorSupport implements Contextualizable, LogEnabled {
    private Logger logger;
    private DateTimeFormatter iso8601 = ISODateTimeFormat.dateOptionalTimeParser();

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        super.processRequest(appleRequest, appleResponse);
        if (document == null) {
            initialiseWithExistingDocument(appleResponse);
            return;
        }

        String method = request.getMethod();
        if (method.equals("POST")) {
            if (request.getParameter("cancelEditing") != null) {
                document.releaseLock();
            } else {
                updateLiveHistory(appleRequest, appleResponse);
            }
            appleResponse.redirectTo(currentPath + "/live-versions.html" + getVariantQueryString());
        } else {
            showLiveHistory(appleRequest, appleResponse);
        }
    }
    
    protected String getPath() {
        return currentPath + "/live-history-edit/" + getContinuationId();
    }

    protected void updateLiveHistory(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        FrontEndContext ctx = FrontEndContext.get(request);
        
        String liveHistoryData = request.getParameter("liveversions-postdata");
        
        final Map<Long, LiveHistoryEntry> lheById = new HashMap<Long, LiveHistoryEntry>();
        final Timeline timeline = document.getTimeline();
        for (LiveHistoryEntry lhe: timeline.getLiveHistory()) {
            lheById.put(lhe.getId(), lhe);
        }
        DefaultHandler liveHistoryUpdateHandler = new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName,
                    Attributes attr) throws SAXException {
                if (localName.equals("add")) {
                    Date beginDate = iso8601.parseDateTime(attr.getValue("begindate")).toDate();
                    Date endDate = null;
                    if (attr.getValue("enddate") != null && !attr.getValue("enddate").equals("")) {
                        endDate = iso8601.parseDateTime(attr.getValue("enddate")).toDate();
                    }
                    if (ObjectUtils.equals(beginDate, endDate))
                        return;
                    long versionId = Long.parseLong(attr.getValue("versionid"));
                    timeline.addLiveHistoryEntry(beginDate, endDate, versionId);
                } else if (localName.equals("del")) {
                    long entryId = Long.parseLong(attr.getValue("id"));
                    if (lheById.containsKey(entryId)) {
                        timeline.deleteLiveHistoryEntry(lheById.get(entryId));
                    }
                }
            }
        };
        
        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setContentHandler(liveHistoryUpdateHandler);
        reader.parse(new InputSource(new ByteArrayInputStream(liveHistoryData.getBytes())));
        
        timeline.save();
        document.releaseLock();
    }

    /**
     * duplicated from DocumentApple (which does the readonly view)
     * @throws Exception
     */
    protected void showLiveHistory(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("variantParams", getVariantParams());
        viewData.put("variantQueryString", getVariantQueryString());
        viewData.put("documentId", String.valueOf(document.getId()));
        viewData.put("branch", String.valueOf(document.getBranchId()));
        viewData.put("language", String.valueOf(document.getLanguageId()));
        viewData.put("localeAsString", request.getAttribute("localeAsString"));
        viewData.put("activePath", getPath());
        GenericPipeConfig pipeConf = GenericPipeConfig.templatePipe("resources/xml/liveversion_overview.xml");
        pipeConf.setStylesheet("daisyskin:xslt/liveversion_overview.xsl");
        viewData.put("pipeConf", pipeConf);
        viewData.put("editPath", getPath());

        viewData.put("readOnly", false);
        SaxBuffer publisherResponse = performPublisherRequest("versionspage", viewData);
        viewData.put("pageXml", publisherResponse);

        appleResponse.sendPage("internal/genericPipe", viewData);
    }

    /**
     * Duplicated from DocumetnApple
     * @throws Exception
     */
    private SaxBuffer performPublisherRequest(String name, Map params) throws Exception {
        String pipe = "internal/" + name + "_pubreq.xml";
        WikiPublisherHelper wikiPublisherHelper = new WikiPublisherHelper(request, getContext(), serviceManager);
        return wikiPublisherHelper.performPublisherRequest(pipe, params, "html");
    }

}
