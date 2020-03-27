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
package org.outerj.daisy.publisher.serverimpl;

import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.diff.Diff;
import org.outerj.daisy.diff.HtmlSaxDiffOutput;
import org.outerj.daisy.docdiff.DocDiffOutput;
import org.outerj.daisy.docdiff.DocDiffOutputHelper;
import org.outerj.daisy.publisher.serverimpl.requestmodel.ContentDiffType;

import java.text.DateFormat;

public class XmlDocDiffOutput implements DocDiffOutput {
    private ContentHandler consumer;
    private DocDiffOutputHelper outputHelper;
    private ContentDiffType diffType;
    private Repository repository;

    public XmlDocDiffOutput(ContentHandler consumer, DocDiffOutputHelper outputHelper, ContentDiffType diffType,
            Repository repository) {
        this.consumer = consumer;
        this.outputHelper = outputHelper;
        this.diffType = diffType;
        this.repository = repository;
    }

    public void begin() throws Exception {
        AttributesImpl attrs = new AttributesImpl();

        // Add an attribute on the root to inform about the default content diff type.
        // The actual diff type will always be 'text' for non-Daisy-HTML part types
        attrs.addAttribute("", "contentDiffType", "contentDiffType", "CDATA", diffType.toString());
        
        startElement("diff-report", attrs);
        startElement("info");

        Version version1 = outputHelper.getVersion1();
        Version version2 = outputHelper.getVersion2();

        Document document1 = outputHelper.getDocument1();
        Document document2 = outputHelper.getDocument2();
        VariantManager variantManager = outputHelper.getRepository().getVariantManager();

        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, outputHelper.getLocale());
        UserManager userManager = outputHelper.getRepository().getUserManager();

        // Version 1
        attrs.clear();
        attrs.addAttribute("", "branch", "branch", "CDATA", variantManager.getBranch(document1.getBranchId(), false).getName());
        attrs.addAttribute("", "language", "language", "CDATA", variantManager.getLanguage(document1.getLanguageId(), false).getName());
        attrs.addAttribute("", "documentId", "documentId", "CDATA", document1.getId());
        attrs.addAttribute("", "id", "id", "CDATA", String.valueOf(version1.getId()));
        attrs.addAttribute("", "created", "created", "CDATA", dateFormat.format(version1.getCreated()));
        attrs.addAttribute("", "creatorId", "creatorId", "CDATA", String.valueOf(version1.getCreator()));
        attrs.addAttribute("", "creatorName", "creatorName", "CDATA", userManager.getUserDisplayName(version1.getCreator()));
        attrs.addAttribute("", "state", "state", "CDATA", version1.getState().toString());
        attrs.addAttribute("", "lastVersionId", "lastVersionId", "CDATA", String.valueOf(document1.getLastVersionId()));

        startElement("version1", attrs);
        endElement("version1");

        // Version 2
        attrs.clear();
        attrs.addAttribute("", "branch", "branch", "CDATA", variantManager.getBranch(document2.getBranchId(), false).getName());
        attrs.addAttribute("", "language", "language", "CDATA", variantManager.getLanguage(document2.getLanguageId(), false).getName());
        attrs.addAttribute("", "documentId", "documentId", "CDATA", document2.getId());
        attrs.addAttribute("", "id", "id", "CDATA", String.valueOf(version2.getId()));
        attrs.addAttribute("", "created", "created", "CDATA", dateFormat.format(version2.getCreated()));
        attrs.addAttribute("", "creatorId", "creatorId", "CDATA", String.valueOf(version2.getCreator()));
        attrs.addAttribute("", "creatorName", "creatorName", "CDATA", userManager.getUserDisplayName(version2.getCreator()));
        attrs.addAttribute("", "state", "state", "CDATA", version2.getState().toString());
        attrs.addAttribute("", "lastVersionId", "lastVersionId", "CDATA", String.valueOf(document2.getLastVersionId()));

        startElement("version2", attrs);
        endElement("version2");

        endElement("info");


        String version1Name = version1.getDocumentName();
        String version2Name = version2.getDocumentName();
        attrs.clear();
        attrs.addAttribute("", "version1", "version1", "CDATA", version1Name);
        attrs.addAttribute("", "version2", "version2", "CDATA", version2Name);
        startElement("documentName", attrs);
        endElement("documentName");
    }

    public void end() throws Exception {
        endElement("diff-report");
    }

    private void startElement(String name, Attributes attrs) throws SAXException {
        consumer.startElement("", name, name, attrs);
    }

    private void startElement(String name) throws SAXException {
        consumer.startElement("", name, name, new AttributesImpl());
    }

    private void endElement(String name) throws SAXException {
        consumer.endElement("", name, name);
    }

    public void partRemoved(Part removedPart) throws Exception {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "typeId", "typeId", "CDATA", String.valueOf(removedPart.getTypeId()));
        attrs.addAttribute("", "typeLabel", "typeLabel", "CDATA", outputHelper.getPartLabel(removedPart.getTypeId()));
        startElement("partRemoved", attrs);
        endElement("partRemoved");
    }

    public void partAdded(Part addedPart) throws Exception {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "typeId", "typeId", "CDATA", String.valueOf(addedPart.getTypeId()));
        attrs.addAttribute("", "typeLabel", "typeLabel", "CDATA", outputHelper.getPartLabel(addedPart.getTypeId()));
        startElement("partAdded", attrs);
        endElement("partAdded");
    }

    public void partUnchanged(Part unchangedPart) throws Exception {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "typeId", "typeId", "CDATA", String.valueOf(unchangedPart.getTypeId()));
        attrs.addAttribute("", "typeLabel", "typeLabel", "CDATA", outputHelper.getPartLabel(unchangedPart.getTypeId()));
        startElement("partUnchanged", attrs);
        endElement("partUnchanged");
    }

    public void partUpdated(Part version1Part, Part version2Part, String part1Data, String part2Data) throws Exception {
        String version1FileName = nullToEmpty(version1Part.getFileName());
        String version2FileName = nullToEmpty(version2Part.getFileName());

        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "typeId", "typeId", "CDATA", String.valueOf(version2Part.getTypeId()));
        attrs.addAttribute("", "typeLabel", "typeLabel", "CDATA", outputHelper.getPartLabel(version2Part.getTypeId()));
        attrs.addAttribute("", "version1MimeType", "version1MimeType", "CDATA", version1Part.getMimeType());
        attrs.addAttribute("", "version2MimeType", "version2MimeType", "CDATA", version2Part.getMimeType());
        attrs.addAttribute("", "version1FileName", "version1FileName", "CDATA", version1FileName);
        attrs.addAttribute("", "version2FileName", "version2FileName", "CDATA", version2FileName);
        attrs.addAttribute("", "version1Size", "version1Size", "CDATA", String.valueOf(version1Part.getSize()));
        attrs.addAttribute("", "version2Size", "version2Size", "CDATA", String.valueOf(version2Part.getSize()));
        startElement("partUpdated", attrs);

        if (part1Data != null) {
            // The diff type only applies to Daisy-HTML parts
            PartType partType = repository.getRepositorySchema().getPartTypeById(version1Part.getTypeId(), false);
            
            ContentDiffType diffType;
            if(partType.isDaisyHtml() || this.diffType!=ContentDiffType.HTML && version1Part.getMimeType().indexOf("xml")!=-1){
                diffType = this.diffType;
            }else{
                diffType = ContentDiffType.TEXT;
            }

            // add an attribute with the actually used content diff type for this part
            attrs.clear();
            attrs.addAttribute("", "contentDiffType", "contentDiffType", "CDATA", diffType.toString());

            switch (diffType) {
                case HTML:
                    startElement("diff", attrs);
                    Diff.diffHTML(part1Data, part2Data, consumer, String.valueOf(version1Part.getTypeId()),  outputHelper.getLocale());
                    endElement("diff");
                    break;
                case HTMLSOURCE:
                    startElement("diff", attrs);
                    Diff.diffTag(part1Data, part2Data, consumer);
                    endElement("diff");
                    break;
                case TEXT:
                    startElement("diff", attrs);
                    Diff.diff(part1Data, part2Data, new HtmlSaxDiffOutput(consumer), -1);
                    endElement("diff");
                    break;
            }
        }

        endElement("partUpdated");
    }

    private String nullToEmpty(String value) {
        if (value == null)
            return "";
        return value;
    }

    public void partMightBeUpdated(Part version2Part) throws Exception {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "typeId", "typeId", "CDATA", String.valueOf(version2Part.getTypeId()));
        attrs.addAttribute("", "typeLabel", "typeLabel", "CDATA", outputHelper.getPartLabel(version2Part.getTypeId()));
        startElement("partMightBeUpdated", attrs);
        endElement("partMightBeUpdated");
    }

    public void beginPartChanges() throws Exception {
        consumer.startElement("", "parts", "parts", new AttributesImpl());
    }

    public void endPartChanges() throws Exception {
        endElement("parts");
    }

    public void beginFieldChanges() throws Exception {
        startElement("fields");
    }

    public void endFieldChanges() throws Exception {
        endElement("fields");
    }

    public void fieldRemoved(Field removedField) throws Exception {
        long typeId = removedField.getTypeId();
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "typeId", "typeId", "CDATA", String.valueOf(typeId));
        attrs.addAttribute("", "typeLabel", "typeLabel", "CDATA", outputHelper.getFieldLabel(typeId));
        attrs.addAttribute("", "version1", "version1", "CDATA", FieldHelper.getFormattedValue(removedField.getValue(), removedField.getValueType(), outputHelper.getLocale(), outputHelper.getRepository()));
        startElement("fieldRemoved", attrs);
        endElement("fieldRemoved");
    }

    public void fieldAdded(Field addedField) throws Exception {
        long typeId = addedField.getTypeId();
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "typeId", "typeId", "CDATA", String.valueOf(typeId));
        attrs.addAttribute("", "typeLabel", "typeLabel", "CDATA", outputHelper.getFieldLabel(typeId));
        attrs.addAttribute("", "version2", "version2", "CDATA", FieldHelper.getFormattedValue(addedField.getValue(), addedField.getValueType(), outputHelper.getLocale(), outputHelper.getRepository()));
        startElement("fieldAdded", attrs);
        endElement("fieldAdded");
    }

    public void fieldUpdated(Field version1Field, Field version2Field) throws Exception {
        long typeId = version1Field.getTypeId();
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "typeId", "typeId", "CDATA", String.valueOf(typeId));
        attrs.addAttribute("", "typeLabel", "typeLabel", "CDATA", outputHelper.getFieldLabel(typeId));
        attrs.addAttribute("", "version1", "version1", "CDATA", FieldHelper.getFormattedValue(version1Field.getValue(), version1Field.getValueType(), outputHelper.getLocale(), outputHelper.getRepository()));
        attrs.addAttribute("", "version2", "version2", "CDATA", FieldHelper.getFormattedValue(version2Field.getValue(), version2Field.getValueType(), outputHelper.getLocale(), outputHelper.getRepository()));
        startElement("fieldUpdated", attrs);
        endElement("fieldUpdated");
    }

    public void beginLinkChanges() throws Exception {
        startElement("links");
    }

    public void linkRemoved(Link link) throws Exception {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "title", "title", "CDATA", link.getTitle());
        attrs.addAttribute("", "target", "target", "CDATA", link.getTarget());
        startElement("linkRemoved", attrs);
        endElement("linkRemoved");
    }

    public void linkAdded(Link link) throws Exception {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "title", "title", "CDATA", link.getTitle());
        attrs.addAttribute("", "target", "target", "CDATA", link.getTarget());
        startElement("linkAdded", attrs);
        endElement("linkAdded");
    }

    public void endLinkChanges() throws Exception {
        endElement("links");
    }
}
