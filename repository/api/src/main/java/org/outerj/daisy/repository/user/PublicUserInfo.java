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
package org.outerj.daisy.repository.user;

import org.outerx.daisy.x10.PublicUserInfoDocument;

/**
 * Information about a user which is accessible to all other users.
 *
 * <p>The full user record, as represented by the {@link User} object,
 * is only accessible to Administrator users.
 */
public interface PublicUserInfo {
    long getId();

    String getLogin();

    String getDisplayName();

    PublicUserInfoDocument getXml();
}