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

  <xsl:import href="daisyskin:xslt/workflow/wf_search_common.xsl"/>
  <xsl:include href="daisyskin:xslt/util.xsl"/>
  <xsl:variable name="resultId">processSearchResult</xsl:variable> <!-- required by wf_search_common.xsl -->

  <!-- Note: wf_search_common.xsl contains the root template (matching on 'page')
       where the output generation starts. -->

  <xsl:template name="content">
    <xsl:choose>
      <xsl:when test="wf:searchResult/wf:rows/wf:row">
        <xsl:apply-templates select="wf:searchResult"/>
      </xsl:when>
      <xsl:otherwise>
        <i18n:text key="wfsearch.no-processes-found"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="wf:searchResult">
    <table class="default">
      <tr>
        <td colspan="8" style="text-align: right">
          <xsl:call-template name="resultNavigation"/>
        </td>
      </tr>
      <tr>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$processIdPos"/>
            <xsl:with-param name="seq">0</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$descrPos"/>
            <xsl:with-param name="customTitle"><i18n:text key="wfsearch.alt-title.description"/></xsl:with-param>
            <xsl:with-param name="seq">1</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$processStartPos"/>
            <xsl:with-param name="seq">2</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$processEndPos"/>
            <xsl:with-param name="seq">3</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$processOwnerPos"/>
            <xsl:with-param name="customTitle"><i18n:text key="wfsearch.alt-title.owner"/></xsl:with-param>
            <xsl:with-param name="seq">4</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$processSuspendedPos"/>
            <xsl:with-param name="customTitle"><i18n:text key="wfsearch.alt-title.suspension"/></xsl:with-param>
            <xsl:with-param name="seq">5</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          <i18n:text key="searchresult.actions"/>
        </th>
      </tr>
      <xsl:apply-templates select="wf:rows/wf:row"/>
    </table>
  </xsl:template>

  <xsl:template match="wf:row">
    <xsl:variable name="processId" select="string(wf:value[$processIdPos]/wf:raw)"/>
    <xsl:variable name="processSuspended" select="boolean(string(wf:value[$processSuspendedPos]/wf:raw) = 'true')"/>

    <tr>
      <td><xsl:apply-templates select="wf:value[$processIdPos]"/></td>
      <td><xsl:apply-templates select="wf:value[$descrPos]"/></td>
      <td><xsl:apply-templates select="wf:value[$processStartPos]"/></td>
      <td><xsl:apply-templates select="wf:value[$processEndPos]"/></td>
      <td><xsl:apply-templates select="wf:value[$processOwnerPos]"/></td>
      <td>
        <xsl:choose>
          <xsl:when test="$processSuspended">
            <xsl:call-template name="generatePostLink">
              <xsl:with-param name="action" select="concat('process/', $processId, '?action=resume&amp;returnTo=', $returnTo, '&amp;returnToLabel=', $returnToLabel)"/>
              <xsl:with-param name="label"><i18n:text key="wfsearch.action.resume"/></xsl:with-param>
              <xsl:with-param name="confirmMessage">
                <i18n:translate>
                  <i18n:text key="wfsearch.action-confirm.resume"/>
                  <i18n:param><xsl:value-of select="wf:value[$descrPos]/wf:raw"/></i18n:param>
                </i18n:translate>
              </xsl:with-param>
              <xsl:with-param name="id" select="concat('resume-', generate-id(.))"/>
            </xsl:call-template>
          </xsl:when>
          <xsl:when test="not($processSuspended) and not(wf:value[$processEndPos]/wf:raw)">
            <xsl:call-template name="generatePostLink">
              <xsl:with-param name="action" select="concat('process/', $processId, '?action=suspend&amp;returnTo=', $returnTo, '&amp;returnToLabel=', $returnToLabel)"/>
              <xsl:with-param name="label"><i18n:text key="wfsearch.action.suspend"/></xsl:with-param>
              <xsl:with-param name="confirmMessage">
                <i18n:translate>
                  <i18n:text key="wfsearch.action-confirm.suspend"/>
                  <i18n:param><xsl:value-of select="wf:value[$descrPos]/wf:raw"/></i18n:param>
                </i18n:translate>
              </xsl:with-param>
              <xsl:with-param name="id" select="concat('suspend-', generate-id(.))"/>
            </xsl:call-template>
          </xsl:when>
        </xsl:choose>
      </td>
      <td>
        <a href="process/{$processId}"><i18n:text key="wfsearch.action.view"/></a>
        |
        <xsl:call-template name="generatePostLink">
          <xsl:with-param name="action" select="concat('process/', $processId, '?action=delete&amp;returnTo=', $returnTo, '&amp;returnToLabel=', $returnToLabel)"/>
          <xsl:with-param name="label"><i18n:text key="wfsearch.action.delete"/></xsl:with-param>
          <xsl:with-param name="confirmMessage">
            <i18n:translate>
              <i18n:text key="wfsearch.action-confirm.delete"/>
              <i18n:param><xsl:value-of select="wf:value[$descrPos]/wf:raw"/></i18n:param>
            </i18n:translate>
          </xsl:with-param>
          <xsl:with-param name="id" select="concat('delete-', generate-id(.))"/>
        </xsl:call-template>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>