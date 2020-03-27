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
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:p="http://outerx.org/daisy/1.0#publisher">

  <xsl:template match="document">
    <xsl:apply-templates select="p:publisherResponse/d:document"/>
  </xsl:template>

  <xsl:template match="d:document">
    <html>
      <body>
        <h0 id="dsy{@id}" daisyDocument="{@id}" daisyBranch="{@branchId}" daisyLanguage="{@languageId}"><xsl:value-of select="@name"/></h0>
        <xsl:apply-templates select="d:parts/d:part"/>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="d:part">
    <xsl:if test="@daisyHtml = 'true'">
      <xsl:apply-templates select="html/body/node()"/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="img">
    <xsl:choose>
      <xsl:when test="@bookStorePath">
        <img src="bookinstance:{@bookStorePath}">
          <xsl:copy-of select="@*[local-name(.) != 'src']"/>
        </img>
      </xsl:when>
      <xsl:otherwise>
        <img src="{@src}">
          <xsl:copy-of select="@*[local-name(.) != 'src']"/>
        </img>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>