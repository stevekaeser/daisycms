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
  xmlns:daisyutil="xalan://org.outerj.daisy.frontend.util.XslUtil">

  <xsl:import href="list_sorting.xsl"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle>Daisy: User Administration</pageTitle>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1>User Administration</h1>
    <ul>
      <li><a href="user/new">Create a new User</a></li>
    </ul>

    <p><b>WARNING:</b> the built-in users $system, internal, guest and registrar are required for the proper
    operation of the system and should not be removed. See the documentation for more information. Also,
    for these users it is highly advisable not to enter an e-mail address, since otherwise anyone will
    be able to reset the passwords of these users using the password reminder of the Daisy Wiki.</p>

    <xsl:apply-templates select="d:users"/>

    <script type="text/javascript">
      function deleteUser(id, name) {
        var confirmed = confirm("Are you sure you want to delete the user \"" + name + "\"?");
        if (confirmed) {
          var form = document.forms.actionForm;
          form.id.value = id;
          form.submit();
        }
      }
    </script>
    <div style="display: none">
      <form name="actionForm" method="POST" action="user">
        <input type="hidden" name="action" value="delete"/>
        <input type="hidden" name="id"/>
      </form>
    </div>
  </xsl:template>

  <xsl:template match="d:users">
    <table class="default">
      <tr>
        <th>
          ID
          <xsl:call-template name="sortIndication">
            <xsl:with-param name="name">id</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          Login
          <xsl:call-template name="sortIndication">
            <xsl:with-param name="name">login</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          First Name
          <xsl:call-template name="sortIndication">
            <xsl:with-param name="name">firstName</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>
          Last Name
          <xsl:call-template name="sortIndication">
            <xsl:with-param name="name">lastName</xsl:with-param>
          </xsl:call-template>
        </th>
        <th>Roles (default is emphasized)</th>
        <th>Actions</th>
      </tr>
      <xsl:call-template name="insertSortedUsers"/>
    </table>
  </xsl:template>

  <xsl:template match="d:user">
    <xsl:variable name="defaultRoleId" select="d:role/@id"/>
    <tr>
      <td><xsl:value-of select="@id"/></td>
      <td><xsl:value-of select="@login"/></td>
      <td><xsl:value-of select="@firstName"/></td>
      <td><xsl:value-of select="@lastName"/></td>
      <td>
        <xsl:for-each select="d:roles/d:role">
          <xsl:if test="position() != 1">, </xsl:if>
          <xsl:choose>
            <xsl:when test="$defaultRoleId = @id">
              <em><xsl:value-of select="@name"/></em>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="@name"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:for-each>
      </td>
      <td>
        <a href="user/{@id}/edit">view/edit</a> |
        <a href="#" onClick="deleteUser('{@id}', '{daisyutil:escape(concat(@firstName, ' ', @lastName, ' (', @login, ')'))}'); return false;">delete</a>
      </td>
    </tr>
  </xsl:template>

  <xsl:template name="insertSortedUsers">
    <!-- asc/desc for boolean values is reversed on purpose so that true would put before false -->
    <xsl:choose>
      <xsl:when test="$sortKey = 'id' and $sortOrder = 'asc'">
        <xsl:apply-templates select="d:user">
          <xsl:sort select="@id" order="ascending" data-type="number"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'id' and $sortOrder = 'desc'">
        <xsl:apply-templates select="d:user">
          <xsl:sort select="@id" order="descending" data-type="number"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'login' and $sortOrder = 'asc'">
        <xsl:apply-templates select="d:user">
          <xsl:sort select="@login" order="ascending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'login' and $sortOrder = 'desc'">
        <xsl:apply-templates select="d:user">
          <xsl:sort select="@login" order="descending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'firstName' and $sortOrder = 'asc'">
        <xsl:apply-templates select="d:user">
          <xsl:sort select="@firstName" order="ascending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'firstName' and $sortOrder = 'desc'">
        <xsl:apply-templates select="d:user">
          <xsl:sort select="@firstName" order="descending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'lastName' and $sortOrder = 'asc'">
        <xsl:apply-templates select="d:user">
          <xsl:sort select="@lastName" order="ascending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="$sortKey = 'lastName' and $sortOrder = 'desc'">
        <xsl:apply-templates select="d:user">
          <xsl:sort select="@lastName" order="descending"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise>
        <!-- catch case of invalid sortKey or sortOrder  -->
        <xsl:apply-templates select="d:user"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>