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
  <xsl:variable name="additionalSelects" select="page/additionalSelects/*"/>
  <xsl:variable name="chunkSize" select="10"/>
  <xsl:variable name="mountPoint" select="string(page/context/mountPoint)"/>
  <xsl:variable name="siteName" select="string(page/context/site/@name)"/>
  <xsl:variable name="skin" select="string(page/context/skin)"/>
  <xsl:variable name="activeNavPath" select="page/n:navigationTree/@selectedPath"/>
  <xsl:variable name="selectedOptions" select="page/facetDefinitions/@selectedOptions"/>
  <xsl:variable name="foundRequestParams" select="page/facetDefinitions/requestParams/requestParam"/>
  <xsl:variable name="fullTextSearchQuery" select="page/fullTextSearchQuery/text()"/>
  <xsl:variable name="orderBy" select="string(page/orderBy/text())"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <xsl:copy-of select="n:navigationTree"/>
      
      <pageTitle><i18n:text key="facetedbrowser.pagetitle"/></pageTitle>
      <layoutHints wideLayout="true"/>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1><i18n:text key="facetedbrowser.pagetitle"/></h1>

    <div class="facetbrowser-resultcount">
      <i18n:translate>
        <i18n:text key="facetedbrowser.docs-found"/>
        <i18n:param><xsl:value-of select="d:facetedQueryResult/d:searchResult/d:resultInfo/@size"/></i18n:param>
      </i18n:translate>
    </div>

    <br/>
    <xsl:call-template name="options"/>
    <br/>
    <a href='{$mountPoint}/{$siteName}/querySearch?daisyquery={urlencoder:encode(d:facetedQueryResult/d:searchResult/d:executionInfo/d:query/text(), "UTF-8")}'><i18n:text key="facetedbrowser.open-query"/></a>
    <br/>
    <br/>
    <xsl:call-template name="results"/>
    <xsl:call-template name="javascript"/>
  </xsl:template>
  
  <xsl:template name="fulltext">
    <form id="ft">
        <table class="filter first">
          <col width="5%"/>
          <col width="90%"/>
          <col width="5%"/>
          <tr>
            <td colspan="2">
              <span id="query">
                <input type="hidden" class="forms field active" name="query" id="fullTextSearchQuery" value="{/page/fullTextSearchQuery}" title=""/>
                <input type="text" class="forms field active" name="displayQuery" id="displayFTSQ" value="{/page/fullTextSearchQuery}" title=""/>
              </span>
            </td>
            <td>
              <input type="submit" onclick="updateFullTextSearch(); return false;" value="fulltext.search" i18n:attr="value"/>
            </td>
          </tr>
        </table>
    </form>
  </xsl:template>
  
  <xsl:template name="options">
    <input type="checkbox" id="limitToSiteCollection" onclick="sendRequest();">
      <xsl:if test="limitToSiteCollection = 'true'"><xsl:attribute name="checked">checked</xsl:attribute></xsl:if>
    </input>
    <label for="limitToSiteCollection">
      <i18n:translate>
        <i18n:text key="facetedbrowser.limit-to-site-collection"/>
        <i18n:param><xsl:value-of select="context/site/@collection"/></i18n:param>
      </i18n:translate>
    </label>
    <xsl:text> </xsl:text>
    <input type="checkbox" id="limitToSiteVariant" onclick="sendRequest();">
      <xsl:if test="limitToSiteVariant = 'true'"><xsl:attribute name="checked">checked</xsl:attribute></xsl:if>
    </input>
    <label for="limitToSiteVariant">
      <i18n:translate>
        <i18n:text key="facetedbrowser.limit-to-site-variant"/>
        <i18n:param><xsl:value-of select="context/site/@branch"/></i18n:param>
        <i18n:param><xsl:value-of select="context/site/@language"/></i18n:param>
      </i18n:translate>
    </label>
    <br/>
    
    <input type="hidden" id="orderBy" value="{$orderBy}"/>
    <script>
      $("#orderBy").val($("#orderBy").attr("defaultValue"));
    </script>

    <i18n:text key="facetedbrowser.order-by"/><xsl:text> </xsl:text>

    <xsl:if test="$fullTextSearchQuery != ''">
      <a class="sorting" href="javascript:void(0)" onclick="updateOrderBy('score', 'DESC')">
        <i18n:text key="facetedbrowser.score"/> <xsl:text>&#160;</xsl:text>
        <xsl:call-template name="sorting-arrow">
          <xsl:with-param name="expression" select="'score'"/>
        </xsl:call-template>
      </a>
      <xsl:text>, </xsl:text>
    </xsl:if>

    <a class="sorting" href="javascript:void(0)" onclick="updateOrderBy('name', 'ASC')">
      <i18n:text key="facetedbrowser.name"/> <xsl:text>&#160;</xsl:text>
      <xsl:call-template name="sorting-arrow">
        <xsl:with-param name="expression" select="'name'"/>
      </xsl:call-template>
    </a>

    <xsl:for-each select="$facets">
      <xsl:if test="@multiValue = 'false'">
        <xsl:text>, </xsl:text>
        <span style="white-space:nowrap">
          <xsl:call-template name="sorting-link">
            <xsl:with-param name="expression" select="@expression"/>
            <xsl:with-param name="label" select="@label"/>
          </xsl:call-template>
        </span>
      </xsl:if>
    </xsl:for-each>
        
  </xsl:template>
  
  <xsl:template name="results">
    <table class="plainTable" width="100%">
      <col width="30%"/>
      <col width="70%"/>
      <tr>
        <td valign="top" style="padding-right: 1em;">
          <xsl:apply-templates select="d:facetedQueryResult/d:facets"/>
        </td>
        <td valign="top" style="padding-left: 1em; border-left: 1px dotted grey;">
          <xsl:apply-templates select="d:facetedQueryResult/d:searchResult"/>
        </td>
      </tr>
    </table>
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
    <div class="facetbrowser-fulltext">
    <xsl:call-template name="fulltext"/>
    </div>

    <div class="facetbrowser-paging">
    <xsl:variable name="chunkEnd" select="d:resultInfo/@chunkOffset + d:resultInfo/@chunkLength - 1"/>
    <xsl:if test="d:resultInfo/@chunkOffset > 1">
      <a href="#" onclick="return showChunk({d:resultInfo/@chunkOffset - $chunkSize}, {$chunkSize});" onmouseover="status=''; return true;">&lt; <i18n:text key="facetedbrowser.previous"/></a>
      <xsl:text> </xsl:text>
    </xsl:if>
    <xsl:value-of select="d:resultInfo/@chunkOffset"/>-<xsl:value-of select="$chunkEnd"/><xsl:text> </xsl:text><i18n:text key="facetedbrowser.of"/><xsl:text> </xsl:text><xsl:value-of select="d:resultInfo/@size"/>
    <xsl:if test="$chunkEnd &lt; d:resultInfo/@size">
      <xsl:text> </xsl:text>
      <a href="#" onclick="return showChunk({d:resultInfo/@chunkOffset + $chunkSize}, {$chunkSize});" onmouseover="status=''; return true;"><i18n:text key="facetedbrowser.next"/> ></a>
    </xsl:if>
    </div>
    
    <xsl:variable name="titles" select="d:titles/d:title"/>
    <xsl:variable name="offset" select="count($titles)-count($additionalSelects)-count($facetDefs)"/>

    <div class="facetbrowser-results">
      <xsl:for-each select="d:rows/d:row">
        <div class="facetbrowser-resultdoc">
          <a class="facetbrowser-doclink" href="{$mountPoint}/{$siteName}/{@documentId}.html?branch={@branchId}&amp;language={@languageId}"><xsl:value-of select="*[$offset - 1]"/></a>
          <div class="facetbrowser-docdetails">
              
            <xsl:for-each select="*">
              <xsl:variable name="pos" select="position()"/>
              <xsl:choose>
                <xsl:when test="$pos &lt;= $offset">
                </xsl:when>
                <xsl:otherwise>
                  <b><xsl:value-of select="$titles[$pos]"/>: </b>
                  <xsl:apply-templates select="."/>
                  <br/>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:for-each>
            
            <div class="facetbrowser-docsummary">
              <xsl:choose>
                <xsl:when test="$fullTextSearchQuery!=''">
                  <xsl:copy-of select="*[1]/html/body/node()"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="*[2]"/>
                </xsl:otherwise>
              </xsl:choose>
            </div>
          </div>
        </div>
      </xsl:for-each>
  </div>
  </xsl:template>
  
  <xsl:template name="sorting-link">
    <xsl:param name="expression"/>
     <xsl:param name="defaultOrder" select="'ASC'"/>
    <xsl:param name="label"/>
    <xsl:variable name="orderBy" select="/page/orderBy/text()"/>
    
    <a class="sorting" href="javascript:void(0)" onclick="updateOrderBy('{$expression}', '{$defaultOrder}')">
      <xsl:value-of select="$label"/><xsl:text>&#160;</xsl:text>
      <xsl:call-template name="sorting-arrow">
        <xsl:with-param name="expression" select="$expression"/>
        <xsl:with-param name="defaultOrder" select="$defaultOrder"/>
      </xsl:call-template>
    </a>
  </xsl:template>
  
  <xsl:template name="sorting-arrow">
     <xsl:param name="expression"/>
     <xsl:param name="defaultOrder" select="'ASC'"/>
     <xsl:choose>
      <xsl:when test="$orderBy = concat($expression,' ASC')">                        
        <img src="{$mountPoint}/resources/skins/{$skin}/images/facet_sort_asc_active.gif" alt="[desc]"/>
      </xsl:when>
      <xsl:when test="$orderBy = concat($expression,' DESC')">
        <xsl:attribute name="onclick">updateOrderBy('{$expression}', '{$defaultOrder}')"></xsl:attribute>
        <img src="{$mountPoint}/resources/skins/{$skin}/images/facet_sort_desc_active.gif" alt="[asc]"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:attribute name="onclick">updateOrderBy('{$expression}', '{$defaultOrder}')"></xsl:attribute>
        <img src="{$mountPoint}/resources/skins/{$skin}/images/facet_sort_asc.gif" alt="[asc]"/>
      </xsl:otherwise>
    </xsl:choose>
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

  <xsl:template name="javascript">
    <script type="text/javascript">
    
      function updateOrderBy(whatEncoded, defaultOrder) {
        var what = unescape(whatEncoded);
        var current = $("#orderBy").val();
        if (!defaultOrder) {
          defaultOrder='ASC';
        }
        var opposite = defaultOrder=='ASC'?'DESC':'ASC'; 
        if (current == what + ' ' + defaultOrder) {
          $("#orderBy").val(what + ' ' + opposite);
        } else {
          $("#orderBy").val(what + ' ' + defaultOrder);
        }
        sendRequest();
      }
      
      function updateFullTextSearch() {
        $('#fullTextSearchQuery').get(0).value=$('#displayFTSQ').val();
        var order = $("#orderBy").val();
        if (order == '') {
          $("#orderBy").val("score DESC");
        }
        sendRequest();
        return false;
      }
    
      function addFilter(facetName, queryValue, displayValue, operator, isDiscrete) {
        var filters = getCurrentFilters();
        filters.push(createFilter(facetName, queryValue, displayValue, operator, isDiscrete));
        sendRequest(filters);
        return false;
      }

      /**
       * @param index 1-based index
       */
      function removeFilter(index) {
        var index = index - 1;
        var filters = getCurrentFilters();
        var newFilters = filters.slice(0, index);
        newFilters = newFilters.concat(filters.slice(index + 1));
        sendRequest(newFilters);
        return false;
      }

      function clearFilters() {
        sendRequest([]);
      }

      function sendRequest(filters, chunkOffset, chunkLength, facetConfs) {
        if (filters == null)
           filters = getCurrentFilters();
        if (chunkOffset == null)
          chunkOffset = 1;
        if (chunkLength == null)
          chunkLength = <xsl:value-of select="$chunkSize"/>;
        if (facetConfs == null)
          facetConfs = getCurrentFacetConfs();
        var limitToSiteCollection = document.getElementById("limitToSiteCollection").checked;
        var limitToSiteVariant = document.getElementById("limitToSiteVariant").checked;
        var orderBy = document.getElementById("orderBy").value;
        var ftsqInput = document.getElementById("fullTextSearchQuery");
        var ftsq = "";
        if (ftsqInput) { ftsq = ftsqInput.value }
        var requestParams = [];

        for (var i = 0; i &lt; filters.length; i++) {
          var prefix = "f." + (i + 1);
          requestParams.push(prefix + ".fn=" + encodeURIComponent(filters[i].facetName));
          requestParams.push(prefix + ".qv=" + encodeURIComponent(filters[i].queryValue));
          requestParams.push(prefix + ".dv=" + encodeURIComponent(filters[i].displayValue));
          requestParams.push(prefix + ".o=" + encodeURIComponent(filters[i].operator));
          requestParams.push(prefix + ".d=" + encodeURIComponent(filters[i].isDiscrete));
        }

        for (var i = 0; i &lt; facetConfs.length; i++) {
          var prefix = "fc." + (i + 1);
          requestParams.push(prefix + ".mv=" + facetConfs[i].maxValues);
          requestParams.push(prefix + ".sv=" + facetConfs[i].sortOnValue);
          requestParams.push(prefix + ".sa=" + facetConfs[i].sortAscending);
        }

        requestParams.push("ltsc=" + limitToSiteCollection);
        requestParams.push("ltsv=" + limitToSiteVariant);
        if (orderBy != '') {
          requestParams.push("ord=" + orderBy);
        }

        requestParams.push("cho=" + chunkOffset);
        requestParams.push("chl=" + chunkLength);
        requestParams.push("ftsq=" + encodeURIComponent(ftsq));
        
      <xsl:if test="$activeNavPath">
        requestParams.push("anp=" + "<xsl:value-of select="$activeNavPath"/>");
      </xsl:if>
      
        requestParams.push("opt=" + "<xsl:value-of select="$selectedOptions"/>");
        
      <xsl:for-each select="$foundRequestParams">
        requestParams.push("<xsl:value-of select="@name"/>=" + "<xsl:value-of select="@value"/>");
      </xsl:for-each>

        requestParams = requestParamsHook(requestParams);

        var queryString = requestParams.join("&amp;");
        window.location = "?" + queryString;
      }

      function getCurrentFilters() {
        var filters = [];
        <xsl:for-each select="/page/filters/filter">
          filters.push(createFilter("<xsl:value-of select="daisyutil:escape(@facetName)"/>", "<xsl:value-of select="daisyutil:escape(@queryValue)"/>", "<xsl:value-of select="daisyutil:escape(@displayValue)"/>", "<xsl:value-of select="daisyutil:escape(@operator)"/>", "<xsl:value-of select="daisyutil:escape(@isDiscrete)"/>"));
        </xsl:for-each>
        return filters;
      }

      function createFilter(facetName, queryValue, displayValue, operator, isDiscrete) {
        var filter = new Object();
        filter.facetName = facetName;
        filter.queryValue = queryValue;
        filter.displayValue = displayValue;
        filter.operator = operator;
        filter.isDiscrete = isDiscrete;
        return filter;
      }

      function showChunk(chunkOffset, chunkLength) {
        sendRequest(null, chunkOffset, chunkLength);
        return false;
      }
      
      function getCurrentFacetConfs() {
        var facetConfs = [
        <xsl:for-each select="facetConfs/facetConf">
          <xsl:if test="position() > 1">,</xsl:if>
          createFacetConf(<xsl:value-of select="@maxValues"/>, <xsl:value-of select="@sortOnValue"/>, <xsl:value-of select="@sortAscending"/>)
        </xsl:for-each>
        ];
        return facetConfs;
      }

      function createFacetConf(maxValues, sortOnValue, sortAscending) {
        var facetConf = new Object();
        facetConf.maxValues = maxValues;
        facetConf.sortOnValue = sortOnValue;
        facetConf.sortAscending = sortAscending;
        return facetConf;
      }

      function showMoreValues(facetIndex) {
        var facetConfs = getCurrentFacetConfs();
        facetConfs[facetIndex].maxValues = facetConfs[facetIndex].maxValues + 10;
        sendRequest(null, null, null, facetConfs);
        return false;
      }

      function changeFacetSort(facetIndex, sortOnValue, sortAscending) {
        var facetConfs = getCurrentFacetConfs();
        facetConfs[facetIndex].sortOnValue = sortOnValue;
        facetConfs[facetIndex].sortAscending = sortAscending;
        sendRequest(null, null, null, facetConfs);
        return false;
      }
    </script>

    <xsl:call-template name="requestParamsHook"/>
  </xsl:template>

  <xsl:template name="requestParamsHook">
    <script>
      function requestParamsHook(requestParams) {
          return requestParams;
      }
    </script>
  </xsl:template>

</xsl:stylesheet>
