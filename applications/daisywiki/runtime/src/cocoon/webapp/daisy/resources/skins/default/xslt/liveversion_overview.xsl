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
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:urlencoder="xalan://java.net.URLEncoder">
  
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
  <xsl:variable name="pubAccess" select="$pubdoc/d:aclResult/d:permissions/d:permission[@type='publish' and @action='grant']"/>
  <xsl:variable name="canUpdateTimeline" select="$pubAccess and not(boolean($pubAccess/d:accessDetails/d:permission[@type='live_history' and @action='deny']))"/>
  <xsl:variable name="readOnly" select="/page/readOnly='true'"/>
  
  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><xsl:call-template name="pageTitle"/></pageTitle>
      <xsl:call-template name="pageNavigation"/>
      <navigationInfo>
        <xsl:copy-of select="p:publisherResponse/p:group[@id = 'navigationInfo']/p:document/*"/>
      </navigationInfo>
      <extraHeadContent>
        <link rel="Stylesheet" href="{$mountPoint}/resources/skins/{$skin}/css/doceditor.css" type="text/css"/>
        <script src="{$mountPoint}/resources/js/jquery/jquery.livehistory.js"/>
      </extraHeadContent>
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
        <title><i18n:text key="doclayout.versions"/></title>
        <path><xsl:value-of select="concat($documentPath, '/versions.html', $variantQueryString)"/></path>
      </link>
      <link>
        <title><i18n:text key="hide-navigation"/></title>
        <path><xsl:value-of select="concat($documentPath, '/live-versions.html?layoutType=plain', $variantParams)"/></path>
      </link>
    </pageNavigation>
  </xsl:template>

  <xsl:template name="pageTitle">
    <i18n:text key="liveversions.for"/><xsl:text> </xsl:text><xsl:value-of select="$documentName"/>
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
        <li class="ui-state-default ui-corner-top ui-tabs-selected">
          <a href="{$documentPath}/versions.html{$variantQueryString}"><span><i18n:text key="versions.overview"/></span></a>
        </li>
        <li class="ui-state-default ui-corner-top ui-tabs-selected ui-state-active">
          <a href="{$documentPath}/live-versions.html{$variantQueryString}"><span><i18n:text key="liveversions.history"/></span></a>
        </li>
      </ul>
    </span>

    <xsl:apply-templates select="$pubdoc/d:document/d:timeline"/>

  </xsl:template>
  
  <xsl:template match="d:timeline">

    <xsl:if test="/page/warnLockUserName">
      <i18n:translate>
        <i18n:text key="versions.lock.info-warn"/>
        <i18n:param><xsl:value-of select="/page/warnLockUserName"/></i18n:param>
      </i18n:translate>
      <xsl:if test="$document/d:lockInfo/@type = 'warn'">
        <xsl:call-template name="generatePostLink">
          <xsl:with-param name="action" select="concat($documentPath, '/version/', /page/attemptedMakeLiveVersionId, '?forceMakeLive=true')"/>
          <xsl:with-param name="label"><i18n:text key="versions.lock.ignore"/></xsl:with-param>
          <xsl:with-param name="id" select="concat(generate-id(), '-makeLive')"/>
        </xsl:call-template>
      </xsl:if>
    </xsl:if>
      
    <table class="versions default">
      <thead>
        <tr>
          <th style="min-width: 200px"><i18n:text key="versions.time"/></th>
          <th style="minwidth: 250px"><i18n:text key="version"/></th>
          <xsl:if test="not($readOnly)">
            <th style="min-width: 100px"><i18n:text key="versions.actions"/></th>
          </xsl:if>
        </tr>
      </thead>
      <tbody id="livehistorylist"/>
    </table>
    
    <div id="emptyHistory" style="display:none">
      <i18n:text key="liveversions.emptyHistory"/>
    </div>

    <div><!--  FIXME: don't render at all when readonly? -->
      <xsl:if test="$readOnly"><xsl:attribute name="style">display:none</xsl:attribute></xsl:if>
      <form id="cancelForm" action="" method="POST" name="cancelEditing">
        <input type="hidden" name="cancelEditing" value="true"/>
      </form>
      <form id="saveForm" method="POST" action="{/page/editPath}">
        <input type="hidden" id="liveversions-postdata" name="liveversions-postdata"/>
        <input type="hidden" name="action" value="updateLiveHistory"/>
      </form>
      <br/>
      <a href="javascript:void" onclick="$('#cancelForm').submit()" class="button"><i18n:text key="editdoc.cancel"/></a>&#160;
      <a href="javascript:void" onclick="$('#saveForm').submit()" class="button"><i18n:text key="editdoc.save"/></a>
    </div>
    
    <xsl:if test="$readOnly and $canUpdateTimeline">
      <div>
        <form id="toeditor" method="POST" action="{$documentPath}/live-history-edit{$variantQueryString}">
          <input type="submit" value="doclayout.edit" i18n:attr="value"/>
        </form>
      </div>
    </xsl:if>
    
    <script>
      <xsl:if test="not($readOnly)">
        window.onbeforeunload = function() {
          if (window.needsConfirmForLeaving) {
            window.needsConfirmForLeaving = true;
            return "<i18n:text key="liveversions.confirm-leave-editor"/> " + "<xsl:value-of select="translate($document/@name, '&quot;','\&quot;')"/>";
          }
        }
        window.needsConfirmForLeaving = true;
      </xsl:if>
     
      $(function() {
        $("#livehistorylist").livehistory({ 'initialData': [
          <xsl:for-each select="d:liveHistoryEntry">
            <xsl:if test="position() > 1">,</xsl:if>
            {'id': <xsl:value-of select="@id"/>,
             'beginDate': '<xsl:value-of select="@localBeginDate"/>',
             'endDate': '<xsl:value-of select="@localEndDate"/>',
             'versionId': '<xsl:value-of select="@versionId"/>'}
          </xsl:for-each>
          ],
          liveVersionId: <xsl:if test="not(../@liveVersionId)">-1</xsl:if><xsl:value-of select="../@liveVersionId"/>,
          lastVersionId: <xsl:if test="not(../@lastVersionId)">-1</xsl:if><xsl:value-of select="../@lastVersionId"/>,
          readOnly: <xsl:value-of select="$readOnly"/>,
          documentPath: '<xsl:value-of select="$documentPath"/>',
          emptyHistoryEl: '#emptyHistory'
        });
        $("#cancelForm").submit(function() {
          window.needsConfirmForLeaving = false;
        });
        $("#saveForm").submit(function() {
          window.needsConfirmForLeaving = false;
          $("#liveversions-postdata").val($("#livehistorylist").livehistory('getXmlDiff'));
        });
      });
    </script>
    
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
              <variant href="{concat($mountPoint, '/', $siteName, '/', $documentId, '/live-versions.html?branch=', @branchName, '&amp;language=', @languageName)}"
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
