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
package org.outerj.daisy.books.publisher.impl.bookmodel;

import java.util.List;

import org.outerx.daisy.x10Bookdef.SectionDocument;

public class Section implements SectionContainer {
    private String documentId = null;
    private long branchId = -1;
    private long languageId = -1;
    private String version;
    private String type;
    private String bookStorePath;
    private String title;
    private String navlabel;
	private boolean changed = false; // can be used to mark changed sections in book updating mechanism
    private SectionContainerHelper sectionContainerHelper = new SectionContainerHelper();

    public Section() {
        super();
    }
    
    public Section(String documentId, long branchId, long languageId, String version, String type, String bookStorePath, String title) {
        super();
        this.documentId = documentId;
        this.branchId = branchId;
        this.languageId = languageId;
        this.version = version;
        this.type = type;
        this.bookStorePath = bookStorePath;
        this.title = title;
    }
    
    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public long getBranchId() {
        return branchId;
    }

    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    public long getLanguageId() {
        return languageId;
    }

    public void setLanguageId(long languageId) {
        this.languageId = languageId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBookStorePath() {
        return bookStorePath;
    }

    public void setBookStorePath(String bookStorePath) {
        this.bookStorePath = bookStorePath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void addSection(Section section) {
        sectionContainerHelper.addSection(section);
    }

    public Section[] getSections() {
        return sectionContainerHelper.getSections();
    }

    public String getNavlabel() {
		return navlabel;
	}

	public void setNavlabel(String navlabel) {
		this.navlabel = navlabel;
	}
    
    public SectionDocument.Section getXml() {
        SectionDocument.Section sectionXml = SectionDocument.Section.Factory.newInstance();

        if (documentId != null)
            sectionXml.setDocumentId(documentId);
        if (branchId != -1)
            sectionXml.setBranch(String.valueOf(branchId));
        if (languageId != -1)
            sectionXml.setLanguage(String.valueOf(languageId));
        if (version != null)
            sectionXml.setVersion(version);
        if (type != null)
            sectionXml.setType(type);
        if (bookStorePath != null)
            sectionXml.setBookStorePath(bookStorePath);
        if (title != null)
            sectionXml.setTitle(title);
        if (navlabel != null)
            sectionXml.setNavlabel(navlabel);

        sectionContainerHelper.addXml(sectionXml);

        return sectionXml;
    }

    public void setXml(SectionDocument.Section sectionXml) {
        if (sectionXml.getDocumentId() != null)
            this.setDocumentId(sectionXml.getDocumentId());
        if (sectionXml.getBranch() != null)
            this.setBranchId(Long.parseLong(sectionXml.getBranch()));
        if (sectionXml.getLanguage() != null)
            this.setLanguageId(Long.parseLong(sectionXml.getLanguage()));
        if (sectionXml.getVersion() != null)
            this.setVersion(sectionXml.getVersion());
        if (sectionXml.getType() != null)
            this.setType(sectionXml.getType());
        if (sectionXml.getBookStorePath() != null)
            this.setBookStorePath(sectionXml.getBookStorePath());
        if (sectionXml.getTitle() != null)
            this.setTitle(sectionXml.getTitle());
        if (sectionXml.getNavlabel() != null)
            this.setNavlabel(sectionXml.getNavlabel());
        
        List<SectionDocument.Section> sectionList = sectionXml.getSectionList();
        
        // iterate through subsections
        for(SectionDocument.Section sXml : sectionList){
            Section s = new Section();
            s.setXml(sXml);
            this.addSection(s);
        }
    }
    
}
