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
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:param name="bookTitle"/>

  <xsl:template match="html">
    <html>
      <head>
        <link rel="stylesheet" type="text/css" href="resources/css/books.css"/>
      </head>
      <xsl:apply-templates select="body"/>
    </html>
  </xsl:template>

  <xsl:template match="body">
    <body>
      <div class="top">
        <div class="toparea" style="text-align: center">
          <xsl:value-of select="$bookTitle"/>
          <br/>
          <a href="../../../../../books"><i18n:text key="bookindex"/></a>
        </div>
      </div>
      <div class="content">
        <xsl:apply-templates/>
      </div>
      <div class="bottom">
        <div class="bottomarea">
          <br/>
        </div>
      </div>
    </body>
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>