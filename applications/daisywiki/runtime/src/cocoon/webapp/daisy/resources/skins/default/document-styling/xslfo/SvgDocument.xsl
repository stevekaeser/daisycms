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
  xmlns:fo="http://www.w3.org/1999/XSL/Format">

  <xsl:import href="daisyskin:xslt/document-to-xslfo.xsl"/>
  <xsl:param name="documentBasePath"/>

  <xsl:template match="d:document">
    <xsl:variable name="server" select="/document/context/request/@server"/>
    <xsl:variable name="url" select="concat('daisy:', @id, '@', @branch, ':', @language, ':', @dataVersionId, '!SvgData')"/>
    <fo:external-graphic src="url('{$url}')" width="75%" content-width="75%"/>
  </xsl:template>

</xsl:stylesheet>