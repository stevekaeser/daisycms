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
      <pageTitle>Document Types</pageTitle>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1>Document Types</h1>
    <ul>
      <li><a href="documentType/new">Create a new Document Type</a></li>
    </ul>

    <p><strong>Note:</strong> document types can only be deleted when they are not in use by any document.</p>

    <p><strong>Note:</strong> it is strongly recommended not to modify the default document types provided with Daisy.</p>

    <p><strong>Warning:</strong> before you rename or delete a document type, make sure it is not in use
      in the ACL, otherwise execution of the ACL will fail and documents will not be accessible.</p>

    <xsl:apply-templates select="d:documentTypes"/>

    <script type="text/javascript">
      function deleteDocumentType(id, name) {
        var confirmed = confirm("Are you sure you want to delete the document type \"" + name + "\"?");
        if (confirmed) {
          var form = document.forms.actionForm;
          form.id.value = id;
          form.submit();
        }
      }
    </script>
    <div style="display: none">
      <form name="actionForm" method="POST" action="documentType">
        <input type="hidden" name="action" value="delete"/>
        <input type="hidden" name="id"/>
      </form>
    </div>
  </xsl:template>

  <xsl:template match="d:documentTypes">
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
          Deprecated
          <xsl:call-template name="sortIndication">
            <xsl:with-param name="name">deprecated</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>Actions</th>
      </tr>
      <xsl:call-template name="insertSortedDocumentTypes"/>
    </table>
  </xsl:template>

  <xsl:template match="d:documentType">
    <tr>
      <td><xsl:value-of select="@id"/></td>
      <td>
        <xsl:if test="@deprecated = 'true'">
          <xsl:attribute name="class">deprecated</xsl:attribute>
        </xsl:if>
        <xsl:value-of select="@name"/>
      </td>
      <td><xsl:value-of select="@deprecated"/></td>
      <td>
        <a href="documentType/{@id}/edit">view/edit</a> |
        <a href="#" onClick="deleteDocumentType('{@id}', '{@name}'); return false;">delete</a>
      </td>
    </tr>
  </xsl:template>

  <xsl:template name="insertSortedDocumentTypes">
    <!-- asc/desc for boolean values is reversed on purpose so that true would put before false -->
    <xsl:choose>
      <xsl:when test="$sortKey = 'id' and $sortOrder = 'asc'">
        <xsl:apply-templates select="d:documentType">
          <xsl:sort select="@id" order="ascending" data-type="number"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'id' and $sortOrder = 'desc'">
        <xsl:apply-templates select="d:documentType">
          <xsl:sort select="@id" order="descending" data-type="number"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'name' and $sortOrder = 'asc'">
        <xsl:apply-templates select="d:documentType">
          <xsl:sort select="@name" order="ascending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'name' and $sortOrder = 'desc'">
        <xsl:apply-templates select="d:documentType">
          <xsl:sort select="@name" order="descending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'deprecated' and $sortOrder = 'asc'">
        <xsl:apply-templates select="d:documentType">
          <xsl:sort select="@deprecated" order="descending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'deprecated' and $sortOrder = 'desc'">
        <xsl:apply-templates select="d:documentType">
          <xsl:sort select="@deprecated" order="ascending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise>
        <!-- catch case of invalid sortKey or sortOrder  -->
        <xsl:apply-templates select="d:documentType"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
