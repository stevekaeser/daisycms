<?xml version="1.0" encoding="UTF-8"?>
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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
  xmlns:n="http://outerx.org/daisy/1.0#navigation">
  <xsl:variable name="mountPoint" select="string(container/context/mountPoint)"/>
  <xsl:variable name="siteName" select="string(container/context/site/@name)"/>
  <xsl:variable name="basePath" select="concat($mountPoint, '/', $siteName)"/>
  <xsl:variable name="pageURI" select="container/context/request/@uri"/>
  
  <xsl:include href="entity-compare.xsl"/>
  
  <xsl:template match="container">
    <page>
      <xsl:copy-of select="context"/>
      <xsl:copy-of select="n:navigationTree"/>
      <content>
        <xsl:apply-templates select="sync"></xsl:apply-templates>
      </content>
    </page>
  </xsl:template>
  
  <xsl:template match="sync">
    <h1>Synchronizer</h1>
    <p>
    <xsl:apply-templates select="status"/>
    </p>
    <form action="{$pageURI}" method="post">
      <input type="submit" value="Start Synchronizing"/>
    </form>
    <p>
      <xsl:apply-templates select="commandResult"/>
    </p>
  </xsl:template>
  
  <xsl:template match="status">
    Current Synchronizer Status : <xsl:value-of select="."/>
    <br/>
  </xsl:template>
  
  <xsl:template match="commandResult">
    <xsl:choose>
      <xsl:when test="string(.) = 'true'">
        Successful Synchronization Start
      </xsl:when>
      <xsl:otherwise>
        Unsuccessful Synchronization Start
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>
