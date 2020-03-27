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

  <xsl:variable name="mountPoint" select="/page/context/mountPoint"/>
  <xsl:variable name="siteName" select="/page/context/site/@name"/>

  <xsl:variable name="variantSet" select="/page/d:availableVariants" />
  <xsl:variable name="documentId" select="/page/documentId"/>

  <xsl:key name="variants-by-branch" match="d:availableVariant" use="@branchName" />
  <xsl:key name="variants-by-language" match="d:availableVariant" use="@languageName" />
  <xsl:key name="variants-by-both" match="d:availableVariant" use="concat(@branchId, '|', @languageId)" />

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><i18n:text key="altvariants.title"/></pageTitle>
      <content>
        <h1><i18n:text key="altvariants.title"/></h1>
        <p>
          <i18n:text key="altvariants.intro"/>
        </p>

        <table class="default">
          <tbody>
            <!-- heading -->
            <tr>
              <th rowspan="2"><i18n:text key="branch"/></th>
              <!-- note: we're slightly overcounting the language-columns here... -->
              <th colspan="{count($variantSet/d:availableVariant/@languageName)}"><i18n:text key="language"/></th>
            </tr>
            <xsl:call-template name="languages-row-for-branch"/>

            <!-- force first branch-row to be 'main' -->
            <xsl:call-template name="languages-row-for-branch">
              <xsl:with-param name="branchId"   select="'1'" />
              <xsl:with-param name="branchName" select="'main'" />
            </xsl:call-template>

            <!-- actual rows -->
            <xsl:for-each select="$variantSet/d:availableVariant[@branchName != 'main'][count(. | key('variants-by-branch', @branchName)[1]) = 1]">
              <xsl:sort select="@branchName" />
              <xsl:variable name="branchId" select="@branchId" />

              <xsl:call-template name="languages-row-for-branch">
                <xsl:with-param name="branchId"   select="$branchId" />
                <xsl:with-param name="branchName" select="@branchName" />
              </xsl:call-template>

            </xsl:for-each>
          </tbody>
        </table>

      </content>
    </page>
  </xsl:template>


  <xsl:template name="languages-row-for-branch" >
    <!-- note: logic in here is reused to produce the heading-row listing all the languages -->
    <xsl:param name="branchId" />
    <xsl:param name="branchName" />

    <tr>
      <xsl:choose>
        <xsl:when test="$branchId">
          <td style="text-align: center;"><xsl:value-of select="$branchName" /></td>
          <!-- force first language-column to be 'default' -->
          <xsl:call-template name="variant-cell" >
            <xsl:with-param name="branchId" select="$branchId"/>
            <xsl:with-param name="languageId" select="'1'"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <th>default</th>
        </xsl:otherwise>
      </xsl:choose>

      <xsl:for-each select="$variantSet/d:availableVariant[@languageName!='default'][count(.| key('variants-by-language', @languageName)[1]) = 1]">
        <xsl:sort select="@languageName" />
        <xsl:variable name="languageId" select="@languageId" />

        <xsl:choose>
          <xsl:when test="$branchId">
            <xsl:call-template name="variant-cell" >
              <xsl:with-param name="branchId" select="$branchId"/>
              <xsl:with-param name="languageId" select="$languageId"/>
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <th><xsl:value-of select="@languageName" /></th>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:for-each>
    </tr>
  </xsl:template>


  <xsl:template name="variant-cell" >
    <xsl:param name="branchId" />
    <xsl:param name="languageId" />

    <xsl:variable name="matchingVariant" select="key('variants-by-both', concat($branchId, '|' ,$languageId))" />
    <td>
      <xsl:if test="$matchingVariant">
        <a href="{$mountPoint}/{$siteName}/{$documentId}.html?branch={$matchingVariant/@branchName}&amp;language={$matchingVariant/@languageName}">
          <xsl:call-template name="language-label" >
            <xsl:with-param name="languageName" select="$matchingVariant/@languageName"/>
          </xsl:call-template>
        </a>
      </xsl:if>
    </td>
  </xsl:template>


  <!-- extra template to allow easy language-flag implementation in custom skin -->
  <xsl:template name="language-label" >
    <xsl:param name="languageName" />
    <xsl:value-of select="$languageName"/>
  </xsl:template>

</xsl:stylesheet>
