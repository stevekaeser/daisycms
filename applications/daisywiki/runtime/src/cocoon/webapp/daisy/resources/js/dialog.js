// The code in here is based on HTMLArea's dialog code, but has been changed by
// the Daisy developers

// Original HTMLArea copyright note:
// htmlArea v3.0 - Copyright (c) 2003-2004 interactivetools.com, inc.
// This copyright notice MUST stay intact for use (see license.txt).
//
// Portions (c) dynarch.com, 2003-2004
//
// A free WYSIWYG editor replacement for <textarea> fields.
// For full source code and docs, visit http://www.interactivetools.com/
//
// Version 3.0 developed by Mihai Bazon.
//   http://dynarch.com/mishoo

dojo.provide("daisy.dialog");


daisy.dialog.init = function() {
    window.dialogArguments = opener.daisy.dialog._arguments;
    dojo.event.connect(document.body, "onkeypress", daisy.dialog, "_closeOnEsc");
}

daisy.dialog.sizeAndPosition = function(args) {
    var dialogcontent = dojo.byId("dialogcontent");
    if (dialogcontent == null)
        alert("dialogcontent div not found");
    dialogcontent.style.position = "absolute";

    // doing the sizing via a timeout is needed for IE6 for dialogs with dynamically
    // filled select elements, such as the go to dialog. Otherwise, IE6 doesn't size it correctly.
    // IE7 does it ok though, so when IE6 becomes irrelevant the timeout can be removed
    setTimeout( function() {
        // calculate height / width
        var contentbox = dojo.html.getContentBox(dialogcontent);
        var viewport = dojo.html.getViewport();
        var marginbox = dojo.html.getMarginBox(dialogcontent);
        var width = marginbox.width;
        if (args && args.minWidth && width < args.minWidth)
            width = args.minWidth;
        var height = marginbox.height;
        if (args && args.minHeight && height < args.minHeight)
            height = args.minHeight;

        var widthChange = width - viewport.width;
        var heightChange =  height - viewport.height;

        // center on screen
        var x = (screen.availWidth - width) / 2;
        var y = (screen.availHeight - height) / 2;
        window.moveTo(x, y);

        // resize after positioning windows, because it is not possible to make window larger then
        // available space towards bottom/right (at least in Windows/IE)
        window.resizeBy(widthChange, heightChange);

        // especially for IE6: make smaller in height again if possible
        viewport = dojo.html.getViewport();
        //height = dojo.html.getMarginBox(dialogcontent).height;
        if (height < viewport.height) {
            window.resizeBy(0, height - viewport.height);
        }

        dialogcontent.style.position = "";
    }, 2);
}

daisy.dialog._closeOnEsc = function(ev) {
    if (ev.keyCode == 27) {
        window.close();
        return false;
    }
    return true;
}

daisy.dialog._onClose = function() {
	opener.daisy.dialog._return(null);
};

daisy.dialog.translate = function(i18n) {
	var types = ["span", "option", "td", "button", "div"];
	for (var type in types) {
		var spans = document.getElementsByTagName(types[type]);
		for (var i = spans.length; --i >= 0;) {
			var span = spans[i];
			if (span.firstChild && span.firstChild.data) {
				var txt = i18n[span.firstChild.data];
				if (txt)
					span.firstChild.data = txt;
			}
		}
	}
	var txt = i18n[document.title];
	if (txt)
		document.title = txt;
};

// closes the dialog and passes the return info upper.
daisy.dialog.close = function(val) {
    if (opener.closed || this.getCurrentOpenerModalDialog() != window) {
        if (val != null)
            alert("Parent window has been closed, action will have no effect.")
    } else {
        opener.daisy.dialog._return(val);
    }
    window.close();
}

daisy.dialog.getCurrentOpenerModalDialog = function() {
    var daisy = opener.daisy;
    if (daisy == null)
        return null;
    var daisyDialog = daisy.dialog;
    if (daisyDialog == null)
        return null;
    return daisy.dialog._modal;
}


daisy.dialog._parentEvent = function(ev) {
    if (daisy.dialog._modal && !daisy.dialog._modal.closed) {
        dojo.event.browser.stopEvent(ev);
        daisy.dialog._modal.focus();
    }
};

// should be a function, the return handler of the currently opened dialog.
daisy.dialog._return = null;

// constant, the currently opened dialog
daisy.dialog._modal = null;

// the dialog will read it's args from this variable
daisy.dialog._arguments = null;

daisy.dialog.popupDialog = function(url, action, init) {
    if (daisy.dialog.uniqueDialogId == null) {
        daisy.dialog.uniqueDialogId = "dsydialog" + new Date().getTime();
    }

    // If there's an existing dialog, close it first
    if (daisy.dialog._modal != null && !daisy.dialog._modal.closed) {
        daisy.dialog._modal.close();
    }

    var initialWidth = 200;
    var initialHeight = 100;
    var initialLeft = (screen.availWidth - initialWidth) / 2;
    var initialTop = (screen.availHeight - initialHeight) / 2;
    var features = "width=" + initialWidth + ",height=" + initialHeight + ",left=" + initialLeft + ",top=" + initialTop
                + ",toolbar=no,menubar=no,personalbar=no,scrollbars=yes,resizable=yes";

    var dlg = window.open(url, daisy.dialog.uniqueDialogId, features);

    daisy.dialog._modal = dlg;
    daisy.dialog._arguments = init;

    // capture some window's events
    function capwin(w) {
        dojo.event.connect(w, "onclick", daisy.dialog, "_parentEvent");
        dojo.event.connect(w, "onmousedown", daisy.dialog, "_parentEvent");
        dojo.event.connect(w, "onfocus", daisy.dialog, "_parentEvent");
    };
    // release the captured events
    function relwin(w) {
        dojo.event.disconnect(w, "onclick", daisy.dialog, "_parentEvent");
        dojo.event.disconnect(w, "onmousedown", daisy.dialog, "_parentEvent");
        dojo.event.disconnect(w, "onfocus", daisy.dialog, "_parentEvent");
    };
    capwin(window);
    // capture other frames
    for (var i = 0; i < window.frames.length; capwin(window.frames[i++]));
    // make up a function to be called when the dialog ends.
    daisy.dialog._return = function (val) {
        if (val && action) {
            action(val);
        }
        relwin(window);
        // capture other frames
        for (var i = 0; i < window.frames.length; relwin(window.frames[i++]));
        daisy.dialog._modal = null;
    };

    dlg.focus();
}