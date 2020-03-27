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

dojo.require("daisy.html");
dojo.require("dojo.widget.PopupContainer");
dojo.require("dojo.i18n.common");
dojo.requireLocalization("daisy.widget", "messages", null, /* available languages, to avoid 404 requests */ "ROOT,nl,fr,de,es,ru");

window.i18n_bundle = dojo.i18n.getLocalization("daisy.widget", "messages");

/*
 * Initialiases the HTMLAreas for daisy.
 *
 * This assumes the existance of the following functions or objects:
 *   isHTMLAreaEnabled (function): returns true or false whether you want to actually have the textareas replaced by htmlareas
 *   htmlAreaRegistry: an set of inputid
 *   daisy (object): structure holding mountPoint, siteName, ...
 *   getBranchId(): returns branch of the edited document
 *   getLanguageId(): returns language of the edited document
 */
function initEditor(id, options) {
    var textarea = document.getElementById(id);
    var editor = new HTMLArea(id);
    textarea.htmlarea = editor;
    editor.daisyMountPoint = daisy.mountPoint;
    editor.daisySiteName = daisy.site.name;
    editor.daisyDocumentBranchId = getBranchId();
    editor.daisyDocumentLanguageId = getLanguageId();
    editor.prepareDocumentCallback = [];
    options = $.extend({}, initEditor.prototype.defaultOptions, options); 
    var cfg = editor.config;
    cfg.fullPage = true;
    // Note: enabling the statusbar disables undo in IE
    cfg.statusBar = false;
    cfg.height = options.height || getEditorHeight();
    cfg.sizeIncludesToolbar = true;

    editor.registerPlugin("TableOperations");
    editor.registerPlugin("DaisyImageUtils");
    editor.registerPlugin("DaisyMisc");
    editor.registerPlugin("DaisyLinkUtils");
    editor.registerPlugin("DaisyBookUtils");

    var blocks = new Object();
    blocks[i18n("h1")] = "h1";
    blocks[i18n("h2")] = "h2";
    blocks[i18n("h3")] = "h3";
    blocks[i18n("h4")] = "h4";
    blocks[i18n("h5")] = "h5";
    blocks[i18n("formatted")] = "pre";
    blocks[i18n("paragraph")] = "p";
    blocks[i18n("note")] = "p!note";
    blocks[i18n("warning")] = "p!warn";
    blocks[i18n("fixme")] = "p!fixme";
    blocks[i18n("query")] = "pre!query";
    blocks[i18n("include")] = "pre!include";
    blocks[i18n("query-include")] = "pre!query-and-include";

    editor.registerPlugin("BlockSwitcher", {"blocks": blocks});
    
    if (options.customInitCallback) {
      options.customInitCallback(editor);
    }

    cfg.toolbar = options.toolbar;

    var oldOnSubmit = textarea.form.onsubmit;
    editor.generate();
    // Remove HTML-area's change to the onsubmit, we'll do that ourselves
    textarea.form.onsubmit = oldOnSubmit != null ? oldOnSubmit : null;

    // We do the HTML-area submit handler thing ourselves by adding it to the CForms
    // submit handlers. This makes sures it also gets executed in case of programmatic submits.
    var htmlAreaSubmitHandler = new Object();
    htmlAreaSubmitHandler.forms_onsubmit = function() { editor._textArea.value = editor.getHTML(); return true; };
    cocoon.forms.addOnSubmitHandler(textarea.form, htmlAreaSubmitHandler);
    
    // replace the default state function of the toolbar objects with our own variant,
    // see description of our new function for why
    var daisyButtons = /^daisy-.*$/
    for (var i in editor._toolbarObjects) {
        var btn = editor._toolbarObjects[i];
        if (btn.name.match(daisyButtons)) {
            btn.stateOrig = btn.state;
            btn.state = setButtonStatus;
        }
    }

    // See the function daisyIsEditingAllowed for what this is for
    // Firefox is slower then IE, the value of 100 is tested for somewhat older computers to be still OK.
    editor.daisyEditCheckInterval = HTMLArea.is_ie ? 30 : 100;

    // The daisyIsEditingAllowed function is called from various locations to check if it
    // is OK to modify the document. When the optional fast argument is true, the check
    // will only be performed with a certain interval (causing a potential wrong outcome,
    // but it's no big drama if an include preview is accidentely edited).
    editor.daisyIsEditingAllowed = function(fast) {
        // Note that in case the daisyLastEditCheckResult, we always check the result freshly,
        // to avoid that editing would be disallowed outside of an include preview.
        if (fast == null || !fast || editor.daisyLastEditCheckTime == null || (new Date().getTime() - this.daisyLastEditCheckTime) > this.daisyEditCheckInterval || !this.daisyLastEditCheckResult) {
            var parent = this.getParentElement();
            while (parent != null && parent.nodeType == dojo.dom.ELEMENT_NODE) {
                var tagName = parent.tagName.toLowerCase();
                // editing is not allowed when cursor is located inside a document include preview
                if (tagName == "div" && parent.className == daisyIncludePreviewClass) {
                    if (confirm(i18n("editdoc.include-preview-edit"))) {
                        DaisyLinkUtils.prototype.open(editor);
                    }
                    this.daisyLastEditCheckResult = false;
                    this.daisyLastEditCheckTime = new Date().getTime();
                    return false;
                }
                parent = parent.parentNode;
            }
            this.daisyLastEditCheckResult = true;
            this.daisyLastEditCheckTime = new Date().getTime();
            return true;
        } else {
            // use cached result
            this.daisyLastEditCheckTime = new Date().getTime();
            return this.daisyLastEditCheckResult;
        }
    }

    // Replace HTMLArea's execCommand function so that we can first check
    // if editing is allowed
    editor._originalExecCommand = editor.execCommand;
    editor.execCommand = function(cmdID, UI, param) {
        this.focusEditor();
        if (!this.daisyIsEditingAllowed())
            return;
        editor._originalExecCommand(cmdID, UI, param);
    }

    // replace standard _editorEvent function with our own variant to be able
    // to block the ctrl+v interception by htmlarea, this allows to use shortcuts
    // to paste in mozilla without special configuration. Meanwhile also added various other stuff.
    editor._originalEditorEvent = editor._editorEvent;
    editor._editorEvent = function(ev) {
        var keyEvent = ((HTMLArea.is_ie || HTMLArea.is_webkit) && ev.type == "keydown") || (ev.type == "keypress");

        // Make sure dojo popupwidgets are hidden
        // Ideally we would do:
        //    dojo.widget.PopupManager.registerWin(editor._iframe.contentWindow);
        // but it doesn't work (I assume because HTMLArea stops the events before they can be handled by dojo)
        if (keyEvent) {
            dojo.widget.PopupManager.onKey(ev);
        } else if (ev.type == "mousedown") {
            dojo.widget.PopupManager.onClick(ev);
        }

        // Disable key actions inside include previews
        if (keyEvent)  {
            // keyCode 33 through 40 are all navigation keys
            // (arrows, page up, page down, home, end)
            // 16 through 20 are shift, ctrl, alt (needed only for IE)
            if ((ev.keyCode < 33 || ev.keyCode > 40) && (ev.keyCode < 16 || ev.keyCode > 20) && !this.daisyIsEditingAllowed(true)) {
                HTMLArea._stopEvent(ev);
                return;
            }
        }

        // if editor is IE and tab is pressed: do tab handling ourselves because otherwise it changes focus
        // to other form elements (would be better if there is a way to avoid this). Currently implemented:
        // indent/outdent, and table cell navigation
        if (keyEvent && ev.keyCode == 9) {
            var parentCell = daisySearchParentTableCell(this);

            // if in a table cell
            if (parentCell != null) {
                var cellSibling = ev.shiftKey ? parentCell.previousSibling : parentCell.nextSibling;
                if (cellSibling != null) {
                    editor.selectNodeContents(cellSibling, true);
                } else {
                    var parent = parentCell.parentNode;
                    if (parent.tagName.toLowerCase() == "tr") {
                        var rowSibling = ev.shiftKey ? parent.previousSibling : parent.nextSibling;
                        if (rowSibling != null) {
                            var firstCell = rowSibling.firstChild;
                            if (firstCell != null) {
                                editor.selectNodeContents(firstCell, true);
                            }
                        }
                    }
                }
                HTMLArea._stopEvent(ev);
                return;
            }

            // if not in a table cell
            if (ev.shiftKey)
                this.execCommand("outdent");
            else
                this.execCommand("indent");

            HTMLArea._stopEvent(ev);
            return;
        } else {
            if (HTMLArea.is_ie && keyEvent && ev.ctrlKey && ev.altKey) {
                // When pressing "Alt Gr", Internet Explorer reports that ctrl is also pressed
                // but we don't want to handle those (as these are used to enter special
                // characters on some keyboard layouts)
                return;
            }
            if (keyEvent && ev.ctrlKey) {
                var key = String.fromCharCode((HTMLArea.is_ie || HTMLArea.is_webkit) ? ev.keyCode : ev.charCode).toLowerCase();
                switch (key) {
                    case 'v':
                        return;
                    case 'q':
                        this.execCommand("insertunorderedlist");
                        HTMLArea._stopEvent(ev);
                        return;
                    case 'r':
                        this.execCommand("removeformat");
                        HTMLArea._stopEvent(ev);
                        return;
                }
            }
        }
        editor._originalEditorEvent(ev);
    }

    function prepareDocument() {
        // Wait for document to be loaded
        if (!editor._doc.body) {
            setTimeout(prepareDocument, 50);
            return;
        }

        // insert CSS stylesheet link
        var docEl = editor._doc.documentElement;
        var head = docEl.getElementsByTagName("head")[0];
        // check if there is already a <link> element for the stylesheet to avoid adding it more then once
        // (it can already be present if no HTML cleanup happend on the server side, ie when form
        // was submitted for another reason)
        var hasStylesheet = false;
        var headChildren = head.childNodes;
        for (var i = 0; i < headChildren.length; i++) {
            if (headChildren[i].nodeType == 1 && headChildren[i].tagName.toLowerCase() == "link") {
                if (headChildren[i].getAttribute("rel") == "stylesheet") {
                    hasStylesheet = true;
                }
            }
        }
        if (!hasStylesheet) {
            var link = editor._doc.createElement("link");
            link.setAttribute("rel", "stylesheet");
            link.setAttribute("type", "text/css");
            link.setAttribute("href", daisy.mountPoint + "/resources/skins/" + daisy.skin + "/css/htmlarea.css");
            head.appendChild(link);
        }
        // The below is needed to make IE pick up the stylesheet
        if (HTMLArea.is_ie) {
            editor._doc.body.contentEditable = false;
            editor._doc.body.contentEditable = true;
        }

        if ((HTMLArea.is_gecko || HTMLArea.is_webkit)) {
            // In firefox 1.5, no cursor is visible if the document is empty (and the editor has focus)
            // This is solved by adding an empty paragraph.
            var body = editor._doc.body;
            if (body.childNodes.length == 0 || (body.childNodes[0].nodeType == 3 && body.childNodes[0].nodeValue == "\n")) {
                editor.execCommand("inserthtml", false, "<br/>");
            }
        }

        // change image URL's from daisy: to public URL's
        var images = editor._doc.images;
        for (var i = 0; i < images.length; i++) {
            var image = images[i];
            if (image.src != null && image.src.match(daisyUrlRegexp)) {
                var imageDocId = RegExp.$1;
                var branch = RegExp.$2;
                if (branch == null || branch == "")
                    branch = getBranchId();
                var language = RegExp.$3;
                if (language == null || language == "")
                    language = getLanguageId();
                var version = RegExp.$4;
                if (version == null || version == "")
                    version = "live";
                image.setAttribute("daisy-src", image.src);
                image.src = editor.daisyMountPoint + "/" + editor.daisySiteName + "/" + imageDocId + "/version/" + version + "/part/ImageData/data?branch=" + branch + "&language=" + language;
            }
        }

        if (HTMLArea.is_ie) {
            // store href's in a daisy-href attribute since otherwise IE will
            // change them if they are '#...' only (see DSY-286)
            var links = editor._doc.links;
            for (var i = 0; i < links.length; i++) {
                var link = links[i];
                var href = link.getAttribute("href", 2); // the ", 2" is needed to get the unmodified value
                if (href != null && href != "")
                    link.setAttribute("daisy-href", href);
            }
        }

        if ((HTMLArea.is_gecko || HTMLArea.is_webkit)) {
            // for mozilla: prefer tags to span's with CSS styles
            editor.execCommand("useCSS", false, true);
            // In Firefox 1.5, empty table cells (thus <td> without children) are not editable, therefore put a br in them
            // This shouldn't hurt other firefox/mozilla versions so we don't check for the exact version
            daisyPutBrInTableCells("TD", editor._doc);
            daisyPutBrInTableCells("TH", editor._doc);
            // Firefox can behave somewhat strangely if there is whitespace between e.g. paragraph nodes.
            // E.g. when pressing enter twice at the end of a paragraph, it is impossible to remove
            // those enters by pressing backspace.
            removeEmptyText(editor._doc.documentElement);
        }

        daisyInitColumnWidths(editor._doc);

        // Trigger include preview loading (asynchronously)
        DaisyLinkUtils.prototype.loadIncludePreviews(editor);
        
        if (editor.prepareDocumentCallback) {
        	if (jQuery.isFunction(editor.prepareDocumentCallback)) {
        		editor.prepareDocumentCallback();
        	} else if (jQuery.isArray(editor.prepareDocumentCallback)) {
        		for (var i = 0; i < editor.prepareDocumentCallback.length; i++) {
        			editor.prepareDocumentCallback[i]();
        		}
        	}
        }
    }
    setTimeout(prepareDocument, 150);
    if (window.editorHeightListeners == null)
        window.editorHeightListeners = new Array();
    window.editorHeightListeners.push(
      function(height) { 
        editor._iframe.style.height = (height - editor._toolbar.offsetHeight).toFixed(0) + "px"; 
      }
    );
}

initEditor.prototype.defaultOptions = {
   'toolbar': [ ["daisy-block-switcher",
                         "separator", "bold", "italic", "daisy-make-tt", "strikethrough", "daisy-remove-format",
                         "separator", "subscript", "superscript",
                         "separator", "copy", "cut", "paste", "space", "undo", "redo",
                         "separator", "insertunorderedlist", "insertorderedlist", "daisy-ol-settings",
                         "separator", "daisy-align-none", "daisy-align-left", "daisy-align-center", "daisy-align-right",
                         "separator", "daisy-quote", "daisy-unquote",
                         "separator", "daisy-create-link", "daisy-browse-link", "daisy-link-to-new", "daisy-create-attachment", "daisy-unlink",
                         "separator", "daisy-insert-image", "daisy-alter-image",
                         "separator", "daisy-open",
                         "separator", "daisy-edit-id", "daisy-goto",
                         "separator", "daisy-switch-to-source", "daisy-cleanup"],
                         ["daisy-insert-div", "daisy-delete-div", "daisy-div-settings",
                         "separator", "daisy-insert-table", "daisy-delete-table", "daisy-table-settings",
                         "separator", "TO-row-insert-above", "TO-row-insert-under", "TO-row-delete", "TO-row-split",
                         "separator", "TO-col-insert-before", "TO-col-insert-after", "TO-col-delete", "TO-col-split",
                         "separator", "TO-cell-merge", "daisy-td-th-switch",
                         "separator", "daisy-valign-none", "daisy-valign-top", "daisy-valign-middle", "daisy-valign-bottom",
                         "separator", "T[Books:]", "daisy-indexentry", "daisy-footnote", "daisy-crossreference",
                         "separator", "T[Includes:]", "daisy-insert-include", "daisy-include-settings", "daisy-browse-include", "daisy-load-include-previews", "daisy-remove-include-previews", "daisy-insert-query", "daisy-insert-query-include",
                         "separator", "daisy-variables"] ]
};

function daisyPutBrInTableCells(cellTagName, doc) {
    var cellElements = doc.getElementsByTagName(cellTagName);
    for (var i = 0; i < cellElements.length; i++) {
        var cell = cellElements[i];
        if (cell.childNodes.length == 1 && cell.childNodes[0].nodeType == dojo.dom.TEXT_NODE && cell.childNodes[0].nodeValue == "\n") {
            cell.appendChild(doc.createElement("br"));
        }
    }
}

/**
 * This function removes non-significant whitespace from the DOM-tree.
 * It doesn't remove all non-significant whitespace, mainly those that
 * bother the Firefox HTML editor.
 */
function removeEmptyText(element) {
    var children = element.childNodes;
    for (var i = children.length - 1; i >= 0; i--) {
        var child = children[i];
        // when encountering a text node which only contains whitespace
        if (child.nodeType == dojo.dom.TEXT_NODE && child.nodeValue.replace(/^[\s\n]+$/g, '') == "") {
            // if the previous and next nodes are block-level elements or br's, we remove the whitespace
            if ( (i == 0 || (!daisyIsInlineElement(children[i - 1]) || children[i - 1].tagName.toLowerCase() == "br"))
                    && (i == children.length -1 || (!daisyIsInlineElement(children[i + 1]) || children[i + 1].tagName.toLowerCase() == "br")) ) {
                element.removeChild(child);
            }
        } else if (child.nodeType == dojo.dom.ELEMENT_NODE) {
            var childName = children[i].tagName.toLowerCase();
            if (childName != "pre") {
                removeEmptyText(child);
            }
        }
    }
}

function daisyIsInlineElement(el) {
    return daisyIsInlineTagName(el.tagName);
}

function daisyIsInlineTagName(tag) {
    tag = tag.toLowerCase();
    return (tag == "span" || tag == "b" || tag == "i" || tag == "strong" || tag == "em"
      || tag == "a" || tag == "sub" || tag == "sup" || tag == "img" || tag == "tt" || tag == "br"
      || tag == "del" || tag == "strike");
}

function daisyInitColumnWidths(doc) {
    var valueWithUnitRegExp = /^([0-9.]*)([a-z%*]*)$/;
    var tables = doc.getElementsByTagName("TABLE");
    for (var i = 0; i < tables.length; i++) {
        var table = tables[i];
        var widthsAttr = table.getAttribute("column-widths");

        if (widthsAttr == null || widthsAttr.length == 0)
            continue;

        var widths = widthsAttr.split(";");

        for (var k = 0; k < widths.length; k++) {
           if (!widths[k].match(valueWithUnitRegExp)) {
               widths[k] = null;
           }
        }

        daisyApplyColumnWidths(table, widths, doc, false);
    }
}

daisyApplyColumnWidths = function(table, columnWidths, document, firstRemove) {
    if (firstRemove == null || firstRemove == false) {
        // first remove current <col> elements
        var childNodes = table.childNodes;
        for (var i = childNodes.length - 1; i >= 0; i--) {
            var childNode = childNodes[i];
            if (childNode.nodeType == 1 && childNode.tagName.toLowerCase() == "col")
                table.removeChild(childNode);
        }
    }

    if (columnWidths == null)
        return;

    // now add the new <col> elements
    var firstChild = table.firstChild;
    for (var i = 0; i < columnWidths.length; i++) {
        var col = document.createElement("col");
        if (columnWidths[i] != null)
            col.setAttribute("width", columnWidths[i]);
        table.insertBefore(col, firstChild);
    }
}

/**
 * Customized variant of the setButtonState function of HTMLArea. HTMLArea will
 * always update the button state even if we rather do that ourselves (via the
 * onUpdateToolbar function).
 */
function setButtonStatus(id, newval, reallyDoIt) {
    if (reallyDoIt) {
        this.stateOrig(id, newval);
    }
}

function getEditorHeight() {
    var delta = 200; // estimation of the other stuff on the screen besides the editor
    return dojo.html.getViewport().height - delta;
}

function i18n(key) {
    var translated = window.i18n_bundle[key];
    if (translated == null)
        return key;
    else
        return translated;
}

function lookupDocumentLink(targetLinkField, targetNameField) {
    var mountPoint = daisy.mountPoint;
    var siteName = daisy.site.name;
    var dialogParams = { "daisyMountPoint" : mountPoint, "daisySiteName" : siteName };

    daisy.dialog.popupDialog(mountPoint + "/" + siteName + "/editing/documentBrowser?branch=" + getBranchId() + "&language=" + getLanguageId(),
        function(params) {
            targetLinkField.value = params.url;
            if (params.docName != null && targetNameField != null && targetNameField.value == "")
                targetNameField.value = params.docName;
        }, dialogParams);
}

function openLink(link) {
    if (link == null || link == "")
        return;

    var m = daisyUrlRegexp.exec(link);
    if (m) {
        var branch = m[2];
        var language = m[3];
        var versionId = m[4];
        var target;
        if (versionId) {
            target = daisy.mountPoint + "/" + daisy.site.name + "/" + m[1] + "/version/" + versionId + getBranchLangQueryString(branch, language);
        } else {
            target = daisy.mountPoint + "/" + daisy.site.name + "/" + m[1] + getBranchLangQueryString(branch, language);
        }
        window.open(target);
    } else {
        window.open(link);
    }
}

function lookupDocumentLinkForLinkField(linkEl, labelEl, configName, whereclause) {
    var mountPoint = daisy.mountPoint;
    var siteName = daisy.site.name;
    
    var initialSelection = [];
    
    
    if (linkEl && linkEl.value){
        var linkArray = linkEl.value.split(" / ");
        initialSelection.push(daisy.util.parseDaisyLink(linkArray[linkArray.length-1]));
    }
    var dialogParams = { "daisyMountPoint" : mountPoint, "daisySiteName" : siteName , "initialSelection": initialSelection};
    
    if(whereclause && whereclause!='')
      dialogParams.whereclause = whereclause;

    daisy.dialog.popupDialog(mountPoint + "/" + siteName + "/editing/documentBrowser?branch=" + getBranchId() + "&language=" + getLanguageId() + "&config=" +configName,
        function(params) {
            if (linkEl.tagName.toLowerCase() == "select") {
                var select = linkEl;
                for (var i = 0; i < select.options.length; i++)
                    select.options[i].selected = false;
                select.options[select.options.length] = new Option(params.docName != null ? params.docName : params.url, params.url, false, true);
            } else {
                linkEl.value = params.url;

                var nodes = labelEl.childNodes;
                for (var i = 0; nodes.length > 0;)
                  labelEl.removeChild(nodes.item(i));
                if (params.docName != null)
                    labelEl.appendChild(labelEl.ownerDocument.createTextNode(params.docName));
            }
        }, dialogParams);
}

function openLinkField(linkField) {
    if (linkField.tagName.toLowerCase() == "select") {
        var options = linkField.options;
        for (var i = 0; i < options.length; i++)
            if (options[i].selected)
                openLink(options[i].value);
    } else {
        openLink(linkField.value);
    }
}

function getBranchLangQueryString(optBranch, optLanguage) {
    // when inline editing, there is no getBranchId() and getLanguageId() function.  Instead we fall back to daisy.branchId and daisy.languageId
    var branch = getBranchId();
    if (optBranch && optBranch != "")
        branch = optBranch;
    var language = getLanguageId();
    if (optLanguage && optLanguage != "")
        language = optLanguage;

    return "?branch=" + branch + "&language=" + language;
}

var daisyUrlRegexp = /^daisy:([0-9]{1,19}(?:-[a-zA-Z0-9_]{1,200})?)(?:@([^:#?]*)(?::([^:#?]*))?(?::([^:#?]*))?)?()(?:\?([^#]*))?(#.*)?$/;

var daisyDocIdRegexp = /^[0-9]{1,19}(?:-[a-zA-Z0-9_]{1,200})?$/;

var daisyIncludePreviewClass = "daisy-include-preview";

/**
 * Used by fieldtype_to_widgettemplate.xsl
 */
function daisyShowSelectionList(widgetPath, fieldTypeId) {
    var popup = window.open('selectionList?widgetPath=' + widgetPath + '&fieldTypeId=' + fieldTypeId, '',
            'toolbar=no,menubar=no,personalbar=no,width=350,height=400,left=20,top=40,scrollbars=yes,resizable=yes');

    popup.onLinkSelected = function (text, value) {
        var element = document.getElementById(widgetPath + ":input");
        if (element == null) // For hidden fields, no ":input" is added
            element = document.getElementById(widgetPath);
        if (element == null) {
            alert("Field not found: " + widgetPath);
            return;
        }
        if (element.tagName.toLowerCase() == "select") {
          var select = element;
          for (var i = 0; i < select.options.length; i++)
              select.options[i].selected = false;
          select.options[select.options.length] = new Option(text, value, false, true);
        } else {
          element.value = value;
          var labelEl = document.getElementById(widgetPath + '-label');
          if (labelEl != null) {
            var nodes = labelEl.childNodes;
            for (var i = 0; nodes.length > 0;)
              labelEl.removeChild(nodes.item(i));
            labelEl.appendChild(document.createTextNode(text));
          }
        }
        daisyUpdateClearFieldDisplay(widgetPath);
    }
}

/**
 * Used by fieldtype_to_widgettemplate.xsl
 */
function daisyClearField(widgetName) {
    // clear value
    var input = document.getElementById(widgetName);
    input.value = "";

    // clear label
    var label = document.getElementById(widgetName + "-label");
    var nodes = label.childNodes;
    for (var i = 0; nodes.length > 0;)
        label.removeChild(nodes.item(i));

    daisyUpdateClearFieldDisplay(widgetName);
}

function daisyUpdateClearFieldDisplay(widgetName) {
    var clearButton = document.getElementById(widgetName + "-clearbutton");
    if (clearButton == null) {
        return;
    }

    var value = document.getElementById(widgetName).value;
    if (value == null || value == "") {
        clearButton.style.display = "none";
    } else {
        clearButton.style.display = "";
    }
}

function daisyIgnoreEnter(event) {
    if (event == null) event = window.event; // Internet Explorer
    if (event.keyCode == 13 || event.keyCode == 10) {
        return false;
    } else {
        return true;
    }
}

function daisyGetLocalName(element) {
    if (element.localName != null)
        return element.localName; // DOM
    else
        return element.baseName; // Internet Explorer
}

function daisyMvListAdd(widgetName) {
    // read selected value
    var availableSelect = document.getElementById(widgetName + ":available");
    if (availableSelect.selectedIndex == -1)
        return;
    var selectedOption = availableSelect.options[availableSelect.selectedIndex];
    var selectedValue = selectedOption.value;
    var selectedLabel = selectedOption.hierarchicalLabel;
    if (selectedLabel == null)
        selectedLabel = selectedValue;

    // add to list
    var selectedSelect = document.getElementById(widgetName +  ":input");
    selectedSelect.options[selectedSelect.options.length] = new Option(selectedLabel, selectedValue);
}

function daisyLoadListOptions(widgetName, selectId, fieldTypeId, defaultSelected, addBlank) {
    var loadOptionsPanel = document.getElementById(widgetName + ":loadOptionsPanel");
    loadOptionsPanel.style.display = "none";

    var pleaseWaitPanel = document.getElementById(widgetName + ":pleaseWaitPanel");
    pleaseWaitPanel.style.display = "";

    var availableSelect = document.getElementById(selectId);

    // load the options
    var url = "selectionList.xml?widgetPath=" + widgetName + "&fieldTypeId=" + fieldTypeId;
    var dojoRequest = dojo.io.bind({
       url: url,
       load: function(type, data, evt) {
           // Result document from the server has the structure
           //  <options>
           //     <option value="..." label="..."/>
           //  </options>
           var options = availableSelect.options;
           options.length = 0;
           if (addBlank) {
               options[0] = new Option("", "");
           }
           var children = data.documentElement.childNodes;
           for (var i = 0; i < children.length; i++) {
               var child = children[i];
               if (child.nodeType == dojo.dom.ELEMENT_NODE && daisyGetLocalName(child) == "option") {
                   var value = child.getAttribute("value");
                   var newOption = new Option(child.getAttribute("label"), value);
                   newOption.hierarchicalLabel = child.getAttribute("hierarchicalLabel");
                   if (value == defaultSelected)
                       newOption.selected = true;
                   options[options.length] = newOption;
               }
           }

           // show result
           pleaseWaitPanel.style.display = "none";

           var availablePanel = document.getElementById(widgetName + ":availablePanel");
           availablePanel.style.display = "";
       },
       error: function(type, error) {
           alert(i18n("fieldedit.load-list-data-failed"));
           loadOptionsPanel.style.display = "";
           pleaseWaitPanel.style.display = "none";
       },
       mimetype: "text/xml"
    });
}

/**
 * DaisyFormsMultiValueEditor is the implementation of the free-form multivalue field editor.
 * It is a copy of the old Cocoon FormsMultiValueEditor, this code should be reworked/removed in the future.
 */
function DaisyFormsMultiValueEditor(id) {
    this.select = document.getElementById(id + ":input");
    this.entry = document.getElementById(id + ":entry");
    var self = this;
    this.entry.onkeypress = function(event) { return self.processInputKey(event); };
    this.select.onkeydown = function(event) { return self.processSelectKey(event); };
    this.select.onchange = function(event) { return self.processSelectChange(event); };

    var deleteEl = document.getElementById(id + ":delete");
    deleteEl.onclick = function() { self.deleteValues(); return false; };

    var upEl = document.getElementById(id + ":up");
    upEl.onclick = function() { self.moveUp(); return false; };

    var downEl = document.getElementById(id + ":down");
    downEl.onclick = function() { self.moveDown(); return false; };

    var onsubmitHandler = new Object();
    onsubmitHandler.forms_onsubmit = function () {
        self.selectAll();
    }
    cocoon.forms.addOnSubmitHandler(document.getElementById(id), onsubmitHandler);
}

/**
 * Key event handler for keypresses in the input box.
 */
DaisyFormsMultiValueEditor.prototype.processInputKey = function(event) {
    if (event == null) event = window.event; // Internet Explorer
    if (event.keyCode == 13 || event.keyCode == 10) {
        var entry = this.entry;
        var select = this.select;
        var newItem = entry.value;
        if (newItem == null || newItem == "")
            return false;
        // if ctrl+enter is pressed, the first selected item is replaced with the new value
        // (otherwise, the new value is appended at the end of the list)
        var replace = event.ctrlKey;
        var newItemPos = -1;
        for (var i = 0; i < select.options.length; i++) {
            if (select.options[i].selected && replace && newItemPos == -1)
                newItemPos = i;
            select.options[i].selected = false;
        }
        if (newItemPos == -1)
            newItemPos = select.options.length;
        select.options[newItemPos] = new Option(newItem, newItem, false, true);
        entry.value = "";
        return false;
    } else {
        return true;
    }
}

/**
 * Key event handler for keypresses in the select list.
 */
DaisyFormsMultiValueEditor.prototype.processSelectKey = function(event) {
    if (event == null) event = window.event; // Internet Explorer
    // 46 = delete key
    if (event.keyCode == 46) {
        this.deleteValues();
        return false;
    } else if (event.ctrlKey && event.keyCode == 38) {
        // key up = 38
        this.moveUp();
        return false;
    } else if (event.ctrlKey && event.keyCode == 40) {
        // key down = 40
        this.moveDown();
        return false;
    }
}


DaisyFormsMultiValueEditor.prototype.deleteValues = function() {
    var options = this.select.options;
    var i = 0;
    var lastRemovedItem = -1;
    while (i < options.length) {
        if (options[i].selected) {
            options[i] = null;
            lastRemovedItem = i;
        } else {
             i++;
        }
    }

    if (lastRemovedItem != -1) {
        if (options.length > lastRemovedItem) {
            options[lastRemovedItem].selected = true;
        } else if (lastRemovedItem - 1 >= 0) {
            options[lastRemovedItem - 1].selected = true;
        }
    }
}

DaisyFormsMultiValueEditor.prototype.processSelectChange = function() {
    var options = this.select.options;
    for (var i = 0; i < options.length; i++) {
        if (options[i].selected) {
            this.entry.value = options[i].value;
            break;
        }
    }
}

DaisyFormsMultiValueEditor.prototype.moveUp = function() {
    var options = this.select.options;
    if (options.length == 0)
        return;
    if (options[0].selected)
        return;

    for (var i = 0; i < options.length; i++) {
        if (options[i].selected) {
            var prev = this.cloneOption(options[i - 1]);
            var current = this.cloneOption(options[i]);
            options[i - 1] = current;
            options[i] = prev;
        }
    }
}

DaisyFormsMultiValueEditor.prototype.cloneOption = function(option) {
    return new Option(option.text, option.value, false, option.selected);
}

DaisyFormsMultiValueEditor.prototype.moveDown = function() {
    var options = this.select.options;
    if (options.length == 0)
        return;
    if (options[options.length - 1].selected)
        return;

    for (var i = options.length - 1; i >= 0; i--) {
        if (options[i].selected) {
            var next = this.cloneOption(options[i + 1]);
            var current = this.cloneOption(options[i]);
            options[i + 1] = current;
            options[i] = next;
        }
    }
}

DaisyFormsMultiValueEditor.prototype.selectAll = function() {
    var options = this.select.options;
    for (var i = 0; i < options.length; i++) {
        options[i].selected = true;
    }
}

var daisy;
if (!daisy) {
  daisy = {}
}

if (!daisy.tm) {
  daisy.tm = {}
}

/**
 *  Helper class for selecting a language and version for synced-with fields.  This class makes sure the correct
 *  languages are made available (and the wrong ones unavailable).
 *
 *  The actual values of syncedWith are stored in two hidden fields (syncedWithLanguageId and syncedWithVersionId).
 *
 *  The main idea is to keep the default browser refresh behaviour intact (firefox does not clear form values, IE does, other browsers: ??).
 *  For this, the actual selected language/version is stored in hidden fields.  When loading the page, the select elements are populated
 *  and set to match these values.
 *
 *  @param documentLanguageId: language of the variant being edited
 *  @param documentReferenceLanguage: used for knowing if the current variant is the reference language,
 *             if a select with id "referenceLanguageId:input" is present, it takes precedence over the value of startReferenceLanguageId
 *
 *  @param availableLanguageVariants: a list of languages representing which language variants exist within this variant's branch
 *  @param hideWhenInactive: if true and if there is no reference language or this document is the reference language, the 'synced with' fields are set to -1 and hidden
 */
daisy.tm.EditorHelper = function(documentLanguageId, documentReferenceLanguageId, availableLanguageVariants, hideWhenInactive) {
  this.documentLanguageId = documentLanguageId;
  this.documentReferenceLanguageId = documentReferenceLanguageId; 
  this.availableLanguageVariants = availableLanguageVariants;
  this.hideWhenInactive = hideWhenInactive;

  this.availableLanguageVariantMap = {};
  
  this.i18n_bundle = dojo.i18n.getLocalization("daisy.widget", "messages");

  this.languageInput = dojo.byId('syncedWithLanguageId');
  this.languageSelect = dojo.byId('syncedWithLanguage');
  this.versionInput = dojo.byId('syncedWithVersionId');
  this.versionSelect = dojo.byId('syncedWithVersion');
  this.referenceLanguageSelect = document.getElementById("referenceLanguageId:input");
  
  while (this.languageSelect.options.length > 0) {
    this.languageSelect.remove(0);
  }

  daisy.html.addOption(this.languageSelect, -1, this.i18n('none'));

  for (var i = 0; i < this.availableLanguageVariants.length; i++) {
    var variant = this.availableLanguageVariants[i];
    if (variant.languageId == this.documentLanguageId)
      continue;
    
    this.hasSyncedWithCandidates = true;
    daisy.html.addOption(this.languageSelect, variant.languageId, variant.languageName);
    this.availableLanguageVariantMap['lang_'+variant.languageId] = variant;
     
  }
  
  this.updateEditor();
  this.updateVersionIds();

  if (this.referenceLanguageSelect) {
    dojo.event.connect(this.referenceLanguageSelect, 'onchange', dojo.lang.hitch(this, this.updateEditor));
  }
  dojo.event.connect(this.languageSelect, 'onchange', dojo.lang.hitch(this, this.syncedWithLanguageChanged));
  dojo.event.connect(this.versionSelect, 'onchange', dojo.lang.hitch(this, this.syncedWithVersionChanged));
}

daisy.tm.EditorHelper.prototype.setSyncedWithSelection = function(languageId, versionId) {
  this.languageInput.value = languageId;
  this.versionInput.value = versionId;
  this.updateVersionIds();
  this.updateSyncedWithSelects();
}
        
daisy.tm.EditorHelper.prototype.syncedWithLanguageChanged = function() {
  this.languageInput.value = this.languageSelect.options[this.languageSelect.selectedIndex].value;
  var variant = this.availableLanguageVariantMap['lang_' + this.languageInput.value];
  if (variant) {
    this.versionInput.value = variant.lastVersionId;
  } else {
    this.versionInput.value = -1;
  }

  this.updateVersionIds();
  this.onChange();
}

daisy.tm.EditorHelper.prototype.syncedWithVersionChanged = function() {
  this.versionInput.value = this.versionSelect.options[this.versionSelect.selectedIndex].value;
  this.onChange();
}
      
daisy.tm.EditorHelper.prototype.updateVersionIds = function() {
  while (this.versionSelect.options.length > 0) {
    this.versionSelect.remove(0);
  }
  
  var variant = this.availableLanguageVariantMap['lang_'+this.languageInput.value];
  
  if (!variant) {
    daisy.html.addOption(this.versionSelect, -1, "---");
    this.versionSelect.disabled = true;
    return; 
  }
  this.versionSelect.disabled = false;
  
  for (var i = variant.lastVersionId; i > 0; i--) {
    var opt;
    if (i == variant.liveVersionId) {
      opt = daisy.html.addOption(this.versionSelect, i, "" + i + " (" + this.i18n("live") + ")");
    } else {
      opt = daisy.html.addOption(this.versionSelect, i, i);
    }

    if (i == this.versionInput.value) {
      this.versionSelect.selectedIndex = opt.index;
    }
  }
}
      
daisy.tm.EditorHelper.prototype.getReferenceLanguageId = function() {
  if (this.referenceLanguageSelect) {
    return this.referenceLanguageSelect.options[this.referenceLanguageSelect.selectedIndex].value;
  } else {
    return this.documentReferenceLanguageId; 
  }
}
      
daisy.tm.EditorHelper.prototype.updateEditor = function() {
  var refLangId = this.getReferenceLanguageId();
  
  var syncedWithActive = this.hasSyncedWithCandidates && (refLangId != -1) && (refLangId != this.documentLanguageId);

  this.updateSyncedWithSelects();
  
  if (this.hideWhenInactive) {
    if (refLangId == -1 || this.documentLanguageId == refLangId) {
      this.languageInput.value = -1;
      this.versionInput.value = -1;
      dojo.html.hide('syncedWithContainer');
    } else {
      dojo.html.show('syncedWithContainer');
    }
  }
  
}
      
daisy.tm.EditorHelper.prototype.updateSyncedWithSelects = function() {
  daisy.html.selectOption(this.languageSelect, this.languageInput.value);
  daisy.html.selectOption(this.versionSelect, this.versionInput.value);
}
      
daisy.tm.EditorHelper.prototype.initHiddenSyncedWith = function() {
  if (this.languageInput.value == -1) {
    // the user has never specified a choice 
    var refLangId = this.getReferenceLanguageId();
    if (refLangId == -1 || refLangId == this.documentLanguageId) {
      this.languageInput.value = this.languageSelect.options[0].value;
    } else {
      this.languageInput.value = refLangId;
    }
    var variant = this.availableLanguageVariantMap['lang_' + this.languageInput.value];
    if (variant) {
      this.versionInput.value = variant.lastVersionId;
    } else {
      this.versionInput.value = -1;
    } 
    this.updateVersionIds();
  }
}

daisy.tm.EditorHelper.prototype.i18n = function(key) {
    var translated = this.i18n_bundle[key];
    if (translated == null)
        return key;
    else
        return translated;
}

// does nothing, serves as an event handler
daisy.tm.EditorHelper.prototype.onChange = function() {
}

function openMultiValueDocBrowserDialog(targetLinkField, configName, whereclause) {
   var processResult = function(params) {
        var tlf = $(targetLinkField);
        tlf.empty();
        for (var i=0;i<params.length;i++) {
          var selected = params[i];
          var opt = $(document.createElement("option"));
          opt.text(selected.docName);
          var url = selected.url;
          var docBranch = ""+getBranchId();
          var docLanguage = ""+getLanguageId();
          opt.attr('value',url);
          tlf.append(opt);
        }
    }
    var mountPoint = daisy.mountPoint;
    var siteName = daisy.site.name;

    var options = $(targetLinkField).children("option");
    var initialSelection = [];
    options.each(function(idx) {
        if(this.value){
            var linkArray = this.value.split(" / ");
            return initialSelection.push(daisy.util.parseDaisyLink(linkArray[linkArray.length-1]));
        }
    });
   
    var dialogParams = { "daisyMountPoint" : mountPoint, "daisySiteName" : siteName, "multiSelect" : true,
      initialSelection: initialSelection
    };
    if(whereclause && whereclause!='')
      dialogParams.whereclause = whereclause;

    daisy.dialog.popupDialog(mountPoint + "/" + siteName + "/editing/documentBrowser?branch=" + getBranchId() + "&language=" + getLanguageId()+"&config="+configName,
         processResult, dialogParams);

}