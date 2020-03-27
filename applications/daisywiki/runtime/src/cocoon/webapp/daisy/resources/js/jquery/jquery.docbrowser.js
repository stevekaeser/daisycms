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
;(function($) {

// FIXME: Avoid using dojo (requires a jquery linkWidget & another i18n approach)

var _userMsg;

if (typeof dojo != 'undefined') { // i18n can only be loaded if dojo is available (fortunately it is only needed in cases in the document browser popup, and dojo is available there)
  dojo.require("dojo.i18n.common");
 dojo.require("daisy.widget.LinkEditor");
  dojo.requireLocalization("daisy.widget", "messages", null, /* available languages, to avoid 404 requests */ "ROOT,nl,de,es,ru,fr");
  _userMsg = dojo.i18n.getLocalization("daisy.widget", "messages");
}

function i18n(key) {
  if (typeof _userMsg == 'undefined') {
    return key;
  } else {
    return _userMsg[key] || key;
  }
}

function invertMap(m) {
    var result = {};
    if (typeof m != 'undefined') {
        for (key in m) {
            result[m[key]] = key;
        }
    }
    return result;
}

// If the UI scope is not available, add it
$.ui = $.ui || {};

// Make sure progress indicator image gets loaded
var progressIndicatorImage = new Image();
var imgpath = daisy.mountPoint + "/resources/skins/" + daisy.skin + "/images/";
progressIndicatorImage.src = imgpath + "progress_indicator_flat.gif";

$.fn.extend({

    daisyDocumentBrowser: function(options, data) {
        var args = Array.prototype.slice.call(arguments, 1);
        
        if (typeof options == "string" && options == 'getQuerySearchParams') {
            return this.map(function() {
                var docbrowser = $.data(this, "daisy-documentbrowser");
                return docbrowser[options].apply(docbrowser, args);
            });
        } else if (typeof options == "string") {
            return this.each(function() {
                var docbrowser = $.data(this, "daisy-documentbrowser");
                docbrowser[options].apply(docbrowser, args);
            });
        } else if (!$(this).is(".daisy-documentbrowser")) {
            return this.each(function() {
                
                $.data(this, "daisy-documentbrowser", new $.ui.daisyDocumentBrowser(this, options));
            });
        }
    },
    attrIgnorecase: function(name){
        return $(this).attr(name) || $(this).attr(name.toLowerCase());
    }
   
});

// TODO: currently still dojo based (reusing the existing LinkEditor dojo widget)
var SingleSelection = function(dojoLinkWidget, options, ui) {
    this.options = options = $.extend({}, SingleSelection.defaults, options);
    this.ui = ui;
    this.linkWidget = dojoLinkWidget;
    this.linkWidget.setCustomContext(window.daisyContextBranch, window.daisyContextLanguage);
    // TODO: UI feedback: checkbox before selected value.  Take changes to the linkWidget into account!
    this.selectedValue = "";

    this.branchesById = invertMap(availableBranches);
    this.languagesById = invertMap(availableLanguages);

    if (options.initialSelection) {
        for (var i=0;i<options.initialSelection.length;i++) {
            var selected = options.initialSelection[i];
            var branchId = selected.branch;
            if (isNaN(branchId) && typeof availableBranches[branchId] != 'undefined') {
                branchId = availableBranches[branchId];
            }
            var languageId = selected.language;
            if (isNaN(languageId) && typeof availableLanguages[languageId] != 'undefined') {
                languageId = availableLanguages[languageId];
            }
            this.documentSelected(selected.documentId, branchId==null?window.daisyContextBranch:branchId, languageId==null?window.daisyContextLanguage:languageId, selected.version)
        }
    }    
}

SingleSelection.prototype = {
    documentSelected: function(documentId, branchId, languageId, versionMode){
          // Set the value of the link widget
          var variantKey = documentId;
          if (branchId != window.daisyContextBranch || languageId != window.daisyContextLanguage) {
            variantKey += "@";
            variantKey += window.daisyContextBranch != branchId ? this.branchesById[Number(branchId)]:'';
            variantKey += ":";
            variantKey += window.daisyContextLanguage != languageId ? this.languagesById[Number(languageId)]:'';
          }
          this.documentSelectedKey(variantKey);
    },
    documentSelectedKey: function(variantKey, versionMode) {
          var link = "daisy:" + variantKey;
          
          this.linkWidget.setValue(link);
          if (this.selectedValue) {
              this.clearCheckmark();
          }
          this.selectedValue = variantKey;
          var row = this.ui.getCurrentRows()[variantKey];
          if (row) {
              row.addClass("selected");
          }
          // Since the dialog doesn't accept an empty answer,
          // there is no point in showing the clear button
          //$("#dsyClearSelection").show();
    },
    clearCheckmark: function() {
        var row = this.ui.getCurrentRows()[this.selectedValue];
        if (row) {
            row.removeClass("selected");
        }
    },
    getResultParameters: function() {
        var link = this.linkWidget.getValue();
        var parsedLink = daisy.util.parseDaisyLink(link);
        if (parsedLink == null) {
          alert(i18n('docbrowser.invalid-link'));
          return;
        }

        var docName = this.linkWidget.getDocumentName();
        var params = new Object();

        params.docId = parsedLink.documentId;
        params.docName = docName;
        
        params.url = daisy.util.formatDaisyLink(parsedLink);
        params.branch = parsedLink.branch;
        params.language = parsedLink.language;
        params.version = parsedLink.version;
        params.fragmentId = parsedLink.fragmentId;
        
        return params;
    },
    isSelected: function(documentId, branchId, languageId) {
      var variantKey = documentId + "@" + branchId + ":" + languageId;
      if (this.selectedValue && this.selectedValue == variantKey)
        return true;
      return false;
    },
    clear: function() {
        if (this.selectedValue) {
            this.linkWidget.setValue(null);
            this.clearCheckmark();
            $("#dsyClearSelection").hide();
        }
    }
}

SingleSelection.defaults = {};

MultiSelection = function(container, options, ui) {
    this.options = options = $.extend({}, MultiSelection.defaults, options);

    this.selection = {};
    this.selectionList = [];
    this.container = $(container);
    this.ul = $(document.createElement("ul"));
    this.ui = ui;

    this.container.empty();
    this.container.append(this.ul);

    this.branchesById = invertMap(availableBranches);
    this.languagesById = invertMap(availableLanguages);

    if (options.initialSelection) {
        for (var i=0;i<options.initialSelection.length;i++) {
            var selected = options.initialSelection[i];
            var branchId = selected.branch;
            if (isNaN(branchId) && typeof availableBranches[branchId] != 'undefined') {
                branchId = availableBranches[branchId];
            }
            var languageId = selected.language;
            if (isNaN(languageId) && typeof availableLanguages[languageId] != 'undefined') {
                languageId = availableLanguages[languageId];
            }
            this.addToSelection(selected.documentId, branchId==null?window.daisyContextBranch:branchId, languageId==null?window.daisyContextLanguage:languageId, selected.version)
        }
    }    
}

MultiSelection.prototype = {
    // toggle the selected document
    addToSelection: function(documentId, branchId, languageId, versionMode) {
        var variantKey = documentId + "@" + branchId + ":" + languageId;
        var row = this.ui.getCurrentRows()[variantKey];
        if (row) {
            row.addClass("selected");
        }                // create Link Editor
        var link = dojo.widget.createWidget("daisy:LinkEditor");
        var daisyLink = daisy.util.formatDaisyLink({
             documentId: documentId,
             branch: branchId == window.daisyContextBranch ? null : this.branchesById[Number(branchId)],
             language: languageId == window.daisyContextLanguage ? null : this.languagesById[Number(languageId)]
        });
        link.setValue(daisyLink);
        var li = $(document.createElement("li"));
        li.append(link.domNode);
        
        // append a delete button
        var me = this;
        var del = $("<span style='cursor:pointer'>[x]</span>").click(function(){
            //var representation = me.selection[variantKey];
            var li = $(this).parent();
            me.removeSingleLi(li, documentId, branchId, languageId);
        });
        li.append(del);
        
        if (this.selection[variantKey]) {
          var t = this.selection[variantKey];
          this.selection[variantKey].lis.push(li);
        } else {
          // TODO: verify meaning of -1 and -2.
          var representation = {
              lis: [li],
              widget: link,
              key: variantKey,
              documentId: documentId,
              branchId: branchId,
              languageId: languageId
          };
          this.selection[variantKey] = representation;
          this.selectionList.push(representation);
        }
        
        this.ul.append(li);
    },
    cleanRepresentation: function(variantKey, representation) {
        if (representation.lis.length == 0) {
            delete this.selection[variantKey];
            
            var idx = $.inArray(representation, this.selectionList);
            if  (idx != -1) {
              this.selectionList.splice(idx, 1);
            }
            var row = this.ui.getCurrentRows()[variantKey];
            if (row) {
                row.removeClass("selected");
            }
        }
    },
    removeSingleLi: function(li, documentId, branchId, languageId, versionMode) {
        var variantKey = documentId + "@" + branchId + ":" + languageId;
        var representation = this.selection[variantKey];
        if (representation) {
            li.remove();
            representation.lis.splice(representation.lis.indexOf(li), 1);
            this.cleanRepresentation(variantKey, representation);
        }
    },
    removeSingle: function(documentId, branchId, languageId, versionMode) {
        var variantKey = documentId + "@" + branchId + ":" + languageId;
        var representation = this.selection[variantKey];
        if (representation) {
            representation.lis[0].remove();
            representation.lis.shift();

            this.cleanRepresentation(variantKey, representation);
        }
    },
    documentSelected : function(documentId, branchId, languageId,
                versionMode) {
        var variantKey = documentId + "@" + branchId + ":" + languageId;
        var representation = this.selection[variantKey];
        if (representation) {
            for (var li in representation.lis) {
              representation.lis[li].remove();
            }
            representation.lis = [];
            this.cleanRepresentation(variantKey, representation);
        }
        else {
            this.addToSelection(documentId, branchId, languageId, versionMode);
        }
        $("#dsyClearSelection")[this.selectionList.length==0?'hide':'show']();
    },
    getResultParameters: function() {
        var params = [];

        for (var i=0;i<this.selectionList.length;i++) {
            var selected = this.selectionList[i];

            var branch = selected.branchId != window.daisyContextBranch ? this.branchesById[selected.branchId] : null;
            var language = selected.languageId != window.daisyContextLanguage ? this.languagesById[selected.languageId] : null;
            
            params.push({
              url : daisy.util.formatDaisyLink({ documentId: selected.documentId, branch: branch, language: language }),
              docId : selected.documentId,
              docName : selected.widget.getDocumentName(), 
              branch : selected.branchId,
              language : selected.languageId,
              version : selected.versionId || null,
              fragmentId : selected.fragmentId || null // link fields can not contain fragmentIds, is there an option to omit 'fragmentId' selection in linkeditors?
            });
        }
        return params;
    },
    isSelected: function(documentId, branchId, languageId) {
      var variantKey = documentId + "@" + branchId + ":" + languageId;
      if (this.selection[variantKey])
        return true;
      return false;
    },
    clear: function() {
        for (var key in this.selection) {
            var selected = this.selection[key];
            this.documentSelected(selected.documentId, selected.branchId, selected.languageId, false);
        }
    }
};

$.ui.getPageMap = function (url) {
    var map = {};
    var href = "";
    if (url.indexOf("#") > 0) {
        href = url.replace(/^.*#/, "");
    }
    var arr = href.split("&");
    for (var i = 0; i < arr.length; i++) {                
        var frags = arr[i].split("=");
        var key = frags[0];
        var value = new Number(frags[1]);
        if (key != null && key.length > 0)
            map[key] = value;
    }
    return map;
};

$.ui.daisyDocumentBrowser = function(container, options) {
    this.options = options = $.extend({}, $.ui.daisyDocumentBrowser.defaults, options);
    this.rootElement = $(container);
    this.element = $(document.createElement('div'));
    this.mode = options.startMode;
    this.sortColumn = options.initialSortColumn;
    this.sortOrder = options.initialSortOrder;
    this.page = this.options.page;
    this.faceted = this.options.faceted;
    
    $(this.element).attr("style","display:table;width:100%;");
    $(container).addClass("daisy-documentbrowser");
    $(container).css("width","100%");
    
    this.errorDialog = $('<div title="Error"></div>');
    this.errorDialog.hide();
    $(document.body).append(this.errorDialog);
    this.errorDialogInit = false;
    
    this.querySource = this.options.querySource || this;
    
    this.rootElement.append(this.element);
    
    this.ui = this.createUI();
    this.selection = this.createSelection();
    
    if(options.dataAvailable != undefined && options.dataAvailable){
        var $container = $(container);
        var dom = parseXML(($container.find(".dataresult"))[0].innerHTML);
        var $dom = $(dom); 

       this.ui.update($dom.children("searchresult"));
    } else if (this.options.loadImmediately) {
       this.update();
    }
};

function parseXML(xml){
    if (window.ActiveXObject && window.GetObject) {
        var dom = new ActiveXObject('Microsoft.XMLDOM');
        dom.loadXML(xml);
        return dom;
    }
    if (window.DOMParser) 
        return new DOMParser().parseFromString(xml, 'text/xml');
    throw new Error('No XML parser available');
}

var SimplePager = function(browser) {
    this.browser = browser;
    this.element = $("<div style='display: table-row; float:right;'></div>");
    this.current = $("<span class='pager'></span>");   
    var rootId = browser.rootElement.attr("id")
    this.first = $("<a href='#" + rootId +"=1'><img class='pager' src='" + imgpath +"first.png'/></a>");
    this.prev = $("<a href='#" + rootId +"=0'><img class='pager' src='" + imgpath +"previous.png'/></a>");    
    this.last = $("<a href='#" + rootId +"=last'><img class='pager' src='" + imgpath +"last.png'/></a>");    
    this.next = $("<a href='#" + rootId +"=2'><img class='pager' src='" + imgpath +"next.png'/></a>");
    
    this.element.append(this.current);
    this.element.append(this.first);
    this.element.append(this.prev);
    this.element.append(this.next);
    this.element.append(this.last);   
    
    var updateUrls = function (currentPage, data) {
        var rootId = data.browser.rootElement.attr("id");
        var re = new RegExp(rootId + "=[0-9]*", "g");
        
        var mapToHref = function (map) {            
            var arr = [];
            for (var key in map) {
                if (key != null && key.length > 1)
                    arr.push(key + "=" + map[key]);
            }
            return "#" + arr.join("&");
        }
        var pageMap = $.ui.getPageMap(window.location.href)    
        
        pageMap[rootId] = currentPage - 1;
        data.prev.attr("href", mapToHref(pageMap));
        
        pageMap[rootId] = currentPage + 1;
        data.next.attr("href", mapToHref(pageMap));
    }
    
    // bind events for first/last
    this.first.bind("click", this, function(ev) {
      if (ev.data.browser.page > 1) {
        updateUrls(ev.data.browser.page, ev.data);
        ev.data.browser.goToPage(1);        
      }
    });
    this.last.bind("click", this, function(ev) {
      if (ev.data.browser.page < ev.data.lastPage) {
        updateUrls(ev.data.browser.page, ev.data);
        ev.data.browser.goToPage(ev.data.lastPage);
      }
    });
    
    // bind events for prev/next
    this.prev.bind("click", this, function(ev) {
      if (ev.data.browser.page > 1) {
        updateUrls(ev.data.browser.page, ev.data);
        ev.data.browser.goToPage(ev.data.browser.page - 1);
      }
    });
    this.next.bind("click", this, function(ev) {
      if (ev.data.browser.page < ev.data.lastPage) {
        updateUrls(ev.data.browser.page, ev.data);
        ev.data.browser.goToPage(ev.data.browser.page + 1);
      }
    });
    
}

SimplePager.prototype.update = function(chunkOffset, chunkLength, total, current, lastPage){
    if (lastPage == 0) {
        this.current.text("");
    } else {
      this.current.text(chunkOffset + " - " + (chunkOffset + chunkLength - 1) + " of " + total);
    }
    this.lastPage = lastPage;
    // update the disabled images and remove the cursor pointer if button is disabled
    if (current > this.lastPage) {
      current = 1;
    }
    
    // First/previous is available?
    if (current != 1) {
        $("img.pager",this.first).attr('src', imgpath + 'first.png');
        $("img.pager",this.prev).attr('src', imgpath + 'previous.png');
        this.first.removeClass('disabled');
        this.prev.removeClass('disabled');
    }
    else {
        $("img.pager",this.first).attr('src', imgpath + 'first-disabled.png');
        $("img.pager",this.prev).attr('src', imgpath + 'previous-disabled.png');
        this.first.addClass('disabled');
        this.prev.addClass('disabled');
    }
    
    // Next/last is available?
    if (lastPage != 0 && current != lastPage) {
        $("img.pager",this.next).attr('src', imgpath + 'next.png');
        $("img.pager",this.last).attr('src', imgpath + 'last.png');
        this.next.removeClass('disabled');
        this.last.removeClass('disabled');
        
    }
    else {
        $("img.pager", this.next).attr('src', imgpath + 'next-disabled.png');
        $("img.pager", this.last).attr('src', imgpath + 'last-disabled.png');
        this.next.addClass('disabled');
        this.last.addClass('disabled');
    }
};

var TableUI = function(browser, element, columns, noExtraColumns) {
    this.browser = browser;
    this.element = element;
    this.noExtraColumns = noExtraColumns;
    element.attr("style", "display:table; width: 100%");
    element.empty();
    this.showingRows = {};
    
    if (!columns) {
        this.columns = this.browser.options.columns;
    } else {
        this.columns = columns;
    }

    var table = $("<table class='default result' id='result' width='100%'></table>");
    table.css("max-height", "360px");    
    this.tbody = $("<tbody></tbody>");
    table.append(this.tbody);

    this.pager = new SimplePager(browser);
    element.append(this.pager.element);
    
    var tableDiv = $("<div style='display: table-row;'></div>");    
    $(tableDiv).append(table);
    element.append(tableDiv);
    
    // add the faceted browser to the GUI
   if (this.browser.faceted && this.browser.faceted.length>0) {
        this.updateFacets(this.browser.getQuery(), this.browser.options.versionMode);
    }
};

/**
 * Update the piece of faceted browsing html on the GUI
 * @param {Object} facetContent
 */
TableUI.prototype.updateFacets = function(query, versionMode){
    var params=this.getFacetedSearchParams(query, versionMode);
    var browser = this.browser;
    params['config']='';
    $.ajax({
        type: "GET",
        data: params,
        cache: false,
        url: daisy.mountPoint + "/" + daisy.site.name + "/facetedBrowser/" + this.browser.options.faceted + "/html?layoutType=plain",
        success: function(msg){
            $("#faceted").empty();
            // find the facets div in the html and place it in emptied #faceted
            $("#faceted").append($("div#facets", msg).children());
            facetedConfs= getCurrentFacetConfs($("div#data", msg));
        },
        error: function(msg, a, b, c) {
          browser.handleAjaxError(msg, a, b, c);
        }
    })
}

TableUI.prototype.loading = function(){
    var tbody = this.tbody;
    tbody.find("tr:gt(0)").remove();
    var row = $(document.createElement("tr"));
    var td = $(document.createElement("td"));
    td.attr("colspan", this.browser.options.columns.length);
    this.browser.options.columns.length
    row.append(td);
    td.append(document.createTextNode("loading..."));
    tbody.append(row);
}

TableUI.prototype.update = function(dataResult, versionMode){
    var tbody = this.tbody;
    tbody.empty();
    var browser = this.browser;
    var ui = this;
    
    var hrow = $(document.createElement("tr"));
    this.tbody.append(hrow);

    var func = function(ev){
        browser.sort(ev.data, (browser.sortColumn == ev.data) ? !browser.sortOrder : true);
    };
    
    for (var i=0; i<this.columns.length; i++) {
        var colLabel=null;
        if(browser.options.columnLabels && browser.options.columnLabels.length>0){
            colLabel = browser.options.columnLabels[i];
        } else {
            colLabel = this.columns[i];
        }
        if (this.columns[i] && (!browser.options.columnLabels || browser.options.columnLabels[i])) {
            var th = $(document.createElement("th"));
            hrow.append(th);
            
            th.append(document.createTextNode(colLabel));
            
            var sortValues = this.browser.options.sortingValues;
            var sortValue = (sortValues)?sortValues[i]:this.columns[i];
            if(sortValue)
                th.bind("click", sortValue, func);
        }
    }

    if (!this.noExtraColumns) {
        if(browser.options.columnLabels && browser.options.columnLabels.length > this.columns.length){
            for (var i = this.columns.length; i < browser.options.columnLabels.length; i++) {
                var th = $(document.createElement("th"));
                th.append(browser.options.columnLabels[i]);
                th.bind("click", this.columns[i], func);
                hrow.append(th);
            }
        }
    }
    
    var needsCallback =  (browser.options.columnActions && browser.options.columnActions.length > 0);
    
    // use children() instead of find() because of case sensitivity in certain cases (http://dev.jquery.com/ticket/1991)
    dataResult.children("rows").children("row").each(function(rowIndex){
        var resultObj = {};
        var resultRow = this;
        var row = $(document.createElement("tr"));
        var jqResultRow = $(resultRow);
        var docId = jqResultRow.attrIgnorecase("documentId");
        var branchId = jqResultRow.attrIgnorecase("branchId");
        var languageId = jqResultRow.attrIgnorecase("languageId");
        variantKey = docId + "@" + branchId + ":" + languageId;

        var row_click = function(ev){
            ev.data.browser.documentSelected(docId, branchId, languageId, ev.data.versionMode);
        }        

        tbody.append(row);
        
        $(this).children().each(function(n){
            // build an object containing the labels to display and to give to callback function if needed
            var t = $(this);
            if ($(t).is("multivalue")) { 
                var text = "";
                t.children().each(function(){
                    var t2 = $(this);
                    text += (t2.is(":first-child") ? "" : ", ");
                    text += t2.text();
                });
               resultObj[n] = text;
            }
            else {
                resultObj[n] = t.text();
            }
        });
        
        // for each value in resultset
        $(this).children().each(function(n){
            // check if columnNames was defined: this checks if the column should be displayed (then there is
            // a label defined for this column    
            var t = $(this);
            var text=resultObj[n];
            var node;
            var showColumn = (!browser.options.columnLabels || !browser.options.columnLabels.length>0 || browser.options.columnLabels[n]) && (n < ui.columns.length);     

            if(this.nodeName.toLowerCase() == "linkvalue"){
                node = $(document.createElement("a"));
                var href = browser.options.searchResultBasePath + t.attrIgnorecase("documentId") + ".html?branch=" + t.attrIgnorecase("branchId") + "&language=" + t.attrIgnorecase("languageId");
                node.attr("href", href);
                node.append(text);
            }else{
                if (browser.options.columnActions && browser.options.columnActions[n]) {
                    var docProperties = ui.createDocproperties(docId, branchId, languageId);
                    node = browser.options.columnActions[n](docProperties, resultObj);
                }
                else {
                    node = document.createTextNode(text);
                }
            }
                
            if (showColumn) {
                var td = $(document.createElement("td"));
                if (n == 0) {
                    td.addClass("first");
                    ui.getCurrentRows()[variantKey] = td;
                    if (browser.isSelected(docId, branchId, languageId)) {
                        td.addClass("selected");
                    }
                }
                td.append(node);
                row.append(td);
            }
            
            if (browser.options.valueCreateCallback) {
                browser.options.valueCreateCallback(text, node, n);
            }
        });
        
        
        if (browser.options.columnLabels && browser.options.columnLabels.length > browser.options.columns.length) {
            for (var i = browser.options.columns.length; i < browser.options.columnLabels.length; i++) {
                var docProperties = ui.createDocproperties(docId, branchId, languageId);
                var td = ui.createActionNode(browser.options.columnActions[i], docProperties, resultObj);
                row.append(td);
            }
        }
        row.bind("click", {
            row: resultRow,
            browser: browser,            
            versionMode: browser.options.versionMode
        }, row_click);
        
        if (browser.options.rowCreateCallback) {
            browser.options.rowCreateCallback(resultRow, row, rowIndex)
        }
    });
    
    var info = dataResult.children("resultInfo")[0];
    if (info) {
        var $info = $(info);
        var size = Number($info.attr("size"));
        
        var rCL = Number($info.attrIgnorecase("requestedChunkLength"));
        if(rCL == 0) rCL = size;
        var rCO = Number($info.attrIgnorecase("requestedChunkOffset"));
        var CL = Number($info.attrIgnorecase("chunkLength")) || size;
        var lastPage = 1 + Math.floor(((size - 1) / rCL));
        var page = 1 + Math.floor(+((rCO - 1) / rCL));
        
        this.pager.update(rCO, CL, size, page, lastPage);
    }
};

TableUI.prototype.createDocproperties = function(docId, branchId, languageId){
    var docProperties={};
    docProperties.documentId = docId;
    docProperties.branchId = branchId;
    docProperties.languageId = languageId;
    return docProperties;
};

TableUI.prototype.createActionNode = function(columnAction, docProperties, resultObj){
    var td = $(document.createElement("td"));
    td.append(columnAction(docProperties, resultObj));
    return td;
};


TableUI.prototype.getQuerySearchParams = function(query, versionMode){
  var params={};
  var queryString = $.trim(query.toString()).replace("\n"," ");
  params['daisyquery']=queryString;
  params['forms_submit_id']= "send";
  params['chunkOffset']=(((this.browser.page-1) * this.browser.options.chunkLength) + 1);
  params['chunkLength']=this.browser.options.chunkLength;
  params['config']=this.browser.options.config;
  if (this.browser.options.contextDocument) {
      params['contextDocument'] = this.browser.options.contextDocument;
  }

  if (document.getElementById("orderBy")) {
      var orderBy = document.getElementById("orderBy").options[document.getElementById("orderBy").selectedIndex].value;
      params["extraOrderBy"] = orderBy;
  }
  var whereClauses = query.whereClauses; 
  if(whereClauses!=undefined && whereClauses.length>0)
    params['where'] = whereClauses.join(" and ");
  if(this.browser.sortColumn!=undefined)
    params["extraOrderBy"] = "ORDER BY "+this.browser.sortColumn + " " + (this.browser.sortOrder?"asc":"desc");
  var options = query.options; 
  if(options!=undefined)
    params["optionList"] = options;
  return params;
}
TableUI.prototype.getFacetedSearchParams = function(query, versionMode){
  var params={};
  var queryString = query.toString();
  params['daisyquery']=queryString;
  params['forms_submit_id']= "send";
  params['cho']=(((this.browser.page-1) * this.browser.options.chunkLength) + 1);
  params['chl']=this.browser.options.chunkLength;
  params['config']=this.browser.options.config;

  for (var i = 0; i < filters.length; i++) {
      var prefix = "f." + (i + 1);
      params[prefix + ".fn"] = filters[i].facetName;
      params[prefix + ".qv"] = filters[i].queryValue;
      params[prefix + ".dv"] = filters[i].displayValue;
      params[prefix + ".o"] = filters[i].operator;
      params[prefix + ".d"] = filters[i].isDiscrete;
  }
  
  for (var i = 0; i < facetConfs.length; i++) {
      var prefix = "fc." + (i + 1);
      params[prefix + ".mv"] = facetConfs[i].maxValues;
      params[prefix + ".sv"] = facetConfs[i].sortOnValue;
      params[prefix + ".sa"] = facetConfs[i].sortAscending;
  }  
  var whereClauses = query.whereClauses; 
  if(whereClauses!=undefined && whereClauses.length>0)
    params['where'] = whereClauses.join(" and ");

  var options = query.options; 
  if(options!=undefined && options.length>0)
    params['opt'] = options.join(", ");

  if (this.browser.sortColumn != undefined)
    params["ord"] = this.browser.sortColumn + " " + (this.browser.sortOrder ? "asc" : "desc");

  if (query.ftsQuery) {
    params["options"] = "fts";
    params["ftsq"] = formatString(query.ftsQuery.query);
    params["ftsn"] = query.ftsQuery.name;
    params["ftsc"] = query.ftsQuery.content;
    params["ftsf"] = query.ftsQuery.fields;
  }
  return params;
}



TableUI.prototype.performQuery = function(query, versionMode) {
  var ui = this;
  var vm = versionMode;
  var params={};
  var url;
  
  var browser = this.browser;
 
  if (browser.faceted && browser.faceted.length>0) {
      params = ui.getFacetedSearchParams(query, versionMode);
      url = daisy.mountPoint + "/" + daisy.site.name + "/facetedBrowser/" + this.browser.faceted + "/xml?layoutType=plain";
  }
  else {
      params = ui.getQuerySearchParams(query, versionMode);
      url = daisy.mountPoint + "/" + daisy.site.name + "/querySearch/xml?layoutType=plain";
  }

  $.ajax({
        type: "GET",
        url: url, 
        data: params,
        traditional : true,
        cache: false,
        success: function(xmlResult) {
          ui.update($(xmlResult).find("data searchResult"), vm);
        },
        error: function(msg, a, b, c) {
          browser.handleAjaxError(msg, a, b, c);
        }
  });
  
}

TableUI.prototype.getCurrentRows = function() {
    return this.showingRows;
};

TableUI.prototype.selectionChanged = function(lastSelected) {
    variantKey = lastSelected.documentId + "@" + lastSelected.branchId + ":" + lastSelected.languageId;
};

var PreviewUI = function(browser, element) {
    this.browser = browser;
    var elem = element;
    elem.empty();
    elem.attr("style", "display:table;width:100%;");
    
    this.columns = ['id', 'name'];

    var tableDiv = $(document.createElement("div"));
    tableDiv.attr("style", "display:table;");
    this.tableHolder = tableDiv;
    this.tableUI = new TableUI(browser, tableDiv, this.columns, true);
    this.uriTemplate = new Template(browser.options.previewUriTemplate);
    
    this.preview = $(document.createElement("iframe"));
    
    var table = $(document.createElement("table"));
    table.attr("width", "100%");
    table.attr("border", "0");
    elem.append(table);
    var tbody = $(document.createElement("tbody"));
    table.append(tbody);
    
    var tr = $(document.createElement("tr"));
    tbody.append(tr);
    var left = $(document.createElement("td"));
    left.attr("width", "100%");
    left.css("vertical-align","top");
    tr.append(left);
    left.append(tableDiv);
    var right = $(document.createElement("td"));
    right.attr("width", "400px");
    right.css("vertical-align","top");
    tr.append(right);
    right.append(this.preview);
    this.tableUI.element.css("border", "1px solid gray");    
    this.preview.css({"border": "1px solid gray", "background-color": "white", "width": "400px", "height": "323px"});
    
    if (browser.lastSelected) {
        this.selectionChanged(browser.lastSelected);
    }

};

PreviewUI.prototype.getCurrentRows = function() {
    return this.tableUI.getCurrentRows();
}

PreviewUI.prototype.loading = function() {
    this.tableUI.loading();
}

PreviewUI.prototype.performQuery = function(queryString, versionMode) {
    this.tableUI.performQuery(queryString, versionMode);
}

PreviewUI.prototype.selectionChanged = function(lastSelected) {
    // progress indicator
    var previewWindow = this.preview.get(0).contentWindow;
    var doc = $(previewWindow.document);
    doc.css({
        "margin": "0px",
        "padding" : "0px"
    });
    
    var indicator = $("<p style='padding-left: 1em; margin-top: 1em; position: absolute; z-index : 3'><img src='" + progressIndicatorImage.src + "' alt='...'/></p>", previewWindow.document);
    $("body",previewWindow.document).empty();
    $("body",previewWindow.document).append(indicator);
    var documentURL = this.uriTemplate.expand(lastSelected);
     
    // actual document
    if (previewWindow.location.href == documentURL) {
      previewWindow.location.reload();
    } else {
      previewWindow.location.replace(documentURL);
    }

    // tableUI
    this.tableUI.selectionChanged(lastSelected);
}

PreviewUI.prototype.getQuerySearchParams = function(query, versionMode) {
    return this.tableUI.getQuerySearchParams(query, versionMode);
};

PreviewUI.prototype.updateFacets = function(query, versionMode){
    this.tableUI.updateFacets(query, versionMode);
}

PreviewUI.prototype.update = function(dataResult, versionMode){
    return this.tableUI.update(dataResult, versionMode, true);
}

Query = function(columns) {
  this.columns = columns;
  this.whereClauses = [];
  this.options = [];
  this.sortClauses = [];
}

Query.prototype = {
  addWhereClause: function(clause) {
    this.whereClauses.push(clause);
  },
  addOption: function(opt) {
    this.options.push(opt);
  },
  addSortClause: function(clause) {
    this.sortClauses.push(clause);
  },
  setLimitClause: function(clause) {
    this.limitClause = clause;
  },
  toString: function() {
    var result = "select " + this.columns.join(",");
    result += " where " + this.whereClauses.join(" and ");
  
    if (this.sortClauses.length > 0) {
      result += " order by " + this.sortClauses.join(", ");
    }
  
    if (this.limitClause) {
      result += " limit " + limitClause;
    }

    if (this.options.length > 0) {
      result += " option " + this.options.join(", ");
    }
    return result;
  }
};

function formatString(str) {
    return str.replace("'", "''", "g");
}

$.ui.daisyDocumentBrowser.prototype = {
    update: function(page) {
      if (this.selection) {
        this.selection.ui = this.ui;
      }
      if (typeof page != 'undefined') {
        this.page = page;
      }
      this.ui.loading();
      var query = this.getQuery();
      this.ui.performQuery(query, this.options.versionMode);
      if (this.options.faceted && this.options.faceted.length>0) 
        this.ui.updateFacets(query, this.options.versionMode);
    },
    sort: function(column, order) {
        this.page = 1;
        this.sortColumn = column;
        this.sortOrder = order;
        this.update();
    },
    getQuery: function() {
        var q = this.querySource.getBaseQuery();
        q.options=this.getOptions();
        q.whereClauses=this.getWhereClauses();
        q.ftsQuery=this.getFullTextQuery();

        // add chunking and sorting:
        if (this.sortColumn) {
          q.addSortClause(this.sortColumn + " " + (this.sortOrder?"asc":"desc"));
        }
        return q;
    },
    getQuerySearchParams: function() {
        var q = this.ui.getQuerySearchParams(this.getQuery(), this.options.versionMode);
        return q;
    },
    getFullTextQuery: function() {
        if (this.options.faceted && this.options.faceted.length>0) {
            var fulltextQuery = $.trim($("#fulltextQuery").val());
            if (fulltextQuery != '') {
                return { query: fulltextQuery, name: $("#searchName:checked").val()?true:false, content: $("#searchContent:checked").val()?true:false, fields: $("#searchContent:checked").val()?true:false };
            } else {
                return null;
            }
        } else {
            return null;
        }
    },
    getWhereClauses: function() {
      whereclauses = $.extend([], this.options.baseConditions);
      /* To overcome limitations with fulltext filtering, we don't always include the fulltext clause in the query sent to the server: 
       *    - when the facet search aspect is not enabled, we add the clause here (to the front of whereclauses)
       *    - when the facet search aspect is enabled, and the fulltext query is not empty, the fulltext clause is not added to the whereclauses, but 
       *      this is added to the request: option=fts&ftsq={fulltext query}&ftsn=...&ftsc=...&ftsf=...
       *      To handle this, use an optionList in your facet search, containing an option id='fts' to handle the fulltext clause using this:
       *            <defaultConditions>FullText('request-attr:ftsq|*') [...]</defaultConditions>  
       */      
      if (!(this.options.faceted && this.options.faceted.length>0)) {      
        var fulltextQuery = $.trim($("#fulltextQuery").val());
        var searchName = $("#searchName:checked").val()?1:0;
        var searchContent = $("#searchContent:checked").val()?1:0;
        var searchFields = $("#searchFields:checked").val()?1:0;
        if (fulltextQuery != '') {
          whereclauses.unshift("FullText('"+formatString(fulltextQuery)+"', "+searchName+", "+searchContent+", "+searchFields+")");
        }
      }
      
      var branchId = $("#branchId").val();
      if (branchId && branchId != "-1") 
        whereclauses.push("branchId=" + branchId);
        
      var languageId = $("#languageId").val();
      if (languageId && languageId != "-1") 
        whereclauses.push("languageId=" + languageId);
        
      var attachmentDocType = $("#attachmentDocType:first");
      if (attachmentDocType && attachmentDocType.length>0 && attachmentDocType[0].checked) 
         whereclauses.push("documentType!='Attachment'");
    
      var imageDocType = $("#imageDocType:first");
      if (imageDocType && imageDocType.length>0 && imageDocType[0].checked) 
        whereclauses.push("documentType!='Image'");
        
      var currentCollection = $("#currentCollection");
      if (currentCollection && currentCollection.length>0 && currentCollection[0].checked) 
        whereclauses.push("InCollection('"+daisy.site.collection+"')");
        
      var documentName = $.trim($("#name").val());
      if (documentName != '') {
        whereclauses.push("name like '%" + formatString(documentName) + "%'");
      }
      
      var predefined = $("#predefined").val();
      if(predefined != null){
          $.each(predefined, function(n,value){
            whereclauses.push(value);    
        });
      }
      
      return whereclauses;
    },
    getOptions: function() {
      var options = new Array();
      
      var versionMode = $("#versionMode").val();

      if (versionMode && versionMode != 'live')
        options.push("point_in_time='" + versionMode + "'");

      options.push("chunk_length=" + this.options.chunkLength);
      options.push("chunk_offset=" + (((this.page-1) * this.options.chunkLength) + 1));
        
      return options;
    },

    getBaseQuery: function() {
      var q = new Query(this.options.columns);
      return q;
    },
    createUI: function() {
        if (this.mode=="tableMode") {
            return new TableUI(this, $(this.element));
        } else {
            return new PreviewUI(this, $(this.element));
        }
    },
    createSelection: function () {
      var selection;
      if (this.options.multiSelect) {
          selection = new MultiSelection(this.options.selectionContainer, this.options, this.ui);
      } else {
          if (typeof dojo != "undefined") {
              var linkWidget = dojo.widget.byId(this.options.linkWidgetId);
              if (linkWidget) {
                  selection = new SingleSelection(linkWidget, this.options, this.ui);
              }
          } 
      }
      return selection;
    },
    goToPage: function(page) {
        this.page = page;
        this.update();
    },
    documentSelected: function(docId, branchId, languageId, versionMode) {
        this.lastSelected = { documentId:docId, branchId:branchId, languageId:languageId, versionMode:versionMode };
        if (this.selection) {
            this.selection.documentSelected(docId, branchId, languageId, versionMode);
        } else {
            this.selection = this.createSelection();
            if (this.selection != null)
                this.selection.documentSelected(docId, branchId, languageId, versionMode);
        }
        this.ui.selectionChanged(this.lastSelected);
    },
    isSelected: function(docId, branchId, languageId) {
        if (this.selection) {
            return this.selection.isSelected(docId, branchId, languageId);
        } else {
            return false;
        }
    },
    toggleMode: function() {
        switch(this.mode) {
            case 'previewMode':
              this.setMode('tableMode');
              $("#preview-toggle").attr('src',imgpath+'preview.png');
              break;
            default:
              this.setMode('previewMode');
              $("#preview-toggle").attr('src',imgpath+'table.png');
              break; 
        }
    },
    setMode: function(mode) {
        if (this.mode != mode) {
            this.mode = mode;
            this.ui = this.createUI();
            this.update();
        }
    },
    insertLink: function() {
        var params = this.selection.getResultParameters();
        if (typeof params == 'undefined') {
          return;
        } else {
          daisy.dialog.close(params);
        }
    },
    addFilter: function (facetName, queryValue, displayValue, operator, isDiscrete){
        filters.push(createFilter(facetName, queryValue, displayValue, operator, isDiscrete));
        //this.ui.performQuery(this.getQuery(), this.options.versionMode);
        this.ui.updateFacets(this.getQuery(), this.options.versionMode);
        return false;
    },
    removeFilter: function(index){
        var index = index - 1;
        var newFilters = filters.slice(0, index);
        filters = newFilters.concat(filters.slice(index + 1));
        //this.ui.performQuery(this.getQuery(), this.options.versionMode);
        this.ui.updateFacets(this.getQuery(), this.options.versionMode);
        return false;
    },
    removeAllFilters: function(index){
        filters=new Array();
        //this.ui.performQuery(this.getQuery(), this.options.versionMode);
        this.ui.updateFacets(this.getQuery(), this.options.versionMode);
        return false;
    },
    changeFacetSort: function(facetIndex, sortOnValue, sortAscending){
        facetConfs[facetIndex].sortOnValue = sortOnValue;
        facetConfs[facetIndex].sortAscending = sortAscending;
        //this.ui.performQuery(this.getQuery(), this.options.versionMode);
        this.ui.updateFacets(this.getQuery(), this.options.versionMode);
        return false;
    }, 
    showMoreFacetValues: function(facetIndex){
        facetConfs[facetIndex].maxValues = facetConfs[facetIndex].maxValues + 10;
        this.ui.updateFacets(this.getQuery(), this.options.versionMode);
        return false;
    },
    clearSelection: function() {
        this.selection.clear();
    },
    handleAjaxError: function(msg, a, b, c) {
        if (!this.errorDialogInit) {
            this.errorDialogInit = true;
            this.errorDialog.dialog({ autoOpen: false, modal: true, maxWidth: 600, maxHeight: 400, width: 600, height: 400 });
        }
        this.errorDialog.dialog('open');
        this.errorDialog.html(msg.responseText);
    }
}

function scopeCallback(callback, scope) {
    return function() {
        return callback.apply(scope, arguments);
    };
}

var filters=new Array();
var facetConfs=new Array();

function getCurrentFacetConfs(dataResult) {
    facetConfs = [];
    $("facetConfs facetConf",dataResult).each(function(){
        facetConfs.push(createFacetConf(this.attributes.getNamedItem("maxValues").nodeValue,this.attributes.getNamedItem("sortOnValue").nodeValue,this.attributes.getNamedItem("sortAscending").nodeValue));
    });
    return facetConfs;
}

function createFilter(facetName, queryValue, displayValue, operator, isDiscrete){
    var filter = new Object();
    filter.facetName = facetName;
    filter.queryValue = queryValue;
    filter.displayValue = displayValue;
    filter.operator = operator;
    filter.isDiscrete = isDiscrete;
    return filter;
}

function createFacetConf(maxValues, sortOnValue, sortAscending){
    var facetConf = new Object();
    facetConf.maxValues = maxValues;
    facetConf.sortOnValue = sortOnValue;
    facetConf.sortAscending = sortAscending;
    return facetConf;
}

$.extend($.ui.daisyDocumentBrowser, {
    defaults: {
        columns: [ "name" ],
        page: 1,
        chunkLength: 10,
        previewUriTemplate: daisy.mountPoint + '/' + daisy.site.name + '/{documentId}/version/{versionMode}?layoutType=plain&branch={branchId}&language={languageId}',
        versionMode: 'live',
        multiSelect: false,
        loadImmediately: true,
        initialSortColumn: "name",
        initialSortOrder: "asc",
        startMode: "previewMode",
        initialSortColumn: undefined,
        initialSortOrder: true, //true => ascending
        selectionContainer: "#documentSelection", // only used if multiSelect = true
        linkWidgetId: "daisyLink" // only used if multiSelect = false, widget id of the dojo LinkEditorWidget
    }
});

})(jQuery);
