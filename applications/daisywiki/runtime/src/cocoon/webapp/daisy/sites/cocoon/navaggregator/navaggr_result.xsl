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
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:d="http://outerx.org/daisy/1.0"
  xmlns:p="http://outerx.org/daisy/1.0#publisher"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:n="http://outerx.org/daisy/1.0#navigation"
  version="1.0">

  <xsl:variable name="context" select="/navAggrData/context"/>

  <xsl:template match="navAggrData">
    <html>
      <head>
        <title>Aggregated navigation tree documents</title>
        <link rel="stylesheet" type="text/css" href="{context/mountPoint}/resources/skins/default/css/daisy.css"/>
      </head>
      <body>
        <xsl:for-each select="p:publisherResponse/p:document/p:preparedDocuments">
          <insertStyledDocument styledResultsId="{@styledResultsId}"/>
          <xsl:if test="position() != last()">
            <hr/>            
          </xsl:if>
        </xsl:for-each>
      </body>
    </html>
  </xsl:template>

</xsl:stylesheet>