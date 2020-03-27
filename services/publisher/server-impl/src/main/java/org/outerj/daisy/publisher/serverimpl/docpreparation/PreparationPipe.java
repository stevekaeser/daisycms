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
package org.outerj.daisy.publisher.serverimpl.docpreparation;

import java.text.DateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlLong;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.outerj.daisy.publisher.docpreparation.FieldAnnotator;
import org.outerj.daisy.publisher.serverimpl.DummyLexicalHandler;
import org.outerj.daisy.publisher.serverimpl.requestmodel.PublisherContext;
import org.outerj.daisy.repository.CollectionManager;
import org.outerj.daisy.repository.CollectionNotFoundException;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.VersionNotFoundException;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.DocumentTypeNotFoundException;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.variant.BranchNotFoundException;
import org.outerj.daisy.repository.variant.LanguageNotFoundException;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerx.daisy.x10.DocumentDocument;
import org.outerx.daisy.x10.LiveHistoryEntryDocument;
import org.xml.sax.ContentHandler;

public class PreparationPipe {

    /**
     * Streams a document's XML through a chain of SAX processors which will deliver prepared
     * content for publishing. Note that this will happen recursively through the IncludesProcessor.
     */
    public static void process(ContentProcessor parentProcessor, Document document, Version version, boolean doDiff, Version diffVersion, 
            PublisherContext publisherContext, Set inlineParts, Map<QName, LinkAnnotationConfig> linkAnnotationConfig,
            ContentHandler contentHandler) throws Exception {

        DocumentDocument documentDocument = document.getXml(version.getId());
        
        prepareDocumentXml(documentDocument, publisherContext);

        //  Construct the following pipe:
        //
        //      Merge parts -> process content -> substitute variables -> enhance links


        DaisyLinkEnhancerHandler daisyLinkEnhancer =
                new DaisyLinkEnhancerHandler(document.getBranchId(), document.getLanguageId(), linkAnnotationConfig,
                        publisherContext, contentHandler);

        VariablesProcessor variablesProcessor = new VariablesProcessor(daisyLinkEnhancer, publisherContext);

        ContentProcessor contentProcessor = new ContentProcessor(document, version, variablesProcessor, publisherContext, parentProcessor);

        MergePartsHandler mergePartsHandler = new MergePartsHandler(version, doDiff, diffVersion, inlineParts, contentProcessor, publisherContext);

        documentDocument.save(mergePartsHandler, new DummyLexicalHandler());
    }

    public static void prepareDocumentXml(DocumentDocument documentDocument, PublisherContext publisherContext) throws RepositoryException {
        Repository repository = publisherContext.getRepository();
        Locale locale = publisherContext.getLocale();
        VersionMode versionMode = publisherContext.getVersionMode();

        annotateDocument(documentDocument.getDocument(), publisherContext);
        annotateFields(documentDocument.getDocument(), repository, locale, versionMode);
    }

    public static void annotateDocument(DocumentDocument.Document documentXml, PublisherContext publisherContext) throws RepositoryException {
        Repository repository = publisherContext.getRepository();
        VariantManager variantManager = repository.getVariantManager();
        UserManager userManager = repository.getUserManager();
        DateFormat dateFormat = publisherContext.getTimestampFormat();

        // Note: the branch/language from which a document variant has been created may have been deleted
        // in the meantime, so therefore we handle the branch/languageNotFoundExceptions
        String createdFromBranch = null;
        String createdFromLanguage = null;
        if (documentXml.getCreatedFromBranchId() != -1) {
            try {
                createdFromBranch = variantManager.getBranch(documentXml.getCreatedFromBranchId(), false).getName();
            } catch (BranchNotFoundException e) {
                createdFromBranch = String.valueOf(documentXml.getCreatedFromBranchId());
            }
            try {
                createdFromLanguage = variantManager.getLanguage(documentXml.getCreatedFromLanguageId(), false).getName();
            } catch (LanguageNotFoundException e) {
                createdFromLanguage = String.valueOf(documentXml.getCreatedFromLanguageId());
            }
        }
        String typeName = null;
        String typeLabel = null;
        try {
            DocumentType documentType = repository.getRepositorySchema().getDocumentTypeById(documentXml.getTypeId(), false);
            typeName = documentType.getName();
            typeLabel = documentType.getLabel(publisherContext.getLocale());
        } catch (DocumentTypeNotFoundException e) {
            // ignore
        }

        XmlCursor cursor = documentXml.newCursor();
        cursor.toNextToken();
        long modeVersionId = -1;
        try {
            modeVersionId = publisherContext.getDocument().getVersionId(publisherContext.getVersionMode());
        } catch (VersionNotFoundException vnfe) {
            // intentionally empty
        }
        cursor.insertAttributeWithValue("modeVersionId", String.valueOf(modeVersionId));
        cursor.insertAttributeWithValue("createdFormatted", dateFormat.format(documentXml.getCreated().getTime()));
        cursor.insertAttributeWithValue("ownerDisplayName", userManager.getUserDisplayName(documentXml.getOwner()));
        cursor.insertAttributeWithValue("lastModifiedFormatted", dateFormat.format(documentXml.getLastModified().getTime()));
        cursor.insertAttributeWithValue("lastModifierDisplayName", userManager.getUserDisplayName(documentXml.getLastModifier()));
        cursor.insertAttributeWithValue("branch", variantManager.getBranch(documentXml.getBranchId(), false).getName());
        cursor.insertAttributeWithValue("language", variantManager.getLanguage(documentXml.getLanguageId(), false).getName());
        cursor.insertAttributeWithValue("variantLastModifiedFormatted", dateFormat.format(documentXml.getVariantLastModified().getTime()));
        cursor.insertAttributeWithValue("variantLastModifierDisplayName", userManager.getUserDisplayName(documentXml.getVariantLastModifier()));
        
        long referenceLanguageId = documentXml.getReferenceLanguageId();
        if (referenceLanguageId != -1) {
            cursor.insertAttributeWithValue("referenceLanguage", variantManager.getLanguage(documentXml.getReferenceLanguageId(), false).getName());
        }
        if (createdFromBranch != null) {
            cursor.insertAttributeWithValue("createdFromBranch", createdFromBranch);
            cursor.insertAttributeWithValue("createdFromLanguage", createdFromLanguage);
        }
        if (typeName != null)
            cursor.insertAttributeWithValue("typeName", typeName);
        if (typeLabel != null)
            cursor.insertAttributeWithValue("typeLabel", typeLabel);
        cursor.dispose();

        CollectionManager collectionManager = repository.getCollectionManager();
        for (XmlLong collectionIdsXml : documentXml.getCollectionIds().xgetCollectionIdList()) {
            long collectionId = collectionIdsXml.getLongValue();
            String collectionName = null;
            try {
                collectionName = collectionManager.getCollection(collectionId, false).getName();
            } catch (CollectionNotFoundException e) {
                // ignore
            }
            if (collectionName != null) {
                cursor = collectionIdsXml.newCursor();
                cursor.toNextToken();
                cursor.insertAttributeWithValue("name", collectionName);
                cursor.dispose();
            }
        }

        String updatedName = publisherContext.resolveVariables(documentXml.getName());
        if (updatedName != null)
            documentXml.setName(updatedName);
    }

    public static void annotateFields(DocumentDocument.Document documentXml, Repository repository, Locale locale,
            VersionMode versionMode) throws RepositoryException {
        FieldAnnotator.annotateFields(documentXml, repository, locale, versionMode);
    }

    public static void annotateTimeline(DocumentDocument.Document documentXml, PublisherContext ctx) throws RepositoryException {
        DateTimeFormatter formatter = ISODateTimeFormat.dateTime();

        for (LiveHistoryEntryDocument.LiveHistoryEntry entryXml: documentXml.getTimeline().getLiveHistoryEntryArray()) {
            XmlCursor cursor = entryXml.newCursor();
            cursor.toNextToken();
            cursor.insertAttributeWithValue("localBeginDate", formatter.print(new LocalDateTime(entryXml.getBeginDate().getTimeInMillis())));
            if (entryXml.isSetEndDate()) {
                cursor.insertAttributeWithValue("localEndDate", formatter.print(new LocalDateTime(entryXml.getEndDate().getTimeInMillis())));
            }
            cursor.dispose();
        }
    }

}
