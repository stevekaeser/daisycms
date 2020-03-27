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
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:bm="http://outerx.org/daisy/1.0#bookstoremeta">

  <xsl:import href="daisyskin:xslt/util.xsl"/>

  <xsl:variable name="mountPoint" select="string(page/context/mountPoint)"/>
  <xsl:variable name="user" select="/page/context/user"/>
  <xsl:variable name="onlyGuestRole" select="boolean($user/activeRoles/role[@name='guest']) and count($user/activeRoles/role) = 1"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><i18n:text key="bookinstancemgmt.title"/></pageTitle>
      <layoutHints wideLayout="true"/>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1><i18n:text key="bookinstancemgmt.title"/></h1>

    <xsl:if test="not($onlyGuestRole)">
      <a href="definitions"><i18n:text key="bookinstacemgmt.publish-a-book"/></a>
      <br/>
      <a href="publishTasks"><i18n:text key="bookinstancemgmt.view-running-tasks"/></a>
    </xsl:if>

    <xsl:choose>
      <xsl:when test="group/*">
        <table width="100%" class="books">
          <xsl:apply-templates select="group/*">
            <xsl:with-param name="nesting" select="1"/>
          </xsl:apply-templates>
        </table>
      </xsl:when>
      <xsl:otherwise>
        <p><i18n:text key="bookinstancemgmt.no-books"/></p>
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
    <xsl:apply-templates select="bookInstance">
      <xsl:with-param name="nesting" select="$nesting + 1"/>
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="bookInstance">
    <xsl:param name="nesting"/>
    <xsl:variable name="bookInstanceName" select="string(@name)"/>

    <tr>
      <xsl:if test="(position() mod 2) = 0">
        <xsl:attribute name="class">bookAltRow</xsl:attribute>
      </xsl:if>
      <td style="padding-left: {($nesting * 1) - 1}em;">
        <xsl:value-of select="bm:bookInstanceMetaData/bm:label"/>
        <br/>
        <xsl:for-each select="bm:publicationsInfo/bm:publicationInfo">
          <xsl:if test="position() > 1"> | </xsl:if>
          <a href="{$mountPoint}/books/{$bookInstanceName}/publications/{@name}/{@startResource}"><xsl:value-of select="@label"/></a>
          <xsl:text>&#160;</xsl:text>
          <xsl:if test="@package">
            (<a href="{$mountPoint}/books/{$bookInstanceName}/publications/{@name}/{@package}">zip</a>)
          </xsl:if>
        </xsl:for-each>
      </td>
      <td valign="top">
        <xsl:value-of select="bm:bookInstanceMetaData/@createdOnFormatted"/><br/>
        <!-- Show creator only to users with management access on the book instance -->
        <xsl:if test="@canManage = 'true'">
          <xsl:value-of select="bm:bookInstanceMetaData/@createdByDisplayName"/>
        </xsl:if>
      </td>
      <td valign="top">
        <xsl:if test="@canManage = 'true'">
          <xsl:call-template name="generatePostLink">
            <xsl:with-param name="action" select="concat($mountPoint, '/books/', $bookInstanceName, '/acl')"/>
            <xsl:with-param name="label">[<i18n:text key="bookinstancemgmt.edit-acl"/>]</xsl:with-param>
            <xsl:with-param name="id" select="concat(generate-id(.), '_editacl')"/>
          </xsl:call-template>
          <xsl:call-template name="generatePostLink">
            <xsl:with-param name="action" select="concat($mountPoint, '/books/', $bookInstanceName, '?action=delete')"/>
            <xsl:with-param name="label">[<i18n:text key="bookinstancemgmt.delete"/>]</xsl:with-param>
            <xsl:with-param name="id" select="concat(generate-id(.), '_del')"/>
            <xsl:with-param name="confirmMessage">
              <i18n:translate>
                <i18n:text key="bookinstancemgmt.confirm-delete"/>
                <i18n:param><xsl:value-of select="$bookInstanceName"/></i18n:param>
              </i18n:translate>
            </xsl:with-param>
          </xsl:call-template>
          <a href="{$mountPoint}/books/{$bookInstanceName}/edit">[<i18n:text key="bookinstancemgmt.change-names"/>]</a>
          <a href="{$mountPoint}/books/{$bookInstanceName}/publications/link_errors.txt">[<i18n:text key="bookinstancemgmt.link-log"/>]</a>
          <a href="{$mountPoint}/books/{$bookInstanceName}/publications/log.txt">[<i18n:text key="bookinstancemgmt.publication-log"/>]</a>
        </xsl:if>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>