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
  xmlns:d="http://outerx.org/daisy/1.0"
  xmlns:p="http://outerx.org/daisy/1.0#publisher"
  xmlns:n="http://outerx.org/daisy/1.0#navigation"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:import href="daisyskin:xslt/pagevars.xsl"/>
  <xsl:import href="daisyskin:xslt/util.xsl"/>

  <xsl:variable name="skin" select="page/context/skin"/>
  
  <xsl:variable name="pubdoc" select="/page/p:publisherResponse/p:document"/>
  <xsl:variable name="document" select="$pubdoc/d:document"/>
  <xsl:variable name="documentId" select="$pubdoc/d:document/@id"/>
  <xsl:variable name="branchId" select="$pubdoc/d:document/@branchId"/>
  <xsl:variable name="languageId" select="$pubdoc/d:document/@languageId"/>
  <xsl:variable name="lastVersionId" select="number($document/@lastVersionId)"/>
  <xsl:variable name="documentName" select="$document/@name"/>
  <xsl:variable name="canPublish" select="boolean($pubdoc/d:aclResult/d:permissions/d:permission[@type='publish' and @action='grant'])"/>
  <xsl:variable name="isEditor" select="boolean($pubdoc/d:aclResult/d:permissions/d:permission[@type='write' and @action='grant'])"/>
  <xsl:variable name="canChangeMetadata" select="boolean($pubdoc/d:aclResult/d:permissions/d:permission[@type='write' and @action='grant']/d:accessDetails/d:permission[@type='version_meta' and @action='grant'])"/>
  
  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><xsl:call-template name="pageTitle"/></pageTitle>
      <xsl:call-template name="pageNavigation"/>
      <navigationInfo>
        <xsl:copy-of select="p:publisherResponse/p:group[@id = 'navigationInfo']/p:document/*"/>
      </navigationInfo>
      <xsl:call-template name="availableVariants"/>
      <content><xsl:call-template name="content"/></content>
      <layoutHints needsDojo="true" wideLayout="true"/>
    </page>
  </xsl:template>

  <xsl:template name="pageNavigation">
    <pageNavigation>
      <link>
        <title><i18n:text key="back-to-document"/></title>
        <path><xsl:value-of select="concat($documentPath, '.html', $variantQueryString)"/></path>
      </link>
      <link>
        <title><i18n:text key="doclayout.live-versions"/></title>
        <path><xsl:value-of select="concat($documentPath, '/live-versions.html', $variantQueryString)"/></path>
      </link>
      <link>
        <title><i18n:text key="hide-navigation"/></title>
        <path><xsl:value-of select="concat($documentPath, '/versions.html?layoutType=plain', $variantParams)"/></path>
      </link>
    </pageNavigation>
  </xsl:template>

  <xsl:template name="pageTitle">
    <i18n:text key="versions.for"/><xsl:text> </xsl:text><xsl:value-of select="$documentName"/>
  </xsl:template>

  <xsl:template name="content">
    <link rel="stylesheet" type="text/css" href="{$mountPoint}/resources/skins/{$skin}/css/inlinedialog.css"/>
    <script type="text/javascript">
      dojo.require("dojo.html.selection");
      dojo.require("daisy.dialog");
      dojo.require("dojo.dom");
    </script>
    
    <h1><xsl:value-of select="$documentName"/></h1>
    <span class="ui-tabs">
      <ul class="ui-tabs-nav ui-helper-reset ui-helper-clearfix ui-corner-all" style="border-bottom: 1px solid #ccc">
        <li class="ui-state-default ui-corner-top ui-tabs-selected ui-state-active">
          <a href="{$documentPath}/versions.html{$variantQueryString}"><span><i18n:text key="versions.overview"/></span></a>
        </li>
        <li class="ui-state-default ui-corner-top ui-tabs-selected">
          <a href="{$documentPath}/live-versions.html{$variantQueryString}"><span><i18n:text key="liveversions.history"/></span></a>
        </li>
      </ul>
    </span>
    <xsl:apply-templates select="$pubdoc/d:versions"/>

    <xsl:if test="$document/@createdFromBranch != ''">
      <br/>
       <i18n:text key="versions.created-from"/>
       <xsl:text> </xsl:text>
       <a href="{concat($documentPath, '/versions.html?branch=', $document/@createdFromBranch, '&amp;language=', $document/@createdFromLanguage)}"><i18n:text key="branch_lc"/><xsl:text> </xsl:text><xsl:value-of select="$document/@createdFromBranch"/>, <i18n:text key="language_lc"/><xsl:text> </xsl:text><xsl:value-of select="$document/@createdFromLanguage"/>, <i18n:text key="version_lc"/><xsl:text> </xsl:text><xsl:value-of select="$document/@createdFromVersionId"/></a>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="d:versions">

    <xsl:if test="/page/warnLockUserName">
      <i18n:translate>
        <i18n:text key="versions.lock.info-warn"/>
        <i18n:param><xsl:value-of select="/page/warnLockUserName"/></i18n:param>
      </i18n:translate>
      <xsl:if test="$document/d:lockInfo/@type = 'warn'">
        <xsl:call-template name="generatePostLink">
          <xsl:with-param name="action" select="concat($documentPath, '/version/', /page/requestedLiveVersionId, '?forceChangeLive=true')"/>
          <xsl:with-param name="label"><i18n:text key="versions.lock.ignore"/></xsl:with-param>
          <xsl:with-param name="id" select="concat(generate-id(), '-makeLive')"/>
        </xsl:call-template>
      </xsl:if>
    </xsl:if>
    <table class="versions default">
      <tr>
        <td style="border: none; border-right: inherit;"><span style="visibility:hidden"><strong><i18n:text key="versions.live"/></strong></span></td>
        <td style="border: none; border-right: inherit;"/>
        <th><i18n:text key="versions.nr"/></th>
        <th><i18n:text key="versions.time"/></th>
        <th><i18n:text key="versions.creator"/></th>
        <th><i18n:text key="versions.state"/></th>
        <th><i18n:text key="versions.change-type"/></th>
        <th><i18n:text key="versions.synced-with"/></th>
        <xsl:if test="$isEditor">
          <th><i18n:text key="versions.actions"/></th>
        </xsl:if>
        <th><i18n:text key="versions.diff"/></th>
        <th><i18n:text key="versions.change-comment"/></th>
      </tr>
      <xsl:apply-templates select="d:version">
        <xsl:sort select="@id" data-type="number" order="descending"/>
      </xsl:apply-templates>
      <xsl:if test="count($pubdoc/d:versions/d:version) > 1">
        <tr>
          <td style="border: none;"/>
          <td colspan="6" style="border: none; border-top: inherit;"/>
          <xsl:if test="$isEditor">
            <td style="border: none; border-top: inherit;"/>
          </xsl:if>
          <td colspan="2" style="border: none; border-top: inherit;">
            <input type="button" onclick="compareSelectedVersions();" value="versions.compare-selected" i18n:attr="value"/>
          </td>
        </tr>
      </xsl:if>

      
    </table>

    <xsl:if test="$canChangeMetadata or $canPublish">
      <div dojoType="dialog" id="version-update-dialog" widgetId="version-update-dialog" bgColor="white" style="display: none">
        <div class="dsy-version-update-dialog">
          <div class="dsydlg-title"><i18n:text key="versions.edit.version"/></div>
          <form id="version-update-form" method="POST">
            <input type="hidden" name="versionId"/>
            <table>
              <tr>
                <td><i18n:text key="versions.nr"/></td>
                <td><span id="versionId"/></td>
              </tr>
              <tr>
                <td><i18n:text key="versions.state"/></td>
                <td><xsl:choose>
                  <xsl:when test="$canPublish"><select id="newState" name="newState">
                    <option value="publish"><i18n:text key="publish"/></option>
                    <option value="draft"><i18n:text key="draft"/></option>
                  </select></xsl:when>
                  <xsl:otherwise>
                    <span id="fixedState"/>
                  </xsl:otherwise>
                </xsl:choose></td>
              </tr>
              <tr>
                <td><i18n:text key="versions.change-type"></i18n:text></td>
                <td><xsl:choose>
                  <xsl:when test="$canChangeMetadata"><select id="newChangeType" name="newChangeType">
                    <option value="major"><i18n:text key="major"/></option>
                    <option value="minor"><i18n:text key="minor"/></option>
                  </select></xsl:when>
                  <xsl:otherwise><span id="fixedChangeType"/></xsl:otherwise>
                </xsl:choose></td>
              </tr>
              <tr>
                <td><i18n:text key="versions.synced-with"></i18n:text></td>
                <td>
                  <div style="display:none"> <!-- used instead of input type="hidden" because those don't support the 'disabled' attribute-->
                    <input type="text" id="syncedWithLanguageId" name="newSyncedWithLanguageId">
                      <xsl:if test="not($canChangeMetadata)"><xsl:attribute name="disabled">true</xsl:attribute></xsl:if>
                    </input>
                    <input type="text" id="syncedWithVersionId" name="newSyncedWithVersionId">
                     <xsl:if test="not($canChangeMetadata)"><xsl:attribute name="disabled">true</xsl:attribute></xsl:if>
                    </input>
                  </div>
                  <select id="syncedWithLanguage">
                    <xsl:if test="not($canChangeMetadata)"><xsl:attribute name="disabled">true</xsl:attribute></xsl:if>
                  </select>
                  <xsl:text> </xsl:text>
                  <select id="syncedWithVersion">
                    <xsl:if test="not($canChangeMetadata)"><xsl:attribute name="disabled">true</xsl:attribute></xsl:if>
                  </select>
                  <xsl:text> </xsl:text>
                  <button type="button" name="lookupsyncedwith" title="tm.select-with-preview" i18n:attr="title" onclick="showPreviewSyncedWithDialog();">
                    <img src="{$mountPoint}/resources/skins/{$skin}/images/browse-link.gif"/>
                  </button>
                </td>
              </tr>
              <tr>
                <td><i18n:text key="versions.change-comment"/></td>
                <td><xsl:choose>
                  <xsl:when test="$canChangeMetadata"><input id="newChangeComment" name="newChangeComment" size="40" maxlength="1023"></input></xsl:when>
                  <xsl:otherwise><span id="newChangeComment"/></xsl:otherwise>
                </xsl:choose></td>
              </tr>
            </table>
            <div class="dsydlg-buttons">
              <input type="submit" class="dsyfrm-primaryaction" value="versions.editor.save" i18n:attr="value"/>
              <xsl:text> </xsl:text>
              <input type="button" value="versions.editor.cancel" i18n:attr="value" onclick="dojo.widget.byId('version-update-dialog').hide()"/>
            </div>
          </form>
        </div>
      </div>
      <script src="{$mountPoint}/resources/js/daisy_edit.js"/>
      <script type="text/javascript">
        dojo.require("dojo.widget.Dialog");
        
        function showVersionDialog(versionId, state, syncedWithLanguageId, syncedWithVersionId, changeType, changeComment, live) {
          dojo.byId('version-update-form').action = '<xsl:value-of select="concat($documentPath, '/version/')"/>' + versionId + '?<xsl:value-of select="$variantParams"/>';
        
          var versionIdSpan = dojo.byId('versionId');
          dojo.dom.removeChildren(versionIdSpan);
          versionIdSpan.appendChild(document.createTextNode(versionId));
          
          var i;
          
          var newStateSelect = dojo.byId('newState');
          if (newStateSelect) {
            i = 0;
            while (i &lt; newStateSelect.options.length &amp;&amp; newStateSelect.options[i].value != state)
              i++;
            if (i &lt; newStateSelect.options.length) {
              newStateSelect.selectedIndex = i;
            }
          } else {
            var stateSpan = dojo.byId('fixedState');
            dojo.dom.removeChildren(stateSpan);
            stateSpan.appendChild(document.createTextNode(i18n("version." + state)));
          }
          
          var newChangeTypeSelect = dojo.byId('newChangeType');
          if (newChangeTypeSelect) {
            i = 0;
            while (i &lt; newChangeTypeSelect.options.length &amp;&amp; newChangeTypeSelect.options[i].value != changeType)
              i++;
            if (i &lt; newChangeTypeSelect.options.length) {
              newChangeTypeSelect.selectedIndex = i;
            }
          } else {
            var changeTypeSpan = dojo.byId('fixedChangeType');
            dojo.dom.removeChildren(changeTypeSpan);
            changeTypeSpan.appendChild(document.createTextNode(i18n("changeType." + changeType)));
          }          
          
          dojo.byId('syncedWithLanguageId').value = syncedWithLanguageId;
          dojo.byId('syncedWithVersionId').value = syncedWithVersionId;
          tmEditorHelper.updateVersionIds();
          tmEditorHelper.updateSyncedWithSelects();
          
          var newChangeComment = dojo.byId('newChangeComment');
          if (newChangeComment.tagName == 'input') {
            newChangeComment.value = changeComment;
          } else {
            $("#newChangeComment").text(changeComment);
          }
          
          dojo.widget.byId('version-update-dialog').show();
        }
        
        function showPreviewSyncedWithDialog() {
          var languageInput = dojo.byId('syncedWithLanguageId');
          var versionInput = dojo.byId('syncedWithVersionId');
          
          var swLanguageId = dojo.byId('syncedWithLanguageId').value;
          var swVersionId = dojo.byId('syncedWithVersionId').value;
          
          if (swLanguageId == '-1')
            return;
          
          daisy.dialog.popupDialog(daisy.mountPoint + "/" + daisy.site.name + "/editing/previewSyncedWith?documentId=<xsl:value-of select="$document/@id"/>&amp;branch=<xsl:value-of select="$document/@branchId"/>&amp;language=<xsl:value-of select="$document/@languageId"/>"
                                   + "&amp;referenceLanguage=" + tmEditorHelper.getReferenceLanguageId() + "&amp;syncedWithLanguage=" + swLanguageId + "&amp;syncedWithVersionId=" + swVersionId, 
            function(params) {
              tmEditorHelper.setSyncedWithSelection(params.syncedWithLanguageId, params.syncedWithVersionId);
            },
            {}
          );
          
        }
        
        var tmEditorHelper;
        
        function initEditor() {
          var availableVariants = [];
          
          <xsl:for-each select="$pubdoc/d:availableVariants/d:availableVariant">
            <xsl:sort select="@languageName"/>
            availableVariants.push({ languageId: <xsl:value-of select="@languageId"/>,
              languageName: '<xsl:value-of select="@languageName"/>',
              branchId: <xsl:value-of select="@branchId"/>,
              branchName: '<xsl:value-of select="@branchName"/>', 
              lastVersionId: <xsl:value-of select="@lastVersionId"/>,
              liveVersionId: <xsl:value-of select="@liveVersionId"/>
            });
          </xsl:for-each>
          
          var docBranchId = '<xsl:value-of select="$document/@branchId"/>';
          
          var availableLanguageVariants = [];
          var haveLanguage = {};
          for (var i = 0; i &lt; availableVariants.length; i++) {
            var variant = availableVariants[i];
            if (variant.branchId == docBranchId &amp;&amp; !haveLanguage[variant.languageName]) {
              haveLanguage[variant.languageName] = true;
              availableLanguageVariants.push(variant);
            }
          }
          
          tmEditorHelper = new daisy.tm.EditorHelper('<xsl:value-of select="$document/@languageId"/>', '<xsl:value-of select="$document/@referenceLanguageId"/>', availableLanguageVariants);
        }
  
        dojo.addOnLoad(initEditor);
      </script>
    </xsl:if>
    
    <script type="text/javascript">
      var leftVersion = 0;
      var rightVersion = 0;
      
      function leftVersionChanged() {
        var valueRadios = document.getElementsByName('compareLeft');
        var updateRadios = document.getElementsByName('compareRight');
        if (valueRadios.length == 0 || updateRadios.length == 0) return;
        var i = 0;
        while (i &lt; valueRadios.length &amp;&amp; !valueRadios[i].checked) {
          i++;
        }
        if (i > valueRadios[i].length) return;
        var value = Number(valueRadios[i].value);
        leftVersion = value;
        for (var i = 0; i &lt; updateRadios.length; i++) {
          updateRadios[i].style.visibility = Number(updateRadios[i].value) > value ? 'visible' : 'hidden';
        }
      }

      function rightVersionChanged() {
        var valueRadios = document.getElementsByName('compareRight');
        var updateRadios = document.getElementsByName('compareLeft');
        if (valueRadios.length == 0 || updateRadios.length == 0) return;
        var i = 0;
        while (i &lt; valueRadios.length &amp;&amp; !valueRadios[i].checked) {
          i++;
        }
        if (i > valueRadios[i].length) return;
        var value = Number(valueRadios[i].value);
        rightVersion = value;
        for (var i = 0; i &lt; updateRadios.length; i++) {
          updateRadios[i].style.visibility = Number(updateRadios[i].value) &lt; value ? 'visible' : 'hidden';
        }
      }
      
      function compareSelectedVersions() {
        window.location='<xsl:value-of select="concat($documentPath,'/version/')"/>'+leftVersion+'/diff?otherVersion='+rightVersion+'<xsl:value-of select="$variantParams"/>';
      }
      
      dojo.addOnLoad(leftVersionChanged);
      dojo.addOnLoad(rightVersionChanged);
    </script>
  </xsl:template>

  <xsl:template match="d:version">
    <tr>
      <xsl:if test="@live">
        <xsl:attribute name="class">live</xsl:attribute>
      </xsl:if>
      <td style="border: none; border-right: inherit">
        <xsl:if test="@live"><strong><i18n:text key="versions.live"/></strong></xsl:if>
      </td>
      <xsl:if test="$canPublish">
        <td style="border: none; border-right: inherit">
          <a onclick="concat(generate-id(), '-makelive').submit()"></a>
          <xsl:choose>
            <xsl:when test="not(@live)">              
              <xsl:call-template name="generatePostLink">
                <xsl:with-param name="action" select="concat($documentPath, '/version/', @id, '?makeLive=true', $variantParams)"/>
                <xsl:with-param name="label">
                <img title="versions.make.live.immediately" i18n:attr="title" src="{$mountPoint}/resources/skins/{$skin}/images/make-live.gif"/>
                </xsl:with-param>
                <xsl:with-param name="id" select="concat(generate-id(), '-state')"/>
              </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
              <xsl:call-template name="generatePostLink">
                <xsl:with-param name="action" select="concat($documentPath, '/version/', @id, '?makeLive=false', $variantParams)"/>
                <xsl:with-param name="label">
                <img title="versions.undo.live.immediately" i18n:attr="title" src="{$mountPoint}/resources/skins/{$skin}/images/undo-live.gif"/>
                </xsl:with-param>
                <xsl:with-param name="id" select="concat(generate-id(), '-state')"/>
              </xsl:call-template>
            </xsl:otherwise>
            
          </xsl:choose>
          
        </td>        
      </xsl:if>
      <td class="dsy-nowrap"><a href="{$documentPath}/version/{@id}{$variantQueryString}"><xsl:value-of select="@id"/></a></td>
      <td class="dsy-nowrap"><xsl:value-of select="@createdFormatted"/></td>
      <td class="dsy-nowrap"><xsl:value-of select="@creatorDisplayName"/></td>
      <td class="dsy-nowrap">
        <!-- when user can publish doc, make state clickable to change it -->
        <xsl:choose>
          <xsl:when test="$canPublish">
            <xsl:variable name="newState">
              <xsl:choose>
                <xsl:when test="@state = 'draft'">publish</xsl:when>
                <xsl:when test="@state = 'publish'">draft</xsl:when>
              </xsl:choose>
            </xsl:variable>

            <xsl:call-template name="generatePostLink">
              <xsl:with-param name="action" select="concat($documentPath, '/version/', @id, '?newState=', $newState, $variantParams)"/>
              <xsl:with-param name="label"><i18n:text key="{@state}"/></xsl:with-param>
              <xsl:with-param name="id" select="concat(generate-id(), '-state')"/>
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <i18n:text key="{@state}"/>
          </xsl:otherwise>
        </xsl:choose>
      </td>
      <td class="dsy-nowrap">
            <xsl:variable name="newChangeType">
              <xsl:choose>
                <xsl:when test="@changeType = 'major'">minor</xsl:when>
                <xsl:when test="@changeType = 'minor'">major</xsl:when>
              </xsl:choose>
            </xsl:variable>
            <xsl:choose>
              <xsl:when test="$canChangeMetadata">
                <xsl:call-template name="generatePostLink">
                  <xsl:with-param name="action" select="concat($documentPath, '/version/', @id, '?newChangeType=', $newChangeType, $variantParams)"/>
                  <xsl:with-param name="label"><i18n:text key="{@changeType}"/></xsl:with-param>
                  <xsl:with-param name="id" select="concat(generate-id(), '-change')"/>
                </xsl:call-template>        
              </xsl:when>
              <xsl:otherwise><i18n:text key="{@changeType}"/></xsl:otherwise>
            </xsl:choose>
      </td>
      <td class="dsy-nowrap">
        <xsl:if test="@syncedWithLanguageId">
          <a href="{$documentPath}/version/{@syncedWithVersionId}?branch={$document/@branch}&amp;language={@syncedWithLanguageName}">
            <xsl:value-of select="@syncedWithLanguageName"/><xsl:text>, </xsl:text><i18n:text key="versions.version-abbreviated"/><xsl:value-of select="@syncedWithVersionId"/>
          </a>
        </xsl:if>
      </td>
      <xsl:if test="$isEditor or $canPublish">
        <td class="dsy-nowrap">
          <xsl:if test="$canChangeMetadata or $canPublish">
            <xsl:choose>
              <xsl:when test="@syncedWithLanguageId">
                <a href="javascript:void(0)" onclick="showVersionDialog({@id}, '{@state}', {@syncedWithLanguageId}, {@syncedWithVersionId}, '{@changeType}', '{d:changeComment/text()}', {boolean(@live)});"><i18n:text key="versions.edit"/></a>
              </xsl:when>
              <xsl:otherwise>
                <a href="javascript:void(0)" onclick="showVersionDialog({@id}, '{@state}', -1, -1, '{@changeType}', '{d:changeComment/text()}', {boolean(@live)});"><i18n:text key="versions.edit"/></a>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:if>
          <xsl:if test="$isEditor and @id != $lastVersionId">
            <xsl:if test="$canChangeMetadata or $canPublish"> | </xsl:if>
            <xsl:call-template name="generatePostLink">
              <xsl:with-param name="action" select="concat($documentPath, '/edit?versionId=', @id, $variantParams)"/>
              <xsl:with-param name="label"><i18n:text key="versions.revert"/></xsl:with-param>
              <xsl:with-param name="id" select="concat(generate-id(), '-revert')"/>
              <xsl:with-param name="confirmMessage"><i18n:text key="versions.revert-info"/></xsl:with-param>
            </xsl:call-template>
          </xsl:if>
        </td>
      </xsl:if>
      <td class="dsy-nowrap"><xsl:choose>
        <xsl:when test="@id = $lastVersionId and $lastVersionId != 1">
          <input style="visibility:hidden" type="radio" name="compareLeft" value="{@id}" onclick="leftVersionChanged(this.value);"/>
          <input type="radio" name="compareRight" value="{@id}" checked="checked" onclick="rightVersionChanged(this.value);"/>
          <img class="dsy-version-action" style="visibility:hidden" src="{$mountPoint}/resources/skins/{$skin}/images/version-diff-next.gif"/>
          <a href="{$documentPath}/version/{@id - 1}/diff?otherVersion={@id}{$variantParams}">
            <img title="versions.diff-to-previous" i18n:attr="title" src="{$mountPoint}/resources/skins/{$skin}/images/version-diff-previous.gif"/>
          </a>
        </xsl:when>
        <xsl:when test="@id > 1 and @id &lt; $lastVersionId">
          <xsl:choose>
            <xsl:when test="@id = $lastVersionId - 1">
              <input type="radio" name="compareLeft" value="{@id}" checked="checked" onclick="leftVersionChanged(this.value);"/>
            </xsl:when>
            <xsl:otherwise>
              <input type="radio" name="compareLeft" value="{@id}" onclick="leftVersionChanged(this.value);"/>
            </xsl:otherwise>
          </xsl:choose>
          <input style="visibility:hidden" type="radio" name="compareRight" value="{@id}" onclick="rightVersionChanged(this.value);"/>
          <a href="{$documentPath}/version/{@id}/diff?otherVersion={@id + 1}{$variantParams}">
            <img class="dsy-version-action" title="versions.diff-to-next" i18n:attr="title" src="{$mountPoint}/resources/skins/{$skin}/images/version-diff-next.gif"/>
          </a>
          <a href="{$documentPath}/version/{@id - 1}/diff?otherVersion={@id}{$variantParams}">
            <img class="dsy-version-action" title="versions.diff-to-previous" i18n:attr="title" src="{$mountPoint}/resources/skins/{$skin}/images/version-diff-previous.gif"/>
          </a>
        </xsl:when>
        <xsl:when test="@id = 1 and $lastVersionId > 1">
          <xsl:choose>
            <xsl:when test="@id = $lastVersionId - 1">
              <input type="radio" name="compareLeft" value="{@id}" checked="checked" onclick="leftVersionChanged(this.value);"/>
            </xsl:when>
            <xsl:otherwise>
              <input type="radio" name="compareLeft" value="{@id}" onclick="leftVersionChanged(this.value);"/>
            </xsl:otherwise>
          </xsl:choose>
          <input style="visibility:hidden" type="radio" name="compareRight" value="{@id}" onclick="rightVersionChanged(this.value);"/>
          <a href="{$documentPath}/version/{@id}/diff?otherVersion={@id + 1}{$variantParams}">
            <img class="dsy-version-action" title="versions.diff-to-next" i18n:attr="title" src="{$mountPoint}/resources/skins/{$skin}/images/version-diff-next.gif"/>
          </a>
          <img class="dsy-version-action" style="visibility:hidden" src="{$mountPoint}/resources/skins/{$skin}/images/version-diff-previous.gif"/>
        </xsl:when>
      </xsl:choose></td>
      <xsl:choose>
        <xsl:when test="string-length(d:changeComment) > 0">
          <td class="dsy-change-comment dsy-change-comment-collapsed" onclick="if (dojo.html.selection.getSelectedText()) return; (dojo.html.hasClass(this, 'dsy-change-comment-collapsed')?dojo.html.removeClass:dojo.html.addClass)(this, 'dsy-change-comment-collapsed')">
            <div><xsl:value-of select="d:changeComment"/></div>
          </td>
        </xsl:when>
        <xsl:otherwise>
          <td></td>
        </xsl:otherwise>
      </xsl:choose>
    </tr>
  </xsl:template>
  
  <xsl:template name="availableVariants">
    <availableVariants>
      <variants>
        <xsl:for-each select="$pubdoc/d:availableVariants/d:availableVariant">
          <xsl:sort select="@branchName"/>
          <xsl:sort select="@languageName"/>
          <!-- exclude retired versions and show unpublished variants only to editors -->
          <xsl:if test="@retired = 'false'">
            <xsl:if test="@liveVersionId != '-1' or ($isEditor and @liveVersionId = '-1')">
              <variant href="{concat($mountPoint, '/', $siteName, '/', $documentId, '/versions.html?branch=', @branchName, '&amp;language=', @languageName)}"
                branchName="{@branchName}" languageName="{@languageName}">
                <xsl:if test="@branchId = $document/@branchId and @languageId = $document/@languageId">
                  <xsl:attribute name="current">true</xsl:attribute>
                </xsl:if>
              </variant>
            </xsl:if>
          </xsl:if>
        </xsl:for-each>
      </variants>
      <xsl:if test="$isEditor">
        <createVariant href="{concat($documentPath, '/createVariant', $variantQueryString)}"/>
      </xsl:if>
    </availableVariants>
  </xsl:template>

</xsl:stylesheet>