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
  xmlns:d="http://outerx.org/daisy/1.0">

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><xsl:value-of select="title"/></pageTitle>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1><xsl:value-of select="title"/></h1>

    <xsl:value-of select="message"/>

    <br/>
    <br/>
    <a href="{link}"><xsl:value-of select="linkTitle"/></a>
  </xsl:template>

</xsl:stylesheet>