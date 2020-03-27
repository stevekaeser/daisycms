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

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle>ACL Evaluation Result</pageTitle>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <xsl:apply-templates select="d:aclResult"/>
  </xsl:template>

  <xsl:template match="d:aclResult">
    <h1>ACL Evaluation Result</h1>

    Result of ACL evaluation for:
    <ul>
      <li>User: <xsl:value-of select="d:user/@id"/></li>
      <li>Role:
        <xsl:for-each select="d:user/d:roles/d:roleId">
          <xsl:if test="position() > 1">, </xsl:if>
          <xsl:value-of select="."/>
        </xsl:for-each>
      </li>
      <li>Document id: <xsl:value-of select="@documentId"/></li>
      <li>Branch id: <xsl:value-of select="@branchId"/></li>
      <li>Language id: <xsl:value-of select="@languageId"/></li>
    </ul>

    <table class="default">
      <tr>
        <th>Permission</th>
        <th>Action</th>
        <th>Object reason or matching expression</th>
        <th>Subject reason</th>
      </tr>
      <xsl:apply-templates select="d:permissions/d:permission"/>
    </table>
  </xsl:template>

  <xsl:template match="d:permission">
    <tr>
      <td><xsl:value-of select="@type"/></td>
      <td>
        <xsl:value-of select="@action"/>
        <xsl:variable name="details" select="d:accessDetails"/>
        <xsl:if test="$details">
          <xsl:if test="$details/d:permission[@type='non_live']/@action = 'deny'"><br/>Only live version</xsl:if>
          <xsl:if test="$details/d:permission[@type = 'all_fields']/@action = 'deny'">
            <br/>
            <xsl:choose>
              <xsl:when test="$details/d:allowFieldAccess">
                Only these fields:
                <xsl:for-each select="$details/d:allowFieldAccess">
                  <xsl:if test="position() > 1">, </xsl:if>
                  <xsl:value-of select="@name"/>
                </xsl:for-each>
              </xsl:when>
              <xsl:otherwise>
                No fields.
              </xsl:otherwise>
            </xsl:choose>
          </xsl:if>
          <xsl:if test="$details/d:permission[@type = 'all_parts']/@action = 'deny'">
            <br/>
            <xsl:choose>
              <xsl:when test="$details/d:allowPartAccess">
                Only these parts:
                <xsl:for-each select="$details/d:allowPartAccess">
                  <xsl:if test="position() > 1">, </xsl:if>
                  <xsl:value-of select="@name"/>
                </xsl:for-each>
              </xsl:when>
              <xsl:otherwise>
                No parts.
              </xsl:otherwise>
            </xsl:choose>
          </xsl:if>
          <xsl:if test="$details/d:permission[@type = 'fulltext']/@action = 'deny'"><br/>No full text index.</xsl:if>
          <xsl:if test="$details/d:permission[@type = 'summary']/@action = 'deny'"><br/>No document summary.</xsl:if>
          <xsl:if test="$details/d:permission[@type = 'doc_name']/@action = 'deny'"><br/>No document name.</xsl:if>
          <xsl:if test="$details/d:permission[@type = 'links']/@action = 'deny'"><br/>No out-of-line links.</xsl:if>
          <xsl:if test="$details/d:permission[@type = 'custom_fields']/@action = 'deny'"><br/>No custom fields.</xsl:if>
          <xsl:if test="$details/d:permission[@type = 'collections']/@action = 'deny'"><br/>No collections.</xsl:if>
          <xsl:if test="$details/d:permission[@type = 'doctype']/@action = 'deny'"><br/>No document type.</xsl:if>
          <xsl:if test="$details/d:permission[@type = 'retired']/@action = 'deny'"><br/>No retired flag.</xsl:if>
          <xsl:if test="$details/d:permission[@type = 'private']/@action = 'deny'"><br/>No private flag.</xsl:if>
          <xsl:if test="$details/d:permission[@type = 'reflang']/@action = 'deny'"><br/>No reference language.</xsl:if>
          <xsl:if test="$details/d:permission[@type = 'version_meta']/@action = 'deny'"><br/>No version metadata.</xsl:if>
        </xsl:if>
      </td>
      <td><xsl:value-of select="@objectReason"/></td>
      <td><xsl:value-of select="@subjectReason"/></td>
    </tr>
  </xsl:template>

</xsl:stylesheet>