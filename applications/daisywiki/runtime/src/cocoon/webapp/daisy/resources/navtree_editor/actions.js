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
// This file contains actions specific for the navigation tree editor
//

//
// Class NewDocumentNodeAction
//

NewDocumentNodeAction.prototype = new AbstractCreateNodeAction();

function NewDocumentNodeAction(treeView) {
    this.treeView = treeView;
}

NewDocumentNodeAction.prototype.perform = function() {
    var treeNode = new TreeNode(DOCUMENT_NODE_TYPE, this.treeView.tree);
    this.addNode(treeNode);
}

NewDocumentNodeAction.prototype.icon = "new_documentnode.gif";

NewDocumentNodeAction.prototype.tooltip = nt_i18n("insert-document-node");

//
// Class NewGroupNodeAction
//

NewGroupNodeAction.prototype = new AbstractCreateNodeAction();

function NewGroupNodeAction(treeView) {
    this.treeView = treeView;
}

NewGroupNodeAction.prototype.perform = function() {
    var treeNode = new TreeNode(GROUP_NODE_TYPE, this.treeView.tree);
    this.addNode(treeNode);
}

NewGroupNodeAction.prototype.icon = "new_groupnode.gif";

NewGroupNodeAction.prototype.tooltip = nt_i18n("insert-group-node");

//
// Class NewLinkNodeAction
//

NewLinkNodeAction.prototype = new AbstractCreateNodeAction();

function NewLinkNodeAction(treeView) {
    this.treeView = treeView;
}

NewLinkNodeAction.prototype.perform = function() {
    var treeNode = new TreeNode(LINK_NODE_TYPE, this.treeView.tree);
    this.addNode(treeNode);
}

NewLinkNodeAction.prototype.icon = "new_linknode.gif";

NewLinkNodeAction.prototype.tooltip = nt_i18n("insert-link-node");

//
// Class NewQueryNodeAction
//

NewQueryNodeAction.prototype = new AbstractCreateNodeAction();

function NewQueryNodeAction(treeView) {
    this.treeView = treeView;
}

NewQueryNodeAction.prototype.perform = function() {
    var treeNode = new TreeNode(QUERY_NODE_TYPE, this.treeView.tree);
    this.addNode(treeNode);
}

NewQueryNodeAction.prototype.icon = "new_querynode.gif";

NewQueryNodeAction.prototype.tooltip = nt_i18n("insert-query-node");

//
// Class NewImportNodeAction
//

NewImportNodeAction.prototype = new AbstractCreateNodeAction();

function NewImportNodeAction(treeView) {
    this.treeView = treeView;
}

NewImportNodeAction.prototype.perform = function() {
    var treeNode = new TreeNode(IMPORT_NODE_TYPE, this.treeView.tree);
    this.addNode(treeNode);
}

NewImportNodeAction.prototype.icon = "new_importnode.gif";

NewImportNodeAction.prototype.tooltip = nt_i18n("insert-import-node");

//
// Class NewImportNodeAction
//

NewQueryImportAction.prototype = new AbstractCreateNodeAction();

function NewQueryImportAction(treeView) {
    this.treeView = treeView;
}

NewQueryImportAction.prototype.perform = function() {
    var queryNode = new TreeNode(QUERY_NODE_TYPE, this.treeView.tree);
    queryNode.state = {
            "query": "select id where documentType='Navigation'",
            "useSelectValues": "0"
    };
    this.addNode(queryNode);
    var importNode = new TreeNode(IMPORT_NODE_TYPE, this.treeView.tree);
    importNode.state = {
            "docId": "${documentId}",
            "branch": "${branchId}",
            "language": "${languageId}"
    };
    queryNode.addChild(importNode, 0);
}

NewQueryImportAction.prototype.icon = "insert_queryimport.gif";

NewQueryImportAction.prototype.tooltip = nt_i18n("insert-query-import");

//
// Class NewSeparatorNodeAction
//

NewSeparatorNodeAction.prototype = new AbstractCreateNodeAction();

function NewSeparatorNodeAction(treeView) {
    this.treeView = treeView;
}

NewSeparatorNodeAction.prototype.perform = function() {
    var treeNode = new TreeNode(SEPARATOR_NODE_TYPE, this.treeView.tree);
    this.addNode(treeNode);
}

NewSeparatorNodeAction.prototype.icon = "new_separatornode.gif";

NewSeparatorNodeAction.prototype.tooltip = nt_i18n("insert-separator-node");

//
// Class InsertDocumentTagAction
//

function InsertDocumentTagAction(treeId) {
    this.treeId = treeId;
}

InsertDocumentTagAction.prototype.perform = function() {
    var textArea = document.getElementById(this.treeId);
    insertInTextArea(textArea, '<d:doc id=""></d:doc>');
}

InsertDocumentTagAction.prototype.icon = "new_documentnode.gif";
InsertDocumentTagAction.prototype.tooltip = nt_i18n("insert-document-tag");

//
// Class InsertQueryTagAction
//

function InsertQueryTagAction(treeId) {
    this.treeId = treeId;
}

InsertQueryTagAction.prototype.perform = function() {
    var textArea = document.getElementById(this.treeId);
    insertInTextArea(textArea, '<d:query q=""/>');
}

InsertQueryTagAction.prototype.icon = "new_querynode.gif";
InsertQueryTagAction.prototype.tooltip = nt_i18n("insert-query-tag");

//
// Class InsertGroupTagAction
//

function InsertGroupTagAction(treeId) {
    this.treeId = treeId;
}

InsertGroupTagAction.prototype.perform = function() {
    var textArea = document.getElementById(this.treeId);
    insertInTextArea(textArea, '<d:group label=""></d:group>');
}

InsertGroupTagAction.prototype.icon = "new_groupnode.gif";
InsertGroupTagAction.prototype.tooltip = nt_i18n("insert-group-tag");

//
// Class InsertImportTagAction
//

function InsertImportTagAction(treeId) {
    this.treeId = treeId;
}

InsertImportTagAction.prototype.perform = function() {
    var textArea = document.getElementById(this.treeId);
    insertInTextArea(textArea, '<d:import docId=""/>');
}

InsertImportTagAction.prototype.icon = "new_importnode.gif";
InsertImportTagAction.prototype.tooltip = nt_i18n("insert-import-tag");

//
// Class InsertLinkTagAction
//

function InsertLinkTagAction(treeId) {
    this.treeId = treeId;
}

InsertLinkTagAction.prototype.perform = function() {
    var textArea = document.getElementById(this.treeId);
    insertInTextArea(textArea, '<d:link url="" label=""></d:link>');
}

InsertLinkTagAction.prototype.icon = "new_linknode.gif";
InsertLinkTagAction.prototype.tooltip = nt_i18n("insert-link-tag");

//
// Class InsertSeparatorTagAction
//

function InsertSeparatorTagAction(treeId) {
    this.treeId = treeId;
}

InsertSeparatorTagAction.prototype.perform = function() {
    var textArea = document.getElementById(this.treeId);
    insertInTextArea(textArea, '<d:separator/>');
}

InsertSeparatorTagAction.prototype.icon = "new_separatornode.gif";
InsertSeparatorTagAction.prototype.tooltip = nt_i18n("insert-separator-tag");

//
// Class InsertNavigationTreeTemplate
//

function InsertNavigationTreeTemplate(treeId) {
    this.treeId = treeId;
}

InsertNavigationTreeTemplate.prototype.perform = function() {
    var textArea = document.getElementById(this.treeId);
    var navigationTreeTemplate = "<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>\n  <d:collections>\n    <d:collection name=\"\"/>\n  </d:collections>\n  <!-- Insert nodes here -->\n</d:navigationTree>";
    insertInTextArea(textArea, navigationTreeTemplate);
}

InsertNavigationTreeTemplate.prototype.icon = "template.gif";
InsertNavigationTreeTemplate.prototype.tooltip = nt_i18n("insert-template");

//
// function previewNavigationTree
//

function previewNavigationTree(xml, treeContext) {
    // Since the navigation tree XML can get large, this needs to be done using POST thus generate a page
    // with a form
    var popup = window.open('', '', 'toolbar=no,menubar=no,personalbar=no,width=400,height=400,left=20,top=40,scrollbars=yes,resizable=yes');
    var doc = popup.document;
    doc.open();
    var actionUrl = treeContext.daisyMountPoint + '/' + treeContext.daisySiteName + '/editing/navigationPreview';
    doc.write("<html><body>Please wait... <form action='" + actionUrl + "' method='POST'>" +
      "<input type='hidden' name='navigationXml' id='navigationXml'/>" +
      "<input type='hidden' name='branch' id='branch'/>" +
      "<input type='hidden' name='language' id='language'/>" +
      "</form></body></html>");
    doc.close();
    doc.getElementById("navigationXml").value = xml;
    doc.getElementById("branch").value = getBranchId();
    doc.getElementById("language").value = getLanguageId();
    doc.forms[0].submit();
}

//
// Class TextAreaPreviewNavigation
//

function TextAreaPreviewNavigation(treeId, treeContext) {
    this.treeId = treeId;
    this.treeContext = treeContext;
}

TextAreaPreviewNavigation.prototype.perform = function() {
    var textArea = document.getElementById(this.treeId);
    var xml = textArea.value;
    previewNavigationTree(xml, this.treeContext);
}

TextAreaPreviewNavigation.prototype.icon = "preview.gif";
TextAreaPreviewNavigation.prototype.tooltip = nt_i18n("preview");

//
// Class TreeViewPreviewNavigation
//

function TreeViewPreviewNavigation(treeView, treeId) {
    this.treeView = treeView;
    this.treeId = treeId;
    this.treeContext = window[treeId + "_treeContext"];
}

TreeViewPreviewNavigation.prototype.perform = function() {
    var nodeEditorKey = this.treeId + "_nodeEditor";
    window[nodeEditorKey].checkChangesApplied();

    var serializer = new NavigationTreeSerializer();
    var xml = serializer.serialize(this.treeView.tree);

    previewNavigationTree(xml, this.treeContext);
}

TreeViewPreviewNavigation.prototype.icon = "preview.gif";
TreeViewPreviewNavigation.prototype.tooltip = nt_i18n("preview");
