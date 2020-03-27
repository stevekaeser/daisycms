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

function DaisyMisc(editor, params) {
    this.editor = editor;
    var cfg = editor.config;
    var toolbar = cfg.toolbar;
    var self = this;
    var i18n = DaisyMisc.I18N;
    var plugin_config = params[0];

    cfg.registerButton("daisy-align-none", i18n["hint.default-alignment"], editor.imgURL("align_none.gif", "DaisyMisc"), false,
               function(editor, id) {
                   self.changeAlignment(editor, id, "none");
               }, null);
    cfg.registerButton("daisy-align-left", i18n["hint.align-left"], editor.imgURL("ed_align_left.gif", "DaisyMisc"), false,
               function(editor, id) {
                   self.changeAlignment(editor, id, "left");
               }, null);
    cfg.registerButton("daisy-align-center", i18n["hint.align-center"], editor.imgURL("ed_align_center.gif", "DaisyMisc"), false,
               function(editor, id) {
                   self.changeAlignment(editor, id, "center");
               }, null);
    cfg.registerButton("daisy-align-right", i18n["hint.align-right"], editor.imgURL("ed_align_right.gif", "DaisyMisc"), false,
               function(editor, id) {
                   self.changeAlignment(editor, id, "right");
               }, null);

    cfg.registerButton("daisy-valign-none", i18n["hint.default-vertical-alignment"], editor.imgURL("cell_valign_none.gif", "DaisyMisc"), false,
               function(editor, id) {
                   self.changeCellAlignment(editor, id, "none");
               }, null);
    cfg.registerButton("daisy-valign-top", i18n["hint.align-top"], editor.imgURL("cell_valign_top.gif", "DaisyMisc"), false,
               function(editor, id) {
                   self.changeCellAlignment(editor, id, "top");
               }, null);
    cfg.registerButton("daisy-valign-middle", i18n["hint.align-middle"], editor.imgURL("cell_valign_middle.gif", "DaisyMisc"), false,
               function(editor, id) {
                   self.changeCellAlignment(editor, id, "middle");
               }, null);
    cfg.registerButton("daisy-valign-bottom", i18n["hint.align-bottom"], editor.imgURL("cell_valign_bottom.gif", "DaisyMisc"), false,
               function(editor, id) {
                   self.changeCellAlignment(editor, id, "bottom");
               }, null);

    cfg.registerButton("daisy-td-th-switch", i18n["hint.switch-normal-header-cell"], editor.imgURL("td_th_switch.gif", "DaisyMisc"), false,
               function(editor, id) {
                   self.switchBetweenTdAndTh(editor, id);
               }, null);
    cfg.registerButton("daisy-insert-table", i18n["hint.insert-table"], editor.imgURL("insert_table.gif", "DaisyMisc"), false,
               function(editor, id) {
                   self.insertTable(editor, id);
               }, null);
   cfg.registerButton("daisy-table-settings", i18n["hint.table-settings"], editor.imgURL("table_settings.gif", "DaisyMisc"), false,
             function(editor, id) {
                 self.editTableSettings(editor, id);
             }, null);
    cfg.registerButton("daisy-delete-table", i18n["hint.delete-table"], editor.imgURL("delete_table.gif", "DaisyMisc"), false,
               function(editor, id) {
                   self.deleteTable(editor, id);
               }, null);
    cfg.registerButton("daisy-insert-div", i18n["hint.insert-div"], editor.imgURL("insert_div.gif", "DaisyMisc"), false,
            function(editor, id) {
                self.showInsertDivDialog(editor, id);
            }, null);
    cfg.registerButton("daisy-div-settings", i18n["hint.div-settings"], editor.imgURL("div_settings.gif", "DaisyMisc"), false,
          function(editor, id) {
              self.editDivSettings(editor, id);
          }, null);
    cfg.registerButton("daisy-delete-div", i18n["hint.delete-div"], editor.imgURL("delete_div.gif", "DaisyMisc"), false,
            function(editor, id) {
                self.deleteDiv(editor, id);
            }, null);
    cfg.registerButton("daisy-remove-format", i18n["hint.remove-formatting"], editor.imgURL("eraser.gif", "DaisyMisc"), false,
               function(editor, id) {
                   self.removeFormat(editor, id);
               }, null);
    cfg.registerButton("daisy-make-tt", i18n["hint.tt"], editor.imgURL("teletype.gif", "DaisyMisc"), false,
               function(editor, id) {
                   self.makeTeletype(editor, id);
               }, null);

    cfg.registerButton("daisy-quote", i18n["hint.quote"], editor.imgURL("quote.gif", "DaisyMisc"), false,
               function(editor, id) {
                   editor.execCommand("indent");
               }, null);
    cfg.registerButton("daisy-unquote", i18n["hint.unquote"], editor.imgURL("unquote.gif", "DaisyMisc"), false,
               function(editor, id) {
                   editor.execCommand("outdent");
               }, null);

    cfg.registerButton("daisy-goto", i18n["hint.goto"], editor.imgURL("goto.gif", "DaisyMisc"), false,
               function(editor, id) {
                   self.gotoElement(editor, id);
               }, null);

    cfg.registerButton("daisy-switch-to-source", i18n["hint.switch-to-source"], editor.imgURL("ed_html.gif", "DaisyMisc"), false,
               function(editor, id) {
                   self.switchToSource(editor, id);
               }, null);

    cfg.registerButton("daisy-cleanup", i18n["hint.cleanup"], editor.imgURL("cleanup.gif", "DaisyMisc"), false,
               function(editor, id) {
                   self.cleanupHtml(editor, id);
               }, null);

    cfg.registerButton("daisy-variables", i18n["hint.variables"], editor.imgURL("variable.gif", "DaisyMisc"), false,
               function(editor, id) {
                   self.variables(editor, id);
               }, null);

    cfg.registerButton("daisy-ol-settings", i18n["hint.ol-settings"], editor.imgURL("ol_settings.gif", "DaisyMisc"), false,
               function(editor, id) {
                   self.orderedListSettings(editor, id);
               }, null);
};

DaisyMisc._pluginInfo = {
    name          : "DaisyMisc",
    version       : "1.0",
    developer     : "Outerthought",
    developer_url : "http://outerthought.org",
    c_owner       : "Outerthought",
    sponsor       : null,
    sponsor_url   : null,
    license       : "htmlArea"
};

DaisyMisc.prototype.changeAlignment = function(editor, id, type) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    var parent = daisySearchParentBlockElement(editor);
    var parentName = parent.tagName.toLowerCase();

    if (parentName == "body" || parentName == "td" || parentName == "th") {
        // create a p
        var value = "p";
        if (HTMLArea.is_ie) value = "<" + value + ">";
        editor.execCommand("formatblock", false, value);
        parent = editor.getParentElement();
        parentName = parent.tagName.toLowerCase();
    }

    if (parentName == "p") {
        if (type == "none")
            parent.align = "";
        else
            parent.align = type;
    }
}

DaisyMisc.prototype.changeCellAlignment = function(editor, id, type) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    var element = editor.getParentElement();
    var found = false;
    while (element != null && element.nodeType == 1) {
        var tagName = element.tagName.toLowerCase();
        if (tagName == "td" || tagName == "th") {
            found = true;
            break;
        }
        element = element.parentNode;
    }

    if (found) {
        if (type == "none")
            element.vAlign = "";
        else
            element.vAlign = type;
    }
}

DaisyMisc.prototype.switchBetweenTdAndTh = function(editor, id) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    var element = editor.getParentElement();
    var found = false;
    while (element != null && element.nodeType == 1) {
        var tagName = element.tagName.toLowerCase();
        if (tagName == "td" || tagName == "th") {
            found = true;
            break;
        }
        element = element.parentNode;
    }

    if (!found) {
        alert (DaisyMisc.I18N["js.no-td-th-found"]);
        return;
    }

    var oldEl = element;
    var newEl = editor._doc.createElement(element.tagName.toLowerCase() == "td" ? "TH" : "TD");

    // copy child nodes
    var children = oldEl.childNodes;
    for (var i = 0; i < children.length; i++) {
        var child = children[i];
        var newChild = child.cloneNode(true);
        newEl.appendChild(newChild);
    }

    // copy attributes
    // can't do this by iterating over the attributes since IE reports unset attrs also,
    // and needs special treatment for rowspan/colspan/class anyhow
    if (oldEl.colSpan != null)
        newEl.colSpan = oldEl.colSpan;
    if (oldEl.rowSpan != null)
        newEl.rowSpan = oldEl.rowSpan;
    if (oldEl.id != null)
        newEl.id = oldEl.id;
    if (oldEl.className != null)
        newEl.className = oldEl.className;

    oldEl.parentNode.replaceChild(newEl, oldEl);
    editor.selectNodeContents(newEl, true);
}

/**
 * This function is based on the original from HTMLArea.
 */
DaisyMisc.prototype.insertTable = function(editor) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    var sel = editor._getSelection();
    var range = editor._createRange(sel);
    var self = this;
    daisy.dialog.popupDialog(daisyGetHtmlAreaDialog("DaisyMisc", "insert_table.html"), function(param) {
        if (!param) {    // user must have pressed Cancel
            return false;
        }
        var doc = editor._doc;
        var firstRowHeadings = param["firstRowHeadings"] == true;
        var firstColHeadings = param["firstColHeadings"] == true;
        // create the table element
        var table = doc.createElement("table");
        var tbody = doc.createElement("tbody");
        table.appendChild(tbody);
        for (var i = 0; i < param["f_rows"]; ++i) {
            var tr = doc.createElement("tr");
            tbody.appendChild(tr);
            for (var j = 0; j < param["f_cols"]; ++j) {
                var cellName = "td";
                if (firstRowHeadings && i == 0 || firstColHeadings && j == 0)
                    cellName = "th";
                var td = doc.createElement(cellName);
                tr.appendChild(td);
                // Mozilla & webkit like to see something inside the cell.
                (HTMLArea.is_gecko || HTMLArea.is_webkit) && td.appendChild(doc.createElement("br"));
            }
        }

        // Make table 100% width by default
        table.setAttribute("width", "100%");
        table.setAttribute("print-width", "100%");

        // Do the actual table insertion
        table = daisyInsertNode(table, editor);

        // Put cursor in first cell
        var firstCell = self.findFirstTableCell(table);
        if (firstCell != null) {
            editor.selectNodeContents(firstCell, true);
            editor.updateToolbar();
        }
        return true;
    }, null);
}

DaisyMisc.prototype.findFirstTableCell = function(table) {
    var children = table.childNodes;
    for (var i = 0; i < children.length; i++) {
        if (children[i].nodeType == dojo.dom.ELEMENT_NODE) {
            var tagName = children[i].tagName.toLowerCase();
            if (tagName == "td" || tagName == "th")
                return children[i];
            var result = this.findFirstTableCell(children[i]);
            if (result != null)
                return result;
        }
    }
    return null;
}

DaisyMisc.prototype.deleteTable = function(editor, id) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    var element = this.getTable(editor, id);
    if (element == null)
        return;

    var confirmed = confirm(DaisyMisc.I18N["js.remove-table-confirm"]);

    if (confirmed) {
        element.parentNode.removeChild(element);
        editor.updateToolbar();
    }
}

DaisyMisc.prototype.editTableSettings = function(editor, id) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    var table = this.getTable(editor, id);
    if (table == null)
        return;

    var dialogParams = new Object();
    dialogParams.align = table.getAttribute("align");
    dialogParams.columnCount = this.countMaxColumns(table.childNodes, 0);
    dialogParams.screenWidth = table.getAttribute("width");
    dialogParams.printWidth = table.getAttribute("print-width");
    dialogParams.printColumnWidths = table.getAttribute("print-column-widths");
    dialogParams.screenColumnWidths = table.getAttribute("column-widths");
    dialogParams.tableCaption = table.getAttribute("daisy-caption");
    dialogParams.tableType = table.getAttribute("daisy-table-type");
    dialogParams.classes = table.className;

    daisy.dialog.popupDialog(daisyGetHtmlAreaDialog("DaisyMisc", "table_settings.html"), function(param) {
        if (!param) {    // user must have pressed Cancel
            return false;
        }

        if (param.printColumnWidths != null)
            table.setAttribute("print-column-widths", param.printColumnWidths);
        else
            table.removeAttribute("print-column-widths");

        if (param.screenColumnWidths != null)
            table.setAttribute("column-widths", param.screenColumnWidths);
        else
            table.removeAttribute("column-widths");
        daisyApplyColumnWidths(table, param.screenColumnWidthsArray, editor._doc);

        if (param.printWidth != null)
            table.setAttribute("print-width", param.printWidth);
        else
            table.removeAttribute("print-width");

        if (param.screenWidth != null)
            table.setAttribute("width", param.screenWidth);
        else
            table.removeAttribute("width");

        if (param.tableCaption != null)
            table.setAttribute("daisy-caption", param.tableCaption);
        else
            table.removeAttribute("daisy-caption");

        if (param.tableType != null)
            table.setAttribute("daisy-table-type", param.tableType);
        else
            table.removeAttribute("daisy-table-type");

        if (param.align != null)
            table.setAttribute("align", param.align);
        else
            table.removeAttribute("align");

        if (param.classes == null || param.classes.trim() == "") {
            table.className = "";
        } else {
            table.className = param.classes;
        }

        return true;
    }, dialogParams);
}

DaisyMisc.prototype.countMaxColumns = function(nodes, currentMax) {
    var newMax = currentMax;
    for (var i = 0; i < nodes.length; i++) {
        var node = nodes.item(i);
        if (node.nodeType == 1) { // 1 = element node
            if (node.tagName.toLowerCase() == "tr") {
                var rowChildren = node.childNodes;
                var cellCount = 0;
                for (var k = 0; k < rowChildren.length; k++) {
                    var rowChild = rowChildren.item(k);
                    if (rowChild.nodeType == 1 && (rowChild.tagName.toLowerCase() == "td" || rowChild.tagName.toLowerCase() == "th"))
                        cellCount++;
                }
                if (cellCount > newMax)
                    newMax = cellCount;
            } else if (node.tagName.toLowerCase() == "tbody") {
                var cellCount = this.countMaxColumns(node.childNodes, newMax);
                if (cellCount > newMax)
                    newMax = cellCount;
                // normally there should only be one tbody and it should contain all rows, but lets
                // just continue to handle invalid structured tables.
            }
        }
    }
    return newMax;
}

DaisyMisc.prototype.getTable = function(editor, id) {
    var element = editor.getParentElement();
    var found = false;
    while (element != null && element.nodeType == 1) {
        var tagName = element.tagName.toLowerCase();
        if (tagName == "table") {
            found = true;
            break;
        }
        element = element.parentNode;
    }

    if (!found) {
        alert (DaisyMisc.I18N["js.cursor-not-inside-table"]);
        return null;
    }

    return element;
}

/**
 * This function is based on the original from HTMLArea.
 */
DaisyMisc.prototype.showInsertDivDialog = function(editor) {
    editor.focusEditor();
    if (!editor.daisyIsEditingAllowed())
        return;

    var doc = editor._doc;
    var self = this;

    daisy.dialog.popupDialog(daisyGetHtmlAreaDialog("DaisyMisc", "div_settings.html"), function(param) {
        if (!param) {    // user must have pressed Cancel
            return false;
        }
        var div = self.insertDiv(editor, param.classes, "");

	    div.appendChild(doc.createElement("br"));
	    if  (param.divId!=null && param.divId!="") {
	       div.id = param.divId;
	    }
	    
	    // Put cursor in div
	    editor.selectNodeContents(div, true);
	    editor.updateToolbar();
	    
	    return true;
    }, null);

}

DaisyMisc.prototype.insertDiv = function(editor, className, elementText) {
    editor.focusEditor();
    if (!editor.daisyIsEditingAllowed())
        return;

    if (!daisyEmptySelection(editor)) {
        if (HTMLArea.is_ie) {
            var range = editor._createRange(editor._getSelection());
            range.collapse(true);
            range.select();
        } else {
            editor._createRange(editor._getSelection()).collapse(true);
        }
    }
    
    // The new include is inserted as a sibling of the block-level element in which the
    // cursor is located (p, pre, ...) or if the cursor is one of the 'container elements'
    // listed below, the include is inserted at the position of the cursor instead.

    var containerElements = ["body"];
    var parentNode = editor.getParentElement();
    while (!this.arrayContains(containerElements, parentNode.tagName.toLowerCase()) && daisyIsInlineElement(parentNode)) {
        parentNode = parentNode.parentNode;
    }

    var newElement;
    if (this.arrayContains(containerElements, parentNode.tagName.toLowerCase())) {
        editor.insertHTML("<div class='" + className + "'>" + elementText.replace(/</g, "&lt;").replace(/>/g, "&gt;") + "</div>");
        newElement = editor.getParentElement();
    } else {
        var pre = editor._doc.createElement("div");
        pre.className = className;
        var text = editor._doc.createTextNode(elementText);
        pre.appendChild(text);
        var where = parentNode.parentNode;
        where.insertBefore(pre, parentNode.nextSibling);
        newElement = pre;

        // If the cursor was located inside an empty paragraph, remove it.
        // (An empty paragraph in Firefox typically contains a single br, therefore that check)
        if (parentNode.tagName.toLowerCase() == "p"
                && (parentNode.childNodes.length == 0 || (parentNode.childNodes.length == 1 && parentNode.childNodes[0].nodeType == dojo.dom.ELEMENT_NODE && parentNode.childNodes[0].toLowerCase() == "br"))) {
            parentNode.parentNode.removeChild(parentNode);
        }
    }

    return newElement;

}

DaisyMisc.prototype.deleteDiv = function(editor, id) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    var element = this.getDiv(editor, id);
    if (element == null)
        return;

    var confirmed = confirm(DaisyMisc.I18N["js.remove-div-confirm"]);

    if (confirmed) {
        element.parentNode.removeChild(element);
        editor.updateToolbar();
    }
}

DaisyMisc.prototype.editDivSettings = function(editor, id) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    var div = this.getDiv(editor, id);
    if (div == null)
        return;

    var dialogParams = new Object();
    dialogParams.classes = div.getAttribute("class");

    daisy.dialog.popupDialog(daisyGetHtmlAreaDialog("DaisyMisc", "div_settings.html"), function(param) {
        if (!param) {    // user must have pressed Cancel
            return false;
        }

        if(param.classes != null && param.classes != "")
	    	div.setAttribute("class", param.classes);
	    if(param.divId != null && param.divId != "")
	    	div.setAttribute("id", param.divId);

        return true;
    }, dialogParams);
}

DaisyMisc.prototype.getDiv = function(editor, id) {
    var element = editor.getParentElement();
    var found = false;
    while (element != null && element.nodeType == 1) {
        var tagName = element.tagName.toLowerCase();
        if (tagName == "div") {
            found = true;
            break;
        }
        element = element.parentNode;
    }

    if (!found) {
        alert (DaisyMisc.I18N["js.cursor-not-inside-div"]);
        return null;
    }

    return element;
}

DaisyMisc.prototype.removeFormat = function(editor, id) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    editor.execCommand("removeformat");
    // Internet Explorer doesn't remove inline span's, therefore do this this way.
    if (HTMLArea.is_ie) {
        var sel = editor._getSelection();
        var range = editor._createRange(sel);
        if (daisyCheckInlineRange(range, editor._doc) == 2) {
            var tekst = range.text;
            range.text = tekst;
            sel = editor._getSelection();
            range = editor._createRange(sel);
            range.select();
            editor.focusEditor();
        }
    }
}

DaisyMisc.prototype.makeTeletype = function(editor, id) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

	var sel = editor._getSelection();
	var range = editor._createRange(sel);
    // check the selection contains only inline elements, otherwise this may lead
    // to undoable loss of text.
    if (daisyCheckInlineRange(range, editor._doc) != 2) {
        alert(DaisyMisc.I18N["js.teletype-impossible"]);
        return;
    }
	editor.surroundHTML("<tt>", "</tt>");
}

DaisyMisc.prototype.gotoElement = function(editor, id) {
    var elementsWithIds = [];
    this.getElementsWithIds(editor._doc.documentElement, elementsWithIds);
    var headerElements = [];
    this.getHeaderElements(editor._doc.documentElement, headerElements);

    if (elementsWithIds.length == 0 && headerElements.length == 0) {
        alert(DaisyMisc.I18N["js.nothing-to-go-to"]);
        return;
    }

    var dialogParams = {
        "elementsWithIds" : elementsWithIds,
        "headerElements" : headerElements,
        "editorWindow" : editor._iframe.contentWindow };

    daisy.dialog.popupDialog(daisyGetHtmlAreaDialog("DaisyMisc", "goto.html"),
        function(param) {
            // nothing to do on return
        }, dialogParams);
}

DaisyMisc.prototype.getElementsWithIds = function(element, list) {
    var id = element.getAttribute("id");
    if (id != null && id.length > 0)
        list.push(element);

    var childNodes = element.childNodes;
    for (var i = 0; i < childNodes.length; i++) {
        if (childNodes[i].nodeType == 1)
            this.getElementsWithIds(childNodes[i], list);
    }
}

DaisyMisc.prototype.getHeaderElements = function(element, list) {
    // don't process include previews
    if (element.className == daisyIncludePreviewClass)
        return;

    var tagName = element.tagName.toLowerCase();
    if (tagName.match(/^h[1-9]$/))
        list.push(element);

    var childNodes = element.childNodes;
    for (var i = 0; i < childNodes.length; i++) {
        if (childNodes[i].nodeType == 1)
            this.getHeaderElements(childNodes[i], list);
    }
}

DaisyMisc.prototype.switchToSource = function(editor, id) {
	if (!editor._textArea.form['switchEditors']) { // for the inline editor, the widget is named part_{partId}.validateEditors
        var textAreaId = editor._textArea.id;
        var actionId = textAreaId.replace(".part:input", ".switchEditors");
        cocoon.forms.submitForm(editor._textArea.form[actionId]);
    } else {
        cocoon.forms.submitForm(editor._textArea.form.switchEditors);
    }
}

DaisyMisc.prototype.variables = function(editor, id) {
    if (this.variablesListPopup == null) {
        this.variablesListPopup = new VariablesListPopup(editor);
    }

    var triggerElement = editor._toolbarObjects["daisy-variables"].element;
    this.variablesListPopup.toggleDisplay(triggerElement);
}

DaisyMisc.prototype.orderedListSettings = function(editor, id) {
    if (this.orderedListPopup == null) {
        this.orderedListPopup = new OrderedListSettingsPopup(editor);
    }

    var triggerElement = editor._toolbarObjects["daisy-ol-settings"].element;
    this.orderedListPopup.toggleDisplay(triggerElement);
}

DaisyMisc.prototype.cleanupHtml = function(editor, id) {
	if (!editor._textArea.form['validateEditors']) { // for the inline editor, the widget is named part_{partId}.validateEditors
		var textAreaId = editor._textArea.id;
		var actionId = textAreaId.replace(".part:input", ".validateEditors");
		cocoon.forms.submitForm(editor._textArea.form[actionId]);
	} else {
        cocoon.forms.submitForm(editor._textArea.form.validateEditors);
	}
}

DaisyMisc.prototype.arrayContains = function(someArray, someValue) {
    for (var i = 0; i < someArray.length; i++) {
        if (someArray[i] == someValue)
            return true;
    }
    return false;
}

DaisyMisc.prototype.onUpdateToolbar = function() {
    var parent = daisySearchParentBlockElement(this.editor);
    var parentName = parent.tagName.toLowerCase();
    if (parentName == "p" || parentName == "body" || parentName == "th" || parentName == "td") {
        this.editor._toolbarObjects["daisy-align-left"].state("enabled", true);
        this.editor._toolbarObjects["daisy-align-center"].state("enabled", true);
        this.editor._toolbarObjects["daisy-align-right"].state("enabled", true);
    } else {
        this.editor._toolbarObjects["daisy-align-left"].state("enabled", false);
        this.editor._toolbarObjects["daisy-align-center"].state("enabled", false);
        this.editor._toolbarObjects["daisy-align-right"].state("enabled", false);
    }

    var inCell = false;
    while (parent != null && parent.nodeType == 1) {
        var tagName = parent.tagName.toLowerCase();
        if (tagName == "td" || tagName == "th") {
            inCell = true;
            break;
        }
        parent = parent.parentNode;
    }

    if (inCell) {
        this.editor._toolbarObjects["daisy-td-th-switch"].state("enabled", true, true);
        this.editor._toolbarObjects["daisy-delete-table"].state("enabled", true, true);
        this.editor._toolbarObjects["daisy-valign-none"].state("enabled", true, true);
        this.editor._toolbarObjects["daisy-valign-top"].state("enabled", true, true);
        this.editor._toolbarObjects["daisy-valign-middle"].state("enabled", true, true);
        this.editor._toolbarObjects["daisy-valign-bottom"].state("enabled", true, true);
    } else {
        this.editor._toolbarObjects["daisy-td-th-switch"].state("enabled", false, true);
        this.editor._toolbarObjects["daisy-delete-table"].state("enabled", false, true);
        this.editor._toolbarObjects["daisy-valign-none"].state("enabled", false, true);
        this.editor._toolbarObjects["daisy-valign-top"].state("enabled", false, true);
        this.editor._toolbarObjects["daisy-valign-middle"].state("enabled", false, true);
        this.editor._toolbarObjects["daisy-valign-bottom"].state("enabled", false, true);
    }
}

// Class containing the implementation of the variables popup
dojo.declare("VariablesListPopup", null, {
    variablesLoadCounter: 0,

    initializer: function(editor) {
        this.editor = editor;
        this.varsPopup = dojo.widget.createWidget("PopupContainer", {toggle: "plain"});
        var varsPopupContainerNode = this.varsPopup.domNode;

        dojo.body().appendChild(this.varsPopup.domNode);

        this.contentDiv = document.createElement("div");
        varsPopupContainerNode.appendChild(this.contentDiv);

        var title = document.createElement("div");
        title.appendChild(document.createTextNode(DaisyMisc.I18N["variables.title"]));
        this.contentDiv.appendChild(title);

        this.variablesList = document.createElement("div");
        this.contentDiv.appendChild(this.variablesList);

        var actionsDiv = document.createElement("div");
        this.contentDiv.appendChild(actionsDiv);

        var button = document.createElement("button");
        button.appendChild(document.createTextNode(DaisyMisc.I18N["variables.remove"]));
        var self = this;
        dojo.event.connect(button, "onclick", function(event) {self._removeVariable(); });
        actionsDiv.appendChild(button);

        dojo.html.setStyleAttributes(this.contentDiv, "background-color: #ebebeb; border: 1px solid gray; padding: .5em; width: 25em; min-height: 3em;");
        dojo.html.setStyleAttributes(this.variablesList, "margin: .3em 0; height: 10em; overflow: auto;");
        dojo.html.setStyleAttributes(actionsDiv, "padding: 3px 0px");
        dojo.html.setStyleAttributes(title, "padding: .3em; border-bottom: 1px solid black; background: #ddf; font-weight: bold");

        dojo.html.setDisplay(this.contentDiv, "");
    },

    toggleDisplay: function(triggerElement) {
        if (this.varsPopup.isShowingNow) {
            this.varsPopup.close();
        } else {
            this._loadVariablesList();
            var self = this;
            var parentStub = {
                "focus": function() { self.editor.focusEditor(); }
            }
            this.varsPopup.open(triggerElement, parentStub, triggerElement);
        }
    },

    _loadVariablesList: function() {
        var progressIcon = daisy.mountPoint + "/resources/skins/default/images/progress_indicator_flat.gif";
        this.variablesList.innerHTML = "<img src='" + progressIcon + "'/>";

        var counter = ++this.variablesLoadCounter;

        var self = this;
        var url = daisy.mountPoint + "/" + daisy.site.name + "/editing/variables.xml";
        dojo.io.bind({
                preventCache: true,
                url: url,
                load: function(type, data, evt) {
                    if (counter != self.variablesLoadCounter) // ignore, there's a newer request already
                        return;
                    var result = data.documentElement.getAttribute("result");
                    if (result == "ok") {
                        self.variablesList.innerHTML = "result ok";
                        self._buildVariablesList(data.documentElement);
                    } else {
                        self.variablesList.innerHTML = "Error loading variables";
                    }
                },
                error: function(type, error) {
                    if (counter != self.variablesLoadCounter) // ignore, there's a newer request already
                        return;
                    self.variablesList.innerHTML = "Error loading variables";
                },
                mimetype: "text/xml"
        });
    },

    _buildVariablesList: function(documentEl) {
        try {
            var doc = this.variablesList.ownerDocument;
            var tableEl = doc.createElement("table");
            var tableBody = doc.createElement("tbody");
            tableEl.appendChild(tableBody);
            var self = this;

            var addCell = function(type, content, row) {
                var cell = doc.createElement(type);
                if (dojo.lang.isString(content)) {
                    cell.appendChild(doc.createTextNode(content));
                } else {
                    cell.appendChild(content);
                }
                row.appendChild(cell);
            }

            var addVariableRow = function(name, value) {
                var row = doc.createElement("tr");

                var a = doc.createElement("a");
                a.href = "#";
                a.appendChild(doc.createTextNode(name));
                dojo.event.connect(a, "onclick", function(event) {self._insertVariable(name, event); });

                addCell("td", a, row);
                addCell("td", value, row);

                tableBody.appendChild(row);
            }

            // Add the variables
            var variablesAvailable = false;
            var variableEl = dojo.dom.firstElement(documentEl, "variable");
            while (variableEl != null) {
                var name = variableEl.getAttribute("name");
                var value = doc.createElement("div");
                var children = variableEl.childNodes;
                value.innerHTML = dojo.dom.innerXML(variableEl);
                addVariableRow(name, value);
                variableEl = dojo.dom.nextElement(variableEl, "variable");
                variablesAvailable = true;
            }

            dojo.dom.removeChildren(this.variablesList); // removes progress indicator
            if (variablesAvailable) {
                this.variablesList.appendChild(tableEl);
            } else {
                this.variablesList.innerHTML = DaisyMisc.I18N["variables.no-variables"];
            }
        } catch (error) {
            dojo.debug("VariablesList: error building variables list rendering.", error);
            this.versionsList.innerHTML = "(error rendering variables list)"; // should not occur, no i18n needed
        }
    },

    _insertVariable : function(name) {
        var editor = this.editor;
        editor.focusEditor();

        if (!editor.daisyIsEditingAllowed())
            return;

        this.hidePopup();

        // Test that we are currently not inside a variable -- nested variables make no sense
        var element = editor.getParentElement();
        while (element != null && daisyIsInlineElement(element)) {
            if (element.tagName.toLowerCase() == "span" && element.className == "variable") {
                alert(DaisyMisc.I18N["variables.cannot-nest-variable"]);
                return;
            }
            element = element.parentNode;
        }

        // Collapse selection if any
        daisyClearSelection(editor);

        // insert the variable
        var tempId = "newly-inserted-variable-" + new Date().getTime();
        editor.insertHTML("<span class='variable' id='" + tempId + "'>" + name + "</variable>");
        var newSpan = editor._doc.getElementById(tempId);
        newSpan.removeAttribute("id");

        // move the cursor behind the inserted span so that the user can continue typing
        //  (happens by default in IE)
        if (!HTMLArea.is_ie) {
            var sibling = newSpan.nextSibling;
            if (sibling != null && sibling.nodeType == dojo.dom.TEXT_NODE) {
                daisySelectText(editor, sibling, 0, 0);
            } else {
                var newText = editor._doc.createTextNode(" ");
                if (sibling == null)
                    newSpan.parentNode.appendChild(newText);
                else
                    newSpan.parentNode.insertBefore(newText, sibling);
                daisySelectText(editor, newText, 1, 1);
            }
        }

    },

    _removeVariable: function() {
        var editor = this.editor;
        editor.focusEditor();

        if (!editor.daisyIsEditingAllowed())
            return;

        var element = editor.getParentElement();
        while (element != null && daisyIsInlineElement(element)) {
            if (element.tagName.toLowerCase() == "span" && element.className == "variable") {
                element.parentNode.removeChild(element);
                this.hidePopup();
                return;
            }
            element = element.parentNode;
        }

        alert(DaisyMisc.I18N["variables.not-in-variable"]);
    },

    hidePopup: function() {
        if (this.varsPopup.isShowingNow) {
            this.varsPopup.close();
        }
    }
})

// Class containing the implementation of the ordered list settings popup
dojo.declare("OrderedListSettingsPopup", null, {
    initializer: function(editor) {
        this.editor = editor;
        this.popup = dojo.widget.createWidget("PopupContainer", {toggle: "plain"});
        dojo.body().appendChild(this.popup.domNode);

        var contentDiv = document.createElement('div');
        this.popup.domNode.appendChild(contentDiv);

        var titleDiv = document.createElement("div");
        contentDiv.appendChild(titleDiv);        
        titleDiv.appendChild(document.createTextNode(DaisyMisc.I18N["olsettings.title"]));
        
        this.form = document.createElement('form');
        contentDiv.appendChild(this.form);
        this.form.action='javascript:void(0);';
        
        var table = document.createElement("table");
        this.form.appendChild(table);

        var tablebody = document.createElement("tbody");
        table.appendChild(tablebody);
        
        var row = document.createElement("tr");
        tablebody.appendChild(row);
        var cell = document.createElement("td");
        cell.appendChild(document.createTextNode(DaisyMisc.I18N["olsettings.start-number"]));
        row.appendChild(cell);
        cell = document.createElement("td");
        cell.innerHTML = "<div style='padding: .2em'><input dojoType='IntegerSpinner' widgetId='orderListStartLevel' size='3' value='1'/></div>";
        row.appendChild(cell);
        
        row = document.createElement("tr");
        tablebody.appendChild(row);
        cell = document.createElement("td");
        cell.appendChild(document.createTextNode(DaisyMisc.I18N["olsettings.style"]));
        row.appendChild(cell);
        cell = document.createElement("td");
        row.appendChild(cell);
        this.styles = document.createElement("select");
        cell.appendChild(this.styles);
        var addOption = function(select, value, label) {
          var opt = document.createElement('option');
          opt.appendChild(document.createTextNode(label));
          opt.value=value;
          select.appendChild(opt);
        };
        addOption(this.styles, 'decimal', DaisyMisc.I18N["olsettings.style.decimal"]);
        addOption(this.styles, 'lower-latin', DaisyMisc.I18N["olsettings.style.lower-latin"]);
        addOption(this.styles, 'upper-latin', DaisyMisc.I18N["olsettings.style.upper-latin"]);
        addOption(this.styles, 'lower-roman', DaisyMisc.I18N["olsettings.style.lower-roman"]);
        addOption(this.styles, 'upper-roman', DaisyMisc.I18N["olsettings.style.upper-roman"]);

        dojo.html.setStyleAttributes(contentDiv, "background-color: #ebebeb; border: 1px solid gray; padding: .5em;");
        dojo.html.setStyleAttributes(titleDiv, "padding: .3em; border-bottom: 1px solid black; background: #ddf; font-weight: bold;");

        var parser = new dojo.xml.Parse();
        var frag = parser.parseElement(table, null, true);
        var comps = dojo.widget.getParser().createComponents(frag);

        this.startLevelInput = comps[0];

        dojo.event.connect(this.startLevelInput, "adjustValue", this, "_updateOrderedList");
        dojo.event.connect(this.startLevelInput.textbox, "onkeyup", this, "_updateOrderedList");
        dojo.event.connect(this.styles, "onchange", this, "_updateOrderedList");
        
        dojo.html.setDisplay(contentDiv, "");
    },

    toggleDisplay: function(triggerElement) {
        if (this.popup.isShowingNow) {
            this.popup.close();
        } else {
            if (!this.editor.daisyIsEditingAllowed())
                return;

            this.orderedList = this._getOrderedList();
            if (this.orderedList == null) {
                alert(DaisyMisc.I18N["olsettings.no-ol-found"]);
                return;
            }

            var start = this.orderedList.getAttribute("start");
            this.startLevelInput.textbox.value = start == "" || start == null ? "1" : start;
            
            var selection = 0;
            // this part could be made more efficient by looping over the element's classes and using a map for lookup
            for (var i=0;i<this.styles.options.length;i++) {
              var style = this.styles.options[i].value;
              if ((dojo.html.hasClass(this.orderedList, 'dsy-liststyle-decimal') && style=='decimal')
                || (dojo.html.hasClass(this.orderedList, 'dsy-liststyle-lower-latin') && style=='lower-latin')
                || (dojo.html.hasClass(this.orderedList, 'dsy-liststyle-upper-latin') && style=='upper-latin')
                || (dojo.html.hasClass(this.orderedList, 'dsy-liststyle-lower-roman') && style=='lower-roman')
                || (dojo.html.hasClass(this.orderedList, 'dsy-liststyle-upper-roman') && style=='upper-roman')) {
                selection = i;
              }
            }
            this.styles.selectedIndex = selection;
            this.styles.options[selection].selected = true;
            
    
            var self = this;
            var parentStub = {
                "focus": function() { self.editor.focusEditor(); }
            }
            this.popup.open(triggerElement, parentStub, triggerElement);
            this.startLevelInput.textbox.focus();

            // This trick is for Firefox to cause to redraw the popup after
            // the images of the spinner are loaded. Not perfect of course.
            // (to see the problem: empty browser cache, reopen editor and open the popup)
            if (this.firstAppearance == null) {
                this.firstAppearance = false;
                dojo.lang.setTimeout(function() {
                    self.hidePopup();
                    self.popup.open(triggerElement, parentStub, triggerElement);
                }, 200);
            }
        }
    },

    hidePopup: function() {
        if (this.popup.isShowingNow) {
            this.popup.close();
        }
    },

    _getOrderedList: function() {
        // Search ol element
        var element = this.editor.getParentElement();
        var olFound = false;
        while (element != null && element.nodeType == dojo.dom.ELEMENT_NODE) {
            if (element.tagName.toLowerCase() == "ol") {
                olFound = true;
                break;
            }
            element = element.parentNode;
        }

        if (!olFound)
            return null;
        else
            return element;
    },

    _updateOrderedList: function() {
        var editor = this.editor;
        
        var start = this.startLevelInput.textbox.value;
        if (start == null || start == "" || start == "1") {
            this.orderedList.removeAttribute("start");
        } else {
            if (start.match(/^[ \-0-9]+$/))
                this.orderedList.setAttribute("start", start);
        }
        
        this.orderedList.className = '';
        if (this.styles.options[this.styles.selectedIndex].value != 'decimal') {
          dojo.html.addClass(this.orderedList, 'dsy-liststyle-' + this.styles.options[this.styles.selectedIndex].value);
        }

        // In IE, updated ordered list moves focus away from input
        this.startLevelInput.textbox.focus();
    }
})

