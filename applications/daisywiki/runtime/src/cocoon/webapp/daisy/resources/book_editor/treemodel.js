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
// This file contains the node type definitions, XML parser and serializer specific for the Book Tree Editor
//

//
// Node types
//

var ROOT_NODE_TYPE = new RootNodeType();
var SECTION_NODE_TYPE = new SectionNodeType();
var QUERY_NODE_TYPE = new QueryNodeType();
var IMPORTNAVTREE_NODE_TYPE = new ImportNavTreeNodeType();

function SectionNodeType() {
}

SectionNodeType.prototype.canHaveChildren = function() {
    return true;
}

SectionNodeType.prototype.getLabel = function(state) {
    if (state.title != null)
        return state.title;
    else if (state.documentId != null)
        return "[" + state.documentId + "]";
    else
        return "\u00A0";
}

SectionNodeType.prototype.styleClass = "sectionnode";
SectionNodeType.prototype.name = "section";


function QueryNodeType() {
}

QueryNodeType.prototype.canHaveChildren = function() {
    return false;
}

QueryNodeType.prototype.getLabel = function(state) {
    if (state.query != null)
        return "Query: " + state.query;
    else
        return "Query";
}

QueryNodeType.prototype.styleClass = "querynode";
QueryNodeType.prototype.name = "query";


function ImportNavTreeNodeType() {
}

ImportNavTreeNodeType.prototype.canHaveChildren = function() {
    return false;
}

ImportNavTreeNodeType.prototype.getLabel = function(state) {
    var label = "Import navigation tree";
    if (state.documentId != null)
        label += " " + state.documentId;
    return label;
}

ImportNavTreeNodeType.prototype.styleClass = "importnavtreenode";
ImportNavTreeNodeType.prototype.name = "importnavtree";

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

//
// Class BookTreeBuilder -- builds trees from book description XML
//

function BookTreeBuilder() {
}

BookTreeBuilder.prototype.build = function(xml) {
    var xmldoc = null;

    try {
        xmldoc = treeEditorParseXML(xml);
    } catch (e) {
        alert(e);
        return null;
    }

    var documentElement = xmldoc.documentElement;
    if (this.getLocalName(documentElement) != "book" || documentElement.namespaceURI != BookTreeBuilder.NS) {
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

BookTreeBuilder.prototype.buildRecursive = function(treeNode, xmlNode, tree) {
    var childNodes = xmlNode.childNodes;
    for (var i = 0; i < childNodes.length; i++) {
        var childNode = childNodes[i];
        if (childNode.nodeType == 1) {
            if (!treeNode.canHaveChildren())
                throw "Node " + this.getLocalName(xmlNode) + " is not allowed to have children.";
            if (childNode.namespaceURI != BookTreeBuilder.NS)
                throw "Encountered element in wrong namespace: " + this.getLocalName(childNode) + " in namespace " + childNode.namespaceURI;

            var newNode = null;
            var state = new Object();
            var localName = this.getLocalName(childNode);
            if (localName == "section") {
                newNode = new TreeNode(SECTION_NODE_TYPE, tree);
                var documentId = childNode.getAttribute("documentId");
                var title = childNode.getAttribute("title");
                var navlabel = childNode.getAttribute("navlabel");
                if (documentId != null) {
                    var branch = childNode.getAttribute("branch");
                    var language = childNode.getAttribute("language");
                    var label = childNode.getAttribute("label");
                    var version = childNode.getAttribute("version");
                    var type = childNode.getAttribute("type");
                    if (!this.domAttrEmpty(documentId))
                        state.documentId = documentId;
                    if (!this.domAttrEmpty(branch))
                        state.branch = branch;
                    if (!this.domAttrEmpty(language))
                        state.language = language;
                    if (!this.domAttrEmpty(label))
                        state.label = label;
                    if (!this.domAttrEmpty(version))
                        state.version = version;
                    if (!this.domAttrEmpty(type))
                        state.type = type;
                    if (!this.domAttrEmpty(navlabel))
                        state.navlabel = navlabel;
                    if (!this.domAttrEmpty(title))
                        state.title = title;
                } else if (title != null){
                    var type = childNode.getAttribute("type");
                    if (!this.domAttrEmpty(title))
                        state.title = title;
                    if (!this.domAttrEmpty(type))
                        state.type = type;
                } else {
                    state.nothing = true;
                }
            } else if (localName == "query") {
                newNode = new TreeNode(QUERY_NODE_TYPE, tree);
                var query = childNode.getAttribute("q");
                var filterVariants = childNode.getAttribute("filterVariants");
                var sectionType = childNode.getAttribute("sectionType");
                if (!this.domAttrEmpty(query))
                    state.query = query;
                if (!this.domAttrEmpty(sectionType))
                    state.sectionType = sectionType;
                if (!this.domAttrEmpty(filterVariants)) {
                    state.filterVariants = (filterVariants == "true");
                } else {
                    state.filterVariants = true;
                }
            } else if (localName == "importNavigationTree") {
                newNode = new TreeNode(IMPORTNAVTREE_NODE_TYPE, tree);
                var documentId = childNode.getAttribute("id");
                var branch = childNode.getAttribute("branch");
                var language = childNode.getAttribute("language");
                var path = childNode.getAttribute("path");
                if (!this.domAttrEmpty(documentId))
                    state.documentId = documentId;
                if (!this.domAttrEmpty(branch))
                    state.branch = branch;
                if (!this.domAttrEmpty(language))
                    state.language = language;
                if (!this.domAttrEmpty(path))
                    state.path = path;
            } else if (localName == "content") {
                // do nothing
            } else {
                throw "Invalid element: " + localName;
            }

            if (newNode != null) {
                newNode.setState(state);
                treeNode.addChild(newNode, treeNode.children.length);
                this.buildRecursive(newNode, childNode, tree);
            } else {
                this.buildRecursive(treeNode, childNode, tree);
            }
        }
    }
}

BookTreeBuilder.prototype.domAttrEmpty = function(value) {
    // normally, if you get a non-defined attribute from a dom tree it should
    // give an empty string, but firefox 1.0 feels good about herself and
    // thinks she is allowed to return null
    return value == null || value == "";
}

BookTreeBuilder.NS = "http://outerx.org/daisy/1.0#bookdef";

BookTreeBuilder.prototype.getLocalName = function(element) {
    if (element.localName != null)
        return element.localName; // DOM
    else
        return element.baseName; // Internet Explorer
}

//
// Class BookTreeSerializer
//

function BookTreeSerializer() {
}

BookTreeSerializer.prototype.serialize = function(tree) {
    var buffer = new StringBuffer();

    var rootNode = tree.rootNode;
    this.serializeNode(rootNode, buffer, 0);

    return buffer.toString();
}

BookTreeSerializer.prototype.serializeChildren = function(treeNode, buffer, nestingLevel) {
    for (var i = 0; i < treeNode.children.length; i++) {
        this.serializeNode(treeNode.children[i], buffer, nestingLevel);
    }
}

BookTreeSerializer.prototype.serializeNode = function(treeNode, buffer, nestingLevel) {
    buffer.append(this.spaces(nestingLevel * 2));

    if (treeNode.nodeType == ROOT_NODE_TYPE) {
        buffer.append('<d:book xmlns:d="http://outerx.org/daisy/1.0#bookdef">\n');
        buffer.append('  <d:content>\n');
        this.serializeChildren(treeNode, buffer, nestingLevel + 2);
        buffer.append('  </d:content>\n');
        buffer.append('</d:book>');
    } else if (treeNode.nodeType == SECTION_NODE_TYPE) {
        buffer.append('<d:section');
        var state = treeNode.getState();
        if (state.documentId != null) {
            buffer.append(' documentId="').append(this.escape(state.documentId)).append('"');
            if (state.branch != null)
                buffer.append(' branch="').append(this.escape(state.branch)).append('"');
            if (state.language != null)
                buffer.append(' language="').append(this.escape(state.language)).append('"');
            if (state.version != null)
                buffer.append(' version="').append(this.escape(state.version)).append('"');
            if (state.title != null)
                buffer.append(' title="').append(this.escape(state.title)).append('"');
        } else {
            if (state.title != null)
                buffer.append(' title="').append(this.escape(state.title)).append('"');
        }
        if (state.type != null)
            buffer.append(' type="').append(this.escape(state.type)).append('"');
        if (state.navlabel != null)
            buffer.append(' navlabel="').append(this.escape(state.navlabel)).append('"');

        
        if (treeNode.children.length > 0) {
            buffer.append(">\n");
            this.serializeChildren(treeNode, buffer, nestingLevel + 1);
            buffer.append(this.spaces(nestingLevel * 2)).append("</d:section>\n");
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
        if (state.sectionType != null)
            buffer.append(' sectionType="').append(this.escape(state.sectionType)).append('"');
        buffer.append('/>\n');
    } else if (treeNode.nodeType == IMPORTNAVTREE_NODE_TYPE) {
        buffer.append('<d:importNavigationTree');
        var state = treeNode.getState();
        if (state.documentId != null)
            buffer.append(' id="').append(this.escape(state.documentId)).append('"');
        if (state.branch != null)
            buffer.append(' branch="').append(this.escape(state.branch)).append('"');
        if (state.language != null)
            buffer.append(' language="').append(this.escape(state.language)).append('"');
        if (state.path != null)
            buffer.append(' path="').append(this.escape(state.path)).append('"');
        buffer.append('/>\n');
    } else {
        alert("Encountered unexepected node type during tree serialization. Will continue.");
    }
}

BookTreeSerializer.prototype.spaces = function(count) {
    var text = "";
    for (var i = 0; i < count; i++)
        text = text + " ";
    return text;
}

BookTreeSerializer.prototype.escape = function(input) {
    return input.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&apos;");
}
