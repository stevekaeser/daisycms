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
  <xsl:variable name="skin" select="string(page/context/skin)"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><i18n:text key="doctaskdetails.title"/></pageTitle>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <script type="text/javascript">
      function showDetails(div) {
        var popup = window.open("", "",
                "toolbar=no,menubar=no,personalbar=no,width=800,height=500,left=20,top=40" +
                ",scrollbars=yes,resizable=yes");

        var doc = popup.document;
        doc.open();
        doc.write("&lt;html>&lt;head>&lt;title>Document Execution Details&lt;/title>&lt;body>&lt;pre>");
        doc.write(div.innerHTML);
        doc.write("&lt;/pre>&lt;/body>&lt;/html>");
        doc.close();
      }
    </script>

    <h1><i18n:text key="doctaskdetails.title"/></h1>

    <ul>
      <li><a href="../../doctask"><i18n:text key="doctaskdetails.to-task-overview"/></a></li>
    </ul>

    <table class="plainTable">
      <tr>
        <td style="text-align: right"><i18n:text key="doctaskdetails.taskid"/>:</td>
        <td><xsl:value-of select="t:task/@id"/></td>
      </tr>
      <tr>
        <td style="text-align: right"><i18n:text key="doctaskdetails.taskdescription"/>:</td>
        <td><xsl:value-of select="t:task/@description"/></td>
      </tr>
      <tr>
        <td style="text-align: right"><i18n:text key="doctaskdetails.state"/>:</td>
        <td><b><i18n:text key="taskstate.{t:task/@state}"/></b></td>
      </tr>
    </table>
    <br/>

    <xsl:apply-templates select="t:taskDocDetails"/>
  </xsl:template>

  <xsl:template match="t:taskDocDetails">
    <table class="default">
      <tr>
        <th><i18n:text key="doctaskdetails.doc-id"/></th>
        <th><i18n:text key="branch"/></th>
        <th><i18n:text key="language"/></th>
        <th><i18n:text key="doctaskdetails.execution-state"/></th>
        <th><i18n:text key="doctaskdetails.details"/></th>
      </tr>
      <xsl:apply-templates select="t:taskDocDetail"/>
    </table>
  </xsl:template>

  <xsl:template match="t:taskDocDetail">
    <tr>
      <td><xsl:value-of select="@documentId"/></td>
      <td><xsl:value-of select="@branch"/></td>
      <td><xsl:value-of select="@language"/></td>
      <td><img src="{$mountPoint}/resources/skins/{$skin}/images/docstate_{@state}.png" title="documentexecutionstate.{@state}" alt="documentexecutionstate.{@state}" i18n:attr="title alt"/></td>
      <td>
        <xsl:if test="t:details">
          <xsl:variable name="id" select="generate-id(.)"/>
          <div id="{$id}" style="display: none">
            <xsl:copy-of select="t:details/node()"/>
          </div>
          <a href="#" onmouseover="status=''; return true;" onclick="showDetails(document.getElementById('{$id}')); return false;"><i18n:text key="doctaskdetails.show"/></a>
        </xsl:if>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>