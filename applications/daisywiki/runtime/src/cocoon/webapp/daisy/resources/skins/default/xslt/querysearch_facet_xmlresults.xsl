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
  xmlns:n="http://outerx.org/daisy/1.0#navigation"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:daisyutil="xalan://org.outerj.daisy.frontend.util.XslUtil"
  xmlns:urlencoder="xalan://java.net.URLEncoder">

  <xsl:import href="daisyskin:xslt/facets_common.xsl"/>

  <xsl:variable name="facetDefs" select="page/facetDefinitions/facetDefinition"/>
  <xsl:variable name="filters" select="page/filters/filter"/>
  <xsl:variable name="facets" select="page/d:facetedQueryResult/d:facets/d:facet"/>
  <xsl:variable name="facetConfs" select="page/facetConfs/facetConf"/>
  <xsl:variable name="chunkSize" select="10"/>
  <xsl:variable name="mountPoint" select="string(page/context/mountPoint)"/>
  <xsl:variable name="siteName" select="string(page/context/site/@name)"/>
  <xsl:variable name="skin" select="string(page/context/skin)"/>
  <xsl:variable name="activeNavPath" select="page/n:navigationTree/@selectedPath"/>
  <xsl:variable name="selectedOptions" select="page/facetDefinitions/@selectedOptions"/>
  <xsl:variable name="foundRequestParams" select="page/facetDefinitions/requestParams/requestParam"/>
  
  <xsl:template match="page">
    <page>
    <content>
	    <div id="data">
	        <xsl:copy-of select="facetConfs"/>
	    </div>
      <div id="facets">
          <xsl:call-template name="content"/>
      </div>
      </content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <xsl:call-template name="results"/>
  </xsl:template>
  
  <xsl:template name="results">
          <xsl:apply-templates select="filters"/>
          <xsl:apply-templates select="d:facetedQueryResult/d:facets"/>
  </xsl:template>
 
  <xsl:template match="d:facets">
    <xsl:apply-templates select="d:facet"/>
  </xsl:template>

  <xsl:template match="d:facet">
    <xsl:call-template name="render-facet">
      <xsl:with-param name="facet" select="."/>
      <xsl:with-param name="pos" select="position()"/>
      <xsl:with-param name="facetDefs" select="$facetDefs"/>
      <xsl:with-param name="facetConfs" select="$facetConfs"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="d:searchResult">
    <xsl:copy-of select="."/>
  </xsl:template>

  <xsl:template match="d:value">
    <xsl:value-of select="."/>
  </xsl:template>

  <xsl:template match="d:linkValue">
    <a href="{$mountPoint}/{$siteName}/{@documentId}.html?branch={@branchId}&amp;language={@languageId}"><xsl:value-of select="."/></a>
  </xsl:template>

  <xsl:template match="d:multiValue">
    <xsl:for-each select="*">
      <xsl:if test="position() > 1">, </xsl:if>
      <xsl:apply-templates select="."/>
    </xsl:for-each>
  </xsl:template>
  
  <xsl:template match="d:hierarchyValue">
    <xsl:for-each select="*">
      <xsl:text>/</xsl:text>
      <xsl:apply-templates select="."/>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="sort-by-value">
    <xsl:param name="facet"/>
    <xsl:param name="facetConf"/>
    <xsl:param name="pos"/>

    <a href="javascript:void(0)" title="Sort by name">
      <xsl:choose>
        <xsl:when test="($facetConf/@sortOnValue = 'true') and ($facetConf/@sortAscending = 'true')">
          <xsl:attribute name="onclick">return $("#docbrowser").daisyDocumentBrowser("changeFacetSort", <xsl:value-of select="$pos - 1"/>, true, false);</xsl:attribute>
          <img src="{$mountPoint}/resources/skins/{$skin}/images/facet_sort_asc_active.gif" alt="[desc]"/>
        </xsl:when>
        <xsl:when test="($facetConf/@sortOnValue = 'true')">
          <xsl:attribute name="onclick">return $("#docbrowser").daisyDocumentBrowser("changeFacetSort", <xsl:value-of select="$pos - 1"/>, true, true);</xsl:attribute>
          <img src="{$mountPoint}/resources/skins/{$skin}/images/facet_sort_desc_active.gif" alt="[asc]"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="onclick">return $("#docbrowser").daisyDocumentBrowser("changeFacetSort", <xsl:value-of select="$pos - 1"/>, true, true);</xsl:attribute>
          <img src="{$mountPoint}/resources/skins/{$skin}/images/facet_sort_asc.gif" alt="[asc]"/>
        </xsl:otherwise>
      </xsl:choose>
    </a>
  </xsl:template>

  <xsl:template name="sort-by-occurrence">
    <xsl:param name="facetConf"/>
    <xsl:param name="pos"/>

    <a href="javascript:void(0)" title="Sort by occurrence">
      <xsl:choose>
        <xsl:when test="($facetConf/@sortOnValue != 'true') and ($facetConf/@sortAscending = 'true')">                        
          <xsl:attribute name="onclick">return $("#docbrowser").daisyDocumentBrowser("changeFacetSort", <xsl:value-of select="$pos - 1"/>, false, false);</xsl:attribute>
          <img src="{$mountPoint}/resources/skins/{$skin}/images/facet_sort_asc_active.gif" alt="[desc]"/>
        </xsl:when>
        <xsl:when test="($facetConf/@sortOnValue != 'true')">
          <xsl:attribute name="onclick">return $("#docbrowser").daisyDocumentBrowser("changeFacetSort", <xsl:value-of select="$pos - 1"/>, false, true);</xsl:attribute>
          <img src="{$mountPoint}/resources/skins/{$skin}/images/facet_sort_desc_active.gif" alt="[asc]"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="onclick">return $("#docbrowser").daisyDocumentBrowser("changeFacetSort", <xsl:value-of select="$pos - 1"/>, false, true);</xsl:attribute>
          <img src="{$mountPoint}/resources/skins/{$skin}/images/facet_sort_asc.gif" alt="[asc]"/>
        </xsl:otherwise>
      </xsl:choose>
    </a>
  </xsl:template>

  <xsl:template name="facet-value">
    <xsl:param name="facetDef"/>
    <xsl:param name="operator"/>

    <td colspan="2">
      <a class="name" href="#"
        onclick="return $('#docbrowser').daisyDocumentBrowser('addFilter','{daisyutil:escape($facetDef/@name)}', '{daisyutil:escape(@queryFormat)}', '{daisyutil:escape(@userFormat)}', '{$operator}', '{daisyutil:escape(@isDiscrete)}');"
        onmouseover="status=''; return true;">
        <xsl:value-of select="@userFormat"/>
      </a>
    </td>
    <td>
      <xsl:value-of select="@count"/>
    </td>
  </xsl:template>

  <xsl:template name="active-filter">
    <xsl:param name="filter"/>
    <xsl:attribute name="class">selected</xsl:attribute>
    <xsl:variable name="filter-position"
      select="count($filter/preceding-sibling::filter)+1"/>
    <td colspan="2">
      <a class="name" href="#" onclick="return $('#docbrowser').daisyDocumentBrowser('removeFilter', {$filter-position});"
        onmouseover="status=''; return true;">
        <xsl:value-of select="$filter/@displayValue"/>
      </a>
    </td>
    <td>
      <a href="#" onclick="return $('#docbrowser').daisyDocumentBrowser('removeFilter', {$filter-position});"
        onmouseover="status=''; return true;">
        <img src="{$mountPoint}/resources/skins/{$skin}/images/facet_filter_delete.gif" alt="facetedbrowser.remove-filter-alt" title="facetedbrowser.remove-filter-title" i18n:attr="alt title"/>
      </a>
    </td>
  </xsl:template>


</xsl:stylesheet>
