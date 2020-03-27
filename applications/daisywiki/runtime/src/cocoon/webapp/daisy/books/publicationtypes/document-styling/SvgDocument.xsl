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
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:d="http://outerx.org/daisy/1.0"
  xmlns:p="http://outerx.org/daisy/1.0#publisher"
  xmlns:cp="http://outerx.org/daisy/1.0#bookCallPipelineTask"
  xmlns:rs="http://outerx.org/daisy/1.0#bookSvgRenderTask">

  <xsl:template match="document">
    <xsl:apply-templates select="p:publisherResponse/d:document"/>
  </xsl:template>

  <xsl:template match="d:document">
    <html>
      <body>
        <!-- Only generate a title if the SVG is not included inside some other document. -->
        <xsl:if test="/document/@isIncluded = 'false'">
          <h0 id="dsy{@id}" daisyDocument="{@id}" daisyBranch="{@branchId}" daisyLanguage="{@languageId}"><xsl:value-of select="@name"/></h0>
        </xsl:if>

        <xsl:variable name="svgBookStorePath" select="d:parts/d:part[@name='SvgData']/@bookStorePath"/>

        <rs:renderSVG bookStorePath="{$svgBookStorePath}"/>

      </body>
    </html>
  </xsl:template>

</xsl:stylesheet>