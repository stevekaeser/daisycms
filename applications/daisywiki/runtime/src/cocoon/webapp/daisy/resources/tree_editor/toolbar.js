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
// Class TreeEditorToolbar
//

/**
 * Constructor.
 */
function TreeEditorToolbar(div, config, focusRequester, treeContext) {
    this.div = div;
    this.focusRequester = focusRequester;
    this.treeContext = treeContext;

    for (var i = 0; i < config.length; i++) {
        if (config[i] == "spacer") {
            this.addSpacer();
        } else {
            this.addButton(config[i]);
        }
    }
}

TreeEditorToolbar.prototype.addButton = function(action) {
    var button = this.div.ownerDocument.createElement("BUTTON");
    if (this.treeContext.disabled && !action.alwaysEnabled) {
      button.disabled='true';
    }
    var self = this;
    button.onclick = function() { try { action.perform(); } catch(e) { alert(e); } finally { if (self.focusRequester != null) self.focusRequester.focus(); return false; } };

    if (action.icon != undefined) {
        var img = this.div.ownerDocument.createElement("IMG");
        // icon prefix can overrule the resourcesPrefix
        if (action.iconprefix)
            img.src = action.iconprefix + "images/" + action.icon;
        else
            img.src = this.treeContext.resourcesPrefix + "images/" + action.icon;
        button.appendChild(img);
    } else {
        var label = action.label != undefined ? action.label : "(missing label or icon)";
        var textNode = this.div.ownerDocument.createTextNode(label);
        button.appendChild(textNode);
    }

    if (action.tooltip != undefined)
        button.title = action.tooltip;

    this.div.appendChild(button);
}

TreeEditorToolbar.prototype.addSpacer = function() {
    var spacer = this.div.ownerDocument.createTextNode("\u00A0");
    this.div.appendChild(spacer);
}

TreeEditorToolbar.prototype.show = function() {
    this.div.style.display = "";
}

TreeEditorToolbar.prototype.hide = function() {
    this.div.style.display = "none";
}

TreeEditorToolbar.prototype.getHeight = function() {
    return this.div.offsetHeight;
}
