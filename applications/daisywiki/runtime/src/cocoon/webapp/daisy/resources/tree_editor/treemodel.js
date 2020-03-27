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
// Tree class
//

/**
 * Constructor.
 */
function Tree() {
    this.eventSupport = new EventSupport();
    this.rootNode = new TreeNode(ROOT_NODE_TYPE, this);
}

/**
 * Event listeners registered on the tree itself will receive events
 * from all nodes.
 */
Tree.prototype.addEventListener = function(eventListener) {
    this.eventSupport.addEventListener(eventListener);
}

Tree.prototype.removeEventListener = function(eventListener) {
    this.eventSupport.removeEventListener(eventListener);
}

//
// TreeNode class
//

/**
 * Constructor.
 */
function TreeNode(nodeType, owner) {
    // Note: don't forget to update clone function if new state is added to this object

    if (nodeType == null)
        throw "TreeNode constructor: nodeType is null.";
    if (owner == null)
        throw "TreeNode constructor: owner is null.";

    this.children = new Array();
    this.eventSupport = new EventSupport();
    this.nodeType = nodeType;
    this.state = new Object();
    this.owner = owner;
}

TreeNode.prototype.addChild = function(newChild, position) {
    if (!this.nodeType.canHaveChildren())
        throw "TreeNode.addChild: this node does not support children.";

    if (position < 0 || position > this.children.length) {
        throw "TreeNode.addChild: position out of range: " + position;
    }

    if (this.owner != newChild.owner) {
        throw "TreeNode.addChild: newChild has different owner.";
    }

    if (position == this.children.length) {
        this.children[position] = newChild;
    } else if (position == 0) {
        this.children.unshift(newChild);
    } else {
        var beforeSlice = this.children.slice(0, position);
        var afterSlice = this.children.slice(position);
        beforeSlice.push(newChild);
        this.children = beforeSlice.concat(afterSlice);
    }

    var event = new NodeAddedEvent(this, newChild, position);
    this.eventSupport.fireEvent("nodeAdded", event);
    this.owner.eventSupport.fireEvent("nodeAdded", event);
}

TreeNode.prototype.removeChild = function(position) {
    if (!this.nodeType.canHaveChildren())
        throw "TreeNode.removeChild: this node does not support children.";

    if (position < 0 || position >= this.children.length) {
        throw "TreeNode.removeChild: position out of range: " + position;
    }

    var removedChild = this.children[position];

    var beforeSlice = this.children.slice(0, position);
    var afterSlice = this.children.slice(position + 1);
    this.children = beforeSlice.concat(afterSlice);

    var event = new NodeRemovedEvent(this, removedChild, position);
    this.eventSupport.fireEvent("nodeRemoved", event);
    this.owner.eventSupport.fireEvent("nodeRemoved", event);
}

TreeNode.prototype.findChild = function(childNode) {
    for (var i = 0; i < this.children.length; i++) {
        if (this.children[i] == childNode) {
            return i;
            break;
        }
    }
    return -1;
}

TreeNode.prototype.addEventListener = function(eventListener) {
    this.eventSupport.addEventListener(eventListener);
}

TreeNode.prototype.removeEventListener = function(eventListener) {
    this.eventSupport.removeEventListener(eventListener);
}

TreeNode.prototype.getChildren = function() {
    return this.children;
}

TreeNode.prototype.getState = function() {
    return this.state;
}

TreeNode.prototype.setState = function(state) {
    var oldState = this.state;
    this.state = state;

    var event = new NodeUpdatedEvent(this, oldState);
    this.eventSupport.fireEvent("nodeUpdated", event);
    this.owner.eventSupport.fireEvent("nodeUpdated", event);
}

TreeNode.prototype.getLabel = function() {
    return this.nodeType.getLabel(this.state);
}

TreeNode.prototype.canHaveChildren = function() {
    return this.nodeType.canHaveChildren();
}

TreeNode.prototype.getStyleClass = function() {
    return this.nodeType.styleClass;
}

TreeNode.prototype.clone = function() {
    var myClone = new TreeNode(this.nodeType, this.owner);

    var stateClone = new Object();
    for (var prop in this.state) {
        stateClone[prop] = this.state[prop];
    }

    myClone.state = stateClone;

    for (var i = 0; i < this.children.length; i++) {
        myClone.children[i] = this.children[i].clone();
    }

    return myClone;
}

//
// class NodeAddedEvent
//

/**
 * Constructor.
 */
function NodeAddedEvent(parentNode, newChild, position) {
    this.parentNode = parentNode;
    this.newChild = newChild;
    this.position = position;
}

//
// class NodeRemovedEvent
//

/**
 * Constructor.
 */
function NodeRemovedEvent(parentNode, removedChild, position) {
    this.parentNode = parentNode;
    this.removedChild = removedChild;
    this.position = position;
}

//
// Class NodeUpdatedEvent
//

/**
 * Constructor.
 */
function NodeUpdatedEvent(node, oldState) {
    this.node = node;
    this.oldState = oldState;
}


//
// Class EventSupport
//

/**
 * Constructor.
 */
function EventSupport() {
    this.eventListeners = new Array();
}

EventSupport.prototype.addEventListener = function(eventListener) {
    this.eventListeners[this.eventListeners.length] = eventListener;
}

EventSupport.prototype.removeEventListener = function(eventListener) {
    var pos = -1;
    for (var i = 0; i < this.eventListeners.length; i++) {
        if (this.eventListeners[i] == eventListener) {
            pos = i;
            break;
        }
    }
    if (pos == -1) {
        throw "EventSupport.removeEventListener: eventListener not found";
    } else {
        var beforeSlice = this.eventListeners.slice(0, pos);
        var afterSlice = this.eventListeners.slice(pos + 1);
        this.eventListeners = beforeSlice.concat(afterSlice);
    }
}

EventSupport.prototype.fireEvent = function(functionName, eventObject) {
    for (var i = 0; i < this.eventListeners.length; i++) {
        this.eventListeners[i][functionName](eventObject);
    }
}

