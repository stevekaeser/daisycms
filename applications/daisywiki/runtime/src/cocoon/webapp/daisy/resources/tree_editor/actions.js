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
// Class AbstractRemoveNodesAction
//

function AbstractRemoveNodesAction() {
}

AbstractRemoveNodesAction.prototype.removeRecursive = function(viewNode, removedNodes) {
    var newActiveNode = null;
    if (viewNode.children != null) {
        var i = 0;
        while (i < viewNode.children.length) {
            var childView = viewNode.children[i];
            if (childView.selected) {
                var childPos = viewNode.treeNode.findChild(childView.treeNode);
                if (childPos == -1) {
                    alert("Assertion error: node not found among the children of its parent.");
                    return;
                }
                var nextSibling = childView.getNextSibling();
                newActiveNode = nextSibling != null ? nextSibling : viewNode;
                if (removedNodes != null)
                    removedNodes.push(childView.treeNode);
                viewNode.treeNode.removeChild(childPos);
            } else {
                var result = this.removeRecursive(childView, removedNodes);
                if (result != null)
                    newActiveNode = result;
                i++;
            }
        }
    }
    return newActiveNode;
}

//
// Class DeleteNodesAction
//

DeleteNodesAction.prototype = new AbstractRemoveNodesAction();

function DeleteNodesAction(treeView) {
    this.treeView = treeView;
}

DeleteNodesAction.prototype.perform = function() {
    var rootNode = this.treeView.rootNode;
    var newActiveNode = null;

    this.treeView.undoAction.startUndoTransaction();
    try {
        newActiveNode = this.removeRecursive(rootNode, null);
    } finally {
        this.treeView.undoAction.endUndoTransaction();
    }

    if (newActiveNode != null) {
        this.treeView.selectionController.unselectAll();
        newActiveNode.select();
    }
    this.treeView.selectionController.setActive(newActiveNode);
}

DeleteNodesAction.prototype.icon = "removenode.gif";

DeleteNodesAction.prototype.tooltip = te_i18n("remove-selected-nodes");

//
// Class CutAction
//

CutAction.prototype = new AbstractRemoveNodesAction();

function CutAction(treeView) {
    this.treeView = treeView;
}

CutAction.prototype.perform = function() {
    var rootNode = this.treeView.rootNode;
    var newActiveNode = null;
    var removedNodes = new Array();

    this.treeView.undoAction.startUndoTransaction();
    try {
        newActiveNode = this.removeRecursive(rootNode, removedNodes);
    } finally {
        this.treeView.undoAction.endUndoTransaction();
    }

    var newClipboardContent = new Array(removedNodes.length);
    for (var i = 0; i < removedNodes.length; i++) {
        newClipboardContent[i] = removedNodes[i].clone();
    }
    this.treeView.clipboard = newClipboardContent;

    if (newActiveNode != null) {
        this.treeView.selectionController.unselectAll();
        newActiveNode.select();
    }
    this.treeView.selectionController.setActive(newActiveNode);
}

CutAction.prototype.icon = "cut.gif";

CutAction.prototype.tooltip = te_i18n("cut-selected-nodes");

//
// Class CopyAction
//

function CopyAction(treeView) {
    this.treeView = treeView;
}

CopyAction.prototype.perform = function() {
    var rootNode = this.treeView.rootNode;
    var copiedNodes = new Array();

    this.copyRecursive(rootNode, copiedNodes);

    if (copiedNodes.length > 0) {
        this.treeView.clipboard = copiedNodes;
    }
}

CopyAction.prototype.copyRecursive = function(viewNode, copiedNodes) {
    if (viewNode.children != null) {
        for (var i = 0; i < viewNode.children.length; i++) {
            var childView = viewNode.children[i];
            if (childView.selected) {
                copiedNodes.push(childView.treeNode.clone());
            } else {
                this.copyRecursive(childView, copiedNodes);
            }
        }
    }
}

CopyAction.prototype.icon = "copy.gif";

CopyAction.prototype.tooltip = te_i18n("copy-selected-nodes");

//
// Class PasteAction
//

function PasteAction(treeView, type) {
    this.treeView = treeView;
    this.type = type;
    if (type == PasteAction.PASTE_BEFORE) {
        this.icon = "paste_before.gif";
        this.tooltip = te_i18n("paste-before");
    } else if (type == PasteAction.PASTE_AFTER) {
        this.icon = "paste_after.gif";
        this.tooltip = te_i18n("paste-after");
    } else if (type == PasteAction.PASTE_INSIDE) {
        this.icon = "paste_inside.gif";
        this.tooltip = te_i18n("paste-inside");
    }
}

PasteAction.prototype.perform = function() {
    var clipboard = this.treeView.clipboard;
    if (clipboard == null || clipboard.length == 0) {
        alert(te_i18n("clipboard-empty"));
        return;
    }

    var activeNode = this.treeView.selectionController.activeNode;
    if (activeNode == null) {
        alert(te_i18n("no-node-selected"));
        return;
    }

    if (this.type == PasteAction.PASTE_BEFORE) {
        if (activeNode.parent == null) {
            alert(te_i18n("cannot-insert-before-root"));
            return;
        }
        var parent = activeNode.parent.treeNode;
        var childPos = parent.findChild(activeNode.treeNode);
        if (childPos == -1) {
            alert("Assertion error: node not found among the children of its parent.");
            return;
        }
        this.treeView.undoAction.startUndoTransaction();
        try {
            for (var i = 0; i < clipboard.length; i++) {
                parent.addChild(clipboard[i].clone(), childPos + i);
            }
        } finally {
            this.treeView.undoAction.endUndoTransaction();
        }
    } else if (this.type == PasteAction.PASTE_AFTER) {
        if (activeNode.parent == null) {
            alert(te_i18n("cannot-insert-after-root"));
            return;
        }
        var parent = activeNode.parent.treeNode;
        var childPos = parent.findChild(activeNode.treeNode);
        if (childPos == -1) {
            alert("Assertion error: node not found among the children of its parent.");
            return;
        }
        this.treeView.undoAction.startUndoTransaction();
        try {
            for (var i = 0; i < clipboard.length; i++) {
                parent.addChild(clipboard[i].clone(), childPos + i + 1);
            }
        } finally {
            this.treeView.undoAction.endUndoTransaction();
        }
    } else if (this.type == PasteAction.PASTE_INSIDE) {
        if (!activeNode.treeNode.canHaveChildren()) {
            alert(te_i18n("cannot-have-children"));
            return;
        }
        var treeNode = activeNode.treeNode
        this.treeView.undoAction.startUndoTransaction();
        try {
            for (var i = 0; i < clipboard.length; i++) {
                treeNode.addChild(clipboard[i].clone(), treeNode.children.length);
            }
        } finally {
            this.treeView.undoAction.endUndoTransaction();
        }
    } else {
        alert("Paste action: invalid type.");
    }
}

PasteAction.PASTE_BEFORE = 1;
PasteAction.PASTE_AFTER  = 2;
PasteAction.PASTE_INSIDE = 3;


//
// Class MoveNodeUpAction
//

function MoveNodeUpAction(treeView) {
    this.treeView = treeView;
}

MoveNodeUpAction.prototype.perform = function() {
    var activeNode = this.treeView.selectionController.activeNode;

    if (activeNode == null) {
        alert(te_i18n("no-node-selected"));
        return;
    }

    if (activeNode.parent == null) {
        // root node cannot be moved
        return;
    }

    var activeTreeNode = activeNode.treeNode;
    var parentViewNode = activeNode.parent;
    var parentTreeNode = parentViewNode.treeNode;

    var childPos = parentTreeNode.findChild(activeTreeNode);
    if (childPos == -1) {
        alert("Assertion error: node not found among the children of its parent.");
        return;
    }
    if (childPos == 0) {
        // node is already the first child
        return;
    }

    var newPos = childPos - 1;

    this.treeView.undoAction.startUndoTransaction();
    try {
        parentTreeNode.removeChild(childPos);
        parentTreeNode.addChild(activeTreeNode, newPos);
    } finally {
        this.treeView.undoAction.endUndoTransaction();
    }

    var newViewNode = parentViewNode.children[newPos];
    this.treeView.selectionController.unselectAll();
    newViewNode.select();
    this.treeView.selectionController.setActive(newViewNode);
    this.treeView.assureVisible(newViewNode, true);
}

MoveNodeUpAction.prototype.icon = "move_up.gif";

MoveNodeUpAction.prototype.tooltip = te_i18n("move-node-up");

//
// Class MoveNodeDownAction
//

function MoveNodeDownAction(treeView) {
    this.treeView = treeView;
}

MoveNodeDownAction.prototype.perform = function() {
    var activeNode = this.treeView.selectionController.activeNode;

    if (activeNode == null) {
        alert(te_i18n("no-node-selected"));
        return;
    }

    if (activeNode.parent == null) {
        // root node cannot be moved
        return;
    }

    var activeTreeNode = activeNode.treeNode;
    var parentViewNode = activeNode.parent;
    var parentTreeNode = parentViewNode.treeNode;

    var childPos = parentTreeNode.findChild(activeTreeNode);
    if (childPos == -1) {
        alert("Assertion error: node not found among the children of its parent.");
        return;
    }
    if (childPos == parentTreeNode.children.length - 1) {
        // node is already the last child
        return;
    }

    var newPos = childPos + 1;

    this.treeView.undoAction.startUndoTransaction();
    try {
        parentTreeNode.removeChild(childPos);
        parentTreeNode.addChild(activeTreeNode, newPos);
    } finally {
        this.treeView.undoAction.endUndoTransaction();
    }

    var newViewNode = parentViewNode.children[newPos];
    this.treeView.selectionController.unselectAll();
    newViewNode.select();
    this.treeView.selectionController.setActive(newViewNode);
    this.treeView.assureVisible(newViewNode, false);
}

MoveNodeDownAction.prototype.icon = "move_down.gif";

MoveNodeDownAction.prototype.tooltip = te_i18n("move-node-down");

//
// Class MoveNodesRightAction
//

function MoveNodesRightAction(treeView) {
    this.treeView = treeView;
}

MoveNodesRightAction.prototype.perform = function() {
    var movedNodes = new Array();
    this.treeView.undoAction.startUndoTransaction();
    try {
        lastMovedNode = this.moveSelectedNodesRight(this.treeView.rootNode, movedNodes);
    } finally {
        this.treeView.undoAction.endUndoTransaction();
    }

    if (movedNodes.length > 0) {
        this.treeView.selectionController.unselectAll();
        for (var i = 0; i < movedNodes.length; i++) {
            movedNodes[i].select();
        }
        this.treeView.selectionController.autoSelectChildren();
        this.treeView.selectionController.setActive(movedNodes[0]);
        this.treeView.assureVisible(movedNodes[0], true);
    }
}

MoveNodesRightAction.prototype.moveSelectedNodesRight = function(viewNode, movedNodes) {
    if (viewNode.parent != null && viewNode.selected) {
        var result = this.moveNodeRight(viewNode);
        if (result != null)
            movedNodes.push(result);
    } else {
        if (viewNode.children != null) {
            var children = this.cloneArray(viewNode.children);
            for (var i = 0; i < children.length; i++) {
                this.moveSelectedNodesRight(children[i], movedNodes);
            }
        }
    }
}

MoveNodesRightAction.prototype.cloneArray = function(anArray) {
    var newArray = new Array(anArray.length);
    for (var i = 0; i < anArray.length; i++) {
        newArray[i] = anArray[i];
    }
    return newArray;
}

MoveNodesRightAction.prototype.moveNodeRight = function(viewNode) {
    var treeNode = viewNode.treeNode;
    var parentViewNode = viewNode.parent;
    var parentTreeNode = parentViewNode.treeNode;

    var childPos = parentTreeNode.findChild(treeNode);
    if (childPos == -1) {
        alert("Assertion error: node not found among the children of its parent.");
        return null;
    }
    if (childPos == 0) {
        // first child node cannot be moved further to the right
        return null;
    }

    var previousSibling = parentTreeNode.children[childPos - 1];
    if (previousSibling.canHaveChildren() == false) {
        return;
    }

    parentTreeNode.removeChild(childPos);
    previousSibling.addChild(treeNode, previousSibling.children.length);

    var siblingNode = parentViewNode.children[childPos - 1];
    var newViewNode = siblingNode.children[siblingNode.children.length - 1];
    return newViewNode;
}

MoveNodesRightAction.prototype.icon = "move_right.gif";

MoveNodesRightAction.prototype.tooltip = te_i18n("move-nodes-right");

//
// Class MoveNodesLeftAction
//

function MoveNodesLeftAction(treeView) {
    this.treeView = treeView;
}

MoveNodesLeftAction.prototype.perform = function() {
    var movedNodes = new Array();
    this.treeView.undoAction.startUndoTransaction();
    try {
        this.moveSelectedNodesLeft(this.treeView.rootNode, movedNodes);
    } finally {
        this.treeView.undoAction.endUndoTransaction();
    }

    if (movedNodes.length > 0) {
        this.treeView.selectionController.unselectAll();
        for (var i = 0; i < movedNodes.length; i++) {
            movedNodes[i].select();
        }
        this.treeView.selectionController.autoSelectChildren();
        this.treeView.selectionController.setActive(movedNodes[movedNodes.length - 1]);
        this.treeView.assureVisible(movedNodes[movedNodes.length - 1], true);
    }
}

MoveNodesLeftAction.prototype.moveSelectedNodesLeft = function(viewNode, movedNodes) {
    if (viewNode.parent != null && viewNode.parent.parent != null && viewNode.selected) {
        var result = this.moveNodeLeft(viewNode);
        if (result != null)
            movedNodes.push(result);
    } else if (!viewNode.selected) {
        if (viewNode.children != null) {
            for (var i = viewNode.children.length - 1; i >= 0; i--) {
                this.moveSelectedNodesLeft(viewNode.children[i], movedNodes);
            }
        }
    }
}

MoveNodesLeftAction.prototype.cloneArray = function(anArray) {
    var newArray = new Array(anArray.length);
    for (var i = 0; i < anArray.length; i++) {
        newArray[i] = anArray[i];
    }
    return newArray;
}

MoveNodesLeftAction.prototype.moveNodeLeft = function(viewNode) {
    var treeNode = viewNode.treeNode;
    var parentViewNode = viewNode.parent;
    var grandParentViewNode = parentViewNode.parent;
    var parentTreeNode = parentViewNode.treeNode;
    var grandParentTreeNode = grandParentViewNode.treeNode;

    var childPos = parentTreeNode.findChild(treeNode);
    if (childPos == -1) {
        alert("Assertion error: node not found among the children of its parent.");
        return;
    }

    if (!treeNode.canHaveChildren() && childPos != parentTreeNode.children.length - 1) {
        // moving the node to the left in this case would make that its following siblings
        // become children of the node, but since this node can't have children...
        return;
    }

    // copy all following siblings of the node into an array and remove them from the tree
    var followingSiblings = new Array();
    var maxChildren = parentTreeNode.children.length;
    for (var i = childPos + 1; i < maxChildren; i++) {
        followingSiblings[followingSiblings.length] = parentTreeNode.children[childPos + 1];
        parentTreeNode.removeChild(childPos + 1);
    }

    // remove the node itself
    parentTreeNode.removeChild(childPos);

    // add (append) the 'followingSiblings' as children
    for (var i = 0; i < followingSiblings.length; i++) {
        treeNode.addChild(followingSiblings[i], treeNode.children.length);
    }

    //
    var parentChildPos = grandParentViewNode.treeNode.findChild(parentTreeNode);
    if (parentChildPos == -1) {
        alert("Assertion error: node not found among the children of its parent. (2)");
        return;
    }

    grandParentTreeNode.addChild(treeNode, parentChildPos + 1);

    var newViewNode = grandParentViewNode.children[parentChildPos + 1];
    return newViewNode;
}

MoveNodesLeftAction.prototype.icon = "move_left.gif";

MoveNodesLeftAction.prototype.tooltip = te_i18n("move-nodes-left");

//
// Class AbstractCreateNodeAction
//

function AbstractCreateNodeAction() {
}

AbstractCreateNodeAction.prototype.addNode = function(treeNode) {
    var activeNode = this.treeView.selectionController.activeNode;

    if (activeNode == null) {
        alert(te_i18n("no-node-selected"));
        return;
    }


    if (activeNode.parent == null) {
        activeNode.treeNode.addChild(treeNode, 0);
    } else {
        // insert after selected node
        var childPos = activeNode.parent.treeNode.findChild(activeNode.treeNode);
        if (childPos == -1) {
            alert("Assertion error: node not found among the children of its parent.");
            return;
        }

        activeNode.parent.treeNode.addChild(treeNode, childPos + 1);
    }

    var viewNode = this.treeView.searchViewNode(treeNode);
    this.treeView.selectionController.unselectAll();
    viewNode.select();
    this.treeView.selectionController.setActive(viewNode);
    this.treeView.assureVisible(viewNode, false);
    return false;
}

//
// Class UndoAction
//

function UndoAction(treeView) {
    this.tree = treeView.tree;
    this.tree.addEventListener(this);
    this.treeView = treeView;
    this.undoStack = new Array();
    this.undoing = false;
    this.undoTransaction = null;
}

UndoAction.prototype.perform = function() {
    if (this.undoStack.length == 0) {
        alert(te_i18n("nothing-to-undo"));
        return;
    }

    var undoable = this.undoStack.pop();
    try {
        this.undoing = true;
        var focusNode = undoable.undo();

        this.treeView.selectionController.unselectAll();
        this.treeView.selectionController.setActive(null);
        if (focusNode != null) {
            var viewNode = this.treeView.searchViewNode(focusNode);
            if (viewNode == null) {
                alert("Abnormal situation: view node for tree node not found.");
                return;
            }
            viewNode.select();
            this.treeView.selectionController.setActive(viewNode);
        }
    } finally {
        this.undoing = false;        
    }
}

UndoAction.prototype.icon = "undo.gif";

UndoAction.prototype.tooltip = te_i18n("undo");

UndoAction.prototype.connectToModel = function(tree) {
    this.tree.removeEventListener(this);
    this.tree = tree;

    this.undoStack.length = new Array();
    this.undoTransaction = null;
    this.undoing = false;
    this.tree.addEventListener(this);
}

UndoAction.prototype.nodeAdded = function(event) {
    if (!this.undoing)
        this.addUndoable(new NodeAddedUndoable(event));
}

UndoAction.prototype.nodeRemoved = function(event) {
    if (!this.undoing)
        this.addUndoable(new NodeRemovedUndoable(event));
}

UndoAction.prototype.nodeUpdated = function(event) {
    if (!this.undoing)
        this.addUndoable(new NodeUpdatedUndoable(event));
}

UndoAction.prototype.addUndoable = function(undoable) {
    if (this.undoTransaction != null)
        this.undoTransaction.add(undoable);
    else
        this.undoStack.push(undoable);
}

UndoAction.prototype.startUndoTransaction = function() {
    this.undoTransaction = new UndoTransaction();
}

UndoAction.prototype.endUndoTransaction = function() {
    if (this.undoTransaction == null) {
        throw "Error: endUndoTransaction called but undoTransaction is null.";
        return;
    }
    if (this.undoTransaction.undoables.length > 0)
        this.undoStack.push(this.undoTransaction);
    this.undoTransaction = null;
}

//
// Class NodeAddedUndoable
//

function NodeAddedUndoable(event) {
    this.position = event.position;
    this.parentNode = event.parentNode;
}

NodeAddedUndoable.prototype.undo = function() {
    this.parentNode.removeChild(this.position);
    if (this.position > 0)
        return this.parentNode.children[this.position - 1];
    else
        return this.parentNode;
}

//
// Class NodeRemovedUndoable
//

function NodeRemovedUndoable(event) {
    this.parentNode = event.parentNode;
    this.position = event.position;
    this.removedChild = event.removedChild;
}

NodeRemovedUndoable.prototype.undo = function() {
    this.parentNode.addChild(this.removedChild, this.position);
    return this.removedChild;
}

//
// Class NodeUpdatedUndoable
//

function NodeUpdatedUndoable(event) {
    this.node = event.node;
    this.oldState = event.oldState;
}

NodeUpdatedUndoable.prototype.undo = function() {
    this.node.setState(this.oldState);
    return this.node;
}

//
// Class UndoTransaction
//

function UndoTransaction() {
    this.undoables = new Array();
}

UndoTransaction.prototype.undo = function() {
    var focusNode = null;
    for (var i = this.undoables.length - 1; i >= 0; i--) {
        focusNode = this.undoables[i].undo();
    }
    return focusNode;
}

UndoTransaction.prototype.add = function(undoable) {
    this.undoables.push(undoable);
}

//
// Class SwitchToGuiAction
//

function SwitchToGuiAction(treeId) {
    this.treeId = treeId;
    this.treeContext = window[treeId + "_treeContext"];
    this.textarea = document.getElementById(treeId);
    if (this.textarea == null)
        throw "Did not find textarea with id: " + treeId;
    this.alwaysEnabled = true;
}

SwitchToGuiAction.prototype.perform = function() {
    var builder = this.treeContext.getTreeModelBuilder();
    var xml = this.textarea.value;
    if (xml == null || xml == "")
        xml = this.treeContext.getEmptyTreeXml();
    var tree = builder.build(xml);
    if (tree == null) {
        // building tree model failed, stay in text area mode
        return;
    }

    var treeId = this.treeId;
    var treeViewKey = this.treeId + "_treeView";
    if (window[treeViewKey] == null) {
        var treeView = new TreeView(tree, this.treeId);
        window[treeViewKey] = treeView;
        this.treeContext.initTreeView(treeView);
        if (window.editorHeightListeners == null)
            window.editorHeightListeners = new Array();
        window.editorHeightListeners.push(function(height) {
            var treeViewHeight = height - window[treeId + "_nodeEditor"].getHeight() - window[treeId + "_guiToolbar"].getHeight();
            window[treeViewKey].setHeight(treeViewHeight.toFixed(0) + "px");
        });
    } else {
        window[treeViewKey].connectToModel(tree);
        window[treeViewKey].show();
    }

    var guiToolbarKey = this.treeId + "_guiToolbar";
    if (window[guiToolbarKey] == null) {
        window[guiToolbarKey] = this.treeContext.createGuiToolbar();
    } else {
        window[guiToolbarKey] = this.treeContext.recreateGuiToolbar();
        window[guiToolbarKey].show();
    }

    var nodeEditorKey = this.treeId + "_nodeEditor";
    if (window[nodeEditorKey] == null) {
        var treeContext = window[this.treeId + "_treeContext"];
        window[nodeEditorKey] = treeContext.createNodeEditor();
        window[treeViewKey].selectionController.addEventListener(window[nodeEditorKey]);
    } else {
        // nothing
        window[nodeEditorKey].show();
    }

    var sourceToolbarKey = this.treeId + "_sourceToolbar";
    window[sourceToolbarKey].hide();
    this.textarea.style.display = "none";
}

SwitchToGuiAction.prototype.icon = "treeview.gif";
SwitchToGuiAction.prototype.tooltip = te_i18n("treeview");

//
// Class SwitchToSourceAction
//

function SwitchToSourceAction(treeId) {
    this.treeId = treeId;
    this.treeContext = window[treeId + "_treeContext"];
    this.alwaysEnabled = true;
}

SwitchToSourceAction.prototype.perform = function() {
    var nodeEditorKey = this.treeId + "_nodeEditor";
    window[nodeEditorKey].checkChangesApplied();
    var treeViewKey = this.treeId + "_treeView";

    try {
        var serializer = this.treeContext.getSerializer();
        var xml = serializer.serialize(window[treeViewKey].tree);
    } catch (error) {
        alert("Cannot switch to source view: " + error);
        return;
    }

    window[treeViewKey].hide();
    var guiToolbarKey = this.treeId + "_guiToolbar";
    window[guiToolbarKey].hide();
    window[nodeEditorKey].hide();

    var sourceToolbarKey = this.treeId + "_sourceToolbar";
    window[sourceToolbarKey].show();
    var textarea = document.getElementById(this.treeId);
    textarea.style.display = "";
    textarea.value = xml;
}

SwitchToSourceAction.prototype.icon = "sourceview.gif";

SwitchToSourceAction.prototype.tooltip = te_i18n("sourceview");

//
// Class ValidateAction
//

function ValidateAction(treeId) {
    this.treeView = window[treeId + "_treeView"];
    this.treeContext = window[treeId + "_treeContext"];
}

ValidateAction.prototype.perform = function() {
    var rootNode = this.treeView.rootNode;
    var result = this.validate(rootNode);
    if (result) {
        alert(te_i18n("no-validation-errors"));
    }
}

ValidateAction.prototype.validate = function(viewNode) {
    var nodeTypeName = viewNode.treeNode.nodeType.name;
    var nodeInfo = this.treeContext.validationNodeInfos[nodeTypeName];
    if (nodeInfo != null) {
        var state = viewNode.treeNode.getState();
        for (var i = 0; i < nodeInfo.length; i++) {
            var stateValue = state[nodeInfo[i].stateProperty];
            if (stateValue == null && nodeInfo[i].required) {
                alert(te_i18n("property-missing") + nodeInfo[i].label);
                this.treeView.selectionController.unselectAll();
                viewNode.select();
                this.treeView.selectionController.setActive(viewNode);
                this.treeView.assureVisible(viewNode, false);
                return false;
            }
            if (stateValue != null && nodeInfo[i].validationRegExp != null) {
                if (!stateValue.match(nodeInfo[i].validationRegExp)) {
                    alert(te_i18n("property-invalid") + nodeInfo[i].label);
                    this.treeView.selectionController.unselectAll();
                    viewNode.select();
                    this.treeView.selectionController.setActive(viewNode);
                    this.treeView.assureVisible(viewNode, false);
                    return false;
                }
            }
        }
    }

    if (viewNode.children != null) {
        for (var i = 0; i < viewNode.children.length; i++) {
            var result = this.validate(viewNode.children[i]);
            if (!result)
                return false;
        }
    }

    return true;
}

ValidateAction.prototype.icon = "validate.gif";
ValidateAction.prototype.tooltip = te_i18n("validate");

function ValidationNodeInfo(stateProperty, required, validationRegexp, label) {
    this.stateProperty = stateProperty;
    this.required = required;
    this.validationRegExp = validationRegexp;
    this.label = label;
}

//
// Class TextAreaValidateAction
//

function TextAreaValidateAction(treeId, submitWidget) {
    this.treeContext = window[treeId + "_treeContext"];
    this.submitWidget = submitWidget;
}

TextAreaValidateAction.prototype.perform = function() {
    cocoon.forms.submitForm(this.submitWidget);
}

TextAreaValidateAction.prototype.icon = "validate.gif";
TextAreaValidateAction.prototype.tooltip = te_i18n("validate");

//
// function insertInTextArea()
//

insertInTextArea = function(textarea, text) {
	if(document.selection) {
	    textarea.focus();
        var range = document.selection.createRange();
        range.collapse(true);
        range.text = text;
	} else if(textarea.selectionStart || textarea.selectionStart == "0") {
 		var startPos = textarea.selectionStart;
 		textarea.value = textarea.value.substring(0, startPos) + text + textarea.value.substring(startPos, textarea.value.length);
        textarea.focus();
 		textarea.selectionStart = startPos;
 		textarea.selectionEnd = startPos;
	} else {
	  alert(te_i18n("inserting-text-not-supported") + text);
	}
}

//
// Class InsertDocumentIdAction
//

function InsertDocumentIdAction(treeId) {
    this.treeId = treeId;
    this.treeContext = window[treeId + "_treeContext"];
}

InsertDocumentIdAction.prototype.perform = function() {
    var textArea = document.getElementById(this.treeId);
    var mountPoint = this.treeContext.daisyMountPoint;
    var siteName = this.treeContext.daisySiteName;
    var dialogParams = { "daisyMountPoint" : mountPoint, "daisySiteName" : siteName };

    daisy.dialog.popupDialog(mountPoint + "/" + siteName + "/editing/documentBrowser?branch=" + getBranchId() + "&language=" + getLanguageId(),
      function(params) {
        insertInTextArea(textArea, params.docId);
      }, dialogParams);
}

InsertDocumentIdAction.prototype.icon = "documentlookup.gif";
InsertDocumentIdAction.prototype.tooltip = te_i18n("lookup-doc-id");
