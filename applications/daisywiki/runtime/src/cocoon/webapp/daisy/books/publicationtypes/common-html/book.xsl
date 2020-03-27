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
  xmlns:p="http://outerx.org/daisy/1.0#publisher">

  <xsl:template match="html">
    <html>
      <xsl:apply-templates select="body"/>
    </html>
  </xsl:template>

  <xsl:template match="body">
    <body>
      <xsl:apply-templates/>
    </body>
  </xsl:template>

  <xsl:template match="toc">
    <h1 class="booktitle"><xsl:value-of select="/html/metadata/entry[@key='title']"/></h1>
    <h2><i18n:text key="toc"/></h2>
    <div class="toc">
      <ul>
        <xsl:apply-templates select="tocEntry"/>
      </ul>
    </div>
  </xsl:template>

  <xsl:template match="tocEntry">
    <li>
      <a href="#{@targetId}">
        <xsl:if test="@daisyNumber">
          <xsl:value-of select="@daisyNumber"/><xsl:text> </xsl:text>
        </xsl:if>
        <xsl:apply-templates select="caption/node()"/>
      </a>
      <xsl:if test="tocEntry">
        <ul>
          <xsl:apply-templates select="tocEntry"/>
        </ul>
      </xsl:if>
    </li>
  </xsl:template>

  <xsl:template match="table">
    <table>
      <xsl:copy-of select="@*"/>
      <xsl:if test="not(@class)">
        <xsl:attribute name="class">content</xsl:attribute>
      </xsl:if>
      <xsl:if test="@daisy-caption != ''">
        <caption>
          <xsl:if test="@daisyNumber">
            <i18n:text key="table.{@daisy-table-type}"/><xsl:text> </xsl:text><xsl:value-of select="@daisyNumber"/><xsl:text>: </xsl:text>
          </xsl:if>
          <xsl:value-of select="@daisy-caption"/>
        </caption>
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
          <xsl:apply-templates select="node()"/>
        </xsl:copy>
      </xsl:for-each>
    </table>
  </xsl:template>

  <xsl:template match="table/computedInfo">
    <!-- coputedInfo in tables, drop this -->
  </xsl:template>

  <xsl:template match="img">
    <xsl:choose>
      <xsl:when test="@daisy-caption">
        <table class="plainTable">
          <tr>
            <td><xsl:call-template name="insertImage"/></td>
          </tr>
          <tr>
            <td class="imageCaption">
              <xsl:if test="@daisyNumber">
                <i18n:text key="figure.{@daisy-image-type}"/><xsl:text> </xsl:text><xsl:value-of select="@daisyNumber"/><xsl:text>: </xsl:text>
              </xsl:if>
              <xsl:value-of select="@daisy-caption"/>
            </td>
          </tr>
        </table>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="insertImage"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="insertImage">
    <img>
      <xsl:copy-of select="@*"/>
      <xsl:if test="p:linkInfo/@documentName">
        <xsl:attribute name="alt"><xsl:value-of select="p:linkInfo/@documentName"/></xsl:attribute>
        <xsl:attribute name="title"><xsl:value-of select="p:linkInfo/@documentName"/></xsl:attribute>
      </xsl:if>
      <xsl:apply-templates/>
    </img>
  </xsl:template>

  <xsl:template match="p:linkInfo" mode="PartContent">
    <!-- empty template, becuase p:linkInfo should be removed from output -->
  </xsl:template>

  <xsl:template match="span[@class='indexentry']">
    <!-- remove index entries from output -->
  </xsl:template>

  <xsl:template match="h1|h2|h3|h4|h5|h6|h7|h8|h9">
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:if test="@daisyNumber">
        <xsl:value-of select="@daisyNumber"/><xsl:text> </xsl:text>
      </xsl:if>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="list-of-figures">
    <h2><i18n:text key="list-of-figures.{@type}"/></h2>
    <table class="borderless" width="100%">
      <col width="5%"/>
      <col width="95%"/>
      <tbody>
        <xsl:apply-templates select="list-item"/>
      </tbody>
    </table>
  </xsl:template>

  <xsl:template match="list-of-tables">
    <h2><i18n:text key="list-of-tables.{@type}"/></h2>
    <table class="borderless" width="100%">
      <col width="5%"/>
      <col width="95%"/>
      <tbody>
        <xsl:apply-templates select="list-item"/>
      </tbody>
    </table>
  </xsl:template>

  <xsl:template match="list-item">
    <tr>
      <td><a href="#{@targetId}"><xsl:value-of select="@daisyNumber"/></a></td>
      <td>
        <xsl:apply-templates select="node()"/>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="span[@class='crossreference']">
    <xsl:variable name="crossRefType" select="string(@crossRefType)"/>
    <xsl:variable name="targetId" select="substring(string(@crossRefBookTarget), 2)"/> <!-- substring is to drop the '#' before the target -->
    <xsl:variable name="reftypes">ref,reftitle,ref+reftitle,ref+reftitle+textpage,ref+textpage,</xsl:variable>
    <xsl:variable name="validRefTargets">img,table,h1,h2,h3,h4,h5,h6,h7,h8,h9,</xsl:variable>

    <xsl:choose>
      <xsl:when test="$crossRefType = 'invalid'">
        <span class="invalidcrossref">[invalid cross reference]</span>
      </xsl:when>
      <xsl:when test="$crossRefType = 'page'">
        <a href="#{$targetId}"><i18n:text key="crossref.here"/></a>
      </xsl:when>
      <xsl:when test="$crossRefType = 'textpage'">
        <a href="#{$targetId}"><i18n:text key="crossref.here"/></a>
      </xsl:when>
      <xsl:when test="contains($reftypes, concat($crossRefType, ','))">
        <xsl:variable name="targetEl" select="//*[@id=$targetId][1]"/>
        <xsl:choose>
          <xsl:when test="not($targetEl)">
            <span class="invalidcrossref">[cross reference error: target element not found]</span>
          </xsl:when>
          <xsl:when test="not(contains($validRefTargets, concat(local-name($targetEl), ',')))">
            <span class="invalidcrossref">[cross reference error: target is not a header, image or table]</span>
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
                <span class="invalidcrossref"><xsl:value-of select="$error"/></span>
              </xsl:when>
              <xsl:when test="$crossRefType = 'ref'">
                <a href="#{$targetId}">
                  <i18n:text key="{$crossrefkey}"/>
                  <xsl:text> </xsl:text>
                  <xsl:value-of select="$targetEl/@daisyNumber"/>
                </a>
              </xsl:when>
              <xsl:when test="$crossRefType = 'reftitle'">
                <a href="#{$targetId}">
                  <xsl:call-template name="crossRefTargetTitle">
                    <xsl:with-param name="target" select="$targetEl"/>
                  </xsl:call-template>
                </a>
              </xsl:when>
              <xsl:when test="$crossRefType = 'ref+reftitle' or $crossRefType = 'ref+reftitle+textpage'">
                <!-- for HTML rendering, we ignore the "+textpage" -->
                <a href="#{$targetId}">
                  <i18n:text key="{$crossrefkey}"/>
                  <xsl:text> </xsl:text>
                  <xsl:value-of select="$targetEl/@daisyNumber"/>
                  <xsl:text> &#8220;</xsl:text>
                  <xsl:call-template name="crossRefTargetTitle">
                    <xsl:with-param name="target" select="$targetEl"/>
                  </xsl:call-template>
                  <xsl:text>&#8221; </xsl:text>
                </a>
              </xsl:when>
              <xsl:when test="$crossRefType = 'ref+textpage'">
                <a href="#{$targetId}">
                  <i18n:text key="{$crossrefkey}"/>
                  <xsl:text> </xsl:text>
                  <xsl:value-of select="$targetEl/@daisyNumber"/>
                </a>
              </xsl:when>
            </xsl:choose>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <span class="invalidcrossref">[unknown cross reference type: <xsl:value-of select="$crossRefType"/>]</span>
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

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
