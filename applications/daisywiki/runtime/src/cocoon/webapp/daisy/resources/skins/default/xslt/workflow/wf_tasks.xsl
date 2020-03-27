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
  xmlns:d="http://outerx.org/daisy/1.0"
  xmlns:wf="http://outerx.org/daisy/1.0#workflow"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:daisyutil="xalan://org.outerj.daisy.frontend.util.XslUtil">

  <xsl:variable name="title"><i18n:text key="wftasks.title"/></xsl:variable>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><xsl:copy-of select="$title"/></pageTitle>
      <layoutHints wideLayout="true" needsDojo="true"/>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1><xsl:copy-of select="$title"/></h1>

    <i18n:text key="wf.section.my-tasks"/>
    | <a href="taskSearch"><i18n:text key="wf.section.search-tasks"/></a>
    | <a href="processSearch"><i18n:text key="wf.section.search-processes"/></a>
    | <a href="timerSearch"><i18n:text key="wf.section.search-timers"/></a>
    <br/>
    <br/>

    <h2><i18n:text key="wftasks.my-tasks"/></h2>
    <div id="myTasks" dojoType="dojo:ContentPane"/>

    <h2><i18n:text key="wftasks.pooled-tasks"/></h2>
    <div id="pooledTasks" dojoType="dojo:ContentPane"/>

    <xsl:variable name="returnTo" select="/page/context/@uri"/>

    <script>
      dojo.require("daisy.workflow");

      dojo.addOnLoad(function() {
        new daisy.workflow.SearchResultController("myTasks", "tasks/mine", "<xsl:value-of select="daisyutil:escape(/page/context/request/@uri)"/>", "<i18n:text key="wfsearch.return-to-tasks"/>");
        new daisy.workflow.SearchResultController("pooledTasks", "tasks/pooled", "<xsl:value-of select="daisyutil:escape(/page/context/request/@uri)"/>", "<i18n:text key="wfsearch.return-to-tasks"/>");
      });
    </script>
  </xsl:template>

</xsl:stylesheet>