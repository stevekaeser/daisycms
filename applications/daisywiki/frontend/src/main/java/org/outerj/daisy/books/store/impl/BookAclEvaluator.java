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
package org.outerj.daisy.books.store.impl;

import org.outerj.daisy.books.store.BookAcl;
import org.outerj.daisy.books.store.BookAclEntry;
import org.outerj.daisy.books.store.BookAclSubjectType;
import org.outerj.daisy.books.store.BookAclActionType;
import org.outerj.daisy.repository.user.Role;

public class BookAclEvaluator {
    public static AclResult evaluate(BookAcl acl, long userId, long[] activeRoleIds) {
        if (hasRole(activeRoleIds, Role.ADMINISTRATOR)) {
            return new AclResult(true, true);
        }

        boolean canRead = false;
        boolean canManage = false;

        BookAclEntry[] entries = acl.getEntries();
        for (BookAclEntry entry : entries) {
            BookAclSubjectType subjectType = entry.getSubjectType();
            long subjectValue = entry.getSubjectValue();
            boolean subjectMatch = (subjectType == BookAclSubjectType.EVERYONE)
                    || (subjectType == BookAclSubjectType.USER && subjectValue == userId)
                    || (subjectType == BookAclSubjectType.ROLE && hasRole(activeRoleIds, subjectValue));
            if (subjectMatch) {
                if (entry.getReadPermission() != BookAclActionType.NOTHING)
                    canRead = entry.getReadPermission() == BookAclActionType.GRANT;
                if (entry.getManagePermission() != BookAclActionType.NOTHING)
                    canManage = entry.getManagePermission() == BookAclActionType.GRANT;
            }
        }

        if (!canRead)
            canManage = false;

        return new AclResult(canRead, canManage);
    }

    private static boolean hasRole(long[] roles, long searchedRole) {
        for (long role : roles) {
            if (role == searchedRole)
                return true;
        }
        return false;
    }
}
