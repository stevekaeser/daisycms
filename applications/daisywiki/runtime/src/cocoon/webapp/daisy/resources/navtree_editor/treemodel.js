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

//
// This file contains the node type definitions, XML parser and serializer specific for the Navigation Tree Editor
//

//
// Node types
//

var ROOT_NODE_TYPE = new RootNodeType();
var DOCUMENT_NODE_TYPE = new DocumentNodeType();
var GROUP_NODE_TYPE = new GroupNodeType();
var QUERY_NODE_TYPE = new QueryNodeType();
var LINK_NODE_TYPE = new LinkNodeType();
var IMPORT_NODE_TYPE = new ImportNodeType();
var SEPARATOR_NODE_TYPE = new SeparatorNodeType();

function DocumentNodeType() {
}

DocumentNodeType.prototype.canHaveChildren = function() {
    return true;
}

DocumentNodeType.prototype.getLabel = function(state) {
    if (state.label != null)
        return state.label;
    else if (state.documentId != null)
        return "[" + state.documentId + "]";
    else
        return "\u00A0";
}

DocumentNodeType.prototype.styleClass = "documentnode";
DocumentNodeType.prototype.name = "document";


function GroupNodeType() {
}

GroupNodeType.prototype.canHaveChildren = function() {
    return true;
}

GroupNodeType.prototype.getLabel = function(state) {
    if (state.label != null)
        return state.label;
    else
        return "\u00A0";
}

GroupNodeType.prototype.styleClass = "groupnode";
GroupNodeType.prototype.name = "group";

function QueryNodeType() {
}

QueryNodeType.prototype.canHaveChildren = function() {
    return true;
}

QueryNodeType.prototype.getLabel = function(state) {
    if (state.query != null)
        return "Query: " + state.query;
    else
        return "Query";
}

QueryNodeType.prototype.styleClass = "querynode";
QueryNodeType.prototype.name = "query";

function LinkNodeType() {
}

LinkNodeType.prototype.canHaveChildren = function() {
    return true;
}

LinkNodeType.prototype.getLabel = function(state) {
    if (state.label != null)
        return state.label;
    else
        return "\u00A0";
}

LinkNodeType.prototype.styleClass = "linknode";
LinkNodeType.prototype.name = "link";

function ImportNodeType() {
}

ImportNodeType.prototype.canHaveChildren = function() {
    return false;
}

ImportNodeType.prototype.getLabel = function(state) {
    return "Import";
}

ImportNodeType.prototype.styleClass = "importnode";
ImportNodeType.prototype.name = "import";

function RootNodeType() {
}

RootNodeType.prototype.canHaveChildren = function() {
    return true;
}

RootNodeType.prototype.getLabel = function(state) {
    return "Root";
}

RootNodeType.prototype.styleClass = "rootnode";
RootNodeType.prototype.name = "root";

function SeparatorNodeType() {
}

SeparatorNodeType.prototype.canHaveChildren = function() {
    return false;
}

SeparatorNodeType.prototype.getLabel = function(state) {
    return "-------------";
}

SeparatorNodeType.prototype.styleClass = "separatornode";
SeparatorNodeType.prototype.name = "separator";

//
// Class NavigationTreeBuilder -- builds trees from navigation description XML
//

function NavigationTreeBuilder() {
}

NavigationTreeBuilder.prototype.build = function(xml) {
    var xmldoc = null;

    try {
        xmldoc = treeEditorParseXML(xml);
    } catch (e) {
        alert(e);
        return null;
    }

    var documentElement = xmldoc.documentElement;
    if (this.getLocalName(documentElement) != "navigationTree" || documentElement.namespaceURI != NavigationTreeBuilder.NS) {
        alert("Wrong root element or wrong root element namespace.");
        return null;
    }

    var tree = new Tree();

    try {
        this.buildRecursive(tree.rootNode, documentElement, tree);
    } catch (e) {
        alert(e);
        return;
    }

    return tree;
}

NavigationTreeBuilder.prototype.buildRecursive = function(treeNode, xmlNode, tree) {
    var childNodes = xmlNode.childNodes;
    for (var i = 0; i < childNodes.length; i++) {
        var childNode = childNodes[i];
        if (childNode.nodeType == 1) {
            if (childNode.namespaceURI != NavigationTreeBuilder.NS)
                throw "Encountered element in wrong namespace: " + this.getLocalName(childNode) + " in namespace " + childNode.namespaceURI;

            var newNode = null;
            var state = new Object();
            var localName = this.getLocalName(childNode);
            if (localName == "doc") {
                newNode = new TreeNode(DOCUMENT_NODE_TYPE, tree);
                var id = childNode.getAttribute("id");
                var branch = childNode.getAttribute("branch");
                var language = childNode.getAttribute("language");
                var label = childNode.getAttribute("label");
                var nodeId = childNode.getAttribute("nodeId");
                var visibility = childNode.getAttribute("visibility");
                if (!this.domAttrEmpty(id))
                    state.documentId = id;
                if (!this.domAttrEmpty(branch))
                    state.branch = branch;
                if (!this.domAttrEmpty(language))
                    state.language = language;
                if (!this.domAttrEmpty(label))
                    state.label = label;
                if (!this.domAttrEmpty(nodeId))
                    state.nodeId = nodeId;
                if (!this.domAttrEmpty(visibility))
                    state.visibility = visibility;
            } else if (localName == "link") {
                newNode = new TreeNode(LINK_NODE_TYPE, tree);
                var url = childNode.getAttribute("url");
                var label = childNode.getAttribute("label");
                var id = childNode.getAttribute("id");
                var inheritAclDocId = childNode.getAttribute("inheritAclDocId");
                var inheritAclBranch = childNode.getAttribute("inheritAclBranch");
                var inheritAclLanguage = childNode.getAttribute("inheritAclLanguage");
                if (!this.domAttrEmpty(url))
                    state.url = url;
                if (!this.domAttrEmpty(label))
                    state.label = label;
                if (!this.domAttrEmpty(id))
                    state.id = id;
                if (!this.domAttrEmpty(inheritAclDocId))
                    state.inheritAclDocId = inheritAclDocId;
                if (!this.domAttrEmpty(inheritAclBranch))
                    state.inheritAclBranch = inheritAclBranch;
                if (!this.domAttrEmpty(inheritAclLanguage))
                    state.inheritAclLanguage = inheritAclLanguage;
            } else if (localName == "group") {
                newNode = new TreeNode(GROUP_NODE_TYPE, tree);
                var label = childNode.getAttribute("label");
                var id = childNode.getAttribute("id");
                var visibility = childNode.getAttribute("visibility");
                if (!this.domAttrEmpty(label))
                    state.label = label;
                if (!this.domAttrEmpty(id))
                    state.id = id;
                if (!this.domAttrEmpty(visibility))
                    state.visibility = visibility;
            } else if (localName == "import") {
                newNode = new TreeNode(IMPORT_NODE_TYPE, tree);
                var docId = childNode.getAttribute("docId");
                var branch = childNode.getAttribute("branch");
                var language = childNode.getAttribute("language");
                if (!this.domAttrEmpty(docId))
                    state.docId = docId;
                if (!this.domAttrEmpty(branch))
                    state.branch = branch;
                if (!this.domAttrEmpty(language))
                    state.language = language;
            } else if (localName == "query") {
                newNode = new TreeNode(QUERY_NODE_TYPE, tree);
                var query = childNode.getAttribute("q");
                var filterVariants = childNode.getAttribute("filterVariants");
                var visibility = childNode.getAttribute("visibility");
                var useSelectValues = childNode.getAttribute("useSelectValues");
                if (!this.domAttrEmpty(query))
                    state.query = query;
                if (!this.domAttrEmpty(filterVariants)) {
                    state.filterVariants = (filterVariants == "true");
                } else {
                    state.filterVariants = true;
                }
                if (!this.domAttrEmpty(visibility))
                    state.visibility = visibility;
                if (!this.domAttrEmpty(useSelectValues))
                    state.useSelectValues = useSelectValues
                var queryChildren = childNode.childNodes;
                var columns = [];
                var columnIndex = -1;
                for (var k = 0; k < queryChildren.length; k++) {
                    if (queryChildren[k].nodeType == dojo.dom.ELEMENT_NODE && this.getLocalName(queryChildren[k]) == "column") {
                        var columnSortOrder = queryChildren[k].getAttribute("sortOrder");
                        var columnVisibility = queryChildren[k].getAttribute("visibility");
                        columnIndex++;
                        columns[columnIndex] = {"sortOrder" : columnSortOrder, "visibility" : columnVisibility};
                    }
                }
                state.columns = columns;
            } else if (localName == "separator") {
                newNode = new TreeNode(SEPARATOR_NODE_TYPE, tree);
            } else if (localName == "collections") {
                if (treeNode.nodeType != ROOT_NODE_TYPE)
                    throw "Encountered collections element at an invalid location.";
                var collectionNodes = childNode.childNodes;
                for (var k = 0; k < collectionNodes.length; k++) {
                    if (collectionNodes[k].nodeType == 1 && this.getLocalName(collectionNodes[k]) == "collection") {
                        var collectionName = collectionNodes[k].getAttribute("name");
                        if (collectionName != "") {
                            if (state.collections == null)
                                state.collections = new Array();
                            state.collections[state.collections.length] = collectionName;
                        }
                    }
                }
                // set state on the root node
                treeNode.setState(state);
            } else if (localName == "column") {
                if (treeNode.nodeType != QUERY_NODE_TYPE)
                    throw "Encountered <column> element at an invalid location.";
                // do nothing with it here
            } else {
                throw "Invalid element: " + localName;
            }

            if (newNode != null) {
                if (!treeNode.canHaveChildren())
                    throw "Node " + this.getLocalName(xmlNode) + " is not allowed to have children.";
                newNode.setState(state);
                treeNode.addChild(newNode, treeNode.children.length);
                this.buildRecursive(newNode, childNode, tree);
            }
        }
    }
}

NavigationTreeBuilder.prototype.domAttrEmpty = function(value) {
    // normally, if you get a non-defined attribute from a dom tree it should
    // give an empty string, but firefox 1.0 feels good about herself and
    // thinks she is allowed to return null
    return value == null || value == "";
}

NavigationTreeBuilder.NS = "http://outerx.org/daisy/1.0#navigationspec";

NavigationTreeBuilder.prototype.getLocalName = function(element) {
    if (element.localName != null)
        return element.localName; // DOM
    else
        return element.baseName; // Internet Explorer
}

//
// Class NavigationTreeSerializer
//

function NavigationTreeSerializer() {
}

NavigationTreeSerializer.prototype.serialize = function(tree) {
    var buffer = new StringBuffer();

    var rootNode = tree.rootNode;
    this.serializeNode(rootNode, buffer, 0);

    return buffer.toString();
}

NavigationTreeSerializer.prototype.serializeChildren = function(treeNode, buffer, nestingLevel) {
    for (var i = 0; i < treeNode.children.length; i++) {
        this.serializeNode(treeNode.children[i], buffer, nestingLevel);
    }
}

NavigationTreeSerializer.prototype.serializeNode = function(treeNode, buffer, nestingLevel) {
    buffer.append(this.spaces(nestingLevel * 2));

    if (treeNode.nodeType == ROOT_NODE_TYPE) {
        buffer.append('<d:navigationTree xmlns:d="http://outerx.org/daisy/1.0#navigationspec">\n');
        var state = treeNode.getState();
        if (state.collections != null) {
            buffer.append("  <d:collections>\n");
            for (var i = 0; i < state.collections.length; i++) {
                buffer.append('    <d:collection name="').append(this.escape(state.collections[i])).append('"/>\n');
            }
            buffer.append("  </d:collections>\n");
        }
        this.serializeChildren(treeNode, buffer, nestingLevel + 1);
        buffer.append('</d:navigationTree>');
    } else if (treeNode.nodeType == DOCUMENT_NODE_TYPE) {
        buffer.append('<d:doc');
        var state = treeNode.getState();
        if (state.documentId != null)
            buffer.append(' id="').append(this.escape(state.documentId)).append('"');
        if (state.branch != null)
            buffer.append(' branch="').append(this.escape(state.branch)).append('"');
        if (state.language != null)
            buffer.append(' language="').append(this.escape(state.language)).append('"');
        if (state.nodeId != null)
            buffer.append(' nodeId="').append(this.escape(state.nodeId)).append('"');
        if (state.label != null)
            buffer.append(' label="').append(this.escape(state.label)).append('"');
        if (state.visibility != null && state.visibility != "always")
            buffer.append(' visibility="').append(this.escape(state.visibility)).append('"');
        if (treeNode.children.length > 0) {
            buffer.append(">\n");
            this.serializeChildren(treeNode, buffer, nestingLevel + 1);
            buffer.append(this.spaces(nestingLevel * 2)).append("</d:doc>\n");
        } else {
            buffer.append("/>\n");
        }
    } else if (treeNode.nodeType == GROUP_NODE_TYPE) {
        buffer.append('<d:group');
        var state = treeNode.getState();
        if (state.id != null)
            buffer.append(' id="').append(this.escape(state.id)).append('"');
        if (state.label != null)
            buffer.append(' label="').append(this.escape(state.label)).append('"');
        if (state.visibility != null && state.visibility != "always")
            buffer.append(' visibility="').append(this.escape(state.visibility)).append('"');
        if (treeNode.children.length > 0) {
            buffer.append(">\n");
            this.serializeChildren(treeNode, buffer, nestingLevel + 1);
            buffer.append(this.spaces(nestingLevel * 2)).append("</d:group>\n");
        } else {
            buffer.append("/>\n");
        }
    } else if (treeNode.nodeType == QUERY_NODE_TYPE) {
        buffer.append('<d:query');
        var state = treeNode.getState();
        if (state.query != null)
            buffer.append(' q="').append(this.escape(state.query)).append('"');
        if (state.filterVariants != null && state.filterVariants == false)
            buffer.append(' filterVariants="false"');
        if (state.visibility != null && state.visibility != "always")
            buffer.append(' visibility="').append(this.escape(state.visibility)).append('"');
        if (state.useSelectValues != null && state.useSelectValues != "all")
            buffer.append(' useSelectValues="').append(this.escape(state.useSelectValues)).append('"');
        if (treeNode.children.length > 0 || (state.columns != null && state.columns.length > 0)) {
            buffer.append(">\n");
            if (state.columns != null) {
                for (var i = 0; i < state.columns.length; i++) {
                    var column = state.columns[i];
                    buffer.append(this.spaces((nestingLevel + 1) * 2));
                    buffer.append("<d:column");
                    if (column.sortOrder != null)
                        buffer.append(' sortOrder="').append(this.escape(column.sortOrder)).append('"');
                    if (column.visibility != null)
                        buffer.append(' visibility="').append(this.escape(column.visibility)).append('"');
                    buffer.append("/>\n");
                }
            }
            this.serializeChildren(treeNode, buffer, nestingLevel + 1);
            buffer.append(this.spaces(nestingLevel * 2)).append("</d:query>\n");
        } else {
            buffer.append('/>\n');
        }
    } else if (treeNode.nodeType == LINK_NODE_TYPE) {
        buffer.append('<d:link');
        var state = treeNode.getState();
        if (state.url != null)
            buffer.append(' url="').append(this.escape(state.url)).append('"');
        if (state.label != null)
            buffer.append(' label="').append(this.escape(state.label)).append('"');
        if (state.id != null)
            buffer.append(' id="').append(this.escape(state.id)).append('"');
        if (state.inheritAclDocId != null)
            buffer.append(' inheritAclDocId="').append(this.escape(state.inheritAclDocId)).append('"');
        if (state.inheritAclBranch != null)
            buffer.append(' inheritAclBranch="').append(this.escape(state.inheritAclBranch)).append('"');
        if (state.inheritAclLanguage != null)
            buffer.append(' inheritAclLanguage="').append(this.escape(state.inheritAclLanguage)).append('"');
        if (treeNode.children.length > 0) {
            buffer.append(">\n");
            this.serializeChildren(treeNode, buffer, nestingLevel + 1);
            buffer.append(this.spaces(nestingLevel * 2)).append("</d:link>\n");
        } else {
            buffer.append("/>\n");
        }
    } else if (treeNode.nodeType == IMPORT_NODE_TYPE) {
        buffer.append('<d:import');
        var state = treeNode.getState();
        if (state.docId != null)
            buffer.append(' docId="').append(this.escape(state.docId)).append('"');
        if (state.branch != null)
            buffer.append(' branch="').append(this.escape(state.branch)).append('"');
        if (state.language != null)
            buffer.append(' language="').append(this.escape(state.language)).append('"');
        buffer.append('/>\n');
    } else if (treeNode.nodeType == SEPARATOR_NODE_TYPE) {
        buffer.append("<d:separator/>\n");
    } else {
        alert("Encountered unexepected node type during tree serialization. Will continue.");
    }
}

NavigationTreeSerializer.prototype.spaces = function(count) {
    var text = "";
    for (var i = 0; i < count; i++)
        text = text + " ";
    return text;
}

NavigationTreeSerializer.prototype.escape = function(input) {
    return input.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&apos;");
}
