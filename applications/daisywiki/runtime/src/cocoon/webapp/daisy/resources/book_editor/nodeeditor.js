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
// NodeEditor class for the Book editor
//

/**
 * Constructor.
 */
function NodeEditor(treeId, treeContext) {
    this.editorDivs = new Object();
    this.editorForms = new Object();
    this.treeId = treeId;
    this.treeContext = treeContext;

    var iframeAnchor = document.getElementById(treeId + "_treeNodeEditorAnchor");
    this.iframe = document.createElement("IFRAME");
    this.iframe.style.width = "100%";
    this.iframe.style.height = "250px";
    this.iframe.id = treeId + "_treeNodeEditor";
    this.iframe.frameBorder = 0;
    iframeAnchor.parentNode.replaceChild(this.iframe, iframeAnchor);

    var iframeDoc = this.iframe.contentWindow.document;
    iframeDoc.open();
    iframeDoc.write(this.getNodeEditorHTML());
    iframeDoc.close();

    var maxHeight = 0;
    var self = this;

    var nodeNames = ["section", "query", "root", "importnavtree"];
    for (var i = 0; i < nodeNames.length; i++) {
        var nodeName = nodeNames[i];
        var divName = nodeName + "NodeEditor";
        var nodeEditorDiv = this.iframe.contentWindow.document.getElementById(divName);
        if (nodeEditorDiv == null)
            throw "Missing element: " + divName;
        this.editorDivs[nodeName] = nodeEditorDiv;
        if (nodeEditorDiv.offsetHeight > maxHeight)
            maxHeight = nodeEditorDiv.offsetHeight;

        var formName = nodeName + "NodeEditorForm";
        var nodeEditorForm = this.iframe.contentWindow.document.getElementById(formName);
        if (nodeEditorForm == null)
            throw "Missing element: " + formName;
        this.editorForms[nodeName] = nodeEditorForm;
    }

    this.editorForms["section"].title_apply.onclick = function() { return self.apply() };
    this.editorForms["section"].doc_apply.onclick = function() { return self.apply() };
    this.editorForms["query"].apply.onclick = function() { return self.apply() };

    this.iframe.style.height = maxHeight + 15 + "px"; // the 15 is an arbitrary margin

    var documentIdField = this.editorForms["section"].sne_id;
    var documentBranchField = this.editorForms["section"].sne_branch;
    var documentLanguageField = this.editorForms["section"].sne_language;
    var documentNameField = this.editorForms["section"].sne_doc_title;
    this.editorForms["section"].sne_lookupdocument.onclick = function() { self.documentLookup(documentIdField, documentBranchField, documentLanguageField, documentNameField); return false; };
    this.editorForms["section"].sne_createdocument.onclick = function() { self.createDocument(documentIdField, documentBranchField, documentLanguageField, documentNameField); return false; };
    this.editorForms["query"].qne_test.onclick = function() { self.testQuery(); return false; };

    var navDocumentIdField = this.editorForms["importnavtree"].ine_docId;
    var navBranchField = this.editorForms["importnavtree"].ine_branch;
    var navLanguageField = this.editorForms["importnavtree"].ine_language;
    this.editorForms["importnavtree"].ine_lookupdocument.onclick = function() { self.documentLookup(navDocumentIdField, navBranchField, navLanguageField); return false; };

    this.hideEditors();
}

NodeEditor.prototype.getHeight = function() {
    return this.iframe.offsetHeight;
}

NodeEditor.prototype.checkChangesApplied = function() {
    if (this.treeNode != null) {
        var nodeType = this.treeNode.nodeType;
        var state = this.treeNode.getState();
        var form = this.editorForms[nodeType.name];
        var changes = false;
        if (nodeType == SECTION_NODE_TYPE) {
            var document = this.iframe.contentWindow.document;
            if (document.getElementById("documentMode").checked) {
                if (state.documentId == null) {
                    changes = true;
                } else {
                    if (state.documentId != this.normalize(form["sne_id"].value)
                            || state.branch != this.normalize(form["sne_branch"].value)
                            || state.language != this.normalize(form["sne_language"].value)
                            || state.version != this.normalize(form["sne_version"].value)
                            || state.type != this.normalize(form["sne_sectiontype"].value)
                            || state.navlabel != this.normalize(form["sne_navlabel"].value))
                        changes = true;
                }
            } else if (document.getElementById("titleMode").checked) {
                if (state.title == null) {
                    changes = true;
                } else {
                    if (state.title != this.normalize(form["sne_title"].value) || state.type != this.normalize(form["sne_title_sectiontype"].value))
                        changes = true;
                }
            } else if (document.getElementById("shiftMode").checked) {
                if (!state.nothing)
                    changes = true;
            }
        } else if (nodeType == QUERY_NODE_TYPE) {
            if (state.query != this.normalize(form["qne_query"].value)
                    || state.sectionType != this.normalize(form["qne_sectiontype"].value)
                    || state.filterVariants != form["qne_filtervariants"].checked)
                changes = true;
        } else if (nodeType == IMPORTNAVTREE_NODE_TYPE) {
            if (state.documentId != this.normalize(form["ine_docId"].value)
                    || state.branch != this.normalize(form["ine_branch"].value)
                    || state.language != this.normalize(form["ine_language"].value)
                    || state.path != this.normalize(form["ine_path"].value))
                changes = true;
        }
        if (changes) {
            this.apply();
        }
    }
}

NodeEditor.prototype.normalize = function(value) {
    if (value == "")
        return null;
    else if (value == null)
        return null;
    else
        return value.replace(/^\s+|\s+$/g, ''); // trim text
}

NodeEditor.prototype.activeNodeChanged = function(event) {
    // first check if the user didn't forget to apply changes
    this.checkChangesApplied();

    this.treeNode = event.treeNode;

    this.hideEditors();

    if (this.treeNode != null) {
        var state = this.treeNode.getState();
        var nodeType = this.treeNode.nodeType;

        if (nodeType == SECTION_NODE_TYPE) {
            var sectionForm = this.editorForms[nodeType.name];

            // First clear all fields
            sectionForm["sne_id"].value = "";
            sectionForm["sne_doc_title"].value = "";
            sectionForm["sne_branch"].value = "";
            sectionForm["sne_language"].value = "";
            sectionForm["sne_version"].value = "";
            sectionForm["sne_sectiontype"].value = "";
            sectionForm["sne_title"].value = "";
            sectionForm["sne_title_sectiontype"].value = "";

            if (state.documentId != null) {
                

                    sectionForm.documentMode.checked = true;
                    this.showSectionEditor("documentMode");
                    sectionForm["sne_id"].value = state.documentId != null ? state.documentId : "";
                    sectionForm["sne_doc_title"].value = state.title != null ? state.title : "";
                    sectionForm["sne_branch"].value = state.branch != null ? state.branch : "";
                    sectionForm["sne_language"].value = state.language != null ? state.language : "";
                    sectionForm["sne_version"].value = state.version != null ? state.version : "";
                    sectionForm["sne_sectiontype"].value = state.type != null ? state.type : "";
                    sectionForm["sne_navlabel"].value = state.navlabel != null ? state.navlabel : "";
 
                
            } else if (state.title != null) {
                sectionForm.titleMode.checked = true;
                this.showSectionEditor("titleMode");
                sectionForm["sne_title"].value = state.title != null ? state.title : "";
                sectionForm["sne_title_sectiontype"].value = state.type != null ? state.type : "";
            } else if (state.nothing){
                sectionForm.shiftMode.checked = true;
                this.showSectionEditor("shiftMode");
            } else {
                // New node: active documentMode as default
                sectionForm.documentMode.checked = true;
                this.showSectionEditor("documentMode");
            }
        } else if (nodeType == QUERY_NODE_TYPE) {
            var queryForm = this.editorForms[nodeType.name];

            queryForm["qne_query"].value = state.query != null ? state.query : "";
            queryForm["qne_sectiontype"].value = state.sectionType != null ? state.sectionType : "";
            queryForm["qne_filtervariants"].value = state.filterVariants != null ? state.filterVariants == "true" : true;
        } else if (nodeType == IMPORTNAVTREE_NODE_TYPE) {
            var importNavTreeForm = this.editorForms[nodeType.name];
            importNavTreeForm["ine_docId"].value = state.documentId != null ? state.documentId : "";
            importNavTreeForm["ine_branch"].value = state.branch != null ? state.branch : "";
            importNavTreeForm["ine_language"].value = state.language != null ? state.language : "";
            importNavTreeForm["ine_path"].value = state.path != null ? state.path : "";
        }

        this.editorDivs[nodeType.name].style.display = "";
    }
}

NodeEditor.prototype.showSectionEditor = function(name) {
    var document = this.iframe.contentWindow.document;
    document.getElementById("documentModeParams").style.display = name == "documentMode" ? "" : "none";
    document.getElementById("titleModeParams").style.display = name == "titleMode" ? "" : "none";
    document.getElementById("shiftModeParams").style.display = name == "shiftMode" ? "" : "none";
}

NodeEditor.prototype.arrayContains = function(anArray, value) {
    for (var i = 0; i < anArray.length; i++) {
        if (anArray[i] == value)
            return true;
    }
    return false;
}

NodeEditor.prototype.hideEditors = function() {
    for (var editorDiv in this.editorDivs) {
        this.editorDivs[editorDiv].style.display = "none";
    }
}

NodeEditor.prototype.hide = function() {
    this.hideEditors();
    this.iframe.style.display = "none";
}

NodeEditor.prototype.show = function() {
    this.iframe.style.display = "";
}

NodeEditor.prototype.apply = function() {
    var nodeType = this.treeNode.nodeType;
    var form = this.editorForms[nodeType.name];

    var state = new Object();
    if (nodeType == SECTION_NODE_TYPE) {
        var document = this.iframe.contentWindow.document;
        if (document.getElementById("documentMode").checked) {
            if (this.normalize(form["sne_id"].value) != null)
                state.documentId = this.normalize(form["sne_id"].value);
            if (this.normalize(form["sne_doc_title"].value) != null)
                state.title = this.normalize(form["sne_doc_title"].value);
            if (this.normalize(form["sne_branch"].value) != null)
                state.branch = this.normalize(form["sne_branch"].value);
            if (this.normalize(form["sne_language"].value) != null)
                state.language = this.normalize(form["sne_language"].value);
            if (this.normalize(form["sne_version"].value) != null)
                state.version = this.normalize(form["sne_version"].value);
            if (this.normalize(form["sne_sectiontype"].value) != null)
                state.type = this.normalize(form["sne_sectiontype"].value);
            if (this.normalize(form["sne_navlabel"].value) != null)
                state.navlabel = this.normalize(form["sne_navlabel"].value);            
        } else if (document.getElementById("titleMode").checked) {
            if (this.normalize(form["sne_title"].value) != null)
                state.title = this.normalize(form["sne_title"].value);
            if (this.normalize(form["sne_title_sectiontype"].value) != null)
                state.type = this.normalize(form["sne_title_sectiontype"].value);
        } else {
            state.nothing = true;
        }
    } else if (nodeType == QUERY_NODE_TYPE) {
        if (this.normalize(form["qne_query"].value) != null)
            state.query = this.normalize(form["qne_query"].value);
        if (this.normalize(form["qne_sectiontype"].value) != null)
            state.sectionType = this.normalize(form["qne_sectiontype"].value);
        state.filterVariants = form["qne_filtervariants"].checked;
    } else if (nodeType == IMPORTNAVTREE_NODE_TYPE) {
        if (this.normalize(form["ine_docId"].value) != null)
            state.documentId = this.normalize(form["ine_docId"].value);
        if (this.normalize(form["ine_branch"].value) != null)
            state.branch = this.normalize(form["ine_branch"].value);
        if (this.normalize(form["ine_language"].value) != null)
            state.language = this.normalize(form["ine_language"].value);
        if (this.normalize(form["ine_path"].value) != null)
            state.path = this.normalize(form["ine_path"].value);
    }

    this.treeNode.setState(state);

    return false;
}

NodeEditor.prototype.documentLookup = function(documentIdField, branchField, languageField, documentNameField) {
    var mountPoint = this.treeContext.daisyMountPoint;
    var siteName = this.treeContext.daisySiteName;

    daisy.dialog.popupDialog(mountPoint + "/" + siteName + "/editing/documentBrowser?branch=" + getBranchId() + "&language=" + getLanguageId(),
      function(params) {
        documentIdField.value = params.docId;
        if (params.docName != null && documentNameField != null)
            documentNameField.value = params.docName;
        branchField.value = params.branch != null ? params.branch : "";
        languageField.value = params.language != null ? params.language : "";
      }, {});
}

NodeEditor.prototype.createDocument = function(documentIdField, branchField, languageField, documentNameField) {
    var mountPoint = this.treeContext.daisyMountPoint;
    var siteName = this.treeContext.daisySiteName;

    daisy.dialog.popupDialog(mountPoint + "/" + siteName + "/editing/createPlaceholder?branch=" + getBranchId() + "&language=" + getLanguageId(),
      function(params) {
        documentIdField.value = params.docId;
        if (params.name != null && documentNameField != null)
            documentNameField.value = params.name;
        if (params.branchId != getBranchId())
            branchField.value = params.branch;
        if (params.languageId != getLanguageId())
            languageField.value = params.language;
      }, {});
}

NodeEditor.prototype.testQuery = function() {
    var query = this.editorForms["query"].qne_query.value;
    if (query == null || query == "") {
        alert(bt_i18n("nodeeditor-noquery"));
        return;
    }

    window.open(this.treeContext.daisyMountPoint + "/" + this.treeContext.daisySiteName + "/querySearch?daisyquery=" + encodeURIComponent(query));
}

NodeEditor.prototype.getNodeEditorHTML = function() {
var disabled=this.treeContext.disabled?' disabled=""':'';
var html =
'<html>' +
  '<head>' +
    '<style type="text/css">' +
      'body { margin: 0px; padding: 0px; background-color: ButtonFace } ' +
      'body, table td { font-family: helvetica, sans-serif; font-size: 76%; }' +
    '</style>' +
  '</head>' +
  '<body>' +
    '<div id="nodeEditor">' +
      '<div id="sectionNodeEditor">' +
        '<form action="" id="sectionNodeEditorForm">' +
          '<script type="text/javascript">' +
          '  function showSectionEditor(name) {' +
          '    document.getElementById("documentModeParams").style.display = name == "documentMode" ? "" : "none";' +
          '    document.getElementById("titleModeParams").style.display = name == "titleMode" ? "" : "none";' +
          '    document.getElementById("shiftModeParams").style.display = name == "shiftMode" ? "" : "none";' +
          '  }' +
          '</script>' +
          '<input'+disabled+' type="radio" name="mode" id="documentMode" onclick="showSectionEditor(\'documentMode\');"/><label for="documentMode">' + bt_i18n('nodeeditor-sne-document-reference') + '</label> ' +
          '<input'+disabled+' type="radio" name="mode" id="titleMode" onclick="showSectionEditor(\'titleMode\');"/><label for="titleMode">' + bt_i18n('nodeeditor-sne-title') + '</label> ' +
          '<input'+disabled+' type="radio" name="mode" id="shiftMode" onclick="showSectionEditor(\'shiftMode\');"/><label for="shiftMode">' + bt_i18n('nodeeditor-sne-shift') + '</label> ' +
          '<table width="100%" id="documentModeParams">' +
            '<tbody>' +
              '<tr>' +
                '<td><label for="dne_id">' + bt_i18n("nodeeditor-sne-docid") + '</label></td>' +
                '<td>' +
                  '<input'+disabled+' id="sne_id" name="sne_id" size="6"/>' +
                  '<input'+disabled+' type="hidden" id="sne_doc_title" name="sne_doc_title"/>' +
                  '<button'+disabled+' type="button" name="sne_lookupdocument"><img src="' + this.treeContext.resourcesPrefix + 'images/documentlookup.gif"/></button> ' +
                  '<button'+disabled+' type="button" name="sne_createdocument"><img src="' + this.treeContext.resourcesPrefix + 'images/link-to-new.gif"/></button> ' +
                  bt_i18n("nodeeditor-sne-branch") +
                  ' <input'+disabled+' id="sne_branch" name="sne_branch" size="8"/>' +
                  bt_i18n("nodeeditor-sne-language") +
                  ' <input'+disabled+' id="sne_language" name="sne_language" size="8"/>' +
                  bt_i18n("nodeeditor-sne-version") +
                  ' <input'+disabled+' id="sne_version" name="sne_version" size="8"/>' +
                '</td>' +
              '</tr>' +
              '<tr>' +
                '<td><label for="sne_sectiontype">' + bt_i18n("nodeeditor-sne-sectiontype") + '</label></td>' +
                '<td><input'+disabled+' id="sne_sectiontype" name="sne_sectiontype" size="40"/></td>' +
              '</tr>' +
              '<tr>' +
	              '<td><label for="sne_navlabel">' + bt_i18n("nodeeditor-sne-navlabel") + '</label></td>' +
	              '<td><input'+disabled+' id="sne_navlabel" name="sne_navlabel" size="40"/></td>' +
              '</tr>' +
              '<tr>' +
                '<td colspan="2">' +
                  '<input'+disabled+' type="button" value="' + bt_i18n("nodeeditor-apply-changes") + '" name="doc_apply"/>' +
                '</td>' +
              '</tr>' +
            '</tbody>' +
          '</table>' +
         '<table width="100%" id="titleModeParams" style="display: none">' +
           '<tbody>' +
             '<tr>' +
               '<td><label for="sne_title">' + bt_i18n("nodeeditor-sne-title") + '</label></td>' +
               '<td><input'+disabled+' id="sne_title" name="sne_title" size="40"/></td>' +
             '</tr>' +
             '<tr>' +
               '<td><label for="sne_title_sectiontype">' + bt_i18n("nodeeditor-sne-sectiontype") + '</label></td>' +
               '<td><input'+disabled+' id="sne_title_sectiontype" name="sne_title_sectiontype" size="40"/></td>' +
             '</tr>' +
             '<tr>' +
               '<td colspan="2">' +
                 '<input'+disabled+' type="button" value="' + bt_i18n("nodeeditor-apply-changes") + '" name="title_apply"/>' +
               '</td>' +
             '</tr>' +
           '</tbody>' +
         '</table>' +
         '<div id="shiftModeParams" style="display: none">' +
         bt_i18n("nodeeditor-sne-shift-info") +
         '</div>' +
        '</form>' +
      '</div>' +
      '<div id="queryNodeEditor">' +
        '<form action="" id="queryNodeEditorForm">' +
          '<label for="qne_query">' + bt_i18n("nodeeditor-qne-query") + '</label>' +
          '<br/>' +
          '<textarea'+disabled+' id="qne_query" name="qne_query" rows="3" style="width: 100%"></textarea>' +
          '<br/><div style="float: left"><input'+disabled+' type="button" value="' + bt_i18n("nodeeditor-apply-changes") + '" name="apply"/></div>' +
          '<div style="float: right">' +
            '<label for="qne_sectiontype">' + bt_i18n("nodeeditor-sne-sectiontype") + '</label>' +
            '<input'+disabled+' name="qne_sectiontype" id="qne_sectiontype" size="40">' +
            '<input'+disabled+' type="checkbox" id="qne_filtervariants" name="qne_filtervariants"><label for="qne_filtervariants">' +
            bt_i18n("nodeeditor-qne-filtervariants") +
            ' <button'+disabled+' type="button" name="qne_test">' + bt_i18n("nodeeditor-qne-test") + '</button>' +
          '</div>' +
        '</form>' +
      '</div>' +
      '<div id="importnavtreeNodeEditor">' +
        '<form action="" id="importnavtreeNodeEditorForm">' +
          '<label for="ine_docId">' + bt_i18n("nodeeditor-ine-navdocid") + '</label>' +
          '<input'+disabled+' id="ine_docId" name="ine_docId" size="6"/>' +
          ' <button'+disabled+' type="button" name="ine_lookupdocument"><img src="' + this.treeContext.resourcesPrefix + 'images/documentlookup.gif"/></button> ' +
          bt_i18n("nodeeditor-branch") +
          ' <input'+disabled+' id="ine_branch" name="ine_branch" size="8"/>' +
          bt_i18n("nodeeditor-language") +
          ' <input'+disabled+' id="ine_language" name="ine_language" size="8"/>' +
          '<br/>' + bt_i18n("nodeeditor-ine-path") + ' <input'+disabled+' id="ine_path" name="ine_path" size="20"/>' +
          '<br/><input'+disabled+' type="button" value="' + bt_i18n("nodeeditor-apply-changes") + '" name="apply"/>' +
        '</form>' +
      '</div>' +
      '<div id="rootNodeEditor">' +
       '<form action="" id="rootNodeEditorForm">' +
       '</form>' +
      '</div>' +
    '</div>' +
  '</body>' +
'</html>';
return html;
}
