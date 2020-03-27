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

function showHelpPopup(helpDiv) {
    var helpWindow = window.open("", "",
               "toolbar=no,menubar=no,personalbar=no,width=400,height=200,left=20,top=40" +
                  ",scrollbars=yes,resizable=yes");

    var doc = helpWindow.document;
    doc.open();
    doc.write("<html><body>");
    doc.write(helpDiv.innerHTML);
    doc.write("</body></html>");
    doc.close();
}

/**
 * Toggles an element between being displayed and not.
 */
function toggleDisplay(id) {
    var el = document.getElementById(id);
    el.style.display = el.style.display == 'none' ? '' : 'none';
}

/**
 * Changes the class of an <a> element in the navigation tree from
 * navnode-open to navnode-closed and vice versa, taking care that
 * the <a> element may have more then one class name.
 */
function toggleNavClassName(element) {
    var className = element.className;
    if (className.indexOf("navnode-open") != -1) {
        element.className = className.replace(/navnode-open/, "navnode-closed");
    } else {
        element.className = className.replace(/navnode-closed/, "navnode-open");
    }
}

/**
 * Collapses/expands a node in the navigation tree, which consists
 * of displaying/hiding the child <ul> and changing the class of the
 * <a> link. Works also in case the navigation tree is server-side
 * contextualized, i.e. not fully loaded in the client.
 */
function collapseExpandNavNode(linkElement) {
    var navChild = null;
    var children = linkElement.parentNode.childNodes;
    for (var i = 0; i < children.length; i++) {
        if (children[i].nodeType == 1 && children[i].tagName.toLowerCase() == "ul") {
            navChild = children[i];
            break;
        }
    }

    if (navChild == null) {
        // follow the link, causing the node to be expanded server-side
        return true;
    }

    navChild.style.display = navChild.style.display == 'none' ? '' : 'none';
    toggleNavClassName(linkElement);
    return false;
}

/**
 * Gets the text content of an element.
 */
function daisyElementText(element) {
    var text = "";
    var childNodes = element.childNodes;
    for (var i = 0; i < childNodes.length; i++) {
        var node = childNodes.item(i);
        if (node.nodeType == 1) { // element node
            if (node.tagName.toLowerCase() == "br") {
                text = text + "\n";
            }
            text = text + daisyElementText(node);
        } else if (node.nodeType == 3) { // text node
            text = text + node.nodeValue;
        }
    }
    return text;
}

function daisyPushOnLoad(someFunction) {
    if (window.dojo != undefined) {
        dojo.addOnLoad(someFunction);
    } else if (window.onload != undefined && typeof window.onload == "function") {
        var currentFunction = window.onload;
        window.onload = function() {
            currentFunction();
            someFunction();
        }
    } else {
        window.onload = someFunction;
    }
}
