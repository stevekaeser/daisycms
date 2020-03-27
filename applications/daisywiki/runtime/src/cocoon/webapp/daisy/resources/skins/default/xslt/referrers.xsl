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
  <xsl:import href="daisyskin:xslt/searchresult.xsl"/>

  <xsl:variable name="pubdoc" select="/page/p:publisherResponse/p:document"/>
  <xsl:variable name="documentName" select="$pubdoc/d:document/@name"/>
  <xsl:variable name="linksInLastVersion" select="/page/referrersOptions/@linksInLastVersion"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><xsl:call-template name="pageTitle"/></pageTitle>
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
        <title><i18n:text key="back-to-document"/></title>
        <path><xsl:value-of select="concat($documentPath, '.html', $variantQueryString)"/></path>
      </link>
      <link>
        <title><i18n:text key="hide-navigation"/></title>
        <path><xsl:value-of select="concat($documentPath, '/referrers.html?linksInLastVersion=', $linksInLastVersion, '&amp;layoutType=plain', $variantParams)"/></path>
      </link>
    </pageNavigation>
  </xsl:template>

  <xsl:template name="pageTitle">
    <i18n:text key="referrers.referrers-for"/>
    <xsl:text> </xsl:text><xsl:value-of select="$documentName"/>
  </xsl:template>

  <xsl:template name="content">
    <h1><xsl:value-of select="$documentName"/></h1>
    <h2><i18n:text key="referrers.title"/></h2>

    <form action="" method="GET">
      <input type="hidden" name="branch" value="{$pubdoc/d:document/@branch}"/>
      <input type="hidden" name="language" value="{$pubdoc/d:document/@language}"/>
      <p>
        <i18n:translate>
          <i18n:text key="referrers.infoline"/>
          <i18n:param>
            <input name="linksInLastVersion" id="linksInLiveVersion" type="radio" value="false">
              <xsl:if test="$linksInLastVersion != 'true'">
                <xsl:attribute name="checked"/>
              </xsl:if>
            </input>
            <label for="linksInLiveVersion"><i18n:text>referrers.live</i18n:text></label>
          </i18n:param>
          <i18n:param>
            <input name="linksInLastVersion" id="linksInLastVersion" type="radio" value="true">
              <xsl:if test="$linksInLastVersion = 'true'">
                <xsl:attribute name="checked"/>
              </xsl:if>
            </input>
            <label for="linksInLastVersion"><i18n:text>referrers.last</i18n:text></label>
          </i18n:param>
        </i18n:translate>
      <input type="submit" value="referrers.show-button" i18n:attr="value"/>
      </p>
    </form>

    <xsl:variable name="searchResult" select="p:publisherResponse/p:group[@id='referrers']/d:searchResult"/>
    <xsl:choose>
      <xsl:when test="$searchResult/d:rows/*">
        <xsl:apply-templates select="$searchResult"/>
      </xsl:when>
      <xsl:otherwise>
        <strong><i18n:text key="referrers.no-referring-docs"/></strong>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>

</xsl:stylesheet>