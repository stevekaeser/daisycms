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
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><i18n:text key="login.title"/></pageTitle>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <xsl:variable name="user" select="context/user"/>
    <h1><i18n:text key="login.title"/></h1>

    <script>
      daisyPushOnLoad(function() {
        document.getElementById("username").select();
        document.getElementById("username").focus();
      });
    </script>

    <form action="{continuationId}" method="POST">
      <table class="dsyfrm-table">
        <tr>
          <td class="dsyfrm-labelcell dsy-nowrap"><i18n:text key="login.user-label"/></td>
          <td class="dsyfrm-widgetcell dsy-nowrap">
            <input name="username" id="username" type="text" style="width: 20em;">
              <!-- Don't set value in case of 'guest' to enable browsers which have
                   the credentials remembered to insert them automatically. -->
              <xsl:if test="$user/login != 'guest'">
                <xsl:attribute name="value"><xsl:value-of select="$user/login"/></xsl:attribute>
              </xsl:if>
            </input>
          </td>
        </tr>
        <tr>
          <td><i18n:text key="login.password-label"/></td>
          <td><input name="password" type="password" style="width: 20em;"/></td>
        </tr>
        <tr>
          <td/>
          <td class="dsyfrm-primaryaction">
            <input class="dsyfrm-primaryaction" type="submit" value="login.login-button" i18n:attr="value"/>
            <input type="hidden" name="action" value="login"/>
          </td>
        </tr>
        <tr>
          <td/>
          <td>
            <a href="{context/mountPoint}/passwordReminder"><i18n:text key="login.forgot-password"/></a>
          </td>
        </tr>
      </table>
    </form>

    <!-- Show role switch option only if user has a default role or the Administrator role -->
    <xsl:if test="$user/availableRoles/@default != '' or $user/availableRoles/role[@id='1']">
      <br/>
      <br/>
      <h1><i18n:text key="login.switchrole"/></h1>
      <form action="{continuationId}" method="POST">
        <select name="newrole">
          <xsl:variable name="adminRole" select="$user/availableRoles/role[@id='1']"/>
          <xsl:if test="count($user/availableRoles/role) - count($adminRole) > 1">
            <option value="all">
              <i18n:text key="login.use-all-roles"/>
              <xsl:text> </xsl:text>
              <xsl:if test="$adminRole"><i18n:text key="login.excl-administrator"/></xsl:if>
            </option>
          </xsl:if>
          <xsl:for-each select="$user/availableRoles/role">
            <option value="{@id}"><xsl:value-of select="@name"/></option>
          </xsl:for-each>
        </select>
        <input type="hidden" name="action" value="changeRole"/>
        <xsl:text> </xsl:text>
        <input type="submit" value="login.switchrole-button" i18n:attr="value"/>
      </form>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>