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
  <xsl:variable name="resultId">timerSearchResult</xsl:variable> <!-- required by wf_search_common.xsl -->

  <!-- Note: wf_search_common.xsl contains the root template (matching on 'page')
       where the output generation starts. -->

  <xsl:template name="content">
    <i18n:text key="wfsearch.timers-info"/>
    <xsl:choose>
      <xsl:when test="wf:searchResult/wf:rows/wf:row">
        <xsl:apply-templates select="wf:searchResult"/>
      </xsl:when>
      <xsl:otherwise>
        <i18n:text key="wfsearch.no-timers-found"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="wf:searchResult">
    <table class="default">
      <tr>
        <td colspan="7" style="text-align: right">
          <xsl:call-template name="resultNavigation"/>
        </td>
      </tr>
      <tr>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$timerIdPos"/>
            <xsl:with-param name="seq">0</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$timerNamePos"/>
            <xsl:with-param name="seq">1</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$timerDueDatePos"/>
            <xsl:with-param name="seq">2</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$timerRecurrencePos"/>
            <xsl:with-param name="seq">3</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$timerSuspendedPos"/>
            <xsl:with-param name="seq">4</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$timerFailedPos"/>
            <xsl:with-param name="seq">5</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$processIdPos"/>
            <xsl:with-param name="seq">6</xsl:with-param>
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
    <tr>
      <td><xsl:apply-templates select="wf:value[$timerIdPos]"/></td>
      <td><xsl:apply-templates select="wf:value[$timerNamePos]"/></td>
      <td><xsl:apply-templates select="wf:value[$timerDueDatePos]"/></td>
      <td><xsl:apply-templates select="wf:value[$timerRecurrencePos]"/></td>
      <td><xsl:apply-templates select="wf:value[$timerSuspendedPos]"/></td>
      <td><xsl:apply-templates select="wf:value[$timerFailedPos]"/></td>
      <td>
        <a href="process/{wf:value[$processIdPos]/wf:raw}">
          <xsl:apply-templates select="wf:value[$processIdPos]"/>
        </a>
      </td>
      <td>
        <xsl:variable name="timerId" select="string(wf:value[$timerIdPos]/wf:raw)"/>
        <a href="timer/{$timerId}"><i18n:text key="wfsearch.action.view"/></a>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>