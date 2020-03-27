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

import org.outerx.daisy.x10Bookstoremeta.BookAclDocument;
import org.outerx.daisy.x10Bookstoremeta.BookAclSubject;
import org.outerx.daisy.x10Bookstoremeta.BookAclAction;

/**
 * An entry in an {@link BookAcl}. This is simply a class and not an interface,
 * since it is just a data holder, it has no behaviour. This object is immutable.
 */
public final class BookAclEntry {
    private final BookAclSubjectType subjectType;
    private final long subjectValue;
    private final BookAclActionType readPermission;
    private final BookAclActionType managePermission;

    public BookAclEntry(BookAclSubjectType subjectType, long subjectValue,
                        BookAclActionType readPermission, BookAclActionType managePermission) {
        if (subjectType == null)
            throw new IllegalArgumentException("subjectType cannot be null");
        if (readPermission == null)
            throw new IllegalArgumentException("readPermission cannot be null");
        if (managePermission == null)
            throw new IllegalArgumentException("managePermission cannot be null");
        if (subjectType == BookAclSubjectType.EVERYONE && subjectValue != -1)
            throw new IllegalArgumentException("subjectValue must be -1 if subjectType is 'everyone'");

        this.subjectType = subjectType;
        this.subjectValue = subjectValue;
        this.readPermission = readPermission;
        this.managePermission = managePermission;
    }

    public BookAclSubjectType getSubjectType() {
        return subjectType;
    }

    public long getSubjectValue() {
        return subjectValue;
    }

    public BookAclActionType getReadPermission() {
        return readPermission;
    }

    public BookAclActionType getManagePermission() {
        return managePermission;
    }

    public BookAclDocument.BookAcl.BookAclEntry getXml() {
        BookAclDocument.BookAcl.BookAclEntry entryXml = BookAclDocument.BookAcl.BookAclEntry.Factory.newInstance();
        entryXml.setSubjectType(BookAclSubject.Enum.forString(subjectType.toString()));
        entryXml.setSubjectValue(subjectValue);
        entryXml.setPermRead(BookAclAction.Enum.forString(readPermission.toString()));
        entryXml.setPermManage(BookAclAction.Enum.forString(managePermission.toString()));
        return entryXml;
    }
}
