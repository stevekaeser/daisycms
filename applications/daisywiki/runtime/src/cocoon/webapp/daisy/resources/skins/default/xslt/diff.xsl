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
  xmlns:p="http://outerx.org/daisy/1.0#publisher"
  xmlns:n="http://outerx.org/daisy/1.0#navigation"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:lt="http://outerx.org/daisy/1.0#linktransformer"
  xmlns:daisyutil="xalan://org.outerj.daisy.frontend.util.XslUtil">

  <xsl:import href="daisyskin:xslt/pagevars.xsl"/>

  <xsl:variable name="skin" select="page/context/skin"/>
  
  <xsl:variable name="pubdoc" select="/page/p:publisherResponse/p:document"/>
  <xsl:variable name="version1Info" select="$pubdoc/diff-report/info/version1"/>
  <xsl:variable name="version2Info" select="$pubdoc/diff-report/info/version2"/>
  <xsl:variable name="version1Id" select="$version1Info/@id"/>
  <xsl:variable name="version2Id" select="$version2Info/@id"/>
  
  <xsl:variable name="isEditor" select="boolean($pubdoc/d:aclResult/d:permissions/d:permission[@type='write' and @action='grant'])"/>

  <xsl:variable name="contentDiffType" select="$pubdoc/diff-report/@contentDiffType"/>
  <xsl:variable name="baseDiffURL" select="concat($documentPath, '/version/', $version1Id, '/diff?otherDocumentId=', $version2Info/@documentId, '&amp;otherVersion=', $version2Id, '&amp;otherBranch=', $version2Info/@branch, '&amp;otherLanguage=', $version2Info/@language, '&amp;contentDiffType=', $contentDiffType, $variantParams)"/>
  <xsl:variable name="diffURL" select="concat($baseDiffURL, '&amp;contentDiffType=', $pubdoc/diff-report/@contentDiffType)"/>
  <xsl:variable name="document2Path" select="concat($mountPoint, '/', $siteName, '/', $version2Info/@documentId)"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><i18n:text key="diff.title"/></pageTitle>
      <xsl:copy-of select="p:publisherResponse/n:navigationTree"/>
      <xsl:call-template name="pageNavigation"/>
      <xsl:call-template name="availableVariants"/>
      <navigationInfo>
        <xsl:copy-of select="p:publisherResponse/p:group[@id = 'navigationInfo']/p:document/*"/>
      </navigationInfo>
      <layoutHints needsDojo="true"/>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="pageNavigation">
    <pageNavigation>
      <link>
        <title><i18n:text key="back-to-document"/></title>
        <path><xsl:value-of select="concat($documentPath, '.html', $variantQueryString)"/></path>
      </link>
      <link>
        <title><i18n:text key="hide-navigation"/></title>
        <path><xsl:value-of select="concat($diffURL, '&amp;layoutType=plain')"/></path>
      </link>      
    </pageNavigation>
  </xsl:template>

  <xsl:template name="content">
    <xsl:call-template name="availableVariants"/>
    <h1><i18n:text key="diff.title"/></h1>
    
    <!-- (HTML diff) This must be included before any changed image. -->
    <script type="text/javascript" src="{$mountPoint}/resources/js/diff.js"/>

    <xsl:if test="$contentDiffType = 'html'">
      <script>
        htmlDiffInit();
      </script>

      <script type="text/javascript" src="{$mountPoint}/resources/js/tooltip/wz_tooltip.js"/>
        <!--
            This must be included after diff.js because the image path is calculated there. -->
      <script type="text/javascript" src="{$mountPoint}/resources/js/tooltip/tip_balloon.js"/>
    </xsl:if>
    
    <div style="border: 1px solid black; padding: .25em; margin: 1em">
      <div style="text-align: center; font-weight: bold"><i18n:text key="diff.compare-settings"/></div>
      <table class="dsyfrm-table">
        <tbody>
          <tr>
            <td class="dsyfrm-labelcell dsy-nowrap"><i18n:text key="diff.changes-from"/></td>
            <td class="dsyfrm-widgetcell">
              <input id="document1" value="{concat('daisy:', $version1Info/@documentId, '@', $version1Info/@branch, ':', $version1Info/@language, ':', $version1Info/@id)}" dojoType="daisy:LinkEditor" enableFragmentId="false" contextMode="site"/>
            </td>
          </tr>
          <tr>
            <td class="dsyfrm-labelcell dsy-nowrap"><i18n:text key="diff.changes-to"/></td>
            <td class="dsyfrm-widgetcell">
              <input id="document2" value="{concat('daisy:', $version2Info/@documentId, '@', $version2Info/@branch, ':', $version2Info/@language, ':', $version2Info/@id)}" dojoType="daisy:LinkEditor" enableFragmentId="false" contextMode="site"/>
            </td>
          </tr>
          <tr>
            <td class="dsyfrm-labelcell dsy-nowrap"><i18n:text key="diff.type"/></td>
            <td class="dsyfrm-widgetcell">
              <select id="diffType">
                <option value="text"><xsl:if test="$contentDiffType = 'text'"><xsl:attribute name="selected">selected</xsl:attribute></xsl:if><i18n:text key="diff.txt-compare"/></option>
                <option value="html"><xsl:if test="$contentDiffType = 'html'"><xsl:attribute name="selected">selected</xsl:attribute></xsl:if><i18n:text key="diff.html-compare"/></option>
                <option value="htmlsource"><xsl:if test="$contentDiffType = 'htmlsource'"><xsl:attribute name="selected">selected</xsl:attribute></xsl:if><i18n:text key="diff.text-xml-compare"/></option>
              </select>
            </td>
          </tr>
          <tr>
            <td/>
            <td>
              <button onclick="getDiff(); return false;" class="dsyfrm-primaryaction"><i18n:text key="diff.compare"/></button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!--
       The 'position: relative' styling is so that the image overlays (for added/removed images)
       would be absolutely positioned relative to this div. This avoids that any layout shifts
       in higher parts of the document during or after page loading don't cause incorrect
       positioning of the image overlays.
    -->
    <div parseWidgets="false" style="position: relative">
      <xsl:apply-templates select="$pubdoc/diff-report"/>      
    </div>

  </xsl:template>

  <xsl:template match="diff-report">
    <xsl:variable name="version1Id" select="info/version1/@id"/>
    <xsl:variable name="version2Id" select="info/version2/@id"/>

    <xsl:choose>
      <xsl:when test="parts/*[name()!='partUnchanged'] or links/* or fields/*">
        <xsl:apply-templates select="parts">
          <xsl:with-param name="version1Id" select="$version1Id"/>
          <xsl:with-param name="version2Id" select="$version2Id"/>
        </xsl:apply-templates>
        <xsl:apply-templates select="links"/>
        <xsl:apply-templates select="fields">
          <xsl:with-param name="version1Id" select="$version1Id"/>
          <xsl:with-param name="version2Id" select="$version2Id"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise>
        <div class="dsydiff-text"><span class="diffpage-nochanges"><i18n:text key="diff.no-changes-detected"/></span></div>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:apply-templates select="info">
      <xsl:with-param name="version1Id" select="$version1Id"/>
      <xsl:with-param name="version2Id" select="$version2Id"/>
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="info">
    <xsl:param name="version1Id"/>
    <xsl:param name="version2Id"/>

    <xsl:variable name="documentName" select="../documentName"/>
    <table style="padding: 1em; width: 100%; table-layout:fixed;">
      <!-- Column widths are specified on the cells rather than using col elements, since
           that doesn't seem to work with IE -->
      <tr>
        <td class="dsydiff-versionbox" style="width: 45%;">
          <xsl:call-template name="versionInfo">
            <xsl:with-param name="version" select="version1"/>
            <xsl:with-param name="docName" select="$documentName/@version1"/>
          </xsl:call-template>
          <div class="dsydiff-prevnextnav">
            <xsl:if test="$version1Id > 1">
              <a href="{concat($documentPath, '/version/', $version1Id - 1, '/diff?otherVersion=', $version1Id, '&amp;branch=', $version1Info/@branch, '&amp;language=', $version1Info/@language, '&amp;contentDiffType=', $contentDiffType)}">
                <img class="diff-icon" src="{$mountPoint}/resources/skins/{$skin}/images/diff-previous.gif"/>
                &#160;
                <i18n:translate>
                  <i18n:text key="diff.compare-versions"/>
                  <i18n:param>
                    <strong><xsl:value-of select="$version1Id - 1"/></strong>
                  </i18n:param>
                  <i18n:param>
                    <strong><xsl:value-of select="$version1Id"/></strong>
                  </i18n:param>
                </i18n:translate>
              </a>
            </xsl:if>
          </div>
        </td>
        <td style="text-align: center; width: 10%;">
          <img src="{$mountPoint}/resources/skins/{$skin}/images/diff_between.png"/>
        </td>
        <td class="dsydiff-versionbox" style="width: 45%;">
          <xsl:call-template name="versionInfo">
            <xsl:with-param name="version" select="version2"/>
            <xsl:with-param name="docName" select="$documentName/@version2"/>
          </xsl:call-template>
          <div class="dsydiff-prevnextnav" style="text-align: right">
            <xsl:if test="$version2Id &lt; $version2Info/@lastVersionId">
              <a href="{concat($document2Path, '/version/', $version2Id, '/diff?otherVersion=', $version2Id + 1, '&amp;branch=', $version2Info/@branch, '&amp;language=', $version2Info/@language, '&amp;contentDiffType=', $contentDiffType)}">
                <i18n:translate>
                  <i18n:text key="diff.compare-versions"/>
                  <i18n:param>
                    <strong><xsl:value-of select="$version2Id"/></strong>
                  </i18n:param>
                  <i18n:param>
                    <strong><xsl:value-of select="$version2Id + 1"/></strong>
                  </i18n:param>
                </i18n:translate>
              </a>
              &#160;
              <img class="diff-icon" src="{$mountPoint}/resources/skins/{$skin}/images/diff-next.gif"/>
            </xsl:if>
          </div>
        </td>
      </tr>
    </table>

    <xsl:if test="$version1Id > $version2Id">
      <p class="dsydiff-text"><strong><i18n:text key="diff.reverse-diff-warning"/></strong></p>
    </xsl:if>
  </xsl:template>

  <xsl:template match="parts">
    <xsl:param name="version1Id"/>
    <xsl:param name="version2Id"/>

    <xsl:if test="*[name()!='partUnchanged']">
      <h2 class="dsydiff"><i18n:text key="diff.part-changes"/></h2>
      <xsl:apply-templates>
        <xsl:with-param name="version1Id" select="$version1Id"/>
        <xsl:with-param name="version2Id" select="$version2Id"/>
      </xsl:apply-templates>
    </xsl:if>
  </xsl:template>

  <xsl:template match="partUpdated">
    <xsl:param name="version1Id"/>
    <xsl:param name="version2Id"/>

    <h3 class="dsydiff">
      <i18n:translate>
        <i18n:text key="diff.part-has-changed"/>
        <i18n:param><xsl:value-of select="@typeLabel"/></i18n:param>
      </i18n:translate>
    </h3>

    <xsl:if test="diff">
      <br/>
      <div class="diffpage-contentdiff-title"><i18n:text key="diff.content-diff"/>
        <xsl:if test="diff/@contentDiffType = 'html'">
          <div class="diffpage-html-info"><i18n:text key="diff.click-on-changed"/><br/><i18n:text key="diff.use-arrows"/></div>
          <xsl:variable name="spans" select="diff//span[(@class='diff-html-added' or @class='diff-html-removed' or @class='diff-html-changed')  and @id]"/>
            <table class="diffpage-html-firstlast"><tr><td style="text-align: left;">
                <a>
                  <xsl:attribute name="onclick">scrollToEvent(event)</xsl:attribute>
                  <xsl:attribute name="id">first-<xsl:value-of select="@typeId"/></xsl:attribute>
                  <xsl:attribute name="lt:ignore">true</xsl:attribute>
                  <xsl:attribute name="href">
                    <xsl:text>#</xsl:text>
                    <xsl:value-of select="$spans[1]/@id"/>
                  </xsl:attribute>
                  <xsl:attribute name="next">
                    <xsl:value-of select="$spans[1]/@id"/>
                  </xsl:attribute>
                  <img class="diff-icon"
                    src="{$mountPoint}/resources/skins/{$skin}/images/diff-first.gif"
                    title="diff.go-to-first"
                    i18n:attr="title"/>
                </a>
                <a>
                  <xsl:attribute name="onclick">scrollToEvent(event)</xsl:attribute>
                   <xsl:attribute name="lt:ignore">true</xsl:attribute>
                  <xsl:attribute name="href">
                    <xsl:text>#</xsl:text>
                    <xsl:value-of select="$spans[1]/@id"/>
                  </xsl:attribute>
                  <xsl:text>&#160;</xsl:text><i18n:text key="diff.first"/>
                </a>
            </td>
            <td style="text-align: right;">
                <a>
                  <xsl:attribute name="onclick">scrollToEvent(event)</xsl:attribute>
                   <xsl:attribute name="lt:ignore">true</xsl:attribute>
                  <xsl:attribute name="href">
                    <xsl:text>#</xsl:text>
                    <xsl:value-of select="$spans[last()]/@id"/>
                  </xsl:attribute>
                  <i18n:text key="diff.last"/><xsl:text>&#160;</xsl:text>
                </a>
                <a>
                  <xsl:attribute name="onclick">scrollToEvent(event)</xsl:attribute>
                  <xsl:attribute name="id">last-<xsl:value-of select="@typeId"/></xsl:attribute>
                  <xsl:attribute name="lt:ignore">true</xsl:attribute>
                  <xsl:attribute name="href">
                    <xsl:text>#</xsl:text>
                    <xsl:value-of select="$spans[last()]/@id"/>
                  </xsl:attribute>
                  <xsl:attribute name="previous">
                    <xsl:value-of select="$spans[last()]/@id"/>
                  </xsl:attribute>
                  <img class="diff-icon"
                    src="{$mountPoint}/resources/skins/{$skin}/images/diff-last.gif"
                    title="diff.go-to-last"
                    i18n:attr="title"/>
                </a>
             </td></tr></table>
        </xsl:if>
      </div>
      <div class="diffpage-{diff/@contentDiffType}-contentdiff">
        <xsl:choose>
          <xsl:when test="diff/@contentDiffType = 'text'">
            <xsl:for-each select="diff/div">
              <xsl:choose>
                <xsl:when test="./node()">
                  <xsl:copy-of select="."/>
                </xsl:when>
                <xsl:otherwise>
                  <div class="{@class}">&#160;</div>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:for-each>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="diff/node()" mode="HtmlDiffContent"/>
            <div style="clear: both;"></div>
          </xsl:otherwise>
        </xsl:choose>
      </div>
    </xsl:if>

    <table style="padding: 1em; width: 100%; table-layout:fixed">
      <xsl:call-template name="valueComparison">
        <xsl:with-param name="label"><i18n:text key="diff.mime-type"/></xsl:with-param>
        <xsl:with-param name="val1" select="@version1MimeType"/>
        <xsl:with-param name="val2" select="@version2MimeType"/>
      </xsl:call-template>

      <xsl:call-template name="valueComparison">
        <xsl:with-param name="label"><i18n:text key="diff.filename"/></xsl:with-param>
        <xsl:with-param name="val1" select="@version1FileName"/>
        <xsl:with-param name="val2" select="@version2FileName"/>
      </xsl:call-template>

      <xsl:call-template name="valueComparison">
        <xsl:with-param name="label"><i18n:text key="diff.size"/></xsl:with-param>
        <xsl:with-param name="val1" select="@version1Size"/>
        <xsl:with-param name="val2" select="@version2Size"/>
      </xsl:call-template>
    </table>
  </xsl:template>

  <xsl:template match="@*|node()" mode="HtmlDiffContent">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()" mode="HtmlDiffContent"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="img" mode="HtmlDiffContent">
    <img>
      <xsl:copy-of select="@*"/>
      <xsl:attribute name="lt:partLink">ImageData</xsl:attribute>
      <xsl:if test="@changeType='diff-removed-image' or @changeType='diff-added-image'">
            <xsl:attribute name="onLoad">updateOverlays()</xsl:attribute>
            <xsl:attribute name="onError">updateOverlays()</xsl:attribute>
            <xsl:attribute name="onAbort">updateOverlays()</xsl:attribute>
      </xsl:if>

    </img>
  </xsl:template>

  <xsl:template match="span[@class='diff-html-changed']" mode="HtmlDiffContent">
    <span>
      <xsl:copy-of select="@*"/>
      <xsl:attribute name="onclick">return tipC(constructToolTipC(this));</xsl:attribute>
      <xsl:apply-templates select="node()" mode="HtmlDiffContent"/>
    </span>
  </xsl:template>

  <xsl:template match="span[@class='diff-html-added']" mode="HtmlDiffContent">
    <span>
      <xsl:copy-of select="@*"/>
      <xsl:attribute name="onclick">return tipA(constructToolTipA(this));</xsl:attribute>
      <xsl:apply-templates select="node()" mode="HtmlDiffContent"/>
    </span>
  </xsl:template>

  <xsl:template match="span[@class='diff-html-removed']" mode="HtmlDiffContent">
    <span>
      <xsl:copy-of select="@*"/>
      <xsl:attribute name="onclick">return tipR(constructToolTipR(this));</xsl:attribute>
      <xsl:apply-templates select="node()" mode="HtmlDiffContent"/>
    </span>
  </xsl:template>
  
  <xsl:template match="table" mode="HtmlDiffContent">
    <table class="content">
      <xsl:for-each select="@*">
        <xsl:copy/>
      </xsl:for-each>
      <xsl:if test="@daisy-caption != ''">
        <caption><xsl:value-of select="@daisy-caption"/></caption>
      </xsl:if>
      <xsl:for-each select="tbody|tr|td">
        <xsl:copy>
          <xsl:apply-templates select="node()" mode="HtmlDiffContent"/>
        </xsl:copy>
      </xsl:for-each>
    </table>
  </xsl:template>

  <xsl:template match="partRemoved">
    <h3 class="dsydiff">
      <i18n:translate>
        <i18n:text key="diff.part-removed"/>
        <i18n:param><xsl:value-of select="@typeLabel"/></i18n:param>
      </i18n:translate>
    </h3>
  </xsl:template>

  <xsl:template match="partAdded">
    <h3 class="dsydiff">
      <i18n:translate>
        <i18n:text key="diff.part-added"/>
        <i18n:param><xsl:value-of select="@typeLabel"/></i18n:param>
      </i18n:translate>
    </h3>
  </xsl:template>

  <xsl:template match="partUnchanged">
    <h3 class="dsydiff">
      <i18n:translate>
        <i18n:text key="diff.part-unchanged"/>
        <i18n:param><xsl:value-of select="@typeLabel"/></i18n:param>
      </i18n:translate>
    </h3>
  </xsl:template>

  <xsl:template match="partMightBeUpdated">
    <h3 class="dsydiff">
      <i18n:translate>
        <i18n:text key="diff.part-mightbe-changed"/>
        <i18n:param><xsl:value-of select="@typeLabel"/></i18n:param>
      </i18n:translate>
    </h3>
  </xsl:template>

  <xsl:template match="fields">
    <xsl:param name="version1Id"/>
    <xsl:param name="version2Id"/>

    <xsl:if test="*">
      <h2 class="dsydiff"><i18n:text key="diff.field-changes"/></h2>
      <table style="padding: 1em; width: 100%; table-layout: fixed;">
        <xsl:apply-templates/>
      </table>
    </xsl:if>
  </xsl:template>

  <xsl:template match="fieldRemoved">
    <tr>
      <td style="color: grey;"><span style="diffpage-removedfield"><xsl:value-of select="@typeLabel"/></span></td>
      <td><span style="diffpage-removedfield"><xsl:value-of select="@version1"/></span></td>
      <td/>
      <td><span class="diffpage-nochanges"><i18n:text key="diff.deleted-field"/></span></td>
    </tr>
  </xsl:template>

  <xsl:template match="fieldAdded">
    <tr>
      <td style="color: grey;"><xsl:value-of select="@typeLabel"/></td>
      <td><span class="diffpage-nochanges"><i18n:text key="diff.new-field"/></span></td>
      <td/>
      <td><xsl:value-of select="@version2"/></td>
    </tr>
  </xsl:template>

  <xsl:template match="fieldUpdated">
    <xsl:call-template name="valueComparison">
      <xsl:with-param name="label" select="string(@typeLabel)"/>
      <xsl:with-param name="val1" select="@version1"/>
      <xsl:with-param name="val2" select="@version2"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="links">
    <xsl:if test="*">
      <h2 class="dsydiff"><i18n:text key="diff.link-changes"/></h2>

      <xsl:if test="linkRemoved">
        <h3><i18n:text key="diff.removed-links"/></h3>
        <table class="default" style="width: 100%">
          <xsl:for-each select="linkRemoved">
            <tr>
              <td>
                <xsl:value-of select="@title"/><br/>
                <xsl:value-of select="@target"/><br/>
              </td>
            </tr>
          </xsl:for-each>
        </table>
      </xsl:if>

      <xsl:if test="linkAdded">
        <h3><i18n:text key="diff.added-links"/></h3>
        <table class="default" style="width: 100%">
          <xsl:for-each select="linkAdded">
            <tr>
              <td>
                <xsl:value-of select="@title"/><br/>
                <xsl:value-of select="@target"/><br/>
              </td>
            </tr>
          </xsl:for-each>
        </table>
      </xsl:if>
    </xsl:if>
  </xsl:template>

  <xsl:template name="versionInfo">
    <xsl:param name="version"/>
    <xsl:param name="docName"/>

    <div style="padding: .25em;">
      <strong><i18n:text key="diff.version"/><xsl:text> </xsl:text><xsl:value-of select="$version/@id"/></strong>&#160;<span style="color: grey;"><i18n:text key="diff.by"/></span>&#160;<xsl:value-of select="$version/@creatorName"/>
      <br/><span style="color: grey;"><i18n:text key="diff.on"/></span>&#160;<xsl:value-of select="$version/@created"/>
      <br/><span style="color: grey;"><i18n:text key="diff.name"/></span>&#160;<xsl:value-of select="$docName"/>
      <br/><span style="color: grey;"><i18n:text key="diff.variant"/></span>&#160;<xsl:value-of select="concat($version/@branch, ' - ', $version/@language)"/>
      <br/><span style="color: grey;"><i18n:text key="diff.state"/></span>&#160;<i18n:text key="{$version/@state}"/>
    </div>
  </xsl:template>

  <xsl:template name="valueComparison">
    <xsl:param name="label"/>
    <xsl:param name="val1"/>
    <xsl:param name="val2"/>

    <tr>
      <td style="color: grey; text-align: right; width: 18%;">
        <xsl:copy-of select="$label"/>
      </td>
      <td style="text-align: right; width: 27%;"><xsl:value-of select="$val1"/></td>
      <td style="vertical-align: middle; text-align: center; width: 10%;">
        <xsl:choose>
          <xsl:when test="string($val1) = string($val2)">
            <img src="{$mountPoint}/resources/skins/{$skin}/images/diff_equals.png"/>
          </xsl:when>
          <xsl:otherwise>
            <img src="{$mountPoint}/resources/skins/{$skin}/images/diff_notequals.png"/>
          </xsl:otherwise>
        </xsl:choose>
      </td>
      <td style="width: 45%;"><xsl:value-of select="$val2"/></td>
    </tr>
  </xsl:template>
  
  <xsl:template name="availableVariants">
    <availableVariants>
      <variants>
        <xsl:for-each select="$pubdoc/d:availableVariants/d:availableVariant">
          <xsl:sort select="@branchName"/>
          <xsl:sort select="@languageName"/>
          <!-- exclude retired versions and show unpublished variants only to editors -->
          <xsl:if test="@retired = 'false'">
            <xsl:if test="@liveVersionId != '-1' or ($isEditor and @liveVersionId = '-1')">
              <xsl:variable name="nextToLast"><xsl:choose>
                <xsl:when test="@lastVersionId = '1'">1</xsl:when>
                <xsl:otherwise><xsl:value-of select="number(@lastVersionId) - 1"/></xsl:otherwise>
              </xsl:choose></xsl:variable>
              <xsl:choose>
                <xsl:when test="@branchId = $pubdoc/@branchId and @languageId = $pubdoc/@languageId">
                  <variant href="{concat($mountPoint, '/', $siteName, '/', $documentId, '/version/', $version1Id, '/diff?otherVersion=', $version2Id, '&amp;branch=', @branchName, '&amp;language=', @languageName)}"
                    branchName="{@branchName}" languageName="{@languageName}" current="true"/>
                </xsl:when>
                <xsl:otherwise>
                  <variant href="{concat($mountPoint, '/', $siteName, '/', $documentId, '/version/', $nextToLast, '/diff?otherVersion=', @lastVersionId, '&amp;branch=', @branchName, '&amp;language=', @languageName)}"
                    branchName="{@branchName}" languageName="{@languageName}"/>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:if>
          </xsl:if>
        </xsl:for-each>
      </variants>
      <xsl:if test="$isEditor">
        <createVariant href="{concat($documentPath, '/createVariant', $variantQueryString)}"/>
      </xsl:if>
    </availableVariants>
  </xsl:template>

</xsl:stylesheet>