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

import java.text.DateFormat;
import java.util.Date;

import org.apache.xmlbeans.XmlCursor;
import org.outerj.daisy.publisher.serverimpl.DummyLexicalHandler;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.xmlutil.StripDocumentHandler;
import org.outerx.daisy.x10.VersionDocument;
import org.xml.sax.ContentHandler;

public class ShallowAnnotatedVersionRequest extends AbstractRequest implements Request {
    public ShallowAnnotatedVersionRequest(LocationInfo locationInfo) {
        super(locationInfo);
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        Document document = publisherContext.getDocument();
        Version version = publisherContext.getVersion();
        if (version != null) {

            VersionDocument versionXml = version.getShallowXml();
            Version liveVersion = document.getLiveVersion();
            long liveVersionId = liveVersion != null ? liveVersion.getId() : -1;
            annotateVersion(versionXml.getVersion(), liveVersionId, publisherContext);
            versionXml.save(new StripDocumentHandler(contentHandler), new DummyLexicalHandler());
        }
    }

    private void annotateVersion(VersionDocument.Version versionXml, long liveVersionId, PublisherContext publisherContext) throws RepositoryException {
        DateFormat dateFormat = publisherContext.getTimestampFormat();
        Repository repository = publisherContext.getRepository();
        UserManager userManager = repository.getUserManager();
        VariantManager variantManager = repository.getVariantManager();

        long creatorId = versionXml.getCreator();
        Date created = versionXml.getCreated().getTime();
        Date lastModified = versionXml.getLastModified().getTime();
        long lastModifierId = versionXml.getLastModifier();
        boolean isLiveVersion = (versionXml.getId() == liveVersionId);

        XmlCursor cursor = versionXml.newCursor();
        cursor.toNextToken();
        cursor.insertAttributeWithValue("createdFormatted", dateFormat.format(created));
        cursor.insertAttributeWithValue("creatorDisplayName", userManager.getUserDisplayName(creatorId));
        cursor.insertAttributeWithValue("lastModifiedFormatted", dateFormat.format(lastModified));
        cursor.insertAttributeWithValue("lastModifierDisplayName", userManager.getUserDisplayName(lastModifierId));
        if (versionXml.isSetSyncedWithLanguageId() && versionXml.getSyncedWithLanguageId() != -1)
            cursor.insertAttributeWithValue("syncedWithLanguage", variantManager.getLanguage(versionXml.getSyncedWithLanguageId(), false).getName());
        if (isLiveVersion)
            cursor.insertAttributeWithValue("live", "true");
        cursor.dispose();

        String updatedName = publisherContext.resolveVariables(versionXml.getDocumentName());
        if (updatedName != null)
            versionXml.setDocumentName(updatedName);
    }
}
