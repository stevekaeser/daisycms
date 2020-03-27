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
  <xsl:variable name="mountPoint" select="string(/page/context/mountPoint)"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle>Daisy: Members of pool "<xsl:value-of select="wf:pool/@name"/>"</pageTitle>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1>Members of pool "<xsl:value-of select="wf:pool/@name"/>"</h1>

    <xsl:variable name="memberCount" select="count(members/member)"/>

    <xsl:if test="$memberCount > 0">
      <p>This pool contains <b><xsl:value-of select="$memberCount"/></b> member(s).</p>
    </xsl:if>

    <xsl:call-template name="actions"/>
    <br/>
    <br/>

    <xsl:choose>
      <xsl:when test="$memberCount > 0">
        <xsl:apply-templates select="members"/>
        <br/>
        <br/>
        <xsl:call-template name="actions"/>
      </xsl:when>
      <xsl:otherwise>
        This pool does not contain any members.
      </xsl:otherwise>
    </xsl:choose>


    <script type="text/javascript">
      function addUser() {
        var popup = window.open("<xsl:value-of select="$mountPoint"/>/selectUser", "selectuser", "toolbar=no,menubar=no,personalbar=no,width=400,height=400,left=20,top=40,scrollbars=yes,resizable=yes");
        popup.onUserSelected = function(id, name) {
          var form = document.forms.actionForm;
          form.action.value = "add";
          form.userId.value = id;
          form.submit();
        }
      }

      function removeAllUsers() {
        var form = document.forms.actionForm;
        form.action.value = "clear";
        form.submit();
      }

      function removeSelectedUsers() {
        var form = document.forms.membersForm;
        form.action.value = "remove";
        form.submit();
      }
    </script>

    <div style="display: none">
      <form name="actionForm" method="POST" action="members">
        <input type="hidden" name="action"/>
        <input type="hidden" name="userId"/>
      </form>
    </div>
  </xsl:template>

  <xsl:template match="members">
    <form name="membersForm" method="POST">
      <input type="hidden" name="action"/>

      <table class="default">
        <tr>
          <th>ID</th>
          <th>Login</th>
          <th>Display name</th>
          <th>Select</th>
        </tr>
        <xsl:apply-templates select="member">
          <xsl:sort select="@login" order="ascending"/>
        </xsl:apply-templates>
      </table>
    </form>
  </xsl:template>

  <xsl:template match="member">
    <tr>
      <td><xsl:value-of select="@id"/></td>
      <td><xsl:value-of select="@login"/></td>
      <td><xsl:value-of select="@displayName"/></td>
      <td><input type="checkbox" name="userId" value="{@id}"/></td>
    </tr>
  </xsl:template>

  <xsl:template name="actions">
    [<a href="#" onclick="addUser(); return false;">Add a member</a>]
    [<a href="#" onclick="removeSelectedUsers(); return false;">Remove selected members</a>]
    [<a href="#" onclick="removeAllUsers(); return false;">Remove all members</a>]
  </xsl:template>

</xsl:stylesheet>