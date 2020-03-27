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

  <xsl:variable name="returnTo" select="urlencoder:encode(/page/context/request/@uri, 'UTF-8')"/>
  <xsl:variable name="title">Workflow Process</xsl:variable>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><xsl:value-of select="$title"/></pageTitle>
      <layoutHints wideLayout="true"/>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1><xsl:value-of select="$title"/></h1>

    <xsl:apply-templates select="wf:process"/>
    <xsl:apply-templates select="wf:timers"/>
  </xsl:template>

  <xsl:template match="wf:process">
    Actions:
    <xsl:variable name="processId" select="string(@id)"/>
    <xsl:variable name="processSuspended" select="boolean(string(@suspended) = 'true')"/>
    <xsl:choose>
      <xsl:when test="$processSuspended">
        <xsl:call-template name="generatePostLink">
          <xsl:with-param name="action" select="concat($processId, '?action=resume&amp;returnTo=', $returnTo, '&amp;returnToLabel=Back to process page')"/>
          <xsl:with-param name="label">Resume</xsl:with-param>
          <xsl:with-param name="id">resume-process</xsl:with-param>
        </xsl:call-template>
        |
      </xsl:when>
      <xsl:when test="not($processSuspended) and not(@end)">
        <xsl:call-template name="generatePostLink">
          <xsl:with-param name="action" select="concat($processId, '?action=suspend&amp;returnTo=', $returnTo, '&amp;returnToLabel=Back to process page')"/>
          <xsl:with-param name="label">Suspend</xsl:with-param>
          <xsl:with-param name="id">suspend-process</xsl:with-param>
        </xsl:call-template>
        |
      </xsl:when>
    </xsl:choose>
    <xsl:call-template name="generatePostLink">
      <xsl:with-param name="action" select="concat($processId, '?action=delete&amp;returnTo=', urlencoder:encode('../processSearch'), '&amp;returnToLabel=To process search page')"/>
      <xsl:with-param name="label">Delete</xsl:with-param>
      <xsl:with-param name="confirmMessage">Are you sure you want to delete this process?</xsl:with-param>
      <xsl:with-param name="id">delete-process</xsl:with-param>
    </xsl:call-template>


    <p><b>Below is detailed information on the workflow process. In its current form it is only intended for technical users.</b></p>

    <h2>General info</h2>
    <table class="default">
      <tr>
        <td>ID</td>
        <td><xsl:value-of select="@id"/></td>
      </tr>
      <tr>
        <td>Start time</td>
        <td><xsl:value-of select="@startFormatted"/></td>
      </tr>
      <tr>
        <td>End time</td>
        <td><xsl:value-of select="@endFormatted"/></td>
      </tr>
      <tr>
        <td>Suspended</td>
        <td><xsl:value-of select="@suspended"/></td>
      </tr>
      <tr>
        <td>Definition ID</td>
        <td><xsl:value-of select="@definitionId"/></td>
      </tr>
    </table>

    <xsl:apply-templates select="wf:tasks"/>
    <xsl:apply-templates select="wf:executionPath"/>
  </xsl:template>

  <xsl:template match="wf:tasks">
    <h2>Tasks</h2>
    <xsl:apply-templates select="wf:task"/>
  </xsl:template>

  <xsl:template match="wf:task">
    <h3>Task "<xsl:copy-of select="wf:taskDefinition/wf:label/node()"/>"</h3>

    <table class="default">
      <tr>
        <td>ID</td>
        <td><xsl:value-of select="@id"/></td>
      </tr>
      <tr>
        <td>Creation time</td>
        <td><xsl:value-of select="@createdFormatted"/></td>
      </tr>
      <tr>
        <td>End time</td>
        <td><xsl:value-of select="@endFormatted"/></td>
      </tr>
      <tr>
        <td>Due date</td>
        <td><xsl:value-of select="@dueDateFormatted"/></td>
      </tr>
      <tr>
        <td>Priority</td>
        <td><xsl:value-of select="@priority"/></td>
      </tr>
      <tr>
        <td>Actor ID</td>
        <td><xsl:value-of select="@actorId"/></td>
      </tr>
      <tr>
        <td>Execution path</td>
        <td><xsl:value-of select="@executionPath"/></td>
      </tr>
    </table>

    <xsl:apply-templates select="wf:variables"/>

  </xsl:template>

  <xsl:template match="wf:variables">
    <p>Variables in this task:</p>
    <table class="default">
      <tr>
        <th>Name</th>
        <th>Scope</th>
        <th>Value</th>
      </tr>
      <xsl:for-each select="wf:variable">
        <tr>
          <td><xsl:value-of select="@name"/></td>
          <td><xsl:value-of select="@scope"/></td>
          <td>
            <xsl:apply-templates select="wf:*"/>
          </td>
        </tr>
      </xsl:for-each>
    </table>
  </xsl:template>

  <xsl:template match="wf:string|wf:date|wf:dateTime|wf:long|wf:user|wf:boolean">
    <xsl:value-of select="."/>
  </xsl:template>

  <xsl:template match="wf:daisyLink">
    daisy:<xsl:value-of select="@documentId"/>@<xsl:value-of select="@branchId"/>:<xsl:value-of select="@languageId"/>:<xsl:value-of select="@version"/>
  </xsl:template>

  <xsl:template match="wf:actor">
    <xsl:choose>
      <xsl:when test="@pool = 'true'">
        Pool:
        <xsl:for-each select="id">
          <xsl:if test="position() > 1">, </xsl:if>
          <xsl:value-of select="."/>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
        User: <xsl:value-of select="@id"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="wf:executionPath">
    <h2>Execution paths</h2>
    <table class="default">
      <tr>
        <td>Path</td>
        <td><xsl:value-of select="@path"/></td>
      </tr>
      <tr>
        <td>Start time</td>
        <td><xsl:value-of select="@start"/></td>
      </tr>
      <tr>
        <td>End time</td>
        <td><xsl:value-of select="@end"/></td>
      </tr>
      <tr>
        <td>Located at node</td>
        <td>
          <xsl:value-of select="wf:nodeDefinition/@fullyQualifiedName"/>
          (available transitions:
          <xsl:for-each select="wf:nodeDefinition/wf:leavingTransitions/wf:transitionDefinition">
            <xsl:if test="position() > 1">, </xsl:if>
            <xsl:copy-of select="wf:label/node()"/>
          </xsl:for-each>
          )
        </td>
      </tr>
      <tr>
        <td>Child execution paths</td>
        <td>
          <xsl:choose>
            <xsl:when test="wf:children/wf:executionPath">
              <xsl:apply-templates select="wf:children/wf:executionPath"/>
            </xsl:when>
            <xsl:otherwise>
              (none)
            </xsl:otherwise>
          </xsl:choose>
        </td>
      </tr>
    </table>
  </xsl:template>

  <xsl:template match="wf:timers">
    <h2>Timers</h2>
    <xsl:choose>
      <xsl:when test="wf:timer">
        <table class="default">
          <tr>
            <th>ID</th>
            <th>Name</th>
          </tr>
          <xsl:apply-templates select="wf:timer"/>
        </table>
      </xsl:when>
      <xsl:otherwise>
        There are no timers in this process.
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="wf:timer">
    <tr>
      <td>
        <a href="../timer/{@id}"><xsl:value-of select="@id"/></a>
      </td>
      <td><xsl:value-of select="@name"/></td>
    </tr>
  </xsl:template>

</xsl:stylesheet>