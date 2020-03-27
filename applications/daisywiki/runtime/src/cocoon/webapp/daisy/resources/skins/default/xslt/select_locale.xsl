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
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:import href="daisyskin:xslt/util.xsl"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><i18n:text key="locale.title"/></pageTitle>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1><i18n:text key="locale.title"/></h1>

    <i18n:text key="locale.select"/>

    <xsl:variable name="submitPath" select="string(submitPath)"/>
    <ul>
      <xsl:for-each select="locales/locale">
        <li>
          <xsl:call-template name="generatePostLink">
            <xsl:with-param name="action" select="concat($submitPath, '&amp;locale=', @name)"/>
            <xsl:with-param name="label" select="string(@displayName)"/>
            <xsl:with-param name="id" select="generate-id()"/>
          </xsl:call-template>
        </li>
      </xsl:for-each>
    </ul>
  </xsl:template>

</xsl:stylesheet>