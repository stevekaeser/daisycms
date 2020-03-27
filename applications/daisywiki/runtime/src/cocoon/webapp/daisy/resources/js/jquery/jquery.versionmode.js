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

  var VersionMode = {};

  // normally we only need one popup, but the dateTime popup breaks if it's removed from the dom tree after construction,
  // so we create one per instance and only show one at a time.
  //
  var _popups = {};
  
  var _instanceNr = 0;
  
  $.extend(VersionMode, {
    widgetEventPrefix: 'versionmode',
    options: {
      value: 'live',
      useButtonSet: true,
      hideLast: false
    },
    _create: function() {
      var self = this, o = this.options;
      self.busy = false;
      
      var type = self.type = $("<span/>");
      type.addClass("dsy-versionmode-type");
      
      self.instanceId = _instanceNr++;
      
      var popup = this.popup = _popups[self.istanceId] = $("<div style='display:none; position:absolute; z-index:100'/>");
      var popupId = this.popupId = "_vmpopup_" + self.instanceId;
      popup.attr('id', popupId);
      $(function() {$("body").append(popup)});
      
      var prefix = self.options.inline ? "inline.":"popup."
      self.typeLast = self.createTypePairUI(type, "last", $.i18n("versionMode",prefix + "last"), !this.options.hideLast);
      self.typeLive = self.createTypePairUI(type, "live", $.i18n("versionMode",prefix + "live"), true);
      self.typePIT = self.createTypePairUI(type, "pit", $.i18n("versionMode",prefix + "date"), true);

      var dateTime = this.dateTime = $("<div/>");
      if (!this.options.inline) {
        this.popup.append(type);
      }
      this.popup.append(dateTime);
      
      var dtpOptions = $.extend({}, this.options);

      var dateTimePicker = this.dateTimePicker = $("<div/>").datetimepicker(dtpOptions);
      dateTimePicker.bind("dpselect", function(evt, x) {
        self.updatePIT();
        if (!self.options.showTime) {
          self.hideDateTime();
        }
      });
      dateTime.append(dateTimePicker);

      if (o.inline) {  // for inline mode, this.element contains our widgets
        this.element.empty();
        this.element.append(self.type);
      } else {
        $(this.element).click(function(ev) {   // for popup mode, this.element is the trigger for the popup (and the popup is set to contain our widgets)
            if (self.popupShowing) {
              self.popup.hide();
              self.popupShowing = false;
            } else {
              var o = $(this).offset();
              self.popup.css({'top':o.top + $(this).outerHeight(), 'left':o.left});
              self.popup.show();
              self.popupShowing = true;
            }
         });
      };
      
      this.valueInput = $(o.valueInput || []);
      var initValue = this.valueInput.val();
      if (initValue == 'last') { // make sure the appropriate value is checked before converting into a buttonset.
        self.typeLast.radio.click();
      } else if (initValue == 'live') {
        self.typeLive.radio.click();
      } else {
        self.typePIT.radio.click();
      }
      if (this.options.useButtonSet) {
        type.buttonset();
      }
      
      self.typeLast.radio.click(function() {
        self.setPIT('last');
        self.element.trigger('select');
      });
      self.typeLive.radio.click(function() {
        self.setPIT('live');
        self.element.trigger('select');
      });
      self.typePIT.radio.click(function() {
        self.setPIT(self.dateTimePicker.datetimepicker('dateTimeIsoFormat'));
      });
      
      $(document).mousedown(function(ev) {
        self._checkExternalClick(ev);
      });
      self.setPIT(initValue);
      popup.hide();
      
      if (this.options.inline && initValue != 'last' && initValue != 'live') {
        if (this.options.useButtonSet) {
          this.typePIT.label.children('span').text(this.dateTimePicker.datetimepicker('dateTimeIsoFormat'));
        } else {
          this.typePIT.label.text(this.dateTimePicker.datetimepicker('dateTimeIsoFormat'));
        }
      }
    },
    // updates the hidden value based on the input widgets
    updatePIT: function() {
      var pit = this.dateTimePicker.datetimepicker('dateTimeIsoFormat');
      this.valueInput.val(pit);
      
      $("#versionModeDisplay").text(this.getUserFormat());
      if (this.options.inline) {
        if (this.options.useButtonSet) {
          this.typePIT.label.children('span').text(this.dateTimePicker.datetimepicker('dateTimeIsoFormat'));
        } else {
          this.typePIT.label.text(this.dateTimePicker.datetimepicker('dateTimeIsoFormat'));
        }
      }
      this.element.trigger('select');
    },
    // sets the specified pit and updates the input widgets to dispay selected pit
    setPIT: function(pit) {
        this.valueInput.val(pit);
        this.pitChanged(pit);
        if (pit != 'last' && pit != 'live') {
          this.dateTimePicker.datetimepicker('setValue', pit);
        }
        $("#versionModeDisplay").text(this.getUserFormat());
    },
    dateTimeChanged: function() {
        this.setPIT(this.dateTimePicker.datetimepicker('dateTimeIsoFormat'));
    },
    createTypePairUI: function(container, idSuffix, label, visible) {
      var radioName = "versionMode" + this.instanceId;
      var radioId = "versionMode_" + idSuffix + this.instanceId;
       
      var radio = $("<input type='radio' id='"+radioId+"' name='"+radioName+"'/>");
      var label = $("<label for='"+radioId+"'>"+ label + "</label>");
      if (visible) {
        container.append(radio);
        container.append(label);
      }
      
      return { radio: radio, label: label };
    },
    hideDateTime: function() {
      if (this.options.inline) {
        this.popup.hide();
        this.popupShowing = false;
      } else {
        this.dateTime.hide();
      }
    },
    showDateTime: function() {
      if (this.options.inline) {
        var base = this.typePIT.label;
        o = $(base).offset();
        this.popup.css({'top':o.top + $(base).outerHeight(), 'left':o.left + $(base).outerWidth() - this.popup.outerWidth()});
        this.popup.show();
        this.popupShowing = true;
      } else {
        this.dateTime.show();
      }
    },
    pitChanged: function(pit) {
        if (pit == 'last') {
          this.hideDateTime();
        } else if (pit == 'live') {
          this.hideDateTime();
        } else {
          this.showDateTime();
          this.busy = false;
        }
    },
    getUserFormat: function() {
      if (this.typeLast.radio.checked)
        return 'last';
      if (this.typeLive.radio.checked)
        return 'live';
      return this.dateTimePicker.datetimepicker('dateTimeIsoFormat');
    },
    _checkExternalClick: function(event) {
      var $target = $(event.target);

      // hide our popup if we are showing our popup and we detect a click that's not on the trigger and not on our popup.
      if ($target[0] != this.element[0] && // check for trigger element (not really needed in inline moded, but it doesn't hurt
              $target.parents('#'+this.popupId).length == 0 && this.popupShowing) { // check for popup
            this.popup.hide();
            this.popupShowing = false;
      }
      
      return false;
    }
  });
  
  if (!$.versionmode) {
    $.widget("ui.versionmode", VersionMode);
  }
  
})(jQuery);