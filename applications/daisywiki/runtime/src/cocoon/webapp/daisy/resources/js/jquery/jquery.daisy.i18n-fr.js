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
    $.i18n("fr.versionMode", {
      "popup.live": "EN LIGNE",
      "popup.last": "ÉBAUCHE",
      "popup.date": "@ DATE",
      "inline.live": "version en ligne",
      "inline.last": "dernière version",
      "inline.date": "@ Date" 
    });
    $.i18n("fr.liveHistory", {
      "live": "version en ligne"
    });
    $.i18n("fr.dateTimePicker", {
      "time.abbrev.hour": "H",
      "time.abbrev.minute": "M",
      "time.abbrev.second": "S"
    });
  });

})(jQuery);
