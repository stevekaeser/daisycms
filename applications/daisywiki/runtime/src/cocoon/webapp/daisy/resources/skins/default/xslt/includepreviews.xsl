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
  xmlns:p="http://outerx.org/daisy/1.0#publisher"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:template match="page">
    <xsl:apply-templates select="p:publisherResponse"/>
  </xsl:template>

  <xsl:template match="p:publisherResponse">
    <includePreviews>
      <xsl:apply-templates select="p:group"/>
    </includePreviews>
  </xsl:template>

  <xsl:template match="p:group">
    <includePreview>
      <xsl:choose>
        <xsl:when test="@error = 'true'">
          <p>
            <i18n:text key="includepreview.error"/>                    
            <br/>
            <xsl:for-each select="d:error/d:cause/d:exception">
              <xsl:value-of select="@message"/>
              <xsl:if test="position() != last">
                <br/>
              </xsl:if>
            </xsl:for-each>
          </p>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="p:document/d:document"/>
        </xsl:otherwise>
      </xsl:choose>
    </includePreview>
  </xsl:template>

  <xsl:template match="d:document">
    <h1><xsl:value-of select="@name"/></h1>
    <xsl:apply-templates select="d:parts"/>
    <xsl:apply-templates select="d:fields"/>
  </xsl:template>

  <xsl:template match="d:parts">
    <xsl:choose>
      <xsl:when test="d:part[@inlined='true']">
        <xsl:for-each select="d:part[@inlined='true']">
          <xsl:if test="position() > 1">
            <br/>
          </xsl:if>
          <xsl:copy-of select="html/body/node()"/>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
        <xsl:if test="d:part">
          <h2><i18n:text key="doceditor.parts"/></h2>
          <ul>
            <xsl:for-each select="d:part">
              <li><xsl:value-of select="@label"/></li>
            </xsl:for-each>
          </ul>
        </xsl:if>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="d:fields">
    <!-- Show table with the fields, if any -->
    <xsl:if test="d:field">
      <h2><i18n:text key="document.fields-title"/></h2>
      <ul>
        <xsl:for-each select="d:field">
          <xsl:variable name="isLinkType" select="@valueType = 'link'"/>
          <xsl:variable name="isHierarchical" select="@hierarchical = 'true'"/>
          <li>
            <xsl:value-of select="@label"/>:
            <xsl:for-each select="*">
              <xsl:if test="position() > 1"><br/></xsl:if>
              <xsl:choose>
                <xsl:when test="$isHierarchical">
                  <xsl:for-each select="*">
                    <xsl:if test="position() > 1"> / </xsl:if>
                    <xsl:value-of select="@valueFormatted"/>
                  </xsl:for-each>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="@valueFormatted"/>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:for-each>
          </li>
        </xsl:for-each>
      </ul>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>