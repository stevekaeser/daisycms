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
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:fo="http://www.w3.org/1999/XSL/Format"
  xmlns:d="http://outerx.org/daisy/1.0"
  xmlns:p="http://outerx.org/daisy/1.0#publisher"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  version="1.0">

  <!-- Include custom query result stylings -->
  <xsl:include href="daisyskin:query-styling/query-styling-xslfo.xsl"/>

  <xsl:include href="daisyskin:xslt/xslfo-styles.xsl"/>
  
  <xsl:variable name="context" select="/document/context"/>
  <!-- 
    Place footnotes at the end of the document due to issues in FOP 
    Fop has problems with certain content and footnotes causing an endless loop
    This setup does not use the footnote block but tries to mimic it. 
  -->
  <xsl:variable name="footnotes-at-end" select="true()"/>

  <xsl:template match="document">
    <xsl:choose>
      <xsl:when test="p:publisherResponse/d:document">
        <xsl:apply-templates select="p:publisherResponse/d:document"/>
      </xsl:when>
      <xsl:otherwise>
        <fo:block>The publisher request used for this document is missing a &lt;p:prepareDocument/&gt; request.</fo:block>      
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="d:document">
    <fo:block xsl:use-attribute-sets="title"><xsl:value-of select="@name"/></fo:block>
    <xsl:apply-templates select="d:parts/d:part"/>
    <xsl:apply-templates select="d:links"/>
    <xsl:apply-templates select="d:fields"/>
    
    <xsl:variable name="htmlParts" select="d:parts/d:part[@daisyHtml='true']"/>
    <xsl:if test="$footnotes-at-end">
      <xsl:call-template name="insertFootNotes">
        <xsl:with-param name="footnotes" select="$htmlParts//span[@class='footnote'] | $htmlParts//a[not(starts-with(@href, '#'))]"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <xsl:template match="d:links">
    <xsl:if test="d:link">
      <fo:block xsl:use-attribute-sets="h1"><i18n:text key="document.links-title"/></fo:block>
      <xsl:for-each select="d:link">
        <fo:block xsl:use-attribute-sets="p">
          <xsl:value-of select="@title"/>
          <fo:block/>
          <fo:basic-link
            color="blue"
            external-destination="url('{@target}')">
            <xsl:value-of select="@target"/>
          </fo:basic-link>
        </fo:block>
      </xsl:for-each>
    </xsl:if>
  </xsl:template>

  <xsl:template match="d:fields">
    <xsl:if test="d:field">
      <fo:block xsl:use-attribute-sets="h1"><i18n:text key="document.fields-title"/></fo:block>
      <fo:table xsl:use-attribute-sets="table" table-layout="fixed" width="100%">
        <fo:table-column column-number="1" column-width="proportional-column-width(1)"/>
        <fo:table-column column-number="2" column-width="proportional-column-width(2)"/>
        <fo:table-body>
          <fo:table-row keep-with-next="always">
            <fo:table-cell xsl:use-attribute-sets="th"><fo:block><i18n:text key="document.fields.name"/></fo:block></fo:table-cell>
            <fo:table-cell xsl:use-attribute-sets="th"><fo:block><i18n:text key="document.fields.value"/></fo:block></fo:table-cell>
          </fo:table-row>
          <xsl:for-each select="d:field">
            <fo:table-row>
              <fo:table-cell xsl:use-attribute-sets="td"><fo:block><xsl:value-of select="@label"/></fo:block></fo:table-cell>
              <fo:table-cell xsl:use-attribute-sets="td">
                <xsl:variable name="isHierarchical" select="@hierarchical = 'true'"/>
                <xsl:for-each select="*">
                  <fo:block>
                    <xsl:choose>
                      <xsl:when test="$isHierarchical">
                        <xsl:for-each select="*">
                          <xsl:if test="position() > 1"> / </xsl:if>
                          <xsl:value-of select="@valueFormatted"/>
                        </xsl:for-each>
                      </xsl:when>
                      <xsl:otherwise>
                        <xsl:value-of select="@valueFormatted"/>
                      </xsl:otherwise>
                    </xsl:choose>
                  </fo:block>
                </xsl:for-each>
              </fo:table-cell>
            </fo:table-row>
          </xsl:for-each>
        </fo:table-body>
      </fo:table>
    </xsl:if>
  </xsl:template>

  <xsl:template match="d:part">
    <xsl:choose>
      <xsl:when test="@daisyHtml = 'true'">
        <xsl:apply-templates select="html/body/node()"/>
      </xsl:when>
      <xsl:otherwise>
        <fo:block xsl:use-attribute-sets="h1"><i18n:text key="document.part.title"/> "<xsl:value-of select="@label"/>"</fo:block>
        <fo:block>Mime-type: <xsl:value-of select="@mimeType"/>, <i18n:text key="document.part.size"/>: <xsl:value-of select="@size"/> bytes</fo:block>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="p">
    <xsl:call-template name="makePara"/>
  </xsl:template>

  <xsl:template name="makePara">
    <xsl:param name="spaceBefore">-1</xsl:param>
    <xsl:param name="spaceAfter">-1</xsl:param>
    <fo:block xsl:use-attribute-sets="p">
      <xsl:if test="@align">
        <xsl:attribute name="text-align"><xsl:value-of select="@align"/></xsl:attribute>
      </xsl:if>
      <xsl:if test="$spaceBefore != -1">
        <xsl:attribute name="space-before"><xsl:value-of select="$spaceBefore"/></xsl:attribute>
      </xsl:if>
      <xsl:if test="$spaceAfter != -1">
        <xsl:attribute name="space-after"><xsl:value-of select="$spaceAfter"/></xsl:attribute>
      </xsl:if>
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <!-- The priority = 1 makes that these templates take precedence
       over the templates further on that handle p's in table cells. -->
  <xsl:template match="p[@class='warn']" priority="1">
    <xsl:call-template name="makeSpecialPara">
      <xsl:with-param name="name"><i18n:text key="document.warning"/></xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="p[@class='note']" priority="1">
    <xsl:call-template name="makeSpecialPara">
      <xsl:with-param name="name"><i18n:text key="document.note"/></xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="p[@class='fixme']" priority="1">
    <xsl:call-template name="makeSpecialPara">
      <xsl:with-param name="name"><i18n:text key="document.fixme"/></xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="pre[@class='include']" priority="1">
    <xsl:call-template name="makeSpecialPara">
      <xsl:with-param name="name"><i18n:text key="document.unprocessed-include-directive"/></xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="p[@class='daisy-error']" priority="1">
    <xsl:call-template name="makeSpecialPara">
      <xsl:with-param name="name"><i18n:text key="document.error"/></xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="makeSpecialPara">
    <xsl:param name="name"/>
    <fo:block
      margin="0.5cm"
      padding="2mm"
      space-before="3pt"
      space-after="6pt"
      border-style="solid"
      border-width=".1mm"
      border-color="{$colorMediumGray}">
      <fo:block xsl:use-attribute-sets="p.title"><xsl:copy-of select="$name"/></fo:block>
      <xsl:call-template name="makePara"/>
    </fo:block>
  </xsl:template>

  <xsl:template match="li/p">
    <fo:block space-after="4pt">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="blockquote">
    <fo:block xsl:use-attribute-sets="blockquote">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="h1">
    <fo:block xsl:use-attribute-sets="h1">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="h2">
    <fo:block xsl:use-attribute-sets="h2">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="h3">
    <fo:block xsl:use-attribute-sets="h3">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="h4">
    <fo:block xsl:use-attribute-sets="h4">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="h5">
    <fo:block xsl:use-attribute-sets="h5">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="a">
    <xsl:variable name="footnoteNumber"><xsl:number count="span[@class='footnote'] | a" level="any"/></xsl:variable>
    <xsl:variable name="fullHref"><xsl:if test="starts-with(@href,'/')"><xsl:value-of select="$context/request/@server"/></xsl:if><xsl:value-of select="@href"/></xsl:variable>
    <fo:basic-link
      color="blue"
      external-destination="url('{$fullHref}')">
      <xsl:apply-templates/>
    </fo:basic-link>
    <xsl:if test="@href != string(.)">
      <xsl:call-template name="insertFootnote">
        <xsl:with-param name="footnoteNumber" select="$footnoteNumber"/>
        <xsl:with-param name="footnoteText">
          <xsl:value-of select="$fullHref"/>
          <xsl:if test="p:linkInfo/@documentName">
            <xsl:value-of select="concat(' (',p:linkInfo/@documentName,')')"/>
          </xsl:if>
        </xsl:with-param>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <xsl:template match="p:linkInfo" mode="PartContent">
    <!-- empty template, becuase p:linkInfo should be removed from output -->
  </xsl:template>

  <xsl:template match="img">
    <xsl:choose>
      <xsl:when test="@daisy-caption != ''">
        <fo:block>
          <xsl:call-template name="insertGraphic"/>
          <fo:block xsl:use-attribute-sets="img.caption">
            <xsl:value-of select="@daisy-caption"/>
          </fo:block>
        </fo:block>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="insertGraphic"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="insertGraphic">
    <fo:external-graphic src="url('{@src}')">
      <xsl:choose>
        <xsl:when test="not(@print-width) and not(@print-height)">
          <!-- This special handling is needed for FOP 0.9.4 which doesn't automatically shrink images (DSY-553) -->
          <xsl:attribute name="width">100%</xsl:attribute>
          <xsl:attribute name="content-width">scale-to-fit</xsl:attribute>
          <xsl:attribute name="content-height">100%</xsl:attribute>
          <xsl:attribute name="scaling">uniform</xsl:attribute>
        </xsl:when>
        <xsl:when test="@print-width">
          <xsl:attribute name="width"><xsl:value-of select="@print-width"/></xsl:attribute>
          <xsl:attribute name="content-width"><xsl:value-of select="@print-width"/></xsl:attribute>
        </xsl:when>
        <xsl:when test="@print-height">
          <xsl:attribute name="height"><xsl:value-of select="@print-height"/></xsl:attribute>
          <xsl:attribute name="content-height"><xsl:value-of select="@print-height"/></xsl:attribute>
        </xsl:when>
      </xsl:choose>
    </fo:external-graphic>
  </xsl:template>

  <xsl:template match="pre">
    <fo:block xsl:use-attribute-sets="pre">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="br">
    <fo:block>
    </fo:block>
  </xsl:template>

  <xsl:template match="em">
    <fo:inline font-style="italic">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="strong">
    <fo:inline font-weight="bold">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="tt">
    <fo:inline font-family="monospace">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="del">
    <fo:inline text-decoration="line-through">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="sub">
    <fo:inline vertical-align="sub" font-size="7pt">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="sup">
    <fo:inline vertical-align="super" font-size="7pt">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="ul/ul | ul/ol | ol/ul | ol/ol">
      <fo:list-item xsl:use-attribute-sets="list.item">
          <fo:list-item-label>
              <fo:block/>
          </fo:list-item-label>
          <fo:list-item-body start-indent="body-start()">
              <fo:list-block>
                  <xsl:apply-templates select="*"/>
              </fo:list-block>
          </fo:list-item-body>
      </fo:list-item>
  </xsl:template>

  <xsl:template match="ul | ol">
      <fo:list-block xsl:use-attribute-sets="list">
          <xsl:apply-templates select="*"/>
      </fo:list-block>
  </xsl:template>

  <xsl:template match="ul/li">
      <fo:list-item xsl:use-attribute-sets="list.item">
          <fo:list-item-label>
              <fo:block>
                  <xsl:if test="not(@style='list-style: none')">
                      <xsl:text>&#8226;</xsl:text>
                  </xsl:if>
              </fo:block>
          </fo:list-item-label>
          <fo:list-item-body start-indent="body-start()">
              <fo:block>
                  <xsl:apply-templates/>
              </fo:block>
          </fo:list-item-body>
      </fo:list-item>
  </xsl:template>

  <xsl:template match="ol/li">
      <fo:list-item xsl:use-attribute-sets="list.item">
          <fo:list-item-label end-indent="label-end()">
              <fo:block text-align="end">
                  <xsl:variable name="requestedStartNumber" select="../@start"/>
                   <xsl:variable name="startNumber">
                     <xsl:choose>
                       <xsl:when test="$requestedStartNumber != ''">
                         <xsl:value-of select="$requestedStartNumber"/>
                       </xsl:when>
                       <xsl:otherwise>
                         <xsl:text>1</xsl:text>
                       </xsl:otherwise>
                     </xsl:choose>
                   </xsl:variable>
                <xsl:call-template name="render-ol-item-label">
                  <xsl:with-param name="number" select="count(preceding-sibling::li) + number($startNumber)"/>
                </xsl:call-template>
              </fo:block>
          </fo:list-item-label>
          <fo:list-item-body start-indent="body-start()">
              <fo:block>
                  <xsl:apply-templates/>
              </fo:block>
          </fo:list-item-body>
      </fo:list-item>
  </xsl:template>
  
  <xsl:template name="render-ol-item-label">
    <xsl:param name="number"/>
    <xsl:choose>
      <xsl:when test="$number &lt; 0"><!-- negative numbers result in exceptions and generally inconsistent behaviour -->
        <xsl:number format="1." value="$number"/>
      </xsl:when>
      <xsl:when test="contains(concat(' ', ../@class, ' '), ' dsy-liststyle-lower-latin ')">
        <xsl:number format="a." value="$number"/>
      </xsl:when>
      <xsl:when test="contains(concat(' ', ../@class, ' '), ' dsy-liststyle-upper-latin ')">
        <xsl:number format="A." value="$number"/>
      </xsl:when>
      <xsl:when test="contains(concat(' ', ../@class, ' '), ' dsy-liststyle-lower-roman ')">
        <xsl:number format="i." value="$number"/>
      </xsl:when>
      <xsl:when test="contains(concat(' ', ../@class, ' '), ' dsy-liststyle-upper-roman ')">
        <xsl:number format="I." value="$number"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:number format="1." value="$number"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="table">
    <xsl:choose>
      <xsl:when test="@daisy-caption != ''">
        <fo:block space-before="3pt">
          <fo:block xsl:use-attribute-sets="table.caption">
            <xsl:value-of select="@daisy-caption"/>
          </fo:block>
          <xsl:call-template name="insertTable"/>
        </fo:block>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="insertTable"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="insertTable">
    <fo:table table-layout="fixed" xsl:use-attribute-sets="table">
      <xsl:attribute name="width">
        <xsl:choose>
          <xsl:when test="@print-width"><xsl:value-of select="@print-width"/></xsl:when>
          <xsl:otherwise>100%</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <!-- Note: the computedInfo element is inserted by a Cocoon transformer, see pipeline definition. -->
      <xsl:variable name="columnCount" select="computedInfo/@maxColumns"/>
      <xsl:choose>
        <xsl:when test="computedInfo/print">
          <xsl:for-each select="computedInfo/print/col">
            <fo:table-column column-number="{position()}">
              <xsl:attribute name="column-width">
                <xsl:choose>
                  <xsl:when test="contains(@width, '*')"><xsl:value-of select="concat('proportional-column-width(', substring-before(@width, '*'), ')')"/></xsl:when>
                  <xsl:when test="@width"><xsl:value-of select="@width"/></xsl:when>
                  <xsl:otherwise>proportional-column-width(1)</xsl:otherwise>
                </xsl:choose>
              </xsl:attribute>
            </fo:table-column>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="makeTableColumn">
            <xsl:with-param name="number" select="$columnCount"/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
      <fo:table-body>
        <xsl:apply-templates select="tbody/tr | tr"/>
      </fo:table-body>
    </fo:table>
  </xsl:template>

  <xsl:template name="makeTableColumn">
    <xsl:param name="number"/>
    <xsl:if test="$number > 1">
      <xsl:call-template name="makeTableColumn">
        <xsl:with-param name="number" select="$number -1"/>
      </xsl:call-template>
    </xsl:if>
    <fo:table-column column-number="{$number}" column-width="proportional-column-width(1)"/>
  </xsl:template>

  <xsl:template match="tr">
    <fo:table-row>
      <!-- Keep first row of a table always with the next, if it contains header cells -->
      <xsl:if test="position() = 1 and th">
        <xsl:attribute name="keep-with-next">always</xsl:attribute>
      </xsl:if>
      <xsl:apply-templates select="td|th"/>
    </fo:table-row>
  </xsl:template>

  <xsl:template match="td">
    <!-- This seems like rather expensive to do for every cell, but what's the alternative? -->
    <xsl:variable name="containingTableClass" select="string(ancestor::table/@class)"/>
    <xsl:choose>
      <xsl:when test="$containingTableClass = 'borderless'">
        <fo:table-cell xsl:use-attribute-sets="td.borderless">
          <xsl:call-template name="cellContent"/>
        </fo:table-cell>
      </xsl:when>
      <xsl:otherwise>
        <fo:table-cell xsl:use-attribute-sets="td">
          <xsl:call-template name="cellContent"/>
        </fo:table-cell>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="th">
    <xsl:variable name="containingTableClass" select="string(ancestor::table/@class)"/>
    <xsl:choose>
      <xsl:when test="$containingTableClass = 'borderless'">
        <fo:table-cell xsl:use-attribute-sets="th.borderless">
          <xsl:call-template name="cellContent"/>
        </fo:table-cell>
      </xsl:when>
      <xsl:otherwise>
        <fo:table-cell xsl:use-attribute-sets="th">
          <xsl:call-template name="cellContent"/>
        </fo:table-cell>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="cellContent">
    <xsl:if test="@colspan > 1">
      <xsl:attribute name="number-columns-spanned"><xsl:value-of select="@colspan"/></xsl:attribute>
    </xsl:if>
    <xsl:if test="@rowspan > 1">
      <xsl:attribute name="number-rows-spanned"><xsl:value-of select="@rowspan"/></xsl:attribute>
    </xsl:if>
    <fo:block>
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="td/p[position() = 1 and position() = last()]">
    <xsl:call-template name="makePara">
      <xsl:with-param name="spaceBefore">0cm</xsl:with-param>
      <xsl:with-param name="spaceAfter">0cm</xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="td/p[position() = 1]">
    <xsl:call-template name="makePara">
      <xsl:with-param name="spaceBefore">0cm</xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="td/p[position() = last()]">
    <xsl:call-template name="makePara">
      <xsl:with-param name="spaceAfter">0cm</xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="th/p[position() = 1 and position() = last()]">
    <xsl:call-template name="makePara">
      <xsl:with-param name="spaceBefore">0cm</xsl:with-param>
      <xsl:with-param name="spaceAfter">0cm</xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="th/p[position() = 1]">
    <xsl:call-template name="makePara">
      <xsl:with-param name="spaceBefore">0cm</xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="th/p[position() = last()]">
    <xsl:call-template name="makePara">
      <xsl:with-param name="spaceAfter">0cm</xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="d:searchResult">
    <fo:table xsl:use-attribute-sets="table" table-layout="fixed" width="100%">
      <xsl:for-each select="d:titles/d:title">
        <fo:table-column column-number="position()" column-width="proportional-column-width(1)"/>
      </xsl:for-each>
      <fo:table-body>
        <fo:table-row>
          <xsl:for-each select="d:titles/d:title">
            <fo:table-cell xsl:use-attribute-sets="th"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
          </xsl:for-each>
        </fo:table-row>
        <xsl:apply-templates select="d:rows/d:row"/>
      </fo:table-body>
    </fo:table>
  </xsl:template>

  <xsl:template match="d:row">
    <fo:table-row>
      <xsl:for-each select="d:value|d:xmlValue|d:multiValue|d:linkValue|d:hierarchyValue">
        <fo:table-cell xsl:use-attribute-sets="td">
          <fo:block>
            <xsl:apply-templates select="."/>
          </fo:block>
        </fo:table-cell>
      </xsl:for-each>
    </fo:table-row>
  </xsl:template>

  <xsl:template match="d:value|d:linkValue">
    <xsl:value-of select="."/>
  </xsl:template>

  <xsl:template match="d:multiValue">
    <xsl:for-each select="*">
      <xsl:if test="position() > 1">, </xsl:if>
      <xsl:apply-templates select="."/>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="d:xmlValue">
    <xsl:apply-templates select="html/body/node()"/>
  </xsl:template>

  <xsl:template match="d:hierarchyValue">
    <xsl:for-each select="*">
      <xsl:if test="position() > 1"> / </xsl:if>
      <xsl:apply-templates select="."/>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="p:daisyPreparedInclude">
    <xsl:copy-of select="."/>
  </xsl:template>

  <xsl:template match="span[@class='footnote']">
    <xsl:variable name="footnoteNumber"><xsl:number count="span[@class='footnote'] | a" level="any"/></xsl:variable>
    <xsl:call-template name="insertFootnote">
      <xsl:with-param name="footnoteNumber" select="$footnoteNumber"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="insertFootNotes">
    <xsl:param name="footnotes"/>
    <xsl:if test="count($footnotes) > 0">
      <fo:block xsl:use-attribute-sets="h3"><i18n:text key="notes"/></fo:block>
      <xsl:for-each select="$footnotes">
        <fo:list-block provisional-label-separation="0pt"
                       provisional-distance-between-starts="18pt"
                       space-after.optimum="6pt"
                       start-indent="0cm">
            <fo:list-item>
              <fo:list-item-label end-indent="label-end()">
                <fo:block xsl:use-attribute-sets="footnote.text"><xsl:value-of select="position()"/><xsl:text>.</xsl:text></fo:block>
              </fo:list-item-label>
              <fo:list-item-body start-indent="body-start()">
                <fo:block xsl:use-attribute-sets="footnote.text">
                  <xsl:choose>
                    <xsl:when test="local-name(.) = 'a'">
                      <xsl:value-of select="@href"/>
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:apply-templates/>
                    </xsl:otherwise>
                  </xsl:choose>
                </fo:block>
              </fo:list-item-body>
            </fo:list-item>
        </fo:list-block>
      </xsl:for-each>
    </xsl:if>
  </xsl:template>

  <xsl:template name="insertFootnote">
    <xsl:param name="footnoteNumber"/>
    <xsl:param name="footnoteText"/>
    
    <xsl:choose>
    	<xsl:when test="$footnotes-at-end">
          <fo:inline xsl:use-attribute-sets="footnote.ref"><xsl:value-of select="$footnoteNumber"/></fo:inline>
    	</xsl:when>
    	<xsl:otherwise>
          <fo:footnote>
            <fo:inline xsl:use-attribute-sets="footnote.ref"><xsl:value-of select="$footnoteNumber"/></fo:inline>
            <fo:footnote-body>
              <fo:list-block provisional-label-separation="0pt"
                  provisional-distance-between-starts="18pt"
                  space-after.optimum="6pt"
                  start-indent="0cm">
                <fo:list-item>
                  <fo:list-item-label end-indent="label-end()">
                    <fo:block xsl:use-attribute-sets="footnote.text"><xsl:value-of select="$footnoteNumber"/><xsl:text>.</xsl:text></fo:block>
                  </fo:list-item-label>
                  <fo:list-item-body start-indent="body-start()">
                    <fo:block xsl:use-attribute-sets="footnote.text">
                      <xsl:choose>
                        <xsl:when test="$footnoteText">
                          <xsl:value-of select="$footnoteText"/>
                        </xsl:when>
                        <xsl:otherwise>
                          <xsl:apply-templates/>
                        </xsl:otherwise>
                      </xsl:choose>
                    </fo:block>
                  </fo:list-item-body>
                </fo:list-item>
                </fo:list-block>
            </fo:footnote-body>
          </fo:footnote>
    	</xsl:otherwise>
    </xsl:choose>
  </xsl:template>
 
  <xsl:template match="span[@class='indexentry']">
    <!-- remove index entries from output -->
  </xsl:template>

  <xsl:template match="span[@class='crossreference']">
    (crossreference)
  </xsl:template>

  <xsl:template match="span[@class='daisy-unresolved-variable']">
    <fo:inline xsl:use-attribute-sets="unresolved-variable">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

</xsl:stylesheet>