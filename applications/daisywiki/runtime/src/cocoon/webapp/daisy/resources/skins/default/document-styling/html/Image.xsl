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
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:p="http://outerx.org/daisy/1.0#publisher"
  xmlns:lt="http://outerx.org/daisy/1.0#linktransformer">

  <xsl:import href="daisyskin:xslt/document-to-html.xsl"/>

  <xsl:template match="d:document">
    <h1 class="daisy-document-name"><xsl:value-of select="@name"/></h1>

    <xsl:variable name="thumb">
      <xsl:choose>
        <xsl:when test="d:parts/d:part[@name='ImagePreview']">
          <xsl:text>ImagePreview</xsl:text>
        </xsl:when>
        <xsl:when test="d:parts/d:part[@name='ImageThumbnail']">
          <xsl:text>ImageThumbnail</xsl:text>
        </xsl:when>
      </xsl:choose>
    </xsl:variable>

    <xsl:if test="$thumb != ''">
      <a href="daisy:{@id}@{@branch}:{@language}:{@dataVersionId}" lt:partLink="ImageData" lt:fileName="{d:parts/d:part[@name='ImageData']/@fileName}">
        <img src="daisy:{@id}@{@branch}:{@language}:{@dataVersionId}" lt:partLink="{$thumb}"/>
      </a>
    </xsl:if>

    <xsl:apply-templates select="d:parts/d:part"/>
    <xsl:apply-templates select="d:links"/>
    <xsl:apply-templates select="d:fields"/>

    <p>
      <a href="{$documentBasePath}{$document/@id}/referrers.html{/document/variantQueryString}"><i18n:text key="image.show-referrers" catalogue="skin"/></a>
    </p>

    <xsl:call-template name="insertFootnotes">
      <xsl:with-param name="root" select="."/>
    </xsl:call-template>
  </xsl:template>

</xsl:stylesheet>