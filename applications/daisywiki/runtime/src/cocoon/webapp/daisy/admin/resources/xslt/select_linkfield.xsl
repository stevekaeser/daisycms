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
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:daisyutil="xalan://org.outerj.daisy.frontend.util.XslUtil">

  <xsl:variable name="mountPoint" select="string(page/context/mountPoint)"/>
  <xsl:variable name="skin" select="string(page/context/skin)"/>

  <xsl:template match="page">
    <html>
      <head>
        <title>Select link field</title>
        <script type="text/javascript">
          function selectField(id, name) {
              window["fieldSelected"](id, name);
              window.close();
          }
        </script>
        <link rel="stylesheet" type="text/css" href="{$mountPoint}/resources/skins/{$skin}/css/popup.css"/>
      </head>
      <body>
        <xsl:apply-templates select="linkFields"/>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="linkFields">
    <h1>Select a link field</h1>
    <table class="daisySelectList" width="100%">
      <col width="5%"/>
      <col width="40%"/>
      <tr>
        <th>ID</th>
        <th>Name</th>
        <th>Label</th>
      </tr>
      <xsl:choose>
        <xsl:when test="field">
          <xsl:apply-templates select="field">
            <xsl:sort select="@name"/>
          </xsl:apply-templates>          
        </xsl:when>
        <xsl:otherwise>
          <tr>
            <td colspan="3"><i>No link-type fields available.</i></td>
          </tr>
        </xsl:otherwise>
      </xsl:choose>
    </table>
  </xsl:template>

  <xsl:template match="field">
    <tr>
      <td><xsl:value-of select="@id"/></td>
      <td><a href="#" onclick="selectField('{@id}', '{daisyutil:escape(@name)}');"><xsl:value-of select="@name"/></a></td>
      <td><xsl:value-of select="@label"/></td>
    </tr>
    <tr>
      <td/>
      <td colspan="2">
        <i><xsl:value-of select="@description"/></i>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>