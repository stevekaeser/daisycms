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

import org.xml.sax.ContentHandler;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.xmlutil.StripDocumentHandler;
import org.outerj.daisy.publisher.serverimpl.DummyLexicalHandler;
import org.outerx.daisy.x10.VersionsDocument;
import org.outerx.daisy.x10.VersionDocument;
import org.apache.xmlbeans.XmlCursor;

import java.text.DateFormat;
import java.util.Date;

public class AnnotatedVersionListRequest extends AbstractRequest {
    public AnnotatedVersionListRequest(LocationInfo locationInfo) {
        super(locationInfo);
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        UserManager userManager = publisherContext.getRepository().getUserManager();
        VariantManager variantManager = publisherContext.getRepository().getVariantManager();
        DateFormat dateFormat = publisherContext.getTimestampFormat();
        Document document = publisherContext.getDocument();
        VersionsDocument versionsDocument = document.getVersions().getXml();
        Version liveVersion = document.getLiveVersion();
        long liveVersionId = liveVersion != null ? liveVersion.getId() : -1;
        for (VersionDocument.Version version : versionsDocument.getVersions().getVersionList()) {
            annotateVersion(version, dateFormat, userManager, variantManager, liveVersionId);
        }
        versionsDocument.save(new StripDocumentHandler(contentHandler), new DummyLexicalHandler());
    }

    private void annotateVersion(VersionDocument.Version versionXml, DateFormat dateFormat, UserManager userManager, VariantManager variantManager, long liveVersionId) throws RepositoryException {
        long creatorId = versionXml.getCreator();
        Date created = versionXml.getCreated().getTime();
        Date lastModified = versionXml.getLastModified().getTime();
        long syncedWithLanguageId = -1;
        if (versionXml.isSetSyncedWithLanguageId()) {
             syncedWithLanguageId = versionXml.getSyncedWithLanguageId();
        }
        boolean isLiveVersion = (versionXml.getId() == liveVersionId);
        XmlCursor cursor = versionXml.newCursor();
        cursor.toNextToken();
        cursor.insertAttributeWithValue("createdFormatted", dateFormat.format(created));
        cursor.insertAttributeWithValue("creatorDisplayName", userManager.getUserDisplayName(creatorId));
        cursor.insertAttributeWithValue("lastModifiedFormatted", dateFormat.format(lastModified));
        if (isLiveVersion)
            cursor.insertAttributeWithValue("live", "true");
        if (syncedWithLanguageId != -1) {
            cursor.insertAttributeWithValue("syncedWithLanguageName", variantManager.getLanguage(syncedWithLanguageId, false).getName());
        }
        cursor.dispose();
    }
}
