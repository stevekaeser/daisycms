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
        <title><i18n:text key="selectuser.title"/></title>
        <script type="text/javascript">
          function selectUser(id, name) {
            window["onUserSelected"](id, name);
            window.close();
          }
        </script>
        <link rel="stylesheet" type="text/css" href="{$mountPoint}/resources/skins/{$skin}/css/popup.css"/>
      </head>
      <body>
        <h1><i18n:text key="selectuser.title"/></h1>
        <xsl:apply-templates select="d:publicUserInfos"/>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="d:publicUserInfos">
    <table class="daisySelectList" width="100%">
      <col width="5%"/>
      <col width="35%"/>
      <col width="60%"/>
      <tr>
        <th><i18n:text key="selectuser.id"/></th>
        <th><i18n:text key="selectuser.login"/></th>
        <th><i18n:text key="selectuser.name"/></th>
      </tr>
      <xsl:apply-templates select="d:publicUserInfo[@id != 1]">
        <xsl:sort select="@login"/>
      </xsl:apply-templates>
    </table>
  </xsl:template>

  <xsl:template match="d:publicUserInfo">
    <tr>
      <td><xsl:value-of select="@id"/></td>
      <td><a href="#" onclick="selectUser('{@id}', '{daisyutil:escape(@login)}');"><xsl:value-of select="@login"/></a></td>
      <td><xsl:value-of select="@displayName"/></td>
    </tr>
  </xsl:template>

</xsl:stylesheet>