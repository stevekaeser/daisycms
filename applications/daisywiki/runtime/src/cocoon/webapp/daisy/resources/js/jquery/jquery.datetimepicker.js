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

  var DateTimePicker = {};

  // normally we only need one popup, but the dateTime popup breaks if it's removed from the dom tree after construction,
  // so we create one per instance and only show one at a time.
  //
  var _popups = {};
  
  var _instanceNr = 0;
  
  $.extend(DateTimePicker, {
    widgetEventPrefix: 'datetimepicker',
    options: {
      value: 'live',
      useButtonSet: true
    },
    _create: function() {
      var self = this;
      var datePicker = this.datePicker = $("<div/>").datepicker({
        dateFormat: 'yy-mm-dd',
        onSelect: function(dateText, inst) {
          if (!self.options.showTime) {
            self.element.trigger('dpselect', self.dateTimeIsoFormat());
          }
        },
        changeMonth: true,
        changeYear: true,
        showOtherMonths: true,
        firstDay: 1,
        selectOtherMonths: true
      });
      
      var timePicker = self.timePicker = $("<div/>"); // TODO: create a 'timepicker' widget?
      if (!this.options.showTime) {
        timePicker.css({'display': 'none'});
      }
//      $(datePicker).children().addClass('dsy-versionmode-pit-details');
//      $(timePicker).addClass('dsy-versionmode-pit-details');
      var timePickerH = self.timeH = $("<input size='2'/>");
      var timePickerM = self.timeM = $("<input size='2'/>");
      var timePickerS = self.timeS = $("<input size='2'/>");
      
      timePicker.append($.i18n('dateTimePicker','time.abbrev.hour')+ ' ');
      timePicker.append(timePickerH);
      timePicker.append(' ' + $.i18n('dateTimePicker','time.abbrev.minute') + ' ');
      timePicker.append(timePickerM);
      timePicker.append(' ' + $.i18n('dateTimePicker','time.abbrev.second') + ' ');
      timePicker.append(timePickerS);
      
      var timeConfirm = $("<input type='button' value='ok'/>").button(); // TODO: i18n or use a checkbox image
      timeConfirm.click(function(ev) {
        self.element.trigger('dpselect', self.dateTimeIsoFormat()); // (side effect of dateTimeIsoFormat: cleans the time fields)
      });
      timePicker.append(timeConfirm);
      
      $(this.element).append(datePicker);
      $(this.element).append(timePicker);
    },
    $h: function() {
      return this.timeH;
    },
    $m: function() {
      return this.timeM;
    },
    $s: function() {
      return this.timeS;
    },
    cleanTimeField: function($field, min, max) {
        var value = $field.val();
        if (value != '') {
            value = parseInt(value, 10);
            if (value < min) value = 0;
            else if (value >= max) value = max - 1;
        } else {
          value == 0;
        }
        return value;
    },
    /** returns the date and time from the ui widgets in iso format
     */
    dateTimeIsoFormat: function() {
        var date = this.datePicker.datepicker('getDate');
        var result = $.datepicker.formatDate('yy-mm-dd', date);
        var self = this;
        
        var s = self.cleanTimeField(this.timeS, 0, 60);
        var m = self.cleanTimeField(this.timeM, 0, 60);
        var h = self.cleanTimeField(this.timeH, 0, 24);

        // TODO: prevent stripping via options.fullFormat = true. Or add a method to allow retrieving the date in full format
        // TODO: check for max/min values!!
        if (this.minDate && this.minDate.toDateString() == date.toDateString()) {
          if (h < this.minH) {
            h = this.minH; m = this.minM; s = this.minS;
          }
          if (h == this.minH && m < this.minM) {
            m = this.minM; s = this.minS;
          }
          if (h == this.minH && m == this.minM && s < this.minS) {
            s = this.minS;
          }
        }
        if (this.maxDate && this.maxDate.toDateString() == date.toDateString()) {
          if (h > this.maxH) {
            h = this.maxH; m = this.maxH; s = this.maxS;
          }
          if (h == this.maxH && m > this.maxM) {
            m = this.maxM; s = this.maxS;
          }
          if (h == this.maxH && m == this.maxM && s > this.maxS) {
            s = this.maxS;
          }
        }

        // always fill in higher-order blanks? option to control this?
        var padH = this.pad(''+h,2,'0'); 
        var padM = this.pad(''+m,2,'0'); 
        var padS = this.pad(''+s,2,'0');

        if (this.timeS.val() != '' || s != 0) {
          this.timeS.val(padS);
          if (this.timeM.val() == '') this.timeM.val(padM);
        }
        if (this.timeM.val() != '' || m != 0) {
          // TODO: zero-pad
          this.timeM.val(padM);
          if (this.timeH.val() == '') this.timeH.val(padH);
        }
        if (this.timeH.val() != '' || h != 0) {
          // TODO: zero-pad
          this.timeH.val(padH);
        }
        
        if (this.timeH.val() != '' || this.timeS.val() != '' || this.timeS.val() != '')
          result += 'T' + this.timeH.val();
        if (this.timeS.val() != '' || this.timeS.val() != '')
          result += ':' + this.timeM.val();
        if (this.timeS.val() != '')
          result += ':' + this.timeS.val();

        return result;
    },
    pad: function(value, minlength, char) {
      value = String(value);
      while (value.length < minlength) {
        value = char + value;
      }
      return value;
    },
    userFormat: function(d) {
        var h = this.pad(''+d.getHours(), 2, '0');
        var m = this.pad(''+d.getMinutes(), 2, '0');
        var s = this.pad(''+d.getSeconds(), 2, '0');
        var result = $.datepicker.formatDate('yy-mm-dd', d);
        result += ' ' + h + ':' + m + ':' + s;
        return result; 
    },
    wireFormat: function(d) {
      var h = this.pad(''+d.getHours(), 2, '0');
      var m = this.pad(''+d.getMinutes(), 2, '0');
      var s = this.pad(''+d.getSeconds(), 2, '0');
      var result = $.datepicker.formatDate('yy-mm-dd', d);
      result += 'T' + h + ':' + m + ':' + s;
      return result; 
    },
    parseDateTime: function(dt) {
        var date = dt;
        if (date.indexOf("T") >= 0)
          date = date.replace(/T.*/,'');

        date = $.datepicker.parseDate('yy-mm-dd', dt);

        var h = 0;
        var m = 0;
        var s = 0;
        var time = dt;
        if (time.indexOf("T") >= 0)
          time = time.replace(/.*T/, '');
        if (time.indexOf("+") >= 0 || time.indexOf("-") >= 0)
          time = time.replace(/[+-].*/,'');
          
        if (time.indexOf(":") >= 0) {
          hms = time.split(":");
          if (hms.length > 0)
            h = parseInt(hms[0], 10)
          if (time.length > 1)
            m = parseInt(hms[1], 10)
          if (time.length > 2)
            s = parseInt(hms[2], 10)
        }
          
        date.setHours(h);
        date.setMinutes(m);
        date.setSeconds(s);
        
        return date;
    },
    setValue: function(dateTime) {
        var d = this.parseDateTime(dateTime);
        // timefields must be processed first because the datepicker resets them
        if (dateTime.indexOf('T') > 0) {
          this.timeH.val(this.pad(d.getHours(), 2, '0'));
          this.timeM.val(this.pad(d.getMinutes(), 2, '0'));
          this.timeS.val(this.pad(d.getSeconds(), 2, '0'));
        } else {
          this.timeH.val('');
          this.timeM.val('');
          this.timeS.val('');
        }
        this.datePicker.datepicker('setDate', d);
    },
    setMaxValue: function(maxValue) {
        var d = this.parseDateTime(maxValue);
        // timefields must be processed first because the datepicker resets them
        this.maxH = d.getHours();
        this.maxM = d.getMinutes();
        this.maxS = d.getSeconds();
        this.datePicker.datepicker('option', 'maxDate', d);
        this.maxDate = d;
    },
    setMinValue: function(minValue) {
        // timefields must be processed first because the datepicker resets them
        var d = this.parseDateTime(minValue);
        this.minH = d.getHours();
        this.minM = d.getMinutes();
        this.minS = d.getSeconds();
        this.datePicker.datepicker('option', 'minDate', d);
        this.minDate = d;
    }
  });
  
  if (!$.datetimepicker) {
    $.widget("ui.datetimepicker", DateTimePicker);
  }
  
})(jQuery);