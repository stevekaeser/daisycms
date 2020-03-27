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
package org.outerj.daisy.repository.clientimpl;

import org.outerj.daisy.repository.commonimpl.*;
import org.outerj.daisy.repository.commonimpl.comment.CommentStrategy;
import org.outerj.daisy.repository.commonimpl.variant.VariantStrategy;
import org.outerj.daisy.repository.commonimpl.user.UserManagementStrategy;
import org.outerj.daisy.repository.commonimpl.acl.AclStrategy;
import org.outerj.daisy.repository.commonimpl.schema.SchemaStrategy;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.repository.clientimpl.query.RemoteQueryManager;
import org.outerj.daisy.repository.spi.ExtensionProvider;
import org.outerj.daisy.repository.RepositoryManager;

import java.util.Map;

public class RemoteCommonRepository extends CommonRepository {
    private RemoteRepositoryManager.Context context;

    public RemoteCommonRepository(RepositoryManager repositoryManager, RepositoryStrategy repositoryStrategy,
            DocumentStrategy documentStrategy, SchemaStrategy schemaStrategy, AclStrategy aclStrategy,
            UserManagementStrategy userManagementStrategy, VariantStrategy variantStrategy,
            CollectionStrategy collectionStrategy, CommentStrategy commentStrategy,
            RemoteRepositoryManager.Context context, Map<String, ExtensionProvider> extensions, AuthenticatedUser cacheUser) {
        super(repositoryManager, repositoryStrategy, documentStrategy, schemaStrategy, aclStrategy,
                userManagementStrategy, variantStrategy, collectionStrategy, commentStrategy, extensions, cacheUser);
        this.context = context;
    }

    public QueryManager getQueryManager(AuthenticatedUser user) {
        return new RemoteQueryManager(context, user);
    }
}
