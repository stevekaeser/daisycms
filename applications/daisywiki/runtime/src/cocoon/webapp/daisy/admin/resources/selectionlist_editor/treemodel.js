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
// This file contains the node type definitions, XML parser and serializer specific for the Selection List Tree Editor
//

//
// Node types
//

var ROOT_NODE_TYPE = new RootNodeType();
var LISTITEM_NODE_TYPE = new ListItemNodeType();

function ListItemNodeType() {
}

ListItemNodeType.prototype.canHaveChildren = function() {
    return true;
}

ListItemNodeType.prototype.getLabel = function(state) {
    var label;
    if (state.value != null)
        label = state.value;
    else
        label = "\u00A0";

    if (state.labels != null) {
        var labels = "";
        var locales = state.labels.getKeyList().sort();
        for (var i = 0; i < locales.length; i++) {
            if (i > 0)
                labels += ",";
            if (locales[i] != "")
                labels += locales[i] + "=";
            labels += state.labels.item(locales[i]);
            if (labels.length > 60) {
                labels = labels.substring(0, 59) + "...";
                break;
            }
        }
        if (labels.length > 0) {
            return label + " [" + labels + "]";
        }
    }

    return label;
}

ListItemNodeType.prototype.styleClass = "listitemnode";
ListItemNodeType.prototype.name = "listitem";


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
// Class SelectionListTreeBuilder -- builds trees from static selection list XML
//

function SelectionListTreeBuilder() {
}

SelectionListTreeBuilder.prototype.build = function(xml) {
    var xmldoc = null;

    try {
        xmldoc = treeEditorParseXML(xml);
    } catch (e) {
        alert(e);
        return null;
    }

    var documentElement = xmldoc.documentElement;
    if (this.getLocalName(documentElement) != "staticSelectionList" || documentElement.namespaceURI != SelectionListTreeBuilder.NS) {
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

SelectionListTreeBuilder.prototype.buildRecursive = function(treeNode, xmlNode, tree) {
    var childNodes = xmlNode.childNodes;
    for (var i = 0; i < childNodes.length; i++) {
        var childNode = childNodes[i];
        if (childNode.nodeType == 1) {
            if (childNode.namespaceURI != SelectionListTreeBuilder.NS)
                throw "Encountered element in wrong namespace: " + this.getLocalName(childNode) + " in namespace " + childNode.namespaceURI;

            var newNode = null;
            var state = new Object();
            var localName = this.getLocalName(childNode);
            if (localName == "listItem") {
                newNode = new TreeNode(LISTITEM_NODE_TYPE, tree);
                state.value = this.getValue(childNode, selectionListXmlValueNodes[getFieldValueType()]);
                state.labels = this.getLabels(childNode);
            }

            if (newNode != null) {
                if (!treeNode.canHaveChildren())
                    throw "Node " + this.getLocalName(xmlNode) + " is not allowed to have children.";

                newNode.setState(state);
                treeNode.addChild(newNode, treeNode.children.length);
                this.buildRecursive(newNode, childNode, tree);
            } else {
                this.buildRecursive(treeNode, childNode, tree);
            }
        }
    }
}

selectionListXmlValueNodes = {
    "string"   : "string",
    "date"     : "date",
    "datetime" : "dateTime",
    "long"     : "long",
    "double"   : "double",
    "decimal"  : "decimal",
    "boolean"  : "boolean",
    "link"     : "link"
};

SelectionListTreeBuilder.prototype.getValue = function(xmlNode, tagName) {
    var childNodes = xmlNode.childNodes;
    for (var i = 0; i < childNodes.length; i++) {
        var childNode = childNodes[i];
        if (childNode.nodeType == 1 && childNode.namespaceURI == SelectionListTreeBuilder.NS) {
            var localName = this.getLocalName(childNode);
            if (localName == tagName) {
                if (localName == "link") {
                    var documentId = childNode.getAttribute("documentId");
                    var branchId = childNode.getAttribute("branchId");
                    var languageId = childNode.getAttribute("languageId");
                    branchId = branchId == "-1" ? null : branchId;
                    languageId = languageId == "-1" ? null : languageId;
                    var link = "daisy:" + documentId;
                    if (!this.domAttrEmpty(branchId) || !this.domAttrEmpty(languageId)) {
                        link += "@";
                        if (!this.domAttrEmpty(branchId)) {
                            link += branchId;
                        }
                        if (!this.domAttrEmpty(languageId)) {
                            link += ":" + languageId;
                        }
                    }
                    return link;
                } else {
                    return this.getTextContent(childNode);
                }
            }
        }
    }
}

SelectionListTreeBuilder.prototype.getLabels = function(xmlNode) {
    var childNodes = xmlNode.childNodes;
    for (var i = 0; i < childNodes.length; i++) {
        var childNode = childNodes[i];
        if (childNode.nodeType == 1 && childNode.namespaceURI == SelectionListTreeBuilder.NS && this.getLocalName(childNode) == "labels") {
            return this.buildLabels(childNode);
        }
    }
    return new DaisyDictionary();;
}

SelectionListTreeBuilder.prototype.buildLabels = function(xmlNode) {
    var childNodes = xmlNode.childNodes;
    var labels = new DaisyDictionary();
    for (var i = 0; i < childNodes.length; i++) {
        var childNode = childNodes[i];
        if (childNode.nodeType == 1 && childNode.namespaceURI == SelectionListTreeBuilder.NS && this.getLocalName(childNode) == "label") {
            var locale = childNode.getAttribute("locale");
            if (locale == null)
                locale = "";
            var text = this.getTextContent(childNode);
            if (text != null && text != "")
                labels.add(locale, text);
        }
    }
    return labels;
}

SelectionListTreeBuilder.prototype.getTextContent = function(xmlNode) {
    var childNodes = xmlNode.childNodes;
    var textContent = "";
    for (var i = 0; i < childNodes.length; i++) {
        var childNode = childNodes[i];
        if (childNode.nodeType == 3 /* text */ || childNode.nodeType == 4 /* cdata */) {
            textContent += childNode.data;
        }
    }
    return textContent;
}

SelectionListTreeBuilder.prototype.domAttrEmpty = function(value) {
    // normally, if you get a non-defined attribute from a dom tree it should
    // give an empty string, but firefox 1.0 feels good about herself and
    // thinks she is allowed to return null
    return value == null || value == "";
}

SelectionListTreeBuilder.NS = "http://outerx.org/daisy/1.0";

SelectionListTreeBuilder.prototype.getLocalName = function(element) {
    if (element.localName != null)
        return element.localName; // DOM
    else
        return element.baseName; // Internet Explorer
}

//
// Class SelectionListTreeSerializer
//

function SelectionListTreeSerializer() {
}

SelectionListTreeSerializer.prototype.serialize = function(tree) {
    var buffer = new StringBuffer();

    var rootNode = tree.rootNode;
    this.serializeNode(rootNode, buffer, 0);

    return buffer.toString();
}

SelectionListTreeSerializer.prototype.serializeChildren = function(treeNode, buffer, nestingLevel) {
    for (var i = 0; i < treeNode.children.length; i++) {
        this.serializeNode(treeNode.children[i], buffer, nestingLevel);
    }
}

SelectionListTreeSerializer.prototype.daisyUrlRegexp = /^daisy:([0-9]{1,19}(?:-[a-zA-Z0-9_]{1,200})?)(?:@([^:#?]*)(?::([^:#?]*))?(?::([^:#?]*))?)?()(?:\?([^#]*))?(#.*)?$/;

SelectionListTreeSerializer.prototype.serializeNode = function(treeNode, buffer, nestingLevel) {
    buffer.append(this.spaces(nestingLevel * 2));

    if (treeNode.nodeType == ROOT_NODE_TYPE) {
        buffer.append('<d:staticSelectionList xmlns:d="http://outerx.org/daisy/1.0">\n');
        this.serializeChildren(treeNode, buffer, nestingLevel + 1);
        buffer.append('</d:staticSelectionList>');
    } else if (treeNode.nodeType == LISTITEM_NODE_TYPE) {
        buffer.append('<d:listItem>\n');
        var state = treeNode.getState();
        var valueTag = selectionListXmlValueNodes[getFieldValueType()];

        if (valueTag == "link") {
            if (!state.value.match(this.daisyUrlRegexp))
                throw "Invalid link value: " + state.value;
            var documentId = RegExp.$1;
            var branchId = RegExp.$2;
            var languageId = RegExp.$3;
            buffer.append(this.spaces((nestingLevel + 1) * 2)).append('<d:link documentId="');
            buffer.append(this.escape(documentId));
            buffer.append('"');
            if (branchId != null && branchId != "") {
                buffer.append(' branchId="');
                buffer.append(this.escape(branchId));
                buffer.append('"');
            }
            if (languageId != null && languageId != "") {
                buffer.append(' languageId="');
                buffer.append(this.escape(languageId));
                buffer.append('"');
            }
            buffer.append('/>\n');
        } else {
            buffer.append(this.spaces((nestingLevel + 1) * 2)).append('<d:' + valueTag + '>');
            buffer.append(this.escape(state.value));
            buffer.append('</d:' + valueTag + '>\n');
        }

        var labels = state.labels;
        if (labels != null) {
            var labelKeys = labels.getKeyList();
            if (labelKeys.length > 0) {
                buffer.append(this.spaces((nestingLevel + 1) * 2)).append('<d:labels>\n');
                for (var l = 0; l < labelKeys.length; l++) {
                    buffer.append(this.spaces((nestingLevel + 2) * 2)).append('<d:label');
                    buffer.append(' locale="').append(this.escape(labelKeys[l])).append('">').append(this.escape(labels.item(labelKeys[l])));
                    buffer.append('</d:label>\n');
                }
                buffer.append(this.spaces((nestingLevel + 1) * 2)).append('</d:labels>\n');
            }
        }

        if (treeNode.children.length > 0) {
            this.serializeChildren(treeNode, buffer, nestingLevel + 1);
        }
        buffer.append(this.spaces(nestingLevel * 2)).append("</d:listItem>\n");
    } else {
        alert("Encountered unexepected node type during tree serialization. Will continue.");
    }
}

SelectionListTreeSerializer.prototype.spaces = function(count) {
    var text = "";
    for (var i = 0; i < count; i++)
        text = text + " ";
    return text;
}

SelectionListTreeSerializer.prototype.escape = function(input) {
    return input.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&apos;");
}
