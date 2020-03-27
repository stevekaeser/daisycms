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
package org.outerj.daisy.navigation.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.ValueComparator;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.query.ResultSet;
import org.outerj.daisy.util.ObjectUtils;

/**
 * Builds a navigation tree based on a query.
 */
public class QueryTreeBuilder {
    private final Node parentNode;
    private final String query;
    private final String extraCond;
    private final NodeVisibility nodeVisibility;
    private final List<ColumnConfig> columnConfigs;
    private final NodeBuilder nodeBuilder;
    private final XmlObject[] childNodes;
    private final BuildContext buildContext;
    private final int startNodeIndex;
    private final int maxColumns;
    private Map<Node, EnumMap<BuildContext.CounterType, Counter>> countersByNode = new IdentityHashMap<Node, EnumMap<BuildContext.CounterType, Counter>>();

    private QueryTreeBuilder(Node parentNode, String query, String extraCond,
            int maxColumns, NodeVisibility nodeVisiblity, List<ColumnConfig> columnConfigs, NodeBuilder nodeBuilder,
            XmlObject[] childNodes, BuildContext buildContext) {
        this.parentNode = parentNode;
        this.query = query;
        this.extraCond = extraCond;
        this.nodeVisibility = nodeVisiblity;
        this.columnConfigs = columnConfigs;
        this.nodeBuilder = nodeBuilder;
        this.childNodes = childNodes;
        this.buildContext = buildContext;
        // startNodeIndex is the index of the first node that will be added by the QueryTreeBuilder.
        // We need to remember this because some operations like re-sorting and searching (in getNode)
        // only need to consider the nodes resulting from the query execution, and not its earlier siblings.
        this.startNodeIndex = parentNode.getChildren().size();
        this.maxColumns = maxColumns;

        countersByNode.put(parentNode, buildContext.peekCounters());
    }

    public static void buildQueryBasedTree(Node parentNode, String query, String extraCond,
            int maxColumns, NodeVisibility nodeVisiblity, List<ColumnConfig> columnConfigs, NodeBuilder nodeBuilder,
            XmlObject[] childNodes, BuildContext buildContext) {

        new QueryTreeBuilder(parentNode, query, extraCond, maxColumns, nodeVisiblity, columnConfigs,
                nodeBuilder, childNodes, buildContext).build();

    }

    private void build() {
        // Execute the query
        ResultSet rs;
        try {
            Map<String, String> queryOptions = null;
            if (buildContext.getVersionMode() != null) {
                queryOptions = new HashMap<String, String>(4);
                queryOptions.put("point_in_time", buildContext.getVersionMode().toString());
            }
            rs = buildContext.getNavContext().getRepository().getQueryManager().performQueryReturnResultSet(query, extraCond, queryOptions, Locale.getDefault(), null);
        } catch (Throwable e) {
            parentNode.add(new ErrorNode("(error executing query)"));
            buildContext.getNavContext().getLogger().error("Error executing query in navigation tree: " + query, e);
            return;
        }

        // maxColumns is the number of values that will be used of those selected by the select clause of the query
        // (it can be useful to select more values in order to make them available to expressions in child node attributes)
        int maxColumns = this.maxColumns < 0 ? rs.getColumnCount() : Math.min(this.maxColumns, rs.getColumnCount());

        // Build the subtree from the query results
        buildNavTree(rs, 0, rs.getSize(), 0, maxColumns, parentNode);

        // Sort the siblings in the subtree
        sortNodes(parentNode, new FromQueryNodeComparator());
    }

    private void buildNavTree(ResultSet rs, int startRow, int endRow, int startColumn, int maxColumns, Node startCurrent) {

        // 4 nested loops: one for running over the rows, one for running over the columns,
        // one for running over the values of a field (if it is a multi-value),
        // one for running of the elements in a path (if it is a hierarchical value).
        //
        // In case of a multi-value value, this method is called recursively since we need to
        // run over all columns following the multi-value value for each value of the multi-value.

        nextRow: for (int rowIndex = startRow; rowIndex < endRow; rowIndex++) {
            rs.absolute(rowIndex);
            Node current = startCurrent;

            if (maxColumns == 0) {
                // just execute the children
                executeChildren(current, rs);
            } else {
                for (int columnIndex = startColumn; columnIndex < maxColumns; columnIndex++) {
                    Object[] values;
                    if (rs.isMultiValue(columnIndex)) {
                        values = (Object[])rs.getValue(columnIndex);
                        if (values == null)
                            values = new Object[] { null };
                    } else {
                        values  = new Object[] { rs.getValue(columnIndex) };
                    }

                    Node currentBeforeMultiValue = current;
                    for (int valueIndex = 0; valueIndex < values.length; valueIndex++) {
                        // restore the current node upon each multi-value value, otherwise the values of a multivalue would be nested instead of siblings
                        current = currentBeforeMultiValue;
                        Object[] elements;
                        if (values[valueIndex] != null && rs.isHierarchical(columnIndex))  {
                            elements = ((HierarchyPath)values[valueIndex]).getElements();
                        } else {
                            elements = new Object[] { values[valueIndex] };
                        }

                        for (int pathIndex = 0; pathIndex < elements.length; pathIndex++) {
                            boolean isLastValue = columnIndex == maxColumns - 1 && valueIndex == values.length - 1 && pathIndex == elements.length -1;
                            // if it's the very last value and it is not a link...
                            if (isLastValue && rs.getValueType(columnIndex) != ValueType.LINK) {
                                // .. create a document node
                                current = getNode(current, elements[pathIndex], rs.getValueType(columnIndex), rs.getVariantKey(), columnIndex);
                            } else {
                                // create a document or group node
                                VariantKey variantKey = null;
                                if (rs.getValueType(columnIndex) == ValueType.LINK)
                                    variantKey = (VariantKey)elements[pathIndex];
                                current = getNode(current, elements[pathIndex], rs.getValueType(columnIndex), variantKey, columnIndex);
                            }

                            if (isLastValue) {
                                // execute children
                                executeChildren(current, rs);
                            }

                            // If it is a multivalue and we're at the deepest hierarchical path element, then
                            // run over the remainder of the columns
                            if (rs.isMultiValue(columnIndex) && pathIndex == elements.length - 1) {
                                buildNavTree(rs, rowIndex, rowIndex + 1, columnIndex + 1, maxColumns, current);
                                if (valueIndex == values.length - 1)
                                    continue nextRow;
                            }
                        }
                    }
                }
            }
        }

    }

    private void executeChildren(Node parentNode, ResultSet rs) {
        if (childNodes.length == 0)
            return;

        // Construct the context values we want to make available to the children
        Map<String, ContextValue> contextValues = new HashMap<String, ContextValue>();
        VariantKey key = rs.getVariantKey();
        contextValues.put("documentId", new ContextValue(ValueType.STRING, key.getDocumentId()));
        contextValues.put("branchId", new ContextValue(ValueType.LONG, key.getBranchId()));
        contextValues.put("languageId", new ContextValue(ValueType.LONG, key.getLanguageId()));

        for (int i = 0; i < rs.getColumnCount(); i++) {
            // Don't include hierarchical or multivalues for now, since there's no good way
            // to handle them in the property resolving yet
            if (rs.isHierarchical(i) || rs.isMultiValue(i))
                continue;

            Object value = rs.getValue(i);
            ValueType valueType = rs.getValueType(i);
            contextValues.put(String.valueOf(i + 1), new ContextValue(valueType, value));
        }


        buildContext.pushCounters(getCounters(parentNode));
        buildContext.pushContextValues(contextValues);

        try {
            for (XmlObject childNode : childNodes) {
                nodeBuilder.build(parentNode, childNode, buildContext);
            }
        } finally {
            buildContext.popCounters();
            buildContext.popContextValues();
        }
    }

    /**
     * Returns counters related to the given parent node.
     */
    private EnumMap<BuildContext.CounterType, Counter> getCounters(Node node) {
        EnumMap<BuildContext.CounterType, Counter> counters = countersByNode.get(node);
        if (counters == null) {
            counters = BuildContext.getNewCounters();
            countersByNode.put(node, counters);
        }
        return counters;
    }

    private Node getNode(Node parent, Object labelObject, ValueType labelValueType, VariantKey variantKey, int column) {

        // Search for an existing node
        List<Node> children = parent.getChildren();
        for (int i = getStartIndex(parent); i < children.size(); i++) {
            Node child = children.get(i);
            // It doesn't make sense to have two sibling document nodes pointing to the same variant key,
            // accessing a second one would always result in the first sibling to become active. Therefore,
            // for document nodes only check on the variant key.
            if (variantKey != null) {
                if (!(child instanceof DocumentNode))
                    continue;
                if (((DocumentNode)child).getVariantKey().equals(variantKey))
                    return child;
            } else if (child.getLabelValueType() == labelValueType && ObjectUtils.safeEquals(child.getLabel(), labelObject)) {
                return child;
            }
        }

        QueryParams queryParams = new QueryParams(column);

        // Node does not exist yet, create it
        Node node;
        if (variantKey != null) {
            // if there's a variant key, we make a node that links to it
            node = new DocumentNode(variantKey, null, labelObject, labelValueType, queryParams, getVisibility(column),
                    buildContext.getNavContext(), buildContext.getRootBranchId(), buildContext.getRootLanguageId(), buildContext.getVersionMode());
        } else {
            int groupId = getCounters(parent).get(BuildContext.CounterType.GROUP_COUNTER).augment();
            node = new GroupNode("g" + groupId, labelObject, labelValueType, queryParams, getVisibility(column));
        }
        parent.add(node);
        return node;
    }

    private int getStartIndex(Node node) {
        return node == parentNode ? startNodeIndex : 0;
    }

    private NodeVisibility getVisibility(int column) {
        if (column < columnConfigs.size()) {
            NodeVisibility visibility = columnConfigs.get(column).getVisibility();
            return visibility != null ? visibility : nodeVisibility;
        } else {
            return nodeVisibility;
        }
    }

    private void sortNodes(Node node, FromQueryNodeComparator comparator) {
        boolean hasChildrenFromQuery = false;
        int startIndex = getStartIndex(node);
        List<Node> children = node.getChildren();
        for (int i = startIndex; i < children.size(); i++) {
            Node child = children.get(i);
            if (child.getQueryParams() != null) {
                hasChildrenFromQuery = true;
                break;
            }
        }

        if (hasChildrenFromQuery) {
            Collections.sort(children.subList(startIndex, children.size()), comparator);
            for (int i = startIndex; i < children.size(); i++) {
                sortNodes(children.get(i), comparator);
            }
        }
    }

    public static class ColumnConfig {
        private boolean sort; // sort or not?
        private boolean ascending; // if sorting, how?
        private NodeVisibility visibility;

        public ColumnConfig(boolean sort, boolean sortAscending, NodeVisibility visibility) {
            this.sort = sort;
            this.ascending = sortAscending;
            this.visibility = visibility;
        }

        public boolean sort() {
            return sort;
        }

        public boolean isAscending() {
            return ascending;
        }

        public NodeVisibility getVisibility() {
            return visibility;
        }
    }

    private class FromQueryNodeComparator implements Comparator {
        // Since sorting happens during navigation tree building, the locale here cannot be
        // dependent on the user's request.
        private ValueComparator valueComparator = new ValueComparator(true, Locale.getDefault());

        public int compare(Object o1, Object o2) {
            Node node1 = (Node)o1;
            Node node2 = (Node)o2;

            // QueryParams.column is the index of the selected column,
            // and thus the index in the columnConfigs array. Only values
            // resulting from the same column are sorted among each other,
            // values from different columns are grouped together in the order
            // the column was selected.
            // Mixing of values from different columns can only occur when
            // having variable-length hierarchical values.

            QueryParams qp1 = node1.getQueryParams();
            QueryParams qp2 = node2.getQueryParams();

            if (qp1 == null && qp2 == null) {
                return node1.getPosition() - node2.getPosition();
            } else if (qp1 == null || qp2 == null) {
                // nodes without QueryParams are sorted before nodes without QueryParams
                return qp1 != null ? -1 : 1;
            } else if (node1.getQueryParams().column == node2.getQueryParams().column) {
                ColumnConfig columnConfig = node1.getQueryParams().column < columnConfigs.size() ? columnConfigs.get(node1.getQueryParams().column) : null;

                if (columnConfig == null || !columnConfig.sort()) {
                    // don't sort = sort according to original position
                    return node1.getPosition() - node2.getPosition();
                }

                Object node1Label = node1.getResolvedLabel();
                Object node2Label = node2.getResolvedLabel();
                int result;
                if (node1Label == null && node2Label == null)
                    result = 0;
                else if (node1Label == null)
                    result = -1;
                else if (node2Label == null)
                    result = 1;
                else
                    result = valueComparator.compare(node1Label, node2Label);

                if (node1.getQueryParams().column < columnConfigs.size() && !columnConfigs.get(node1.getQueryParams().column).isAscending())
                    result = result * -1;

                return result;
            } else if (node1.getQueryParams().column < node2.getQueryParams().column) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
