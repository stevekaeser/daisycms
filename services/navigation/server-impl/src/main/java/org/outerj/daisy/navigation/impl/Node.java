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
package org.outerj.daisy.navigation.impl;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ValueType;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.util.List;
import java.util.Map;

public interface Node {
    void add(Node node);

    /**
     * The label of the node. The returned object should correspond in type
     * to what {@link #getLabelValueType()} returns.
     *
     * <p>A label could for example be a Date object, in which case the formatting
     * of the date as string only happens during navigation tree production, based
     * on the locale of the user.
     */
    Object getLabel();

    ValueType getLabelValueType();

    /**
     * Usually the same as getLabel, only document nodes sometimes
     * make a difference.
     */
    Object getResolvedLabel();

    /**
     * Returns list of children, both visible and non-visible.
     */
    List<Node> getChildren();

    /**
     * Returns some data that can be attached to a node when its created
     * as part of a query.
     */
    QueryParams getQueryParams();

    /**
     * Returns the original position this node had in the list of
     * child nodes of its parent. This is useful in node-sorting code
     * for cases where we want to sort on this position.
     */
    int getPosition();

    /**
     * Sets the position of a node within its parent. This should be
     * called by the parent node when {@link #add} is called.
     */
    void setPosition(int position);

    /**
     *
     * @param path the path we're looking for, each array element contains an id
     * @param pos the current position in the array
     * @param branchId
     * @param languageId
     * @param foundPath as the corresponding path nodes are found, they should be assigned
     */
    void searchPath(String[] path, int pos, long branchId, long languageId, Node[] foundPath) throws RepositoryException;

    List<Node> searchDocument(VariantKey document) throws RepositoryException;

    /**
     * Nodes representing documents should add themselves to the map, using as
     * key a VariantKey object and as value
     * a String object representing the navigation tree path. A node should not
     * add itself to the map if there is already another mapping for its document ID
     * in the map.
     */
    void populateNodeLookupMap(Map<VariantKey, String> map, String path) throws RepositoryException;

    /**
     * Returns true if the id of this node equals the specified id.
     */
    boolean checkId(String id, long branchId, long languageId) throws RepositoryException;

    /**
     * Generates a navigation tree as XML.
     *
     * <p>If a node is not visible, it should not generate anything (it is the responsibility
     * of the node itself to check this).
     *
     * @param activeNodePath optional, a list of nodes which form the 'active path' in the tree. Nodes which
     *                       are on this path should have an attribute selected=true, and the last node
     *                       should have an attribute active=true.
     *
     * @param depth up to which 'pos' should the tree be generated? Use Integer.MAX_VALUE for "infinite" depth.
     *              The tree will be generated up to and including the specified pos, except for the
     *              nodes which are on the activeNodePath, which will always be generated completely.
     *
     * @return true if some XML was produced (i.e. the node is visible), otherwise false. This should
     *         usually return the same as the isVisible method, however since it might be expensive
     *         to determine the visibility, is is also returned here.
     */
    boolean generateXml(ContentHandler contentHandler, Node[] activeNodePath, int pos, int depth,
            String path, long userId, long[] roleIds, NavigationValueFormatter valueFormatter, boolean addChildCounts) throws RepositoryException, SAXException;

    /**
     * Returns true if this node has an ID and will generate a corresponding node in
     * the output tree.
     */
    boolean isIdentifiable() throws RepositoryException;

    /**
     * Returns the id of this node, only works when {@link #isIdentifiable()} returns true,
     * otherwise throws an UnsupportedOperationException.
     */
    String getId() throws RepositoryException;

    /**
     * Returns true if this node or any of its chilren would produce a visible node in the
     * generated navigation tree.
     *
     * @param activeNodePath this parameter can be null (if there is not active node path)
     */
    boolean isVisible(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException;

    /**
     * Returns true if this node exists for the current user. This is somewhat weaker check
     * then {@link #isVisible}, as it doesn't take the visibility setting of the node in
     * account.
     */
    boolean exists(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException;

    /**
     * Provides an efficient way to get both the visiblity and exists status of the node.
     * This can be done more efficiently in one call as usually part of the check of these
     * two overlaps.
     */
    boolean[] getVisibleAndExists(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException;

    /**
     * A depth-first search for the first visible document node among the descendants of this node
     * (excluding this node itself).
     */
    String findFirstDocumentNode(String path, long userId, long[] roleIds) throws RepositoryException;

    public static final String NAVIGATION_NS = "http://outerx.org/daisy/1.0#navigation";
}
