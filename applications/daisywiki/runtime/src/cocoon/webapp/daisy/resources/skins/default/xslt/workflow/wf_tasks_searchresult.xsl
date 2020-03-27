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
  <xsl:variable name="resultId">taskSearchResult</xsl:variable> <!-- required by wf_search_common.xsl -->

  <xsl:variable name="userId" select="string(/page/context/user/id)"/>
  <xsl:variable name="isAdmin" select="boolean(/page/context/user/activeRoles/role[@id='1'])"></xsl:variable>

  <!-- Note: wf_search_common.xsl contains the root template (matching on 'page')
       where the output generation starts. -->

  <xsl:template name="content">
    <xsl:choose>
      <xsl:when test="wf:searchResult/wf:rows/wf:row">
        <xsl:apply-templates select="wf:searchResult"/>
      </xsl:when>
      <xsl:otherwise>
        <i18n:text key="wfsearch.no-tasks-found"/>
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
          <br/>
          <i>
            <xsl:call-template name="header">
              <xsl:with-param name="resultPos" select="$taskDefLabelPos"/>
              <xsl:with-param name="seq">2</xsl:with-param>
            </xsl:call-template>
          </i>
        </th>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$taskIsOpenPos"/>
            <xsl:with-param name="seq">3</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$taskPriorityPos"/>
            <xsl:with-param name="seq">4</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$taskCreatePos"/>
            <xsl:with-param name="seq">5</xsl:with-param>
          </xsl:call-template>
          <br/>
          <i>
            <xsl:call-template name="header">
              <xsl:with-param name="resultPos" select="$taskDueDatePos"/>
              <xsl:with-param name="seq">6</xsl:with-param>
            </xsl:call-template>
          </i>
        </th>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$taskActorPos"/>
            <xsl:with-param name="seq">7</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          <xsl:call-template name="header">
            <xsl:with-param name="resultPos" select="$processIdPos"/>
            <xsl:with-param name="seq">8</xsl:with-param>
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
      <td><xsl:apply-templates select="wf:value[$taskIdPos]"/></td>
      <td>
        <xsl:apply-templates select="wf:value[$descrPos]"/>
        <br/>
        <i><xsl:apply-templates select="wf:value[$taskDefLabelPos]"/></i>
      </td>
      <td>
        <xsl:apply-templates select="wf:value[$taskIsOpenPos]"/>
      </td>
      <td>
        <xsl:apply-templates select="wf:value[$taskPriorityPos]"/>
      </td>
      <td>
        <xsl:apply-templates select="wf:value[$taskCreatePos]"/>
        <br/>
        <xsl:choose>
          <xsl:when test="not(wf:value[$taskDueDatePos]/wf:raw)">
            <i>undefined</i>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="wf:value[$taskDueDatePos]"/>
          </xsl:otherwise>
        </xsl:choose>
      </td>
      <td>
        <xsl:apply-templates select="wf:value[$taskActorPos]"/>
      </td>
      <td>
        <a href="process/{wf:value[$processIdPos]/wf:raw}">
          <xsl:apply-templates select="wf:value[$processIdPos]"/>
        </a>
      </td>
      <td>
        <xsl:call-template name="taskActions"/>
      </td>
    </tr>
  </xsl:template>

  <!-- Note: this template is also reused by wf_tasks_list.xsl -->
  <xsl:template name="taskActions">
    <xsl:param name="differentiator">d</xsl:param> <!-- used to generate unique IDs in case multiple tasks lists are shown on one HTML page -->

    <xsl:if test="not(wf:value[$taskEndPos]/wf:raw)">
      <xsl:variable name="taskId" select="string(wf:value[$taskIdPos]/wf:raw/node())"/>
      <xsl:variable name="actorId" select="string(wf:value[$taskActorPos]/wf:raw/node())"/>
      <xsl:variable name="processSuspended" select="boolean(string(wf:value[$processSuspendedPos]/wf:raw) = 'true')"/>
      <xsl:choose>
        <xsl:when test="$actorId = $currentUserId">
          <xsl:if test="not($processSuspended)">
            <xsl:call-template name="generatePostLink">
              <xsl:with-param name="action" select="concat('performtask?taskId=', $taskId)"/>
              <xsl:with-param name="label"><i18n:text key="wfsearch.action.open"/></xsl:with-param>
              <xsl:with-param name="id" select="concat($differentiator, '-perform-', generate-id(.))"/>
            </xsl:call-template>

            <xsl:if test="string(wf:value[$taskHasPoolsPos]/wf:raw/node()) = 'true'">
              <xsl:text>&#160;|&#160;</xsl:text>
              <xsl:call-template name="generatePostLink">
                <xsl:with-param name="action" select="concat('task/', $taskId, '?action=assignBackToPools&amp;returnTo=', $returnTo)"/>
                <xsl:with-param name="label"><i18n:text key="wfsearch.action.pool"/></xsl:with-param>
                <xsl:with-param name="confirmMessage"><i18n:text key="wfsearch.action-confirm.assign-to-pool"/></xsl:with-param>
                <xsl:with-param name="id" select="concat($differentiator, '-unassign-', generate-id(.))"/>
              </xsl:call-template>
            </xsl:if>
          </xsl:if>
        </xsl:when>
        <xsl:when test="$actorId = ''">
          <xsl:call-template name="generatePostLink">
            <xsl:with-param name="action" select="concat('task/', $taskId, '?action=requestPooledTask&amp;returnTo=', $returnTo)"/>
            <xsl:with-param name="label"><i18n:text key="wfsearch.action.assign-to-me"/></xsl:with-param>
            <xsl:with-param name="id" select="concat($differentiator, '-me-', generate-id(.))"/>
          </xsl:call-template>
        </xsl:when>
      </xsl:choose>
      <xsl:if test="$isAdmin or wf:value[$processOwnerPos]/wf:raw = $userId">
        <xsl:text>&#160;|&#160;</xsl:text>
        <xsl:call-template name="generatePostLink">
          <xsl:with-param name="action" select="concat('task/', $taskId, '/assign?returnTo=', $returnTo)"/>
          <xsl:with-param name="label"><i18n:text key="wfsearch.action.assign"/></xsl:with-param>
          <xsl:with-param name="id" select="concat($differentiator, '-assign-', generate-id(.))"/>
        </xsl:call-template>
      </xsl:if>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>