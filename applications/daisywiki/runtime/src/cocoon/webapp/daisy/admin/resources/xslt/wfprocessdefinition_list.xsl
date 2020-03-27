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

  <xsl:import href="daisyskin:xslt/util.xsl"/>
  <xsl:variable name="instanceCounts" select="/page/wf:processInstanceCounts/wf:processInstanceCount"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle>Daisy: Workflow Process Definitions Administration</pageTitle>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1>Workflow Process Definitions Administration</h1>
    <ul>
      <li><a href="wfProcessDefinition/upload">Upload a process definition</a></li>
      <li>
        <xsl:call-template name="generatePostLink">
          <xsl:with-param name="action">wfProcessDefinition?action=loadSamples</xsl:with-param>
          <xsl:with-param name="label">Reinstall sample workflows</xsl:with-param>
          <xsl:with-param name="confirmMessage">Are you sure you want to reinstall the sample workflows? (existing workflows will be left intact)</xsl:with-param>
          <xsl:with-param name="id">reinstall-sample-workflows</xsl:with-param>
        </xsl:call-template>
        </li>
    </ul>

    <xsl:choose>
      <xsl:when test="wf:processDefinitions/wf:processDefinition">
        <xsl:apply-templates select="wf:processDefinitions"/>
      </xsl:when>
      <xsl:otherwise>
        <p>There are currently no workflow process definitions defined.</p>
      </xsl:otherwise>
    </xsl:choose>

    <script type="text/javascript">
      function deleteProcessDefinition(id, name) {
        var confirmed = confirm("BE VERY CAREFUL! Deleting a process definition will also delete any open or closed process instances based on that definition.\n\nAre you sure you want to delete the process definition \"" + name + "\"?");
        if (confirmed) {
          var form = document.forms.actionForm;
          form.id.value = id;
          form.submit();
        }
      }
    </script>
    <div style="display: none">
      <form name="actionForm" method="POST" action="wfProcessDefinition">
        <input type="hidden" name="action" value="delete"/>
        <input type="hidden" name="id"/>
      </form>
    </div>
  </xsl:template>

  <!-- Group the process definitions by name using "The Muenchian Method" -->
  <xsl:key name="processdefs-by-name" match="wf:processDefinitions/wf:processDefinition" use="@name"/>

  <xsl:template match="wf:processDefinitions">
    <table class="default">
      <tr>
        <th></th>
        <th>ID</th>
        <th>Version</th>
        <th>Instance count</th>
        <th>Actions</th>
      </tr>
      <xsl:for-each select="wf:processDefinition[count(. | key('processdefs-by-name', @name)[1]) = 1]">
        <xsl:sort select="@name"/>
        <tr>
          <td colspan="5">
            <b><xsl:value-of select="@name"/></b>
            <xsl:if test="wf:label">
              <div style="margin-left: 1em; font-size: small">
                Label: <i><xsl:copy-of select="wf:label/node()"/></i>
              </div>
            </xsl:if>
            <xsl:if test="wf:description">
              <div style="margin-left: 1em; font-size: small">
                Description: <i><xsl:copy-of select="wf:description/node()"/></i>
              </div>
            </xsl:if>
          </td>
        </tr>
        <xsl:for-each select="key('processdefs-by-name', @name)">
          <xsl:sort select="@version" data-type="number" order="descending"/>
          <tr>
            <td></td>
            <td><xsl:value-of select="@id"/></td>
            <td><xsl:value-of select="@version"/></td>
            <td>
              <xsl:variable name="defId" select="@id"/>
              <xsl:variable name="instanceCount" select="$instanceCounts[@definitionId = $defId]/@count"/>
              <xsl:choose>
                <xsl:when test="$instanceCount != ''">
                  <xsl:value-of select="$instanceCount"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:text>0</xsl:text>
                </xsl:otherwise>
              </xsl:choose>
            </td>
            <td>
              <a href="#" onClick="deleteProcessDefinition('{@id}', '{daisyutil:escape(concat(@name, ' (version ', @version, ')'))}'); return false;">delete</a>
            </td>
          </tr>
        </xsl:for-each>
      </xsl:for-each>
    </table>
  </xsl:template>

</xsl:stylesheet>