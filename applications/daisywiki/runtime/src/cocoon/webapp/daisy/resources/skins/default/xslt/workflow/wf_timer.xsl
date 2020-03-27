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

  <xsl:variable name="title"><i18n:text key="wftimer.title"/></xsl:variable>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><xsl:copy-of select="$title"/></pageTitle>
      <layoutHints wideLayout="true"/>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1><xsl:copy-of select="$title"/></h1>

    <xsl:apply-templates select="wf:timer"/>
  </xsl:template>

  <xsl:template match="wf:timer">
    <table class="default">
      <tr>
        <td><i18n:text key="wftimer.id"/></td>
        <td><xsl:value-of select="@id"/></td>
      </tr>
      <tr>
        <td><i18n:text key="wftimer.due-date"/></td>
        <td><xsl:value-of select="@dueDateFormatted"/></td>
      </tr>
      <tr>
        <td><i18n:text key="wftimer.recurrence"/></td>
        <td><xsl:value-of select="@recurrence"/></td>
      </tr>
      <tr>
        <td><i18n:text key="wftimer.suspended"/></td>
        <td><xsl:value-of select="@suspended"/></td>
      </tr>
      <tr>
        <td><i18n:text key="wftimer.owner-process-id"/></td>
        <td>
          <a href="../process/{@processId}">
            <xsl:value-of select="@processId"/>
          </a>
        </td>
      </tr>
      <tr>
        <td><i18n:text key="wftimer.execution-path"/></td>
        <td><xsl:value-of select="@executionPath"/></td>
      </tr>
      <tr>
        <td><i18n:text key="wftimer.transition"/></td>
        <td><xsl:value-of select="@transitionName"/></td>
      </tr>
      <tr>
        <td><i18n:text key="wftimer.exception"/></td>
        <td>
          <xsl:if test="wf:exception">
            <pre style="overflow: auto; width: 40em;">
              <xsl:copy-of select="wf:exception/node()"/>
            </pre>
          </xsl:if>
        </td>
      </tr>
    </table>

  </xsl:template>

</xsl:stylesheet>