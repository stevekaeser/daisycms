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
<!--
   | Some variables needed by multiple XSLT's.
   -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:d="http://outerx.org/daisy/1.0"
  xmlns:p="http://outerx.org/daisy/1.0#publisher"
  xmlns:n="http://outerx.org/daisy/1.0#navigation"
  xmlns:cinclude="http://apache.org/cocoon/include/1.0">

  <xsl:variable name="mountPoint" select="string(page/context/mountPoint)"/>
  <xsl:variable name="siteName" select="string(page/context/site/@name)"/>
  <xsl:variable name="variantParams" select="string(page/variantParams)"/>
  <xsl:variable name="variantQueryString" select="string(page/variantQueryString)"/>
  <xsl:variable name="selectedPath" select="/page/p:publisherResponse/n:navigationTree/@selectedPath"/>
  <xsl:variable name="documentId" select="/page/p:publisherResponse/p:document/@documentId"/>
  <xsl:variable name="documentPath">
    <xsl:choose>
      <xsl:when test="$selectedPath">
        <xsl:value-of select="concat($mountPoint, '/', $siteName, $selectedPath)"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="concat($mountPoint, '/', $siteName, '/', $documentId)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="user" select="page/context/user"/>

</xsl:stylesheet>