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
  xmlns:a="http://outerx.org/daisy/1.0#documentActions"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:urlencoder="xalan://java.net.URLEncoder">

  <xsl:import href="daisyskin:xslt/util.xsl"/>

  <xsl:variable name="context" select="page/context"/>
  <xsl:variable name="mountPoint" select="string($context/mountPoint)"/>
  <xsl:variable name="skin" select="string($context/skin)"/>
  <xsl:variable name="basePath" select="concat($mountPoint,'/',$context/site/@name, '/')"/>
  <xsl:variable name="continuationId" select="string(/page/taskMonitoring/continuationId)"/>
  <xsl:variable name="continuationPath" select="string(/page/taskMonitoring/continuationPath)"/>
  <xsl:variable name="task" select="page/taskMonitoring/t:task"/>
  
  <xsl:template match="page">
    <page>
      <xsl:copy-of select="/page/*[name() != 'taskMonitoring']|/page/text()"/>
      <pageTitle><i18n:text key="serp.title"/></pageTitle>
      <content>
        <xsl:call-template name="taskMonitorCallbacks"/>
        <xsl:call-template name="content"/>
      </content>
      <extraMainContent><xsl:copy-of select="taskMonitoring/baseSystem/node()"/></extraMainContent>
    </page>
  </xsl:template>

  <xsl:template name="taskMonitorCallbacks">
    <script language="javascript">
      dojo.require("dojo.html.*");
      
      function getProgressResource() {
        return "searchProgress";
      }
      
      function taskFinished() {
        dojo.html.hide('dsy-serp-task-busy');
        dojo.html.show('dsy-serp-task-finished');
        window.location="<xsl:value-of select="$continuationPath"/>/replace";
      }
      
      function updateProgress(progress) {
        var progressElem = document.getElementById('dsy-serp-progressIndication');
        dojo.dom.removeChildren(progressElem);
        progressElem.appendChild(document.createTextNode(progress));
      }
    </script>
  </xsl:template>
  
  <xsl:template name="content">
    <h1><i18n:text key="serp.search.progress.title"/></h1>

    <div id="dsy-serp-task-busy">
      <xsl:if test="$task/@finishedAt">
        <xsl:attribute name="style">display:none</xsl:attribute>
      </xsl:if>
      <p><i18n:text key="serp.search.patience"/></p>
      <p><i18n:text key="serp.current-progress"/>: <span id="dsy-serp-progressIndication"><xsl:value-of select="$task/@progress"/></span></p>
      <div id="dsy-serp-progressupdate-failed" style="display:none" class="dsy-error">
        <i18n:text key="serp.progress-update-failed"/>
      </div>
    </div>

    <div id="dsy-serp-task-finished">
      <xsl:if test="not($task/@finishedAt)">
        <xsl:attribute name="style">display:none</xsl:attribute>
      </xsl:if>

      <p>
        <i18n:text key="serp.search.finished.redirect-to"/>
        <a href="{$continuationPath}/replace"><i18n:text key="serp.search.finished.searchresults-page"/></a>.
      </p>
    </div>
  </xsl:template>

</xsl:stylesheet>