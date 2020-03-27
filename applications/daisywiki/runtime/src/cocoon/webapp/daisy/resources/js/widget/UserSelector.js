dojo.provide("daisy.widget.UserSelector");
dojo.require("daisy.context");
dojo.require("dojo.html");
dojo.require("daisy.util");
dojo.require("dojo.i18n.common");


dojo.requireLocalization("daisy.widget", "messages", null, /* available languages, to avoid 404 requests */ "ROOT,nl,fr,de,es,ru");

/**
 * A widget for selecting a user login.
 */
dojo.widget.defineWidget(
    // widget name and class
    "daisy.widget.UserSelector",

    // superclass
    dojo.widget.HtmlWidget,

    function() {
    },

    // properties and methods
    {
        isContainer: false,

        templatePath: daisy.context.getResourceUri("widget/templates/UserSelector.html"),

        res: dojo.i18n.getLocalization("daisy.widget", "messages"),

        fillInTemplate: function(args, frag) {
            daisy.widget.UserSelector.superclass.fillInTemplate(this, args, frag);

            // copy id and name from original input
            var fragNode = this.getFragNodeRef(frag);
            this.input.id = fragNode.id;
            this.input.name = fragNode.name;
            this.input.value = fragNode.value;
            this.input.disabled = fragNode.disabled;

            // connect events
            dojo.event.connect(this.lookupUserButton, "onclick", this, "_onLookupUserClick");
            dojo.event.connect(this.meButton, "onclick", this, "_onMeClick");

            // i18n
            this.meButton.innerHTML = this.res["userselector.me"];
        },

        _onLookupUserClick: function(event) {
            dojo.event.browser.stopEvent(event);
            var popup = window.open(daisy.mountPoint + "/selectUser", "selectuser", "toolbar=no,menubar=no,personalbar=no,width=400,height=400,left=20,top=40,scrollbars=yes,resizable=yes");
            var self = this;
            popup.onUserSelected = function(userId, userLogin) {
                self.input.value = userLogin;
            }
        },

        _onMeClick: function(event) {
            dojo.event.browser.stopEvent(event);
            this.input.value = daisy.user.login;
        }
    }
);
