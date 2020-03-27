<?xml version="1.0"?>
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
  xmlns:d="http://outerx.org/daisy/1.0" xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns:urlencoder="xalan://java.net.URLEncoder"
  xmlns:daisyutil="xalan://org.outerj.daisy.frontend.util.XslUtil">
 
  <xsl:param name="documentBasePath"/>
  <xsl:variable name="context" select="/*/context"/>
  <xsl:variable name="mountPoint" select="$context/mountPoint"/>
  <xsl:variable name="site" select="$context/site/@name"/>
  <xsl:variable name="skin" select="$context/skin"/>
  <xsl:template match="d:searchResult[@styleHint='bullets']">
    <ul>
      <xsl:for-each select="d:rows/d:row">
        <li><a href="{$searchResultBasePath}{@documentId}.html?branch={@branchId}&amp;language={@languageId}"><xsl:value-of select="d:value[1]"/></a></li>
      </xsl:for-each>
    </ul>
  </xsl:template>

  <!--
     Note that not all query features are supported by this query styling
  -->
  <xsl:template match="d:searchResult[starts-with(@styleHint,'chunked')]">
    <!-- If your query uses the ContextDoc() function try passing the context document you had in mind along -->
    <xsl:param name="contextDocument"/>
    <xsl:call-template name="chunked">
      <xsl:with-param name="contextDocument" select="$contextDocument"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="chunked">
    <xsl:param name="contextDocument"/>
    <link rel="stylesheet" type="text/css" href="{$mountPoint}/resources/skins/{$skin}/css/docbrowser.css"/>

    <!-- these sourced scripts should really be moved to layout.xsl. Look into the jquery lazy loading plugin -->
    <script type="text/javascript" src="{$mountPoint}/resources/js/jquery/jquery-1.2.6.js"></script>
    <script type="text/javascript" src="{$mountPoint}/resources/js/jquery/uritemplate.js"></script>
    <script type="text/javascript" src="{$mountPoint}/resources/js/jquery/jquery.docbrowser.js"></script>
    <script>
        
        $(function(){
          var resultsId = '<xsl:value-of select="generate-id(.)"/>';
          var versionMode = '<xsl:value-of select="$context/versionMode"/>';
          var chunkLength = Number('<xsl:value-of select="d:resultInfo/@requestedChunkLength"/>');
          
          if(chunkLength==0)
            chunkLength = Number('<xsl:value-of select="d:resultInfo/@size"/>');
          
          var baseQuery = "<xsl:value-of select='daisyutil:escape(d:executionInfo/d:query)'/>";
          var searchResultBasePath = "<xsl:value-of select='daisyutil:escape($searchResultBasePath)'/>";
          
          
          var columns=[];
          <xsl:for-each select="d:titles/d:title">
            columns.push('<xsl:value-of select="@name"/>');
          </xsl:for-each>
          
<![CDATA[
            //FixedBaseQuery should be of the form "select <columns> where <conditions>" (no limit, sorting and options may be present)
            var FixedBaseQuery = function(baseQuery) {
              this.baseQuery = baseQuery;
              this.sortClauses = [];
              this.options = [];
            };
            
            FixedBaseQuery.prototype.addSortClause = function(sortClause) {
              this.sortClauses.push(sortClause);
            }
            
            FixedBaseQuery.prototype.addOption = function(option) {
              this.options.push(option);
            }
            
            FixedBaseQuery.prototype.setLimitClause = function(limitClause) {
              this.limitClause = limitClause;
            }
            
            FixedBaseQuery.prototype.toString = function() {
              var result = this.baseQuery;
              return result;
            }
            
            var qs = { getBaseQuery: function() {
              var q = new FixedBaseQuery(baseQuery);
              return q;
            }};
   ]]>  
            var pageMap = $.ui.getPageMap(window.location.href);
            var pageNumber = pageMap[resultsId] ? pageMap[resultsId] : 1;
            var options =  {
                columns: columns,
                columnLabels: (typeof columnLabels !== "undefined")?columnLabels:undefined,
                columnActions: (typeof columnActions !== "undefined")?columnActions:undefined,
                previewUrl: daisy.mountPoint + "/" + daisy.site.name + "/<xsl:value-of select="$contextDocument/@id"/>/version/<xsl:value-of select="$context/versionMode"/>?layoutType=plain&amp;branch=" + daisy.site.branchId + "&amp;language=" + daisy.site.languageId,
                chunkLength: chunkLength,
                multiSelect: false,
                querySource: qs,                
                versionMode : '<xsl:value-of select="$context/versionMode"/>',
                dataAvailable: true,                               
                searchResultBasePath: searchResultBasePath,
      <xsl:if test="$contextDocument">
                contextDocument: "<xsl:value-of select="concat('daisy:', $contextDocument/@id, '@', $contextDocument/@branchId, ':', $contextDocument/@languageId, ':', $contextDocument/@dataVersionId)"/>", 
      </xsl:if>
                startMode: 'tableMode'
            };
            if (pageNumber > 1) {
              options.page = pageNumber;
              options.loadImmediately = true;
              options.dataAvailable = false;
                 
            }

            $("#" + resultsId).daisyDocumentBrowser(options);
            
          var toggle = $(document.createElement("img"));
          toggle.attr("id", "preview-toggle");            
          
          toggle.attr("src", daisy.mountPoint + "/resources/skins/" + daisy.skin + "/images/preview.png");
          toggle.attr("alt", "toggle mode");
          toggle.attr("title", "<i18n:text key="docbrowser.toggle-preview"/>");
          toggle.click(function() {
            $("#"+resultsId+".daisy-documentbrowser").daisyDocumentBrowser("toggleMode");
          }); 

          $("#searchTitle").append(toggle);            
        });
    </script>

    <div id="{generate-id(.)}">
        <div class="dataresult"  style="visibility: hidden; display:none">
            <xsl:apply-templates select="."  mode="drop-ns"/>
        </div>
    </div>
    
    <div>
      <script>
        function getPdf(docbrowser, currentChunk) {
           var path = "<xsl:value-of select="concat($context/request/@server, $mountPoint,'/',$site,'/querySearch/pdf?')"/>";
<![CDATA[
           var d = $(docbrowser);
           var params = d.daisyDocumentBrowser("getQuerySearchParams", currentChunk).get(0);

           if (!currentChunk) 
               params.optionList = [ "chunk_length=0", "chunk_offset=1" ];
           path += $.param(params);
           window.open(path);
]]>
        }
      </script>
      <input type="button" onclick="getPdf('#{generate-id(.)}', true)" value="docbrowser.export-chunk" i18n:attr="value"/>
      <input type="button" onclick="getPdf('#{generate-id(.)}', false)" value="docbrowser.export-full" i18n:attr="value"/>
    </div>
  </xsl:template>

  <xsl:template match="/|comment()|processing-instruction()" mode="drop-ns">
    <xsl:copy>
      <!-- go process children (applies to root node only) -->
      <xsl:apply-templates mode="drop-ns"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="*" mode="drop-ns">
    <xsl:element name="{local-name()}">
      <!-- go process attributes and children -->
      <xsl:apply-templates select="@*|node()" mode="drop-ns"/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="@*" mode="drop-ns">
    <xsl:attribute name="{local-name()}">
        <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>

</xsl:stylesheet>
