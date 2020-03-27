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
  xmlns:t="http://outerx.org/daisy/1.0#doctaskrunner"
  xmlns:a="http://outerx.org/daisy/1.0#documentActions"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:urlencoder="xalan://java.net.URLEncoder">

  <xsl:import href="daisyskin:xslt/util.xsl"/>

  <xsl:variable name="context" select="page/context"/>
  <xsl:variable name="mountPoint" select="string($context/mountPoint)"/>
  <xsl:variable name="skin" select="string($context/skin)"/>
  <xsl:variable name="basePath" select="concat($mountPoint,'/',$context/site/@name)"/>
  <xsl:variable name="continuationId" select="string(/page/continuationId)"/>
  <xsl:variable name="selectionType" select="string(/page/selection/@type)"/>
  <xsl:variable name="documentId" select="string(/page/selection/@documentId)"/>
  <xsl:variable name="branch" select="string(/page/selection/@branch)"/>
  <xsl:variable name="language" select="string(/page/selection/@language)"/>
  <xsl:variable name="query" select="string(/page/selection/@query)"/>
  <xsl:variable name="needle" select="urlencoder:encode(string(/page/forminput/needle))"/>
  <xsl:variable name="replacement" select="urlencoder:encode(string(/page/forminput/replacement))"/>
  <xsl:variable name="caseSensitive" select="string(/page/forminput/caseSensitive)"/>
  <xsl:variable name="regexp" select="string(/page/forminput/regexp)"/>
  
  <xsl:variable name="resources-uri" select="concat($mountPoint,'/resources/cocoon')"/>
  <xsl:variable name="daisy-resources-uri" select="concat($mountPoint, '/resources')"/>
  
  <xsl:key name="docdetails" match="searchtaskoutput/details" use="@variantKey"/>

  <xsl:template match="page">
    <page>
      <pageTitle><i18n:text key="serp.title"/></pageTitle>
      <xsl:copy-of select="context"/>
      <layoutHints needsDojo="true">
        <xsl:if test="/page/searchtaskoutput/details/a:searchActionResult/a:matches/*">
          <xsl:attribute name="wideLayout">true</xsl:attribute>
        </xsl:if>
      </layoutHints>
      <content><xsl:call-template name="content"/></content>
      <extraHeadContent>
        <script type="text/javascript">
          dojo.registerModulePath("cocoon.forms", "../forms/js");
          dojo.require("cocoon.forms.common");
          dojo.addOnLoad(cocoon.forms.callOnLoadHandlers);
        </script>
        <script type="text/javascript">
          var cocoon;
          if (!cocoon)
            cocoon = {};
          cocoon.resourcesUri = "<xsl:value-of select="$resources-uri"/>";
        </script>
        <link href="{$resources-uri}/forms/css/forms.css" type="text/css" rel="stylesheet"/>
      </extraHeadContent>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <xsl:if test="/page/debug">
      <div class="dsy-debug">
        <xsl:copy-of select="/page/debug/node()"/>
      </div>
    </xsl:if>
    
    <script type="text/javascript">
      function showDetails(div) {
        var popup = window.open("", "",
                "toolbar=no,menubar=no,personalbar=no,width=800,height=500,left=20,top=40" +
                ",scrollbars=yes,resizable=yes");

        var doc = popup.document;
        doc.open();
        doc.write("&lt;html>&lt;head>&lt;title>Document Execution Details&lt;/title>&lt;body>&lt;pre>");
        doc.write(div.innerHTML);
        doc.write("&lt;/pre>&lt;/body>&lt;/html>");
        doc.close();
      }
    </script>
    
    <h1><i18n:text key="serp.search.results.title"/></h1>

    <xsl:comment>
      Task id:<xsl:value-of select="searchtask/t:task/@id"/>
      Task description:<xsl:value-of select="searchtask/t:task/@description"/>
      Task state:<xsl:value-of select="searchtask/t:task/@state"/>
    </xsl:comment>

    <xsl:apply-templates select="searchtask/t:taskDocDetails"/>
  </xsl:template>

  <xsl:template match="t:taskDocDetails">
    <xsl:choose>
      <xsl:when test="/page/searchtaskoutput/details/a:searchActionResult/a:matches/*">
        <div style="float:left;">
          <a href="search"><i18n:text key="serp.go-back"/></a>
        </div>
        <form action="{$basePath}/searchAndReplace/{$continuationId}/replace" method="POST">
          <div style="float:right;">
            <input type="submit" name="startReplaceTask" value="serp.replace" i18n:attr="value"/>
          </div>
          <div style="clear:both"/>
          
          <table class="default serp-searchresults">
            <tr>
              <th><i18n:text key="serp.searchresults.doc-id"/></th>
              <th><i18n:text key="branch"/></th>
              <th><i18n:text key="language"/></th>
              <th width="100%"><i18n:text key="serp.searchresults"/></th>
              <th colspan="4"><i18n:text key="serp.action"/></th>
            </tr>
            <xsl:apply-templates select="t:taskDocDetail"/>
          </table>
          
          <div style="float:right; clear:both">
            <input type="submit" name="startReplaceTask" value="serp.replace" i18n:attr="value"/>
          </div>
        </form>
        <form method="POST" id="documentEditorForm" action="" target="_blank">
        </form>
        <script language="javascript">
          function openDocumentEditor(documentId, branch, language) {
            var url = "<xsl:value-of select="$basePath"/>/" + documentId + "/edit?branch=" + encodeURI(branch) + '&amp;language=' + encodeURI(language);
            var form = document.getElementById('documentEditorForm');
            form.action=url;
            form.submit();
          }
        </script>
      </xsl:when>
      <xsl:otherwise>
        <p>
          <i18n:text key="serp.searchresults.empty"/>
        </p>
        <ul>
          <li><xsl:call-template name="restart"/></li>
          <li><a href="{$basePath}"><i18n:text key="serp.searchresults.home"/></a></li>
        </ul>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="t:taskDocDetail">
    <xsl:variable name="id" select="generate-id(.)"/>
    <xsl:variable name="variantKey" select="concat(@documentId,'@',@branchId,':',@languageId)"/>
    <xsl:variable name="result" select="key('docdetails', $variantKey)/a:searchActionResult"/>
    <xsl:variable name="matches" select="$result/a:matches"/>
    
    <xsl:choose>
      <xsl:when test="$matches/*">
        <tr>
          <td class="dsyfrm-labelcell"><a href="{concat($basePath, '/', urlencoder:encode(@documentId), '?branch=', urlencoder:encode(@branch), '&amp;language=', urlencoder:encode(@language))}"><xsl:value-of select="@documentId"/></a></td>
          <td class="dsyfrm-labelcell"><xsl:value-of select="@branch"/></td>
          <td class="dsyfrm-labelcell"><xsl:value-of select="@language"/></td>
          <td>
            <xsl:choose>
              <xsl:when test="@state!='done'">
                <div id="{$id}" class="dsy-search-docdetails-error" style="display: none">
                  <i18n:text key="serp.searchresults.error"/> (<a href="#" onmouseover="status=''; return true;" onclick="showDetails(document.getElementById('{$id}')); return false;"><i18n:text key="serp.searchresults.show-details"/></a>)
                  <xsl:copy-of select="t:details/node()"/>
                </div>
              </xsl:when>
              <xsl:otherwise>
                <xsl:apply-templates select="$matches/*"/>
              </xsl:otherwise>
            </xsl:choose>
          </td>
          <td style="white-space:nowrap">
            <input type="radio" name="select-{$variantKey}" id="select-{$variantKey}" value="on">
              <xsl:if test="/page/selecteddocuments/document[@key=$variantKey]">
                <xsl:attribute name="checked">true</xsl:attribute>
              </xsl:if>
            </input>
            <label for="select-{$variantKey}"><i18n:text key="serp.replace"/></label>
            <input type="radio" name="select-{$variantKey}" id="skip-{$variantKey}" value="off">
              <xsl:if test="not(/page/selecteddocuments/document[@key=$variantKey])">
                <xsl:attribute name="checked">true</xsl:attribute>
              </xsl:if>
            </input>
            <label for="skip-{$variantKey}"><i18n:text key="serp.skip"/></label>
          </td>
          <td>
            <xsl:apply-templates select="$result/d:lockInfo"/>
            <xsl:apply-templates select="$result/d:aclResult"/>
          </td>
          <td style="white-space:nowrap">
            <a href="#" onclick="javascript: openDocumentEditor('{@documentId}', '{@branch}', '{@language}')"
              onmouseover="status=''; return true;"><i18n:text key="serp.edit"/></a>
          </td>
        </tr>
      </xsl:when>
      <xsl:otherwise>
        <!-- document omitted because it does not have matches -->
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template match="d:lockInfo">
    <xsl:if test="./@hasLock = 'true'">
      <span dojoType="forms:infopopup" style="display:none" class="forms-help-popup" icon="../../../../../../..{$daisy-resources-uri}/skins/{$skin}/images/lock.gif">
        <i18n:translate>
          <i18n:text key="serp.searchresults.locked"/>
          <i18n:param><xsl:value-of select="@userId"/></i18n:param> 
        </i18n:translate>
      </span>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="d:aclResult">
    <xsl:if test="d:permissions/d:permission[@type='write' and @action='deny']">
      <span dojoType="forms:infopopup" style="display:none" class="forms-help-popup" icon="../../../../../../..{$daisy-resources-uri}/skins/{$skin}/images/lock.gif">
        <p><i18n:text key="serp.searchresults.no-write-access"/></p>
        <ul class="info">
          <li><xsl:value-of select="d:permissions/d:permission[@type='write']/@objectReason"/></li>
          <ul>
            <li><xsl:value-of select="d:permissions/d:permission[@type='write']/@subjectReason"/></li>
          </ul>
        </ul>
      </span>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="a:match">
    <div class="dsy-regexp-match">
      <xsl:copy-of select="./a:fragment/html/body/node()"/>
    </div>
  </xsl:template>
  
  <xsl:template name="restart">
    <a href="search"><i18n:text key="serp.searchresults.restart"/></a>
  </xsl:template>

</xsl:stylesheet>