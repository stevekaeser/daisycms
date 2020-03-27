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
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:d="http://outerx.org/daisy/1.0"
  xmlns:fo="http://www.w3.org/1999/XSL/Format">

  <xsl:template match="d:searchResult[@styleHint='bullets']">
    <fo:list-block xsl:use-attribute-sets="list">
      <xsl:for-each select="d:rows/d:row">
        <fo:list-item xsl:use-attribute-sets="list.item">
          <fo:list-item-label>
            <fo:block>
              <xsl:if test="not(@style='list-style: none')">
                  <xsl:text>&#8226;</xsl:text>
              </xsl:if>
            </fo:block>
          </fo:list-item-label>
          <fo:list-item-body start-indent="body-start()">
            <fo:block>
              <xsl:value-of select="d:value[1]"/>
            </fo:block>
          </fo:list-item-body>
        </fo:list-item>
      </xsl:for-each>
    </fo:list-block>
  </xsl:template>
  
  <xsl:template match="d:searchResult[@styleHint='chunked']">
    <fo:table xsl:use-attribute-sets="table" table-layout="fixed" width="100%">
      <xsl:for-each select="d:titles/d:title">
        <fo:table-column column-number="position()" column-width="proportional-column-width(1)"/>
      </xsl:for-each>
      <fo:table-body>
        <fo:table-row>
          <xsl:for-each select="d:titles/d:title">
            <fo:table-cell xsl:use-attribute-sets="th"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
          </xsl:for-each>
        </fo:table-row>
        <xsl:apply-templates select="d:rows/d:row"/>
      </fo:table-body>
    </fo:table>
  </xsl:template>
  
  <xsl:template match="d:row">
    <fo:table-row>
      <xsl:for-each select="*">
        <fo:table-cell xsl:use-attribute-sets="td"><fo:block><xsl:apply-templates/></fo:block></fo:table-cell>
      </xsl:for-each>
    </fo:table-row>
  </xsl:template>
  
  <xsl:template match="d:value">
    <fo:block><xsl:value-of select="."/></fo:block>
  </xsl:template>

  <xsl:template match="d:multiValue">
    <fo:block><xsl:for-each select="*">
      <xsl:if test="position() > 1">, </xsl:if>
      <xsl:apply-templates select="."/>
    </xsl:for-each></fo:block>
  </xsl:template>

  <xsl:template match="d:xmlValue">
    <xsl:copy-of select="html/body/node()"/>
  </xsl:template>

  <xsl:template match="d:linkValue">
      <!-- If the document doesn't have the link field, a linkValue tag without any attributes
           or content is added in the row. In that case, don't create an (invalid) link. -->
      <xsl:if test="@documentId != ''">
          <fo:basic-link
            color="blue"
            external-destination="url('{@href}')">
            <xsl:choose>
              <xsl:when test="starts-with(@href, '#')">
                <xsl:attribute name="internal-destination"><xsl:value-of select="substring(@href, 2)"/></xsl:attribute>
              </xsl:when>
              <xsl:otherwise>
                <xsl:attribute name="external-destination">url('<xsl:value-of select="@href"/>')</xsl:attribute>
              </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates/>
          </fo:basic-link>
      </xsl:if>
  </xsl:template>

  <xsl:template match="d:hierarchyValue">
    <xsl:for-each select="*">
      <xsl:if test="position() > 1"> / </xsl:if>
      <xsl:apply-templates select="."/>
    </xsl:for-each>
  </xsl:template>

  

</xsl:stylesheet>