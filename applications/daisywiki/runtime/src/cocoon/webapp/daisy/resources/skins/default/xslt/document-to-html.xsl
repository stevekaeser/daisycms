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
  xmlns:p="http://outerx.org/daisy/1.0#publisher"
  xmlns:lt="http://outerx.org/daisy/1.0#linktransformer"
  xmlns:urlencoder="xalan://java.net.URLEncoder"
  xmlns:ie="http://outerx.org/daisy/1.0#inlineeditor">

  <xsl:import href="daisyskin:xslt/searchresult.xsl"/>
  <xsl:import href="daisyskin:xslt/util.xsl"/>

  <xsl:param name="documentBasePath"/>
  <xsl:variable name="document" select="document/p:publisherResponse/d:document"/>

  <xsl:template match="document">
    <xsl:choose>
      <xsl:when test="p:publisherResponse/d:document">
        <xsl:call-template name="editIncludedDocLink"/>
        <xsl:apply-templates select="p:publisherResponse/d:document"/>
      </xsl:when>
      <xsl:otherwise>
        <p>The publisher request used for this document is missing a &lt;p:prepareDocument/&gt; request.</p>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="d:document">
    <h1 class="daisy-document-name" id="dsy{@id}"><xsl:value-of select="@name"/></h1>
    <xsl:apply-templates select="d:parts/d:part"/>
    <xsl:apply-templates select="d:links"/>
    <xsl:apply-templates select="d:fields"/>
    <xsl:call-template name="insertFootnotes">
      <xsl:with-param name="root" select="."/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="d:part">
    <xsl:choose>
      <xsl:when test="@daisyHtml = 'true'">
        <xsl:apply-templates select="html/body/node()" mode="PartContent"/>
      </xsl:when>
      <xsl:when test="@name = 'LiteralHtmlData' and @inlined='true'">
        <xsl:copy-of select="html/body/node()"/>
      </xsl:when>
      <xsl:otherwise>
        <p>
          <xsl:variable name="fileName">
            <xsl:if test="@fileName != ''"><xsl:value-of select="concat('/', @fileName)"/></xsl:if>
          </xsl:variable>
          <xsl:value-of select="@label"/>: <a href="{$documentBasePath}{$document/@id}/version/{$document/@dataVersionId}/part/{@typeId}/data{$fileName}?branch={$document/@branch}&amp;language={$document/@language}">download</a>
          (<xsl:value-of select="@mimeType"/>,
            <xsl:call-template name="formatSize">
              <xsl:with-param name="size" select="@size"/>
            </xsl:call-template>
          <xsl:text>)</xsl:text>
        </p>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="formatSize">
    <xsl:param name="size"/>
    <xsl:choose>
      <xsl:when test="$size >= 1000000">
        <xsl:value-of select="concat(format-number($size div 1000000, '#.0'), ' MB')"/>
      </xsl:when>
      <xsl:when test="$size >= 1000">
        <xsl:value-of select="concat(format-number($size div 1000, '#.0'), ' kB')"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="concat($size, ' bytes')"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="@*|node()" mode="PartContent">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()" mode="PartContent"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="d:searchResult" mode="PartContent">
    <!-- forward to normal search result formatting templates -->
    <xsl:apply-templates select=".">
    <!-- Add context document parameter for query stylings that make use of it ('chunked' for instance) -->
      <xsl:with-param name="contextDocument" select="$document"/>
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="table" mode="PartContent">
    <table class="content">
      <xsl:for-each select="@*">
        <xsl:copy/>
      </xsl:for-each>
      <xsl:if test="@daisy-caption != ''">
        <caption><xsl:value-of select="@daisy-caption"/></caption>
      </xsl:if>
      <xsl:if test="computedInfo/html">
        <xsl:for-each select="computedInfo/html/col">
          <col>
            <xsl:if test="@width">
              <xsl:attribute name="width"><xsl:value-of select="@width"/></xsl:attribute>
            </xsl:if>
          </col>
        </xsl:for-each>
      </xsl:if>
      <xsl:for-each select="tbody|tr|td">
        <xsl:copy>
          <xsl:apply-templates select="node()" mode="PartContent"/>
        </xsl:copy>
      </xsl:for-each>
    </table>
  </xsl:template>

  <xsl:template match="a" mode="PartContent">
    <xsl:variable name="linkInfo" select="p:linkInfo"/>
    <a>
      <xsl:copy-of select="@*"/>
      <xsl:if test="$linkInfo/@documentName">
        <xsl:attribute name="title"><xsl:value-of select="$linkInfo/@documentName"/></xsl:attribute>
      </xsl:if>
      <xsl:apply-templates mode="PartContent"/>
    </a>
  </xsl:template>

  <xsl:template match="a[p:linkInfo/@documentType='Attachment']" mode="PartContent">
    <xsl:variable name="linkInfo" select="p:linkInfo"/>
    <xsl:variable name="size" select="$linkInfo/p:linkPartInfo[@name='AttachmentData']/@size"/>
    <xsl:variable name="mimeType" select="$linkInfo/p:linkPartInfo[@name='AttachmentData']/@mimeType"/>
    <a>
      <!-- navigationPath attribute is not copied, since it might end on a navigation node ID
           instead of a document ID, and the PartReader can't handle that. -->
      <xsl:copy-of select="@*[local-name(.) != 'navigationPath']"/>
      <xsl:attribute name="lt:partLink">AttachmentData</xsl:attribute>
      <!-- Note: the filename will not always be there, but the link transformer can copy with
           empty lt:fileName attributes -->
      <xsl:attribute name="lt:fileName"><xsl:value-of select="$linkInfo/p:linkPartInfo[@name='AttachmentData']/@fileName"/></xsl:attribute>
      <xsl:if test="$linkInfo/@documentName">
        <xsl:attribute name="title"><xsl:value-of select="$linkInfo/@documentName"/></xsl:attribute>
      </xsl:if>
      <xsl:apply-templates mode="PartContent"/>
    </a>
    <xsl:text> (</xsl:text>
    <xsl:value-of select="$mimeType"/>
    <xsl:text>, </xsl:text> 
    <xsl:call-template name="formatSize">
      <xsl:with-param name="size" select="$size"/>
    </xsl:call-template>
    <xsl:text>, </xsl:text>
    <a>
      <xsl:copy-of select="@*"/>
      <i18n:text key="document.info-link"/>
    </a>
    <xsl:text>)</xsl:text>
  </xsl:template>

  <xsl:template match="p:linkInfo" mode="PartContent">
    <!-- empty template, becuase p:linkInfo should be removed from output -->
  </xsl:template>

  <xsl:template match="img" mode="PartContent">
    <xsl:choose>
      <xsl:when test="@daisy-caption">
        <table class="plainTable">
          <tr>
            <td><xsl:call-template name="insertImage"/></td>
          </tr>
          <tr>
            <td><xsl:value-of select="@daisy-caption"/></td>
          </tr>
        </table>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="insertImage"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="insertImage">
    <xsl:variable name="linkInfo" select="p:linkInfo"/>
    <xsl:variable name="imageWidth" select="string(@p:imageWidth)"/>
    <xsl:choose>
      <xsl:when test="$imageWidth > 650 and $linkInfo/p:linkPartInfo[@name='ImagePreview']">
        <table class="plainTable">
          <tr>
            <td>
              <a href="{@src}" lt:partLink="ImageData" lt:fileName="{$linkInfo/p:linkPartInfo[@name='ImageData']/@fileName}" style="border: 1px">
                <img>
                  <xsl:copy-of select="@*[local-name(.) != 'width' and local-name(.) != 'height']"/>
                  <xsl:attribute name="lt:partLink">ImagePreview</xsl:attribute>
                  <xsl:if test="$linkInfo/@documentName">
                    <xsl:attribute name="alt"><xsl:value-of select="$linkInfo/@documentName"/></xsl:attribute>
                    <xsl:attribute name="title"><xsl:value-of select="$linkInfo/@documentName"/></xsl:attribute>
                  </xsl:if>
                </img>
              </a>
            </td>
          </tr>
          <tr>
            <td style="text-align: center">
              <a href="{@src}" lt:partLink="ImageData"
                 lt:fileName="{$linkInfo/p:linkPartInfo[@name='ImageData']/@fileName}"
                 style="font-style: italic"><i18n:text key="document.click-to-enlarge"/></a>
            </td>
          </tr>
        </table>
      </xsl:when>
      <xsl:otherwise>
        <img>
          <xsl:copy-of select="@*"/>
          <xsl:attribute name="lt:partLink">ImageData</xsl:attribute>
          <xsl:attribute name="lt:fileName"><xsl:value-of select="$linkInfo/p:linkPartInfo[@name='ImageData']/@fileName"/></xsl:attribute>
          <xsl:if test="$linkInfo/@documentName">
            <xsl:if test="not(@alt)">
              <xsl:attribute name="alt"><xsl:value-of select="$linkInfo/@documentName"/></xsl:attribute>
            </xsl:if>
            <xsl:if test="not(@title)">
              <xsl:attribute name="title"><xsl:value-of select="$linkInfo/@documentName"/></xsl:attribute>
            </xsl:if>
          </xsl:if>
        </img>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="pre[@class='include']" mode="PartContent">
    <einclude:include src="{normalize-space(string(.))}"/>
  </xsl:template>

  <xsl:template match="span[@class='indexentry']" mode="PartContent">
    <!-- remove index entries from output -->
  </xsl:template>

  <xsl:template match="span[@class='footnote']" mode="PartContent">
    <xsl:variable name="id" select="generate-id(.)"/>
    <xsl:variable name="footnoteNumber"><xsl:number count="span[@class='footnote']" level="any"/></xsl:variable>
    <sup style="font-size: smaller"><a href="#{$id}" id="dsy{$document/@id}_{$id}_ref"><xsl:value-of select="$footnoteNumber"/></a></sup>
  </xsl:template>

  <xsl:template name="insertFootnotes">
    <xsl:variable name="footnotes" select="//span[@class='footnote']"/>
    <xsl:if test="$footnotes">
      <table class="footnotes">
        <col width="5%"/>
        <col width="95%"/>
        <tbody>
          <xsl:for-each select="$footnotes">
            <xsl:variable name="id" select="generate-id(.)"/>
            <tr>
              <td><a name="dsy{$document/@id}_{$id}" href="#{$id}_ref"><xsl:value-of select="position()"/>.</a></td>
              <td><xsl:apply-templates select="node()" mode="PartContent"/></td>
            </tr>
          </xsl:for-each>
        </tbody>
      </table>
    </xsl:if>
  </xsl:template>

  <xsl:template match="span[@class='crossreference']" mode="PartContent">
    <xsl:variable name="crossRefType" select="string(@crossRefType)"/>
    <xsl:choose>
      <xsl:when test="$crossRefType = 'invalid'">
        [invalid cross reference]
      </xsl:when>
      <xsl:otherwise>
        <a href="{@crossRefTarget}">(cross reference)</a>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="d:links">
    <xsl:if test="d:link">
      <div class="doclinks">
        <h2><i18n:text key="document.links-title"/></h2>
        <ul>
          <xsl:for-each select="d:link">
            <xsl:variable name="linkInfo" select="p:linkInfo"/>
            <li>
              <a href="{@target}">
                <xsl:if test="$linkInfo/@documentName">
                  <xsl:attribute name="title"><xsl:value-of select="$linkInfo/@documentName"/></xsl:attribute>
                </xsl:if>
                <xsl:value-of select="@title"/>
              </a>
            </li>
          </xsl:for-each>
        </ul>
      </div>
    </xsl:if>
  </xsl:template>

  <xsl:template match="d:fields">
    <xsl:if test="d:field">
      <h2><i18n:text key="document.fields-title"/></h2>
      <xsl:if test="d:field">
        <table class="default">
          <tr>
            <th><i18n:text key="document.fields.name"/></th>
            <th><i18n:text key="document.fields.value"/></th>
          </tr>
          <xsl:for-each select="d:field">
            <xsl:variable name="isLinkType" select="@valueType = 'link'"/>
            <xsl:variable name="isHierarchical" select="@hierarchical = 'true'"/>
            <tr>
              <td style="vertical-align: top"><xsl:value-of select="@label"/></td>
              <td>
                <xsl:for-each select="*">
                  <xsl:if test="position() > 1"><br/></xsl:if>
                  <xsl:choose>
                    <xsl:when test="$isHierarchical">
                      <xsl:for-each select="*">
                        <xsl:if test="position() > 1"> / </xsl:if>
                        <xsl:call-template name="insertFieldValue">
                          <xsl:with-param name="isLinkType" select="$isLinkType"/>
                        </xsl:call-template>
                      </xsl:for-each>
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:call-template name="insertFieldValue">
                        <xsl:with-param name="isLinkType" select="$isLinkType"/>
                      </xsl:call-template>
                    </xsl:otherwise>
                  </xsl:choose>
                </xsl:for-each>
              </td>
            </tr>
          </xsl:for-each>
        </table>
      </xsl:if>
    </xsl:if>
  </xsl:template>

  <xsl:template name="insertFieldValue">
    <xsl:param name="isLinkType"/>

    <xsl:choose>
      <xsl:when test="$isLinkType">
        <a href="{@target}">
          <xsl:value-of select="@valueFormatted"/>
        </a>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="@valueFormatted"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Inserts an edit link for included documents, if appropriate -->
  <xsl:template name="editIncludedDocLink">
    <xsl:if test="@isIncluded = 'true'">
      <xsl:variable name="context" select="/document/context"/>

      <!-- Can only return to pages requested with a 'GET', should be mostly the case.
           Also, don't show edit link in plain layout (which is often used for printing). -->
      <xsl:if test="context/request/@method = 'GET' and context/layoutType != 'plain'">

        <!-- Only show edit link when the user has edit rights -->
        <xsl:variable name="isEditor" select="boolean(p:publisherResponse/d:aclResult/d:permissions/d:permission[@type='write' and @action='grant'])"/>
        <xsl:if test="$isEditor">
          <xsl:variable name="returnToURL" select="urlencoder:encode(concat($context/request/@uri, '#dsy', $document/@id), 'UTF-8')"/>
          <div class="editIncluded">
            <xsl:text>[ </xsl:text>
            <xsl:call-template name="generatePostLink">
              <xsl:with-param name="action" select="concat($context/mountPoint, '/', $context/site/@name, '/', $document/@id, '/edit?returnTo=', $returnToURL, '&amp;branch=', urlencoder:encode($document/@branch, 'UTF-8'), '&amp;language=', urlencoder:encode($document/@language, 'UTF-8'))"/>
              <xsl:with-param name="id" select="concat(generate-id(.), $document/@id)"/>
              <xsl:with-param name="label"><i18n:text key="document.edit-included-doc"/></xsl:with-param>
            </xsl:call-template>
            <xsl:text> ]</xsl:text>
          </div>
        </xsl:if>
      </xsl:if>
    </xsl:if>
  </xsl:template>
  
</xsl:stylesheet>