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
  xmlns:n="http://outerx.org/daisy/1.0#navigation"
  xmlns:common="http://exslt.org/common">

  <xsl:variable name="mountPoint" select="string(page/context/mountPoint)"/>
  <xsl:variable name="requestMethod" select="string(page/context/request/@method)"/>
  <xsl:variable name="pageURI" select="string(page/context/request/@uri)"/>

  <xsl:variable name="navigation">
    <n:navigationTree completeTree="true">
      <n:link label="Administration Home" url="{concat($mountPoint, '/admin/')}"/>
      <n:group label="Repository Schema">
        <n:link label="Document Types" url="{concat($mountPoint, '/admin/documentType')}"/>
        <n:link label="Part Types" url="{concat($mountPoint, '/admin/partType')}"/>
        <n:link label="Field Types" url="{concat($mountPoint, '/admin/fieldType')}"/>
      </n:group>
      <n:link label="Collections" url="{concat($mountPoint, '/admin/collection')}"/>
      <n:link label="Namespaces" url="{concat($mountPoint, '/admin/namespace')}"/>
      <n:group label="Variants">
        <n:link label="Branches" url="{concat($mountPoint, '/admin/branch')}"/>
        <n:link label="Languages" url="{concat($mountPoint, '/admin/language')}"/>
      </n:group>
      <n:group label="User Management">
        <n:link label="Users" url="{concat($mountPoint, '/admin/user')}"/>
        <n:link label="Roles" url="{concat($mountPoint, '/admin/role')}"/>
      </n:group>
      <n:group label="ACL">
        <n:group label="Staging">
          <n:link label="View" url="{concat($mountPoint, '/admin/acl/staging')}"/>
          <n:link label="Edit" url="{concat($mountPoint, '/admin/acl/staging/edit')}"/>
          <n:link label="Test" url="{concat($mountPoint, '/admin/testacl/staging')}"/>
          <n:link label="Put live" url="{concat($mountPoint, '/admin/acl/staging?action=putLive')}"/>
          <n:link label="Revert to live" url="{concat($mountPoint, '/admin/acl/staging?action=revertChanges')}"/>
        </n:group>
        <n:group label="Live">
          <n:link label="View" url="{concat($mountPoint, '/admin/acl/live')}"/>
          <n:link label="Test" url="{concat($mountPoint, '/admin/testacl/live')}"/>
        </n:group>
      </n:group>
      <n:group label="Workflow">
        <n:link label="Process definitions" url="{concat($mountPoint, '/admin/wfProcessDefinition')}"/>
        <n:link label="Pools" url="{concat($mountPoint, '/admin/wfPool')}"/>
      </n:group>
    </n:navigationTree>
  </xsl:variable>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="node()"/>
      <extraHeadContent>
        <link rel="stylesheet" type="text/css" href="{$mountPoint}/admin/resources/css/admin.css"/>
      </extraHeadContent>
      <!-- xsl:if test="$requestMethod = 'GET'">
        <pageNavigation>
          <link>
            <title>Hide navigation</title>
            <path>
              <xsl:choose>
                <xsl:when test="contains($pageURI, '?')">
                  <xsl:value-of select="concat($pageURI, '&amp;layoutType=plain')"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="concat($pageURI, '?layoutType=plain')"/>
                </xsl:otherwise>
              </xsl:choose>
            </path>
          </link>
        </pageNavigation>
      </xsl:if -->
      <xsl:apply-templates select="common:node-set($navigation)"/>
    </page>
  </xsl:template>

  <xsl:template match="n:link">
    <n:link>
      <xsl:copy-of select="@*"/>
      <xsl:if test="$pageURI = @url">
        <xsl:attribute name="active">true</xsl:attribute>
      </xsl:if>
    </n:link>
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>