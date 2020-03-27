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
// TreeView class
//

/**
 * Constructor.
 */
function TreeView(tree, treeId) {
    this.tree = tree;
    this.treeContext = window[treeId + "_treeContext"];

    // create the iframe
    var iframeAnchor = document.getElementById(treeId + "_treeViewFrameAnchor");
    this.iframe = document.createElement("IFRAME");
    this.iframe.style.width = "100%";
    this.iframe.style.height = "250px";
    this.iframe.id = treeId + "_treeViewFrame";
    iframeAnchor.parentNode.replaceChild(this.iframe, iframeAnchor);

    // put document in iframe
    var iframeDoc = this.iframe.contentWindow.document;
    iframeDoc.open();
    iframeDoc.write("<html><head></head><body><div id='treeview'></div></body></html>");
    iframeDoc.close();

    // change CSS of the document
    var styleLink = iframeDoc.createElement("link");
    styleLink.setAttribute("type", "text/css");
    styleLink.setAttribute("rel", "stylesheet");
    styleLink.setAttribute("href", this.treeContext.resourcesPrefix + "treeview.css");
    var head = iframeDoc.documentElement.getElementsByTagName("head")[0];
    head.appendChild(styleLink);

    // other initialisation
    var divElement = this.iframe.contentWindow.document.getElementById("treeview");
    this.rootNode = new NodeView(tree.rootNode, divElement, null, this);
    this.selectionController = new SelectionController(this);
    this.selectionController.addEventListener(this);
    this.clipboard = null;
    this.keyMappings = new Object();

    // initialise actions
    this.undoAction = new UndoAction(this);

    // initialise state variables for selections made with shift + key up/down
    this.shiftMode = false;
    this.shiftSelectStart = null;
    // note: we need to use onkeydown instead of onkeypress because IE doens't report special
    // keys like the navigation keys in onkeypress.
    var self = this;
    iframeDoc.onkeydown = function(event) { return self.onkeydown(event); };
    iframeDoc.onkeypress = function(event) { return self.onkeypress(event); };
}

TreeView.prototype.connectToModel = function(tree) {
    this.tree = tree;

    var divElement = this.iframe.contentWindow.document.getElementById("treeview");
    var newDivElement = this.iframe.contentWindow.document.createElement("DIV");
    divElement.parentNode.replaceChild(newDivElement, divElement);
    divElement = newDivElement;
    divElement.setAttribute("id", "treeview");

    this.rootNode = new NodeView(tree.rootNode, divElement, null, this);
    this.clipboard = null;
    this.selectionController.setActive(null);

    this.undoAction.connectToModel(tree);
}

TreeView.prototype.show = function() {
    this.iframe.style.display = "";
}

TreeView.prototype.hide = function() {
    this.iframe.style.display = "none";
}

TreeView.prototype.isVisible = function() {
    return this.iframe.style.display != "none";
}

TreeView.prototype.setHeight = function(height) {
    this.iframe.style.height = height;
}

/**
 * Associates an action with an editor key. The key param should contain
 * a letter or a key code, possibly prepended by "c-" for keys that are used
 * in combination with the Ctrl key.
 */
TreeView.prototype.registerKeyMapping = function(key, action) {
    this.keyMappings[key] = action;
}

TreeView.prototype.onkeydown = function(event) {
    if (event == undefined)
        event = this.iframe.contentWindow.event;

    var handled = false;
    var activeNode = this.selectionController.activeNode;
    var keyChar = String.fromCharCode(event.keyCode).toLowerCase();

    if (event.ctrlKey && !event.shiftKey) {
        var action = this.keyMappings["c-" + keyChar];
        if (action == null)
            action = this.keyMappings["c-" + event.keyCode.toFixed(0)];

        if (action != null) {
            if (!this.treeContext.disabled || action.alwaysEnabled) { 
              action.perform();
            }
            handled = true;
        }
    } else if (event.shiftKey && !event.ctrlKey) {
        if (event.keyCode == 38) { // up
            if (!this.shiftMode) {
                this.shiftSelectStart = activeNode;
            }
            var newActiveNode = activeNode.getPrevious();
            this.selectionController.unselectAll();
            this.selectionController.selectBetween(this.shiftSelectStart, newActiveNode);
            this.selectionController.autoSelectChildren();
            this.selectionController.setActive(newActiveNode);
            this.assureVisible(newActiveNode, true);
            this.shiftMode = true;
            handled = true;
        } else if (event.keyCode == 40) { // down
            if (!this.shiftMode) {
                this.shiftSelectStart = activeNode;
            }
            var newActiveNode = activeNode.getNext();
            this.selectionController.unselectAll();
            this.selectionController.selectBetween(this.shiftSelectStart, newActiveNode);
            this.selectionController.autoSelectChildren();
            this.selectionController.setActive(newActiveNode);
            this.assureVisible(newActiveNode, true);
            this.shiftMode = true;
            handled = true;
        }
    } else if (!event.shiftKey && !event.ctrlKey) {
        var action = this.keyMappings[keyChar];
        if (action == null)
            action = this.keyMappings[event.keyCode.toFixed(0)];

        if (action != null) {
            if (!this.treeContext.disabled || action.alwaysEnabled) { 
              action.perform();
            }
            handled = true;
        } else if (event.keyCode == 40) { // key down
            if (activeNode != null) {
                var newActiveNode = activeNode.getNext();
                this.selectionController.unselectAll();
                newActiveNode.select();
                this.selectionController.setActive(newActiveNode);
                this.assureVisible(newActiveNode, false);
            }
            handled = true;
        } else if (event.keyCode == 38) { // key up
            if (activeNode != null) {
                var newActiveNode = activeNode.getPrevious();
                this.selectionController.unselectAll();
                newActiveNode.select();
                this.selectionController.setActive(newActiveNode);
                this.assureVisible(newActiveNode, true);
            }
            handled = true;
        } else if (event.keyCode == 36) { // home
            this.selectionController.unselectAll();
            this.rootNode.select();
            this.selectionController.setActive(this.rootNode);
            this.assureVisible(this.rootNode, true);
            handled = true;
        } else if (event.keyCode == 35) { // end
            this.selectionController.unselectAll();
            var newActiveNode = this.getLastNode(this.rootNode);
            newActiveNode.select();
            this.selectionController.setActive(newActiveNode);
            this.assureVisible(newActiveNode, false);
            handled = true;
        }
    }

    if (handled) {
        this.stopEvent(event);
        return false;
    } else {
        return true;
    }
}

TreeView.prototype.onkeypress = function(event) {
    if (event == undefined)
        event = this.iframe.contentWindow.event;

    var handled = false;
    if (event.keyCode == 40) { // key down
       handled = true;
    } else if (event.keyCode == 38) {
        handled = true;
    }

    if (handled) {
        this.stopEvent(event);
        return false;
    } else {
        return true;
    }
}

// copoied from HTMLArea
TreeView.prototype.stopEvent = function(ev) {
	if (TreeView.is_ie) {
		ev.cancelBubble = true;
		ev.returnValue = false;
	} else {
		ev.preventDefault();
		ev.stopPropagation();
	}
}

/**
 * Event handler function for the selection controller.
 */
TreeView.prototype.activeNodeChanged = function() {
    this.shiftMode = false;
}

TreeView.prototype.assureVisible = function(viewNode, top) {
    if (viewNode.getTop() > this.getScrollPos() && (viewNode.getTop() + viewNode.getHeight() < this.getScrollPos() + this.iframe.offsetHeight)) {
        return;
    }

    if (top) {
        this.iframe.contentWindow.scrollTo(0, viewNode.getTop() - viewNode.getHeight());
    } else {
        this.iframe.contentWindow.scrollTo(0, viewNode.getTop() - this.iframe.offsetHeight + viewNode.getHeight());
    }

}

TreeView.prototype.getScrollPos = function() {
    var scrollPos = this.iframe.contentWindow.pageYOffset; // Mozilla
    if (scrollPos == undefined) {
       var doc = this.iframe.contentWindow.document;
       if (doc.compatMode && doc.compatMode != "BackCompat")
           scrollPos = doc.documentElement.scrollTop;
       else
           scrollPos = doc.body.scrollTop;
    }
    return scrollPos;
}

TreeView.prototype.searchViewNode = function(treeNode) {
    return this.rootNode.searchViewNode(treeNode);
}

TreeView.prototype.getLastNode = function(startNode) {
    if (startNode.children == null || startNode.children.length == 0)
        return startNode;
    else
        return this.getLastNode(startNode.children[startNode.children.length - 1]);
}

// Browser identification (copied form HTMLArea)
TreeView.agt = navigator.userAgent.toLowerCase();
TreeView.is_ie  = ((TreeView.agt.indexOf("msie") != -1) && (TreeView.agt.indexOf("opera") == -1));


//
// NodeView class
//

function NodeView(treeNode, divElement, parent, treeView) {
    this.treeNode = treeNode;
    this.divElement = divElement;
    this.treeView = treeView;
    this.selected = false;
    this.parent = parent;

    this.treeNode.addEventListener(this);
    this.labelDiv = this.divElement.ownerDocument.createElement("DIV");
    this.divElement.appendChild(this.labelDiv);
    this.updateLabelDiv();

    if (this.treeNode.canHaveChildren()) {
        this.children = new Array();
        var treeNodes = this.treeNode.getChildren();
        for (var i = 0; i < treeNodes.length; i++) {
            var newDiv = this.divElement.ownerDocument.createElement("DIV");
            var childNodeView = new NodeView(treeNodes[i], newDiv, this, this.treeView);
            this.children[i] = childNodeView;
            this.divElement.appendChild(newDiv);
        }
    }

    this.divElement.style.paddingLeft = this.INDENT;

    var ikke = this;
    this.labelDiv.onclick = function(event) { return ikke.onClick(event); };
}

NodeView.prototype.INDENT = 20;
NodeView.prototype.SELECTED_BACKGROUND_COLOR = "#959aa6";
NodeView.prototype.SELECTED_TEXT_COLOR = "white";
NodeView.prototype.ACTIVE_BORDER = "1px solid black";

NodeView.prototype.updateLabelDiv = function() {
    var labelText = this.treeNode.getLabel();

    if (this.labelDiv.childNodes.length > 0)
        this.labelDiv.removeChild(this.labelDiv.firstChild);

    this.labelDiv.className = "label " + this.treeNode.getStyleClass(); 
    var labelSpan = this.labelDiv.ownerDocument.createElement("SPAN");
    labelSpan.className = this.treeNode.getStyleClass();
    var textNode = this.labelDiv.ownerDocument.createTextNode(labelText);
    labelSpan.appendChild(textNode);

    this.labelDiv.appendChild(labelSpan);
}

NodeView.prototype.nodeAdded = function(event) {
    var newDiv = this.divElement.ownerDocument.createElement("DIV");
    var newChild = new NodeView(event.newChild, newDiv, this, this.treeView);

    // insert new NodeView and the new div element
    if (event.position == this.children.length) {
        this.children[event.position] = newChild;
        this.divElement.appendChild(newDiv);
    } else if (event.position == 0) {
        this.children.unshift(newChild);
        this.divElement.insertBefore(newDiv, this.children[event.position + 1].divElement);
    } else {
        var beforeSlice = this.children.slice(0, event.position);
        var afterSlice = this.children.slice(event.position);
        beforeSlice.push(newChild);
        this.children = beforeSlice.concat(afterSlice);
        this.divElement.insertBefore(newDiv, this.children[event.position + 1].divElement);
    }
}

NodeView.prototype.nodeRemoved = function(event) {
    var child = this.children[event.position];
    this.divElement.removeChild(child.divElement);

    var beforeSlice = this.children.slice(0, event.position);
    var afterSlice = this.children.slice(event.position + 1);
    this.children = beforeSlice.concat(afterSlice);

    child.cleanup();
}

NodeView.prototype.nodeUpdated = function(event) {
    this.updateLabelDiv();
}

NodeView.prototype.select = function() {
    if (!this.selected) {
        this.labelDiv.style.backgroundColor = this.SELECTED_BACKGROUND_COLOR;
        this.labelDiv.style.color = this.SELECTED_TEXT_COLOR;
        this.selected = true;
    }
}

NodeView.prototype.unselect = function() {
    if (this.selected) {
        this.labelDiv.style.backgroundColor = "";
        this.labelDiv.style.color = "";
        this.selected = false;
    }
}

NodeView.prototype.onClick = function(event) {
    if (event == undefined)
        event = this.treeView.iframe.contentWindow.event;

    var selectionController = this.treeView.selectionController;
    if ((event.ctrlKey == false && event.shiftKey == false) || selectionController.activeNode == null) {
        selectionController.unselectAll();
        this.select();
        selectionController.setActive(this);
    } else if (event.ctrlKey == true) {
        if (this.selected) {
            this.unselect();
        } else {
            this.select();
        }
        selectionController.autoSelectChildren();
        selectionController.setActive(this);
    } else if (event.shiftKey == true) {
        selectionController.selectBetween(selectionController.activeNode, this);
        selectionController.setActive(this);
        selectionController.autoSelectChildren();
    }

    if (this.treeView.iframe.contentWindow.getSelection) {
        this.treeView.iframe.contentWindow.getSelection().collapseToStart();
    } else if (this.treeView.iframe.contentWindow.document.selection) {
        this.treeView.iframe.contentWindow.document.selection.empty();
    }

    return false;
}

NodeView.prototype.markActive = function() {
    this.labelDiv.style.border = this.ACTIVE_BORDER;
}

NodeView.prototype.unmarkActive = function() {
    this.labelDiv.style.border = "";
}

NodeView.prototype.cleanup = function() {
    this.treeNode.removeEventListener(this);
    if (this.children != null) {
        for (var i = 0; i < this.children.length; i++) {
            this.children[i].cleanup();
        }
    }
}

NodeView.prototype.getNext = function() {
    if (this.children != null && this.children.length > 0) {
        return this.children[0];
    } else {
        var nextSibling = this.getNextSiblingOrParentSibling();
        return nextSibling != null ? nextSibling : this;
    }
}

/**
 * Returns the next sibling, or if there is none, the next sibling of its
 * parent, and so on recursively. Returs null found if none found.
 */
NodeView.prototype.getNextSiblingOrParentSibling = function() {
    if (this.parent != null) {
        var nextSibling = this.parent.getNextSiblingFor(this);
        if (nextSibling == null)
            return this.parent.getNextSiblingOrParentSibling();
        else
            return nextSibling;
    }
    return null;
}

NodeView.prototype.getNextSibling = function() {
    if (this.parent != null) {
        return this.parent.getNextSiblingFor(this);
    }
    return null;
}

NodeView.prototype.getNextSiblingFor = function(childView) {
    if (this.children != null) {
        for (var i = 0; i < this.children.length; i++) {
            if (this.children[i] == childView) {
                if (i + 1 < this.children.length)
                    return this.children[i + 1];
                else
                    return null;
            }
        }
    }
    return null;
}

NodeView.prototype.getPrevious = function() {
    var previousSibling = this.getPreviousSibling();
    if (previousSibling != null) {
        return previousSibling.getLastChild();
    } else if (this.parent != null) {
        return this.parent;
    } else {
        return this;
    }
}

NodeView.prototype.getPreviousSibling = function() {
    if (this.parent != null) {
        return this.parent.getPreviousSiblingFor(this);
    }
    return null;
}

NodeView.prototype.getPreviousSiblingFor = function(childView) {
    if (this.children != null) {
        for (var i = 0; i < this.children.length; i++) {
            if (this.children[i] == childView) {
                if (i > 0)
                    return this.children[i - 1];
                else
                    return null;
            }
        }
    }
}

NodeView.prototype.getLastChild = function() {
    if (this.children != null && this.children.length > 0) {
        return this.children[this.children.length -1 ].getLastChild();
    } else {
        return this;
    }
}

NodeView.prototype.getTop = function() {
    return this.labelDiv.offsetTop;
}

NodeView.prototype.getHeight = function() {
    return this.labelDiv.offsetHeight;
}

NodeView.prototype.searchViewNode = function(treeNode) {
    if (this.treeNode == treeNode) {
        return this;
    }
    if (this.children != null) {
        for (var i = 0; i < this.children.length; i++) {
            var result = this.children[i].searchViewNode(treeNode);
            if (result != null)
                return result;
        }
    }
}


//
// class SelectionController
//

/**
 * Constructor.
 */
function SelectionController(treeView) {
    this.treeView = treeView;
    this.activeNode = null;
    this.eventSupport = new EventSupport();
}

SelectionController.prototype.unselectAll = function() {
    this.recursiveUnselect(this.treeView.rootNode);
}

SelectionController.prototype.recursiveUnselect = function(viewNode) {
    viewNode.unselect();
    if (viewNode.children != undefined) {
        for (var i = 0; i < viewNode.children.length; i++) {
            this.recursiveUnselect(viewNode.children[i]);
        }
    }
}

SelectionController.prototype.setActive = function(viewNode) {
    if (this.activeNode == viewNode)
        return;

    if (this.activeNode != null) {
        this.activeNode.unmarkActive();
    }
    this.activeNode = viewNode;
    if (this.activeNode != null)
        this.activeNode.markActive();

    var eventObject = new ActiveNodeChanged(viewNode != null ? viewNode.treeNode : null);
    this.eventSupport.fireEvent("activeNodeChanged", eventObject);
}

SelectionController.prototype.autoSelectChildren = function() {
    this.autoSelectRecursive(this.treeView.rootNode);
}

SelectionController.prototype.autoSelectRecursive = function(viewNode) {
    if (viewNode.selected) {
        this.selectChildrenRecursive(viewNode);
    } else {
        if (viewNode.children != undefined) {
            for (var i = 0; i < viewNode.children.length; i++) {
                this.autoSelectRecursive(viewNode.children[i]);
            }
        }
    }
}

SelectionController.prototype.selectChildrenRecursive = function(viewNode) {
    viewNode.select();
    if (viewNode.children != undefined) {
        for (var i = 0; i < viewNode.children.length; i++) {
            this.selectChildrenRecursive(viewNode.children[i]);
        }
    }
}

SelectionController.prototype.selectBetween = function(viewNode1, viewNode2) {
    if (viewNode1 == viewNode2) {
        viewNode1.select();
        return;
    }
    var context = new Object();
    context.inside = false;
    context.viewNode1 = viewNode1;
    context.viewNode2 = viewNode2;
    this.selectBetweenRecursive(this.treeView.rootNode, context);
}

SelectionController.prototype.selectBetweenRecursive = function(viewNode, context) {
    if (!context.inside && (viewNode == context.viewNode1 || viewNode == context.viewNode2)) {
        context.inside = true;
        context.stopNode = viewNode == context.viewNode1 ? context.viewNode2 : context.viewNode1;
        viewNode.select();
    } else if (context.inside && viewNode == context.stopNode) {
        context.inside = false;
        viewNode.select();
    } else if (context.inside) {
        viewNode.select();
    } else {
        viewNode.unselect();
    }

    if (viewNode.children != undefined) {
        for (var i = 0; i < viewNode.children.length; i++) {
            this.selectBetweenRecursive(viewNode.children[i], context);
        }
    }
}

SelectionController.prototype.addEventListener = function(eventListener) {
    this.eventSupport.addEventListener(eventListener);
}

//
// Class ActiveNodeChanged
//

/**
 * Constructor.
 */
function ActiveNodeChanged(treeNode) {
    this.treeNode = treeNode;
}

//
// function installTreeEditorOnSubmitHandler -- installs a form onsubmit handler that makes
//  sure that if the treeview is active, its current content is inserted into the textarea
//  before the form is submitted.
//

function installTreeEditorOnSubmitHandler(treeId, fieldPrefix) {
    var textArea = document.getElementById(treeId);
	if (textArea.form) {
        var htmlAreaSubmitHandler = new Object();
        htmlAreaSubmitHandler.forms_onsubmit = function() {
            var treeViewKey = treeId + "_treeView";
		    var treeView = window[treeViewKey];
		    var treemode = document.getElementById(fieldPrefix + "treemode");
		    if (treeView != null && treeView.isVisible()) {
                var nodeEditorKey = treeId + "_nodeEditor";
                window[nodeEditorKey].checkChangesApplied();
                var serializer = window[treeId + "_treeContext"].getSerializer();
                var navigation = serializer.serialize(window[treeViewKey].tree);
                textArea.value = navigation;
                treemode.value = "tree";
		    } else {
                treemode.value = "text";
		    }
            return true;
        };
        cocoon.forms.addOnSubmitHandler(textArea.form, htmlAreaSubmitHandler);
	}

}

function te_i18n(key) {
    var translated = TreeI18N[key];
    if (translated == null)
        return key;
    else
        return translated;
}
