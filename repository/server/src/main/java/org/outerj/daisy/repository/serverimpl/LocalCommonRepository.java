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
package org.outerj.daisy.repository.serverimpl;

import org.outerj.daisy.repository.commonimpl.*;
import org.outerj.daisy.repository.commonimpl.comment.CommentStrategy;
import org.outerj.daisy.repository.commonimpl.variant.VariantStrategy;
import org.outerj.daisy.repository.commonimpl.acl.AclStrategy;
import org.outerj.daisy.repository.commonimpl.schema.SchemaStrategy;
import org.outerj.daisy.repository.commonimpl.user.UserManagementStrategy;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.AvailableVariants;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.spi.ExtensionProvider;
import org.outerj.daisy.repository.serverimpl.query.LocalQueryManager;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.cache.DocumentCache;
import org.outerj.daisy.jdbcutil.JdbcHelper;

import java.util.Map;

/**
 * Extended version of CommonRepository that supports document caching.
 */
public class LocalCommonRepository extends CommonRepository {
    private DocumentCache cache;
    private AuthenticatedUser systemUser;
    private LocalRepositoryManager.Context context;
    private JdbcHelper jdbcHelper;

    public LocalCommonRepository(RepositoryManager repositoryManager, RepositoryStrategy repositoryStrategy,
            DocumentStrategy documentStrategy, SchemaStrategy schemaStrategy, AclStrategy aclStrategy,
            UserManagementStrategy userManagementStrategy, VariantStrategy variantStrategy,
            CollectionStrategy collectionStrategy, CommentStrategy commentStrategy,
            LocalRepositoryManager.Context context, AuthenticatedUser systemUser, DocumentCache cache,
            Map<String, ExtensionProvider> extensions, JdbcHelper jdbcHelper) {
        super(repositoryManager, repositoryStrategy, documentStrategy, schemaStrategy, aclStrategy,
                userManagementStrategy, variantStrategy, collectionStrategy, commentStrategy, extensions, systemUser);

        this.context = context;
        this.cache = cache;
        this.systemUser = systemUser;
        this.jdbcHelper = jdbcHelper;
    }

    public QueryManager getQueryManager(AuthenticatedUser user) {
        return new LocalQueryManager(context, documentStrategy, user, systemUser, jdbcHelper);
    }

    public Document getDocument(DocId docId, long branchId, long languageId, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (updateable) {
            // always load fresh data
            return super.getDocument(docId, branchId, languageId, updateable, user);
        }

        Document document = cache.get(docId.toString(), branchId, languageId);

        if (document == null) {
            document = super.getDocument(docId, branchId, languageId, updateable, systemUser);
            document = document instanceof DocumentWrapper ? ((DocumentWrapper)document).getWrappedDocument(documentStrategy) : document;
            cache.put(docId.toString(), branchId, languageId, (DocumentImpl)document);
        }

        AclResultInfo aclInfo = getAccessManager().getAclInfoOnLive(systemUser, user.getId(), user.getActiveRoleIds(), document);
        return DocumentAccessUtil.protectDocument(aclInfo, document, docId, false, user, this, documentStrategy);
    }

    public AvailableVariants getAvailableVariants(DocId docId, AuthenticatedUser user) throws RepositoryException {
        AvailableVariants availableVariants = cache.getAvailableVariants(docId.toString());
        if (availableVariants == null) {
            availableVariants = super.getAvailableVariants(docId, user);
            cache.put(docId.toString(), availableVariants);
        }
        return availableVariants;
    }
}
