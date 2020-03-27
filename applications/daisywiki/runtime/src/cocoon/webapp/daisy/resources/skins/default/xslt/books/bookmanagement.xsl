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

  <xsl:variable name="user" select="/page/context/user"/>
  <xsl:variable name="onlyGuestRole" select="boolean($user/activeRoles/role[@name='guest']) and count($user/activeRoles/role) = 1"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><i18n:text key="bookmgmt.title"/></pageTitle>
      <content><xsl:call-template name="content"/></content>
      <layoutHints wideLayout="true"/>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1><i18n:text key="bookmgmt.title"/></h1>
    <a href="../books"><i18n:text key="bookmgmt.published-books"/></a>
    <xsl:choose>
      <xsl:when test="group/*">
        <table class="books" width="100%">
          <xsl:apply-templates select="group/*">
            <xsl:with-param name="nesting" select="1"/>
          </xsl:apply-templates>
        </table>
      </xsl:when>
      <xsl:otherwise>
        <p><i18n:text key="bookmgmt.no-bookdefs"/></p>        
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="group">
    <xsl:param name="nesting"/>
    <tr class="bookgroup">
      <td style="padding-left: {($nesting * 1) - 1}em;">
        <b><xsl:value-of select="@name"/></b>
      </td>
      <td/>
    </tr>
    <xsl:apply-templates select="group">
      <xsl:with-param name="nesting" select="$nesting + 1"/>
    </xsl:apply-templates>
    <xsl:apply-templates select="book">
      <xsl:with-param name="nesting" select="$nesting + 1"/>
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="book">
    <xsl:param name="nesting"/>
    <tr>
      <xsl:if test="(position() mod 2) = 0">
        <xsl:attribute name="class">bookAltRow</xsl:attribute>
      </xsl:if>
      <td style="padding-left: {($nesting * 1) - 1}em;">
        <xsl:value-of select="concat(@name, ' (', @branch, ' - ', @language, ')')"/>
      </td>
      <td>
        <xsl:if test="not($onlyGuestRole)">
          <a href="createBookInstance?bookDefinitionDocumentId={@documentId}&amp;bookDefinitionBranchId={@branchId}&amp;bookDefinitionLanguageId={@languageId}"><i18n:text key="bookmgmt.publish"/></a>
        </xsl:if>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>