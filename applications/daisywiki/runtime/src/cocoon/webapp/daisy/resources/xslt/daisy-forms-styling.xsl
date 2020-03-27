<?xml version="1.0"?>
<!--
  Copyright 2004 Outerthought bvba and Schaubroeck nv

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:fi="http://apache.org/cocoon/forms/1.0#instance"
  xmlns:daisyutil="xalan://org.outerj.daisy.frontend.util.XslUtil"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:include href="wikidata:/resources/cocoon/forms/forms-page-styling.xsl"/>
  <xsl:include href="wikidata:/resources/cocoon/forms/forms-advanced-field-styling.xsl"/>
  
  <xsl:include href="wikidata:/resources/xslt/custom-form-styling.xsl"/>

  <xsl:param name="resources-uri"/>
  <xsl:param name="daisy-resources-uri"/>
  <xsl:param name="enable-htmlarea">false</xsl:param>

  <xsl:template match="head">
    <head>
      <!-- Enable this for dojo debugging -->
      <script>
        var djConfig = {};
        if (document.cookie.indexOf("dsy-dojo-debug=true") > -1) {
          djConfig.isDebug = true;
        }
      </script>

      <!-- Copy of the Cocoon initialisation from forms-field-styling.xsl -->
      <!-- Included here because it was always overwriting the djConfig variable -->
      <!-- Otherwise could have done this: -->
      <!--<xsl:apply-templates select="." mode="forms-page"/>-->
      <!--<xsl:apply-templates select="." mode="forms-field"/>-->
      <script type="text/javascript">
        djConfig.locale = "<xsl:value-of select="$dojoLocale"/>";
        var cocoon;
        if (!cocoon)
          cocoon = {};
        cocoon.resourcesUri = "<xsl:value-of select="$resources-uri"/>";
      </script>
      <script src="{$resources-uri}/dojo/dojo.js" type="text/javascript"/>           <!-- load dojo -->
      <script type="text/javascript">
        if (djConfig.isDebug) {
          dojo.require("dojo.debug.console");
          dojo.require("dojo.widget.Tree");
        }
      </script>
      <script type="text/javascript">dojo.require("dojo.widget.*");</script>         <!-- require dojo.widget for auto-loading -->
      <script src="{$resources-uri}/forms/js/forms-lib.js" type="text/javascript"/>  <!-- load legacy scripts -->
      <!-- load forms library -->
      <script type="text/javascript">
      dojo.registerModulePath("cocoon.forms", "../forms/js");                        <!-- tell dojo how to find our forms module. NB: (since 2.1.11, replaces cocoon.js) -->
      dojo.require("cocoon.forms.common");                                           <!-- tell dojo we require the commons library -->
      dojo.addOnLoad(cocoon.forms.callOnLoadHandlers);                               <!-- ask dojo to run our onLoad handlers -->
      </script>
      <xsl:copy-of select="fi:init/node()"/>                                         <!-- copy optional initialisation from form template -->
      <link rel="stylesheet" type="text/css" href="{$resources-uri}/forms/css/forms.css"/>

      <!-- copy the additional cforms initialisation from forms-advanced-field-styling.xsl (excluding htmlarea, which is handled elsewhere) -->
      <script src="{$resources-uri}/forms/mattkruse-lib/AnchorPosition.js" type="text/javascript"/>
      <script src="{$resources-uri}/forms/mattkruse-lib/PopupWindow.js" type="text/javascript"/>
      <script src="{$resources-uri}/forms/mattkruse-lib/OptionTransfer.js" type="text/javascript"/>
      <script src="{$resources-uri}/forms/mattkruse-lib/selectbox.js" type="text/javascript"/>
    
    <xsl:apply-templates select="." mode="forms-htmlarea"/>
      <!-- End of Cocoon stuff, continue Daisy stuff -->
      <script>
        dojo.registerModulePath("daisy", "../../js");
      </script>

      <xsl:if test="$enable-htmlarea = 'true'">
        <script type="text/javascript">
          _editor_url = "<xsl:value-of select="concat($resources-uri, '/forms/htmlarea/')"/>";
          _editor_lang = "<xsl:value-of select="$htmlarea-lang"/>";
        </script>
        <script type="text/javascript" src="{$resources-uri}/forms/htmlarea/htmlarea.js"></script>
        <script type="text/javascript" src="{$resources-uri}/forms/htmlarea/lang/{$htmlarea-lang}.js"></script>
        
        <script src="{$daisy-resources-uri}/js/daisy_edit.js" type="text/javascript"/>
        <script src="{$resources-uri}/forms/htmlarea/plugins/daisy-plugins-common.js" type="text/javascript"/>

        <script src="{$resources-uri}/forms/htmlarea/plugins/TableOperations/table-operations.js"></script>
        <script src="{$resources-uri}/forms/htmlarea/plugins/TableOperations/lang/{$htmlarea-lang}.js"></script>
        <script src="{$resources-uri}/forms/htmlarea/plugins/BlockSwitcher/block-switcher.js"></script>
        <script src="{$resources-uri}/forms/htmlarea/plugins/BlockSwitcher/lang/{$htmlarea-lang}.js"></script>
        <script src="{$resources-uri}/forms/htmlarea/plugins/DaisyImageUtils/daisy-image-utils.js"></script>
        <script src="{$resources-uri}/forms/htmlarea/plugins/DaisyImageUtils/lang/{$htmlarea-lang}.js"></script>
        <script src="{$resources-uri}/forms/htmlarea/plugins/DaisyMisc/daisy-misc.js"></script>
        <script src="{$resources-uri}/forms/htmlarea/plugins/DaisyMisc/lang/{$htmlarea-lang}.js"></script>
        <script src="{$resources-uri}/forms/htmlarea/plugins/DaisyLinkUtils/daisy-link-utils.js"></script>
        <script src="{$resources-uri}/forms/htmlarea/plugins/DaisyLinkUtils/lang/{$htmlarea-lang}.js"></script>
        <script src="{$resources-uri}/forms/htmlarea/plugins/DaisyBookUtils/daisy-book-utils.js"></script>
        <script src="{$resources-uri}/forms/htmlarea/plugins/DaisyBookUtils/lang/{$htmlarea-lang}.js"></script>
      </xsl:if>

      <xsl:apply-templates select="." mode="daisy-admin"/>
      <xsl:apply-templates/>
    </head>
  </xsl:template>

  <xsl:template match="head" mode="forms-htmlarea">
    <!-- overriden from cforms default to insert nothing -->
  </xsl:template>

  <xsl:template match="head" mode="daisy-admin">
  </xsl:template>

  <xsl:template match="body">
    <body>
      <!--+ !!! If template with mode 'forms-page' adds text or elements
          |        template with mode 'forms-field' can no longer add attributes!!!
          +-->
      <xsl:apply-templates select="." mode="forms-page"/>
      <xsl:apply-templates select="." mode="forms-field"/>
      <xsl:apply-templates/>
    </body>
  </xsl:template>

  <xsl:template match="fi:validation-message[*]">
    <div style="border: 1px solid #c03333; background-color: #f1b2b2; margin: 4px; padding: 2px 4px;">
      <xsl:for-each select="*">
        <xsl:choose>
          <xsl:when test="position() = 1">
            <strong><xsl:copy-of select="./node()"/></strong>
          </xsl:when>
          <xsl:otherwise>
            <br/><xsl:copy-of select="./node()"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:for-each>
    </div>
  </xsl:template>
  
  <xsl:template match="validation-warning">
    <xsl:if test="ancestor::fi:form-template//fi:validation-message">
      <p class="daisy-error" style="text-align: center">
        <xsl:choose>
          <xsl:when test="message">
            <xsl:copy-of select="message"/>
          </xsl:when>
          <xsl:otherwise>
            There are validation errors.
          </xsl:otherwise>
        </xsl:choose>
      </p>
    </xsl:if>
  </xsl:template>

  <xsl:template match="fi:action[fi:styling/@type='link']">
    <a id="{@id}LinkPeer" class="{fi:styling/@class}" href="#" onclick="document.getElementById('{@id}').click(); return false"><xsl:copy-of select="fi:label/node()"/></a>
    <input id="{@id}" type="submit" name="{@id}" value="x" style="display:none"/>
  </xsl:template>

  <!--
     | Template to show CForms-like validation messages outside of widgets
     -->
  <xsl:template match="fi:validation-error">
    <span dojoType="forms:infopopup" style="display:none" class="forms-validation-message-popup"
           icon="validation-message.gif">
      <xsl:copy-of select="node()"/>
    </span>
  </xsl:template>

  <!-- alternative styling for messages widget -->
  <xsl:template match="fi:messages">
    <xsl:if test="fi:message">
      <xsl:variable name="id" select="generate-id()"/>
      <div id="{$id}" style="background-color: #97c9fe; border: 1px solid black; margin: 16px; padding: 8px;">
        <xsl:for-each select="fi:message">
          <div style="margin-top: 3px; margin-bottom: 3px"><xsl:apply-templates/></div>
        </xsl:for-each>
        <div style="text-align: right"><a href="#" onmouseover="status=''; return true;" onclick="document.getElementById('{$id}').style.display='none'">Hide</a></div>
      </div>
    </xsl:if>
  </xsl:template>

  <xsl:template match="fi:field[fi:styling[@type='daisy-htmlarea']]">
    <span id="{@id}">
      <textarea id="{@id}:input" name="{@id}" title="{fi:hint}">
        <xsl:apply-templates select="." mode="styling"/>
        <!-- remove carriage-returns (occurs on certain versions of IE and doubles linebreaks at each submit) -->
        <xsl:value-of select="daisyutil:translateForHtmlArea(string(fi:value))"/>
      </textarea>
      <xsl:apply-templates select="." mode="common"/>
    </span>
  </xsl:template>

  <xsl:template match="fi:field[@state='disabled' and fi:styling[@type='daisy-htmlarea']]">
    <span id="{@id}">
      <textarea id="{@id}:input" title="{fi:hint}" style="display:none">
        <!-- remove carriage-returns (occurs on certain versions of IE and doubles linebreaks at each submit) -->
        <xsl:value-of select="daisyutil:translateForHtmlArea(string(fi:value))"/>
      </textarea>
      <iframe id="{@id}:view"/>
      <script>
        (function($){
            var widgetId = "<xsl:value-of select="@id"/>";
            var iframe = document.getElementById(widgetId + ":view");
            iframe.style.width = "100%";
            if (window.editorHeightListeners == null)
              window.editorHeightListeners = new Array();
              var heightListener = function(height) {
                iframe.style.height = height.toFixed(0) + "px";
              };
              window.editorHeightListeners.push(heightListener);
        })(jQuery);
        
        dojo.addOnLoad(function(){
            var widgetId = "<xsl:value-of select="@id"/>";
            var textarea = $(document.getElementById(widgetId + ":input"));
            var iframe = document.getElementById(widgetId + ":view");
            var iframeDoc = iframe.contentWindow.document;
            iframeDoc.open();
            iframeDoc.write(textarea.val());
            iframeDoc.close();
            
            var docEl = iframeDoc.documentElement;
            var head = iframeDoc.getElementsByTagName("head")[0];
            var link = iframeDoc.createElement("link");
            link.setAttribute("rel", "stylesheet");
            link.setAttribute("type", "text/css");
            link.setAttribute("href", daisy.mountPoint + "/resources/skins/" + daisy.skin + "/css/htmlarea.css");
            head.appendChild(link);
        });
      </script>
    </span>
  </xsl:template>

  <!--+
      | fi:multivaluefield that allows free entries but looks up labels from a selection list nonetheless
      | Slight modification from the Cocoon fi:multivaluefield[not(fi:selection-list)] styling
      +-->
  <xsl:template match="fi:multivaluefield[fi:styling/@type='mv-free-entry']">
    <xsl:variable name="id" select="@id"/>

    <div id="{$id}">
      <input name="{$id}:entry" id="{$id}:entry">
        <xsl:if test="fi:styling/@size">
          <xsl:attribute name="size"><xsl:value-of select="fi:styling/@size"/></xsl:attribute>
        </xsl:if>
        <xsl:if test="@state != 'active'">
          <xsl:attribute name="disabled">disabled</xsl:attribute>
        </xsl:if>
      </input>
      <br/>
      <xsl:call-template name="daisyFormsMultiValueEditor"/>
    </div>
  </xsl:template>

  <!--+
      | fi:multivaluefield[fi:styling[@list-type='double-listbox-popup']: produces a select-multiple widget
      | that takes very little space (and uses a popup for editing the selection)
      +-->
  <xsl:template match="fi:multivaluefield[fi:styling/@list-type='double-listbox-popup']" priority="100">
    <xsl:variable name="id" select="@id"/>
    <xsl:variable name="values" select="fi:values/fi:value/text()"/>

    <span id="{@id}" title="{fi:hint}" style="display:none">
      <select id="{@id}:input" name="{$id}" multiple="multiple" style="display: none">
        <xsl:apply-templates select="." mode="styling"/>
        <xsl:for-each select="fi:selection-list/fi:item">
          <xsl:variable name="value" select="@value"/>
          <option value="{$value}">
            <xsl:if test="$values[. = $value]">
              <xsl:attribute name="selected">selected</xsl:attribute>
            </xsl:if>
            <xsl:copy-of select="fi:label/node()"/>
          </option>
        </xsl:for-each>
      </select>
      <xsl:apply-templates select="." mode="common"/>
    </span>

    <div dojoType="daisy:DoubleListboxPopup" inputId="{@id}:input" usePopup="{not(fi:styling/@popup) or fi:styling/@popup='true'}">
    </div>
  </xsl:template>
  
  <xsl:template name="daisyFormsMultiValueEditor">
    <xsl:variable name="id" select="@id"/>
    <xsl:variable name="values" select="fi:values/fi:value/text()"/>
    <xsl:variable name="selectionList" select="fi:selection-list[1]"/>

    <table class="plainTable">
      <tr>
        <td>
          <select name="{$id}" id="{$id}:input" size="5" multiple="multiple" style="width: 25em;">
            <xsl:for-each select="$values">
              <xsl:variable name="value" select="string(.)"/>
              <xsl:variable name="label" select="$selectionList/fi:item[@value = $value]"/>
              <option value="{.}">
                <xsl:choose>
                  <xsl:when test="$label">
                    <xsl:copy-of select="$label/fi:label/node()"/>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:value-of select="$value"/>
                  </xsl:otherwise>
                </xsl:choose>
              </option>
            </xsl:for-each>
          </select>
        </td>
        <td>
          <xsl:if test="@state = 'active'">
            <!-- strangely, IE adds an extra blank line if there only a button on a line. So we surround it with nbsp -->
            <xsl:text>&#160;</xsl:text>
            <input type="image" id="{$id}:delete" src="{$resources-uri}/forms/img/delete.gif"/>
            <xsl:text>&#160;</xsl:text>
            <br/>
            <xsl:text>&#160;</xsl:text>
            <input type="image" id="{$id}:up" src="{$resources-uri}/forms/img/move_up.gif"/>
            <xsl:text>&#160;</xsl:text>
            <br/>
            <xsl:text>&#160;</xsl:text>
            <input type="image" id="{$id}:down" src="{$resources-uri}/forms/img/move_down.gif"/>
            <xsl:text>&#160;</xsl:text>
            <br/>
            <xsl:apply-templates select="." mode="common"/>
          </xsl:if>
        </td>
      </tr>
    </table>
    <xsl:if test="@state = 'active'">
      <script type="text/javascript">
        new DaisyFormsMultiValueEditor("<xsl:value-of select="$id"/>");
      </script>
    </xsl:if>
  </xsl:template>

    <!--+
      | fi:multivaluefield whereby selection list is rendered as a dropdown and
      | allows adding the same value multiple times. Supports async loading
      | of the selection list.
      +-->
  <xsl:template match="fi:multivaluefield[fi:styling/@type='daisy-mv-dropdown-list']" priority="100">
    <xsl:variable name="id" select="@id"/>
    <xsl:variable name="asyncList" select="boolean(fi:styling/@asyncList = 'true')"/>
    <xsl:variable name="values" select="fi:values/fi:value/text()"/>
    <xsl:variable name="selectionList" select="fi:selection-list[1]"/>

    <div id="{$id}">
      <!-- we don't allow free entry, but the entry field is here for the javascript to work -->
      <input type="hidden" name="{$id}:entry" id="{$id}:entry"/>

      <span id="{$id}:availablePanel">
        <xsl:if test="$asyncList">
          <xsl:attribute name="style">display: none</xsl:attribute>
        </xsl:if>

        <select id="{$id}:available" name="{$id}:available">
          <xsl:if test="@state != 'active'">
            <xsl:attribute name="disabled">disabled</xsl:attribute>
          </xsl:if>
        </select>
        <xsl:if test="not($asyncList)">
          <!-- Selection list data is added via javascript so that the hierarchicalLabel can
               be set a property on the option objects. -->
          <script>
            var data = [
              <xsl:for-each select="fi:selection-list/fi:item">
                <xsl:if test="position() != 1">,</xsl:if>
                ["<xsl:value-of select="daisyutil:escape(@value)"/>", "<xsl:value-of select="daisyutil:escape(fi:label)"/>", "<xsl:value-of select="daisyutil:escape(@hierarchicalLabel)"/>"]
              </xsl:for-each>
            ];

            var select = document.getElementById('<xsl:value-of select="$id"/>:available');
            var options = select.options;

            for (var i = 0; i &lt; data.length; i++) {
              var currentItem = data[i];
              var newOption = new Option(currentItem[1], currentItem[0]);
              newOption.hierarchicalLabel = currentItem[2];
              options[options.length] = newOption;
            }
          </script>
        </xsl:if>
        <xsl:text>&#160;</xsl:text>
        <input type="button" value="add" i18n:attr="value" onclick="daisyMvListAdd('{$id}'); return false;">
          <xsl:if test="@state != 'active'">
            <xsl:attribute name="disabled">disabled</xsl:attribute>
          </xsl:if>
        </input>
      </span>

      <xsl:if test="$asyncList">
        <span id="{$id}:loadOptionsPanel">
          <input type="button" value="formstyling.load-options" i18n:attr="value" onclick="daisyLoadListOptions('{$id}', '{$id}:available', '{fi:styling/@fieldTypeId}'); return false;">
            <xsl:if test="@state != 'active'">
              <xsl:attribute name="disabled">disabled</xsl:attribute>
            </xsl:if>
          </input>
        </span>
        <span id="{$id}:pleaseWaitPanel" style="display: none">
          <img src="{$daisy-resources-uri}/skins/default/images/progress_indicator_flat.gif"/>
        </span>
      </xsl:if>
      <br/>

      <xsl:call-template name="daisyFormsMultiValueEditor"/>
    </div>
  </xsl:template>

  <xsl:template match="fi:field[fi:styling/@type='daisy-async-dropdown-list']" priority="100">
    <xsl:variable name="id" select="@id"/>

    <span id="{$id}">
      <span id="{$id}:availablePanel" style="display:none">
        <select name="{$id}" id="{@id}:input">
          <xsl:if test="@state != 'active'">
            <xsl:attribute name="disabled">disabled</xsl:attribute>
          </xsl:if>
          <xsl:if test="fi:value">
            <option value="{fi:value}"/>
          </xsl:if>
        </select>
      </span>

      <span id="{$id}:loadOptionsPanel">
        <xsl:choose>
          <xsl:when test="@state != 'active'">
            <input type="text" value="{fi:styling/@valueLabel}" disabled="disabled"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="fi:styling/@valueLabel"/>
            <xsl:text>&#160;</xsl:text>
            <input type="button" value="formstyling.load-options" i18n:attr="value" onclick="daisyLoadListOptions('{$id}', '{$id}:input', '{fi:styling/@fieldTypeId}', '{daisyutil:escape(fi:value)}', true); return false;"/>
          </xsl:otherwise>
        </xsl:choose>
      </span>
      <span id="{$id}:pleaseWaitPanel" style="display: none">
        <img src="{$daisy-resources-uri}/skins/default/images/progress_indicator_flat.gif"/>
      </span>
      <xsl:text>&#160;</xsl:text>
      <xsl:apply-templates select="." mode="common"/>
    </span>
  </xsl:template>

  <!--+
      | Field in "output" state: display its value
      |
      | This is a modified version of the default cocoon template, which outputs the 'dojoType' attribute
      | for selected dojo widgets.
      +-->
  <xsl:template match="fi:field[@state='output']" priority="3">
    <span id="{@id}">
      <xsl:if test="fi:styling/@dojoType = 'daisy:LinkEditor'">
        <xsl:copy-of select="fi:styling/@dojoType"/>
      </xsl:if>
      <xsl:apply-templates select="." mode="css"/>
      <xsl:value-of select="fi:value/node()"/>
    </span>
  </xsl:template>

  <!--
     | For help messages that are not part of a widget (different with default
     | fi:help: doesn't add an id attribute.
     -->
  <xsl:template match="fi:standalone-help">
    <span dojoType="forms:infopopup" style="display:none" class="forms-help-popup" icon="help.gif">
      <xsl:copy-of select="node()"/>
    </span>
  </xsl:template>

  <!--+
      | Overridden from default Cocoon forms-field-styling.xsl
      |
      | Common stuff like fi:validation-message, @required.
      +-->
  <xsl:template match="fi:*" mode="common">
    <!-- validation message -->
    <xsl:apply-templates select="fi:validation-message"/>
    <!-- required mark -->
    <xsl:if test="@required='true' and not(fi:styling/@disable-required-mark)">
      <span class="forms-field-required forms {local-name()} required-mark"> * </span>
    </xsl:if>
    <xsl:apply-templates select="fi:help"/>
  </xsl:template>


  <!--
    Columns group items layout
  -->
  <xsl:template match="fi:group[fi:styling/@layout='daisy-columns']" mode="group-layout">
    <!-- additional class to set on cells -->
    <xsl:variable name="cellclass">
       <xsl:if test="fi:styling/@nowrap = 'true'">dsy-nowrap</xsl:if>
    </xsl:variable>

    <table class="dsyfrm-table" summary="{fi:hint}">
      <tbody>
        <xsl:apply-templates select="fi:items/*" mode="daisy-group-columns-content">
          <xsl:with-param name="cellclass" select="$cellclass"/>
        </xsl:apply-templates>
      </tbody>
    </table>
  </xsl:template>

  <!--
    Default columns layout : label left and input right
  -->
  <xsl:template match="fi:*" mode="daisy-group-columns-content">
    <xsl:param name="cellclass"/>
    <tr>
      <td class="dsyfrm-labelcell {$cellclass}"><xsl:apply-templates select="." mode="label"/></td>
      <td class="dsyfrm-widgetcell {$cellclass}"><xsl:apply-templates select="."/></td>
    </tr>
  </xsl:template>

  <xsl:template match="fi:custom-entry" mode="daisy-group-columns-content">
    <xsl:param name="cellclass"/>
    <tr>
      <xsl:if test="@entryId != ''">
        <xsl:attribute name="id"><xsl:value-of select="@entryId"/></xsl:attribute>
      </xsl:if>
      <td class="dsyfrm-labelcell {$cellclass} {fi:label-entry/@class}">
        <xsl:apply-templates select="fi:label-entry/node()"/>
      </td>
      <td class="dsyfrm-widgetcell {$cellclass} {fi:widget-entry/@class}">
        <xsl:apply-templates select="fi:widget-entry/node()"/>
      </td>
    </tr>
  </xsl:template>

  <!--
    widget-entry can be used on its own (without fi:custom-entry) for
    the case where there is no label to display.
  -->
  <xsl:template match="fi:widget-entry" mode="daisy-group-columns-content">
    <xsl:param name="cellclass"/>
    <tr>
      <td class="dsyfrm-labelcell {$cellclass}"></td>
      <td class="dsyfrm-widgetcell {$cellclass} {@class}">
        <xsl:apply-templates select="node()"/>
      </td>
    </tr>
  </xsl:template>

  <!--+
      | fi:upload specific for daisy-parts
      +-->
  <xsl:template match="fi:upload[fi:styling[@type='daisy-part']]">
    <script>
      dojo.require("dojo.i18n.common");
      dojo.require("dojo.json");
      
      dojo.requireLocalization("daisy.widget", "messages", null, /* available languages, to avoid 404 requests */ "ROOT,nl,fr,de,es,ru");
      var widgetI18n = dojo.i18n.getLocalization("daisy.widget", "messages");

      var EditAppletController = function(widgetId) {
        this.widgetId = widgetId;
      }
      
      EditAppletController.prototype.triggerUpload = function() {
        document.getElementById('part-uploader-<xsl:value-of select="@id"/>').style.display = 'none';
        document.getElementById('part-upload-notification-<xsl:value-of select="@id"/>').style.display = 'block';
        cocoon.forms.submitForm(dojo.byId(this.widgetId + ':input'));
      }
    
      EditAppletController.prototype.toggleUploadWidget = function(title, mimeTypes) {
        if (dojo.byId(this.widgetId + ':fileUploadContainer').style.display='none') {
          dojo.html.show(this.widgetId + ':fileUploadContainer');
        } else {
          dojo.html.hide(this.widgetId + ':fileUploadContainer');
        }
      }
      
      <![CDATA[
      EditAppletController.prototype.showEditApplet = function(editDialogConf) {
        window.needsConfirmForLeaving = false;
        var appletConf = editDialogConf.appletConf;
        dojo.html.hide(this.widgetId + ':fileUploadContainer');
        
        dojo.dom.removeChildren(dojo.byId('editAppletContainer_' + this.widgetId));
        var appletHtml = '<applet';
        for (aName in appletConf.attributes) {
          appletHtml += ' ' + aName + '=' + appletConf.attributes[aName];
        }
        appletHtml+='>\n';
        for (pName in appletConf.parameters) {
          appletHtml += '<PARAM name="'+pName+'" value ="'+appletConf.parameters[pName]+'"/>\n';
        }
        appletHtml+='</applet>';
        
        var applet = document.createElement("applet");

        for (aName in appletConf.attributes) {
          applet.setAttribute(aName, appletConf.attributes[aName]);
        }

        for (pName in appletConf.parameters) {
          var param = document.createElement('PARAM');
          param.setAttribute("name", pName);
          param.setAttribute("value", appletConf.parameters[pName]);
          applet.appendChild(param);
        }
        
        var outer = dojo.byId('editAppletContainer_' + this.widgetId);
        var inner = document.createElement('div');
        inner.className='editAppletContainer-inner';
        var closeDiv = document.createElement('div');
        closeDiv.className = 'dsy-editAppletButtons';
        var closeLink = document.createElement('a');

        closeLink.style.cursor = 'pointer';
        dojo.event.connect(closeLink, 'onclick', this, "askHideEditApplet");
        
        closeLink.appendChild(document.createTextNode(widgetI18n['editdoc.parteditor-applet.close']));
        closeDiv.appendChild(closeLink);
        inner.appendChild(closeDiv);
        outer.appendChild(inner);
        inner.appendChild(applet);
        
        dojo.widget.byId('parteditor-applet-dialog').show();
      }
      ]]>
      
      EditAppletController.prototype.addFormParameters = function(applet) {
        // here we can add additional parameters that the applet should send back when uploading
      }
      
      EditAppletController.prototype.askHideEditApplet = function() {
        var answer = confirm(widgetI18n['editdoc.parteditor-applet.close.confirm']);
        if (answer) {
          hideEditApplet(this.widgetId);
        }
      }
      
      var hideEditApplet = function(widgetId) {
        window.needsConfirmForLeaving = true;
      
        dojo.widget.byId('parteditor-applet-dialog').hide();
        dojo.dom.removeChildren(dojo.byId('editAppletContainer')); // destroy applet
      }
      
      var editAppletControllers;
      if (!editAppletControllers) {
        editAppletControllers = {};
      }
      editAppletControllers['<xsl:value-of select="@id"/>'] = new EditAppletController('<xsl:value-of select="@id"/>');
      
    </script> 

    <span id="{@id}" title="{fi:hint}">
      <xsl:if test="fi:value">
          <div>
            <!-- Has a value (filename): display it with a change button -->
            <xsl:apply-templates select="." mode="css"/>

            <xsl:text> [</xsl:text>
            <xsl:choose>
              <xsl:when test="fi:styling/@dataPath">
                <a href="{fi:styling/@dataPath}" onclick="window.needsConfirmForLeaving=false; return true;">
                  <xsl:value-of select="fi:value"/>
                </a>
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="fi:value"/>
              </xsl:otherwise>
            </xsl:choose>
            <xsl:text>] </xsl:text>

            <xsl:if test="@state != 'disabled'">
                <!-- 'Edit': Creates a modal applet to edit the existing part -->
                <input type="button" id="{@id}:edit" name="{@id}-edit" value="editdoc.upload.edit" i18n:attr="value" onclick="editAppletControllers['{@id}'].showEditApplet(window.editDialogConf)" class="forms upload-change-button"/>
                <xsl:text> </xsl:text>
                
                <!-- 'Remove': perform a roundtrip to the server to clear the upload widget  -->
                <input type="button" id="{@id}:remove" name="{@id}" value="editdoc.upload.remove" i18n:attr="value" onclick="document.getElementById('part-uploader-{@id}').style.display = 'none'; cocoon.forms.submitForm(document.getElementById('{@id}:input'));" class="forms upload-change-button"/>
                <xsl:text> </xsl:text>
                
                <!-- 'Other file': show an input (type='file'), does not actively clear what is on the server -->
                <xsl:text> </xsl:text>
                <input type="button" id="{@id}:change" name="{@id}-change" value="editdoc.upload.otherfile" i18n:attr="value" onclick="editAppletControllers['{@id}'].toggleUploadWidget('{fi:hint}', '{fi:styling/@mimeTypes}')" class="forms upload-change-button">
                  <xsl:apply-templates select="." mode="styling"/>
                </input>
            </xsl:if>

          </div>
      </xsl:if>
      <div id="{@id}:fileUploadContainer">
        <xsl:if test="fi:value">
          <xsl:attribute name="style">display:none;</xsl:attribute>
        </xsl:if>
        <input type="file" id="{@id}:input" name="{@id}" title="{fi:hint}" accept="{@mime-types}" onchange="editAppletControllers['{@id}'].triggerUpload();">
          <xsl:if test="@state != 'active'">
            <xsl:attribute name="disabled">disabled</xsl:attribute>
          </xsl:if>
          <xsl:apply-templates select="fi:styling"/>
        </input>
      </div>
      <xsl:apply-templates select="." mode="common"/>
    </span>
  </xsl:template>

</xsl:stylesheet>
