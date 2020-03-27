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
// This file contains actions specific for the book tree editor
//

//
// Class NewSectionNodeAction
//

NewSectionNodeAction.prototype = new AbstractCreateNodeAction();

function NewSectionNodeAction(treeView) {
    this.treeView = treeView;
}

NewSectionNodeAction.prototype.perform = function() {
    var treeNode = new TreeNode(SECTION_NODE_TYPE, this.treeView.tree);
    this.addNode(treeNode);
}

NewSectionNodeAction.prototype.icon = "new_sectionnode.gif";

NewSectionNodeAction.prototype.tooltip = bt_i18n("insert-section-node");


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

NewQueryNodeAction.prototype.tooltip = bt_i18n("insert-query-node");

//
// Class NewImportNavTreeNodeAction
//

NewImportNavTreeNodeAction.prototype = new AbstractCreateNodeAction();

function NewImportNavTreeNodeAction(treeView) {
    this.treeView = treeView;
}

NewImportNavTreeNodeAction.prototype.perform = function() {
    var treeNode = new TreeNode(IMPORTNAVTREE_NODE_TYPE, this.treeView.tree);
    this.addNode(treeNode);
}

NewImportNavTreeNodeAction.prototype.icon = "new_importnavtreenode.gif";

NewImportNavTreeNodeAction.prototype.tooltip = bt_i18n("insert-importnavtree-node");

//
// Class InsertSectionTagAction
//

function InsertSectionTagAction(treeId) {
    this.treeId = treeId;
}

InsertSectionTagAction.prototype.perform = function() {
    var textArea = document.getElementById(this.treeId);
    insertInTextArea(textArea, '<d:section documentId=""></d:section>');
}

InsertSectionTagAction.prototype.icon = "new_sectionnode.gif";
InsertSectionTagAction.prototype.tooltip = bt_i18n("insert-section-tag");

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
InsertQueryTagAction.prototype.tooltip = bt_i18n("insert-query-tag");

//
// Class InsertImportNavTreeTagAction
//

function InsertImportNavTreeTagAction(treeId) {
    this.treeId = treeId;
}

InsertImportNavTreeTagAction.prototype.perform = function() {
    var textArea = document.getElementById(this.treeId);
    insertInTextArea(textArea, '<d:importNavigationTree id=""/>');
}

InsertImportNavTreeTagAction.prototype.icon = "new_importnavtreenode.gif";
InsertImportNavTreeTagAction.prototype.tooltip = bt_i18n("insert-importnavtree-tag");

//
// Class InsertBookTreeTemplate
//

function InsertBookTreeTemplate(treeId) {
    this.treeId = treeId;
}

InsertBookTreeTemplate.prototype.perform = function() {
    var textArea = document.getElementById(this.treeId);
    var bookTreeTemplate = "<d:book xmlns:d='http://outerx.org/daisy/1.0#bookdef'>\n  <d:content>\n  <!-- Insert nodes here -->\n  </d:content>\n</d:book>";
    insertInTextArea(textArea, bookTreeTemplate);
}

InsertBookTreeTemplate.prototype.icon = "template.gif";
InsertBookTreeTemplate.prototype.tooltip = bt_i18n("insert-template");

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
    this.alwaysEnabled = true;
}

TextAreaPreviewNavigation.prototype.perform = function() {
    var textArea = document.getElementById(this.treeId);
    var xml = textArea.value;
    previewNavigationTree(xml, this.treeContext);
}

TextAreaPreviewNavigation.prototype.icon = "preview.gif";
TextAreaPreviewNavigation.prototype.tooltip = bt_i18n("preview");

//
// Class TreeViewPreviewNavigation
//

function TreeViewPreviewNavigation(treeView, treeId) {
    this.treeView = treeView;
    this.treeContext = window[treeId + "_treeContext"];
    this.alwaysEnabled = true;
}

TreeViewPreviewNavigation.prototype.perform = function() {
    var serializer = new BookTreeSerializer();
    var xml = serializer.serialize(this.treeView.tree);

    previewNavigationTree(xml, this.treeContext);
}

TreeViewPreviewNavigation.prototype.icon = "preview.gif";
TreeViewPreviewNavigation.prototype.tooltip = bt_i18n("preview");
