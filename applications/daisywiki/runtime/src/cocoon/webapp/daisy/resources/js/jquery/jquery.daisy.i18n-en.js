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
    $.i18n("en.versionMode", {
      "popup.live": "LIVE",
      "popup.last": "STAGING",
      "popup.date": "@ DATE",
      "inline.live": "live",
      "inline.last": "last",
      "inline.date": "@ date" 
    });
    $.i18n("en.liveHistory", {
      "live": "live",
      "split": "Split",
      "remove": "Remove",
      "show": "Show",
      "new.row": "New row"
    });
    $.i18n("en.dateTimePicker", {
      "time.abbrev.hour": "H",
      "time.abbrev.minute": "M",
      "time.abbrev.second": "S"
    });
  });

})(jQuery);