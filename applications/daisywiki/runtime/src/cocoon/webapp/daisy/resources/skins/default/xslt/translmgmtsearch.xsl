<!--
  Copyright 2007 Outerthought bvba and Schaubroeck nv

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
                xmlns:p="http://outerx.org/daisy/1.0#publisher"
                xmlns:n="http://outerx.org/daisy/1.0#navigation"
                xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
                xmlns:urlencoder="xalan://java.net.URLEncoder">

  <xsl:import href="daisyskin:xslt/searchresult.xsl"/>
  <xsl:import href="daisyskin:xslt/util.xsl"/>

  <xsl:variable name="pageURI" select="page/context/request/@uri"/>
  <xsl:variable name="pageURIEncoded" select="urlencoder:encode($pageURI, 'UTF-8')"/>

  <xsl:variable name="idPos" select="1"/>
  <xsl:variable name="branchPos" select="2"/>
  <xsl:variable name="langPos" select="3"/>
  <xsl:variable name="langIdPos" select="4"/>
  <xsl:variable name="namePos" select="5"/>
  <xsl:variable name="refLangPos" select="6"/>
  <xsl:variable name="refLangIdPos" select="7"/>
  <xsl:variable name="versionIdPos" select="8"/>
  <xsl:variable name="syncedWithLangPos" select="9"/>
  <xsl:variable name="syncedWithVersionIdPos" select="10"/>
  <xsl:variable name="syncedWithMajorChangePos" select="11"/>
  
  <xsl:variable name="infoPresent" select="boolean(/page/d:searchResult/d:titles/d:title[$versionIdPos])"/>
  
  <xsl:key name="docIdGroup" match="/page/d:searchResult/d:rows/d:row" use="@documentId" />
  <xsl:key name="docIdBranchGroup" match="/page/d:searchResult/d:rows/d:row" use="concat(@documentId, '@', @branchId)" />

  <xsl:template match="page">
    <xsl:apply-templates select="d:searchResult"/>
  </xsl:template>

  <xsl:template match="d:searchResult">
    <xsl:variable name="titles" select="d:titles"/>
    
    <xsl:choose>
      <xsl:when test="d:rows/d:row">
        <table class="default">
          <tr>
            <th><xsl:value-of select="$titles/d:title[$idPos]"/></th>
            <th><xsl:value-of select="$titles/d:title[$branchPos]"/></th>
            <th><xsl:value-of select="$titles/d:title[$langPos]"/></th>
            <th><xsl:value-of select="$titles/d:title[$namePos]"/></th>
            <th><xsl:value-of select="$titles/d:title[$refLangPos]"/></th>
            <xsl:if test="$infoPresent">
              <th>Info</th>
            </xsl:if>
            <th><i18n:text key="searchresult.actions"/></th>
          </tr>
          <xsl:apply-templates select="d:rows/d:row"/>
        </table>
      </xsl:when>
      <xsl:otherwise>
        <p>Nothing found.</p>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:call-template name="createInfo">
      <xsl:with-param name="info" select="d:executionInfo"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="d:row">
    <xsl:variable name="variantUrl"><xsl:call-template name="variantUrl">
      <xsl:with-param name="id" select="*[$idPos]"/>
      <xsl:with-param name="branch" select="*[$branchPos]"/>
      <xsl:with-param name="language" select="*[$langPos]"/>
    </xsl:call-template></xsl:variable>
    <tr>
      <td><xsl:apply-templates select="*[$idPos]"/></td>
      <td><xsl:apply-templates select="*[$branchPos]"/></td>
      <td><xsl:apply-templates select="*[$langPos]"/></td>
      <td><xsl:apply-templates select="*[$namePos]"/></td>
      <td><xsl:apply-templates select="*[$refLangPos]"/></td>
      <xsl:if test="$infoPresent">
        <td>
          <xsl:choose>
            <xsl:when test="*[$syncedWithLangPos] != ''">
              <!-- [ xxx  ] synced with [ yyy  ] [ major change ] in [ zzz  ]-->
              <!-- [ url1 ] synced with [ url2 ] [    url3      ] in [ url4 ]-->
              <xsl:variable name="url1"><xsl:call-template name="versionUrl">
                <xsl:with-param name="id" select="*[$idPos]"/>
                <xsl:with-param name="branch" select="*[$branchPos]"/>
                <xsl:with-param name="language" select="*[$langPos]"/>
                <xsl:with-param name="versionId" select="*[$versionIdPos]"/>
              </xsl:call-template></xsl:variable>
              <xsl:variable name="url2"><xsl:call-template name="versionUrl">
                <xsl:with-param name="id" select="*[$idPos]"/>
                <xsl:with-param name="branch" select="*[$branchPos]"/>
                <xsl:with-param name="language" select="*[$syncedWithLangPos]"/>
                <xsl:with-param name="versionId" select="*[$syncedWithVersionIdPos]"/>
              </xsl:call-template></xsl:variable>
              <xsl:variable name="url3"><xsl:call-template name="diffUrl">
                <xsl:with-param name="id" select="*[$idPos]"/>
                <xsl:with-param name="branch" select="*[$branchPos]"/>
                <xsl:with-param name="language" select="*[$syncedWithLangPos]"/>
                <xsl:with-param name="versionId" select="*[$syncedWithMajorChangePos]"/>
                <xsl:with-param name="otherVersionId" select="*[$syncedWithVersionIdPos]"/>
              </xsl:call-template></xsl:variable>
              <xsl:variable name="url4"><xsl:call-template name="versionUrl">
                <xsl:with-param name="id" select="*[$idPos]"/>
                <xsl:with-param name="branch" select="*[$branchPos]"/>
                <xsl:with-param name="language" select="*[$syncedWithLangPos]"/>
                <xsl:with-param name="versionId" select="*[$syncedWithMajorChangePos]"/>
              </xsl:call-template></xsl:variable>
              
              <a href="{$url1}"><xsl:value-of select="*[$langPos]"/>:<xsl:value-of select="*[$versionIdPos]"/></a>
              synced with
              <a href="{$url2}"><xsl:value-of select="*[$syncedWithLangPos]"/>:<xsl:value-of select="*[$syncedWithVersionIdPos]"/></a>
              <br/>
              <xsl:choose>
                <xsl:when test="*[$syncedWithVersionIdPos] != *[$syncedWithMajorChangePos]">
                  <a href="{$url3}">major change</a>
                </xsl:when>
                <xsl:otherwise>
                  major change
                </xsl:otherwise>
              </xsl:choose>
              in
              <a href="{$url4}"><xsl:value-of select="*[$syncedWithLangPos]"/>:<xsl:value-of select="*[$syncedWithMajorChangePos]"/></a>
            </xsl:when>
            <xsl:otherwise>
              Not synced.
            </xsl:otherwise>
          </xsl:choose>
        </td>
      </xsl:if>
      <td>
        <a href="{$variantUrl}"><i18n:text key="searchresult.action-show"/></a>
        <xsl:if test="contains(concat(',',@access,','),',write,')">
          <xsl:variable name="variantParam"><xsl:call-template name="variantParam">
            <xsl:with-param name="branch" select="*[$branchPos]"/>
            <xsl:with-param name="language" select="*[$langPos]"/>
          </xsl:call-template></xsl:variable>
          <xsl:variable name="url">
            <xsl:value-of select="concat($searchResultBasePath, *[$idPos], '/edit?returnTo=', $pageURIEncoded)"/><xsl:if test="$variantParam != ''">&amp;<xsl:value-of select="$variantParam"/></xsl:if>
          </xsl:variable>
          <xsl:call-template name="generatePostLink">
            <xsl:with-param name="action" select="$url"/>
            <xsl:with-param name="id"><xsl:value-of select="concat('edit-',@documentId,'@',@branchId,':',@languageId)"/></xsl:with-param>
            <xsl:with-param name="label"><i18n:text key="searchresult.action-edit"/></xsl:with-param>
          </xsl:call-template>
        </xsl:if>
      </td>
    </tr>
  </xsl:template>
  
  <xsl:template name="variantParam">
    <xsl:param name="branch"/>
    <xsl:param name="language"/>
    <xsl:if test="($branch != $siteBranchId and $branch != $siteBranch) or ($language != $siteLanguageId and $language != $siteLanguage)">
      <xsl:value-of select="concat('branch=', $branch, '&amp;language=', $language)"/>
    </xsl:if>
  </xsl:template>
  
  <xsl:template name="variantQueryString">
    <xsl:param name="branch"/>
    <xsl:param name="language"/>
    <xsl:variable name="variantParam"><xsl:call-template name="variantParam">
      <xsl:with-param name="branch" select="$branch"/>
      <xsl:with-param name="language" select="$language"/>
    </xsl:call-template></xsl:variable>
    <xsl:if test="$variantParam != ''">
      <xsl:value-of select="concat('?', $variantParam)"/>
    </xsl:if>
  </xsl:template>
  
  <xsl:template name="versionUrl">
    <xsl:param name="id"/>
    <xsl:param name="branch"/>
    <xsl:param name="language"/>
    <xsl:param name="versionId"/>
    <xsl:variable name="variantQueryString"><xsl:call-template name="variantQueryString">
      <xsl:with-param name="branch" select="$branch"/>
      <xsl:with-param name="language" select="$language"/>
    </xsl:call-template></xsl:variable>
    <xsl:value-of select="concat($searchResultBasePath, $id, '/version/', $versionId, $variantQueryString)"/>
  </xsl:template>

  <xsl:template name="diffUrl">
    <xsl:param name="id"/>
    <xsl:param name="branch"/>
    <xsl:param name="language"/>
    <xsl:param name="versionId"/>
    <xsl:param name="otherVersionId"/>
    
    <xsl:variable name="variantParam"><xsl:call-template name="variantParam">
      <xsl:with-param name="branch" select="$branch"/>
      <xsl:with-param name="language" select="$language"/>
    </xsl:call-template></xsl:variable>
    <xsl:variable name="querySuffix">
      <xsl:if test="$variantParam != ''">&amp;</xsl:if><xsl:value-of select="$variantParam"/>
    </xsl:variable>
    
    <xsl:choose>
      <xsl:when test="number($versionId) > number($otherVersionId)">
        <xsl:value-of select="concat($searchResultBasePath, $id, '/version/', $otherVersionId, '/diff?otherVersion=', $versionId, $querySuffix)"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="concat($searchResultBasePath, $id, '/version/', $versionId, '/diff?otherVersion=', $otherVersionId, $querySuffix)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="variantUrl">
    <xsl:param name="id"/>
    <xsl:param name="branch"/>
    <xsl:param name="language"/>
    <xsl:variable name="variantQueryString"><xsl:call-template name="variantQueryString">
      <xsl:with-param name="branch" select="$branch"/>
      <xsl:with-param name="language" select="$language"/>
    </xsl:call-template></xsl:variable>
    <xsl:value-of select="concat($searchResultBasePath, $id, '.html', $variantQueryString)"/>
  </xsl:template>

</xsl:stylesheet>
