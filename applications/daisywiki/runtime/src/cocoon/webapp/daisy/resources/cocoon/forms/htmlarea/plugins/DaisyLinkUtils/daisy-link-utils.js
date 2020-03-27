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

dojo.require("daisy.util");

function DaisyLinkUtils(editor, params) {
    this.editor = editor;
    var cfg = editor.config;
    var toolbar = cfg.toolbar;
    var self = this;
    var i18n = DaisyLinkUtils.I18N;
    var plugin_config = params[0];

    cfg.registerButton("daisy-create-attachment", i18n["hint.create-attachment-link"], editor.imgURL("attach.gif", "DaisyLinkUtils"), false,
               function(editor, id) {
                   self.createAttachment(editor, id);
               }, null);

    cfg.registerButton("daisy-browse-link", i18n["hint.create-link-by-searching"], editor.imgURL("browse-link.gif", "DaisyLinkUtils"), false,
               function(editor, id) {
                   self.browseLink(editor, id);
               }, null);

    cfg.registerButton("daisy-create-link", i18n["hint.create-link"], editor.imgURL("link.gif", "DaisyLinkUtils"), false,
               function(editor, id) {
                   self.createLink(editor, id);
               }, null);

    cfg.registerButton("daisy-unlink", i18n["hint.remove-link"], editor.imgURL("unlink.gif", "DaisyLinkUtils"), false,
               function(editor, id) {
                   self.removeLink(editor, id);
               }, null);

    cfg.registerButton("daisy-browse-include", i18n["hint.browse-include"], editor.imgURL("browse-include.gif", "DaisyLinkUtils"), false,
               function(editor, id) {
                   self.browseInclude(editor, id);
               }, null);

    cfg.registerButton("daisy-edit-id", i18n["hint.edit-id"], editor.imgURL("edit-id.gif", "DaisyLinkUtils"), false,
               function(editor, id) {
                   self.editId(editor, id);
               }, null);

    cfg.registerButton("daisy-link-to-new", i18n["hint.link-to-new"], editor.imgURL("link-to-new.gif", "DaisyLinkUtils"), false,
               function(editor, id) {
                   self.linkToNew(editor, id);
               }, null);

    cfg.registerButton("daisy-open", i18n["hint.open"], editor.imgURL("open.gif", "DaisyLinkUtils"), false,
               function(editor, id) {
                   self.open(editor, id);
               }, null);

    cfg.registerButton("daisy-insert-include", i18n["hint.insert-include"], editor.imgURL("insert_include.gif", "DaisyLinkUtils"), false,
               function(editor, id) {
                   self.insertInclude(editor, id);
               }, null);

    cfg.registerButton("daisy-include-settings", i18n["hint.include-settings"], editor.imgURL("include_settings.gif", "DaisyLinkUtils"), false,
               function(editor, id) {
                   self.editInclude(editor, id);
               }, null);

    cfg.registerButton("daisy-insert-query", i18n["hint.insert-query"], editor.imgURL("insert_query.gif", "DaisyLinkUtils"), false,
               function(editor, id) {
                   self.insertQuery(editor, id);
               }, null);

    cfg.registerButton("daisy-insert-query-include", i18n["hint.insert-query-include"], editor.imgURL("insert_query_include.gif", "DaisyLinkUtils"), false,
               function(editor, id) {
                   self.insertQueryInclude(editor, id);
               }, null);

    cfg.registerButton("daisy-load-include-previews", i18n["hint.refresh-include-previews"], editor.imgURL("refresh_includes.gif", "DaisyLinkUtils"), false,
               function(editor, id) {
                   self.loadIncludePreviews(editor, id);
               }, null);

    cfg.registerButton("daisy-remove-include-previews", i18n["hint.remove-include-previews"], editor.imgURL("remove_include_previews.gif", "DaisyLinkUtils"), false,
               function(editor, id) {
                   self.removeAllIncludePreviews(editor, id);
               }, null);
};

DaisyLinkUtils._pluginInfo = {
    name          : "DaisyLinkUtils",
    version       : "1.0",
    developer     : "Outerthought",
    developer_url : "http://outerthought.org",
    c_owner       : "Outerthought",
    sponsor       : null,
    sponsor_url   : null,
    license       : "htmlArea"
};


DaisyLinkUtils.prototype.createAttachment = function(editor, id) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    var parent = editor.getParentElement();
    if (parent != null && parent.tagName.toLowerCase() == "a") {
        var href = parent.href;
        if (href != undefined && href != "") {
            if (href.match(daisyUrlRegexp)) {
                daisyOpenLink(href, editor.daisyMountPoint, editor.daisySiteName);
                return;
            }
        }
        alert(DaisyLinkUtils.I18N["attach-move-cursor-outside-link"]);
        return;
    }

    var dialogParams = { "daisyMountPoint" : editor.daisyMountPoint, "daisySiteName" : editor.daisySiteName };
    var self = this;

    daisy.dialog.popupDialog(editor.daisyMountPoint + "/" + editor.daisySiteName + "/editing/upload?documentType=Attachment&partType=AttachmentData&branch=" + getBranchId() + "&language=" + getLanguageId(),
        function(params) {
            var url = "daisy:" + params.docId;
            var branch = params.branchId != editor.daisyDocumentBranchId ? params.branch : "";
            var language = params.languageId != editor.daisyDocumentLanguageId ? params.language : "";
            if (branch != "" || language != "") {
                url = url + "@" + branch
                if (language != "")
                    url = url + ":" + language;
            }

            self.createOrUpdateLink(editor, params.name, url);
        }, dialogParams);
}

DaisyLinkUtils.prototype.linkToNew = function(editor, id) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    var dialogParams = {};
    var self = this;

    daisy.dialog.popupDialog(editor.daisyMountPoint + "/" + editor.daisySiteName + "/editing/createPlaceholder?branch=" + getBranchId() + "&language=" + getLanguageId() + "&documentType=" + getDocumentTypeId(),
        function(params) {
            var url = "daisy:" + params.docId;
            var branch = params.branchId != editor.daisyDocumentBranchId ? params.branch : "";
            var language = params.languageId != editor.daisyDocumentLanguageId ? params.language : "";
            if (branch != "" || language != "") {
                url = url + "@" + branch
                if (language != "")
                    url = url + ":" + language;
            }

            self.createOrUpdateLink(editor, params.name, url);
        }, dialogParams);
}

DaisyLinkUtils.prototype.browseLink = function(editor, id) {
    var dialogParams = { "daisyMountPoint" : editor.daisyMountPoint, "daisySiteName" : editor.daisySiteName };
    var self = this;

    daisy.dialog.popupDialog(editor.daisyMountPoint + "/" + editor.daisySiteName + "/editing/documentBrowser?branch=" + getBranchId() + "&language=" + getLanguageId(),
        function(params) {
            self.createOrUpdateLink(editor, params.docName, params.url);
        }, dialogParams);
}

DaisyLinkUtils.prototype.createOrUpdateLink = function(editor, linkText, url) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    if (daisyEmptySelection(editor)) {
        var link = editor._doc.createElement("a");
        var text = editor._doc.createTextNode(linkText);
        link.appendChild(text);
        link.href = url;
        link.setAttribute("daisy-href", url);
        daisyInsertNode(link, editor);
    } else {
        this.defineLink(editor, url);
    }
}

DaisyLinkUtils.prototype.defineLink = function(editor, url) {
    editor._doc.execCommand("createlink", false, url);

    // try to locate just created link to set daisy-href attribute on it.
    var a = editor.getParentElement();
    if (!/^a$/i.test(a.tagName)) {
        if (HTMLArea.is_ie) {
            // Alternative way to locate node for IE 6/7
            // This case happens when a user makes a selection by double-clicking a word
            // (making the selection by click-draging or shift+arrows doesn't have this problem)
            var sel = editor._getSelection();
            var range = editor._createRange(editor._getSelection());
            range.collapse(true);
            range.move("character");
            range.select();
            a = editor.getParentElement();
        } else {
            // alternative way to locate <a> node for Firefox 1.5 (not needed anymore for Firefox 2.0)
            var sel = editor._getSelection();
            var range = editor._createRange(sel);
            a = range.startContainer;
            if (!/^a$/i.test(a.tagName))
                a = a.nextSibling;
        }
        if (!/^a$/i.test(a.tagName)) {
            // Don't show an alert-window since it might be shown under the dialog (esp. on IE/Windows)
            dojo.debug("defineLink: no <a> node found.");
            return;
        }
    }
    a.setAttribute("daisy-href", url);
}

DaisyLinkUtils.prototype.createLink = function(editor, id) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    var defaultUrl = "http://";
    var inLink = false;

    var link = editor.getParentElement();
    while (link != null && link.tagName.toLowerCase() != "a" && daisyIsInlineElement(link)) {
        link = link.parentNode;
    }
    if (link && link.tagName.toLowerCase() == "a") {
        inLink = true;
        if (HTMLArea.is_ie) {
            // extra parameter is necessary for IE to return the exact value
            // see http://msdn.microsoft.com/workshop/author/dhtml/reference/methods/getattribute.asp
            // otherwise for relative links you can get resolved URLs
            defaultUrl = link.getAttribute("href", 2);
        } else {
            defaultUrl = link.getAttribute("href");
        }
    }

    if (!inLink && daisyEmptySelection(editor)) {
        alert(DaisyLinkUtils.I18N["js.no-text-selected"]);
        return;
    }

    var dialogParams = { "url" : defaultUrl,
        "daisyMountPoint" : editor.daisyMountPoint,
        "daisySiteName" : editor.daisySiteName,
        "daisyDocumentBranchId": editor.daisyDocumentBranchId,
        "daisyDocumentLanguageId": editor.daisyDocumentLanguageId,
        "editor": editor};
    var self = this;

    daisy.dialog.popupDialog(daisyGetHtmlAreaDialog("DaisyLinkUtils", "link.html"),
        function(param) {
            if (!param) {    // user must have pressed Cancel
                return;
            }

            if (inLink) {
                link.setAttribute("href", param.url);
                link.setAttribute("daisy-href", param.url);
            } else {
                self.defineLink(editor, param.url);
            }
        }, dialogParams);
}

DaisyLinkUtils.prototype.removeLink = function(editor, id) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    editor.execCommand("unlink");
}

DaisyLinkUtils.prototype.browseInclude = function(editor, id) {
    editor.focusEditor();
    var parent = editor.getParentElement();
    if (parent == null)
        return;
    if (parent.tagName.toLowerCase() != "pre" || parent.className != "include") {
        // note: this can usually not occur since the toolbar button will be disabled
        //       if we're outside an include, so didn't do any effort to localize this message.
        alert(DaisyLinkUtils.I18N["js.cursor-not-inside-include"]);
        return;
    }

    var dialogParams = { "daisyMountPoint" : editor.daisyMountPoint, "daisySiteName" : editor.daisySiteName };

    daisy.dialog.popupDialog(editor.daisyMountPoint + "/" + editor.daisySiteName + "/editing/documentBrowser?enableFragmentId=false&branch=" + getBranchId() + "&language=" + getLanguageId(),
        function(params) {
            var childrenCount = parent.childNodes.length;
            for (var i = childrenCount - 1; i >=0; i--) {
                parent.removeChild(parent.childNodes[i]);
            }
            var textNode = parent.ownerDocument.createTextNode(params.url + " (" + params.docName + ")");
            parent.appendChild(textNode);
            // the purpose of using setTimeout is so that the code is executed in the context of the calling window
            editor._iframe.contentWindow.top.setTimeout(function() {DaisyLinkUtils.prototype.loadIncludePreviews(editor)}, 0);
        }, dialogParams);
}

DaisyLinkUtils.prototype.editId = function(editor, id) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    var idElements = ["p", "img", "pre", "table", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", "blockquote"];
    var parent = editor.getParentElement();

    var elements = [];
    while (parent != null && parent.nodeType == 1) {
        if (this.arrayContains(idElements, parent.tagName.toLowerCase()))
            elements.push(parent);
        parent = parent.parentNode;
    }

    if (elements.length == 0) {
        alert(DaisyLinkUtils.I18N["js.no-id-elements"]);
        return;
    }

    elements.reverse();

    var dialogParams = { "elements" : elements, "editorDocument" : editor._doc };

    daisy.dialog.popupDialog(daisyGetHtmlAreaDialog("DaisyLinkUtils", "edit-id.html"),
        function(param) {
            // nothing to do on return
        }, dialogParams);
}

DaisyLinkUtils.prototype.arrayContains = function(someArray, someValue) {
    for (var i = 0; i < someArray.length; i++) {
        if (someArray[i] == someValue)
            return true;
    }
    return false;
}

DaisyLinkUtils.prototype.insertInclude = function(editor, id) {
    var prefix = "daisy:";
    var insertIdText = "<enter document ID>";
    var element = this.insertIncludeElement(editor, "include", prefix + insertIdText);

    daisySelectText(editor, element, prefix.length, prefix.length + insertIdText.length);
}

DaisyLinkUtils.prototype.insertQuery = function(editor, id) {
    var part1 = "select name where ";
    var part2 = "<enter condition here>";
    var part3 = " order by name";
    var element = this.insertIncludeElement(editor, "query", part1 + part2 + part3);

    daisySelectText(editor, element, part1.length, part1.length + part2.length);
}

DaisyLinkUtils.prototype.insertQueryInclude = function(editor, id) {
    var part1 = "select name where ";
    var part2 = "<enter condition here>";
    var part3 = " order by name";
    var element = this.insertIncludeElement(editor, "query-and-include", part1 + part2 + part3);

    daisySelectText(editor, element, part1.length, part1.length + part2.length);
}

DaisyLinkUtils.prototype.insertIncludeElement = function(editor, className, elementText) {
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

    var containerElements = ["body", "td", "th", "li"];
    var parentNode = editor.getParentElement();
    while (!this.arrayContains(containerElements, parentNode.tagName.toLowerCase()) && daisyIsInlineElement(parentNode)) {
        parentNode = parentNode.parentNode;
    }

    var newElement;
    if (this.arrayContains(containerElements, parentNode.tagName.toLowerCase())) {
        editor.insertHTML("<pre class='" + className + "'>" + elementText.replace(/</g, "&lt;").replace(/>/g, "&gt;") + "</pre>");
        newElement = editor.getParentElement();
    } else {
        var pre = editor._doc.createElement("PRE");
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

DaisyLinkUtils.prototype.loadIncludePreviews = function(editor, id) {
    // The daisyLoadingIncludePreviews boolean indicates if a preview loading operation
    // is already in progress, if so we don't do it a second time
    if (editor.daisyLoadingIncludePreviews != undefined && editor.daisyLoadingIncludePreviews)
        return;

    this.updateLoadingIncludePreviewState(true, editor);

    try {
        // First search all include instructions
        var includeElements = [];
        this.getIncludeElements(editor._doc.documentElement, includeElements);
        var includes = [];
        for (var i = 0; i < includeElements.length; i++) {
            var includeInfo = new Object();
            includeInfo["htmlElement"] = includeElements[i];
            var include = this.getIncludeText(includeElements[i]);
            if (include.match(daisyUrlRegexp)) {
                var docId = RegExp.$1;
                var branch = RegExp.$2;
                var language = RegExp.$3;
                var version = RegExp.$4;
                if (branch == null || branch == "")
                    branch = editor.daisyDocumentBranchId;
                if (language == null || language == "")
                    language = editor.daisyDocumentLanguageId;
                if (version == null || version == "")
                    version = "live";
                includeInfo["documentId"] = docId;
                includeInfo["branch"] = branch;
                includeInfo["language"] = language;
                includeInfo["version"] = version;
                includeInfo["valid"] = true;
            } else {
                // we also record invalid inclusions, in order to tell the user this,
                // so that he doesn't have to wonder why the include preview didn't
                // do anything.
                includeInfo["valid"] = false;
            }
            includes.push(includeInfo);
        }

        if (includes.length == 0) {
            this.updateLoadingIncludePreviewState(false, editor);
            return;
        }

        // Build parameters
        var params = {};
        for (var i = 0; i < includes.length; i++) {
            var includeInfo = includes[i];
            var prefix = "preview." + (i + 1) + ".";
            params[prefix + "documentId"] = includeInfo.documentId;
            params[prefix + "branch"] = includeInfo.branch;
            params[prefix + "language"] = includeInfo.language;
            params[prefix + "version"] = includeInfo.version;
        }

        var misc = this;

        // launch request (this happens asynchronously)
        var dojoRequest = dojo.io.bind({
           url: "includePreviews",
           encoding: "UTF-8",
           method: "POST", /* Need to use post since the amount of parameters can get too large when there are many includes */
           content: params,
           load: function(type, data, evt) {
               try {
                   // Result document from the server has the structure
                   //  <includePreviews>
                   //    <includePreview>...</includePreview>
                   //    <includePreview>...</includePreview>
                   //  </includePreviews>
                   // The include previews are returned in the same order as requested,
                   // if there would be an error with one of them (e.g. document does
                   // not exist) the corresponding include preview tag will still be
                   // there and contain a description of the error.

                   // Store the previews in the corresponding includeInfo objects
                   var children = data.documentElement.childNodes;
                   var c = 0;
                   for (var i = 0; i < children.length; i++) {
                       var child = children[i];
                       if (child.nodeType == dojo.dom.ELEMENT_NODE && daisyGetLocalName(child) == "includePreview") {
                           includes[c].preview = child;
                           c++;
                       }
                   }

                   // Insert the preview elements
                   for (var i = 0; i < includes.length; i++) {
                       var includeInfo = includes[i];
                       misc.removeIncludePreview(includeInfo["htmlElement"]);
                       var previewEl = editor._doc.createElement("div");
                       previewEl.className = daisyIncludePreviewClass;
                       if (includeInfo.valid) {
                           if (includeInfo.preview == null) {
                               // this is normally not possible, but handle it anyway
                               previewEl.appendChild(editor._doc.createTextNode(DaisyLinkUtils.I18N["js.include-preview-missing"]));
                           } else {
                               misc.importChildren(includeInfo.preview, previewEl, editor._doc);
                               misc.loadImagesInIncludePreview(previewEl, includeInfo.branch, includeInfo.language, editor);
                           }
                       } else {
                           previewEl.appendChild(editor._doc.createTextNode(DaisyLinkUtils.I18N["js.include-preview-invalid"]));
                       }
                       includeInfo["htmlElement"].appendChild(previewEl);
                   }
               } finally {
                   misc.updateLoadingIncludePreviewState(false, editor);
               }
           },
           error: function(type, error) {
               misc.updateLoadingIncludePreviewState(false, editor);
               alert(DaisyLinkUtils.I18N["js.include-preview-load-error"] + error.message);
           },
           mimetype: "text/xml"
        });
    } catch (error) {
        this.updateLoadingIncludePreviewState(false, editor);
        throw error;
    }

}

DaisyLinkUtils.prototype.updateLoadingIncludePreviewState = function(value, editor) {
    editor.daisyLoadingIncludePreviews = value;
    editor.updateToolbar();
}

DaisyLinkUtils.prototype.importChildren = function(fromElement, toElement, toDocument) {
    if (HTMLArea.is_ie) {
        toElement.innerHTML = fromElement.xml;
    } else {
        var children = fromElement.childNodes;
        for (var i = 0; i < children.length; i++) {
            toElement.appendChild(this.importNode(children[i], toDocument));
        }
    }
}

DaisyLinkUtils.prototype.importNode = function(node, targetDoc) {
    // loosely based on code from Cocoon's insertion.js
    switch(node.nodeType) {
        case dojo.dom.ELEMENT_NODE:
            var element = targetDoc.createElement(node.nodeName);
            var attrs = node.attributes;
            for (var i = 0; i < attrs.length; i++) {
                var attr = attrs[i];
                element.setAttribute(attr.nodeName, attr.nodeValue);
            }
            var children = node.childNodes;
            for (var j = 0; j < children.length; j++) {
                var imported = this.importNode(children[j], targetDoc);
                if (imported) element.appendChild(imported);
            }
            return element;
        break;

        case dojo.dom.TEXT_NODE:
            return targetDoc.createTextNode(node.nodeValue);
        break;

        case dojo.dom.CDATA_SECTION_NODE:
            return targetDoc.createTextNode(node.nodeValue);
        break;
    }
}

DaisyLinkUtils.prototype.removeIncludePreview = function(element) {
    var childNodes = element.childNodes;
    for (var i = childNodes.length - 1; i >= 0; i--) {
        var node = childNodes.item(i);
        if (node.nodeType == dojo.dom.ELEMENT_NODE) {
            if (node.tagName.toLowerCase() == "div" && node.className == daisyIncludePreviewClass) {
                element.removeChild(node);
            } else {
                this.removeIncludePreview(node);
            }
        }
    }
}

/**
  * Searches all <pre class="include"> elements without looking
  * at nested pre's (usually not the case, but could happen)
  * and without looking inside include previews.
  */
DaisyLinkUtils.prototype.getIncludeElements = function(element, list) {
    var childNodes = element.childNodes;
    for (var i = 0; i < childNodes.length; i++) {
        var node = childNodes.item(i);
        if (node.nodeType == dojo.dom.ELEMENT_NODE) {
            if (node.tagName.toLowerCase() == "pre" && node.className == "include") {
                list.push(node);
            } else if (node.tagName.toLowerCase() == "div" && node.className == daisyIncludePreviewClass) {
                // do nothing
            } else {
                // search among children
                this.getIncludeElements(node, list);
            }
        }
    }
}

DaisyLinkUtils.prototype.loadImagesInIncludePreview = function(element, documentBranch, documentLanguage, editor) {
    var images = element.getElementsByTagName("IMG");
    for (var i = 0; i < images.length; i++) {
        var image = images[i];
        if (image.src != null && image.src.match(daisyUrlRegexp)) {
            var imageDocId = RegExp.$1;
            var branch = RegExp.$2;
            if (branch == null || branch == "")
                branch = documentBranch;
            var language = RegExp.$3
            if (language == null || language == "")
                language = documentLanguage;
            var version = RegExp.$4;
            if (version == null || version == "")
                version = "live";
            image.src = editor.daisyMountPoint + "/" + editor.daisySiteName + "/" + imageDocId + "/version/" + version + "/part/ImageData/data?branch=" + branch + "&language=" + language;
        }
    }
}

/*
 * Gets the include text in an include instruction (<pre class="include">).
 * This returns the text until the first space character, and ignores the
 * include preview.
 */
DaisyLinkUtils.prototype.getIncludeText = function(element) {
    var text = "";
    var childNodes = element.childNodes;

    for (var i = 0; i < childNodes.length; i++) {
        var node = childNodes.item(i);
        if (node.nodeType == 1) { // element node
            if (node.tagName.toLowerCase() == "div" && node.className == daisyIncludePreviewClass) {
                // skip content of include preview
            } else {
                text = text + this.getIncludeText(node);
            }
        } else if (node.nodeType == 3) { // text node
            text = text + node.nodeValue;
        }
        text = daisyTrim(text);
        var spacePos = text.indexOf(" ");
        if (spacePos != -1)
            return text.substring(0, spacePos);
    }

    text = daisyTrim(text);
    var spacePos = text.indexOf(" ");
    return spacePos != -1 ? text.substring(0, spacePos) : text;
}

DaisyLinkUtils.prototype.removeAllIncludePreviews = function(editor) {
    this.removeIncludePreview(editor._doc.documentElement);
}

DaisyLinkUtils.prototype.editInclude = function(editor) {
    var parent = editor.getParentElement();
    var includeElement;

    while (parent != null && parent.nodeType == 1) {
        var tagName = parent.tagName.toLowerCase();
        if (tagName == "pre" && (parent.className == "include" || parent.className == "query-and-include")) {
            includeElement = parent;
            break;
        }
    }

    if (includeElement == null) {
        alert("No include instruction found.");
    }

    var shiftHeadings = includeElement.getAttribute("daisy-shift-headings");

    var dialogParams = { "shiftHeadings" : shiftHeadings};
    var self = this;

    daisy.dialog.popupDialog(daisyGetHtmlAreaDialog("DaisyLinkUtils", "include_settings.html"),
        function(param) {
            if (!param) {    // user must have pressed Cancel
                return;
            }

            if (param.shiftHeadings != null)
                includeElement.setAttribute("daisy-shift-headings", param.shiftHeadings);
            else
                includeElement.removeAttribute("daisy-shift-headings");

        }, dialogParams);

}

DaisyLinkUtils.prototype.open = function(editor, id) {
    var parent = editor.getParentElement();

    while (parent != null && parent.nodeType == 1) {
        var tagName = parent.tagName.toLowerCase();
        if (tagName == "a") {
            var href = parent.href;
            if (href != undefined && href != "") {
                daisyOpenLink(href, editor.daisyMountPoint, editor.daisySiteName);
                break;
            }
        } else if (tagName == "img") {
            var src = parent.getAttribute("daisy-src");
            if (src.match(daisyUrlRegexp)) {
                daisyOpenLink(src, editor.daisyMountPoint, editor.daisySiteName);
                break;
            }
        } else if (tagName == "pre") {
            var className = parent.className;
            if (className == "query" || className == "query-and-include") {
                var query = daisyElementText(parent);
                var url = editor.daisyMountPoint + "/" + editor.daisySiteName + "/querySearch?daisyquery=" + encodeURIComponent(query);
                if (getDocumentId()!='new') {                
                  var contextDoc = "daisy:" + getDocumentId() + "@" + getBranchId() + ":" + getLanguageId();
                  url += "&contextDocument="+encodeURI(contextDoc);
                }
                window.open(url);
                break;
            } else if (className == "include") {
                var link = this.getIncludeText(parent);
                if (link.match(daisyUrlRegexp)) {
                    daisyOpenLink(link, editor.daisyMountPoint, editor.daisySiteName);
                } else {
                    alert("No valid include instruction found.");
                }
                break;
            }
        }
        parent = parent.parentNode;
    }
}

DaisyLinkUtils.prototype.onUpdateToolbar = function() {
    var parent = this.editor.getParentElement();
    var parentName = parent.tagName.toLowerCase();

    var enableBrowseInclude = (parentName == "pre" && parent.className == "include");
    this.editor._toolbarObjects["daisy-browse-include"].state("enabled", enableBrowseInclude, true);

    var enabledIncludeSettings = parentName == "pre" && (parent.className == "include" || parent.className == "query-and-include");
    this.editor._toolbarObjects["daisy-include-settings"].state("enabled", enabledIncludeSettings, true);

    this.editor._toolbarObjects["daisy-load-include-previews"].state("enabled", this.editor.daisyLoadingIncludePreviews == undefined || !this.editor.daisyLoadingIncludePreviews, true);
}

