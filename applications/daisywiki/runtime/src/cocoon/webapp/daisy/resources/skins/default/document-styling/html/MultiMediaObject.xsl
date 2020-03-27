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
  xmlns:d="http://outerx.org/daisy/1.0"> 
  
  <xsl:import href="daisyskin:xslt/document-to-html.xsl"/>
  
  <xsl:variable name="fileName">
    <xsl:if test="$document/d:parts/d:part[@name = 'MultiMediaData']/@fileName != ''">
      <xsl:value-of select="concat('/', $document/d:parts/d:part[@name = 'MultiMediaData']/@fileName)"/>
    </xsl:if>
  </xsl:variable>
  
  <xsl:variable name="filePath" select="concat ($documentBasePath, $document/@id, '/version/', $document/@dataVersionId, '/part/', $document/d:parts/d:part[@name = 'MultiMediaData']/@typeId, '/data', $fileName, '?branch=', $document/@branch, '&amp;language=', $document/@language)"/>
  
  <xsl:template match="d:document">
    <object>
      <xsl:apply-templates select="d:fields/d:field[(@name = 'MultiMediaObjectHeight') or (@name = 'MultiMediaObjectWidth')]" mode="object"/>
      <xsl:apply-templates select="d:parts/d:part[@name = 'MultiMediaData']" mode="object"/>
      <xsl:apply-templates select="d:fields/d:field[starts-with(@name, 'MultiMediaObject') and
        ((@name != 'MultiMediaObjectHeight') or (@name != 'MultiMediaObjectWidth')) ]" mode="object"/>
      <embed>
        <xsl:apply-templates select="d:parts/d:part[@name = 'MultiMediaData']" mode="embed"/>
        <xsl:apply-templates select="d:fields/d:field[starts-with(@name, 'MultiMediaObject')]" mode="embed"/>
      </embed>
    </object>
    <br/>
    <xsl:apply-templates select="d:links"/>
    
    <xsl:call-template name="insertFootnotes">
      <xsl:with-param name="root" select="."/>
    </xsl:call-template>
    
  </xsl:template>
  
  <xsl:template match="d:part" mode="object"> 
      <xsl:choose>
        <xsl:when test="@mimeType = 'application/x-shockwave-flash'">
          <xsl:attribute name="classid">clsid:D27CDB6E-AE6D-11cf-96B8-444553540000</xsl:attribute>
          <xsl:attribute name="codebase">http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=7,0,19,0</xsl:attribute>
          <param name="movie" value="{$filePath}"/>
        </xsl:when>   
        <xsl:when test="contains(@mimeType, 'audio/')">
          <xsl:attribute name="classid">clsid:22D6F312-B0F6-11D0-94AB-0080C74C7E95</xsl:attribute>          
          <param name="FileName" value="{$filePath}"/>
        </xsl:when> 
        <xsl:when test="contains(@mimeType, 'video/')">     
          <xsl:attribute name="classid">clsid:05589FA1-C356-11CE-BF01-00AA0055595A</xsl:attribute>               
          <param name="FileName" value="{$filePath}"/>
        </xsl:when>     
      </xsl:choose>   
      <xsl:attribute name="type">
        <xsl:value-of select="@mimeType" />
      </xsl:attribute>
  </xsl:template>
  
  <xsl:template match="d:part" mode="embed">
    <xsl:attribute name="src">
      <xsl:value-of select="$filePath"/>
    </xsl:attribute>
  </xsl:template>
  
  <xsl:template match="d:field" mode="object">
    <xsl:variable name="fieldName" select="substring-after(@name,'MultiMediaObject')"/>
    <xsl:choose>
      <xsl:when test="(($fieldName = 'Height') or ($fieldName = 'Width'))">
        <xsl:attribute name="{$fieldName}">
          <xsl:value-of select="@valueFormatted"/>
        </xsl:attribute>
      </xsl:when>
      <xsl:otherwise>
        <param name="{$fieldName}" value="{@valueFormatted}"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template match="d:field" mode="embed">
    <xsl:variable name="fieldName" select="substring-after(@name,'MultiMediaObject')"/>
      
    <xsl:attribute name="{$fieldName}">
      <xsl:value-of select="@valueFormatted"/>
    </xsl:attribute>
  </xsl:template>

</xsl:stylesheet>