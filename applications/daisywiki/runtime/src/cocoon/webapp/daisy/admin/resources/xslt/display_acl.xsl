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
  xmlns:d="http://outerx.org/daisy/1.0">

  <xsl:param name="daisy-resources-uri"/>
  <xsl:variable name="mountPoint" select="page/context/mountPoint"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><xsl:call-template name="pageTitle"/></pageTitle>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="pageTitle">
    <xsl:choose>
      <xsl:when test="d:acl/@id = '1'">Daisy: Live ACL</xsl:when>
      <xsl:when test="d:acl/@id = '2'">Daisy: Staging ACL</xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="content">
    <h1><xsl:call-template name="pageTitle"/></h1>

    <xsl:apply-templates select="d:acl"/>
  </xsl:template>

  <xsl:template match="d:acl">
    <table class="acl">
      <tr>
        <th rowspan="2" colspan="3">Object</th>
        <th colspan="2">Subject</th>
        <th colspan="5">Permissions</th>
      </tr>
      <tr>
        <th>type</th>
        <th>value</th>
        <th>Read</th>
        <th>Write</th>
        <th>Delete</th>
        <th colspan="2">Publish</th>
      </tr>
      <tr class="aclsplitrow">
        <td colspan="11"/>
      </tr>
      <xsl:apply-templates select="d:aclObject"/>
    </table>
  </xsl:template>

  <xsl:template match="d:aclObject">
    <xsl:variable name="borderRowspan">
      <xsl:choose>
        <xsl:when test="d:aclEntry">
          <xsl:value-of select="count(d:aclEntry) + 3"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="5"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <tr class="objecttop">
      <td rowspan="{$borderRowspan}" class="objectleft"/>
      <td colspan="8"/>
      <td rowspan="{$borderRowspan}" class="objectright"/>
    </tr>
    <tr>
      <td><strong>If</strong></td>
      <td colspan="6"><xsl:value-of select="@expression"/></td>
    </tr>
    <tr>
      <td/>
      <td/>
      <td colspan="6"><strong>Then</strong></td>
    </tr>
    <xsl:choose>
      <xsl:when test="d:aclEntry">
        <xsl:apply-templates select="d:aclEntry"/>
      </xsl:when>
      <xsl:otherwise>
        <tr>
          <td/>
          <td/>
          <td colspan="7">
            <em>No entries yet.</em>
          </td>
        </tr>
      </xsl:otherwise>
    </xsl:choose>
    <tr>
      <td class="objectbottom" colspan="10"/>
    </tr>
    <xsl:if test="position() != last()">
      <tr class="aclsplitrow">
        <td colspan="10"/>
      </tr>
    </xsl:if>
  </xsl:template>

  <xsl:template match="d:aclEntry">
    <xsl:variable name="permissions" select="d:permissions/d:permission"/>
    <tr>
      <td></td>
      <td/>
      <td><xsl:value-of select="@subjectType"/></td>
      <td><xsl:value-of select="@subjectValueLabel"/> (<xsl:value-of select="@subjectValue"/>)</td>
      <td>
        <xsl:call-template name="displayPermission">
          <xsl:with-param name="permission" select="$permissions[@type='read']/@action"/>
        </xsl:call-template>
        <xsl:if test="$permissions[@type='read']/d:accessDetails">
          <img src="{$daisy-resources-uri}/skins/default/images/acl_accessdetails.png"/>
        </xsl:if>
      </td>
      <td>
        <xsl:call-template name="displayPermission">
          <xsl:with-param name="permission" select="$permissions[@type='write']/@action"/>
        </xsl:call-template>
        <xsl:if test="$permissions[@type='write']/d:accessDetails">
          <img src="{$daisy-resources-uri}/skins/default/images/acl_accessdetails.png"/>
        </xsl:if>
      </td>
      <td>
        <xsl:call-template name="displayPermission">
          <xsl:with-param name="permission" select="$permissions[@type='delete']/@action"/>
        </xsl:call-template>
      </td>
      <td>
        <xsl:call-template name="displayPermission">
          <xsl:with-param name="permission" select="$permissions[@type='publish']/@action"/>
        </xsl:call-template>
      </td>
    </tr>
  </xsl:template>

  <xsl:template name="displayPermission">
    <xsl:param name="permission"/>

    <img src="{$daisy-resources-uri}/skins/default/images/acl_{$permission}.gif"/>

  </xsl:template>

</xsl:stylesheet>