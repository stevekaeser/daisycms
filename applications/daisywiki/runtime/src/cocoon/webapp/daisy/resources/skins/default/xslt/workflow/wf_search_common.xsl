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

  <xsl:variable name="mountPoint" select="string(page/context/mountPoint)"/>
  <xsl:variable name="skin" select="string(page/context/skin)"/>
  <xsl:variable name="resultId">undefined</xsl:variable> <!-- this variable needs to be specified in the importing stylesheet -->
  <xsl:variable name="currentUserId" select="string(page/context/user/id)"/>
  <xsl:variable name="returnTo" select="urlencoder:encode(/page/actionReturnTo/@url, 'UTF-8')"/>
  <xsl:variable name="returnToLabel" select="urlencoder:encode(/page/actionReturnTo/@label, 'UTF-8')"/>

  <xsl:variable name="searchResultTitles" select="/page/wf:searchResult/wf:titles/wf:title"/>
  <xsl:variable name="taskIdPos" select="count($searchResultTitles[@name='task.id' and @source='property']/preceding-sibling::*) + 1"/>
  <xsl:variable name="descrPos" select="count($searchResultTitles[@name='daisy_description' and @source='process_variable']/preceding-sibling::*) + 1"/>
  <xsl:variable name="taskCreatePos" select="count($searchResultTitles[@name='task.create' and @source='property']/preceding-sibling::*) + 1"/>
  <xsl:variable name="taskDefLabelPos" select="count($searchResultTitles[@name='task.definitionLabel' and @source='property']/preceding-sibling::*) + 1"/>
  <xsl:variable name="taskPriorityPos" select="count($searchResultTitles[@name='task.priority' and @source='property']/preceding-sibling::*) + 1"/>
  <xsl:variable name="taskActorPos" select="count($searchResultTitles[@name='task.actor' and @source='property']/preceding-sibling::*) + 1"/>
  <xsl:variable name="taskDueDatePos" select="count($searchResultTitles[@name='task.dueDate' and @source='property']/preceding-sibling::*) + 1"/>
  <xsl:variable name="taskIsOpenPos" select="count($searchResultTitles[@name='task.isOpen' and @source='property']/preceding-sibling::*) + 1"/>
  <xsl:variable name="taskHasPoolsPos" select="count($searchResultTitles[@name='task.hasPools' and @source='property']/preceding-sibling::*) + 1"/>
  <xsl:variable name="taskEndPos" select="count($searchResultTitles[@name='task.end' and @source='property']/preceding-sibling::*) + 1"/>

  <xsl:variable name="processIdPos" select="count($searchResultTitles[@name='process.id' and @source='property']/preceding-sibling::*) + 1"/>
  <xsl:variable name="processStartPos" select="count($searchResultTitles[@name='process.start' and @source='property']/preceding-sibling::*) + 1"/>
  <xsl:variable name="processEndPos" select="count($searchResultTitles[@name='process.end' and @source='property']/preceding-sibling::*) + 1"/>
  <xsl:variable name="processOwnerPos" select="count($searchResultTitles[@name='daisy_owner' and @source='process_variable']/preceding-sibling::*) + 1"/>
  <xsl:variable name="processDefLabelPos" select="count($searchResultTitles[@name='process.definitionLabel' and @source='property']/preceding-sibling::*) + 1"/>
  <xsl:variable name="processSuspendedPos" select="count($searchResultTitles[@name='process.suspended' and @source='property']/preceding-sibling::*) + 1"/>

  <xsl:variable name="timerIdPos" select="count($searchResultTitles[@name='timer.id' and @source='property']/preceding-sibling::*) + 1"/>
  <xsl:variable name="timerNamePos" select="count($searchResultTitles[@name='timer.name' and @source='property']/preceding-sibling::*) + 1"/>
  <xsl:variable name="timerDueDatePos" select="count($searchResultTitles[@name='timer.dueDate' and @source='property']/preceding-sibling::*) + 1"/>
  <xsl:variable name="timerRecurrencePos" select="count($searchResultTitles[@name='timer.recurrence' and @source='property']/preceding-sibling::*) + 1"/>
  <xsl:variable name="timerSuspendedPos" select="count($searchResultTitles[@name='timer.suspended' and @source='property']/preceding-sibling::*) + 1"/>
  <xsl:variable name="timerFailedPos" select="count($searchResultTitles[@name='timer.failed' and @source='property']/preceding-sibling::*) + 1"/>

  <xsl:template match="page">
    <div id="{$resultId}.root" parseWidgets="false">
      <xsl:call-template name="searchResultRootAttrs"/>
      <xsl:call-template name="content"/>
    </div>
  </xsl:template>

  <xsl:template match="wf:value">
    <xsl:choose>
      <xsl:when test="wf:label">
        <xsl:copy-of select="wf:label/node()"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy-of select="wf:raw/node()"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="resultNavigation">
    <xsl:variable name="resultInfo" select="wf:resultInfo"/>
    <xsl:value-of select="concat($resultInfo/@chunkOffset, ' - ', $resultInfo/@chunkOffset + $resultInfo/@chunkLength - 1)"/>
    <xsl:text> of </xsl:text>
    <xsl:value-of select="$resultInfo/@size"/>
    <xsl:text> </xsl:text>

    <a id="{$resultId}.toFirst"><i18n:text key="wfsearch.nav.first"/></a>
    | &lt;
    <a id="{$resultId}.toPrev"><i18n:text key="wfsearch.nav.prev"/></a>
    |
    <a id="{$resultId}.toNext"><i18n:text key="wfsearch.nav.next"/></a>
    &gt; |
    <a id="{$resultId}.toLast"><i18n:text key="wfsearch.nav.last"/></a>
  </xsl:template>

  <xsl:template name="searchResultRootAttrs">
    <xsl:variable name="sortOrder" select="/page/sortOrder"/>
    <xsl:variable name="resultInfo" select="wf:searchResult/wf:resultInfo"/>

    <xsl:attribute name="chunkOffset"><xsl:value-of select="$resultInfo/@chunkOffset"/></xsl:attribute>
    <xsl:attribute name="resultSize"><xsl:value-of select="$resultInfo/@size"/></xsl:attribute>
    <xsl:attribute name="orderBy"><xsl:value-of select="$sortOrder/@name"/></xsl:attribute>
    <xsl:attribute name="orderBySource"><xsl:value-of select="$sortOrder/@source"/></xsl:attribute>
    <xsl:attribute name="orderByDirection"><xsl:value-of select="$sortOrder/@direction"/></xsl:attribute>
  </xsl:template>

  <xsl:template name="header">
    <xsl:param name="resultPos"/>
    <xsl:param name="seq"/>
    <xsl:param name="customTitle"/>

    <xsl:variable name="title" select="$searchResultTitles[$resultPos]"/>
    <xsl:variable name="sortOrder" select="/page/sortOrder"/>


    <xsl:choose>
      <xsl:when test="$customTitle">
        <xsl:copy-of select="$customTitle"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy-of select="$title/node()"/>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:choose>
      <xsl:when test="$title/@name = $sortOrder/@name and $title/@source = $sortOrder/@source">
        <xsl:choose>
          <xsl:when test="$sortOrder/@direction = 'ascending'">
            <img id="{$resultId}.{$seq}.sort" sortColumn="{$title/@name}" sortSource="{$title/@source}" sortOrder="descending"
                 src="{$mountPoint}/resources/skins/{$skin}/images/sort_asc_active.gif" alt="[^]"
                 style="cursor: pointer"/>
          </xsl:when>
          <xsl:otherwise>
            <img id="{$resultId}.{$seq}.sort" sortColumn="{$title/@name}" sortSource="{$title/@source}" sortOrder="ascending"
                 src="{$mountPoint}/resources/skins/{$skin}/images/sort_desc_active.gif" alt="[v]"
                 style="cursor: pointer"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <img id="{$resultId}.{$seq}.sort" sortColumn="{$title/@name}" sortSource="{$title/@source}" sortOrder="ascending"
             src="{$mountPoint}/resources/skins/{$skin}/images/sort_asc.gif" alt="[v]"
             style="cursor: pointer"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
