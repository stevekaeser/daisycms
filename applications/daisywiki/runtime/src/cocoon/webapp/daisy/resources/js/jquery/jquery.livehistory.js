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
/*jslint white: false, onevar: false, plusplus: false */

(function($) {

  var LiveHistory = {};
  var $dialog = $("<div/>");
  var $dtp = $("<div/>");
  
  $(function() {
    $('body').append($dialog);
    $dialog.dialog({ autoOpen: false, modal:true });
    $dialog.append($dtp);
    $dtp.datetimepicker({
      showTime: true
    });
  });
  
  var lhInstance;
  $dtp.bind('dpselect', function(ev, data) { // TODO: ask @mpo about namespacing etc
    if (!lhInstance)
      return;
    
    $dialog.dialog('close');
    lhInstance.handleDateChange(data);
  });

  $.extend(LiveHistory, {
    widgetEventPrefix: 'livehistory',
    options: {
    },
    _create: function() {
      var self = this, o = this.options;

      // data
      this.newLocalId = 1;
      this.entries = [];
      this.additionsByLocalId = {};
      this.deletionsByLocalId = {};
      
      // read the initial data
      var last;      
      for (var i = 0; i<o.initialData.length; i++) {
        var e = o.initialData[i];
        if (last && last.endDate != e.beginDate) {
          var entry = {
            'localId': self.newLocalId++,
            'beginDate': last.endDate, 
            'endDate': e.beginDate,
            'versionId': null,
          };
          this.entries.push(entry);
        }
        
        e.localId = self.newLocalId++;
        this.entries.push(e);
        last = e;
      }
      if (last && last.endDate) {
        this.entries.push({
          'localId': self.newLocalId++,
          'beginDate': last.endDate,
          'versionId': null
        });
      }
      this.entryCount = this.entries.length;
      // create 'prev' and 'next' pointers.
      for (i = 0; i<this.entryCount-1; i++) {
        this.entries[i].next = this.entries[i+1];
        this.entries[i+1].prev = this.entries[i];
      }
      // ui elements
      this.tbody = this.element;

      this.helperRow = $("<tr><td></td><td></td><td><span/></td></tr>");
      this.tbody.append(this.helperRow);
      this.newRow = this.helperRow.find('span');
      this.newRow.css({cursor:'pointer'});

      // if there are no entries in read-only mode, show a nicer message instead of the (empty) table.
      if (this.entries.length == 0) {
        if (this.options.readOnly) {
            this.tbody.parent().hide();
            if (this.options.emptyHistoryEl) {
              $(this.options.emptyHistoryEl).show();
            }
        } else {
          this.newRow.show();
        }
      }
      
      this.infinityRow = $('<tr><td class="dsy-lh-interspacing" rowspan="2"><center>&#8734;</center></td></tr>');
      if (this.options.readOnly) { // the spacer cells are used to make the date and version rows are properly arranged
        this.spacercells = $('<td class="dsy-lh-spacer">&nbsp;</td>');
      } else {
        this.spacercells = $('<td class="dsy-lh-spacer">&nbsp;</td><td class="dsy-lh-spacer">&nbsp;</td>');
      }

      newRowImg = $('<img/>');
      newRowImg.attr({
        alt:$.i18n("liveHistory","new.row"),
        src:daisy.mountPoint + '/resources/skins/' + daisy.skin + '/images/new.gif'
      });
      this.newRow.append(newRowImg);
      this.newRow.click(function(ev) {
          var entry = {
              'beginDate': $dtp.datetimepicker('wireFormat', new Date()),
              'localId': self.newLocalId++
          };
          self.addLocal(entry);
          self.addRow(entry);
          self.helperRow.hide();
      });
      
      if (this.entries.length > 0) {
        this.helperRow.hide();
      }
      
      // build the UI for the initial data
      for (i=0; i<this.entries.length; i++) {
        this.addRow(this.entries[i]);
      }
      $(this.element).append(this.infinityRow);
      if (this.entryCount == 0) {
        this.infinityRow.hide();
      }
    },
    addRow: function(entry, row) {
        var self = this;
        // Create all required widgets:
        var $dRow, $vRow, $dLabel, $vSelect, $remove1;

        // identify all widgets
        if (this.options.readOnly) {
          var $dRow = $('<tr><td class="dsy-lh-interspacing" rowspan="2"><span/></td></tr>');
          var $vRow = $('<tr><td class="dsy-lh-interspacing" rowspan="2"><select/> <a href="javascript:void(0);"/></td></tr>');
        } else {
          var $dRow = $('<tr><td class="dsy-lh-interspacing" rowspan="2"><span/></td></tr>');
          var $vRow = $('<tr><td class="dsy-lh-interspacing" rowspan="2"><select/> <a href="javascript:void(0);"/></td><td class="dsy-lh-interspacing" rowspan="2"><span><img alt="split"/></span> <span><img alt="remove"/></span></td></tr>');
        }
        
        if (!entry.prev) {
          $dRow.append(this.spacercells);
        }
        
        var $dLabel = $dRow.find("span").eq(0);
        var $vSelect = $vRow.find("select");
        var $remove1 = $vRow.find("span").eq(1);
        var $viewLink = $vRow.find("a");
        $viewLink.text($.i18n("liveHistory","show"));

        // initialize the date label
        var d = $dtp.datetimepicker('parseDateTime', entry.beginDate);
        var t = $dtp.datetimepicker('wireFormat', d);
        $dLabel.text($dtp.datetimepicker('userFormat', d));
        if (!this.options.readOnly) {
            $dLabel.css({cursor: 'pointer'});
            this.addCalendarImage($dLabel);
            $dLabel.click(function(ev) {
              lhInstance = self;
              self.lastEntry = entry;
              self.lastRow = $dRow;
              $dialog.dialog('open');
              $dtp.datetimepicker('setValue', entry.beginDate);
              if (entry.prev) {
                $dtp.datetimepicker('setMinValue', entry.prev.beginDate);
              } else {
                $dtp.datetimepicker('setMinValue', '1970-01-01');
              }
              if (entry.next) {
                $dtp.datetimepicker('setMaxValue', entry.next.beginDate);
              } else {
                $dtp.datetimepicker('setMaxValue', '3000-01-01')
              }
            });
            $dLabel.data('entry', entry);
        }

        // initialize the versionId select box
        var $noVersion = $("<option/>");
        $noVersion.text("-");
        if (!entry.versionId) {
          $noVersion.attr('selected', 'selected');
        }
        $vSelect.append($noVersion);
        for (var j = 1; j <= this.options.lastVersionId; j++) {
          var $o = $("<option/>");
          $o.attr("value", j);
          var text = "" + j;
          if (j == this.options.liveVersionId) {
            text += " (" + $.i18n("liveHistory","live") + ")";
          }
          $o.text(text);
          if (entry.versionId == j) {
            $o.attr('selected','selected');
          }
          $vSelect.append($o);
        }

        if (entry.versionId != '-') {
          $viewLink.attr('href', self.options.documentPath + '/version/' + entry.versionId).show();
        } else {
          $viewLink.hide();
        }
        if (this.options.readOnly) {
          $vSelect.attr('disabled','disabled');
        }
        $vSelect.bind('change', $vSelect, function(ev) {
          var entry = ev.data.data('entry');
          
          var $o = $(this).children('option:selected');
          self.deleteLocal(entry);
            
          delete entry.id;
          entry.localId = self.newLocalId++;
          entry.versionId = $o.val();
          self.addLocal(entry);
          if (entry.versionId != '-') {
            $viewLink.attr('href', self.options.documentPath + '/version/' + entry.versionId).show();
          } else {
            $viewLink.hide();
          }
        });
        $vSelect.data('entry', entry);
        $vSelect.data('viewLink', $viewLink);
        if ($vSelect.children('option:selected').val()=='-') {
          $viewLink.hide();
        }
        
        // add the rows to the table
        if (!row) {
          $(self.element).append($dRow);
          $(self.element).append($vRow);
          // append a row marking 'infinity' row after the last entry.
        } else {
          row.next().after($dRow);
          $dRow.after($vRow);
        }
        if (!entry.endDate) {
          $vRow.after(this.infinityRow);
          this.infinityRow.show();
        }

        if  (!this.options.readOnly) {
          $dRow.find('td:eq(0)').mouseover(function(ev) {
            $(this).addClass('dsy-lh-highlight');
          }).mouseout(function(ev) {
            $(this).removeClass('dsy-lh-highlight');
          });
          
          $vRow.mouseover(function(ev) {
            $dRow.find('td:eq(0)').add($vRow).addClass('dsy-lh-highlight');
          }).mouseout(function(ev) {
            $dRow.find('td:eq(0)').add($vRow).removeClass('dsy-lh-highlight');
          });
        }
        
        $remove1.css('cursor','pointer');
        $remove1.find('img').attr('src',daisy.mountPoint + '/resources/skins/' + daisy.skin + '/images/delete.gif');
        $remove1.attr('title', $.i18n("liveHistory", "remove"));
        $remove1.bind('click', $remove1, function(ev) {
          var t = this;
          self.handleDelete(ev);
        });
        $remove1.data('row', $dRow);
        $remove1.data('entry', entry);
        
        var $split = $vRow.find("span").eq(0);
        $split.find('img').attr('src',daisy.mountPoint + '/resources/skins/' + daisy.skin + '/images/split.gif');
        $split.attr('title', $.i18n("liveHistory", "split"));
        $split.css('cursor','pointer');
        $split.bind('click', $split, function(ev) { // create a new entry and add rows for it.
          var entry = ev.data.data('entry');
          var row = ev.data.data('row');
          self.deleteLocal(entry);
          
          delete entry.id;
          entry.localId = self.newLocalId++;
          self.addLocal(entry);
          var origEndDate = entry.endDate;
          entry.endDate = entry.beginDate;
          
          var newEntry = { localId: self.newLocalId++,
            beginDate: entry.beginDate,
            endDate: origEndDate,
            versionId: entry.versionId
          };
          newEntry.prev = entry;
          newEntry.next = entry.next;
          if (entry.next) { 
            entry.next.prev = newEntry;
          }
          entry.next = newEntry;

          self.addLocal(newEntry);
          
          self.addRow(newEntry, row);
        });
        $split.data('row', $dRow);
        $split.data('entry', entry);
    },
    addCalendarImage: function(label) {
        var calendarImg = $('<img src="/resources/skins/' + daisy.skin + '/images/calendar.gif"></img>');
            calendarImg.css('margin-left', '2px');
            label.append(calendarImg);
    },
    handleDelete: function(ev) {
      var entry = ev.data.data('entry');
      var drow = ev.data.data('row');

      // remove the current entry, fix the linked list
      var prev = entry.prev;
      var next = entry.next;
      if (prev) {
        prev.next = entry.next;
      }
      if (next) {
        next.prev = entry.prev; 
      }

      if (!prev && !next) {
          this.helperRow.show();
      }
      
      // update additions & deletions
      this.deleteLocal(entry);
      if (prev) {
        this.deleteLocal(prev);

        // update the entry
        delete prev.id;
        prev.localId = this.newLocalId++;
        prev.endDate = entry.endDate;
        this.addLocal(prev);
      }
      
      // UI fixups when there are no entries left:
      if (this.entryCount > 0) {
        if (!entry.prev) { // if this was the first entry, move the spacer cells
          drow.next().next().append(this.spacercells);
        }
      } else {
        this.infinityRow.hide();
      }
      
      // remove from UI
      drow.next().remove();
      drow.remove();
    },
    handleDateChange: function(data) {
      var entry = this.lastEntry;
      var row = this.lastRow;

      var self = this;
      this.deleteLocal(entry);
      
      var d = $dtp.datetimepicker('parseDateTime', data);
      var uf = $dtp.datetimepicker('userFormat', d);
      var wf = $dtp.datetimepicker('wireFormat', d);

      delete entry.id;
      entry.beginDate = wf;
      entry.localId = this.newLocalId++;
      this.addLocal(entry);
      
      var $label = row.find("span").first();
      $label.text(uf);
      this.addCalendarImage($label);
      
      var prev = entry.prev;
      if (prev) {
        this.deleteLocal(prev);
        
        delete prev.id;
        prev.localId = this.newLocalId++;
        this.addLocal(prev);
        prev.endDate = wf;
      }
    },
    addLocal: function(entry) {
      this.additionsByLocalId[entry.localId] = entry;
      this.entryCount++;
    },
    deleteLocal: function(entry) {
      delete this.additionsByLocalId[entry.localId];
      this.deletionsByLocalId[entry.localId] = $.extend({}, entry); // register a clone of the entry for deletion
      this.entryCount--;
    },
    debug: function() {
      //debugger;
    },
    getXmlDiff: function() {
      var added = [];
      var deleted = [];
      for (var i in this.additionsByLocalId) {
        if (this.additionsByLocalId[i].versionId && this.additionsByLocalId[i].versionId != '-') {
          added.push(this.additionsByLocalId[i]);
        }
      }
      for (i in this.deletionsByLocalId) {
        if (this.deletionsByLocalId[i].id)
          deleted.push(this.deletionsByLocalId[i].id);
      }
      
      var xml = $('<xml version="1.0"?><livehistory/>');
      var lh = xml.find('livehistory');
      for (i in deleted) {
        var d = $('<del/>');
        d.attr('id', deleted[i]);
        lh.append(d);
      }
      for (i in added) {
        var a = $('<add/>');
        a.attr('beginDate', added[i].beginDate);
        a.attr('endDate', added[i].endDate);
        a.attr('versionId', added[i].versionId);
        lh.append(a);
      }
      
      return xml.html();
    }
  });
  
  if (!$.livehistory) {
    $.widget("ui.livehistory", LiveHistory);
  }
  
})(jQuery);
