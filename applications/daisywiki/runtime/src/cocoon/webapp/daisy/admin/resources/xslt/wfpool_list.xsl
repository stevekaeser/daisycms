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
  xmlns:daisyutil="xalan://org.outerj.daisy.frontend.util.XslUtil">

  <xsl:import href="list_sorting.xsl"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle>Daisy: Workflow Pools Administration</pageTitle>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1>Workflow Pools Administration</h1>
    <ul>
      <li><a href="wfPool/new">Create a new pool</a></li>
    </ul>

    <xsl:choose>
      <xsl:when test="wf:pools/wf:pool">
        <xsl:apply-templates select="wf:pools"/>
      </xsl:when>
      <xsl:otherwise>
        <p>There are currently no workflow pools defined.</p>
      </xsl:otherwise>
    </xsl:choose>

    <script type="text/javascript">
      function deletePool(id, name) {
        var confirmed = confirm("Are you sure you want to delete the workflow pool named \"" + name + "\"?");
        if (confirmed) {
          var form = document.forms.actionForm;
          form.id.value = id;
          form.submit();
        }
      }
    </script>
    <div style="display: none">
      <form name="actionForm" method="POST" action="wfPool">
        <input type="hidden" name="action" value="delete"/>
        <input type="hidden" name="id"/>
      </form>
    </div>
  </xsl:template>

  <xsl:template match="wf:pools">
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
          Description
          <xsl:call-template name="sortIndication">
            <xsl:with-param name="name">description</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>Actions</th>
      </tr>
      <xsl:call-template name="insertSortedPools"/>
    </table>
  </xsl:template>

  <xsl:template name="insertSortedPools">
    <xsl:choose>
      <xsl:when test="$sortKey = 'id' and $sortOrder = 'asc'">
        <xsl:apply-templates select="wf:pool">
          <xsl:sort select="@id" order="ascending" data-type="number"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'id' and $sortOrder = 'desc'">
        <xsl:apply-templates select="wf:pool">
          <xsl:sort select="@id" order="descending" data-type="number"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'name' and $sortOrder = 'asc'">
        <xsl:apply-templates select="wf:pool">
          <xsl:sort select="@name" order="ascending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'name' and $sortOrder = 'desc'">
        <xsl:apply-templates select="wf:pool">
          <xsl:sort select="@name" order="descending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'description' and $sortOrder = 'asc'">
        <xsl:apply-templates select="wf:pool">
          <xsl:sort select="@name" order="ascending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'description' and $sortOrder = 'desc'">
        <xsl:apply-templates select="wf:pool">
          <xsl:sort select="@name" order="descending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise>
        <!-- catch case of invalid sortKey or sortOrder  -->
        <xsl:apply-templates select="wf:pool"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="wf:pool">
    <tr>
      <td><xsl:value-of select="@id"/></td>
      <td><xsl:value-of select="@name"/></td>
      <td><xsl:value-of select="@description"/></td>
      <td>
        <a href="wfPool/{@id}/edit">view/edit</a> |
        <a href="wfPool/{@id}/members">edit members</a> |
        <a href="#" onClick="deletePool('{@id}', '{@name}'); return false;">delete</a>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>