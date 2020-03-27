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
  xmlns:daisyutil="xalan://org.outerj.daisy.frontend.util.XslUtil">

  <!-- Include custom query result stylings -->
  <xsl:include href="daisyskin:query-styling/query-styling-html.xsl"/>

  <xsl:variable name="siteBranch" select="string(/page/context/site/@branch)"/>
  <xsl:variable name="siteBranchId" select="string(/page/context/site/@branchId)"/>
  <xsl:variable name="siteLanguage" select="string(/page/context/site/@language)"/>
  <xsl:variable name="siteLanguageId" select="string(/page/context/site/@languageId)"/>

  <!-- If the searchresult.xsl is imported into a document-styling stylesheet,
       documentBasePath will be defined, otherwise use default. -->
  <xsl:param name="documentBasePath"/>
  <xsl:variable name="searchResultBasePath">
    <xsl:choose>
      <xsl:when test="$documentBasePath != ''">
        <xsl:value-of select="$documentBasePath"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="concat(page/context/mountPoint/text(), '/', page/context/site/@name, '/')"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:template match="page">
    <h2 id="searchTitle"><i18n:text key="searchresult.title"/></h2>

    <xsl:choose>
      <xsl:when test="boolean(d:searchResult/d:rows/d:row)">
        <xsl:apply-templates select="d:searchResult"/>

        <br/>
        <div>
          <button type="button"
             onclick="var form = document.forms['querysearch']; form['daisyquery'].value = '{daisyutil:escape(d:searchResult/d:executionInfo/d:query/text())}'; form['addToDocumentBasket'].value = 'true'; form.submit(); return false;">
            <i18n:text key="searchresult.add-to-basket"/>
          </button>
        </div>

        <xsl:if test="/page/context/user/name != 'guest'">
            <br/>
            <div>
              <form name="serpform" method="GET" action="{$mountPoint}/{/page/context/site/@name}/searchAndReplace">
                <input type="hidden" name="query" value="{d:searchResult/d:executionInfo/d:query/text()}"/>
                <input type="hidden" name="needle" value=""/>
                <input type="submit" i18n:attr="value" value="searchresult.start-search-and-replace"/>
              </form>
            </div>
        </xsl:if>
      </xsl:when>
      <xsl:otherwise>
        <i18n:text key="searchresult.nothing-found"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="d:searchResult">
    <table class="default">
      <tr>
        <xsl:for-each select="d:titles/d:title">
          <th><xsl:value-of select="."/></th>
        </xsl:for-each>
        <th><i18n:text key="searchresult.actions"/></th>
      </tr>
      <xsl:apply-templates select="d:rows/d:row"/>
    </table>
    <xsl:call-template name="createInfo">
      <xsl:with-param name="info" select="d:executionInfo"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="d:row">
    <tr>
      <xsl:for-each select="d:value|d:xmlValue|d:multiValue|d:linkValue|d:hierarchyValue">
        <td>
          <xsl:apply-templates select="."/>
        </td>
      </xsl:for-each>
      <xsl:variable name="hrefSuffix">
        <xsl:if test="@branchId != $siteBranchId or @languageId != $siteLanguageId">
          <xsl:value-of select="concat('?branch=', @branchId, '&amp;language=', @languageId)"/>
        </xsl:if>
      </xsl:variable>
      <td><a href="{$searchResultBasePath}{@documentId}.html{$hrefSuffix}"><i18n:text key="searchresult.action-show"/></a></td>
    </tr>
  </xsl:template>

  <xsl:template match="d:value">
    <xsl:value-of select="."/>
  </xsl:template>

  <xsl:template match="d:multiValue">
    <xsl:for-each select="*">
      <xsl:if test="position() > 1">, </xsl:if>
      <xsl:apply-templates select="."/>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="d:xmlValue">
    <xsl:copy-of select="html/body/node()"/>
  </xsl:template>

  <xsl:template match="d:linkValue">
    <!-- If the document doesn't have the link field, a linkValue tag without any attributes
         or content is added in the row. In that case, don't create an (invalid) link. -->
    <xsl:if test="@documentId != ''">
      <a href="{$searchResultBasePath}{@documentId}.html?branch={@branchId}&amp;language={@languageId}"><xsl:value-of select="."/></a>
    </xsl:if>
  </xsl:template>

  <xsl:template match="d:hierarchyValue">
    <xsl:for-each select="*">
      <xsl:if test="position() > 1"> / </xsl:if>
      <xsl:apply-templates select="."/>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="createInfo">
    <xsl:param name="info"/>

    <xsl:comment>
        Query: <xsl:value-of select="normalize-space($info/d:query/text())"/>
        Extra condition: <xsl:value-of select="$info/d:extraCondition/text()"/>
        Parse and prepare time: <xsl:value-of select="$info/d:parseAndPrepareTime/text()"/>
        FullText query time: <xsl:value-of select="$info/d:fullTextQueryTime/text()"/>
        RDBMS query time: <xsl:value-of select="$info/d:rdbmsQueryTime/text()"/>
        Merge time: <xsl:value-of select="$info/d:mergeTime/text()"/>
        ACL filter time: <xsl:value-of select="$info/d:aclFilterTime/text()"/>
        Sort time: <xsl:value-of select="$info/d:sortTime/text()"/>
        Output generation time: <xsl:value-of select="$info/d:outputGenerationTime/text()"/>
    </xsl:comment>
  </xsl:template>

</xsl:stylesheet>