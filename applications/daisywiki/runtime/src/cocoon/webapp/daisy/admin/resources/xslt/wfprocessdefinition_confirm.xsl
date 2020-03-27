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

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle>Daisy: Process Definition Upload Confirmation</pageTitle>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1>Process Definition uploaded</h1>

    <xsl:apply-templates select="wf:processDefinition"/>
  </xsl:template>

  <xsl:template match="wf:processDefinition">
    <p>The process definition <b><xsl:value-of select="@name"/></b> (ID <xsl:value-of select="@id"/>,
      version <xsl:value-of select="@version"/>) has been uploaded.</p>

    <xsl:choose>
      <xsl:when test="wf:problems/wf:problem">
        <p>Reported problems:</p>
        <ul>
          <xsl:for-each select="wf:problems/wf:problem">
            <li><xsl:value-of select="."/></li>
          </xsl:for-each>
        </ul>
      </xsl:when>
      <xsl:otherwise>
        <p>(No problems reported)</p>
      </xsl:otherwise>
    </xsl:choose>

    <a href="../../wfProcessDefinition">Back to process definitions overview</a>
  </xsl:template>


</xsl:stylesheet>