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
package org.outerj.daisy.repository.serverimpl.acl;

import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.acl.*;
import org.outerj.daisy.repository.commonimpl.acl.*;
import org.outerj.daisy.query.model.Tristate;
import org.outerj.daisy.query.model.PredicateExpr;
import org.outerj.daisy.query.model.ExprDocData;
import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.ExtQueryContext;

import java.util.Set;
import java.util.HashSet;


/**
 * Evaluates ACL's. This code has not directly been put into the AclImpl/AclObjectImpl
 * classes because those would otherwise have been dependent on code only existing
 * in the server implementation.
 */
public class AclEvaluator {
    private AclStrategy aclStrategy;
    private AclImpl.IntimateAccess aclInt;
    private AclEvaluationContext aclEvaluationContext;
    private Repository repository;

    public AclEvaluator(AclImpl acl, AclStrategy aclStrategy, AclEvaluationContext aclEvaluationContext, Repository repository) {
        this.aclStrategy = aclStrategy;
        this.aclInt = acl.getIntimateAccess(aclStrategy);
        this.aclEvaluationContext = aclEvaluationContext;
        this.repository = repository;
    }

    public boolean hasPotentialWriteAccess(long userId, long[] roleIds, long documentTypeId, long collectionId,
            long branchId, long languageId) throws RepositoryException {
        if (roleIds.length < 1)
            throw new RepositoryException("Checking of potential write access requires at least one role.");

        try {
            if (hasRole(roleIds, Role.ADMINISTRATOR)) {
                return true;
            }

            EvaluationInfo evaluationInfo = new EvaluationInfo(new ExtQueryContext(repository));

            // the results array will contain the ACL evaluation result for each of the given roles
            boolean[] results = new boolean[roleIds.length]; // initialized to false
            for (AclObjectImpl object : aclInt.getObjects()) {
                // Note: later rules overwrite earlier ones
                checkPotentialWriteAccess(object, results, userId, roleIds, documentTypeId, collectionId, branchId,
                        languageId, evaluationInfo);
            }

            // if the result for at least one role is true, then return true
            for (boolean result : results)
                if (result)
                    return true;
            return false;
        } catch (Throwable e) {
            throw new RepositoryException("Error evaluating ACL.", e);
        }
    }

    private void checkPotentialWriteAccess(AclObjectImpl aclObject, boolean[] results, long userId, long[] roleIds,
            long documentTypeId, long collectionId, long branchId, long languageId, EvaluationInfo evaluationInfo)
            throws RepositoryException {
        AclObjectImpl.IntimateAccess aclObjectInt = aclObject.getIntimateAccess(aclStrategy);
        assureExpressionCompiled(aclObject, aclObjectInt);
        Tristate appliesTo;
        try {
            PredicateExpr predicateExpr = (PredicateExpr)aclObjectInt.getCompiledExpression();
            Document document = new DummyDocForAppliesToTest(documentTypeId, collectionId, branchId, languageId);
            appliesTo = predicateExpr.appliesTo(new ExprDocData(document, null), evaluationInfo);
        } catch (QueryException e) {
            throw new RepositoryException("Exception evaluating ACL object expression.", e);
        }

        if (appliesTo == Tristate.NO)
            return;

        // NOTE: the logic below is similar to that in completeAclInfo(), so if you adjust
        //       it here, keep it in sync over there
        for (AclEntryImpl entry : aclObjectInt.getEntries()) {

            for (int r = 0; r < roleIds.length; r++) {
                boolean relevant = false;
                if (entry.getSubjectType() == AclSubjectType.EVERYONE) {
                    relevant = true;
                } else if (entry.getSubjectType() == AclSubjectType.ROLE) {
                    if (roleIds[r] == entry.getSubjectValue())
                        relevant = true;
                } else if (entry.getSubjectType() == AclSubjectType.USER) {
                    if (userId != -1 && userId == entry.getSubjectValue())
                        relevant = true;
                } else if (entry.getSubjectType() == AclSubjectType.OWNER) {
                    // owner rules don't apply for new documents, one can
                    // only be owner of existing documents
                    relevant = false;
                }

                if (relevant) {
                    AclActionType entryAction = entry.get(AclPermission.WRITE);
                    // granting applies always, denying only if the object expression surely applies (and not 'maybe')
                    if (entryAction == AclActionType.GRANT) {
                        results[r] = true;
                    } else if (entryAction == AclActionType.DENY && appliesTo == Tristate.YES) {
                        results[r] = false;
                    }
                }
            }
        }
    }

    public AclResultInfo getAclInfo(long userId, long[] roleIds, Document document) throws RepositoryException {
        return getAclInfo(userId, roleIds, document, false);
    }

    public AclResultInfo getAclInfo(long userId, long[] roleIds, Document document, boolean conceptual) throws RepositoryException {
        if (roleIds.length < 1)
            throw new RepositoryException("Evaluation of ACL requires at least one role");

        try {
            AclResultInfo result = new AclResultInfoImpl(userId, roleIds, document.getId(), document.getBranchId(), document.getLanguageId());

            if (hasRole(roleIds, Role.ADMINISTRATOR)) {
                final String message = "granted because role is Administrator (role id " + Role.ADMINISTRATOR + ")";
                for (AclPermission permission : AclPermission.values())
                    result.set(permission, AclActionType.GRANT, message, message);
                return canonicalize(result);
            }

            if ((document.isPrivate() && userId == -1) || (document.isPrivate() && userId != -1 && document.getOwner() != userId)) {
                final String message = "denied because document is marked as private";
                for (AclPermission permission : AclPermission.values())
                    result.set(permission, AclActionType.DENY, message, message);
                return canonicalize(result);
            }

            for (AclPermission permission : AclPermission.values())
                result.set(permission, AclActionType.DENY, "denied by default", "denied by default");

            // For the actual ACL evaluation itself, we evaluate it for each role individually and
            // afterwards merge the results to take the most permissive ones (i.e. if at least one
            // role allows it, it is allowed, or vice versa, a permission is only denied if all
            // roles deny it)
            AclResultInfo[] results = new AclResultInfo[roleIds.length];
            for (int i = 0; i < results.length; i++)
                results[i] = result.clone();

            for (AclObjectImpl object : aclInt.getObjects()) {
                completeAclInfo(object, results, userId, roleIds, document, conceptual);
            }

            result = merge(results);

            if (!result.isNonLiveAllowed(AclPermission.READ) && document.isRetired()) {
                final String message = "cannot read a retired document if only access to live versions is allowed";
                result.set(AclPermission.READ, AclActionType.DENY, message, message);
            }

            if (!result.isAllowed(AclPermission.READ)) {
                if (result.isAllowed(AclPermission.WRITE)) {
                    final String message = "cannot have write access if no read access";
                    result.set(AclPermission.WRITE, AclActionType.DENY, message, message);
                }
                if (result.isAllowed(AclPermission.PUBLISH)) {
                    final String message = "cannot have publish access if no read access";
                    result.set(AclPermission.PUBLISH, AclActionType.DENY, message, message);
                }
                if (result.isAllowed(AclPermission.DELETE)) {
                    final String message = "cannot have delete access if no read access";
                    result.set(AclPermission.DELETE, AclActionType.DENY, message, message);
                }
            }

            // Combined fine-grained read and fine-grained write access: make sure write
            // permissions are a subset of the read permissions
            if (!result.isFullyAllowed(AclPermission.READ) && result.isAllowed(AclPermission.WRITE)) {
                AccessDetails readDetails = result.getAccessDetails(AclPermission.READ);
                AccessDetails writeDetails = result.getAccessDetails(AclPermission.WRITE);

                if (readDetails.get(AclDetailPermission.NON_LIVE) == AclActionType.DENY) {
                    final String message = "cannot have write access if no read access to non-live data";
                    result.set(AclPermission.WRITE, AclActionType.DENY, message, message);
                } else {
                    // We have write access but no full read access: make sure we can't write
                    // document properties we can't read.
                    if (readDetails.get(AclDetailPermission.ALL_FIELDS) == AclActionType.DENY) {
                        if (writeDetails.get(AclDetailPermission.ALL_FIELDS) == AclActionType.GRANT) {
                            writeDetails.set(AclDetailPermission.ALL_FIELDS, AclActionType.DENY);
                            writeDetails.addAccessibleFields(readDetails.getAccessibleFields());
                        } else {
                            Set<String> fields = new HashSet<String>(writeDetails.getAccessibleFields());
                            fields.retainAll(readDetails.getAccessibleFields());
                            writeDetails.clearAccessibleFields();
                            writeDetails.addAccessibleFields(fields);
                        }
                    }

                    if (readDetails.get(AclDetailPermission.ALL_PARTS) == AclActionType.DENY) {
                        if (writeDetails.get(AclDetailPermission.ALL_PARTS) == AclActionType.GRANT) {
                            writeDetails.set(AclDetailPermission.ALL_PARTS, AclActionType.DENY);
                            writeDetails.addAccessibleParts(readDetails.getAccessibleParts());
                        } else {
                            Set<String> parts = new HashSet<String>(writeDetails.getAccessibleParts());
                            parts.retainAll(readDetails.getAccessibleParts());
                            writeDetails.clearAccessibleParts();
                            writeDetails.addAccessibleParts(parts);
                        }
                    }
                }
            }

            return canonicalize(result);
        } catch (Throwable e) {
            throw new RepositoryException("Error evaluating ACL.", e);
        }
    }

    private AclResultInfo canonicalize(AclResultInfo result) {
        // Make sure the access details are always set, even in case everything is allowed, so that
        // users don't have to check "details is null or details is full access"
        if (result.isAllowed(AclPermission.READ) && result.getAccessDetails(AclPermission.READ) == null)
            result.setDetails(AclPermission.READ, new AccessDetailsImpl(null, AclPermission.READ, AclActionType.GRANT));
        if (result.isAllowed(AclPermission.WRITE) && result.getAccessDetails(AclPermission.WRITE) == null)
            result.setDetails(AclPermission.WRITE, new AccessDetailsImpl(null, AclPermission.WRITE, AclActionType.GRANT));

        return result;
    }

    private void assureExpressionCompiled(AclObject aclObject, AclObjectImpl.IntimateAccess aclObjectInt) throws RepositoryException {
        if (aclObjectInt.getCompiledExpression() == null) {
            Object compiledExpr = aclEvaluationContext.compileObjectExpression(aclObject.getObjectExpr(), repository);
            aclObjectInt.setCompiledExpression(compiledExpr);
        }
    }

    private boolean appliesTo(AclObjectImpl aclObject, AclObjectImpl.IntimateAccess aclObjectInt, Document document,
            boolean conceptual) throws RepositoryException {
        assureExpressionCompiled(aclObject, aclObjectInt);
        return aclEvaluationContext.checkObjectExpression(aclObjectInt.getCompiledExpression(), document, conceptual, repository);
    }

    private void completeAclInfo(AclObjectImpl aclObject, AclResultInfo[] results, long userId, long[] roleIds,
            Document document, boolean conceptual) throws RepositoryException {
        AclObjectImpl.IntimateAccess aclObjectInt = aclObject.getIntimateAccess(aclStrategy);
        // NOTE: the logic below is similar to that in hasPotentialWriteAccess(), so if you adjust
        //       it here, keep it in sync over there
        if (appliesTo(aclObject, aclObjectInt, document, conceptual)) {
            for (AclEntryImpl entry : aclObjectInt.getEntries()) {

                for (int r = 0; r < roleIds.length; r++) {
                    String subjectReason = null;
                    if (entry.getSubjectType() == AclSubjectType.EVERYONE) {
                        subjectReason = "everyone";
                    } else if (entry.getSubjectType() == AclSubjectType.ROLE) {
                        if (roleIds[r] == entry.getSubjectValue())
                            subjectReason = "role is " + entry.getSubjectValue();
                    } else if (entry.getSubjectType() == AclSubjectType.USER) {
                        if (userId != -1 && userId == entry.getSubjectValue())
                            subjectReason = "user is " + userId;
                    } else if (entry.getSubjectType() == AclSubjectType.OWNER && document.getId() != null) {
                        // Note that one can only be owner of existing documents.
                        // Otherwise if the owner is the current user, he would always
                        // get any grants associated with this entry.
                        if (userId != -1 && userId == document.getOwner())
                            subjectReason = "owner is " + userId;
                    }

                    if (subjectReason != null) {
                        for (AclPermission permission : AclPermission.values()) {
                            AclActionType entryAction = entry.get(permission);
                            // Access details only apply to granted read, write and publish permissions
                            if (entryAction == AclActionType.GRANT
                                    && (permission == AclPermission.READ || permission == AclPermission.WRITE || permission == AclPermission.PUBLISH)) {
                                AccessDetails details = entry.getDetails(permission);
                                AccessDetails newDetails;
                                if (details == null) { // grant with no details => full grant
                                    newDetails = new AccessDetailsImpl(null, permission, AclActionType.GRANT);
                                } else { // grant with details => merge with previous details
                                    newDetails = results[r].getAccessDetails(permission);
                                    if (newDetails == null)
                                        newDetails = new AccessDetailsImpl(null, permission, AclActionType.GRANT);
                                    newDetails.overwrite(details);
                                }
                                results[r].set(permission, entryAction, newDetails, aclObject.getObjectExpr(), subjectReason);
                            } else if (entryAction != AclActionType.DO_NOTHING) {
                                results[r].set(permission, entryAction, aclObject.getObjectExpr(), subjectReason);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean hasRole(long[] availableRoles, long roleId) {
        for (long availableRole : availableRoles)
            if (availableRole == roleId)
                return true;
        return false;
    }

    /**
     * Merge the given AclResultInfo's so that the most permissive result is obtained.
     * @param results array with at least one entry
     */
    private AclResultInfo merge(AclResultInfo[] results) {
        if (results.length == 1)
            return results[0];

        AclResultInfo mergedResult = new AclResultInfoImpl(results[0].getUserId(), results[0].getRoleIds(),
                results[0].getDocumentId(), results[0].getBranchId(), results[0].getLanguageId());

        for (AclPermission permission : AclPermission.values()) {
            for (AclResultInfo result : results) {
                if (result.isAllowed(permission)) {
                    AccessDetails details = result.getAccessDetails(permission);
                    if (details == null) {
                        mergedResult.set(permission, AclActionType.GRANT, result.getObjectExpr(permission), result.getSubjectReason(permission));
                        break;
                    }

                    AccessDetails existingDetails = mergedResult.getAccessDetails(permission);
                    if (existingDetails != null) {
                        details = new AccessDetailsImpl(null, details);
                        details.makeUnion(existingDetails);
                    }
                    mergedResult.set(permission, AclActionType.GRANT, details, result.getObjectExpr(permission), result.getSubjectReason(permission));
                    // if there are details, don't break but search further                                                                                     
                }
            }
        }

        return mergedResult;
    }

}
