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
  xmlns:d="http://outerx.org/daisy/1.0">

  <xsl:import href="list_sorting.xsl"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle>Field Types</pageTitle>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1>Field Types</h1>
    <ul>
      <li><a href="fieldType/new">Create a new Field Type</a></li>
    </ul>

    <p><strong>Note:</strong> field types can only be deleted when they are not associated any more with any
    document type or not in use in any version of any document.</p>

    <p><strong>Note:</strong> it is strongly recommended not to modify the default field types provided with Daisy.</p>

    <xsl:apply-templates select="d:fieldTypes"/>

    <script type="text/javascript">
      function deleteFieldType(id, name) {
        var confirmed = confirm("Are you sure you want to delete the field type \"" + name + "\"?");
        if (confirmed) {
          var form = document.forms.actionForm;
          form.id.value = id;
          form.submit();
        }
      }
    </script>
    <div style="display: none">
      <form name="actionForm" method="POST" action="fieldType">
        <input type="hidden" name="action" value="delete"/>
        <input type="hidden" name="id"/>
      </form>
    </div>
  </xsl:template>

  <xsl:template match="d:fieldTypes">
    <table class="default">
      <tr>
        <th>
          ID
          <xsl:call-template name="sortIndication">
            <xsl:with-param name="name">id</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          Name
          <xsl:call-template name="sortIndication">
            <xsl:with-param name="name">name</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          Value Type
          <xsl:call-template name="sortIndication">
            <xsl:with-param name="name">valueType</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          Multi<br/>value
          <xsl:call-template name="sortIndication">
            <xsl:with-param name="name">multiValue</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          Hierar-<br/>chical
          <xsl:call-template name="sortIndication">
            <xsl:with-param name="name">hierarchical</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          Select<br/>list
          <xsl:call-template name="sortIndication">
            <xsl:with-param name="name">selectionList</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          Depre-<br/>cated
          <xsl:call-template name="sortIndication">
            <xsl:with-param name="name">deprecated</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>Actions</th>
      </tr>
      <xsl:choose>
        <xsl:when test="d:fieldType">
          <xsl:call-template name="insertSortedFieldTypes"/>
        </xsl:when>
        <xsl:otherwise>
          <tr>
            <td colspan="4"><em>No field types yet.</em></td>
          </tr>
        </xsl:otherwise>
      </xsl:choose>
    </table>
  </xsl:template>

  <xsl:template match="d:fieldType">
    <tr>
      <td><xsl:value-of select="@id"/></td>
      <td>
        <xsl:if test="@deprecated = 'true'">
          <xsl:attribute name="class">deprecated</xsl:attribute>
        </xsl:if>
        <xsl:value-of select="@name"/>
      </td>
      <td><xsl:value-of select="@valueType"/></td>
      <td><xsl:value-of select="@multiValue"/></td>
      <td><xsl:value-of select="@hierarchical"/></td>
      <td><xsl:value-of select="boolean(d:selectionList)"/></td>
      <td><xsl:value-of select="@deprecated"/></td>
      <td>
        <a href="fieldType/{@id}/edit">view/edit</a> |
        <a href="#" onClick="deleteFieldType('{@id}', '{@name}'); return false;">delete</a>
      </td>
    </tr>
  </xsl:template>

  <xsl:template name="insertSortedFieldTypes">
    <!-- asc/desc for boolean values is reversed on purpose so that true would put before false -->
    <xsl:choose>
      <xsl:when test="$sortKey = 'id' and $sortOrder = 'asc'">
        <xsl:apply-templates select="d:fieldType">
          <xsl:sort select="@id" order="ascending" data-type="number"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'id' and $sortOrder = 'desc'">
        <xsl:apply-templates select="d:fieldType">
          <xsl:sort select="@id" order="descending" data-type="number"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'name' and $sortOrder = 'asc'">
        <xsl:apply-templates select="d:fieldType">
          <xsl:sort select="@name" order="ascending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'name' and $sortOrder = 'desc'">
        <xsl:apply-templates select="d:fieldType">
          <xsl:sort select="@name" order="descending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'valueType' and $sortOrder = 'asc'">
        <xsl:apply-templates select="d:fieldType">
          <xsl:sort select="@valueType" order="ascending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'valueType' and $sortOrder = 'desc'">
        <xsl:apply-templates select="d:fieldType">
          <xsl:sort select="@valueType" order="descending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'multiValue' and $sortOrder = 'asc'">
        <xsl:apply-templates select="d:fieldType">
          <xsl:sort select="@multiValue" order="descending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'multiValue' and $sortOrder = 'desc'">
        <xsl:apply-templates select="d:fieldType">
          <xsl:sort select="@multiValue" order="ascending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'hierarchical' and $sortOrder = 'asc'">
        <xsl:apply-templates select="d:fieldType">
          <xsl:sort select="@hierarchical" order="descending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'hierarchical' and $sortOrder = 'desc'">
        <xsl:apply-templates select="d:fieldType">
          <xsl:sort select="@hierarchical" order="ascending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'selectionList' and $sortOrder = 'asc'">
        <xsl:apply-templates select="d:fieldType">
          <xsl:sort select="boolean(d:selectionList)" order="descending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'selectionList' and $sortOrder = 'desc'">
        <xsl:apply-templates select="d:fieldType">
          <xsl:sort select="boolean(d:selectionList)" order="ascending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'deprecated' and $sortOrder = 'asc'">
        <xsl:apply-templates select="d:fieldType">
          <xsl:sort select="@deprecated" order="descending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'deprecated' and $sortOrder = 'desc'">
        <xsl:apply-templates select="d:fieldType">
          <xsl:sort select="@deprecated" order="ascending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise>
        <!-- catch case of invalid sortKey or sortOrder  -->
        <xsl:apply-templates select="d:fieldType"/>        
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>