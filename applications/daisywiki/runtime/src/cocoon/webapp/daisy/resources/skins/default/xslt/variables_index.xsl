<!--
  Copyright 2007 Outerthought bvba and Schaubroeck nv

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
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:p="http://outerx.org/daisy/1.0#publisher"
  xmlns:n="http://outerx.org/daisy/1.0#navigation"
  xmlns:urlencoder="xalan://java.net.URLEncoder">

  <xsl:import href="daisyskin:xslt/util.xsl"/>

  <xsl:variable name="basePath" select="concat(/page/context/mountPoint, '/', /page/context/site/@name, '/')"/>
  <xsl:variable name="requestMethod" select="page/context/request/@method"/>
  <xsl:variable name="pageURI" select="page/context/request/@uri"/>
  <xsl:variable name="pageURIEncoded" select="urlencoder:encode($pageURI, 'UTF-8')"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><i18n:text key="varsindex.title"/></pageTitle>
      <content><xsl:call-template name="content"/></content>
      <xsl:copy-of select="p:publisherResponse/n:navigationTree"/>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1><i18n:text key="varsindex.title"/></h1>

    <xsl:variable name="varDocs" select="p:publisherResponse/p:group[@id='variableDocuments']/p:document"/>

    <xsl:choose>
      <xsl:when test="$varDocs">
        <p><i18n:text key="varsindex.vardocsintro"/></p>
        <table>
          <tbody>
            <xsl:for-each select="$varDocs">
              <tr>
                <td><xsl:value-of select="d:document/@name"/></td>
                <td>
                  <xsl:call-template name="variableDocActions"/>
                </td>
              </tr>
            </xsl:for-each>
          </tbody>
        </table>
      </xsl:when>
      <xsl:otherwise>
        <p><i18n:text key="varsindex.novarsdefined"/></p>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="variableDocActions">
    <xsl:variable name="perm" select="d:aclResult/d:permissions"/>

    <xsl:if test="$perm/d:permission[@type='read' and @action='grant']">
      <a href="{$basePath}{@documentId}?branch={@branch}&amp;language={@language}"><i18n:text key="varsindex.view"/></a>
    </xsl:if>

    <xsl:if test="$perm/d:permission[@type='write' and @action='grant']">
      <xsl:text> | </xsl:text>

      <xsl:variable name="url">
        <xsl:choose>
          <xsl:when test="$requestMethod = 'GET'">
            <xsl:value-of select="concat($basePath, @documentId, '/edit?branch=', @branch, '&amp;language=', @language, '&amp;returnTo=', $pageURIEncoded)"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat($basePath, @documentId, '/edit?branch=', @branch, '&amp;language=', @language)"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

      <xsl:call-template name="generatePostLink">
        <xsl:with-param name="action" select="$url"/>
        <xsl:with-param name="id" select="generate-id(.)"/>
        <xsl:with-param name="label"><i18n:text key="varsindex.edit"/></xsl:with-param>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>  

</xsl:stylesheet>