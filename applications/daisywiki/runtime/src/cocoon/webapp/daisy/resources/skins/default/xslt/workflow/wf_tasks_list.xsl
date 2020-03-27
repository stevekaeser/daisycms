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
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:import href="daisyskin:xslt/workflow/wf_tasks_searchresult.xsl"/>

  <xsl:variable name="selection" select="string(page/selection)"/>

  <!-- required by wf_search_common.xsl -->
  <xsl:variable name="resultId">
    <xsl:choose>
      <xsl:when test="$selection = 'pooled'">pooledTasks</xsl:when>
      <xsl:otherwise>myTasks</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- Note: wf_search_common.xsl contains the root template (matching on 'page')
       where the output generation starts. -->

  <xsl:template name="content">
    <xsl:choose>
      <xsl:when test="wf:searchResult/wf:rows/wf:row">
        <xsl:apply-templates select="wf:searchResult"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:choose>
          <xsl:when test="$selection = 'mine'"><i18n:text key="wftasks.none-assigned-to-you"/></xsl:when>
          <xsl:when test="$selection = 'pooled'"><i18n:text key="wftasks.none-pooled"/></xsl:when>
        </xsl:choose>
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
            <xsl:with-param name="resultPos" select="$taskIdPos"/>
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
            <xsl:with-param name="resultPos" select="$taskDefLabelPos"/>
            <xsl:with-param name="seq">2</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$taskPriorityPos"/>
            <xsl:with-param name="seq">3</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$taskCreatePos"/>
            <xsl:with-param name="seq">4</xsl:with-param>
          </xsl:call-template>
          <br/>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$taskDueDatePos"/>
            <xsl:with-param name="seq">5</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          Actions
        </th>
      </tr>
      <xsl:apply-templates select="wf:rows/wf:row"/>
    </table>
  </xsl:template>

  <xsl:template match="wf:row">
    <tr>
      <td><xsl:apply-templates select="wf:value[$taskIdPos]"/></td>
      <td><xsl:apply-templates select="wf:value[$descrPos]"/></td>
      <td><xsl:apply-templates select="wf:value[$taskDefLabelPos]"/></td>
      <td><xsl:apply-templates select="wf:value[$taskPriorityPos]"/></td>
      <td>
        <xsl:apply-templates select="wf:value[$taskCreatePos]"/>
        <br/>
        <xsl:apply-templates select="wf:value[$taskDueDatePos]"/>
      </td>
      <td>
        <xsl:call-template name="taskActions">
          <xsl:with-param name="differentiator" select="$selection"/>
        </xsl:call-template>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>