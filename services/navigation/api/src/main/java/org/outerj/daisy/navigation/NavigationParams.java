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
package org.outerj.daisy.navigation;

import java.util.Locale;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;

public class NavigationParams {
    private final VariantKey navigationDoc;
    private final VersionMode versionMode;
    private final String activePath;
    private final boolean contextualized;
    private final int depth;
    private final Locale locale;
    private final boolean addChildCounts;

    public static final int DEFAULT_CONTEXTUALIZED_DEPTH = 1;
    public static final int DEFAULT_NONCONTEXTUALIZED_DEPTH = Integer.MAX_VALUE;

    /**
     * @param navigationDoc key of the repository document containing the navigation description
     * @param versionMode indicates whether the live or last versions of documents should be used
     *                   (applies to both the navigation tree documents themselves, as to the document
     *                    nodes and query results).
     * @param activePath a path in the form "&lt;id&gt;/&lt;id&gt;/&lt;id&gt;" leading to the active
     *                   node. This attribute is optional and can be null. This can be used to make
     *                   sure the expected (i.e. clicked) node is marked as selected when the activeDocument
     *                   occurs at more then one location in the tree, or to highlight a non-document node.
     *                   The implementation must be able to handle the case where the activePath does not exist,
     *                   in which case the activeDocument parameter will be used to highlight the first occurence
     *                   of that document in the tree (if activeDocument != null).
     * @param contextualized indicates whether to produce a limited navigation tree only showing
     *                       the nodes leading to the activePath, or a full navigation tree
     *                       including all nodes
     * @param depth How deep the tree should be generated. In case of a contextualized tree, this should usually be 1.
     * @param locale locale used for formatting labels which result from values selected in queries (via the query node)
     */
    public NavigationParams(VariantKey navigationDoc, VersionMode versionMode, String activePath,
            boolean contextualized, int depth, boolean addChildCounts, Locale locale) {
        this.navigationDoc = navigationDoc;
        this.versionMode = versionMode;
        this.activePath = activePath;
        this.contextualized = contextualized;
        this.depth = depth < 1 ? 1 : depth;
        this.addChildCounts = addChildCounts;
        this.locale = locale;
    }

    public NavigationParams(VariantKey navigationDoc, VersionMode versionMode, String activePath,
            boolean contextualized, int depth, Locale locale) {
        this(navigationDoc, versionMode, activePath, contextualized, depth, false, locale);
    }

    public NavigationParams(VariantKey navigationDoc, VersionMode versionMode, String activePath,
            boolean contextualized, Locale locale) {
        // for contextualized trees, default to depth 1, for non-contextualized tree, default to displaying the entire tree
        this(navigationDoc, versionMode, activePath, contextualized,
                contextualized ? DEFAULT_CONTEXTUALIZED_DEPTH : DEFAULT_NONCONTEXTUALIZED_DEPTH, locale);
    }

    public NavigationParams(VariantKey navigationDoc, VersionMode versionMode, String activePath, boolean contextualized) {
        this(navigationDoc, versionMode, activePath, contextualized, Locale.getDefault());
    }

    public NavigationParams(VariantKey navigationDoc, String activePath, boolean contextualized) {
        this(navigationDoc, VersionMode.LIVE, activePath, contextualized);
    }

    public VariantKey getNavigationDoc() {
        return navigationDoc;
    }

    public VersionMode getVersionMode() {
        return versionMode;
    }

    public String getActivePath() {
        return activePath;
    }

    public boolean getContextualized() {
        return contextualized;
    }

    public int getDepth() {
        return depth;
    }

    public Locale getLocale() {
        return locale;
    }

    public boolean getAddChildCounts() {
        return addChildCounts;
    }
}
