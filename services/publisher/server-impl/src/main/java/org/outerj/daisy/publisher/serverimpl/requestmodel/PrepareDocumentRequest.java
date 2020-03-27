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
import org.outerj.daisy.publisher.serverimpl.docpreparation.ContentProcessor;
import org.outerj.daisy.publisher.serverimpl.docpreparation.PreparationPipe;
import org.outerj.daisy.publisher.serverimpl.docpreparation.PreparedDocuments;
import org.outerj.daisy.publisher.serverimpl.docpreparation.LinkAnnotationConfig;
import org.outerj.daisy.publisher.PublisherException;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.util.Constants;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.regex.Matcher;

public class PrepareDocumentRequest extends AbstractRequest implements Request {
    private final Set inlineParts;
    private final Map<QName, LinkAnnotationConfig> linkAnnotationConfig;

    public PrepareDocumentRequest(Set inlineParts,Map<QName, LinkAnnotationConfig> linkAnnotationConfig, LocationInfo locationInfo) {
        super(locationInfo);
        this.inlineParts = inlineParts;
        this.linkAnnotationConfig = linkAnnotationConfig;
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        PreparedDocuments preparedDocuments = publisherContext.getPreparedDocuments();
        if (preparedDocuments == null)
            throw new PublisherException("prepareDocument was used outside the context of a preparedDocuments request");

        Document document = publisherContext.getDocument();
        Version version = publisherContext.getVersion();
        if (document == null || version == null)
            return;
        
        Version diffVersion = null;
        boolean doDiff = preparedDocuments.isDoDiff();
        
        if (doDiff) {
			List<VersionKey> diffList = new ArrayList<VersionKey>();
			if (preparedDocuments.getDiffList() != null) {
				String[] pieces = preparedDocuments.getDiffList().split(",");
				for (int i = pieces.length - 1; i >= 0; i--) {
					pieces[i] = pieces[i].trim();
					diffList.add(parseVersionKey(pieces[i], publisherContext
							.getRepository().getVariantManager()));
				}
			}
			for (VersionKey versionKey : diffList) {
				if (versionKey.getDocumentId().equals(document.getId())
						&& versionKey.getBranchId() == document.getBranchId()
						&& versionKey.getLanguageId() == document
								.getLanguageId()) {
					diffVersion = document.getVersion(versionKey.getVersionId());
					break;
				}
			}
			if (diffVersion == null) {
				// take previous version
				diffVersion = document.getVersion(version.getId() - 1);
			}

		}
        

        ContentProcessor parentContentProcessor = publisherContext.getContentProcessor();
        PreparationPipe.process(parentContentProcessor, document, version, doDiff, diffVersion, publisherContext, inlineParts,
                linkAnnotationConfig, contentHandler);
    }
    
    private static VersionKey parseVersionKey(String link, VariantManager variantManager) {
        Matcher matcher = Constants.DAISY_LINK_PATTERN.matcher(link);
        if (!matcher.matches())
            throw new IllegalArgumentException("Invalid link: " + link);

        String documentId = matcher.group(1);
        String branchInput = matcher.group(2);
        String languageInput = matcher.group(3);
        String versionInput = matcher.group(4);
        long branchId, languageId, versionId;

        if (branchInput != null && branchInput.length() > 0) {
            try {
                branchId = variantManager.getBranch(branchInput, false).getId();
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        } else {
            branchId = -1;
        }

        if (languageInput != null && languageInput.length() > 0) {
            try {
                languageId = variantManager.getLanguage(languageInput, false).getId();
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        } else {
            languageId = -1;
        }
        
        if (versionInput != null && versionInput.length() > 0) {
            try {
                versionId = Long.parseLong(versionInput);
            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            }
        } else {
            versionId = -1;
        }

        return new VersionKey(documentId, branchId, languageId, versionId);
    }

}
