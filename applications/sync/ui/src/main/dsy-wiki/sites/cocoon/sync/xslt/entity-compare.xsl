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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:template match="entity-compare">
    <div class="entity">
      <div class="entity-name">
        <span class="entity-name-internal">
          <xsl:value-of select="sync/entity/internal-name"/>
        </span>
        <span class="entity-name-external"> ( <xsl:value-of select="sync/entity/name"/> ) </span>
      </div> External Id : <xsl:value-of select="sync/entity/external-id"/>
      <xsl:variable name="variant-key" select="sync/entity/variant-key"/>
      <br/><a
        href="{concat( $basePath, '/', $variant-key/@documentId, '?branch=', $variant-key/@branchId, '&amp;language=', $variant-key/@languageId)}"
        >Show Document</a>
      <br/> Daisy Version : <xsl:value-of select="sync/entity/daisy-version"/>
      <table>
        <tr>
          <th>External Values</th>
          <th>Daisy Values</th>
        </tr>
        <tr>
          <td> Last Modified : <xsl:value-of select="sync/entity/lastmodified"/>
          <br/>
            Deleted : <xsl:value-of select="sync/entity/external-deleted"/>
            <xsl:apply-templates select="sync/entity"/>
          </td>
          <td> Last Modified : <xsl:value-of select="daisy/entity/update-timestamp"/>
          <br/>
            Deleted : <xsl:value-of select="daisy/entity/daisy-deleted"/>
            <xsl:apply-templates select="daisy/entity"/>
          </td>
        </tr>
      </table>
    </div>    
  </xsl:template>
  
  <xsl:template match="entity">
    <h3>Attributes :</h3>
    <xsl:apply-templates select="attributes"/>
  </xsl:template>
  
  <xsl:template match="attributes">
    <table>
      <tr>
        <th>Name</th>
        <th>Value</th>
      </tr>
      <xsl:apply-templates select="attribute"/>
    </table>
  </xsl:template>
  
  <xsl:template match="attribute">
    <tr>
      <td>
        <xsl:value-of select="daisy-name"/>
      </td>
      <td>
        <xsl:apply-templates select="values"/>
      </td>
    </tr>
  </xsl:template>
  
  <xsl:template match="values">
    <xsl:choose>
      <xsl:when test="count(value) > 1">
        <ul>
          <xsl:for-each select="value">
            <li>
              <xsl:value-of select="value"/>
            </li>
          </xsl:for-each>
        </ul>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="value"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
