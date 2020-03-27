dojo.require("dojo.event.*");
dojo.require("dojo.lang.*");

dojo.provide("daisy.workflow");

/**
 * Helper class for managing the display of various workflow search results
 * (tasks, processes, timers). Basically it refreshes a ContentPane widget
 * when the users pages through the results or changes the sort order.
 */
dojo.declare("daisy.workflow.SearchResultController", null,
    function(id, url, actionReturnTo, actionReturnToLabel) {
        this.id = id;
        this.url = url;
        this.actionReturnTo = actionReturnTo;
        this.actionReturnToLabel = actionReturnToLabel;
        this.contentPane = dojo.widget.byId(id);

        this.contentPane.cacheContent = false;
        this.contentPane.loadingMessage = this.loadingMessage;
        this.contentPane.adjustPaths = false;
        this._load(this._getDefaultParams());
    },

    {
        loadingMessage: "<div style='height: 5em;'><img src='" + daisy.mountPoint + "/resources/skins/" + daisy.skin + "/images/progress_indicator_flat.gif'/></div>",

        changeOrderBy: function(column, source, sortOrder) {
            var params = this._getDefaultParams();
            params.orderBy = column;
            params.orderBySource = source;
            params.orderByDirection = sortOrder;
            this._load(params);
        },

        _load : function(params) {
            var hasQueryString = this.url.indexOf("?") >= 0;
            var url = this.url + (hasQueryString ? "&" : "?");
            for (var param in params) {
                url += param + "=" + encodeURIComponent(params[param]) + "&";
            }
            this._addOnLoad();
            this.contentPane.setUrl(url);
        },

        _afterLoad : function() {
            // Add event listeners on the sort buttons
            for (var i = 0; true; i++) {
                var sortButton = dojo.byId(this.id + "." + i + ".sort");
                if (sortButton == null)
                    break;
                dojo.event.connect(sortButton, "onclick", this, "_sortButtonClicked");
            }

            // read chunk info and order-by settings
            var root = dojo.byId(this.id + ".root");
            this.chunkOffset = parseInt(root.getAttribute("chunkOffset"));
            this.resultSize = parseInt(root.getAttribute("resultSize"));
            this.orderBy = root.getAttribute("orderBy");
            this.orderBySource = root.getAttribute("orderBySource");
            this.orderByDirection = root.getAttribute("orderByDirection");

            // add event listeners to first / prev / next / last navigation (if present)
            var toFirst = dojo.byId(this.id + ".toFirst");
            if (toFirst != null) {
                var toPrev = dojo.byId(this.id + ".toPrev");
                var toNext = dojo.byId(this.id + ".toNext");
                var toLast = dojo.byId(this.id + ".toLast");

                if (this.chunkOffset > 1) {
                    toFirst.href = "#";
                    dojo.event.connect(toFirst, "onclick", this, "_toFirst");

                    toPrev.href = "#";
                    dojo.event.connect(toPrev, "onclick", this, "_toPrev");
                }

                if (this.resultSize > this.chunkOffset + 10) {
                    toNext.href="#";
                    dojo.event.connect(toNext, "onclick", this, "_toNext");

                    toLast.href="#";
                    dojo.event.connect(toLast, "onclick", this, "_toLast");
                }
            }
        },

        _addOnLoad : function() {
            this.contentPane.addOnLoad(dojo.lang.hitch(this, "_afterLoad"));
        },

        _sortButtonClicked : function(evt) {
            var sortButton = evt.target;
            // these attributes should have been made available
            var column = sortButton.getAttribute("sortColumn");
            var source = sortButton.getAttribute("sortSource");
            var order = sortButton.getAttribute("sortOrder");
            this.changeOrderBy(column, source, order);
            return false;
        },

        _getDefaultParams : function() {
            var params = new Object();

            if (this.offset != null) {
                params.offset = this.chunkOffset;
            }

            if (this.orderBy != null) {
                params.orderBy = this.orderBy;
                params.orderBySource = this.orderBySource;
                params.orderByDirection = this.orderByDirection;
            }

            params.actionReturnTo = this.actionReturnTo;
            params.actionReturnToLabel = this.actionReturnToLabel;

            return params;
        },

        _changeOffset : function(offset) {
            var params = this._getDefaultParams();
            params.offset = offset;
            this._load(params);
            return false;
        },

        _toFirst : function() {
            return this._changeOffset(1);
        },

        _toPrev : function() {
            var offset = this.chunkOffset - 10;
            if (offset < 1) offset = 1;
            return this._changeOffset(offset);
        },

        _toNext : function() {
            var offset = this.chunkOffset + 10;
            if (offset > this.resultSize) offset = this.resultSize;
            return this._changeOffset(offset);
        },

        _toLast : function() {
            var fromEnd = this.resultSize % 10;
            if (fromEnd == 0) fromEnd = 10;
            var offset = this.resultSize - fromEnd;
            if (offset < 1) offset = 1;
            return this._changeOffset(offset);
        }
    }
);
