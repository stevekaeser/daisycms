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
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:d="http://outerx.org/daisy/1.0"
  xmlns:p="http://outerx.org/daisy/1.0#publisher"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  version="1.0">

  <!-- put some often needed stuff in variables for easy access (and performance) -->
  <xsl:variable name="context" select="/feedData/context"/>
  <xsl:variable name="server" select="string($context/request/@server)"/>
  <xsl:variable name="mountPoint" select="string($context/mountPoint)"/>
  <xsl:variable name="siteName" select="string($context/site/@name)"/>

  <xsl:template match="feedData">
    <rss version="2.0">
      <channel>
        <title><xsl:value-of select="$context/site/@title"/></title>
        <link><xsl:value-of select="concat($server, $mountPoint, '/', $siteName)"/></link>
        <description><xsl:value-of select="$context/site/@description"/></description>
        <xsl:apply-templates select="p:publisherResponse/d:searchResult"/>
      </channel>
    </rss>
  </xsl:template>

  <xsl:template match="d:searchResult">
    <xsl:for-each select="d:rows/d:row">
      <item>
        <title><xsl:value-of select="d:value[2]"/></title>
        <link><xsl:value-of select="concat($server, $mountPoint, '/', $siteName, '/', @documentId)"/></link>
        <description><xsl:value-of select="d:value[2]"/></description>
      </item>
    </xsl:for-each>
  </xsl:template>

</xsl:stylesheet>
