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

  <xsl:include href="daisyskin:xslt/searchresult.xsl"/>
  
  <xsl:variable name="mountPoint" select="string(page/context/mountPoint)"/>
  <xsl:variable name="siteName" select="string(page/context/site/@name)"/>
  <xsl:variable name="navigationPath" select="concat($mountPoint, '/', $siteName)"/>
  <xsl:variable name="siteBranchId" select="string(page/context/site/@branchId)"/>
  <xsl:variable name="siteLanguageId" select="string(page/context/site/@languageId)"/>

  <xsl:template match="page">
    <xsl:choose>
      <xsl:when test="d:searchResult/d:rows/d:row">
        <xsl:apply-templates select="d:searchResult"/>
      </xsl:when>
      <xsl:otherwise>
        <strong><i18n:text key="fulltextresult.no-docs-found"/></strong>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="d:searchResult">
    <script type="text/javascript">
      function goToOffset(offset) {
        var form = document.getElementById('searchForm');
        var chunkOffset = document.getElementById('offset:input');
        chunkOffset.value = offset;
        form.submit();
      }
    </script>

    <xsl:apply-templates select="d:resultInfo" mode="top"/>

    <xsl:apply-templates select="d:rows/d:row"/>

    <xsl:apply-templates select="d:resultInfo" mode="bottom"/>

    <xsl:if test="/page/context/user/name != 'guest'">
      <div>
        <form name="daisyquery" method="POST" action="{$mountPoint}/{$siteName}/querySearch">
          <input type="hidden" name="addToDocumentBasket" value="true"/>
          <input type="hidden" name="daisyquery" value="{d:executionInfo/d:query/text()}"/>
          <input type="submit" i18n:attr="value" value="searchresult.add-to-basket"/>
        </form>
      </div>
      
      <br/>
      
      <div>
        <form name="daisyquery" method="GET" action="{$mountPoint}/{$siteName}/searchAndReplace">
          <input type="hidden" name="query" value="{d:executionInfo/d:query/text()}"/>
          <input type="hidden" name="needle" value=""/>
          <input type="submit" i18n:attr="value" value="searchresult.start-search-and-replace" onclick="this.form['needle'].value=document.getElementById('query:input').value;"/>
        </form>
      </div>
    </xsl:if>
    
    <div style="font-size: x-small">
      <xsl:call-template name="createInfo">
        <xsl:with-param name="info" select="d:executionInfo"/>
      </xsl:call-template>
    </div>
  </xsl:template>

  <xsl:template match="d:row">
    <!-- Construct the href smartly: only include branch and language
         if they're different then the site's default -->
    <xsl:variable name="href">
      <xsl:value-of select="concat($navigationPath, '/', @documentId, '.html')"/>
      <xsl:choose>
        <xsl:when test="@branchId != $siteBranchId">
          <xsl:value-of select="concat('?branch=', @branchId)"/>
          <xsl:if test="@languageId != $siteLanguageId">
            <xsl:value-of select="concat('&amp;language=', @languageId)"/>
          </xsl:if>
        </xsl:when>
        <xsl:when test="@languageId != $siteLanguageId">
          <xsl:value-of select="concat('?language=', @languageId)"/>
        </xsl:when>
      </xsl:choose>
    </xsl:variable>

    <a href="{$href}"><xsl:value-of select="d:value[1]"/></a><br/>
    
    <xsl:variable name="ftscore" select="d:value[4]"/>     
    <div class="dsyft-scorebox">
      <div class="dsyft-score" style="width: {$ftscore}%">&#160;</div>
    </div>      

    <xsl:choose>
      <xsl:when test="d:xmlValue[1]/html/body/node()">
        <xsl:call-template name="fulltext-fragments">
          <xsl:with-param name="fragments" select="d:xmlValue[1]"></xsl:with-param>
        </xsl:call-template>        
      </xsl:when>
      <xsl:otherwise>
        <div class="fulltext-doc-summary"><xsl:value-of select="d:value[2]"/></div>
      </xsl:otherwise>
    </xsl:choose>
    
    <div class="dsyft-infoline">
      <i18n:translate>
        <i18n:text key="fulltextresult.infoline"/>
        <i18n:param><xsl:value-of select="d:value[3]"/></i18n:param>
        <i18n:param><xsl:value-of select="@documentId"/></i18n:param>
      </i18n:translate>
    </div>

    <br/>
  </xsl:template>
  
  <xsl:template name="fulltext-fragments">
    <xsl:param name="fragments"/>
    
    <div class="fulltext-fragment">
      <xsl:for-each select="$fragments/html/body/div">
          <xsl:copy-of select="./node()"/>
          <xsl:if test="position() != last()">
            <b> ... </b>
          </xsl:if>
      </xsl:for-each>
    </div>
  </xsl:template>
  
  <xsl:template match="d:resultInfo" mode="top">
    <div class="dsyft-info">
      <i18n:translate>
        <i18n:text key="fulltextresult.results"/>
        <i18n:param>
          <xsl:value-of select="concat(@chunkOffset, ' - ', @chunkOffset + @chunkLength - 1)"/>
        </i18n:param>
        <i18n:param>
          <xsl:value-of select="@size"/>
        </i18n:param>
      </i18n:translate>
    </div>
  </xsl:template>
  
  <xsl:template match="d:resultInfo" mode="bottom">
    <div class="dsyft-pages">
      <div class="dsyft-pagestitle"><i18n:text key="fulltextresult.resultspage"/></div>
      <xsl:if test="@chunkOffset &gt; @chunkLength">
        <a onclick="goToOffset({@chunkOffset - @requestedChunkLength}); return false;" href="#" onmouseover="window.status=''; return true;" class="dsyft-pagelink">
          <span class="dsyft-prevb dsyft-navlink"><i18n:text key="prev"/></span>
        </a>
      </xsl:if>
      <xsl:call-template name="pages">      
        <xsl:with-param name="size"><xsl:value-of select="@size"/></xsl:with-param>
        <xsl:with-param name="offset"><xsl:value-of select="@chunkOffset"/></xsl:with-param>
        <xsl:with-param name="length"><xsl:value-of select="@requestedChunkLength"/></xsl:with-param>
      </xsl:call-template>
      <xsl:if test="@chunkOffset + @chunkLength &lt;= @size">
        <a onclick="goToOffset({@chunkOffset + @requestedChunkLength}); return false;" href="#" onmouseover="window.status=''; return true;" class="dsyft-pagelink">
          <span class="dsyft-nextb dsyft-navlink"><i18n:text key="next"/></span>
        </a>
      </xsl:if>
    </div>
  </xsl:template>
  
  <xsl:template name="pages">
    <xsl:param name="position">0</xsl:param>
    <xsl:param name="size"/>
    <xsl:param name="offset"/>
    <xsl:param name="length"/>    
    
    <xsl:variable name="maxPage">7</xsl:variable>
    <xsl:variable name="currPage"><xsl:value-of select="$offset div $length"/></xsl:variable>
    
    <xsl:if test="(($position &gt; $size div $length - $maxPage) or ($position &gt; $currPage - $maxPage div 2)) and (($position &lt; $maxPage) or ($position &lt; $currPage + $maxPage div 2))">
    
      <xsl:choose>
        <xsl:when test="$position * $length = $offset - 1">
          <span class="dsyft-activepage dsyft-pagelink"><xsl:value-of select="$position + 1"/></span>
        </xsl:when>
        <xsl:otherwise>
          <a onclick="goToOffset({$position * $length + 1}); return false;" href="#" onmouseover="window.status=''; return true;" class="dsyft-pagelink">
            <span class="dsyft-navlink"><xsl:value-of select="$position + 1"/></span>
          </a>
        </xsl:otherwise>
      </xsl:choose>
      
    </xsl:if>
    
    <xsl:if test="$length * ($position+1) &lt; $size">      
      <xsl:call-template name="pages">
        <xsl:with-param name="position"><xsl:value-of select="$position + 1"/></xsl:with-param>
        <xsl:with-param name="size"><xsl:value-of select="$size"/></xsl:with-param>
        <xsl:with-param name="offset"><xsl:value-of select="$offset"/></xsl:with-param>
        <xsl:with-param name="length"><xsl:value-of select="$length"/></xsl:with-param>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>