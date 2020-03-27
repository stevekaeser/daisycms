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
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:variable name="userId" select="string(/page/context/user/id)"/>
  <xsl:variable name="isAdmin" select="boolean(/page/context/user/availableRoles/role[@id='1'])"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><i18n:text key="publishtasks.title"/></pageTitle>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1><i18n:text key="publishtasks.title"/></h1>
    <xsl:choose>
      <xsl:when test="publishTasks/*">
        <table class="default">
          <tr>
            <th><i18n:text key="publishtasks.book-instance"/></th>
            <th><i18n:text key="publishtasks.started-by-user"/></th>
            <th><i18n:text key="publishtasks.started-on"/></th>
            <th><i18n:text key="publishtasks.state"/></th>
          </tr>
          <xsl:for-each select="publishTasks/publishTask">
            <tr>
              <td>
                <xsl:choose>
                  <xsl:when test="$userId = @startingUserId or $isAdmin">
                    <xsl:value-of select="@bookInstanceName"/>
                  </xsl:when>
                  <xsl:otherwise>
                    (<i18n:text key="publishtasks.hidden"/>)
                  </xsl:otherwise>
                </xsl:choose>
              </td>
              <td><xsl:value-of select="@startingUser"/></td>
              <td><xsl:value-of select="@started"/></td>
              <td>
                <xsl:choose>
                  <xsl:when test="$userId = @startingUserId or $isAdmin">
                    <i18n:translate>
                      <i18n:text key="{state[1]}"/>
                      <xsl:for-each select="state">
                        <i18n:param><xsl:value-of select="."/></i18n:param>
                      </xsl:for-each>
                    </i18n:translate>
                  </xsl:when>
                  <xsl:otherwise>
                    (<i18n:text key="publishtasks.hidden"/>)
                  </xsl:otherwise>
                </xsl:choose>
              </td>
            </tr>
          </xsl:for-each>
        </table>
      </xsl:when>
      <xsl:otherwise>
        <i18n:text key="publishtasks.no-tasks-running"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>