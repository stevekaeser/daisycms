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
  xmlns:d="http://outerx.org/daisy/1.0"
  xmlns:einclude="http://outerx.org/daisy/1.0#externalinclude"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:template match="d:searchResult">
    <table class="plainTable">
      <tr>
        <xsl:for-each select="d:titles/d:title">
          <th><xsl:value-of select="."/></th>
        </xsl:for-each>
      </tr>
      <xsl:apply-templates select="d:rows/d:row"/>
    </table>
  </xsl:template>

  <xsl:template match="d:row">
    <tr>
      <xsl:for-each select="d:value|d:xmlValue|d:multiValue|d:hierarchyValue">
        <td>
          <xsl:apply-templates select="."/>
        </td>
      </xsl:for-each>
    </tr>
  </xsl:template>

  <xsl:template match="d:value">
    <xsl:value-of select="."/>
  </xsl:template>

  <xsl:template match="d:multiValue">
    <xsl:for-each select="*">
      <xsl:if test="position() > 1">, </xsl:if>
      <xsl:apply-templates select="."/>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="d:hierarchyValue">
    <xsl:for-each select="*">
      <xsl:if test="position() > 1"> / </xsl:if>
      <xsl:apply-templates select="."/>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="d:xmlValue">
    <xsl:apply-templates select="html/body/node()"/>
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>