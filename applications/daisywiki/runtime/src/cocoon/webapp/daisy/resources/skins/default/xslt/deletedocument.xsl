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

  <xsl:import href="daisyskin:xslt/searchresult.xsl"/>
  <xsl:variable name="document" select="/page/d:document"/>
  <xsl:variable name="documentPath"><xsl:value-of select="concat(/page/context/mountPoint, '/', /page/context/site/@name, '/', /page/navigationPath, '.html')"/></xsl:variable>
  <xsl:variable name="variantParams" select="page/variantParams"/>
  <xsl:variable name="variantQueryString" select="page/variantQueryString"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><i18n:text key="deletedocument.headtitle"/></pageTitle>
      <xsl:call-template name="pageNavigation"/>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="pageNavigation">
    <pageNavigation>
      <link>
        <title><i18n:text key="back-to-document"/></title>
        <path><xsl:value-of select="concat($documentPath, $variantQueryString)"/></path>
      </link>
    </pageNavigation>
  </xsl:template>

  <xsl:template name="content">
    <h1>
      <i18n:translate>
        <i18n:text key="deletedocument.title"/>
        <i18n:param><xsl:value-of select="$document/@name"/></i18n:param>
        <i18n:param><xsl:value-of select="$document/@id"/></i18n:param>
      </i18n:translate>
    </h1>

    <i18n:text key="deletedocument.choose-action"/>
    <ul>
      <xsl:if test="d:aclResult/d:permissions/d:permission[@type='write' and @action='grant']/d:accessDetails/d:permission[@type='retired' and @action='grant']">
        <li style="margin-bottom: 10px">
          <xsl:choose>
            <xsl:when test="$document/@retired = 'true'">
              <i18n:text key="deletedocument.document-variant-retired"/>
            </xsl:when>
            <xsl:otherwise>
              <form action="{$documentPath}?action=delete&amp;type=retire{$variantParams}" method="POST" style="margin: 0px">
                <input type="submit" value="deletedocument.retire-document-variant" i18n:attr="value"/>
              </form>
              <i18n:text key="deletedocument.retire-document-variant-info"/>
            </xsl:otherwise>
          </xsl:choose>
        </li>
      </xsl:if>
      <xsl:if test="d:aclResult/d:permissions/d:permission[@type='delete']/@action = 'grant'">
        <li style="margin-bottom: 10px">
          <button onclick="if (confirm(daisyElementText(document.getElementById('variant-delete-warning')))) document.getElementById('variant-delete-form').submit();"><i18n:text key="deletedocument.delete-variant-permanently"/></button>
          <form action="{$documentPath}?action=delete&amp;type=variant-permanent{$variantParams}" method="POST" style="margin: 0px" id="variant-delete-form">
          </form>
          <div id="variant-delete-warning"><i18n:text key="deletedocument.delete-variant-permanently-info"/></div>
        </li>
        <li>
          <button onclick="if (confirm(daisyElementText(document.getElementById('document-delete-warning')))) document.getElementById('document-delete-form').submit();"><i18n:text key="deletedocument.delete-permanently"/></button>
          <form action="{$documentPath}?action=delete&amp;type=permanent{$variantParams}" method="POST" style="margin: 0px" id="document-delete-form">
          </form>
          <div id="document-delete-warning"><i18n:text key="deletedocument.delete-permanently-info"/></div>
        </li>
      </xsl:if>
    </ul>

    <p style="font-size: x-small"><i18n:text key="deletedocument.retire-note"/></p>

    <xsl:if test="live/d:searchResult/d:rows/* or last/d:searchResult/d:rows/*">
      <h2><i18n:text key="deletedocument.links-title"/></h2>
      
      <xsl:if test="live/d:searchResult/d:rows/*">
        <p><i18n:text key="deletedocument.links-info-live"/></p>
        <xsl:apply-templates select="live/d:searchResult"/>
      </xsl:if>
      
      <xsl:if test="last/d:searchResult/d:rows/*">
        <p><i18n:text key="deletedocument.links-info-last"/></p>
        <xsl:apply-templates select="last/d:searchResult"/>
      </xsl:if>
      
      <p><i18n:text key="deletedocument.links-consider-updating"/></p>
    </xsl:if>

  </xsl:template>


</xsl:stylesheet>