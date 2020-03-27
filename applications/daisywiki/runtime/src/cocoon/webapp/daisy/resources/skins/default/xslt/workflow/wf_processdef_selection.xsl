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
  xmlns:wf="http://outerx.org/daisy/1.0#workflow"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:urlencoder="xalan://java.net.URLEncoder">

  <xsl:include href="daisyskin:xslt/util.xsl"/>
  <xsl:variable name="mountPoint" select="string(page/context/mountPoint)"/>
  <xsl:variable name="documentLink" select="string(page/documentLink)"/>
  <xsl:variable name="documentName" select="string(page/documentName)"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><i18n:text key="wfpsel.title"/></pageTitle>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1><i18n:text key="wfpsel.title"/></h1>

    <p><i18n:text key="wfpsel.intro"/></p>

    <xsl:choose>
      <xsl:when test="wf:processDefinitions/wf:processDefinition">
        <xsl:apply-templates select="wf:processDefinitions/wf:processDefinition"/>
      </xsl:when>
      <xsl:otherwise>
        <i18n:text key="wfpsel.no-workflows-available"/>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>

  <xsl:template match="wf:processDefinition">
    <div style="margin-bottom: 1em">
      <xsl:call-template name="generatePostLink">
        <xsl:with-param name="action" select="concat('start?processDefinitionId=', @id, '&amp;documentLink=', $documentLink, '&amp;documentName=', $documentName)"/>
        <xsl:with-param name="label" select="string(wf:label)"/>
        <xsl:with-param name="id" select="generate-id(.)"/>
      </xsl:call-template>
      <div style="margin-left: 1em; font-style: italic">
        <xsl:copy-of select="wf:description/node()"/>
      </div>
    </div>
  </xsl:template>

</xsl:stylesheet>