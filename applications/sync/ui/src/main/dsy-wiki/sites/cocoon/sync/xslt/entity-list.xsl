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
    <xsl:import href="daisyskin:xslt/util.xsl"/>
    <xsl:param name="pageType"/>


    <xsl:variable name="mountPoint" select="string(entity-list/context/mountPoint)"/>
    <xsl:variable name="siteName" select="string(entity-list/context/site/@name)"/>
    <xsl:variable name="basePath" select="concat($mountPoint, '/', $siteName)"/>
    <xsl:variable name="pageURI" select="entity-list/context/request/@uri"/>
    <xsl:template match="entity-list">
      <page>
            <xsl:copy-of select="context"/>
            <xsl:copy-of select="n:navigationTree"/>
            <content>
                <h1><xsl:value-of select="$pageType"/></h1>
                <xsl:choose>
                    <xsl:when test="count(entities) > 0">
                        <xsl:if test="$pageType='conflict'">
                          <xsl:call-template name="resolveAllConflicts"/>
                        </xsl:if>
                        <xsl:apply-templates select="entities"/>
                    </xsl:when>
                  <xsl:otherwise> No documents found. </xsl:otherwise>
                </xsl:choose>
            </content>
        </page>
    </xsl:template>

   <xsl:template name="resolveAllConflicts">
     <div>
       <xsl:call-template name="generatePostLink">
          <xsl:with-param name="action" select="concat( $pageURI, '/all')"/>
          <xsl:with-param name="label" select="'resolve all conflicts (external)'"/>
          <xsl:with-param name="id" select="'resolveAll'"/>
       </xsl:call-template>
     </div>
   </xsl:template>

    <xsl:template match="entities">
        <div class="entity-type">
            <div class="entity-name">
                <span class="entity-name-internal">
                    <xsl:value-of select="@internal-name"/>
                </span>
                <br/>
                <span class="entity-name-external"> ( <xsl:value-of select="@external-name"/> )
                </span>
            </div>
            <div class="entity-list">
                <xsl:choose>
                    <xsl:when test="count(entity) &gt; 0">
                        <table>
                            <tr>
                                <th>Document</th>
                                <th>External Id</th>
                                <th>Last updated on</th>
                                <th>Detail</th>
                            </tr>
                            <xsl:apply-templates select="entity"/>
                        </table>
                    </xsl:when>
                    <xsl:otherwise> No documents found. </xsl:otherwise>
                </xsl:choose>
            </div>
        </div>
    </xsl:template>

    <xsl:template match="entity">
        <tr>
            <td>
                <a
                    href="{concat( $basePath, '/', variant-key/@documentId, '?branch=', variant-key/@branchId, '&amp;language=', variant-key/@languageId)}"
                    > Document </a>
            </td>
            <td>
                <xsl:value-of select="external-id"/>
            </td>
            <td>
                <xsl:value-of select="update-timestamp"/>
            </td>
            <td>
              <a href="{concat( $pageURI, '/', variant-key/@documentId, '?branch=', variant-key/@branchId, '&amp;language=', variant-key/@languageId)}"> Show detail </a>
            </td>
        </tr>
    </xsl:template>
</xsl:stylesheet>
