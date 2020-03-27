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
 * Plugin for HTMLArea to enable switching between block styles (h1, h2, p, pre, ...) and
 * between p.class values.
 *
 * This can be used as replacement for the standard style-switcher of HTMLArea.
 */
function BlockSwitcher(editor, params) {
    this.editor = editor;
    var cfg = editor.config;
    var toolbar = cfg.toolbar;
    var self = this;
    // var i18n = CSS.I18N;
    var plugin_config = params[0];

    var options = { "(none)" : "p"};

    for (block in plugin_config.blocks) {
        options[block] = plugin_config.blocks[block];
    }

    var id = "daisy-block-switcher";
    var dropdown = {
        id         : id,
        options    : options,
        action     : function(editor) { self.onSelect(editor, this); },
        refresh    : function(editor) { self.updateValue(editor, this); },
        context    : null
    };
    cfg.registerDropdown(dropdown);

};

BlockSwitcher._pluginInfo = {
    name          : "BlockSwitcher",
    version       : "1.0",
    developer     : "Outerthought",
    developer_url : "http://outerthought.org",
    c_owner       : "Outerthought",
    sponsor       : null,
    sponsor_url   : null,
    license       : "htmlArea"
};

BlockSwitcher.prototype.classRegexp = /^(.*)!(.*)$/;

BlockSwitcher.prototype.onSelect = function(editor, obj) {
    editor.focusEditor();

    if (!editor.daisyIsEditingAllowed())
        return;

    // The current element might be an include containing an include preview,
    // which should be removed prior to switching to another block type
    var parentBlockElement = daisySearchParentBlockElement(editor);
    if (parentBlockElement != null)
        DaisyLinkUtils.prototype.removeIncludePreview(parentBlockElement);

    var tbobj = editor._toolbarObjects[obj.id];
    var index = tbobj.element.selectedIndex;

    var blockName = tbobj.element.value;

    var elementName = null;
    var className = null;
    if (blockName.match(this.classRegexp)) {  // if it includes a class change
        elementName = RegExp.$1;
        className = RegExp.$2;
    } else {
        elementName = blockName;
    }

    // collapse any selection there might be, since we are only smart enough to change
    // one block element at a time
    if (!HTMLArea.is_ie) {
        // Non-IE: use DOM API
        var sel = editor._getSelection();
        sel.collapseToStart();
    } else {
        // IE
        var sel = editor._getSelection();
        var range = editor._createRange(editor._getSelection());
        range.collapse(true);
        range.select();
    }

    // change to requested element

    if (HTMLArea.is_ie && elementName == "p" && editor.getParentElement().tagName.toLowerCase() == "li") {
        // IE won't switch to a p when inside an li, except if we first switch to something else
        editor.execCommand("formatblock", false, "<h1>");        
    }

    var value = elementName;
    if (!HTMLArea.is_ie) {
      // firefox 3.0+ (sometimes) gives an error when the cursor is at an empty line
      // see https://bugzilla.mozilla.org/show_bug.cgi?id=481696
         
      var sel = editor._getSelection();
      var range = editor._createRange(sel);
      
      if  (range.commonAncestorContainer.nodeType != 3) {
        var newNode = editor._doc.createElement('p');

        var keepOpen = editor._doc.createElement('br');
        keepOpen.setAttribute("_moz_dirty", ""); 
        keepOpen.setAttribute("type", "_moz");
        newNode.appendChild(keepOpen);
      
        range.surroundContents(newNode);
        newNode.appendChild(keepOpen);
        range.setStart(newNode, 0);
      }
    } else {
      if (HTMLArea.is_ie) { 
        value = "<" + value + ">";
      }
    }

    editor.execCommand("formatblock", false, value);
    var parent = editor.getParentElement();
    
    if (parent.tagName.toLowerCase() == elementName) {
        this.applyClass(parent, className);
    } else {
        // search for the (new) parent tag
        while (parent != null && daisyIsInlineElement(parent)) {
            parent = parent.parentNode;
        }
        if (parent.tagName.toLowerCase() == elementName)
            this.applyClass(parent, className);
        else
            alert("Abnormal situation: no <" + elementName + "> found.");
    }
    editor.updateToolbar();
};

BlockSwitcher.prototype.applyClass = function(el, className) {
    // note: el.setAttribute("class", className) does not work with IE
    if (className == null)
        className = ""; // null does not work with IE
    el.className = className;
}

BlockSwitcher.prototype.updateValue = function(editor, obj) {

    if (editor._editMode != "textmode") {

        try {
            // Note: we don't use queryCommandValue to retrieve the current "formatblock"
            // because IE returns locale-specific strings for them, instead of the tagname
            // Also, this command works very slow in Mozilla

            var element = daisySearchParentBlockElement(editor);
            if (element != null) {
                var tagName = element.tagName.toLowerCase();
                if (element.className != "")
                    tagName = tagName + "!" + element.className;

                var select = editor._toolbarObjects[obj.id].element;
                var options = select.options;
                for (var i = options.length; --i >= 0;) {
                    var option = options[i];
                    if (tagName == option.value) {
                        select.selectedIndex = i;
                        return;
                    }
                }
            }

            // If we get here it is because the tag is not in the list, set it to the default "none" option
            select.selectedIndex = 0;

        } catch(e) {};
    }
};