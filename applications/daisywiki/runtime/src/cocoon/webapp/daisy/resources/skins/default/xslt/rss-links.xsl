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

  <xsl:template match="page">

    <xsl:variable name="feeds" select="context/skinconf/rss-feeds"/>
    <xsl:variable name="mountPoint" select="context/mountPoint"/>
    <xsl:variable name="skin" select="context/skin"/>

    <xsl:if test="$feeds/rss-feed">
      <ul class="rss-feeds">
        <xsl:for-each select="$feeds/rss-feed">
          <li>
            <a href="{path}">
              <img src="{$mountPoint}/resources/skins/{$skin}/images/feed-icon-16x16.png"/>
              <xsl:text> </xsl:text>
              <xsl:copy-of select="label/node()"/>
            </a>
          </li>
        </xsl:for-each>
      </ul>

      <!-- extraHeadContent: see layout.xsl input specification -->
      <extraHeadContent>
        <xsl:for-each select="$feeds/rss-feed">

          <!-- Try to support i18n in labels to some extent... -->
          <xsl:choose>
            <xsl:when test="label/i18n:text">
              <link rel="alternate" type="{type}" title="{label/i18n:text/@catalogue}:{label/i18n:text/@key}" i18n:attr="title" href="{path}"/>
            </xsl:when>
            <xsl:otherwise>
              <link rel="alternate" type="{type}" title="{label}" href="{path}"/>
            </xsl:otherwise>
          </xsl:choose>

        </xsl:for-each>
      </extraHeadContent>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>