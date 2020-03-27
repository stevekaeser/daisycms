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
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:daisyutil="xalan://org.outerj.daisy.frontend.util.XslUtil"
  xmlns:fi="http://apache.org/cocoon/forms/1.0#instance">

  <xsl:variable name="mountPoint" select="string(page/context/mountPoint)"/>
  <xsl:variable name="skin" select="string(page/context/skin)"/>

  <xsl:template match="page">
    <xsl:apply-templates select="selectionList"/>
  </xsl:template>

  <xsl:template match="selectionList">
    <html>
      <head>
        <script>
          function selected(element) {
            var value = element.getAttribute("daisyValue");
            var label = element.getAttribute("daisyLabel");
            var hierarchicalLabel = element.getAttribute("daisyHierLabel");
            if (hierarchicalLabel != null &amp;&amp; hierarchicalLabel != "")
              label = hierarchicalLabel;
            window.onLinkSelected(label, value);
            window.close();
          }
        </script>
        <link rel="stylesheet" type="text/css" href="{$mountPoint}/resources/skins/{$skin}/css/popup.css"/>
      </head>
      <body>
        <p>Select a value for <xsl:value-of select="fieldTypeLabel"/>:</p>
        <xsl:apply-templates select="fi:selection-list"/>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="fi:selection-list">
    <ul>
      <xsl:choose>
        <xsl:when test="../@hierarchical='true'">
          <xsl:attribute name="class">hierarchicalSelectList</xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="class">selectList</xsl:attribute>          
        </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="fi:item"/>
    </ul>
  </xsl:template>

  <xsl:template match="fi:item">
    <li>
      <a onclick="selected(this); return false;" daisyValue="{@value}" daisyLabel="{fi:label}" daisyHierLabel="{@hierarchicalLabel}" href="#" onmouseover="window.status=''; return true;">
        <xsl:value-of select="fi:label"/>
      </a>
      <xsl:if test="fi:item">
        <ul>
          <xsl:apply-templates select="fi:item"/>
        </ul>
      </xsl:if>
    </li>
  </xsl:template>
</xsl:stylesheet>