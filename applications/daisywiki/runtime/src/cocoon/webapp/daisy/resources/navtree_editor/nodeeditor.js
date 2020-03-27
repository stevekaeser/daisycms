
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
// NodeEditor class for the navigation editor
//

dojo.require("daisy.html");

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

    for (var nodeName in this.nodeInfos) {
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

        if (nodeEditorForm.apply)
            nodeEditorForm.apply.onclick = function() { return self.apply() };
    }

    this.iframe.style.height = maxHeight + 5 + "px"; // the 5 is an arbitrary margin

    var documentIdField = this.editorForms["document"].dne_id;
    var documentBranchField = this.editorForms["document"].dne_branch;
    var documentLanguageField = this.editorForms["document"].dne_language;
    var documentNameField = this.editorForms["document"].dne_label;
    this.editorForms["document"].dne_lookupdocument.onclick = function() { self.documentLookup(documentIdField, documentBranchField, documentLanguageField, documentNameField); return false; };
    this.editorForms["document"].dne_createdocument.onclick = function() { self.createDocument(documentIdField, documentBranchField, documentLanguageField, documentNameField); return false; };
    var importIdField = this.editorForms["import"].ine_docId;
    var importBranchField = this.editorForms["import"].ine_branch;
    var importLanguageField = this.editorForms["import"].ine_language;
    this.editorForms["import"].ine_lookupdocument.onclick = function() { self.documentLookup(importIdField, importBranchField, importLanguageField); return false; };
    this.editorForms["query"].qne_test.onclick = function() { self.testQuery(); return false; };
    this.editorForms["query"].qne_add_col.onclick = function() {
        var columns = this.form["qne_columns"].columns;
        columns[columns.length] = new Object();
        self.loadQueryColumnInfo(this.form["qne_columns"].options.length);
        return false;
    };
    this.editorForms["query"].qne_delete_col.onclick = function() {
        var columnSelect = this.form["qne_columns"];
        var columnToRemove = columnSelect.selectedIndex;

        var columns = this.form["qne_columns"].columns;
        columns.splice(columnToRemove, 1);

        self.loadQueryColumnInfo(columnToRemove);
        return false;
    };
    this.editorForms["query"].qne_columns.onchange = function() {
        self.loadQueryColumnInfo(this.form["qne_columns"].selectedIndex);
    };
    this.editorForms["query"].qne_col_sort.onchange = function() {
        var sortOrder = daisy.html.selectedOptionValue(this.form["qne_col_sort"]);
        var columns = this.form["qne_columns"].columns;
        columns[this.form["qne_columns"].selectedIndex].sortOrder = sortOrder;
    };
    this.editorForms["query"].qne_col_visibility.onchange = function() {
        var visibility = daisy.html.selectedOptionValue(this.form["qne_col_visibility"]);
        var columns = this.form["qne_columns"].columns;
        columns[this.form["qne_columns"].selectedIndex].visibility = visibility;
    };

    var useSelectValuesSelect = this.editorForms["query"].qne_selectvalues;
    useSelectValuesSelect.options[0] = new Option(nt_i18n("nodeeditor-qne-useselect-all"), "all");
    for (var i = 0; i < 51; i++) {
        useSelectValuesSelect.options[useSelectValuesSelect.length] = new Option(i, i);
    }

    var aclDocumentIdField = this.editorForms["link"].lne_acldocid;
    var aclBranchField = this.editorForms["link"].lne_aclbranch;
    var aclLanguageField = this.editorForms["link"].lne_acllanguage;
    this.editorForms["link"].lne_lookupdocument.onclick = function() { self.documentLookup(aclDocumentIdField, aclBranchField, aclLanguageField); return false; };
    this.editorForms["link"].lne_acl_this.onclick = function() { aclDocumentIdField.value = "this"; self.selectCorrectInheritAclDoc(); return true; };
    this.editorForms["link"].lne_acl_other.onclick = function() { aclDocumentIdField.value = ""; self.selectCorrectInheritAclDoc(); return true; };


    this.hideEditors();
}

NodeEditor.prototype.getHeight = function() {
    return this.iframe.offsetHeight;
}

// Note: the property names in this object must correspond to the "name" property of the XXX_NODE_TYPE objects
NodeEditor.prototype.nodeInfos =
    {
    "document":   [ new NodeEditorFieldInfo("dne_id",        "documentId", true),
                    new NodeEditorFieldInfo("dne_branch",    "branch", true),
                    new NodeEditorFieldInfo("dne_language",  "language", true),
                    new NodeEditorFieldInfo("dne_label",     "label"),
                    new NodeEditorFieldInfo("dne_nodeid",    "nodeId", true),
                    new NodeEditorFieldInfo("dne_visibility","visibility", true, "always")],
    "query":      [ new NodeEditorFieldInfo("qne_query",     "query"),
                    new NodeEditorFieldInfo("qne_filtervariants", "filterVariants"),
                    new NodeEditorFieldInfo("qne_visibility","visibility", true, "always"),
                    new NodeEditorFieldInfo("qne_selectvalues","useSelectValues", true, "all")],
    "group":      [ new NodeEditorFieldInfo("gne_id",        "id", true),
                    new NodeEditorFieldInfo("gne_label",     "label"),
                    new NodeEditorFieldInfo("gne_visibility","visibility", true, "always")],
    "link":       [ new NodeEditorFieldInfo("lne_url",       "url"),
                    new NodeEditorFieldInfo("lne_label",     "label"),
                    new NodeEditorFieldInfo("lne_id",        "id", true),
                    new NodeEditorFieldInfo("lne_acldocid",  "inheritAclDocId", true),
                    new NodeEditorFieldInfo("lne_aclbranch", "inheritAclBranch", true),
                    new NodeEditorFieldInfo("lne_acllanguage", "inheritAclLanguage", true)],
    "import":     [ new NodeEditorFieldInfo("ine_docId",     "docId", true),
                    new NodeEditorFieldInfo("ine_branch",    "branch", true),
                    new NodeEditorFieldInfo("ine_language",  "language", true)],
    "separator":  [],
    "root":       []
    };

function NodeEditorFieldInfo(formField, stateProperty, trim, defaultValue) {
    this.formField = formField;
    this.stateProperty = stateProperty;
    this.trim = trim != null ? trim : false;
    this.defaultValue = defaultValue;
}

NodeEditor.prototype.checkChangesApplied = function() {
    if (this.treeNode != null) {
        var nodeType = this.treeNode.nodeType;
        var nodeInfo = this.nodeInfos[nodeType.name];
        if (nodeInfo != null) {
            var state = this.treeNode.getState();
            var form = this.editorForms[nodeType.name];
            var changes = false;
            for (var i = 0; i < nodeInfo.length; i++) {
                var field = form[nodeInfo[i].formField];
                var fieldValue;
                if (field.type == "checkbox") {
                    fieldValue = field.checked;
                } else if (field.tagName == "SELECT") {
                    fieldValue = field.options[field.selectedIndex].value;
                } else {
                    fieldValue = field.value;
                    if (fieldValue != null && nodeInfo[i].trim)
                        fieldValue = fieldValue.replace(/^\s+|\s+$/g, ''); // = trim text
                    if (fieldValue == "") fieldValue = null;
                }
                if (fieldValue != state[nodeInfo[i].stateProperty]
                        && (nodeInfo[i].defaultValue == null || nodeInfo[i].defaultValue != fieldValue)) {
                    changes = true;
                    break;
                }
            }

            // special handling for query node
            if (!changes && nodeType == QUERY_NODE_TYPE) {
                if (!this.nullSafeArraysEqual(state.columns, this.editorForms[nodeType.name]["qne_columns"].columns,
                        function(el1, el2) { return el1.sortOrder == el2.sortOrder && el1.visibility == el2.visibility})) {
                    changes = true;
                }
            }

            // special handling for the root node
            if (!changes && nodeType == ROOT_NODE_TYPE) {
                // check if there are changes to the collections
                var collections = state.collections;
                if (collections == null)
                    collections = new Array();
                var collectionsSelect = this.editorForms[nodeType.name]["rne_selectedCollections"];
                if (collections.length != collectionsSelect.options.length) {
                    changes = true;
                } else {
                    for (var i = 0; i < collectionsSelect.options.length; i++) {
                        if (!this.arrayContains(collections, collectionsSelect.options[i].value)) {
                            changes = true;
                            break;
                        }
                    }
                }
            }
            if (changes) {
                this.apply();
            }
        }
    }
}

NodeEditor.prototype.activeNodeChanged = function(event) {
    // first check if the user didn't forget to apply changes
    this.checkChangesApplied();

    this.treeNode = event.treeNode;

    this.hideEditors();

    if (this.treeNode != null) {
        var state = this.treeNode.getState();
        var nodeType = this.treeNode.nodeType;
        var nodeInfo = this.nodeInfos[nodeType.name];

        if (nodeInfo != null) {
            this.editorDivs[nodeType.name].style.display = "";
            for (var i = 0; i < nodeInfo.length; i++) {
                var stateValue = state[nodeInfo[i].stateProperty];
                var field = this.editorForms[nodeType.name][nodeInfo[i].formField];
                if (field.type == "checkbox") {
                    field.checked = stateValue;
                } else if (field.tagName == "SELECT") {
                    field.selectedIndex = 0;
                    for (var o = 0; o < field.options.length; o++) {
                        if (field.options[o].value == stateValue) {
                            field.selectedIndex = o;
                            break;
                        }
                    }
                } else {
                    field.value = stateValue != null ? stateValue : "";
                }
            }
        }

        if (nodeType == QUERY_NODE_TYPE) {
            var columns = state.columns != null ? this.cloneColumns(state.columns) : [];
            this.editorForms[nodeType.name]["qne_columns"].columns = columns;
            this.loadQueryColumnInfo();
        }

        if (nodeType == LINK_NODE_TYPE) {
            this.selectCorrectInheritAclDoc();
        }

        // Special handling for root node. On the root node we allow to change the collections
        if (nodeType == ROOT_NODE_TYPE) {
            var rootState = this.treeNode.getState();
            var collections = rootState.collections;
            if (collections == null)
                collections = new Array();
            var rootForm = this.editorForms[nodeType.name];

            // fill list of available collections
            var availableSelect = rootForm["rne_availableCollections"];
            availableSelect.options.length = 0;
            var availableCollections = this.treeContext.documentCollectionNames;
            for (var i = 0; i < availableCollections.length; i++) {
                if (!this.arrayContains(collections, availableCollections[i]))
                    availableSelect.options[availableSelect.options.length] = new Option(availableCollections[i], availableCollections[i]);
            }

            // fill list of selected collections
            var selectedSelect = rootForm["rne_selectedCollections"];
            selectedSelect.options.length = 0;
            for (var i = 0; i < collections.length; i++) {
                selectedSelect.options[i] = new Option(collections[i], collections[i]);
            }

            // initialiase option transfer functions
            var optionTransfer = new OptionTransfer("rne_availableCollections", "rne_selectedCollections");
            optionTransfer.setAutoSort(true);
            optionTransfer.init(rootForm);
            rootForm.rne_moveLeft.onclick = function() { optionTransfer.transferLeft(); return false; };
            rootForm.rne_moveRight.onclick = function() { optionTransfer.transferRight(); return false; };
            availableSelect.ondblclick = function() { optionTransfer.transferRight(); return false; };
            selectedSelect.ondblclick = function() { optionTransfer.transferLeft(); return false; };
        }
    }
}

/**
 * Load the information from the columns array (stored on qne_columns)
 * into the HTML elements, making the column specified in the 'selected'
 * parameter the default.
 */
NodeEditor.prototype.loadQueryColumnInfo = function(selected) {
    var form = this.editorForms["query"];
    var columns = form["qne_columns"].columns;
    var columnOptions = form["qne_columns"].options;
    columnOptions.length = 0;

    for (var i = 0; i < columns.length; i++) {
        var newColumn = i + 1;
        columnOptions[i] = new Option(newColumn, newColumn);
    }

    form["qne_col_sort"].selectedIndex = 0;
    form["qne_col_visibility"].selectedIndex = 0;

    if (columns.length == 0) {
        form["qne_columns"].disabled = true;
        form["qne_col_sort"].disabled = true;
        form["qne_col_visibility"].disabled = true;
        form["qne_delete_col"].disabled = true;
        return;
    }

    form["qne_columns"].disabled = false;
    form["qne_col_sort"].disabled = false;
    form["qne_col_visibility"].disabled = false;
    form["qne_delete_col"].disabled = false;

    if (selected == null || selected < 0) {
        selected = 0;
    } else if (selected > columns.length - 1) {
        selected = columns.length -1;
    }

    columnOptions[selected].selected = true;

    var sortOrder = columns[selected].sortOrder;
    var visibility = columns[selected].visibility;

    if (sortOrder == null)
        sortOrder = "none";

    if (visibility == null)
        visibility = "";

    daisy.html.selectOption(form["qne_col_sort"], sortOrder);
    daisy.html.selectOption(form["qne_col_visibility"], visibility);
}

NodeEditor.prototype.cloneColumns = function(columns) {
    var newColumns = new Array(columns.length);
    for (var i = 0; i < columns.length; i++) {
        newColumns[i] = new Object();
        newColumns[i].sortOrder = columns[i].sortOrder;
        newColumns[i].visibility = columns[i].visibility;
    }
    return newColumns;
}

NodeEditor.prototype.selectCorrectInheritAclDoc = function() {
    var form = this.editorForms["link"];
    var docId = form.lne_acldocid.value;
    if (docId == "this") {
        form.lne_acl_this.checked = true;
        form.lne_acldocid.disabled = true;
        form.lne_lookupdocument.disabled = true;
        form.lne_aclbranch.disabled = true;
        form.lne_acllanguage.disabled = true;
        // doesn't signify anything to specify branch and language when id is "this"
        form.lne_aclbranch.value = "";
        form.lne_acllanguage.value = "";
    } else {
        form.lne_acl_other.checked = true;
        form.lne_acldocid.disabled = false;
        form.lne_lookupdocument.disabled = false;
        form.lne_aclbranch.disabled = false;
        form.lne_acllanguage.disabled = false;
    }
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
    var nodeInfo = this.nodeInfos[nodeType.name];

    var state = new Object();
    for (var i = 0; i < nodeInfo.length; i++) {
        var field = this.editorForms[nodeType.name][nodeInfo[i].formField];
        if (field.type == "checkbox") {
            state[nodeInfo[i].stateProperty] = field.checked;
        } else if (field.tagName == "SELECT") {
            state[nodeInfo[i].stateProperty] = field.options[field.selectedIndex].value;
        } else {
            var fieldValue = field.value;
            if (fieldValue != null && nodeInfo[i].trim)
                fieldValue = fieldValue.replace(/^\s+|\s+$/g, ''); // = trim text
            if (fieldValue != null && fieldValue != "")
                state[nodeInfo[i].stateProperty] = fieldValue;
        }
    }

    // special handling for column settings
    if (nodeType == QUERY_NODE_TYPE) {
        state.columns = this.editorForms[nodeType.name]["qne_columns"].columns;
    }

    // special handling for the root node
    if (nodeType == ROOT_NODE_TYPE) {
        var rootForm = this.editorForms[nodeType.name];
        var selectedSelect = rootForm["rne_selectedCollections"];
        var collections = new Array();
        for (var i = 0; i < selectedSelect.options.length; i++)
            collections[i] = selectedSelect.options[i].value;
        state.collections = collections;
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
        alert(nt_i18n("nodeeditor-noquery"));
        return;
    }

    window.open(this.treeContext.daisyMountPoint + "/" + this.treeContext.daisySiteName + "/querySearch?daisyquery=" + encodeURIComponent(query));
}

/*
 * Checks if two arrays are equal.
 */
NodeEditor.prototype.nullSafeArraysEqual = function(array1, array2, compareFunction) {
    if (array1 == null && array2 == null)
        return true;

    if (array1 == null || array2 == null)
        return false;

    if (array1.length != array2.length)
        return false;

    for (var i = 0; i < array1.length; i++) {
        if (!compareFunction(array1[i], array2[i]))
            return false;
    }

    return true;
}

/**
 * Checks if the value occurs in the given array (identity test).
 */
NodeEditor.prototype.arrayContains = function(anArray, value) {
    for (var i = 0; i < anArray.length; i++) {
        if (anArray[i] == value)
            return true;
    }
    return false;
}

NodeEditor.prototype.getNodeEditorHTML = function() {
var html =
'<html>' +
  '<head>' +
    '<style type="text/css">' +
      'body { margin: 2px 2px; padding: 0px; background-color: ButtonFace } ' +
      'body, table td { font-family: helvetica, sans-serif; font-size: 76%; }' +
    '</style>' +
  '</head>' +
  '<body>' +
    '<div id="nodeEditor">' +
      '<div id="documentNodeEditor">' +
        '<form action="" id="documentNodeEditorForm">' +
          '<table width="100%">' +
            '<tbody>' +
              '<tr>' +
                '<td><label for="dne_id">' + nt_i18n("nodeeditor-dne-docid") + '</label></td>' +
                '<td>' +
                  '<input id="dne_id" name="dne_id" size="6"/>' +
                  '<button type="button" name="dne_lookupdocument"><img src="' + this.treeContext.resourcesPrefix + 'images/documentlookup.gif"/></button> ' +
                  '<button type="button" name="dne_createdocument"><img src="' + this.treeContext.resourcesPrefix + 'images/link-to-new.gif"/></button> ' +
                  nt_i18n("nodeeditor-branch") +
                  ' <input id="dne_branch" name="dne_branch" size="8"/> ' +
                  nt_i18n("nodeeditor-language") +
                  ' <input id="dne_language" name="dne_language" size="8"/>' +
                '</td>' +
              '</tr>' +
              '<tr>' +
                '<td><label for="dne_label">' + nt_i18n("nodeeditor-dne-label") + '</label></td>' +
                '<td><input id="dne_label" name="dne_label" size="40"/>&#160;&#160;' +
                nt_i18n("nodeeditor-visibility") + this.getVisibilityHtml("dne_visibility") + '</td>' +
              '</tr>' +
              '<tr>' +
                '<td><label for="dne_nodeid">' + nt_i18n("nodeeditor-dne-nodeid") + '</label></td>' +
                '<td><input id="dne_nodeid" name="dne_nodeid" size="40"/> ' + nt_i18n("nodeeditor-dne-nodeidinfo") + '</td>' +
              '</tr>' +
              '<tr>' +
                '<td colspan="2">' +
                  '<input type="button" value="' + nt_i18n("nodeeditor-apply-changes") + '" name="apply"/>' +
                '</td>' +
              '</tr>' +
            '</tbody>' +
          '</table>' +
        '</form>' +
      '</div>' +
      '<div id="groupNodeEditor">' +
        '<form action="" id="groupNodeEditorForm">' +
          '<table>' +
            '<tbody>' +
              '<tr>' +
                '<td><label for="gne_label">' + nt_i18n("nodeeditor-gne-label") + '</label></td>' +
                '<td><input id="gne_label" name="gne_label" size="40"/></td>' +
              '</tr>' +
              '<tr>' +
                '<td><label for="gne_id">' + nt_i18n("nodeeditor-gne-id") + '</label></td>' +
                '<td><input id="gne_id" name="gne_id" size="40"/></td>' +
              '</tr>' +
              '<tr>' +
                '<td>' + nt_i18n("nodeeditor-visibility") + '</td>' +
                '<td>' + this.getVisibilityHtml('gne_visibility') + '</td>' +
              '</tr>' +
              '<tr>' +
                '<td colspan="2">' +
                  '<input type="button" value="' + nt_i18n("nodeeditor-apply-changes") + '" name="apply"/>' +
                '</td>' +
              '</tr>' +
            '</tbody>' +
          '</table>' +
        '</form>' +
      '</div>' +
      '<div id="queryNodeEditor">' +
        '<form action="" id="queryNodeEditorForm">' +
          '<label for="qne_query">' + nt_i18n("nodeeditor-qne-query") + '</label>' +
          '<br/>' +
          '<textarea id="qne_query" name="qne_query" rows="2" style="width: 100%"></textarea>' +
          '<table style="margin-top: 3px; margin-bottom: 3px; width: 100%">' +
            '<tr>' +
              '<td>' +
                nt_i18n("nodeeditor-qne-column") +
                ' <select id="qne_columns" name="qne_columns"></select> <button type="button" name="qne_add_col">' +
                nt_i18n("nodeeditor-qne-add-column") + '</button>&nbsp;<button type="button" name="qne_delete_col">' +
                nt_i18n("nodeeditor-qne-delete-column") +
                '</button> '+ nt_i18n("nodeeditor-qne-sort") + ' ' + this.getSortOrderHtml("qne_col_sort") + ' ' + nt_i18n("nodeeditor-visibility") +
                ' ' + this.getVisibilityHtml('qne_col_visibility', true) +
              '</td>' +
              '<td style="text-align: right">' +
                nt_i18n("nodeeditor-qne-useselect") + ' ' +
                '<select id="qne_selectvalues" name="qne_selectvalues"> </select>' +
              '</td>' +
            '</tr>' +
          '</table>' +
          '<div style="float: left"><input type="button" value="' + nt_i18n("nodeeditor-apply-changes") + '" name="apply"/></div>' +
          '<div style="float: right">' +
            nt_i18n("nodeeditor-qne-default-visiblity") + this.getVisibilityHtml('qne_visibility') +
            '<input type="checkbox" id="qne_filtervariants" name="qne_filtervariants"><label for="qne_filtervariants">' +
            nt_i18n("nodeeditor-qne-filtervariants") +
            '&nbsp;<button type="button" name="qne_test">' + nt_i18n("nodeeditor-qne-test") + '</button>' +
          '</div>' +
        '</form>' +
      '</div>' +
      '<div id="linkNodeEditor">' +
        '<form action="" id="linkNodeEditorForm">' +
          '<label for="lne_url">' + nt_i18n("nodeeditor-lne-url") + '</label>' +
          ' <input id="lne_url" name="lne_url" size="30"/>' +
          ' <label for="lne_label">' + nt_i18n("nodeeditor-lne-label") + '</label>' +
          ' <input id="lne_label" name="lne_label" size="20"/>' +
          ' <label for="lne_id">' + nt_i18n("nodeeditor-lne-id") + '</label>' +
          ' <input id="lne_id" name="lne_id" size="10"/>' +
          '<br/>' +
          '<label for="dne_id">' + nt_i18n("nodeeditor-lne-inheritacl-from") + '</label>' +
          '<br/>&nbsp;&nbsp;<input type="radio" name="lne_acltype" id="lne_acl_this"/><label for="lne_acl_this">' + nt_i18n("nodeeditor-lne-inheritacl-this") + '</label>' +
          '<br/>&nbsp;&nbsp;<input type="radio" name="lne_acltype" id="lne_acl_other"/><label for="lne_acl_other">' + nt_i18n("nodeeditor-lne-inheritacl-other") + '</label>' +
          ' <input id="lne_acldocid" name="lne_acldocid" size="6"/> ' +
          '<button type="button" name="lne_lookupdocument"><img src="' + this.treeContext.resourcesPrefix + 'images/documentlookup.gif"/></button> ' +
          nt_i18n("nodeeditor-branch") +
          ' <input id="lne_aclbranch" name="lne_aclbranch" size="8"/> ' +
          nt_i18n("nodeeditor-language") +
          ' <input id="lne_acllanguage" name="lne_acllanguage" size="8"/>' +
          '<br/><input type="button" value="' + nt_i18n("nodeeditor-apply-changes") + '" name="apply"/>' +
        '</form>' +
      '</div>' +
      '<div id="importNodeEditor">' +
        '<form action="" id="importNodeEditorForm">' +
          '<label for="ine_docId">' + nt_i18n("nodeeditor-ine-navdocid") + '</label>' +
          '<input id="ine_docId" name="ine_docId" size="6"/>' +
          ' <button type="button" name="ine_lookupdocument"><img src="' + this.treeContext.resourcesPrefix + 'images/documentlookup.gif"/></button> ' +
          nt_i18n("nodeeditor-branch") +
          ' <input id="ine_branch" name="ine_branch" size="8"/>' +
          nt_i18n("nodeeditor-language") +
          ' <input id="ine_language" name="ine_language" size="8"/>' +
          '<br/><input type="button" value="' + nt_i18n("nodeeditor-apply-changes") + '" name="apply"/>' +
        '</form>' +
      '</div>' +
      '<div id="separatorNodeEditor">' +
        '<form id="separatorNodeEditorForm">' +
        '</form>' +
      '</div>' +
      '<div id="rootNodeEditor">' +
        '<form action="" id="rootNodeEditorForm">' +
          '<table><tbody>' +
            '<tr><td rowspan="4" width="300px">' + nt_i18n("nodeeditor-rne-collections-info") + '</td><td/><td/></tr>' +
            '<tr><td>' + nt_i18n("nodeeditor-rne-available") + '</td><td/><td>' + nt_i18n("nodeeditor-rne-selected") + '</td></tr>' +
            '<tr>' +
              '<td>' +
                '<select name="rne_availableCollections" size="4"/>' +
              '</td>' +
              '<td>' +
                '&nbsp;<button type="button" name="rne_moveLeft">&lt;</button>&nbsp;'+
                '<br/>' +
                '&nbsp;<button type="button" name="rne_moveRight">&gt;</button>&nbsp;'+
              '</td>' +
              '<td>' +
                '<select name="rne_selectedCollections" size="4"/>' +
              '</td>' +
            '</tr>' +
            '<tr><td colspan="3"><input type="button" value="' + nt_i18n("nodeeditor-apply-changes") + '" name="apply"/></td></tr>' +
          '</tbody></table>' +
        '</form>' +
      '</div>' +
    '</div>' +
  '</body>' +
'</html>';
return html;
}

NodeEditor.prototype.getVisibilityHtml = function(id, includeDefault) {
    var html =
            ' <select id="' + id + '" name="' + id + '">' +
              (includeDefault == true ? '<option value="">' + nt_i18n("visibility-default") + '</option>' : '') +
              '<option value="always">' + nt_i18n("visibility-always") + '</option>' +
              '<option value="when-active">' + nt_i18n("visibility-when-active") + '</option>' +
              '<option value="hidden">' + nt_i18n("visibility-hidden") + '</option>' +
            '</select>';
    return html;
}

NodeEditor.prototype.getSortOrderHtml = function(id) {
    var html =
            ' <select id="' + id + '" name="' + id + '">' +
              '<option value="none">' + nt_i18n("sortorder-none") + '</option>' +
              '<option value="ascending">' + nt_i18n("sortorder-ascending") + '</option>' +
              '<option value="descending">' + nt_i18n("sortorder-descending") + '</option>' +
            '</select>';
    return html;
}
