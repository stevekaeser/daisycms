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
  xmlns:d="http://outerx.org/daisy/1.0"
  xmlns:einclude="http://outerx.org/daisy/1.0#externalinclude"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:fo="http://www.w3.org/1999/XSL/Format"
  xmlns:p="http://outerx.org/daisy/1.0#publisher">

  <xsl:include href="xslfo-styles.xsl"/>
  <xsl:param name="publicationTypeName"/>
  <!-- footnotesAtEndOfChapter was a workaround for an IBEX rendering problem.  Since we are using FOP now, the default 'true'
    should be okay.
  -->
  <xsl:variable name="footnotesAtEndOfChapter" select="/html/properties/entry[@key='footnotes-at-end-of-chapter'] = 'true'"/>

  <xsl:template match="html">
    <xsl:apply-templates select="body"/>
  </xsl:template>


  <xsl:template match="body">
    <fo:root>

<!-- 
  current fop version (0.94) does not support fo:title.
  <fo:title><xsl:value-of select="/html/metadata/entry[@key='title']"/></fo:title>
-->
 
      <fo:layout-master-set>
        <fo:simple-page-master master-name="frontpage"
          page-height="29.7cm"
          page-width="21cm"
          margin-top="1cm"
          margin-bottom="1cm"
          margin-left="2.5cm"
          margin-right="2cm">
          <fo:region-body region-name="body"
            margin-bottom="1.5cm" margin-top="1.5cm"/>
        </fo:simple-page-master>

        <fo:simple-page-master master-name="simplepage"
          page-height="29.7cm"
          page-width="21cm"
          margin-top="1cm"
          margin-bottom="1cm"
          margin-left="2.5cm"
          margin-right="2cm">
          <fo:region-body region-name="body"
            margin-bottom="1.5cm" margin-top="1.5cm"/>
          <fo:region-before region-name="top" extent="1.3cm"/>
          <fo:region-after region-name="bottom" extent="1.3cm"/>
        </fo:simple-page-master>

        <fo:simple-page-master master-name="indexpage"
          page-height="29.7cm"
          page-width="21cm"
          margin-top="1cm"
          margin-bottom="1cm"
          margin-left="2.5cm"
          margin-right="2cm">
          <fo:region-body region-name="body"
            margin-bottom="1.5cm" margin-top="1.5cm" column-count="3"/>
          <fo:region-before region-name="top" extent="1.3cm"/>
          <fo:region-after region-name="bottom" extent="1.3cm"/>
        </fo:simple-page-master>
      </fo:layout-master-set>

      <xsl:apply-templates select="toc" mode="pdfbookmarks"/>
 
      <fo:page-sequence master-reference="frontpage" initial-page-number="2" force-page-count="no-force">
        <fo:flow flow-name="body">
          <fo:block font-size="20pt" font-weight="bold" text-align="center" padding-before="5cm">
            <xsl:value-of select="/html/metadata/entry[@key='title']"/>
          </fo:block>
          <fo:block font-size="11pt" text-align="center" padding-before="2cm">
            <xsl:value-of select="/html/properties/entry[@key='publishDate']"/>
          </fo:block>
        </fo:flow>
      </fo:page-sequence>

      <fo:page-sequence master-reference="simplepage" initial-page-number="1" force-page-count="no-force">
        <fo:static-content flow-name="xsl-footnote-separator">
          <fo:block margin="0cm" padding="0cm" start-indent="0cm" end-indent="0cm">
            <fo:leader leader-pattern="rule"
                       leader-length="100%"
                       rule-style="solid"
                       rule-thickness="0.1mm"/>
          </fo:block>
        </fo:static-content>
        <fo:static-content flow-name="top">
          <xsl:call-template name="header"/>
        </fo:static-content>
        <fo:static-content flow-name="bottom">
          <xsl:call-template name="footer"/>
        </fo:static-content>
        <fo:flow flow-name="body">
          <fo:block xsl:use-attribute-sets="body.text">
            <xsl:apply-templates/>

            <!-- Normally the footnotes for the previous chapter are inserted at the start of the next chapter
                (see h1 template), but for the last chapter we have to do it here. -->
            <xsl:if test="$footnotesAtEndOfChapter">
              <xsl:call-template name="insertFootNotes">
                <xsl:with-param name="footnotes" select="descendant::span[@class='footnote' and count(following::h1) = 0] | descendant::a[@href != string(.) and not(starts-with(@href, '#'))  and count(following::h1) = 0]"/>
              </xsl:call-template>
            </xsl:if>

          </fo:block>
        </fo:flow>
      </fo:page-sequence>

      <xsl:if test="index">
        <fo:page-sequence master-reference="indexpage" force-page-count="no-force">
          <fo:flow flow-name="body">
            <xsl:apply-templates select="h1[@id='index']" mode="index"/>
            <fo:block xsl:use-attribute-sets="body.text">
              <xsl:apply-templates select="index" mode="index"/>
            </fo:block>
          </fo:flow>
        </fo:page-sequence>
      </xsl:if>

    </fo:root>
  </xsl:template>

  <xsl:template name="header">
    <fo:block>
      <xsl:if test="/html/properties/entry[@key='logo-available'] = 'true'">
        <xsl:attribute name="text-align"><xsl:value-of select="/html/properties/entry[@key='logo-align']"/></xsl:attribute>
        <xsl:variable name="logoWidth" select="/html/properties/entry[@key='logo-width']"/>
        <xsl:variable name="logoHeight" select="/html/properties/entry[@key='logo-height']"/>
        <fo:external-graphic src="publication:logo">
          <xsl:if test="$logoWidth">
            <xsl:attribute name="width"><xsl:value-of select="$logoWidth"/></xsl:attribute>
            <xsl:attribute name="content-width"><xsl:value-of select="$logoWidth"/></xsl:attribute>
          </xsl:if>
          <xsl:if test="$logoHeight">
            <xsl:attribute name="height"><xsl:value-of select="$logoHeight"/></xsl:attribute>
            <xsl:attribute name="content-height"><xsl:value-of select="$logoHeight"/></xsl:attribute>
          </xsl:if>
        </fo:external-graphic>
      </xsl:if>
    </fo:block>
  </xsl:template>

  <xsl:template name="footer">
    <fo:block border-top-width=".1mm" border-top-style="solid" border-top-color="black" xsl:use-attribute-sets="body.text">
      <fo:table width="100%" table-layout="fixed">
        <fo:table-column column-number="1" column-width="proportional-column-width(4)"/>
        <fo:table-column column-number="2" column-width="proportional-column-width(1)"/>
        <fo:table-body>
          <fo:table-row>
            <fo:table-cell><fo:block><xsl:value-of select="/html/metadata/entry[@key='title']"/></fo:block></fo:table-cell>
            <fo:table-cell text-align="end"><fo:block><fo:page-number/></fo:block></fo:table-cell>
          </fo:table-row>
        </fo:table-body>
      </fo:table>
   </fo:block>
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
      <xsl:call-template name="copy-id"/>
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
    <xsl:call-template name="makeLogoPara">
      <xsl:with-param name="logo">
        <fo:external-graphic src="publicationtype:resources/warn.gif" width="4.923mm" content-width="4.923mm" height="10mm" content-height="10mm"/>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="p[@class='note']" priority="1">
    <xsl:call-template name="makeLogoPara">
      <xsl:with-param name="logo">
        <fo:external-graphic src="publicationtype:resources/note.gif" width="10mm" content-width="10mm" height="7.03mm" content-height="7.03mm"/>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="p[@class='fixme']" priority="1">
    <xsl:call-template name="makeSpecialPara">
      <xsl:with-param name="name">FIXME</xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="pre[@class='include']" priority="1">
    <xsl:call-template name="makeSpecialPara">
      <xsl:with-param name="name">UNPROCESSED INCLUDE DIRECTIVE</xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="p[@class='daisy-error']" priority="1">
    <xsl:call-template name="makeSpecialPara">
      <xsl:with-param name="name">ERROR</xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="makeSpecialPara">
    <xsl:param name="name"/>
    <fo:block
      margin="0.5cm"
      padding="2mm"
      border-style="solid"
      border-width=".1mm"
      border-color="{$colorMediumGray}">
      <fo:block xsl:use-attribute-sets="p.title" keep-with-next="always"><xsl:copy-of select="$name"/></fo:block>
      <xsl:call-template name="makePara"/>
    </fo:block>
  </xsl:template>

  <xsl:template name="makeLogoPara">
    <xsl:param name="logo"/>
    <fo:list-block provisional-label-separation="3mm"
                   provisional-distance-between-starts="13mm"
                   start-indent="4mm"
                   padding-before="4mm" padding-after="4mm">
      <fo:list-item>
        <fo:list-item-label end-indent="label-end()">
          <fo:block text-align="right">
            <xsl:copy-of select="$logo"/>
          </fo:block>
        </fo:list-item-label>
        <fo:list-item-body start-indent="body-start()">
          <fo:block>
            <xsl:apply-templates/>
          </fo:block>
        </fo:list-item-body>
      </fo:list-item>
    </fo:list-block>
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
    <!-- Insert footnotes of the previous chapter if footnotesAtEndOfChapter option is set.
    This is meant as a temporary work-around for footnote rendering problems in IBEX.
    -->
    <xsl:if test="$footnotesAtEndOfChapter">
      <xsl:call-template name="insertFootNotes">
        <xsl:with-param name="footnotes" select="preceding::span[@class='footnote' and generate-id(following::h1[1]) = generate-id(current())] | preceding::a[@href != string(.) and not(starts-with(@href, '#')) and generate-id(following::h1[1]) = generate-id(current())]"/>
      </xsl:call-template>
    </xsl:if>

    <fo:block xsl:use-attribute-sets="h1">
      <xsl:call-template name="heading"/>
    </fo:block>
  </xsl:template>

  <xsl:template match="h2">
    <fo:block xsl:use-attribute-sets="h2">
      <xsl:call-template name="heading"/>
    </fo:block>
  </xsl:template>

  <xsl:template match="h3">
    <fo:block xsl:use-attribute-sets="h3">
      <xsl:call-template name="heading"/>
    </fo:block>
  </xsl:template>

  <xsl:template match="h4">
    <fo:block xsl:use-attribute-sets="h4">
      <xsl:call-template name="heading"/>
    </fo:block>
  </xsl:template>

  <xsl:template match="h5">
    <fo:block xsl:use-attribute-sets="h5">
      <xsl:call-template name="heading"/>
    </fo:block>
  </xsl:template>

  <xsl:template match="h6">
    <fo:block xsl:use-attribute-sets="h6">
      <xsl:call-template name="heading"/>
    </fo:block>
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

  <xsl:template name="heading">
    <xsl:call-template name="copy-id"/>
    <xsl:if test="local-name(.) = 'h1'">
      <fo:marker marker-class-name="chapter"><xsl:apply-templates/></fo:marker>
    </xsl:if>
    <xsl:if test="@daisyNumber">
      <xsl:value-of select="@daisyNumber"/><xsl:text> </xsl:text>
    </xsl:if>
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="a">
    <xsl:variable name="footnoteNumber"><xsl:number count="span[@class='footnote'] | a[@href != string(.)  and not(starts-with(@href, '#'))]" level="any" from="h1"/></xsl:variable>
    <fo:basic-link
      color="blue"
      external-destination="url('{@href}')">
      <xsl:choose>
        <xsl:when test="starts-with(@href, '#')">
          <xsl:attribute name="internal-destination"><xsl:value-of select="substring(@href, 2)"/></xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="external-destination">url('<xsl:value-of select="@href"/>')</xsl:attribute>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates/>
    </fo:basic-link>
    <xsl:if test="starts-with(@href, '#')">
      <xsl:text> (</xsl:text><i18n:text key="link.page"/><xsl:text> </xsl:text><fo:page-number-citation ref-id="{substring(@href, 2)}"/><xsl:text>)</xsl:text>
    </xsl:if>
    <xsl:if test="@href != string(.) and not(starts-with(@href, '#'))">
      <xsl:choose>
        <xsl:when test="$footnotesAtEndOfChapter">
          <fo:inline xsl:use-attribute-sets="footnote.ref"><xsl:value-of select="$footnoteNumber"/></fo:inline>
        </xsl:when>
        <xsl:otherwise>
            <xsl:call-template name="insertFootnote">
              <xsl:with-param name="footnoteNumber" select="$footnoteNumber"/>
              <xsl:with-param name="footnoteText">
                <xsl:value-of select="@href"/>
                <xsl:if test="p:linkInfo/@documentName">
                  <xsl:value-of select="concat(' (',p:linkInfo/@documentName,')')"/>
                </xsl:if>
              </xsl:with-param>
            </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
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
            <xsl:if test="@daisyNumber">
              <i18n:text key="figure.{@daisy-image-type}"/><xsl:text> </xsl:text><xsl:value-of select="@daisyNumber"/><xsl:text>: </xsl:text>
            </xsl:if>
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
    <fo:external-graphic src="{@src}">
      <!-- for FOP, id can be on the external graphic element (for ibex: see svn history)-->
      <xsl:call-template name="copy-id"/>
      <xsl:if test="@print-width">
        <xsl:attribute name="width"><xsl:value-of select="@print-width"/></xsl:attribute>
        <xsl:attribute name="content-width"><xsl:value-of select="@print-width"/></xsl:attribute>
      </xsl:if>
      <xsl:if test="@print-height">
        <xsl:attribute name="height"><xsl:value-of select="@print-height"/></xsl:attribute>
        <xsl:attribute name="content-height"><xsl:value-of select="@print-height"/></xsl:attribute>
      </xsl:if>
    </fo:external-graphic>
  </xsl:template>

  <xsl:template match="pre">
    <fo:block xsl:use-attribute-sets="pre">
      <xsl:call-template name="copy-id"/>
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
    <fo:inline xsl:use-attribute-sets="tt">
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
          <fo:list-item-label end-indent="label-end()">
              <fo:block/>
          </fo:list-item-label>
          <fo:list-item-body start-indent="body-start()">
            <fo:list-block xsl:use-attribute-sets="list">
              <xsl:call-template name="copy-id"/>
              <xsl:apply-templates select="*"/>
            </fo:list-block>
          </fo:list-item-body>
      </fo:list-item>
  </xsl:template>

  <xsl:template match="ul | ol">
      <fo:list-block xsl:use-attribute-sets="list">
        <xsl:call-template name="copy-id"/>
        <xsl:apply-templates select="*"/>
      </fo:list-block>
  </xsl:template>

  <xsl:template match="ul/li">
    <fo:list-item xsl:use-attribute-sets="list.item">
      <xsl:call-template name="copy-id"/>
      <fo:list-item-label end-indent="label-end()">
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
      <xsl:call-template name="copy-id"/>
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
            <xsl:if test="@daisyNumber">
              <i18n:text key="table.{@daisy-table-type}"/><xsl:text> </xsl:text><xsl:value-of select="@daisyNumber"/><xsl:text>: </xsl:text>
            </xsl:if>
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
      <xsl:call-template name="copy-id"/>
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

  <xsl:template match="span[@class='footnote']">
    <xsl:variable name="footnoteNumber"><xsl:number count="span[@class='footnote'] | a[@href != string(.) and not(starts-with(@href, '#'))]" level="any" from="h1"/></xsl:variable>
    <xsl:choose>
      <xsl:when test="not($footnotesAtEndOfChapter)">
        <xsl:call-template name="insertFootnote">
          <xsl:with-param name="footnoteNumber" select="$footnoteNumber"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <fo:inline xsl:use-attribute-sets="footnote.ref"><xsl:value-of select="$footnoteNumber"/></fo:inline>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="insertFootnote">
    <xsl:param name="footnoteNumber"/>
    <xsl:param name="footnoteText"/>
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
  </xsl:template>

  <xsl:template match="span[@class='indexentry']">
    <fo:inline>
      <xsl:call-template name="copy-id"/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="h1[@id='index']">
    <!-- skip -->
  </xsl:template>

  <xsl:template match="index">
    <!-- skip -->
  </xsl:template>

  <xsl:template match="h1[@id='index']" mode="index">
    <fo:block xsl:use-attribute-sets="h1" span="all">
      <xsl:call-template name="heading"/>
    </fo:block>
  </xsl:template>

  <xsl:template match="index" mode="index">
    <xsl:apply-templates select="indexGroup" mode="index"/>
  </xsl:template>

  <xsl:template match="indexGroup" mode="index">
    <fo:block xsl:use-attribute-sets="indexGroupTitle"><xsl:value-of select="@name"/></fo:block>
    <xsl:apply-templates select="indexEntry" mode="index"/>
  </xsl:template>

  <xsl:template match="indexEntry" mode="index">
    <fo:block>
      <xsl:if test="indexEntry">
        <xsl:attribute name="keep-with-next">always</xsl:attribute>
      </xsl:if>
      <xsl:value-of select="@name"/>
      <xsl:for-each select="id">
        <xsl:text>, </xsl:text>
        <fo:basic-link internal-destination="{.}">
          <fo:page-number-citation ref-id="{.}"/>
        </fo:basic-link>
      </xsl:for-each>
    </fo:block>
    <xsl:if test="indexEntry">
      <fo:block start-indent="4mm">
        <xsl:apply-templates select="indexEntry" mode="index"/>
      </fo:block>
    </xsl:if>
  </xsl:template>

  <xsl:template match="toc" mode="pdfbookmarks">
    <fo:bookmark-tree>
      <xsl:apply-templates select="tocEntry" mode="pdfbookmarks"/>
    </fo:bookmark-tree>
  </xsl:template>

  <xsl:template match="tocEntry" mode="pdfbookmarks">
    <fo:bookmark internal-destination="{@targetId}">
      <fo:bookmark-title>
        <xsl:if test="@daisyNumber">
          <xsl:value-of select="@daisyNumber"/>
        </xsl:if>
        <xsl:text> </xsl:text>
        <xsl:value-of select="caption"/>
      </fo:bookmark-title>
      <xsl:apply-templates select="tocEntry" mode="pdfbookmarks"/>
    </fo:bookmark>
  </xsl:template>

  <xsl:template match="toc">
    <fo:block xsl:use-attribute-sets="h1">
      <i18n:text key="toc"/>
    </fo:block>
    <fo:block xsl:use-attribute-sets="toc">
      <xsl:apply-templates select="tocEntry">
        <xsl:with-param name="level" select="1"/>
      </xsl:apply-templates>
    </fo:block>
  </xsl:template>

  <xsl:template match="tocEntry">
    <xsl:param name="level"/>

    <fo:block text-align="justify" text-align-last='justify' start-indent="{$level - 1}cm">
      <xsl:if test="tocEntry">
        <xsl:attribute name="keep-with-next">always</xsl:attribute>
      </xsl:if>
      <xsl:if test="$level = 1">
        <xsl:attribute name="font-size">14pt</xsl:attribute>
        <xsl:attribute name="padding-before">.8cm</xsl:attribute>
      </xsl:if>


      <fo:basic-link internal-destination="{@targetId}">
        <xsl:if test="@daisyNumber">
          <xsl:value-of select="@daisyNumber"/><xsl:text> </xsl:text>
        </xsl:if>
        <xsl:apply-templates select="caption/node()"/>
        <fo:leader leader-pattern='rule' rule-style="dotted" rule-thickness="0.1mm"/>
        <fo:page-number-citation ref-id="{@targetId}"/>
      </fo:basic-link>
    </fo:block>
    <xsl:apply-templates select="tocEntry">
      <xsl:with-param name="level" select="$level + 1"/>
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="list-of-figures">
    <fo:block xsl:use-attribute-sets="h1">
      <i18n:text key="list-of-figures.{@type}"/>
    </fo:block>
    <fo:table table-layout="fixed" xsl:use-attribute-sets="table" width="100%">
      <fo:table-column column-number="1" column-width="1.5cm"/>
      <fo:table-column column-number="2" column-width="proportional-column-width(1)"/>
      <fo:table-body>
        <xsl:apply-templates select="list-item"/>
      </fo:table-body>
    </fo:table>
  </xsl:template>

  <xsl:template match="list-of-tables">
    <fo:block xsl:use-attribute-sets="h1">
      <i18n:text key="list-of-tables.{@type}"/>
    </fo:block>
    <fo:table table-layout="fixed" xsl:use-attribute-sets="table" width="100%">
      <fo:table-column column-number="1" column-width="1.5cm"/>
      <fo:table-column column-number="2" column-width="proportional-column-width(1)"/>
      <fo:table-body>
        <xsl:apply-templates select="list-item"/>
      </fo:table-body>
    </fo:table>
  </xsl:template>

  <xsl:template match="list-item">
    <fo:table-row>
      <fo:table-cell>
        <fo:block>
          <fo:basic-link internal-destination="{@targetId}">
            <xsl:value-of select="@daisyNumber"/>
          </fo:basic-link>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell>
        <fo:block>
          <xsl:apply-templates select="node()"/>
          <fo:leader leader-length="6mm" rule-style="none"/>
          <fo:basic-link internal-destination="{@targetId}">
            <fo:page-number-citation ref-id="{@targetId}"/>
          </fo:basic-link>
        </fo:block>
      </fo:table-cell>
    </fo:table-row>
  </xsl:template>

  <xsl:template match="span[@class='crossreference']">
    <xsl:variable name="crossRefType" select="string(@crossRefType)"/>
    <xsl:variable name="targetId" select="substring(string(@crossRefBookTarget), 2)"/> <!-- substring is to drop the '#' before the target -->
    <xsl:variable name="reftypes">ref,reftitle,ref+reftitle,ref+reftitle+textpage,ref+textpage,</xsl:variable>
    <xsl:variable name="validRefTargets">img,table,h1,h2,h3,h4,h5,h6,h7,h8,h9,</xsl:variable>

    <xsl:choose>
      <xsl:when test="$crossRefType = 'invalid'">
        <fo:inline xsl:use-attribute-sets="invalidcrossref">[invalid cross reference]</fo:inline>
      </xsl:when>
      <xsl:when test="$crossRefType = 'page' or $crossRefType = 'textpage'">
        <fo:basic-link internal-destination="{$targetId}">
          <xsl:if test="$crossRefType = 'textpage'">
            <i18n:text key="crossref.onpage"/>
            <xsl:text> </xsl:text>
          </xsl:if>
          <fo:page-number-citation ref-id="{$targetId}"/>
        </fo:basic-link>
      </xsl:when>
      <xsl:when test="contains($reftypes, concat($crossRefType, ','))">
        <xsl:variable name="targetEl" select="//*[@id=$targetId][1]"/>
        <xsl:choose>
          <xsl:when test="not($targetEl)">
            <fo:inline xsl:use-attribute-sets="invalidcrossref">[cross reference error: target element not found]</fo:inline>
          </xsl:when>
          <xsl:when test="not(contains($validRefTargets, concat(local-name($targetEl), ',')))">
            <fo:inline xsl:use-attribute-sets="invalidcrossref">[cross reference error: target is not a header, image or table]</fo:inline>
          </xsl:when>
          <xsl:otherwise>
            <xsl:variable name="targetElName" select="local-name($targetEl)"/>
            <xsl:variable name="error">
              <xsl:choose>
                <xsl:when test="($crossRefType = 'ref' or starts-with($crossRefType, 'ref+')) and not($targetEl/@daisyNumber)">
                  <xsl:text>[cross reference error: target does not have a number]</xsl:text>
                </xsl:when>
                <xsl:when test="contains($crossRefType, 'reftitle') and ($targetElName = 'img' or $targetElName = 'table') and not($targetEl/@daisy-caption)">
                  <xsl:text>[cross reference error: target image or table does not have a caption]</xsl:text>
                </xsl:when>
              </xsl:choose>
            </xsl:variable>
            <xsl:variable name="crossrefkey">
              <xsl:choose>
                <xsl:when test="$targetElName = 'img'">
                  <xsl:value-of select="concat('crossrefname.', $targetElName, '.', $targetEl/@daisy-image-type)"/>
                </xsl:when>
                <xsl:when test="$targetElName = 'table'">
                  <xsl:value-of select="concat('crossrefname.', $targetElName, '.', $targetEl/@daisy-table-type)"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="concat('crossrefname.', $targetElName)"/>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:variable>
            <xsl:choose>
              <xsl:when test="$error != ''">
                <fo:inline xsl:use-attribute-sets="invalidcrossref"><xsl:value-of select="$error"/></fo:inline>
              </xsl:when>
              <xsl:when test="$crossRefType = 'ref'">
                <fo:basic-link internal-destination="{$targetId}">
                  <i18n:text key="{$crossrefkey}"/>
                  <xsl:text> </xsl:text>
                  <xsl:value-of select="$targetEl/@daisyNumber"/>
                </fo:basic-link>
              </xsl:when>
              <xsl:when test="$crossRefType = 'reftitle'">
                <fo:basic-link internal-destination="{$targetId}">
                  <xsl:call-template name="crossRefTargetTitle">
                    <xsl:with-param name="target" select="$targetEl"/>
                  </xsl:call-template>
                </fo:basic-link>
              </xsl:when>
              <xsl:when test="$crossRefType = 'ref+reftitle'">
                <fo:basic-link internal-destination="{$targetId}">
                  <i18n:text key="{$crossrefkey}"/>
                  <xsl:text> </xsl:text>
                  <xsl:value-of select="$targetEl/@daisyNumber"/>
                  <xsl:text> &#8220;</xsl:text>
                  <xsl:call-template name="crossRefTargetTitle">
                    <xsl:with-param name="target" select="$targetEl"/>
                  </xsl:call-template>
                  <xsl:text>&#8221; </xsl:text>
                </fo:basic-link>
              </xsl:when>
              <xsl:when test="$crossRefType = 'ref+reftitle+textpage'">
                <fo:basic-link internal-destination="{$targetId}">
                  <i18n:text key="{$crossrefkey}"/>
                  <xsl:text> </xsl:text>
                  <xsl:value-of select="$targetEl/@daisyNumber"/>
                  <xsl:text> &#8220;</xsl:text>
                  <xsl:call-template name="crossRefTargetTitle">
                    <xsl:with-param name="target" select="$targetEl"/>
                  </xsl:call-template>
                  <xsl:text>&#8221; </xsl:text>
                  <xsl:text> </xsl:text>
                  <i18n:text key="crossref.onpage"/>
                  <xsl:text> </xsl:text>
                  <fo:page-number-citation ref-id="{$targetId}"/>
                </fo:basic-link>
              </xsl:when>
              <xsl:when test="$crossRefType = 'ref+textpage'">
                <fo:basic-link internal-destination="{$targetId}">
                  <i18n:text key="{$crossrefkey}"/>
                  <xsl:text> </xsl:text>
                  <xsl:value-of select="$targetEl/@daisyNumber"/>
                  <xsl:text> </xsl:text>
                  <i18n:text key="crossref.onpage"/>
                  <xsl:text> </xsl:text>
                  <fo:page-number-citation ref-id="{$targetId}"/>
                </fo:basic-link>
              </xsl:when>
            </xsl:choose>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <fo:inline xsl:use-attribute-sets="invalidcrossref">[unknown cross reference type: <xsl:value-of select="$crossRefType"/>]</fo:inline>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="crossRefTargetTitle">
    <xsl:param name="target"/>
    <xsl:choose>
      <xsl:when test="local-name($target) = 'img' or local-name($target) = 'table'">
        <xsl:value-of select="$target/@daisy-caption"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="$target/node()"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="i18n:text">
    <xsl:copy-of select="."/>
  </xsl:template>

  <xsl:template match="span[@class='daisy-unresolved-variable']">
    <fo:inline xsl:use-attribute-sets="unresolved-variable">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>
  
  <!-- make sure id attribute is unique (fop requires this) -->
  <xsl:template name="copy-id">
    <xsl:variable name="t" select="."/>
    <xsl:variable name="first" select="@id and generate-id(//*[@id=$t/@id][1])=generate-id(.)"/>
    <xsl:if test="$first">
      <xsl:copy-of select="@id"/>
    </xsl:if>
  </xsl:template>
  
</xsl:stylesheet>