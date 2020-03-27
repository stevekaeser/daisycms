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
 * Stuff shared by multiple daisy htmlarea plugins.
 */

function daisySearchParentBlockElement(editor) {
    var element = editor.getParentElement();
    while (element != null && daisyIsInlineElement(element)) {
        element = element.parentNode;
    }
    return element;
}

function daisySearchParentTableCell(editor) {
    var element = editor.getParentElement();
    while (element != null && element.nodeType == dojo.dom.ELEMENT_NODE) {
        var tagName = element.tagName.toLowerCase();
        if (tagName == "td" || tagName == "th")
            return element;
        element = element.parentNode;
    }
    return null;
}

function daisyInsertNode(node, editor) {
    var createdNode;
    if (HTMLArea.is_ie) {
        // To find back the created node, we temporarily assign
        // a unique ID to the node
        var origId = node.id;
        var idPrefix = "daisy-temp-node-";
        var idCounter = 0;
        while (editor._doc.getElementById(idPrefix + idCounter) != null) {
            idCounter++;
        }
        node.id = idPrefix + idCounter;

        var sel = editor._getSelection();
        var range = editor._createRange(sel);
        range.pasteHTML(node.outerHTML);

        node = editor._doc.getElementById(node.id);
        node.removeAttribute("id");
        if (origId != null)
            node.id = origId;
        createdNode = node;
    } else {
        editor.insertNodeAtSelection(node);
        createdNode = node;
    }
    return createdNode;
}

function daisyEmptySelection(editor) {
    var selection = editor._getSelection();
    if (HTMLArea.is_ie) {
        return selection.type == "None";
    } else {
        var range = editor._createRange(selection);
        return range.startContainer == range.endContainer && range.startOffset == range.endOffset;
    }
}

function daisyClearSelection(editor) {
    if (!daisyEmptySelection(editor)) {
        if (HTMLArea.is_ie) {
            var range = editor._createRange(editor._getSelection());
            range.collapse(true);
            range.select();
        } else {
            editor._createRange(editor._getSelection()).collapse(true);
        }
    }    
}

/**
 * Checks that a range contains only inline elements.
 *
 * Returns:
 *    1 - if range is empty
 *    2 - if range contains only inline tags
 *    3 - all other cases
 */
function daisyCheckInlineRange(range, doc) {
    if (HTMLArea.is_ie) {
        var htmlText = range.htmlText;
        if (htmlText == null || htmlText == "")
            return 1;
        return daisyCheckOnlyInlineTags(htmlText) ? 2 : 3;
    } else {
        if (range.collapsed)
            return 1;
        var fragment = range.cloneContents();
        var treeWalker = doc.createTreeWalker(fragment, NodeFilter.SHOW_ELEMENT, null, true);
        var node = treeWalker.nextNode();
        while (node != null) {
            if (node.nodeType == 1 && !daisyIsInlineElement(node)) {
                return 3;
            }
            node = treeWalker.nextNode();
        }
        return 2;
    }
}

/**
* This function checks that a piece of HTML text only contains inline tags.
*/
function daisyCheckOnlyInlineTags(text) {
    var tagRegExp = /((<([^ >]+))|(<\/([^ >]+)))/g; // matches tags, thus <... and </...
    var result;
    while ((result = tagRegExp.exec(text)) != null) {
        var tagName = result[3];
        if (tagName.charAt(0) == '/')
            tagName = tagName.substr(1);
        if (!daisyIsInlineTagName(tagName))
            return false;
    };
    return true;
}

function extractIdsFromDoc(doc) {
    var list = [];
    extractIdsFromEl(doc.documentElement, list);
    return list;
}

function extractIdsFromEl(element, list) {
    // don't process include previews
    if (element.className == daisyIncludePreviewClass)
        return;

    var id = element.getAttribute("id");
    if (id != null && id != "")
        list.push(id);
    var childNodes = element.childNodes;
    for (var i = 0; i < childNodes.length; i++) {
        if (childNodes[i].nodeType == 1) {
            extractIdsFromEl(childNodes[i], list);
        }
    }
}

function daisyOpenLink(link, mountPoint, siteName) {
    if (link.match(daisyUrlRegexp)) {
        var docId = RegExp.$1;
        var branch = RegExp.$2;
        var language = RegExp.$3;
        var versionId = RegExp.$4;
        // TODO add fragment id handling
        if (versionId != "")
            window.open(mountPoint + "/" + siteName + "/" + docId + "/version/" + versionId + getBranchLangQueryString(branch, language));
        else
            window.open(mountPoint + "/" + siteName + "/" + docId + getBranchLangQueryString(branch, language));
    } else {
        window.open(link);
    }
}

/**
 * trims a string
 */
function daisyTrim(value) {
    return value.replace(/^\s+|\s+$/g, '');
}

/**
 * Selects the specified range in the first child text node
 * of the given element.
 */
function daisySelectText(editor, node, start, end) {
    var textNode = null;
    if (node.nodeType == dojo.dom.TEXT_NODE) {
        textNode = node;
    } else {
        // search first child node
        var children = node.childNodes;
        for (var i = 0; i < children.length; i++) {
            if (children[i].nodeType == dojo.dom.TEXT_NODE) {
                textNode = children[i];
                break;
            }
        }
    }

    if (textNode == null) {
        return;
    }

    if (!HTMLArea.is_ie) {
        // Non-IE: use DOM API
        var sel = editor._getSelection();
        sel.removeAllRanges();
        var range = editor._doc.createRange();
        range.setStart(textNode, start);
        range.setEnd(textNode, end);
        sel.addRange(range);
    } else {
        // IE
        var sel = editor._getSelection();
        var range = editor._createRange(editor._getSelection());
        range.moveToElementText(textNode.parentNode);
        range.collapse(true);
        range.moveStart("character", start);
        range.moveEnd("character", end - start);
        range.select();
    }

    editor.updateToolbar();
}

/**
 * Calculates the path for displaying a HTMLArea plugin dialog.
 */
function daisyGetHtmlAreaDialog(plugin, dialog) {
    return daisy.mountPoint + "/" + daisy.site.name + "/dialog/" + "cocoon/forms/htmlarea/plugins/" + plugin + "/popups/" + dialog;
}