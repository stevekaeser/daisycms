dojo.provide("daisy.widget.LinkEditor");
dojo.require("daisy.context");
dojo.require("dojo.html");
dojo.require("daisy.util");
dojo.require("daisy.dialog");
dojo.require("dojo.i18n.common");


dojo.requireLocalization("daisy.widget", "messages", null, /* available languages, to avoid 404 requests */ "ROOT,nl,fr,de,es,ru");

/**
 * A widget for editing daisy link values ("daisy:" links). It shows the document name
 * (loaded via an ajax request) and provides a popup for editing the link, browsing, ...
 */
dojo.widget.defineWidget(
    // widget name and class
    "daisy.widget.LinkEditor",

    // superclass
    dojo.widget.HtmlWidget,

    function() {
    },

    // properties and methods
    {
        isContainer: false,

        dropdownIcon: daisy.context.getResourceUri("widget/templates/images/dropdown.gif"),
        progressIcon: daisy.context.getResourceUri("widget/templates/images/progress_indicator_flat.gif"),

        templatePath: daisy.context.getResourceUri("widget/templates/LinkEditor.html"),
        templateCssPath: daisy.context.getResourceUri("widget/templates/LinkEditor.css"),

        readonlyMode: false, /* Indicates if this widget should behave in 'readonly' mode */

        containerToggle: "plain", /* plain, explode, wipe, fade */

        containerToggleDuration: 150,

        contextMode: "site", /* value: site, default or custom. Indicates whether missing branch/language should
                                default to those of the site, to main/default, or to custom value set via setCustomContext(...) */

        enableFragmentId: "false",

        enableBrowseDocuments: "true",

        openInNewWindows: "false", /* when the view and edit actions are used, should these open in new windows (rather than tabs on firefox) */

        // These counters are used to ignore obsolete ajax responses
        nameLoadCounter: 0,
        versionsLoadCounter: 0,
        fragmentIdsLoadCounter: 0,

        res: dojo.i18n.getLocalization("daisy.widget", "messages"),

        attachTemplateNodes: function() {
            // summary: use attachTemplateNodes to specify containerNode, as fillInTemplate is too late for this
            daisy.widget.LinkEditor.superclass.attachTemplateNodes.apply(this, arguments);

            this.docPopup = dojo.widget.createWidget("PopupContainer", {toggle: this.containerToggle, toggleDuration: this.containerToggleDuration});
            this.docPopupContainerNode = this.docPopup.domNode;

            this.versionsPopup = dojo.widget.createWidget("PopupContainer", {toggle: this.containerToggle, toggleDuration: this.containerToggleDuration})
            this.versionsPopupContainerNode = this.versionsPopup.domNode;

            this.fragmentIdPopup = dojo.widget.createWidget("PopupContainer", {toggle: this.containerToggle, toggleDuration: this.containerToggleDuration})
            this.fragmentIdPopupContainerNode = this.fragmentIdPopup.domNode;
        },

        fillInTemplate: function(args, frag) {
            daisy.widget.LinkEditor.superclass.fillInTemplate(this, args, frag);

            // copy id and name from original input to the hidden input
            var fragNode = this.getFragNodeRef(frag);
            if (fragNode.tagName.toLowerCase() == "input") {
                this.input.id = fragNode.id;
                this.input.name = fragNode.name;
                this.input.value = fragNode.value;
                this.readonlyMode = fragNode.disabled;
            } else {
                this.readonlyMode = true;
                this.input.value = dojo.dom.textContent(fragNode);
            }

            this._setDocumentNameMessage("Initialising");
            
            // init the popup
            this.domNode.appendChild(this.docPopup.domNode);
            this.domNode.removeChild(this.docPopupContent);
            this.docPopupContainerNode.appendChild(this.docPopupContent);
            dojo.html.setDisplay(this.docPopupContent, "");

            // init the versions popup
            if (!this.readonlyMode) {
                this.domNode.appendChild(this.versionsPopup.domNode);
                this.domNode.removeChild(this.versionsPopupContent);
                this.versionsPopupContainerNode.appendChild(this.versionsPopupContent);
                dojo.html.setDisplay(this.versionsPopupContent, "");
            }
            
            // init the datetimepicker
            if (!this.readonlyMode) {
                this.dp = $(this.datePickerContent);
                this.dp.css('font-size', 'smaller');
                this.dp.datetimepicker({
                  showTime: 'true'
                });
                var self = this;
                this.dp.bind('dpselect', function(event, data) {
                  self._setVersion(self.dp.datetimepicker('dateTimeIsoFormat'), event);
                });
                // FIXME: show, depending on the initial value
                this.dp.hide();
                //dojo.html.hide(this.versionsList);
                
            }

            // init the fragmentId popup
            if (!this.readonlyMode && this.enableFragmentId == "true") {
                this.domNode.appendChild(this.fragmentIdPopup.domNode);
                this.domNode.removeChild(this.fragmentIdPopupContent);
                this.fragmentIdPopupContainerNode.appendChild(this.fragmentIdPopupContent);
                dojo.html.setDisplay(this.fragmentIdPopupContent, "");
            } else if (this.enableFragmentId != "true") {
                this.fragmentIdButton.style.display = "none";
                this.fragmentIdLabel.style.display = "none";
            }

            // connect events -- document dropdown
            dojo.event.connect(this.dropdownButton, "onclick", this, "_onDropdownButtonClick");
            dojo.event.connect(this.documentLabel, "onclick", this, "_onLabelClick");
            dojo.event.connect(this.viewButton, "onclick", this, "_onViewClick");
            dojo.event.connect(this.editButton, "onclick", this, "_onEditClick");
            if (!this.readonlyMode) {
                dojo.event.connect(this.inputOkButton, "onclick", this, "_onInputOkClick");
                dojo.event.connect(this.linkInput, "onkeypress", this, "_processInputKey");
                dojo.event.connect(this.browseButton, "onclick", this, "_onBrowseClick");
            }

            // connect events -- versions dropdown
            if (!this.readonlyMode) {
                dojo.event.connect(this.versionButton, "onclick", this, "_onVersionButtonClick");
                dojo.event.connect(this.versionLabel, "onclick", this, "_onVersionLabelClick");
                dojo.event.connect(this.noVersionButton, "onclick", this, "_onNoVersionClick");
                dojo.event.connect(this.liveVersionButton, "onclick", this, "_onLiveVersionClick");
                dojo.event.connect(this.dateVersionButton, "onclick", this, "_onDateVersionClick");
                dojo.event.connect(this.lastVersionButton, "onclick", this, "_onLastVersionClick");
            }

            // connect events -- versions dropdown
            if (!this.readonlyMode && this.enableFragmentId == "true") {
                dojo.event.connect(this.fragmentIdButton, "onclick", this, "_onFragmentIdButtonClick");
                dojo.event.connect(this.fragmentIdLabel, "onclick", this, "_onFragmentIdLabelClick");
                dojo.event.connect(this.fragmentIdOkButton, "onclick", this, "_onFragmentIdOkClick");
                dojo.event.connect(this.fragmentIdInput, "onkeypress", this, "_processFragmentIdInputKey");
            }

            // Disable things for readonly mode
            if (this.readonlyMode) {
                this.linkInput.disabled = true;
                this.inputOkButton.style.display = "none";
                this.browseButtonSpan.style.display = "none";
                this.versionButton.style.display = "none";
                this.versionLabel.style.cursor = "";
                this.fragmentIdButton.style.display = "none";
                this.fragmentIdLabel.style.cursor = "";
            }

            if (this.enableBrowseDocuments != "true") {
                this.browseButtonSpan.style.display = "none";
            }

            // i18n of things inside the template
            this.browseButton.innerHTML = this.res["linkeditor.browse-for-document"];
            this.viewButton.innerHTML = this.res["linkeditor.view"];
            this.editButton.innerHTML = this.res["linkeditor.edit"];
            this.noVersionButton.innerHTML = this.res["linkeditor.no-version"];
            this.liveVersionButton.innerHTML = this.res["linkeditor.live-version"];
            this.dateVersionButton.inerHTML = this.res["linkeditor.date-version"];
            this.lastVersionButton.innerHTML = this.res["linkeditor.last-version"];

            // Init _windowFeatures
            if (this.openInNewWindows == "true") {
                // forcing the opening in a new window on firefox (rather than in a tab) can be forced
                // by setting width and height window features.
                this._windowFeatures = "left=0,top=0,width=1020,height=700,menubar=yes,resizable=yes,scrollbars=yes,status=yes,toolbar=yes";
            } else {
                this._windowFeatures = "";
            }
        },

        postCreate: function(/*Object*/args, /*Object*/frag, /*Widget*/parent) {
            daisy.widget.LinkEditor.superclass.postCreate(this, args, frag, parent);
            this._updateVersion();
            this._updateFragmentId();
            this._loadDocumentName();
        },

        /**
         * Loads the document name for the current link value using Ajax.
         */
        _loadDocumentName: function() {
            var link = this.input.value;
            if (link == null || link == "") {
                this._setDocumentNameMessage(this.res["linkeditor.no-document-selected"]);
                return;
            }

            var parsedLink = daisy.util.parseDaisyLink(link);
            if (parsedLink == null) {
                this._setDocumentNameMessage("Invalid link: " + link);
                return;
            }

            this._setDocumentNameMessage(this.res["linkeditor.loading-document-name"]);
            var counter = ++this.nameLoadCounter;
            var self = this;

            var url = this._buildInfoUrl(parsedLink);
            dojo.io.bind({
                    url: url,
                    load: function(type, data, evt) {
                        if (counter != self.nameLoadCounter) // ignore, there's a newer request already
                            return;
                        var result = data.documentElement.getAttribute("result");
                        if (result == "ok") {
                            self._setDocumentName(data.documentElement);
                        } else {
                            var resultLabel = dojo.dom.textContent(dojo.dom.firstElement(data.documentElement, "resultLabel"));
                            self._setDocumentNameMessage(self.res["linkeditor.error-loading-document-name"] + resultLabel);
                        }
                    },
                    error: function(type, error) {
                        if (counter != self.nameLoadCounter) // ignore, there's a newer request already
                            return;
                        self._setDocumentNameMessage(self.res["linkeditor.error-loading-document-name"] + type);
                    },
                    mimetype: "text/xml"
            });
        },

        _setDocumentName: function(documentEl) {
            var name = dojo.dom.textContent(dojo.dom.firstElement(documentEl, "name"));
            this.documentLabel.innerHTML = name;
            dojo.html.setClass(this.documentLabel, "");

            try {
                var documentType = dojo.dom.textContent(dojo.dom.firstElement(documentEl, "documentType"));
                var info = "<b>" + this.res["linkeditor.document-type"] + ":</b> " + documentType;

                var summary = dojo.dom.textContent(dojo.dom.firstElement(documentEl, "summary"));
                if (summary != "") {
                    info += "<br/><b>" + this.res["linkeditor.summary"] + ":</b> " + summary;
                }

                this.documentInfo.innerHTML = info;
            } catch (e) {
                dojo.debug("LinkEditor: error building/setting document summary info", e);
                this.documentInfo.innerHTML = "";
            }
        },

        _setDocumentNameMessage: function(text) {
            this.documentLabel.innerHTML = text;
            this.documentInfo.innerHTML = this.res["linkeditor.link-help"];
            dojo.html.setClass(this.documentLabel, "LinkEditorLabelInactive");
        },

        _onLabelClick: function() {
            this._showDropDown(this.documentLabel);
        },

        _onDropdownButtonClick: function() {
            this._showDropDown(this.dropdownButton);
        },

        _showDropDown: function(sourceNode) {
            if (!this.docPopup.isShowingNow) {
                this.linkInput.value = this.input.value;
                this.docPopup.open(this.documentLabel, this, sourceNode);
                this.linkInput.focus();
            } else {
                this.docPopup.close();
            }
        },

        _onInputOkClick: function() {
            this._acceptNewInput();
        },

        _processInputKey: function(event) {
            if (event.keyCode == 13 || event.keyCode == 10) {
                dojo.event.browser.stopEvent(event);
                this._acceptNewInput();
            }
        },

        _acceptNewInput: function() {
            if (this.readonlyMode) {
                alert("Unexpected situation: _acceptNewInput called in readonly mode");
                return;
            }

            this._setValueInternal(this.linkInput.value, false);

            // close editor in case of valid input
            var parsedLink = daisy.util.parseDaisyLink(this.input.value);
            if (parsedLink != null) {
                this.docPopup.close();
            }

            this._updateVersion();
            this._updateFragmentId();
            this._loadDocumentName();
        },

        /**
         * Updates the version label to match the current input.value state.
         */
        _updateVersion: function() {
            var parsedLink = daisy.util.parseDaisyLink(this.input.value);
            if (parsedLink != null && parsedLink.version != null && parsedLink.version != "") {
                this.versionLabel.innerHTML = this.res["linkeditor.version-label-prefix"] + parsedLink.version;
                dojo.html.setClass(this.versionLabel, "LinkEditorActive");
            } else {
                this.versionLabel.innerHTML = this.res["linkeditor.no-specific-version"];
                dojo.html.setClass(this.versionLabel, "LinkEditorLabelInactive");
            }
        },

        /**
         * Updates the fragment id label to match the current input.value state.
         */
        _updateFragmentId: function() {
            if (this.enableFragmentId != "true")
                return;
            var parsedLink = daisy.util.parseDaisyLink(this.input.value);
            if (parsedLink != null && parsedLink.fragmentId != null) {
                this.fragmentIdLabel.innerHTML = "#" + parsedLink.fragmentId;
                dojo.html.setClass(this.fragmentIdLabel, "LinkEditorActive");
            } else {
                this.fragmentIdLabel.innerHTML = this.res["linkeditor.no-fragment-id"];
                dojo.html.setClass(this.fragmentIdLabel, "LinkEditorLabelInactive");
            }
        },

        hidePopup: function() {
            this.docPopup.close();
        },

        _onViewClick: function(event) {
            dojo.event.browser.stopEvent(event);
            var link = this.input.value;

            if (link == "") {
                alert(this.res["linkeditor.no-document-selected"]);
                return;
            }

            var parsedLink = daisy.util.parseDaisyLink(link);
            if (parsedLink == null) {
                alert(this.res["linkeditor.invalid-document-link"] + link);
                return;
            }

            var url = daisy.mountPoint + "/" + daisy.site.name + "/" + parsedLink.documentId;
            if (parsedLink.version != null) {
                url += "/version/" + parsedLink.version;
            }
            url += "?" + this._getBranchAndLangParams(parsedLink);

            if (parsedLink.fragmentId != null) {
                url += "#" + parsedLink.fragmentId;
            }

            window.open(url, null, this._windowFeatures);
        },

        _onEditClick: function(event) {
            dojo.event.browser.stopEvent(event);
            var link = this.input.value;

            if (link == "") {
                alert(this.res["linkeditor.no-document-selected"]);
                return;
            }

            var parsedLink = daisy.util.parseDaisyLink(link);
            if (parsedLink == null) {
                alert(this.res["linkeditor.invalid-document-link"] + link);
                return;
            }

            var url = daisy.mountPoint + "/" + daisy.site.name + "/" + parsedLink.documentId + "/edit?startWithGet=true&" + this._getBranchAndLangParams(parsedLink);
            window.open(url, null, this._windowFeatures);
        },

        _getBranchAndLangParams: function(parsedLink) {
            var result = "branch=" + encodeURIComponent(parsedLink.branch != null ? parsedLink.branch : this._getDefaultBranch())
                    + "&language=" + encodeURIComponent(parsedLink.language != null ? parsedLink.language : this._getDefaultLanguage());
            return result;
        },

        _onBrowseClick: function(event) {
            dojo.event.browser.stopEvent(event);
            var currentWindow = window;
            var self = this;
            daisy.dialog.popupDialog(daisy.mountPoint + "/" + daisy.site.name + "/editing/documentBrowser?branch=" + this._getDefaultBranch() + "&language=" + this._getDefaultLanguage() + "&enableFragmentId=" + this.enableFragmentId,
              function(params) {
                  self.linkInput.value = params.url;
                  // using setTimeout so that the javascript is executed in the context of the original window, not the dialog
                  currentWindow.setTimeout(function(){self._acceptNewInput();}, 0);
              }, {});
        },

        _onVersionButtonClick: function(event) {
            dojo.event.browser.stopEvent(event);
            this._toggleVersionsPopup(this.versionButton);
        },

        _onVersionLabelClick: function(event) {
            dojo.event.browser.stopEvent(event);
            this._toggleVersionsPopup(this.versionLabel);
        },

        _toggleVersionsPopup: function(sourceNode) {
            if (!this.versionsPopup.isShowingNow) {

                var link = this.input.value;
                if (link == null || link == "") {
                    alert(this.res["linkeditor.no-document-selected"]);
                    return;
                }

                var parsedLink = daisy.util.parseDaisyLink(link);
                if (parsedLink == null) {
                    alert(this.res["linkeditor.invalid-document-link"] + link);
                    return;
                }

                this._loadVersionsList(parsedLink);

                this.versionsPopup.open(this.versionLabel, this, sourceNode);
            } else {
                this.versionsPopup.close();
            }
        },

        _loadVersionsList: function(parsedLink) {
            this.versionsList.innerHTML = "<img src='" + this.progressIcon + "'/>";

            var counter = ++this.versionsLoadCounter;

            var self = this;
            var url = this._buildInfoUrl(parsedLink, true);
            dojo.io.bind({
                    url: url,
                    load: function(type, data, evt) {
                        if (counter != self.versionsLoadCounter) // ignore, there's a newer request already
                            return;
                        var result = data.documentElement.getAttribute("result");
                        if (result == "ok") {
                            self._buildVersionsList(data.documentElement);
                        } else {
                            var resultLabel = dojo.dom.textContent(dojo.dom.firstElement(data.documentElement, "resultLabel"));
                            self.versionsList.innerHTML = self.res["linkeditor.error-loading-versions"] + resultLabel;
                        }
                    },
                    error: function(type, error) {
                        if (counter != self.versionsLoadCounter) // ignore, there's a newer request already
                            return;
                        self.versionsList.innerHTML = self.res["linkeditor.error-loading-versions"] + type;
                    },
                    mimetype: "text/xml"
            });
        },

        _buildInfoUrl: function(parsedLink, includeVersions, includeIds) {
            var branch = parsedLink.branch != null ? parsedLink.branch : this._getDefaultBranch();
            var language = parsedLink.language != null ? parsedLink.language : this._getDefaultLanguage();

            var url = daisy.mountPoint + "/" + daisy.site.name + "/editing/documentInfo.xml?documentId="
                    + encodeURIComponent(parsedLink.documentId)
                    + "&branch=" + encodeURIComponent(branch)
                    + "&language=" + encodeURIComponent(language)

            if (parsedLink.version != null)
                url += "&version=" + encodeURIComponent(parsedLink.version);

            if (includeVersions)
                url += "&includeVersionList=true";

            if (includeIds)
                url += "&includeIdList=true";

            return url;
        },

        /**
         * Builds the versions table from the received versions data.
         * This could use some client-side templating...
         */
        _buildVersionsList: function(documentEl) {
            try {
                var doc = this.versionsList.ownerDocument;
                var tableEl = doc.createElement("table");
                var tableBody = doc.createElement("tbody");
                tableEl.appendChild(tableBody);
                var self = this;

                // Define some utility functions
                var addCell = function(type, content, row) {
                    var cell = doc.createElement(type);
                    if (dojo.lang.isString(content)) {
                        cell.appendChild(doc.createTextNode(content));
                    } else {
                        cell.appendChild(content);
                    }
                    row.appendChild(cell);
                }

                var addVersionRow = function(col1, col2, col3) {
                    var row = doc.createElement("tr");

                    var a = doc.createElement("a");
                    a.href = "#";
                    a.appendChild(doc.createTextNode(col1));
                    dojo.event.connect(a, "onclick", function(event) {self._setVersion(col1, event); });
                    addCell("td", a, row);

                    addCell("td", col2, row);
                    addCell("td", col3, row);

                    tableBody.appendChild(row);
                }

                // Add header row
                var header = doc.createElement("tr");
                addCell("th", this.res["linkeditor.versionstable.id"], header);
                addCell("th", this.res["linkeditor.versionstable.created"], header);
                addCell("th", this.res["linkeditor.versionstable.state"], header);
                tableBody.appendChild(header);

                // Add the versions
                var versionsEl = dojo.dom.firstElement(documentEl, "versions");
                var versionEl = dojo.dom.firstElement(versionsEl, "version");
                while (versionEl != null) {
                    var id = versionEl.getAttribute("id");
                    var created = versionEl.getAttribute("created");
                    var state = versionEl.getAttribute("state");
                    addVersionRow(id, created, state);

                    versionEl = dojo.dom.nextElement(versionEl, "version");
                }

                // Put the table in place
                dojo.dom.removeChildren(this.versionsList); // removes progress indicator
                this.versionsList.appendChild(tableEl);
            } catch (error) {
                dojo.debug("LinkEditor: error building versions list rendering.", error);
                this.versionsList.innerHTML = "(error rendering versions list)"; // should not occur, no i18n needed
            }
        },

        /**
         * Update the link with a new version, triggers update of the document name
         * and versions labels.
         */
        _setVersion: function(version, event) {
            if (this.readonlyMode) {
                alert("Unexpected situation: _setVersion called in readonly mode");
                return;
            }

            if (event != null) {
                dojo.event.browser.stopEvent(event);
                this._toggleVersionsPopup();
            }

            var link = this.input.value;
            if (link == null || link == "") {
                // fail silently
                return;
            }

            var parsedLink = daisy.util.parseDaisyLink(link);
            if (parsedLink == null) {
                // fail silently
                return;
            }

            var branch = parsedLink.branch != null ? parsedLink.branch : "";
            var language = parsedLink.language != null ? parsedLink.language : "";

            var newLink = this._buildLink(parsedLink.documentId, parsedLink.branch, parsedLink.language, version, parsedLink.fragmentId);

            this._setValueInternal(newLink);
            this._updateVersion();
            this._loadDocumentName(); // name might be different in the new version
        },

        _onNoVersionClick: function(event) {
            if (this.dp.is(':visible'))
              this._toggleDatePicker(event); 
            
            this._setVersion(null, event);
        },

        _onLiveVersionClick: function(event) {
            if (this.dp.is(':visible'))
              this._toggleDatePicker(event); 
            
            this._setVersion("live", event);
            
        },
        
        _onDateVersionClick: function(event) {
            this._toggleDatePicker(event); 
        },

        _onLastVersionClick: function(event) {
            if (this.dp.is(':visible'))
              this._toggleDatePicker(event); 
            
            this._setVersion("last", event);
        },
        
        _toggleDatePicker: function(event) {
            if (this.dp.is(':visible')) {
              this.dp.hide();
              dojo.html.show(this.versionsList);
              this.versionsPopup.close();
              this.versionsPopup.open(this.versionLabel, this, this.versionButton);
            } else {
              this.dp.show();
              dojo.html.hide(this.versionsList);
              this.versionsPopup.close();
              this.versionsPopup.open(this.versionLabel, this, this.versionButton);
            }
        },

        _onFragmentIdButtonClick: function(event) {
            dojo.event.browser.stopEvent(event);
            this._toggleFragmentIdPopup(this.fragmentIdButton);
        },

        _onFragmentIdLabelClick: function(event) {
            dojo.event.browser.stopEvent(event);
            this._toggleFragmentIdPopup(this.fragmentIdLabel);
        },

        _toggleFragmentIdPopup: function(sourceNode) {
            if (!this.fragmentIdPopup.isShowingNow) {

                var link = this.input.value;
                if (link == null || link == "") {
                    alert(this.res["linkeditor.no-document-selected"]);
                    return;
                }

                var parsedLink = daisy.util.parseDaisyLink(link);
                if (parsedLink == null) {
                    alert(this.res["linkeditor.invalid-document-link"] + link);
                    return;
                }

                this.fragmentIdInput.value = parsedLink.fragmentId != null ? parsedLink.fragmentId : "";

                this._loadFragmentIdList(parsedLink);

                this.fragmentIdPopup.open(this.fragmentIdLabel, this, sourceNode);

                this.fragmentIdInput.focus();
            } else {
                this.fragmentIdPopup.close();
            }
        },

        _loadFragmentIdList: function(parsedLink) {
            this.fragmentIdList.innerHTML = "<img src='" + this.progressIcon + "'/>";

            var counter = ++this.fragmentIdsLoadCounter;

            var self = this;
            var url = this._buildInfoUrl(parsedLink, false, true);
            dojo.io.bind({
                    url: url,
                    load: function(type, data, evt) {
                        if (counter != self.fragmentIdsLoadCounter) // ignore, there's a newer request already
                            return;
                        var result = data.documentElement.getAttribute("result");
                        if (result == "ok") {
                            self._buildFragmentIdList(data.documentElement);
                        } else {
                            var resultLabel = dojo.dom.textContent(dojo.dom.firstElement(data.documentElement, "resultLabel"));
                            self.fragmentIdList.innerHTML = self.res["linkeditor.error-loading-ids"] + resultLabel;
                        }
                    },
                    error: function(type, error) {
                        if (counter != self.fragmentIdsLoadCounter) // ignore, there's a newer request already
                            return;
                        self.fragmentIdList.innerHTML = self.res["linkeditor.error-loading-ids"] + type;
                    },
                    mimetype: "text/xml"
            });
        },

        /**
         * Builds the fragment id list from the received data.
         * This could use some client-side templating...
         */
        _buildFragmentIdList: function(documentEl) {
            try {
                var doc = this.fragmentIdList.ownerDocument;
                var div = doc.createElement("div");
                var self = this;

                function createLink(id) {
                    var a = doc.createElement("a");
                    a.href = "#";
                    a.appendChild(doc.createTextNode(id));
                    dojo.event.connect(a, "onclick", function(event) {self._setFragmentId(id, event); });
                    return a;
                }

                var idsEl = dojo.dom.firstElement(documentEl, "ids");
                var idEl = dojo.dom.firstElement(idsEl, "id");

                if (idEl == null)
                    div.appendChild(doc.createTextNode(this.res["linkeditor.no-ids"]));

                while (idEl != null) {
                    var id = dojo.dom.textContent(idEl);

                    div.appendChild(createLink(id));
                    div.appendChild(doc.createElement("br"));

                    idEl = dojo.dom.nextElement(idEl, "id");
                }

                // Put the table in place
                dojo.dom.removeChildren(this.fragmentIdList); // removes progress indicator
                this.fragmentIdList.appendChild(div);
            } catch (error) {
                dojo.debug("LinkEditor: error building id list rendering.", error);
                this.fragmentIdList.innerHTML = "(error rendering id list)"; // should not occur, no i18n needed
            }
        },

        /**
         * Update the link with a new fragment id, triggers update of the fragment id display.
         */
        _setFragmentId: function(id, event) {
            if (this.readonlyMode) {
                alert("Unexpected situation: _setFragmentId called in readonly mode");
                return;
            }
            if (this.enableFragmentId != "true") {
                alert("Unexpected situation: _setFragmentId called but fragment id is not enabled");
                return;
            }

            if (event != null) {
                dojo.event.browser.stopEvent(event);
                this._toggleFragmentIdPopup();
            }

            var link = this.input.value;
            if (link == null || link == "") {
                // fail silently
                return;
            }

            var parsedLink = daisy.util.parseDaisyLink(link);
            if (parsedLink == null) {
                // fail silently
                return;
            }

            var branch = parsedLink.branch != null ? parsedLink.branch : "";
            var language = parsedLink.language != null ? parsedLink.language : "";

            var newLink = this._buildLink(parsedLink.documentId, parsedLink.branch, parsedLink.language, parsedLink.version, id);

            this._setValueInternal(newLink);
            this._updateFragmentId();
        },

        _buildLink: function (documentId, branch, language, version, fragmentId) {
            var newLink = "daisy:" + documentId;

            if (branch != null || language != null || version != null) {
                newLink += "@";
                if (branch != null)
                    newLink += branch;
                if (language != null || version != null) {
                    newLink += ":";
                    if (language != null)
                        newLink += language;
                    if (version != null)
                        newLink += ":" + version;
                }
            }

            if (fragmentId != null && this.enableFragmentId == "true")
                newLink += "#" + fragmentId;

            return newLink;
        },

        _onFragmentIdOkClick: function(event) {
            dojo.event.browser.stopEvent(event);
            this._acceptNewFragmentIdInput();
        },

        _processFragmentIdInputKey: function(event) {
            if (event.keyCode == 13 || event.keyCode == 10) {
                dojo.event.browser.stopEvent(event);
                this._acceptNewFragmentIdInput();
            }
        },

        _acceptNewFragmentIdInput: function() {
            var fragId = this.fragmentIdInput.value;
            fragId = dojo.string.trim(fragId);
            if (fragId == "")
                fragId = null;
            this._setFragmentId(fragId);
            this.fragmentIdPopup.close();
        },

        /*
         * Sets the link value without triggering display updates.
         */
        _setValueInternal: function(value) {
            this.input.value = value;
            this.onchange();
        },

        setValue: function(value) {
            this._setValueInternal(value);
            this._updateVersion();
            this._updateFragmentId();
            this._loadDocumentName();

            this.fragmentIdPopup.close();
            this.docPopup.close();
            this.versionsPopup.close();
        },

        getValue: function() {
            return this.input.value;
        },

        getDocumentName: function() {
            return dojo.dom.textContent(this.documentLabel);
        },

        onchange: function() {
            // does nothing, simply serves to connect events to
        },

        setCustomContext: function(branch, language) {
            this._customBranch = branch;
            this._customLanguage = language;
        },

        _getDefaultBranch: function() {
            if (this.contextMode == "default") {
                return "main";
            } else if (this.contextMode == "custom") {
                return this._customBranch;
            } else {
                return daisy.site.branch;
            }
        },

        _getDefaultLanguage: function() {
            if (this.contextMode == "default") {
                return "default";
            } else if (this.contextMode == "custom") {
                return this._customLanguage;
            } else {
                return daisy.site.language;
            }
        },

        destroy: function(/*Boolean*/finalize) {
            if (this.docPopup != null)
                this.docPopup.destroy(finalize);

            if (!this.outptuMode && this.versionsPopup != null)
                this.versionsPopup.destory(finalize);

            if (!this.outptuMode && this.fragmentIdPopup != null)
                this.fragmentIdPopup.destory(finalize);

            daisy.widget.LinkEditor.superclass.destroy.apply(this, arguments);
        }
    }
);
