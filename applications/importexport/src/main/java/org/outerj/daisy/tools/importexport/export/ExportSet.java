/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.tools.importexport.export;

import org.outerj.daisy.tools.importexport.docset.DocumentSet;

import java.util.Set;

/**
 * An ExportSet defines what to export. Mainly this specifies the
 * documents, but sometimes it's useful to export some extra schema
 * types or collections.
 */
public interface ExportSet extends DocumentSet {
    Set<String> getDocumentTypes();

    Set<String> getFieldTypes();

    Set<String> getPartTypes();

    Set<String> getCollections();

    Set<String> getNamespaces();
}