/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.repository.serverimpl;

import org.outerj.daisy.repository.AccessException;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentReadDeniedException;
import org.outerj.daisy.repository.acl.AccessDetails;
import org.outerj.daisy.repository.acl.AclDetailPermission;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.CommonRepository;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerj.daisy.repository.commonimpl.DocumentImpl;
import org.outerj.daisy.repository.commonimpl.DocumentReadAccessWrapper;
import org.outerj.daisy.repository.commonimpl.DocumentStrategy;
import org.outerj.daisy.repository.commonimpl.DocumentVariantImpl;
import org.outerj.daisy.repository.commonimpl.DocumentWriteAccessWrapper;

public class DocumentAccessUtil {
    public static Document protectDocument(AclResultInfo aclInfo, Document theDocument, DocId docId, boolean updateable,
            AuthenticatedUser user, CommonRepository repository,
            DocumentStrategy documentStrategy) throws AccessException {

        if (!aclInfo.isAllowed(AclPermission.READ)) {
            LabelUtil labelUtil = new LabelUtil(repository, user);
            throw new DocumentReadDeniedException(docId.toString(), labelUtil.getBranchLabel(theDocument.getBranchId()),
                    labelUtil.getLanguageLabel(theDocument.getLanguageId()), labelUtil.getUserLabel(user));
        }

        DocumentImpl document = (DocumentImpl)theDocument;

        AccessDetails readAccessDetails = aclInfo.getAccessDetails(AclPermission.READ);

        if (!readAccessDetails.isFullAccess()
                && readAccessDetails.liveOnly()
                && document.getLiveVersionId() == -1)
            throw new AccessException("Access denied. Document has no live version.");

        // In case you wonder: even if the user does not have read access, he can still
        // get a copy of the document object on which the setter methods work. Except
        // for the case he can't read all versions.
        if (updateable && readAccessDetails.isGranted(AclDetailPermission.NON_LIVE)) {
            // For updateable documents, it would be practical to do the fields
            // and parts filtering in a wrapper (as it would require to also keep
            // some part and field state in the wrapper, which would make it much
            // too complex), therefore we make sure the document
            // only contains data (fields, parts, summary) that one is allowed to read (but not
            // necessarily write).
            DocumentImpl.IntimateAccess documentInt = document.getIntimateAccess(documentStrategy);
            DocumentVariantImpl.IntimateAccess variantInt = documentInt.getVariant().getIntimateAccess(documentStrategy);

            if (!readAccessDetails.isGranted(AclDetailPermission.ALL_FIELDS)) {
                variantInt.keepFields(readAccessDetails.getAccessibleFields());
            }

            if (!readAccessDetails.isGranted(AclDetailPermission.ALL_PARTS)) {
                variantInt.keepParts(readAccessDetails.getAccessibleParts());
            }
            
            if (!readAccessDetails.isGranted(AclDetailPermission.SUMMARY)) {
                variantInt.setSummary(null);
            }

            return new DocumentWriteAccessWrapper(document, documentStrategy, readAccessDetails, repository, user);
        } else {
            if (!readAccessDetails.isFullAccess()) {
                if (readAccessDetails.liveOnly() && document.getLiveVersionId() == -1)
                    throw new AccessException("Access denied. Document has no live version.");

                return new DocumentReadAccessWrapper(document, readAccessDetails, repository, user, documentStrategy);
            }
        }

        return document;
    }
}
