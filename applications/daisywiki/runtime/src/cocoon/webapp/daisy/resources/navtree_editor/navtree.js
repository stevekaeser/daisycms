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
function initNavTree(basename) {
    var treeId = basename + ".navigation:input";
    var treeContext = new Object();
    treeContext.treeId = treeId;
    treeContext.resourcesPrefix = daisy.mountPoint + "/resources/navtree_editor/";
    treeContext.documentCollectionNames = getDocumentCollectionNames();
    treeContext.daisyMountPoint = daisy.mountPoint;
    treeContext.daisySiteName = daisy.site.name;
    treeContext.fieldPrefix = basename + ".";

    treeContext.initTreeView = function(treeView) {
        this.deleteNodesAction = new DeleteNodesAction(treeView);
        this.moveNodeUpAction = new MoveNodeUpAction(treeView);
        this.moveNodeDownAction = new MoveNodeDownAction(treeView);
        this.moveNodesLeftAction = new MoveNodesLeftAction(treeView);
        this.moveNodesRightAction = new MoveNodesRightAction(treeView);
        this.newDocumentNodeAction = new NewDocumentNodeAction(treeView);
        this.newGroupNodeAction = new NewGroupNodeAction(treeView);
        this.newLinkNodeAction = new NewLinkNodeAction(treeView);
        this.newQueryNodeAction = new NewQueryNodeAction(treeView);
        this.newImportNodeAction = new NewImportNodeAction(treeView);
        this.newQueryImportAction = new NewQueryImportAction(treeView);
        this.newSeparatorNodeAction = new NewSeparatorNodeAction(treeView);
        this.cutAction = new CutAction(treeView);
        this.pasteBeforeAction = new PasteAction(treeView, PasteAction.PASTE_BEFORE);
        this.pasteAfterAction = new PasteAction(treeView, PasteAction.PASTE_AFTER);
        this.pasteInsideAction = new PasteAction(treeView, PasteAction.PASTE_INSIDE);
        this.copyAction = new CopyAction(treeView);
        this.validateAction = new ValidateAction(treeId);
        this.previewAction = new TreeViewPreviewNavigation(treeView, treeId);

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
            this.newDocumentNodeAction,
            this.newGroupNodeAction,
            this.newLinkNodeAction,
            this.newQueryNodeAction,
            this.newImportNodeAction,
            this.newSeparatorNodeAction,
            "spacer",
            this.newQueryImportAction,
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
            this.previewAction,
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
            new InsertDocumentTagAction(treeId),
            new InsertGroupTagAction(treeId),
            new InsertLinkTagAction(treeId),
            new InsertQueryTagAction(treeId),
            new InsertImportTagAction(treeId),
            new InsertSeparatorTagAction(treeId),
            "spacer",
            new InsertDocumentIdAction(treeId, this),
            new InsertNavigationTreeTemplate(treeId),
            "spacer",
            new TextAreaValidateAction(treeId, dojo.byId("validateEditors")),
            new TextAreaPreviewNavigation(treeId, this),
            "spacer",
            new SwitchToGuiAction(treeId, this)
        ];

        return new TreeEditorToolbar(document.getElementById(treeId + "_sourceTreeToolbar"), config, null, treeContext);
    }

    treeContext.createNodeEditor = function() {
        return new NodeEditor(this.treeId, this);
    }

    treeContext.getTreeModelBuilder = function() {
        return new NavigationTreeBuilder();
    }

    treeContext.getEmptyTreeXml = function() {
        return "<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'></d:navigationTree>";
    }

    treeContext.getSerializer = function() {
        return new NavigationTreeSerializer();
    }

    // Note: the property names in this object must correspond to the "name" property of the XXX_NODE_TYPE objects
    treeContext.validationNodeInfos =
        {
        "document":   [ new ValidationNodeInfo("documentId", true,  null, nt_i18n("proplabel-document-id")),
                        new ValidationNodeInfo("label",      false, null, nt_i18n("proplabel-label")),
                        new ValidationNodeInfo("nodeId",     false, /^[^0-9 \t\n\r\f][^ \t\n\r\f]*$/, nt_i18n("proplabel-node-id"))],
        "query":      [ new ValidationNodeInfo("query",      true,  null, nt_i18n("proplabel-query"))],
        "group":      [ new ValidationNodeInfo("id",         false, /^[^0-9 \t\n\r\f][^ \t\n\r\f]*$/, nt_i18n("proplabel-id")),
                        new ValidationNodeInfo("label",      true,  null, nt_i18n("proplabel-label"))],
        "link":       [ new ValidationNodeInfo("url",        true,  null, nt_i18n("proplabel-URL")),
                        new ValidationNodeInfo("label",      true,  null, nt_i18n("proplabel-label")),
                        new ValidationNodeInfo("id",         false, /^[^0-9 \t\n\r\f][^ \t\n\r\f]*$/, nt_i18n("proplabel-id"))],
        "import":     [ new ValidationNodeInfo("docId",      true,  null, nt_i18n("proplabel-navdoc-id"))]
        };


    window[treeId + "_treeContext"] = treeContext;

    window[treeId + "_sourceToolbar"] = treeContext.createSourceToolbar();
    installTreeEditorOnSubmitHandler(treeId, treeContext.fieldPrefix);

    var textarea = document.getElementById(treeId);
    var heightListener = function(height) { textarea.style.height = (height - window[treeId + "_sourceToolbar"].getHeight()).toFixed(0) + "px"; };
    if (window.editorHeightListeners == null)
        window.editorHeightListeners = new Array();
    window.editorHeightListeners.push(heightListener);

    var treemode = document.getElementById(basename + ".treemode");
    if (treemode.value != "text")
        new SwitchToGuiAction(treeId).perform();

}

function nt_i18n(key) {
    var translated = NavTreeI18N[key];
    if (translated == null)
        return key;
    else
        return translated;
}
