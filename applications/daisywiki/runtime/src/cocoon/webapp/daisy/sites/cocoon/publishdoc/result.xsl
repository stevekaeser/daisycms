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
  version="1.0">


  <xsl:template match="data">
    <html>
      <head>
        <title><xsl:value-of select="p:publisherResponse/p:document/d:document/@name"/></title>
        <link rel="stylesheet" type="text/css" href="{context/mountPoint}/resources/skins/default/css/daisy.css"/>
      </head>
      <body>
        <insertStyledDocument styledResultsId="{p:publisherResponse/p:document/p:preparedDocuments/@styledResultsId}"/>
      </body>
    </html>
  </xsl:template>

</xsl:stylesheet>