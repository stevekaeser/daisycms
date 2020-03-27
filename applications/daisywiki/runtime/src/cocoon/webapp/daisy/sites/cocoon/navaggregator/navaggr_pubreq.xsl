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
    <p:publisherRequest locale="{locale}">
      <xsl:copy-of select="p:variablesConfig"/>
      <xsl:variable name="activeNode" select="n:navigationTree//*[@active='true']"/>
      <xsl:choose>
        <!-- If there is an active node, only publish documents below that node
             (including the active node itself if it is a document node) -->
        <xsl:when test="$activeNode">
          <!-- the active node could be a group node, so check it is a doc node -->
          <xsl:if test="local-name($activeNode) = 'doc'">
            <xsl:for-each select="$activeNode">
              <xsl:call-template name="createDocRequest"/>
            </xsl:for-each>
          </xsl:if>
          <xsl:for-each select="$activeNode//n:doc">
            <xsl:call-template name="createDocRequest"/>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <xsl:for-each select="n:navigationTree//n:doc">
            <xsl:call-template name="createDocRequest"/>
          </xsl:for-each>
        </xsl:otherwise>
      </xsl:choose>
    </p:publisherRequest>
  </xsl:template>

  <xsl:template name="createDocRequest">
    <p:document id="{@documentId}" branch="{$context/site/@branchId}" language="{$context/site/@languageId}">
      <xsl:copy-of select="@branch | @language"/>
      <p:preparedDocuments applyDocumentTypeStyling="true" publisherRequestSet="{$context/site/@publisherRequestSet}" displayContext="aggregation">
      </p:preparedDocuments>
    </p:document>
  </xsl:template>

</xsl:stylesheet>