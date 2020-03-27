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
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:template match="diff-report">
    <xsl:variable name="version1Id" select="info/version1/@id"/>
    <xsl:variable name="version2Id" select="info/version2/@id"/>

    <xsl:apply-templates select="info">
      <xsl:with-param name="version1Id" select="$version1Id"/>
      <xsl:with-param name="version2Id" select="$version2Id"/>
    </xsl:apply-templates>
    <xsl:apply-templates select="parts">
      <xsl:with-param name="version1Id" select="$version1Id"/>
      <xsl:with-param name="version2Id" select="$version2Id"/>
    </xsl:apply-templates>
    <xsl:apply-templates select="links"/>
    <xsl:apply-templates select="fields">
      <xsl:with-param name="version1Id" select="$version1Id"/>
      <xsl:with-param name="version2Id" select="$version2Id"/>
    </xsl:apply-templates> 
  </xsl:template>

  <xsl:template match="info">
    <xsl:param name="version1Id"/>
    <xsl:param name="version2Id"/>
    
    <h2><i18n:text key="diff.general"/></h2>
    <table class="default">
      <tr>
        <th/>
        <th><i18n:text key="diff.version"/><xsl:text> </xsl:text><xsl:value-of select="$version1Id"/></th>
        <th><i18n:text key="diff.version"/><xsl:text> </xsl:text><xsl:value-of select="$version2Id"/></th>
      </tr>
      <tr>
        <td><i18n:text key="variant"/></td>
        <td><xsl:value-of select="concat(version1/@branch, ' - ', version1/@language)"/></td>
        <td><xsl:value-of select="concat(version2/@branch, ' - ', version2/@language)"/></td>
      </tr>
      <xsl:variable name="documentName" select="../documentName"/>
      <tr>
        <td><i18n:text key="diff.document-name"/></td>
        <td><xsl:value-of select="$documentName/@version1"/></td>
        <td>
          <xsl:choose>
            <xsl:when test="$documentName/@version2">
              <xsl:value-of select="$documentName/@version2"/>
            </xsl:when>
            <xsl:otherwise>
              <span class="diffpage-nochanges"><i18n:text key="diff.not-changed"/></span>
            </xsl:otherwise>
          </xsl:choose>
        </td>
      </tr>
      <tr>
        <td><i18n:text key="diff.creation-time"/></td>
        <td><xsl:value-of select="version1/@created"/></td>
        <td><xsl:value-of select="version2/@created"/></td>
      </tr>
      <tr>
        <td><i18n:text key="diff.created-by"/></td>
        <td><xsl:value-of select="version1/@creatorName"/></td>
        <td><xsl:value-of select="version2/@creatorName"/></td>
      </tr>
      <tr>
        <td><i18n:text key="diff.state"/></td>
        <td><i18n:text key="{version1/@state}"/></td>
        <td><i18n:text key="{version2/@state}"/></td>
      </tr>
    </table>
  </xsl:template>

  <xsl:template match="parts">
    <xsl:param name="version1Id"/>
    <xsl:param name="version2Id"/>

    <h2><i18n:text key="diff.part-changes"/></h2>
    <xsl:choose>
      <xsl:when test="*">
        <xsl:apply-templates>
          <xsl:with-param name="version1Id" select="$version1Id"/>
          <xsl:with-param name="version2Id" select="$version2Id"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise>
        <span class="diffpage-nochanges"><i18n:text key="diff.no-changes-detected"/></span>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="partUpdated">
    <xsl:param name="version1Id"/>
    <xsl:param name="version2Id"/>

    <h3>
      <i18n:translate>
        <i18n:text key="diff.part-has-changed"/>
        <i18n:param><xsl:value-of select="@typeLabel"/></i18n:param>
      </i18n:translate>
    </h3>
    <br/>
    <table class="default">
      <tr>
        <th/>
        <th><i18n:text key="diff.version"/><xsl:text> </xsl:text><xsl:value-of select="$version1Id"/></th>
        <th><i18n:text key="diff.version"/><xsl:text> </xsl:text><xsl:value-of select="$version2Id"/></th>
      </tr>

      <tr>
        <td><i18n:text key="diff.mime-type"/></td>
        <td><xsl:value-of select="@version1MimeType"/></td>
        <td>
          <xsl:choose>
            <xsl:when test="@version2MimeType">
              <xsl:value-of select="@version2MimeType"/>
            </xsl:when>
            <xsl:otherwise>
              <span class="diffpage-nochanges"><i18n:text key="diff.not-changed"/></span>
            </xsl:otherwise>
          </xsl:choose>
        </td>
      </tr>

      <tr>
        <td><i18n:text key="diff.filename"/></td>
        <td><xsl:value-of select="@version1FileName"/></td>
        <td>
          <xsl:choose>
            <xsl:when test="@version2FileName">
              <xsl:value-of select="@version2FileName"/>
            </xsl:when>
            <xsl:otherwise>
              <span class="diffpage-nochanges"><i18n:text key="diff.not-changed"/></span>
            </xsl:otherwise>
          </xsl:choose>
        </td>
      </tr>

      <tr>
        <td><i18n:text key="diff.size"/></td>
        <td><xsl:value-of select="@version1Size"/></td>
        <td>
          <xsl:choose>
            <xsl:when test="@version2Size">
              <xsl:value-of select="@version2Size"/>
            </xsl:when>
            <xsl:otherwise>
              <span class="diffpage-nochanges"><i18n:text key="diff.not-changed"/></span>
            </xsl:otherwise>
          </xsl:choose>
        </td>
      </tr>
    </table>
    <xsl:if test="diff">
      <br/>
      <div class="diffpage-contentdiff-title"><i18n:text key="diff.content-diff"/></div>
      <div class="diffpage-contentdiff">
        <xsl:for-each select="diff/div">
          <xsl:choose>
            <xsl:when test="./node()">
              <xsl:copy-of select="."/>
            </xsl:when>
            <xsl:otherwise>
              <div class="{@class}">&#160;</div>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:for-each>
      </div>
    </xsl:if>
  </xsl:template>

  <xsl:template match="partRemoved">
    <h3>
      <i18n:translate>
        <i18n:text key="diff.part-removed"/>
        <i18n:param><xsl:value-of select="@typeLabel"/></i18n:param>
      </i18n:translate>
    </h3>
  </xsl:template>

  <xsl:template match="partAdded">
    <h3>
      <i18n:translate>
        <i18n:text key="diff.part-added"/>
        <i18n:param><xsl:value-of select="@typeLabel"/></i18n:param>
      </i18n:translate>
    </h3>
  </xsl:template>

  <xsl:template match="partUnchanged">
    <h3>
      <i18n:translate>
        <i18n:text key="diff.part-unchanged"/>
        <i18n:param><xsl:value-of select="@typeLabel"/></i18n:param>
      </i18n:translate>
    </h3>
  </xsl:template>

  <xsl:template match="partMightBeUpdated">
    <h3>
      <i18n:translate>
        <i18n:text key="diff.part-mightbe-changed"/>
        <i18n:param><xsl:value-of select="@typeLabel"/></i18n:param>
      </i18n:translate>
    </h3>
  </xsl:template>

  <xsl:template match="fields">
    <xsl:param name="version1Id"/>
    <xsl:param name="version2Id"/>

    <h2><i18n:text key="diff.field-changes"/></h2>
    <xsl:choose>
      <xsl:when test="*">
        <table class="default">
          <tr>
            <th/>
            <th><i18n:text key="diff.version"/><xsl:text> </xsl:text><xsl:value-of select="$version1Id"/></th>
            <th><i18n:text key="diff.version"/><xsl:text> </xsl:text><xsl:value-of select="$version2Id"/></th>
          </tr>
          <xsl:apply-templates/>
        </table>
      </xsl:when>
      <xsl:otherwise>
        <span class="diffpage-nochanges"><i18n:text key="diff.no-changes-detected"/></span>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="fieldRemoved">
    <tr>
      <td><span style="diffpage-removedfield"><xsl:value-of select="@typeLabel"/></span></td>
      <td><span style="diffpage-removedfield"><xsl:value-of select="@version1"/></span></td>
      <td><span class="diffpage-nochanges"><i18n:text key="diff.deleted-field"/></span></td>
    </tr>
  </xsl:template>

  <xsl:template match="fieldAdded">
    <tr>
      <td><xsl:value-of select="@typeLabel"/></td>
      <td><span class="diffpage-nochanges"><i18n:text key="diff.new-field"/></span></td>
      <td><xsl:value-of select="@version2"/></td>
    </tr>
  </xsl:template>

  <xsl:template match="fieldUpdated">
    <tr>
      <td><xsl:value-of select="@typeLabel"/></td>
      <td><xsl:value-of select="@version1"/></td>
      <td><xsl:value-of select="@version2"/></td>
    </tr>
  </xsl:template>

  <xsl:template match="links">
    <h2><i18n:text key="diff.link-changes"/></h2>
    <xsl:choose>
      <xsl:when test="*">

        <xsl:if test="linkRemoved">
          <h3><i18n:text key="diff.removed-links"/></h3>
          <table class="default" style="width: 100%">
            <xsl:for-each select="linkRemoved">
              <tr>
                <td>
                  <xsl:value-of select="@title"/><br/>
                  <xsl:value-of select="@target"/><br/>
                </td>
              </tr>
            </xsl:for-each>
          </table>
        </xsl:if>

        <xsl:if test="linkAdded">
          <h3><i18n:text key="diff.added-links"/></h3>
          <table class="default" style="width: 100%">
            <xsl:for-each select="linkAdded">
              <tr>
                <td>
                  <xsl:value-of select="@title"/><br/>
                  <xsl:value-of select="@target"/><br/>
                </td>
              </tr>
            </xsl:for-each>
          </table>
        </xsl:if>
      </xsl:when>
      <xsl:otherwise>
        <span class="diffpage-nochanges"><i18n:text key="diff.no-changes-detected"/></span>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
