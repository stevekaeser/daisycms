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

  <xsl:template name="render-facet">
    <xsl:param name="facet"/>
    <xsl:param name="pos"/>
    <xsl:param name="facetDefs"/>
    <xsl:param name="facetConfs"/>

    <xsl:variable name="facet" select="."/>
    <xsl:variable name="facetDef" select="$facetDefs[$pos]"/>
    <xsl:variable name="facetConf" select="$facetConfs[$pos]"/>
    
    <xsl:variable name="expression" select="$facet/@expression"/>
    <xsl:variable name="valueCount" select="count($facet/d:value)"/>
    <xsl:variable name="isDiscrete" select="$facetDef/@isDiscrete = 'true'"/>
    
    <table class="filter">
      <xsl:if test="not(preceding-sibling::node())">
        <xsl:attribute name="class">filter first</xsl:attribute>
      </xsl:if>

      <col width="5%"/>
      <col width="90%"/>
      <col width="5%"/>
      <tbody>
        <xsl:call-template name="facet-header">
          <xsl:with-param name="facet" select="$facet"/>
          <xsl:with-param name="pos" select="$pos"/>
          <xsl:with-param name="facetConf" select="$facetConf"/>
        </xsl:call-template>
        <tr>
          <!-- first render filters without matching facet values -->
          <xsl:for-each select="$filters[@facetName = $facetDef/@name]">
            <xsl:if test="not($facet/d:value/@queryFormat = @queryValue)">
              <xsl:call-template name="active-filter">
                <xsl:with-param name="filter" select="."/>
              </xsl:call-template>
            </xsl:if>
          </xsl:for-each>
        </tr>
        <xsl:for-each select="$facet/d:value">
          <xsl:variable name="value" select="."/>
          <xsl:variable name="operator">
            <xsl:choose>
              <xsl:when test="@isDiscrete = 'true'">between</xsl:when>
              <xsl:when test="../@hierarchical = 'true'">has path</xsl:when>
              <xsl:otherwise>equals</xsl:otherwise>
            </xsl:choose>
          </xsl:variable>
          <tr>
            <xsl:variable name="filter"
              select="$filters[@facetName = $facetDef/@name and @queryValue = $value/@queryFormat]"/>
            <xsl:choose>
              <xsl:when test="$filter">
                <xsl:call-template name="active-filter">
                  <xsl:with-param name="filter" select="$filter"/>
                </xsl:call-template>
              </xsl:when>
              <xsl:otherwise>
                <xsl:call-template name="facet-value">
                  <xsl:with-param name="value" select="$value"/>
                  <xsl:with-param name="facetDef" select="$facetDef"/>
                  <xsl:with-param name="operator" select="$operator"/>
                </xsl:call-template>
              </xsl:otherwise>
            </xsl:choose>
            
          </tr>
        </xsl:for-each>
        <xsl:variable name="remainingValues" select="@availableValues - $valueCount"/>
        <xsl:if test="$remainingValues > 0">
          <tr><td colspan="2"><div class="facetbrowser-morevalues">
            <a href="#" onclick="return showMoreValues({$pos - 1});" onmouseover="status=''; return true;">(<xsl:value-of select="$remainingValues"/> more)</a>
          </div></td></tr>
        </xsl:if>
      </tbody>
    </table>
  </xsl:template>

  <xsl:template name="facet-header">
        <xsl:param name="facet"/>
        <xsl:param name="facetConf"/>
        <xsl:param name="pos"/>
        <tr class="facet-header">
          <th>
            <xsl:call-template name="sort-by-value">
              <xsl:with-param name="facetConf" select="$facetConf"/>
              <xsl:with-param name="pos" select="$pos"/>
            </xsl:call-template>
          </th>
          <th>
            <span><xsl:value-of select="$facet/@label"/></span>
          </th>
          <th>  
            <xsl:call-template name="sort-by-occurrence">
              <xsl:with-param name="facetConf" select="$facetConf"/>
              <xsl:with-param name="pos" select="$pos"/>
            </xsl:call-template>
          </th>
        </tr>
  </xsl:template>

  <xsl:template name="sort-by-value">
    <xsl:param name="facet"/>
    <xsl:param name="facetConf"/>
    <xsl:param name="pos"/>

    <a href="javascript:void(0)" title="Sort by name">
      <xsl:choose>
        <xsl:when test="($facetConf/@sortOnValue = 'true') and ($facetConf/@sortAscending = 'true')">
          <xsl:attribute name="onclick">return changeFacetSort(<xsl:value-of select="$pos - 1"/>, true, false);</xsl:attribute>
          <img src="{$mountPoint}/resources/skins/{$skin}/images/facet_sort_asc_active.gif" alt="[desc]"/>
        </xsl:when>
        <xsl:when test="($facetConf/@sortOnValue = 'true')">
          <xsl:attribute name="onclick">return changeFacetSort(<xsl:value-of select="$pos - 1"/>, true, true);</xsl:attribute>
          <img src="{$mountPoint}/resources/skins/{$skin}/images/facet_sort_desc_active.gif" alt="[asc]"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="onclick">return changeFacetSort(<xsl:value-of select="$pos - 1"/>, true, true);</xsl:attribute>
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
          <xsl:attribute name="onclick">return changeFacetSort(<xsl:value-of select="$pos - 1"/>, false, false);</xsl:attribute>
          <img src="{$mountPoint}/resources/skins/{$skin}/images/facet_sort_asc_active.gif" alt="[desc]"/>
        </xsl:when>
        <xsl:when test="($facetConf/@sortOnValue != 'true')">
          <xsl:attribute name="onclick">return changeFacetSort(<xsl:value-of select="$pos - 1"/>, false, true);</xsl:attribute>
          <img src="{$mountPoint}/resources/skins/{$skin}/images/facet_sort_desc_active.gif" alt="[asc]"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="onclick">return changeFacetSort(<xsl:value-of select="$pos - 1"/>, false, true);</xsl:attribute>
          <img src="{$mountPoint}/resources/skins/{$skin}/images/facet_sort_asc.gif" alt="[asc]"/>
        </xsl:otherwise>
      </xsl:choose>
    </a>
  </xsl:template>

  <xsl:template name="active-filter">
    <xsl:param name="filter"/>
    <xsl:attribute name="class">selected</xsl:attribute>
    <xsl:variable name="filter-position"
      select="count($filter/preceding-sibling::filter)+1"/>
    <td colspan="2">
      <a class="name" href="#" onclick="return removeFilter({$filter-position});"
        onmouseover="status=''; return true;">
        <xsl:value-of select="$filter/@displayValue"/>
      </a>
    </td>
    <td>
      <a class="disabled-x" href="#" onclick="return removeFilter({$filter-position});"
        onmouseover="status=''; return true;">
        <img src="{$mountPoint}/resources/skins/{$skin}/images/facet_filter_delete.gif" alt="facetedbrowser.remove-filter-alt" title="facetedbrowser.remove-filter-title" i18n:attr="alt title"/>
      </a>
    </td>
  </xsl:template>

  <xsl:template name="facet-value">
    <xsl:param name="facetDef"/>
    <xsl:param name="operator"/>

    <td colspan="2">
      <a class="name" href="#"
        onclick="return addFilter(&quot;{daisyutil:escape($facetDef/@name)}&quot;, &quot;{daisyutil:escape(@queryFormat)}&quot;, &quot;{daisyutil:escape(@userFormat)}&quot;, &quot;{$operator}&quot;, &quot;{daisyutil:escape(@isDiscrete)}&quot;)"
        onmouseover="status=''; return true;">
        <xsl:value-of select="@userFormat"/>
      </a>
    </td>
    <td>
      <xsl:value-of select="@count"/>
    </td>
  </xsl:template>

  <xsl:template name="show-more">
    You should create a template called show-more
  </xsl:template>

</xsl:stylesheet>
