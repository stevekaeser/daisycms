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
        xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:param name="bookTitle"/>

  <xsl:template match="/chunks/chunk/html">
    <html>
      <head>
        <link rel="stylesheet" type="text/css" href="resources/css/books.css"/>
      </head>
      <xsl:apply-templates select="body"/>
    </html>
  </xsl:template>

  <xsl:template match="body">
    <xsl:variable name="prevChunk" select="../../preceding::chunk[1]"/>
    <xsl:variable name="nextChunk" select="../../following::chunk[1]"/>

    <xsl:variable name="prevChunkTitle" select="$prevChunk/html/body/*[1]"/>
    <xsl:variable name="nextChunkTitle" select="$nextChunk/html/body/*[1]"/>

    <body>
      <div class="top">
        <div class="toparea">
          <xsl:call-template name="chunkNavigation">
            <xsl:with-param name="prevChunk" select="$prevChunk"/>
            <xsl:with-param name="nextChunk" select="$nextChunk"/>
            <xsl:with-param name="prevChunkTitle" select="$prevChunkTitle"/>
            <xsl:with-param name="nextChunkTitle" select="$nextChunkTitle"/>
            <xsl:with-param name="top" select="1=1"/>
          </xsl:call-template>
        </div>
      </div>
      <div class="content">
        <xsl:apply-templates/>
      </div>
      <div class="bottom">
        <div class="bottomarea">
          <xsl:call-template name="chunkNavigation">
            <xsl:with-param name="prevChunk" select="$prevChunk"/>
            <xsl:with-param name="nextChunk" select="$nextChunk"/>
            <xsl:with-param name="prevChunkTitle" select="$prevChunkTitle"/>
            <xsl:with-param name="nextChunkTitle" select="$nextChunkTitle"/>
          </xsl:call-template>
        </div>
      </div>
    </body>
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template name="chunkNavigation">
    <xsl:param name="top"/>
    <xsl:param name="prevChunk"/>
    <xsl:param name="nextChunk"/>
    <xsl:param name="prevChunkTitle"/>
    <xsl:param name="nextChunkTitle"/>

    <div class="chunkNavigation">
      <table style="width: 100%;" class="chunkNavigation">
        <col width="40%"/>
        <col width="20%"/>
        <col width="40%"/>
        <tbody>
          <xsl:if test="$top">
            <tr>
              <td colspan="3" style="text-align: center">
                <xsl:value-of select="$bookTitle"/>
              </td>
            </tr>
          </xsl:if>
          <tr>
            <td>
              <xsl:if test="$prevChunk">
                <a href="{$prevChunk/@name}.html"><img src="resources/images/left_arrow.png"/>&#160;<i18n:text key="previous"/></a>
              </xsl:if>
            </td>
            <td style="text-align: center;">
              <a href="index.html"><i18n:text key="home"/></a>
            </td>
            <td style="text-align: right;">
              <xsl:if test="$nextChunk">
                <a href="{$nextChunk/@name}.html"><i18n:text key="next"/>&#160;<img src="resources/images/right_arrow.png"/></a>
              </xsl:if>
            </td>
          </tr>
          <tr>
            <td>
              <xsl:copy-of select="$prevChunkTitle/node()"/>
            </td>
            <td style="text-align:center">
              <xsl:if test="$top">
                <a href="../../../../../books"><i18n:text key="bookindex"/></a>
              </xsl:if>
            </td>
            <td style="text-align: right;">
              <xsl:copy-of select="$nextChunkTitle/node()"/>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </xsl:template>

</xsl:stylesheet>