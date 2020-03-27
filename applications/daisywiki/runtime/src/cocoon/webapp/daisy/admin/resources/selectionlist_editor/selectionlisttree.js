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

function initSelectionListTree(treeId, fieldPrefix) {
    var treeContext = new Object();
    treeContext.treeId = treeId;
    treeContext.resourcesPrefix = daisy.mountPoint + "/admin/resources/selectionlist_editor/";
    treeContext.daisyMountPoint = daisy.mountPoint;
    treeContext.fieldPrefix = fieldPrefix;

    treeContext.initTreeView = function(treeView) {
        this.newListItemNodeAction = new NewListItemNodeAction(treeView);
        this.deleteNodesAction = new DeleteNodesAction(treeView);
        this.moveNodeUpAction = new MoveNodeUpAction(treeView);
        this.moveNodeDownAction = new MoveNodeDownAction(treeView);
        this.moveNodesLeftAction = new MoveNodesLeftAction(treeView);
        this.moveNodesRightAction = new MoveNodesRightAction(treeView);
        this.cutAction = new CutAction(treeView);
        this.pasteBeforeAction = new PasteAction(treeView, PasteAction.PASTE_BEFORE);
        this.pasteAfterAction = new PasteAction(treeView, PasteAction.PASTE_AFTER);
        this.pasteInsideAction = new PasteAction(treeView, PasteAction.PASTE_INSIDE);
        this.copyAction = new CopyAction(treeView);
        this.validateAction = new ValidateAction(treeId);

        treeView.registerKeyMapping("c-x", this.cutAction);
        treeView.registerKeyMapping("c-c", this.copyAction);
        treeView.registerKeyMapping("c-v", this.pasteAfterAction);
        treeView.registerKeyMapping("c-z", treeView.undoAction);
        treeView.registerKeyMapping("c-37", this.moveNodesLeftAction);
        treeView.registerKeyMapping("c-39", this.moveNodesRightAction);
        treeView.registerKeyMapping("c-38", this.moveNodeUpAction);
        treeView.registerKeyMapping("c-40", this.moveNodeDownAction);
        treeView.registerKeyMapping("46", this.deleteNodesAction);
    }

    treeContext.createGuiToolbar = function() {
        var treeView = window[this.treeId + "_treeView"];
        if (treeView == null) {
            throw "createGuiToolbar called before initialisation of tree view";
        }

        var config = [
            this.newListItemNodeAction,
            "spacer",
            this.deleteNodesAction,
            this.cutAction,
            this.copyAction,
            this.pasteBeforeAction,
            this.pasteInsideAction,
            this.pasteAfterAction,
            "spacer",
            treeView.undoAction,
            "spacer",
            this.moveNodesLeftAction,
            this.moveNodesRightAction,
            this.moveNodeUpAction,
            this.moveNodeDownAction,
            "spacer",
            this.validateAction,
            "spacer",
            new SwitchToSourceAction(treeId)
        ];

        return new TreeEditorToolbar(document.getElementById(treeId + "_guiTreeToolbar"), config, treeView.iframe.contentWindow, treeContext);
    }

    treeContext.recreateGuiToolbar = function() {
        var div = document.getElementById(this.treeId + "_guiTreeToolbar");
        var newDiv = document.createElement("DIV");
        div.parentNode.replaceChild(newDiv, div);
        newDiv.setAttribute("id", treeId + "_guiTreeToolbar");

        return this.createGuiToolbar();
    }

    treeContext.createSourceToolbar = function() {
        var treeId = this.treeId;
        var config = [
            new InsertListItemTagAction(treeId),
            "spacer",
            new InsertSelectionListTreeTemplate(treeId),
            "spacer",
            new TextAreaValidateAction(treeId, dojo.byId("selectionlist.static.validateEditors")),
            "spacer",
            new SwitchToGuiAction(treeId, this)
        ];

        return new TreeEditorToolbar(document.getElementById(treeId + "_sourceTreeToolbar"), config, null, treeContext);
    }

    treeContext.createNodeEditor = function() {
        return new NodeEditor(this.treeId, this);
    }

    treeContext.getTreeModelBuilder = function() {
        return new SelectionListTreeBuilder();
    }

    treeContext.getEmptyTreeXml = function() {
        return "<d:staticSelectionList xmlns:d='http://outerx.org/daisy/1.0'></d:staticSelectionList>";
    }

    treeContext.getSerializer = function() {
        return new SelectionListTreeSerializer();
    }

    // Note: the property names in this object must correspond to the "name" property of the XXX_NODE_TYPE objects
    treeContext.validationNodeInfos =
        {
        "listitem":      [ new ValidationNodeInfo("value", true,  null,  bt_i18n("proplabel-listitem-value"))]
        };


    window[treeId + "_treeContext"] = treeContext;

    window[treeId + "_sourceToolbar"] = treeContext.createSourceToolbar();
    installTreeEditorOnSubmitHandler(treeId, fieldPrefix);

    var textarea = document.getElementById(treeId);

    var treemode = document.getElementById(fieldPrefix + "treemode");
    if (treemode.value != "text")
        new SwitchToGuiAction(treeId).perform();
}

function bt_i18n(key) {
    var translated = SelectionListTreeI18N[key];
    if (translated == null)
        return key;
    else
        return translated;
}
