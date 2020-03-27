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

(function($) {

  $(function() {
    $.i18n("nl.versionMode", {
      "popup.live": "LIVE",
      "popup.last": "STAGING",
      "popup.date": "@ DATUM",
      "inline.live": "actieve versie",
      "inline.last": "laatste",
      "inline.date": "@ datum" 
    });
    $.i18n("nl.liveHistory", {
      "live": "actieve versie",
      "split": "Splitsen",
      "remove": "Verwijderen",
      "show": "Tonen",
      "new.row": "Nieuwe rij"
    });
    $.i18n("nl.dateTimePicker", {
      "time.abbrev.hour": "U",
      "time.abbrev.minute": "M",
      "time.abbrev.second": "S"
    });
  });

})(jQuery);