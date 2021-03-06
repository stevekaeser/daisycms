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

(function($) {

  $(function() {
    $.i18n("de.versionMode", {
      "popup.live": "LIVE",
      "popup.last": "VORANSICHT",
      "popup.date": "@ DATUM",
      "inline.live": "Live-Version",
      "inline.last": "Letzte Version",
      "inline.date": "@ Datum" 
    });
    $.i18n("de.liveHistory", {
      "live": "Live-Version",
      "split": "Auftrennen",
      "remove": "Entfernen",
      "show": "Anzeigen",
      "new.row": "Neue Zeile"

    });
    $.i18n("de.dateTimePicker", {
      "time.abbrev.hour": "S",
      "time.abbrev.minute": "M",
      "time.abbrev.second": "S"
    });
  });

})(jQuery);
