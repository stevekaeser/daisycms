/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
dojo.provide("daisy.widget.DoubleListboxPopup");
dojo.require("dojo.widget.PopupContainer");
dojo.require("dojo.widget.ComboBox");
dojo.require("dojo.i18n.common");

dojo.requireLocalization("daisy.widget", "messages", null, /* available languages, to avoid 404 requests */ "ROOT,nl,fr,de,es,ru");

window.i18n_bundle = dojo.i18n.getLocalization("daisy.widget", "messages");

/*
 * This widget displays a double listbox in a dojo.widget.PopupContainer -
 * and because PopupContainer moves the containg elements to an other place in the dom tree
 * we have to keep a third select list in sync with the current selection so the correct selection  
 * is sent when the form is submitted.
 *
 * This widget has not been tested in cases where values occur more than once
 * or where values/labels have not been explicitly set.  It is likely that these cases
 * will cause breakage.
 *
 * Usage:
 * To convert a regular <select multiple="true"> into a double-listbox-popup selectbox,
 * add style="display:none" to the select element and add this:
 *     <span dojoType="daisy:DoubleListboxPopup" inputId="<<<selectId>>>"></span>
 * where <<<selectId>>> is the select element's id.
 */
dojo.widget.defineWidget("daisy.widget.DoubleListboxPopup", dojo.widget.HtmlWidget, {
  templatePath: dojo.uri.moduleUri("daisy", "widget/templates/DoubleListboxPopup.html"),
  
  postCreate: function(args, frag) {
    this.usePopup = this.usePopup=='true'; // convert to boolean
    if  (this.usePopup) {
      this.popupContainer = dojo.widget.createWidget("PopupContainer", {toggle: "plain"});
      dojo.body().appendChild(this.popupContainer.domNode);
    }

    /** create and initialize the select elements */    
    this.selectElement = dojo.byId(args.inputId);
    this.leftSelect = document.createElement("select");
    this.leftSelect.multiple='true';
    this.leftSelect.size=5;
    this.rightSelect = document.createElement("select");
    this.rightSelect.multiple='true';
    this.rightSelect.size=5;
    
    dojo.event.connect(this.leftSelect, "ondblclick", dojo.lang.hitch(this, this.sendRight));
    dojo.event.connect(this.rightSelect, "ondblclick", dojo.lang.hitch(this, this.sendLeft));

    this.initializeSelectElements();
    
    /** fill contentDiv with select elements, labels and buttons */
    var contentDiv = document.createElement('div');
    contentDiv.style.float = "left";
    dojo.html.addClass(contentDiv, "forms-doubleList");
    dojo.html.addClass(contentDiv, "forms");
    dojo.html.addClass(contentDiv, "doubleList");
    dojo.html.addClass(contentDiv, "dsy-doubleList");

    if (this.usePopup) {
      this.popupContainer.domNode.appendChild(contentDiv);
    } else {
      this.expandDiv.domNode.appendChild(contentDiv);
      this.expandDiv.hide();
    }
    
    var table = document.createElement("table");
    contentDiv.appendChild(table);
    
    var tbody = document.createElement("tbody");
    table.appendChild(tbody);
    
    var row = document.createElement("tr");
    tbody.appendChild(row);
    
    var cell = document.createElement("th");
    row.appendChild(cell);
    cell.appendChild(document.createTextNode(i18n("mv-available")));
    
    cell = document.createElement("th");
    row.appendChild(cell);
    
    cell = document.createElement("th");
    row.appendChild(cell);
    cell.appendChild(document.createTextNode(i18n("mv-selected")));
    
    row = document.createElement("tr");
    tbody.appendChild(row);

    cell = document.createElement("td");
    row.appendChild(cell);
    cell.appendChild(this.leftSelect);
    
    cell = document.createElement("td");
    row.appendChild(cell);
    var right = this.createButton('sendRight', '>');
    var left = this.createButton('sendLeft', '<');
    var allRight = this.createButton('sendAllRight', '>>');
    var allLeft = this.createButton('sendAllLeft', '<<');
    cell.appendChild(right);
    cell.appendChild(document.createElement('br'));
    cell.appendChild(allRight);
    cell.appendChild(document.createElement('br'));
    cell.appendChild(left);
    cell.appendChild(document.createElement('br'));
    cell.appendChild(allLeft);
    
    cell = document.createElement("td");
    row.appendChild(cell);
    cell.appendChild(this.rightSelect);
    
    var okContainer = document.createElement("div");
    contentDiv.appendChild(okContainer);

    var okButton = document.createElement("input");
    okButton.type="button";
    okButton.value=i18n('ok');
    okContainer.appendChild(okButton);
    dojo.html.setStyleAttributes(okContainer, "text-align: right");
    /** make the contentDiv somewhat nicer to look at */
    dojo.html.setStyleAttributes(contentDiv, "background-color: #ebebeb; border: 1px solid gray; padding: .5em;");

    /** listen for events */
    dojo.event.connect(this.widgetBase, "onclick", this, "togglePopup");
    dojo.event.connect(okButton, "onclick", this, "togglePopup");
    if (this.usePopup) {
      dojo.event.connect(this.popupContainer, "close", this, "updateSelection");
    }
  },
  initializeSelectElements: function() {
    this.leftValues = {};
    this.rightValues = {};
    this.moveLeft = {};
    this.moveRight = {};
    for (var i=0;i<this.selectElement.options.length;i++) {
      var o = this.selectElement.options[i];
      if (o.selected) {
        this.moveRight[o.value] = true;      
      } else {
        this.moveLeft[o.value] = true;
      }
    }
    this.updateCompactRepresentation();
    this.updateDoubleListbox();
  },
  addOption: function(element, hook, value, label) {
    element.options[element.options.length] = new Option (label, value);
    if (hook != null) {
      // using connectOnce makes sure we do not have to do dojo.event.disconnect ourselves.
      dojo.event.connectOnce(opt, "onDblClick", hook);
    }
  },
  createButton: function(func, label) {
    var button = document.createElement('input');
    button.type='button';
    button.value = label;
    dojo.event.connect(button, "onclick", this, func);
    return button;
  },
  sendLeft: function() {
    this.moveLeft = this.getSelectionMap(this.rightSelect);
    this.updateDoubleListbox();
  },
  sendRight: function() {
    this.moveRight = this.getSelectionMap(this.leftSelect);
    this.updateDoubleListbox();
  },
  sendAllLeft: function() {
    this.moveLeft = this.getAllValues(this.rightSelect); 
    this.updateDoubleListbox();
  },
  sendAllRight: function() {
    this.moveRight = this.getAllValues(this.leftSelect); 
    this.updateDoubleListbox();
  },
  getAllValues: function(select) {
    var result = {};
    for (var i=0;i<select.options.length;i++) {
      var o = select.options[i];
      result[o.value] = o.value;
    }
    return result;
  },
  getSelectionMap: function(select) {
    /*
    * @param select a HtmlSelectElement
    * returns an array containing the values of all selected options
    */
    var result = {};
    for (var i=0;i<select.options.length;i++) {
      if (select.options[i].selected) {
        result[select.options[i].value] = true;
      }
    }
    return result;
  },
  getSelectionArray: function(select) {
    var result = [];
    for (var i=0;i<select.options.length;i++) {
      var o = select.options[i]; 
      if (o.selected) {
        result.push(o.value);
      }
    }
    return result;
  },
  updateDoubleListbox: function() {
    var hasChanges = false;
    for (x in this.moveRight) {
      hasChanges = true;
      this.rightValues[x] = true;
      delete this.leftValues[x];
    }
    for (x in this.moveLeft) {
      hasChanges = true;
      this.leftValues[x] = true;
      delete this.rightValues[x];
    }
    if (hasChanges) {
      this.updateSelectBox(this.leftValues, this.leftSelect);
      this.updateSelectBox(this.rightValues, this.rightSelect);
      this.moveLeft = {};
      this.moveRight = {};
    }
    if (!this.usePopup) {
      this.updateSelection();
    }
  },
  removeValues: function(values, select) {
    for (var i=0;i<select.options.length;i++) {
      var o = select.options[i];
      if (values[o.value]) {
         select.options[i] = null;
      }
    }
  },
  updateSelectBox: function(values, select) {
    this.removeAllOptions(select);
    for (var i=0;i<this.selectElement.options.length;i++) {
      var o = this.selectElement.options[i];
      if (values[o.value]) {
        var hook = (select == this.rightSelect)?this.hitchedSendLeft:this.hitchedSendRight;
        this.addOption(select, hook, o.value, o.text);
      }
    }
  },
  removeAllOptions: function(select) {
    for (var i=(select.options.length-1); i>=0; i--) { 
      select.options[i] = null;
    } 
    select.selectedIndex = -1; 
  },
  togglePopup: function(evt) {
    if (this.usePopup) {
      if (this.popupContainer.isShowingNow) {
          this.popupContainer.close();
      } else {
          var target = dojo.html.getEventTarget(evt);
          this.popupContainer.open(target, {}, target);
      }
    } else {
      this.expandDiv.toggleShowing();
    }
  },
  updateSelection: function() {
    var values = this.getAllValues(this.rightSelect);
    for (var i=0;i<this.selectElement.options.length;i++) {
      var o = this.selectElement.options[i];
      o.selected = values[o.value] || false;
    }
    this.updateCompactRepresentation();
  },
  updateCompactRepresentation: function() {
    dojo.dom.removeChildren(this.compact);
    this.compact.title = '';
    
    var selection = this.getSelectionArray(this.selectElement);
    if (selection.length == 0) {
      dojo.html.addClass(this.compact, "dsy-double-listbox-popup-none");
      this.compact.appendChild(document.createTextNode(i18n("select-any")));
    } else {
      dojo.html.removeClass(this.compact, "dsy-double-listbox-popup-none");
      var selectionText = selection.join(", ");
      var node;
      if (selectionText.length > 50) {
        node = document.createTextNode(selectionText.substring(0, 48) + '...');
        this.compact.title = selectionText;
      } else {
        node = document.createTextNode(selectionText);
      }
      this.compact.appendChild(node);
    }
  },
  widgetsInTemplate: true,
  isContainer: false,
  daisy: daisy, // used to make the global daisy variable available in the template (e.g. ${this.daisy.skin} becomes the daisy skin)
  inputId: undefined, // attribute parameter
  usePopup: 'true' // set to false to use a popdown instead of a popup 
});

/** TODO: duplicated from daisy_edit.js, should be moved to a shared location (daisy.util.Daisy18N?) */
function i18n(key) {
    var translated = window.i18n_bundle[key];
    if (translated == null)
        return key;
    else
        return translated;
}
