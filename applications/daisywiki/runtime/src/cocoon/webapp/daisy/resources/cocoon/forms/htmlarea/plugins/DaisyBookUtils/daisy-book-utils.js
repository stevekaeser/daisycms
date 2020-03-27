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

/*
 * Miscellaneous extensions for htmlarea.
 */

function DaisyBookUtils(editor, params) {
    this.editor = editor;
    var cfg = editor.config;
    var self = this;
    var i18n = DaisyBookUtils.I18N;
    var plugin_config = params[0];

    cfg.registerButton("daisy-indexentry", i18n["hint.indexentry"], editor.imgURL("indexentry.gif", "DaisyBookUtils"), false,
               function(editor, id) {
                   self.indexEntry(editor, id);
               }, null);
    cfg.registerButton("daisy-footnote", i18n["hint.footnote"], editor.imgURL("footnote.gif", "DaisyBookUtils"), false,
               function(editor, id) {
                   self.footnote(editor, id);
               }, null);
    cfg.registerButton("daisy-crossreference", i18n["hint.crossreference"], editor.imgURL("crossreference.gif", "DaisyBookUtils"), false,
               function(editor, id) {
                   self.crossreference(editor, id);
               }, null);
};

DaisyBookUtils._pluginInfo = {
    name          : "DaisyBookUtils",
    version       : "1.0",
    developer     : "Outerthought",
    developer_url : "http://outerthought.org",
    c_owner       : "Outerthought",
    sponsor       : null,
    sponsor_url   : null,
    license       : "htmlArea"
};

DaisyBookUtils.prototype.indexEntry = function(editor, id) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    // check if we are currently inside an index entry element
    var element = editor.getParentElement();
    while (element != null && daisyIsInlineElement(element)) {
        if (element.tagName.toLowerCase() == "span" && element.className == "indexentry") {
            alert(DaisyBookUtils.I18N["indexentry.nested-warning"]);
            return;
        }
        element = element.parentNode;
    }

    var sel = editor._getSelection();
    var range = editor._createRange(sel);
    var selectionType = daisyCheckInlineRange(range, editor._doc);
    if (selectionType == 3) { // selection contains non-inline tags
        alert(DaisyBookUtils.I18N["indexentry.selection-warning"]);
        return;
    }

    if (selectionType == 1) { // selection is empty
        var entry = window.prompt(DaisyBookUtils.I18N["indexentry.prompt"], "");
        if (entry == null || entry == "")
            return;
        var footnoteNode = editor._doc.createElement("span");
        footnoteNode.setAttribute("class", "indexentry");
        var textNode = editor._doc.createTextNode(entry);
        footnoteNode.appendChild(textNode);
        daisyInsertNode(footnoteNode, editor);
    } else {
        editor.surroundHTML("<span class='indexentry'>", "</span>");
    }

    daisyClearSelection(editor);
}

DaisyBookUtils.prototype.footnote = function(editor, id) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    // Test that we are currently not inside a footnote -- nested footnotes make no sense
    var element = editor.getParentElement();
    while (element != null && daisyIsInlineElement(element)) {
        if (element.tagName.toLowerCase() == "span" && element.className == "footnote") {
            alert(DaisyBookUtils.I18N["footnote.nested-warning"]);
            return;
        }
        element = element.parentNode;
    }

    var sel = editor._getSelection();
    var range = editor._createRange(sel);
    var selectionType = daisyCheckInlineRange(range, editor._doc);
    if (selectionType == 3) { // selection contains non-inline tags
        alert("footnote.selection-warning");
        return;
    }

    if (selectionType == 1) { // selection is empty
        var footnoteNode = editor._doc.createElement("span");
        footnoteNode.setAttribute("class", "footnote");
        var textNode = editor._doc.createTextNode(DaisyBookUtils.I18N["footnote.placeholdertext"]);
        footnoteNode.appendChild(textNode);
        daisyInsertNode(footnoteNode, editor);
    } else {
        editor.surroundHTML("<span class='footnote'>", "</span>");
    }

    daisyClearSelection(editor);
}

DaisyBookUtils.prototype.crossreference = function(editor, id) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    var currentReference = null;
    var element = editor.getParentElement();
    while (element != null && daisyIsInlineElement(element)) {
        if (element.tagName.toLowerCase() == "span" && element.className == "crossreference") {
            currentReference = daisyElementText(element);
            break;
        }
        element = element.parentNode;
    }

    var dialogParams = new Object();
    if (currentReference != null) {
        dialogParams.crossref = currentReference;
    }
    dialogParams.editor = editor;
    dialogParams.branchId = getBranchId();
    dialogParams.languageId = getLanguageId();

    daisy.dialog.popupDialog(daisyGetHtmlAreaDialog("DaisyBookUtils", "crossref.html"), function(param) {
        if (!param) {    // user must have pressed Cancel
            return false;
        }
        var crossref = param.crossref;

        if (currentReference != null) {
            var children = element.childNodes;
            for (var i = children.length - 1; i >= 0; i--) {
                element.removeChild(children[i]);
            }
            var textNode = editor._doc.createTextNode(crossref);
            element.appendChild(textNode);
        } else {
            var crossrefNode = editor._doc.createElement("span");
            crossrefNode.setAttribute("class", "crossreference");
            var textNode = editor._doc.createTextNode(crossref);
            crossrefNode.appendChild(textNode);
            daisyInsertNode(crossrefNode, editor);
        }
        daisyClearSelection(editor);
    }, dialogParams);
}

