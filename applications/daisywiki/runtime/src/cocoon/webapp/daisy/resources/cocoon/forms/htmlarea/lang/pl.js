// placeholder, the polish translation of htmlarea seems to be broken
// this is a copy of the english one, with modified lang attribute.

// I18N constants

// LANG: "pl", ENCODING: UTF-8 | ISO-8859-1
// Author: Mihai Bazon, http://dynarch.com/mishoo

// FOR TRANSLATORS:
//
//   1. PLEASE PUT YOUR CONTACT INFO IN THE ABOVE LINE
//      (at least a valid email address)
//
//   2. PLEASE TRY TO USE UTF-8 FOR ENCODING;
//      (if this is not possible, please include a comment
//       that states what encoding is necessary.)

HTMLArea.I18N = {

	// the following should be the filename without .js extension
	// it will be used for automatically load plugin language.
	lang: "pl",

	tooltips: {
		bold:           "Wytłuszczony",
		italic:         "Pochylony",
		underline:      "Podkreślony",
		strikethrough:  "Przekreślony",
		subscript:      "Indeks dolny",
		superscript:    "Indeks górny",
		justifyleft:    "Wyrównaj do lewej",
		justifycenter:  "Do środka",
		justifyright:   "Wyrównaj do prawej",
		justifyfull:    "Do lewej i prawej",
		orderedlist:    "Lista numerowana",
		unorderedlist:  "Lista nienumerowana",
		outdent:        "Zmniejsz wcięcie",
		indent:         "Zwiększ wcięcie",
		forecolor:      "Kolor znków",
		hilitecolor:    "Kolor tła",
		horizontalrule: "Kreska pozioma",
		createlink:     "Wstaw odnośnik",
		insertimage:    "Wstaw/zmodyfikuj obrazek",
		inserttable:    "Wstaw tabelę",
		htmlmode:       "Zmień na źródło HTML",
		popupeditor:    "Powiększ Edytor",
		about:          "O Edytorze",
		showhelp:       "Pomoc",
		textindicator:  "Aktualny styl",
		undo:           "Cofnij ostatnią operację",
		redo:           "Powtórz ostatnią operację",
		cut:            "Wytnij zaznaczenie",
		copy:           "Skopiuj zaznaczenie",
		paste:          "Wklej ze schowka",
		lefttoright:    "Kierunek z lewej do prawej",
		righttoleft:    "Kierunek z prawej do lewej"
	},

	buttons: {
		"ok":           "OK",
		"cancel":       "Porzuć"
	},

	msg: {
		"Path":         "Ścieżka",
		"TEXT_MODE":    "Jesteś w trybie tekstowym.  Użyj przycisku [<>] aby przejść do trybu WYSIWYG.",

		"IE-sucks-full-screen" :
		// translate here
		"The full screen mode is known to cause problems with Internet Explorer, " +
		"due to browser bugs that we weren't able to workaround.  You might experience garbage " +
		"display, lack of editor functions and/or random browser crashes.  If your system is Windows 9x " +
		"it's very likely that you'll get a 'General Protection Fault' and need to reboot.\n\n" +
		"You have been warned.  Please press OK if you still want to try the full screen editor."
	},

	dialogs: {
		"Cancel"                                            : "Cancel",
		"Insert/Modify Link"                                : "Insert/Modify Link",
		"New window (_blank)"                               : "New window (_blank)",
		"None (use implicit)"                               : "None (use implicit)",
		"OK"                                                : "OK",
		"Other"                                             : "Other",
		"Same frame (_self)"                                : "Same frame (_self)",
		"Target:"                                           : "Target:",
		"Title (tooltip):"                                  : "Title (tooltip):",
		"Top frame (_top)"                                  : "Top frame (_top)",
		"URL:"                                              : "URL:",
		"You must enter the URL where this link points to"  : "You must enter the URL where this link points to"
	}
};
