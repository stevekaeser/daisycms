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
/*jslint white: false */
DaisyMisc.I18N = {
  /* daisy-misc.js */
  "hint.default-alignment": "Horizontale Ausrichtung Standard",
  "hint.align-left": "Ausrichtung Links",
  "hint.align-center": "Ausrichtung Mitte",
  "hint.align-right": "Ausrichtung Rechts",
  "hint.default-vertical-alignment": "Vertikale Ausrichtung Standard",
  "hint.align-top": "Ausrichtung Oben",
  "hint.align-middle": "Ausrichtung Mitte",
  "hint.align-bottom": "Ausrichtung Unten",
  "hint.switch-normal-header-cell": "Zwischen normaler Zelle und Kopf-Zelle umschalten",
  "hint.insert-table": "Tabelle einfügen",
  "hint.delete-table": "Tabelle löschen",
  "hint.edit-table-class": "table class editieren",
  "hint.remove-formatting": "Formatierung entfernen oder aussetzen",
  "hint.tt": "Teletype",
  "hint.table-settings": "Tabelleneigenschaften",
  "hint.insert-div": "DIV-Element einfügen",
  "hint.delete-div": "DIV-Element löschen",
  "hint.div-settings": "DIV Eigenschaften",
  "hint.goto": "Gehe zu",
  "hint.cleanup": "HTML aufräumen",
  "hint.switch-to-source": "Zum HTML-Quellcodeeditor wechseln",
  "hint.quote": "Zitieren",
  "hint.unquote": "Zitat entfernen",
  "hint.variables": "Variable einfügen/entfernen",
  "hint.ol-settings": "Einstellungen für nummerierte Liste",
  "js.no-td-th-found": "Kein <td> oder <th> Element gefunden.",
  "js.remove-table-confirm": "Das Entfernen einer Tabelle kann nicht widerrufen werden. Trotzdem fortfahren?",
  "js.cursor-not-inside-table": "Der Cursor befindet sich nicht innerhalb einer Tabelle.",
  "js.remove-div-confirm": "Das Entfernen des DIV-Elements kann nicht rückgängig gemacht werden. Dennoch fortfahren?",
  "js.cursor-not-inside-div": "Der Cursor ist nicht innerhalb eines DIV-Elements platziert.",
  "js.teletype-impossible": "Kann gegenwärtige Auswahl nicht in Teletype umsetzen",
  "js.nothing-to-go-to": "Das Sprungziel (etwa eine Kopfzeile oder eine Element mit dieser ID) existiert nicht.",  

  /* insert_table.html*/
  "inserttable.must-enter-number-rows": "Bitte geben Sie eine gültige Anzahl an Zeilen an.",
  "inserttable.must-enter-number-columns": "Bitte geben Sie eine gültige Anzahl an Spalten an.",
  "inserttable.title": "Tabelle einfügen",
  "inserttable.rows": "Zeilen",
  "inserttable.cols": "Spalten",
  "inserttable.first-row-are-titles": "Erste Zeile enthält Zeilenüberschriften",
  "inserttable.first-column-are-titles": "Erste Spalte enthält Spaltenbeschriftung",
  "inserttable.cancel": "Abbrechen",

  /* goto.html */
  "goto.title": "Gehe zu",
  "goto.element": "Gehe zu Element mit dieser ID",
  "goto.goto": "Gehe zu",
  "goto.header": "Gehe zu dieser Kopfzeile",
  "goto.close": "Schließen",

  /* table_settings.html */
  "tablesettings.invalid-column-width": "Ungültiger Wert für die Spaltenbreite",
  "tablesettings.invalid-table-width": "Ungültiger Wert für die Tabellenbreite",
  "tablesettings.invalid-size-value": "Ungültiger Wert für diese Größe",
  "tablesettings.title": "Tabelleneigenschaften",
  "tablesettings.print-sizes": "Druckgrößen",
  "tablesettings.table-width": "Breite der Tabelle",
  "tablesettings.use-default-column-widths": "Voreingestellte Spaltenbreite benutzen",
  "tablesettings.column": "Spalte",
  "tablesettings.screen-sizes": "Bildschirmgrößen",
  "tablesettings.table-caption": "Überschrift der Tabelle",
  "tablesettings.table-type": "Tabellentyp",
  "tablesettings.ok": "OK",
  "tablesettings.cancel": "Abbrechen",
  "tablesettings.alignment-title": "Ausrichtung der Tabelle",
  "tablesettings.alignment": "Ausrichtung: ",
  "tablesettings.align-default": "Voreinstellung",
  "tablesettings.align-left": "Linksbündig",
  "tablesettings.align-center": "Zentriert",
  "tablesettings.align-right": "Rechtsbündig",
  "tablesettings.classes": "Klass(en):",
  "tablesettings.misc": "Verschiedenes",

  /* div_settings.html */
  "divsettings.title": "DIV Eigenschaften",
  "divsettings.ok": "OK",
  "divsettings.cancel": "Abbrechen",
  "divsettings.class": "Class:",
  "divsettings.misc": "Verschiedenes",

  "variables.title": "Variablen",
  "variables.not-in-variable": "Die Einfügemarke ist nicht innerhalb einer Variablen positioniert.",
  "variables.cannot-nest-variable": "Variablen können nicht verschachtelt werden.",
  "variables.no-variables": "Keine Variablen verfügbar.",
  "variables.remove": "Variable entfernen",

  "olsettings.title": "Startnummer der Liste",
  "olsettings.start-number": "Startnummer",
  "olsettings.style": "Nummerierungsstil",
  "olsettings.style.decimal": "Dezimal (1, 2, 3, ...)",
  "olsettings.style.lower-latin": "Lateinisch, Kleinschreibung (a, b, c, ...)",
  "olsettings.style.upper-latin": "Lateinisch, Großschreibung (A, B, C, ...)",
  "olsettings.style.lower-roman": "Römisch-Kleinschreibung (i, ii, iii, ...)",
  "olsettings.style.upper-roman": "Römisch-Großschreibung (I, II, III, ...)",
  "olsettings.no-ol-found": "Die Einfügemarke ist nicht innerhalb einer nummerierten Liste positioniert."

};
