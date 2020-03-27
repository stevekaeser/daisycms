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
package org.outerj.daisy.books.publisher.impl.publicationprocess;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.cocoon.components.LifecycleHelper;
import org.apache.cocoon.components.flow.util.PipelineUtil;
import org.apache.cocoon.xml.SaxBuffer;
import org.apache.excalibur.source.Source;
import org.outerj.daisy.books.publisher.impl.BookInstanceLayout;
import org.outerj.daisy.books.publisher.impl.bookmodel.Section;
import org.outerj.daisy.books.publisher.impl.bookmodel.SectionContainer;
import org.outerj.daisy.books.publisher.impl.util.XMLPropertiesHelper;
import org.outerj.daisy.frontend.DocumentTypeSpecificStyler;
import org.outerj.daisy.frontend.PreparedDocuments;
import org.outerj.daisy.xmlutil.XmlSerializer;

public class ApplyDocumentTypeStylingTask implements PublicationProcessTask {

    public void run(PublicationContext context) throws Exception {
        context.getPublicationLog().info("Running apply document type styling task.");
        processSections(context.getBook(), context);
    }

    private void processSections(SectionContainer sectionContainer, PublicationContext context) throws Exception {
        Section[] sections = sectionContainer.getSections();
        for (Section section : sections) {
            if (section.getBookStorePath() != null) {
                String bookStorePath = section.getBookStorePath();

                PreparedDocuments preparedDocuments;
                InputStream is = context.getBookInstance().getResource(bookStorePath);
                try {
                    preparedDocuments = PreparedDocumentsBuilder.build(is);
                } finally {
                    is.close();
                }

                applyDocumentTypeStyling(preparedDocuments, context);

                String path = BookInstanceLayout.getDocumentInPublicationStorePath(bookStorePath, context.getPublicationOutputName());
                OutputStream os = context.getBookInstance().getResourceOutputStream(path);
                try {
                    storePreparedDocuments(os, preparedDocuments);
                } finally {
                    os.close();
                }
            }
            processSections(section, context);
        }
    }

    private void applyDocumentTypeStyling(PreparedDocuments preparedDocuments, PublicationContext context) throws Exception {
        SaxBuffer publicationPropertiesBuffer = new SaxBuffer();
        XMLPropertiesHelper.generateSaxFragment(context.getProperties(), publicationPropertiesBuffer);
        SaxBuffer bookMetadataBuffer = new SaxBuffer();
        XMLPropertiesHelper.generateSaxFragment(context.getBookMetadata(), "metadata", bookMetadataBuffer);

        int[] ids = preparedDocuments.getPreparedDocumentIds();
        for (int id : ids) {
            PreparedDocuments.PreparedDocument preparedDocument = preparedDocuments.getPreparedDocument(id);
            SaxBuffer buffer = preparedDocument.getSaxBuffer();

            String stylesheet;
            Object styleInfo = DocumentTypeSpecificStyler.searchStyleInfo(buffer);
            if (styleInfo != null && styleInfo instanceof Long) {
                long documentTypeId = ((Long)styleInfo).longValue();
                String documentTypeName = context.getRepository().getRepositorySchema().getDocumentTypeById(documentTypeId, false).getName();
                stylesheet = determineStylesheet(documentTypeName, context);
            } else {
                // Support for stylehints and possible missing d:document might be implemented later
                throw new Exception("Did not receive a document type ID from searchStyleInfo, instead got: " + styleInfo);
            }

            Map<String, Object> viewData = new HashMap<String, Object>();
            viewData.put("stylesheet", stylesheet);
            viewData.put("isIncluded", String.valueOf(id != 1));
            viewData.put("repository", context.getRepository());
            viewData.put("document", buffer);
            viewData.put("bookMetadataBuffer", bookMetadataBuffer);
            viewData.put("publicationTypeName", context.getPublicationTypeName());
            viewData.put("publicationPropertiesBuffer", publicationPropertiesBuffer);

            SaxBuffer resultBuffer = executePipeline(context.getDaisyCocoonPath() + "/books/StyleDocumentPipe", viewData, context);
            preparedDocuments.putPreparedDocument(preparedDocument.getId(), preparedDocument.getDocumentKey(), resultBuffer);
        }
    }

    private String determineStylesheet(String documentTypeName, PublicationContext context) throws Exception {
        org.apache.excalibur.source.SourceResolver sourceResolver = (org.apache.excalibur.source.SourceResolver)context.getServiceManager().lookup(org.apache.excalibur.source.SourceResolver.ROLE);
        Source source = null;
        try {
            // search for a stylesheet for this document
            // first try a publication-type-specific, document-type-specific XSLT
            source = sourceResolver.resolveURI("wikidata:/books/publicationtypes/" + context.getPublicationTypeName() + "/document-styling/" + documentTypeName + ".xsl");
            if (source.exists()) {
                return source.getURI();
            }
            sourceResolver.release(source);
            source = null;

            // then try a document-type-specific XSLT shared by all publication types
            source = sourceResolver.resolveURI("wikidata:/books/publicationtypes/document-styling/" + documentTypeName + ".xsl");
            if (source.exists()) {
                return source.getURI();
            }

            // finally use the default XSL
            return "wikidata:/books/publicationtypes/common/book-document-to-html.xsl";
        } finally {
            if (source != null)
                sourceResolver.release(source);
            context.getServiceManager().release(sourceResolver);
        }
    }

    /**
     * Executes a Cocoon pipeline and store the result in a SaxBuffer instance.
     */
    private SaxBuffer executePipeline(String pipe, Map viewData, PublicationContext context) throws Exception {
        PipelineUtil pipelineUtil = new PipelineUtil();
        try {
            LifecycleHelper.setupComponent(pipelineUtil, null, context.getAvalonContext(), context.getServiceManager(), null, false);

            SaxBuffer buffer = new SaxBuffer();
            pipelineUtil.processToSAX(pipe, viewData, buffer);

            return buffer;
        } finally {
            LifecycleHelper.dispose(pipelineUtil);
        }
    }

    private void storePreparedDocuments(OutputStream os, PreparedDocuments preparedDocuments) throws Exception {
        XmlSerializer serializer = new XmlSerializer(os);
        serializer.startDocument();
        preparedDocuments.generateSax(serializer);
        serializer.endDocument();
    }

}
