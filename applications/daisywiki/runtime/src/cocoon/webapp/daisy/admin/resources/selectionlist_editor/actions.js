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
// This file contains actions specific for the selection list tree editor
//

//
// Class NewListItemNodeAction
//

NewListItemNodeAction.prototype = new AbstractCreateNodeAction();

function NewListItemNodeAction(treeView) {
    this.treeView = treeView;
}

NewListItemNodeAction.prototype.perform = function() {
    var treeNode = new TreeNode(LISTITEM_NODE_TYPE, this.treeView.tree);
    var state = new Object();
    switch (getFieldValueType()) {
        case "string":
            state.value = "value";
            break;
        case "date":
            var now = new Date();
            var dateString = now.getFullYear() + "-" + this.forceTwoPos(now.getMonth() + 1) + "-" + this.forceTwoPos(now.getDate());
            state.value = dateString;
            break;
        case "datetime":
            var now = new Date();
            var dateString = now.getFullYear() + "-" + this.forceTwoPos(now.getMonth() + 1) + "-" + this.forceTwoPos(now.getDate())
                    + "T" + this.forceTwoPos(now.getHours()) + ":" + this.forceTwoPos(now.getMinutes()) + ":" + this.forceTwoPos(now.getSeconds());
            state.value = dateString;
            break;
        case "long":
            state.value = "1";
            break;
        case "double":
            state.value = "1.1";
            break;
        case "decimal":
            state.value = "1.1";
            break;
        case "boolean":
            state.value = "true";
            break;
        case "link":
            state.value = "daisy:123-DSY";
            break;
    }
    treeNode.setState(state);
    this.addNode(treeNode);
}

NewListItemNodeAction.prototype.forceTwoPos = function(number) {
    var numberString = number.toFixed(0);
    if (numberString.length < 2)
        return "0" + numberString;
    return numberString;
}

NewListItemNodeAction.prototype.icon = "new_listitemnode.gif";

NewListItemNodeAction.prototype.tooltip = bt_i18n("insert-listitem-node");

//
// Class InsertListItemTagAction
//

function InsertListItemTagAction(treeId) {
    this.treeId = treeId;
}

InsertListItemTagAction.prototype.perform = function() {
    var textArea = document.getElementById(this.treeId);
    insertInTextArea(textArea, '<d:listItem></d:listItem>');
}

InsertListItemTagAction.prototype.icon = "new_listitemnode.gif";
InsertListItemTagAction.prototype.tooltip = bt_i18n("insert-listitem-tag");

//
// Class InsertSelectionListTreeTemplate
//

function InsertSelectionListTreeTemplate(treeId) {
    this.treeId = treeId;
}

InsertSelectionListTreeTemplate.prototype.perform = function() {
    var textArea = document.getElementById(this.treeId);
    var selectionListTreeTemplate = "<d:staticSelectionList xmlns:d='http://outerx.org/daisy/1.0'>\n  <!-- Insert listItem's here -->\n</d:staticSelectionList>";
    insertInTextArea(textArea, selectionListTreeTemplate);
}

InsertSelectionListTreeTemplate.prototype.icon = "template.gif";
InsertSelectionListTreeTemplate.prototype.tooltip = bt_i18n("insert-template");