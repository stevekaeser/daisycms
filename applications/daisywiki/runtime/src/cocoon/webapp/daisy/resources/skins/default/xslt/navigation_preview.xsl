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
  xmlns:n="http://outerx.org/daisy/1.0#navigation">

  <xsl:import href="daisyskin:xslt/pagevars.xsl"/>
  <xsl:import href="daisyskin:xslt/navigation.xsl"/>

  <xsl:variable name="mountPoint" select="string(page/context/mountPoint)"/>
  <xsl:variable name="siteName" select="string(page/context/site/@name)"/>
  <xsl:variable name="basePath" select="concat($mountPoint, '/', $siteName)"/>

  <xsl:template match="/">
    <html>
      <head>
        <title>Navigation tree preview</title>
      </head>
      <body>
        <xsl:apply-templates select="page/n:navigationTree"/>
      </body>
    </html>
  </xsl:template>

</xsl:stylesheet>