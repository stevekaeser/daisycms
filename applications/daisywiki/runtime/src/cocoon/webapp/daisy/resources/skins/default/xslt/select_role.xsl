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
        <title><i18n:text key="selectrole.title"/></title>
        <script type="text/javascript">
          function selectRole(id, name) {
            window["onRoleSelected"](id, name);
            window.close();
          }
        </script>
        <link rel="stylesheet" type="text/css" href="{$mountPoint}/resources/skins/{$skin}/css/popup.css"/>
      </head>
      <body>
        <h1><i18n:text key="selectrole.title"/></h1>
        <xsl:apply-templates select="d:roles"/>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="d:roles">
    <table class="daisySelectList" width="100%">
      <col width="5%"/>
      <col width="35%"/>
      <col width="60%"/>
      <tr>
        <th><i18n:text key="selectrole.id"/></th>
        <th><i18n:text key="selectrole.name"/></th>
        <th><i18n:text key="selectrole.description"/></th>
      </tr>
      <xsl:apply-templates select="d:role">
        <xsl:sort select="@name"/>
      </xsl:apply-templates>
    </table>
  </xsl:template>

  <xsl:template match="d:role">
    <tr>
      <td><xsl:value-of select="@id"/></td>
      <td><a href="#" onclick="selectRole('{@id}', '{daisyutil:escape(@name)}');"><xsl:value-of select="@name"/></a></td>
      <td><xsl:value-of select="@description"/></td>
    </tr>
  </xsl:template>

</xsl:stylesheet>