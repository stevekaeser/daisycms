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
package org.outerj.daisy.httpconnector.handlers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.LiveHistoryEntry;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.Timeline;
import org.outerj.daisy.util.HttpConstants;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10.LiveHistoryEntryDocument;
import org.outerx.daisy.x10.TimelineDocument;

public class TimelineHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/document/*/timeline";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        String documentId = (String)matchMap.get("1");
        long branchId = HttpUtil.getBranchId(request, repository);
        long languageId = HttpUtil.getLanguageId(request, repository);

        if (request.getMethod().equals(HttpConstants.GET)) {
            Document document = repository.getDocument(documentId, branchId, languageId, true);
            TimelineDocument timelineDoc = document.getTimeline().getXml();
            timelineDoc.save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.POST)) {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());

            Document document = repository.getDocument(documentId, branchId, languageId, true);
            Timeline timeline = document.getTimeline();
            
            // process incoming xml data
            TimelineDocument timelineDoc = TimelineDocument.Factory.parse(request.getInputStream(), xmlOptions);
            List<Long> newIds = new ArrayList<Long>();
            Map<Long, LiveHistoryEntry> lheById = new HashMap<Long, LiveHistoryEntry>();
            
            for (LiveHistoryEntry entry: timeline.getLiveHistory()) {
                lheById.put(entry.getId(), entry);
            }
            
            for (LiveHistoryEntryDocument.LiveHistoryEntry entry: timelineDoc.getTimeline().getLiveHistoryEntryList()) {
                if (entry.isSetId())
                    newIds.add(entry.getId());
            }

            for (LiveHistoryEntry entry: timeline.getLiveHistory()) {
                if (!newIds.contains(entry.getId())) {
                    timeline.deleteLiveHistoryEntry(lheById.get(entry.getId()));
                }
            }
            
            for (LiveHistoryEntryDocument.LiveHistoryEntry entry: timelineDoc.getTimeline().getLiveHistoryEntryList()) {
                if (!entry.isSetId()) {
                    Date beginDate = entry.getBeginDate().getTime();
                    Date endDate = null;
                    if (entry.isSetEndDate()) {
                        endDate = entry.getEndDate().getTime();
                    }
                    timeline.addLiveHistoryEntry(beginDate, endDate, entry.getVersionId());
                }
            }
            
            timeline.save();
            
            timelineDoc = timeline.getXml();
            TimelineDocument.Timeline timelineXml = timelineDoc.getTimeline();
            Calendar lastModified = Calendar.getInstance();
            lastModified.setTime(document.getLastModified());
            timelineXml.setVariantLastModified(lastModified);
            timelineXml.setVariantLastModifier(document.getLastModifier());
            timelineXml.setLiveVersionId(document.getLiveVersionId());
            timelineXml.setVariantUpdateCount(document.getUpdateCount());

            timelineDoc.save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.DELETE)) {
            Document document = repository.getDocument(documentId, branchId, languageId, true);
            document.releaseLock();
            document.getLockInfo(false).getXml().save(response.getOutputStream());
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }
}
