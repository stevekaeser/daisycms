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
<!--
  Note: this stylesheet is written so that it supports inputs with multiple body elements,
  as is the case for chunked output.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:d="http://outerx.org/daisy/1.0"
  xmlns:einclude="http://outerx.org/daisy/1.0#externalinclude"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:template match="body">
    <xsl:copy>
      <xsl:apply-templates/>
      <xsl:call-template name="insertFootnotes">
        <xsl:with-param name="root" select="."/>
      </xsl:call-template>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="span[@class='footnote']">
    <xsl:variable name="id" select="generate-id(.)"/>
    <xsl:variable name="footnoteNumber"><xsl:number count="span[@class='footnote']" level="any" from="body"/></xsl:variable>
    <sup style="font-size: smaller"><a href="#{$id}" id="{$id}_ref"><xsl:value-of select="$footnoteNumber"/></a></sup>
  </xsl:template>

  <xsl:template name="insertFootnotes">
    <xsl:param name="root"/>
    <xsl:variable name="footnotes" select="$root//span[@class='footnote']"/>

    <xsl:if test="$footnotes">
      <table class="footnotes">
        <col width="5%"/>
        <col width="95%"/>
        <tbody>
          <xsl:for-each select="$root//span[@class='footnote']">
            <xsl:variable name="id" select="generate-id(.)"/>
            <tr>
              <td><a name="{$id}" href="#{$id}_ref"><xsl:value-of select="position()"/>.</a></td>
              <td><xsl:apply-templates select="node()"/></td>
            </tr>
          </xsl:for-each>
        </tbody>
      </table>
    </xsl:if>
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>