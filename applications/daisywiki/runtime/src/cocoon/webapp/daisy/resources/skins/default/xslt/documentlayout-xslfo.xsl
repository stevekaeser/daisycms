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
  xmlns:n="http://outerx.org/daisy/1.0#navigation"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  version="1.0">

  <xsl:variable name="pubdoc" select="/page/p:publisherResponse/p:document"/>
  <xsl:variable name="document" select="$pubdoc/d:document"/>
  <xsl:variable name="version" select="$pubdoc/d:version"/>
  <xsl:variable name="landscape" select="boolean(/page/displayParams/param[@name='landscape']/@value)"/>

  <xsl:include href="daisyskin:xslt/xslfo-styles.xsl"/>

  <xsl:template match="page">
    <fo:root>

      <fo:layout-master-set>
        <fo:simple-page-master master-name="simplepage"
          margin-top="1cm"
          margin-bottom="1cm"
          margin-left="2cm"
          margin-right="2cm"
          page-width="21cm"
          page-height="29.7cm">
          <xsl:if test="$landscape">
            <!-- using reference-orientation creates pdfs which are rotated on screen. Easy to read if your head is mounted like this: :-)
              <xsl:attribute name="reference-orientation">90</xsl:attribute>
            -->
            <xsl:attribute name="page-width">29.7cm</xsl:attribute>
            <xsl:attribute name="page-height">21cm</xsl:attribute>
          </xsl:if>
          <fo:region-body region-name="body"
            margin-bottom="2.5cm"/>
          <fo:region-after region-name="bottom" extent="1.5cm"/>
        </fo:simple-page-master>
      </fo:layout-master-set>

      <fo:page-sequence master-reference="simplepage">
        <fo:static-content flow-name="xsl-footnote-separator">
          <fo:block margin="0cm" padding="0cm" start-indent="0cm" end-indent="0cm">
            <fo:leader leader-pattern="rule"
                       leader-length="100%"
                       rule-style="solid"
                       rule-thickness="0.1mm"/>
          </fo:block>
        </fo:static-content>
        <fo:static-content flow-name="bottom">
          <xsl:call-template name="footer"/>
        </fo:static-content>
        <fo:flow flow-name="body">
          <fo:block xsl:use-attribute-sets="body.text">
            <xsl:call-template name="pageContent"/>
          </fo:block>
        </fo:flow>
      </fo:page-sequence>

    </fo:root>
  </xsl:template>

  <xsl:template name="footer">
    <fo:block font-size="11pt"
      font-family="{$fontSerif}"
      padding-top="3pt"
      border-top-style="solid"
      border-top-width=".1mm">
      <xsl:if test="$version">
        <fo:table width="100%" table-layout="fixed">
          <fo:table-column column-number="1" column-width="proportional-column-width(4)"/>
          <fo:table-column column-number="2" column-width="proportional-column-width(1)"/>
          <fo:table-body>
            <fo:table-row>
              <fo:table-cell><fo:block><xsl:value-of select="$document/@name"/></fo:block></fo:table-cell>
              <fo:table-cell text-align="end"><fo:block><fo:page-number format="1"/></fo:block></fo:table-cell>
            </fo:table-row>
            <fo:table-row>
              <fo:table-cell number-columns-spanned="2">
                <fo:block font-size="8pt">
                  <i18n:text key="doclayout-xslfo.id"/>: <xsl:value-of select="$document/@id"/>
                  <xsl:text> | </xsl:text>
                  <i18n:text key="doclayout-xslfo.version"/>: <xsl:value-of select="$document/@dataVersionId"/>
                  <xsl:text> | </xsl:text>
                  <i18n:text key="doclayout-xslfo.date"/>: <xsl:value-of select="$version/@lastModifiedFormatted"/>
                </fo:block>
              </fo:table-cell>
            </fo:table-row>
          </fo:table-body>
        </fo:table>
      </xsl:if>
    </fo:block>
  </xsl:template>

  <xsl:template name="pageContent">
    <!-- Place the content -->
    <xsl:choose>
      <xsl:when test="not($version)">
        <fo:block xsl:use-attribute-sets="h1"><i18n:text key="doclayout.in-preparation"/></fo:block>
        <fo:block xsl:use-attribute-sets="p"><i18n:text key="doclayout.in-preparation-info"/></fo:block>
      </xsl:when>
      <xsl:otherwise>
        <xsl:if test="$document/@retired = 'true'">
          <fo:block xsl:use-attribute-sets="h1"><i18n:text key="doclayout.retired"/></fo:block>
          <fo:block xsl:use-attribute-sets="p"><i18n:text key="doclayout.retired-info"/></fo:block>
        </xsl:if>
        <insertStyledDocument styledResultsId="{$pubdoc/p:preparedDocuments/@styledResultsId}"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>