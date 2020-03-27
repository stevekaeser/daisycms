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

  <xsl:variable name="mountPoint" select="string(page/context/mountPoint)"/>
  <xsl:variable name="siteName" select="string(page/context/site/@name)"/>
  <xsl:variable name="siteBranchId" select="string(page/context/site/@branchId)"/>
  <xsl:variable name="siteLanguageId" select="string(page/context/site/@languageId)"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><i18n:text key="mycomments.title"/></pageTitle>
      <xsl:copy-of select="p:publisherResponse/n:navigationTree"/>
      <xsl:call-template name="pageNavigation"/>
      <navigationInfo>
        <xsl:copy-of select="p:publisherResponse/p:group[@id = 'navigationInfo']/p:document/*"/>
      </navigationInfo>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="pageNavigation">
    <pageNavigation>
      <link>
        <title><i18n:text key="hide-navigation"/></title>
        <path><xsl:value-of select="concat($mountPoint, '/', $siteName, '/myComments?layoutType=plain')"/></path>
      </link>
    </pageNavigation>
  </xsl:template>


  <xsl:template name="content">
    <h1><i18n:text key="mycomments.title"/></h1>

    <p><i18n:text key="mycomments.intro"/></p>

    <xsl:apply-templates select="p:publisherResponse/d:comments/d:comment"/>

  </xsl:template>

  <xsl:template match="d:comment">
    <div class="comment privateComment">
      <table class="plainTable" width="100%">
        <tr>
          <td>
            <div class="commentheader">
              <i18n:text key="mycomments.createdon"/>: <xsl:value-of select="@createdOnFormatted"/>
              <br/>
              <xsl:variable name="docHref">
                <xsl:value-of select="concat($mountPoint, '/', $siteName, '/', @documentId, '.html')"/>
                <xsl:if test="@branchId != $siteBranchId or @languageId != $siteLanguageId">
                  <xsl:value-of select="concat('?branch=', @branch, '&amp;language=', @language)"/>
                </xsl:if>
              </xsl:variable>
              <i18n:text key="mycomments.document"/>: <a href="{$docHref}"><xsl:value-of select="@documentName"/></a>
            </div>
          </td>
          <td align="right" valign="top">
            <span class="commentActions">
              <form style="display:none" id="comment_{@documentId}_{@id}" action="?action=deleteComment&amp;commentId={@id}&amp;documentId={@documentId}&amp;branch={@branch}&amp;language={@language}" method="POST"/>
              <a href="#" onclick="document.getElementById('comment_{@documentId}_{@id}').submit(); return false;" onmouseover="status='';return true;"><i18n:text key="mycomments.delete"/></a>
            </span>
          </td>
        </tr>
      </table>

      <div class="commentbody">
        <xsl:copy-of select="./node()"/>
      </div>
    </div>

  </xsl:template>

</xsl:stylesheet>