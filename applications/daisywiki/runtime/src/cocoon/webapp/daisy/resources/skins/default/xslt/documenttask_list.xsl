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
  xmlns:t="http://outerx.org/daisy/1.0#doctaskrunner"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:variable name="mountPoint" select="string(page/context/mountPoint)"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><i18n:text key="doctasklist.title"/></pageTitle>
      <layoutHints wideLayout="true"/>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <script type="text/javascript">
      function showDetails(div, title) {
        var popup = window.open("", "",
                "toolbar=no,menubar=no,personalbar=no,width=800,height=500,left=20,top=40" +
                ",scrollbars=yes,resizable=yes");

        var doc = popup.document;
        doc.open();
        doc.write("&lt;html>&lt;head>&lt;title>" + title + "&lt;/title>&lt;body>&lt;pre>");
        doc.write(div.innerHTML);
        doc.write("&lt;/pre>&lt;/body>&lt;/html>");
        doc.close();
      }
    </script>
    
    <h1><i18n:text key="doctasklist.title"/></h1>
    <p><i18n:text key="doctasklist.info"/></p>
    <ul>
      <li><a href="doctask/new"><i18n:text key="doctasklist.create-doctask"/></a></li>
    </ul>
    <xsl:apply-templates select="t:tasks"/>

    <script type="text/javascript">
      function deleteTask(id) {
        var confirmMessage = "<i18n:text key="doctasklist.confirm-delete"/>".replace(/\$id/g, id);
        var confirmed = confirm(confirmMessage);
        if (confirmed) {
          var form = document.forms.actionForm;
          form.action = "<xsl:value-of select="$mountPoint"/>/doctask/" + id + "?action=delete";
          form.submit();
        }
      }
      function interruptTask(id) {
        var confirmMessage = "<i18n:text key="doctasklist.confirm-interrupt"/>".replace(/\$id/g, id);
        var confirmed = confirm(confirmMessage);
        if (confirmed) {
          var form = document.forms.actionForm;
          form.action = "<xsl:value-of select="$mountPoint"/>/doctask/" + id + "?action=interrupt";
          form.submit();
        }
      }
    </script>
    <div style="display: none">
      <form name="actionForm" method="POST" action="">
      </form>
    </div>
  </xsl:template>

  <xsl:template match="t:tasks">
    <table class="default">
      <tr>
        <th><i18n:text key="doctasklist.id"/></th>
        <th><i18n:text key="doctasklist.owner"/></th>
        <th><i18n:text key="doctasklist.state"/></th>
        <th><i18n:text key="doctasklist.progress"/></th>
        <th><i18n:text key="doctasklist.started-at"/></th>
        <th><i18n:text key="doctasklist.finished-at"/></th>
        <th><i18n:text key="doctasklist.description"/></th>
        <th><i18n:text key="doctasklist.script"/></th>
        <th><i18n:text key="doctasklist.details"/></th>
        <th><i18n:text key="doctasklist.actions"/></th>
      </tr>
      <xsl:choose>
        <xsl:when test="t:task">
          <xsl:apply-templates select="t:task"/>
        </xsl:when>
        <xsl:otherwise>
          <td colspan="10"><i><i18n:text key="doctasklist.no-tasks"/></i></td>
        </xsl:otherwise>
      </xsl:choose>
    </table>
  </xsl:template>

  <xsl:template match="t:task">
    <tr>
      <td><xsl:value-of select="@id"/></td>
      <td><xsl:value-of select="@ownerName"/></td>
      <td><i18n:text key="taskstate.{@state}"/></td>
      <td><xsl:value-of select="@progress"/></td>
      <td><xsl:value-of select="@startedAtFormatted"/></td>
      <td><xsl:value-of select="@finishedAtFormatted"/></td>
      <td><xsl:value-of select="@description"/></td>
      <td>
        <xsl:variable name="id" select="generate-id(.)"/>
        <div id="script{$id}" style="display: none">
          <xsl:copy-of select="t:action/t:parameters/node()"/>
        </div>
        <a href="#" onmouseover="status=''; return true;" onclick="showDetails(document.getElementById('script{$id}'), 'Parameters of task {@id}'); return false;"><i18n:text key="doctasklist.show"/></a>
      </td>
      <td>
        <xsl:if test="t:details">
          <xsl:variable name="id" select="generate-id(.)"/>
          <div id="{$id}" style="display: none">
            <xsl:copy-of select="t:details/node()"/>
          </div>
          <a href="#" onmouseover="status=''; return true;" onclick="showDetails(document.getElementById('{$id}'), 'Details of task {@id}'); return false;"><i18n:text key="doctasklist.show"/></a>
        </xsl:if>
      </td>
      <td>
        <a href="#" onmouseover="status=''; return true;" onclick="deleteTask('{@id}'); return false;"><i18n:text key="doctasklist.delete"/></a>
        |
        <a href="#" onmouseover="status=''; return true;" onclick="interruptTask('{@id}'); return false;"><i18n:text key="doctasklist.interrupt"/></a>
        |
        <a href="doctask/{@id}/docdetails"><i18n:text key="doctasklist.show-details"/></a>
        <xsl:variable name="type" select="t:action/@type"/>
        <xsl:if test="/page/allowedActionTypes/allowedActionType[@name=$type]">
          |
          <a href="doctask/new?restartTask={@id}" title="doctasklist.restart.title" i18n:attr="title"><i18n:text key="doctasklist.restart"/></a>
        </xsl:if>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>
