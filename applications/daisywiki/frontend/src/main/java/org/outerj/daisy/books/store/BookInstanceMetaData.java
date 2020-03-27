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
package org.outerj.daisy.books.store;

import java.util.Date;
import java.util.GregorianCalendar;

import org.outerj.daisy.repository.VersionMode;
import org.outerx.daisy.x10Bookstoremeta.BookInstanceMetaDataDocument;

public final class BookInstanceMetaData {
    private final Date createdOn;
    private final long createdBy;
    private String bookPath;
    private String label;
    private String bookDefinition;
    private String updateFrom;
    private long dataBranchId;
    private long dataLanguageId;
    private VersionMode dataVersion;

	public BookInstanceMetaData(String label, Date createdOn, long createdBy,  long dataBranchId, long dataLanguageId, VersionMode versionMode, String bookDefinition, String updateFrom) {
        if (createdOn == null)
            throw new IllegalArgumentException("createdOn parameter can not be null");
        if (label == null)
            throw new IllegalArgumentException("label parameter can not be null");
        this.createdOn = createdOn;
        this.createdBy = createdBy;
        this.label = label;
        this.bookDefinition = bookDefinition;
        this.updateFrom = updateFrom;
        this.dataBranchId = dataBranchId;
        this.dataLanguageId = dataLanguageId;
        this.dataVersion = versionMode;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public long getCreatedBy() {
        return createdBy;
    }

    public String getBookPath() {
        return bookPath;
    }

    public String getLabel() {
        return label;
    }

    public void setBookPath(String bookPath) {
        this.bookPath = bookPath;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getBookDefinition() {
		return bookDefinition;
	}

	public void setBookDefinition(String bookDefinition) {
		this.bookDefinition = bookDefinition;
	}
    
	public String getUpdateFrom() {
		return updateFrom;
	}

	public void setUpdateFrom(String updateFrom) {
		this.updateFrom = updateFrom;
	}	
	
    public long getDataBranchId() {
		return dataBranchId;
	}

	public void setDataBranchId(long dataBranchId) {
		this.dataBranchId = dataBranchId;
	}

	public long getDataLanguageId() {
		return dataLanguageId;
	}

	public void setDataLanguageId(long dataLanguageId) {
		this.dataLanguageId = dataLanguageId;
	}	
	
    public VersionMode getDataVersion() {
        return dataVersion;
    }

    public void setDataVersion(VersionMode dataVersion) {
        this.dataVersion = dataVersion;
    }
    
    public BookInstanceMetaDataDocument getXml() {
        BookInstanceMetaDataDocument document = BookInstanceMetaDataDocument.Factory.newInstance();
        BookInstanceMetaDataDocument.BookInstanceMetaData metaDataXml = document.addNewBookInstanceMetaData();
        metaDataXml.setLabel(label);
        metaDataXml.setCreatedBy(createdBy);
        if (bookDefinition != null)
			metaDataXml.setBookDefinition(bookDefinition);
		if (updateFrom != null)
			metaDataXml.setUpdateFrom(updateFrom);
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(createdOn);
        metaDataXml.setCreatedOn(calendar);
        if (bookPath != null)
            metaDataXml.setBookPath(bookPath);
        metaDataXml.setDataBranchId(dataBranchId);
        metaDataXml.setDataLanguageId(dataLanguageId);
        metaDataXml.setDataVersion(dataVersion.toString());
        return document;
    }

    public Object clone() {
        BookInstanceMetaData clone = new BookInstanceMetaData(label, createdOn, createdBy,  dataBranchId, dataLanguageId, dataVersion, bookDefinition, updateFrom);
        clone.bookPath = bookPath;
        return clone;
    }


}
