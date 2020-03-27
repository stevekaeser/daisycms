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
// NodeEditor class for the Selection List editor
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

    var nodeNames = ["listitem", "root"];
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

    // load list of locales
    this.initLocaleSelect();
    this.loadLocaleValue();

    this.editorForms["listitem"].li_locale.onchange = function() { return self.selectedLocaleChanged() };
    this.editorForms["listitem"].apply.onclick = function() { return self.apply() };

    this.iframe.style.height = maxHeight + 15 + "px"; // the 15 is an arbitrary margin

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
        if (nodeType == LISTITEM_NODE_TYPE) {
            if (state.value != this.normalize(form["li_value"].value))
                changes = true;

            // check the labels are equal
            this.storeLocaleValue();
            var labels = form["li_locale_value"].daisyLabels;
            if (state.labels == null && labels.getKeyList().length == 0) {
                // is equal
            } else if (state.labels == null) {
                changes = true;
            } else if (!state.labels.equals(labels)) {
                changes = true;
            }
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
    else {
        value = value.replace(/^\s+|\s+$/g, ''); // trim text
        if (value == "")
            return null;
        return value;
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

        if (nodeType == LISTITEM_NODE_TYPE) {
            var listItemForm = this.editorForms[nodeType.name];
            listItemForm["li_value"].value = state.value != null ? state.value : "";
            listItemForm["li_locale_value"].daisyLabels = state.labels != null ? state.labels.clone() : new DaisyDictionary();
            this.initLocaleSelect();
            this.loadLocaleValue();
        }

        this.editorDivs[nodeType.name].style.display = "";
    }
}

/**
 * Loads the entries in the locale dropdown selection list, which is the combination
 * of the predefined "admin locales" and the actual locales available on the current node.
 */
NodeEditor.prototype.initLocaleSelect = function() {
    var localeInput = this.editorForms["listitem"].li_locale_value;
    var labels = localeInput.daisyLabels;
    if (labels == null)
        labels = new DaisyDictionary();
    var adminLocales = getAdminLocales();
    var localeList = this.getJoinedLocaleList(labels, adminLocales);

    var localeSelect = this.editorForms["listitem"].li_locale;
    // We want the same locale to stay selected when switching between nodes (if possible)
    var previousSelected = localeSelect.selectedIndex >= 0 ? localeSelect.options[localeSelect.selectedIndex].value : "";

    var anySelected = false;
    localeSelect.options.length = 0;
    for (var i = 0; i < localeList.length; i++) {
        var selected = localeList[i] == previousSelected;
        if (selected)
            anySelected = true;
        localeSelect.options[i] = new Option(localeList[i] == "" ? "(default)" : localeList[i], localeList[i], false, selected);
    }

    //  Make sure one option is marked as selected, otherwise loadLocaleValue() will not work
    if (!anySelected)
        localeSelect.options[0].selected = true;
}

/**
 * Returns an array which is the combination of the locales available in the given
 * dictionary object and the given array.
 */
NodeEditor.prototype.getJoinedLocaleList = function(localeDict, adminLocales) {
    var result = [].concat(adminLocales); // make a clone
    var locales = localeDict.getKeyList();
    for (var i = 0; i < locales.length; i++) {
        if (!this.arrayContains(result, locales[i])) {
            result.push(locales[i]);
        }
    }
    result.sort();
    return result;
}

NodeEditor.prototype.loadLocaleValue = function() {
    var localeSelect = this.editorForms["listitem"].li_locale;
    var selectedLocale = localeSelect.options[localeSelect.selectedIndex].value;
    var localeInput = this.editorForms["listitem"].li_locale_value;
    var labels = localeInput.daisyLabels;
    var label = labels != null ? labels.item(selectedLocale) : null;
    if (label == null)
        label = "";
    localeInput.value = label;
    localeInput.daisyLocale = selectedLocale;
}

NodeEditor.prototype.storeLocaleValue = function() {
    var localeInput = this.editorForms["listitem"].li_locale_value;
    var labels = localeInput.daisyLabels;
    var locale = localeInput.daisyLocale;
    var label = this.normalize(localeInput.value);
    if (label == null)
        labels.remove(locale);
    else
        labels.add(locale, label);
}

NodeEditor.prototype.selectedLocaleChanged = function() {
    this.storeLocaleValue();
    this.loadLocaleValue();
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
    if (nodeType == LISTITEM_NODE_TYPE) {
        if (this.normalize(form["li_value"].value) != null)
            state.value = this.normalize(form["li_value"].value);
        this.storeLocaleValue();
        state.labels = form["li_locale_value"].daisyLabels.clone();
    }

    this.treeNode.setState(state);

    return false;
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
      '<div id="listitemNodeEditor">' +
        '<form action="" id="listitemNodeEditorForm">' +
          '<table width="100%" border="0">' +
          '<tr>' +
            '<td><label for="li_value">' + bt_i18n("nodeeditor-li-value") + '</label></td>' +
            '<td><input name="li_value" id="li_value" size="40"'+ disabled + '></td>' +
          '</tr>' +
          '<tr>' +
            '<td>' + bt_i18n("nodeeditor-li-label") + '</td><td><select name="li_locale" id="li_locale"></select> <input name="li_locale_value" id="li_locale_value" size="30"'+disabled+'/></td>' +
          '</tr>' +
          '<tr><td colspan="2"><input type="button" value="' + bt_i18n("nodeeditor-apply-changes") + '" name="apply"'+disabled+'/></td></tr>' +
          '</table>' +
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