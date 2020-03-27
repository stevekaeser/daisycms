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

<!--
   |
   | This stylesheet is intended for inclusion in layout.xsl and presumes the existence
   | of some variables
   |
   -->

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:urlencoder="xalan://java.net.URLEncoder">

  <xsl:import href="daisyskin:xslt/pagevars.xsl"/>

  <xsl:variable name="skin" select="string(page/context/skin)"/>
  <xsl:variable name="selectedPath"><xsl:if test="$navigationTree"><xsl:value-of select="string($navigationTree/@selectedPath)"/></xsl:if></xsl:variable>
  <xsl:variable name="contextDoc" select="/page/document"/>
  <xsl:variable name="documentId" select="$contextDoc/@id"/>
  <!-- this will only be valid if $inSite is true -->
  <xsl:variable name="documentPath">
    <xsl:choose>
      <xsl:when test="$selectedPath != ''">
        <xsl:value-of select="concat($mountPoint, '/', $siteName, $selectedPath)"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="concat($mountPoint, '/', $siteName, '/', urlencoder:encode($documentId))"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:template name="menu">
      <!-- The general navigation menu -->
      <ul class="tabNavigation">
        <li><a href="#" onmouseover="status=''; return true;" class="menuLink"><i18n:text catalogue="skin" key="layout.user"/><xsl:text> </xsl:text><xsl:value-of select="$user/name"/></a>
          <xsl:if test="$requestMethod = 'GET'">
            <ul>
              <li class="tabNavigation"><a href="{$mountPoint}/login?returnTo={$pageURIEncoded}"><i18n:text catalogue="skin" key="layout.changelogin"/></a></li>
              <li class="tabNavigation"><a href="{$mountPoint}/registration?returnTo={$pageURIEncoded}"><i18n:text catalogue="skin" key="layout.registration"/></a></li>
              <xsl:if test="$user/name != 'guest'">
                <xsl:variable name="logoutPath">
                  <xsl:choose>
                    <xsl:when test="$inSite = 'true'">
                      <xsl:value-of select="concat($basePath, '/logout')"/>
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:value-of select="concat($mountPoint, '/logout')"/>
                    </xsl:otherwise>
                  </xsl:choose>
                </xsl:variable>
                <li class="tabNavigation">
                  <!-- Do logout via a POST so that robots don't follow that link automatically, though it can be done using GET too -->
                  <xsl:call-template name="generatePostLink">
                    <xsl:with-param name="action" select="$logoutPath"/>
                    <xsl:with-param name="label"><i18n:text catalogue="skin" key="layout.logout"/></xsl:with-param>
                    <xsl:with-param name="id" select="generate-id()"/>
                  </xsl:call-template>
                </li>
              </xsl:if>
              <xsl:if test="$user/updateableByUser/text() = 'true'">
                <li class="tabNavigation"><a href="{$mountPoint}/usersettings?returnTo={$pageURIEncoded}"><i18n:text catalogue="skin" key="layout.user-settings"/></a></li>
              </xsl:if>
              <li class="tabNavigation"><a href="{$mountPoint}/locale?returnTo={$pageURIEncoded}"><i18n:text catalogue="skin" key="layout.change-locale"/></a></li>
              <xsl:if test="$inSite = 'true' and $user/name != 'guest'">
                <li class="tabNavigation"><a href="{$mountPoint}/{$siteName}/myComments"><i18n:text catalogue="skin" key="layout.my-comments"/></a></li>
              </xsl:if>
            </ul>
          </xsl:if>
        </li>
        <!-- The role-switching menu is only shown if the user has a default role or if the user
             has the administrator role. Otherwise, all roles of the user are active at once and
             the user doesn't really need the ability to switch roles. -->
        <xsl:if test="$user/availableRoles/@default != '' or $user/availableRoles/role[@id='1']">
          <li class="tabNavigation">
            <xsl:variable name="roleCount" select="count($user/activeRoles/role)"/>
            <a href="#" onmouseover="status=''; return true;" class="menuLink">
              <i18n:text catalogue="skin" key="layout.role"/>
              <xsl:text> </xsl:text>
              <xsl:choose>
                <xsl:when test="$roleCount > 1">
                  <i18n:text catalogue="skin" key="layout.multiple-roles"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="$user/activeRoles/role[1]/@name"/>
                </xsl:otherwise>
              </xsl:choose>
            </a>
            <xsl:if test="$requestMethod = 'GET'">
              <ul>
                <xsl:variable name="adminRole" select="$user/availableRoles/role[@id='1']"/>
                <xsl:if test="count($user/availableRoles/role) - count($adminRole) > 1">
                  <li class="tabNavigation">
                    <xsl:call-template name="generatePostLink">
                      <xsl:with-param name="action" select="concat($mountPoint, '/login?action=changeRole&amp;newrole=all&amp;returnTo=', $pageURIEncoded)"/>
                      <xsl:with-param name="label"><i18n:text catalogue="skin" key="layout.use-all-roles"/><xsl:if test="$adminRole"><br/>&#160;&#160;<i18n:text catalogue="skin" key="layout.excl-administrator"/></xsl:if></xsl:with-param>
                      <xsl:with-param name="id" select="'all-role'"/>
                    </xsl:call-template>
                  </li>
                  <li class="dsy-menuseparator"><div/></li>
                </xsl:if>
                <xsl:if test="$adminRole">
                  <li>
                    <xsl:call-template name="generatePostLink">
                      <xsl:with-param name="action" select="concat($mountPoint, '/login?action=changeRole&amp;newrole=1&amp;returnTo=', $pageURIEncoded)"/>
                      <xsl:with-param name="label"><xsl:value-of select="$adminRole/@name"/></xsl:with-param>
                      <xsl:with-param name="id" select="'admin-role'"/>
                    </xsl:call-template>
                  </li>
                </xsl:if>
                <xsl:for-each select="$user/availableRoles/role[@id != '1']">
                  <xsl:sort select="@name"/>
                  <li class="tabNavigation">
                    <xsl:call-template name="generatePostLink">
                      <xsl:with-param name="action" select="concat($mountPoint, '/login?action=changeRole&amp;newrole=', @id, '&amp;returnTo=', $pageURIEncoded)"/>
                      <xsl:with-param name="label"><xsl:value-of select="@name"/></xsl:with-param>
                      <xsl:with-param name="id" select="generate-id()"/>
                    </xsl:call-template>
                  </li>
                </xsl:for-each>
              </ul>
            </xsl:if>
          </li>
        </xsl:if>
        <li class="tabNavigation">
          <a href="#" onmouseover="status=''; return true;" class="menuLink"><i18n:text catalogue="skin" key="layout.tools"/></a>
          <ul>

            <!-- Query search -->
            <xsl:if test="$inSite = 'true'">
              <li class="tabNavigation"><a href="{$basePath}/querySearch"><i18n:text catalogue="skin" key="layout.query-search"/></a></li>
            </xsl:if>

            <!-- Document tasks -->
            <xsl:if test="not($onlyGuestRole)">
              <li class="tabNavigation"><a href="{$mountPoint}/doctask"><i18n:text catalogue="skin" key="layout.doctask"/></a></li>
            </xsl:if>

            <!-- Search and replace -->
            <xsl:if test="$inSite = 'true' and not($onlyGuestRole)">
              <xsl:choose>
                <xsl:when test="$contextDoc">
                  <xsl:variable name="url" select="concat($basePath,'/searchAndReplace','?documentId=', urlencoder:encode($documentId), '&amp;branch=', urlencoder:encode($contextDoc/@branch), '&amp;language=', urlencoder:encode($contextDoc/@language))"/>
                  <li class="tabNavigation"><a href="{$url}"><i18n:text catalogue="skin" key="layout.search-and-replace"/></a></li>
                </xsl:when>
                <xsl:otherwise>
                  <li class="tabNavigation"><a href="{$basePath}/searchAndReplace"><i18n:text catalogue="skin" key="layout.search-and-replace"/></a></li>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:if>

            <!-- Books -->
            <li class="tabNavigation"><a href="{$mountPoint}/books"><i18n:text catalogue="skin" key="layout.books"/></a></li>

            <!-- Document basket -->
            <xsl:if test="$inSite = 'true'">
              <li class="tabNavigation"><a href="{$basePath}/documentBasket"><i18n:text catalogue="skin" key="layout.basket"/></a></li>
            </xsl:if>

            <xsl:if test="$inSite = 'true' and not($onlyGuestRole)">
              <!-- Variables -->
              <li class="tabNavigation"><a href="{$basePath}/variables"><i18n:text catalogue="skin" key="layout.variables"/></a></li>

              <!-- Translation management -->
              <li class="tabNavigation"><a href="{$basePath}/translmgmt/search"><i18n:text catalogue="skin" key="layout.translation-management"/></a></li>

              <!-- Workflow -->
              <xsl:variable name="startProcessParams">
                <xsl:if test="$contextDoc">
                  <xsl:variable name="documentLink" select="concat('daisy:', $contextDoc/@id, '@', $contextDoc/@branch, ':', $contextDoc/@language, ':', $contextDoc/@versionId)"/>
                  <xsl:value-of select="concat('?documentLink=', urlencoder:encode($documentLink, 'UTF-8'), '&amp;documentName=', urlencoder:encode($contextDoc/@name, 'UTF-8'))"/>
                </xsl:if>
              </xsl:variable>
              <li class="tabNavigation"><a href="{$basePath}/workflow/processDefinitions{$startProcessParams}"><i18n:text catalogue="skin" key="layout.new-workflow"/></a></li>
              <li class="tabNavigation"><a href="{$basePath}/workflow/tasks"><i18n:text catalogue="skin" key="layout.workflow-console"/></a></li>
            </xsl:if>
          </ul>
        </li>

        <xsl:if test="$inSite = 'true'">

          <!-- Recent changes -->
          <li><a href="{$basePath}/recentChanges"><i18n:text catalogue="skin" key="layout.recent-changes"/></a></li>

          <!-- New document -->
          <xsl:if test="$user/name != 'guest'">
            <xsl:choose>
              <xsl:when test="$selectedPath = ''">
                <li class="tabNavigation"><a href="{$basePath}/new"><i18n:text catalogue="skin" key="layout.new-document"/></a></li>
              </xsl:when>
              <xsl:otherwise>
                <li class="tabNavigation"><a href="{$basePath}{$selectedPath}/../new"><i18n:text catalogue="skin" key="layout.new-document"/></a></li>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:if>

        </xsl:if>

        <!-- Administration -->
        <xsl:if test="$user/activeRoles/role[@id='1']">
          <li class="tabNavigation"><a href="{$mountPoint}/admin"><i18n:text catalogue="skin" key="layout.administration"/></a></li>
        </xsl:if>

        <xsl:if test="$pageNavigationInMenu">
          <xsl:if test="/page/pageNavigation">
            <li class="tabNavigation">
              <a href="#" onmouseover="status=''; return true;" class="menuLink"><i18n:text catalogue="skin" key="layout.page-actions"/></a>
              <ul>
                <xsl:for-each select="/page/pageNavigation/*">
                  <xsl:choose>
                    <xsl:when test="local-name(.) = 'separator'">
                      <li class="dsy-menuseparator"><div/></li>
                    </xsl:when>
                    <xsl:otherwise>
                      <li class="tabNavigation">
                        <xsl:choose>
                          <xsl:when test="@needsPost='true'">
                            <xsl:call-template name="generatePostLink">
                              <xsl:with-param name="action" select="path"/>
                              <xsl:with-param name="label"><xsl:copy-of select="title/node()"/></xsl:with-param>
                              <xsl:with-param name="id" select="generate-id()"/>
                            </xsl:call-template>
                          </xsl:when>
                          <xsl:when test="@script='true'">
                            <a href="#" onclick="{path}" onmouseover="status=''; return true;"><xsl:copy-of select="title/node()"/></a>
                          </xsl:when>
                          <xsl:otherwise>
                            <a href="{path}"><xsl:copy-of select="title/node()"/></a>
                          </xsl:otherwise>
                        </xsl:choose>
                      </li>
                    </xsl:otherwise>
                  </xsl:choose>
                </xsl:for-each>
              </ul>
            </li>
            <xsl:if test="$contextDoc">
              <li>
                <a href="javascript:void(0);" onmouseover="window.status=''" onclick="window.open('{$documentPath}/version/{/page/document/@versionId}/info?branch={urlencoder:encode(/page/document/@branch)}&amp;language={urlencoder:encode(/page/document/@language)}','','toolbar=no,menubar=no,personalbar=no,width=780,height=540,left=20,top=40,scrollbars=yes,resizable=yes');">
                  <img src="{concat($mountPoint, '/resources/skins/', $skin, '/images/info.png')}" alt="doclayout.docinfo" i18n:attr="alt"/>              
                </a>
              </li>
            </xsl:if>
          </xsl:if>
          <xsl:if test="/page/availableVariants">
            <xsl:variable name="availableVariants" select="/page/availableVariants"/>
            <li class="tabNavigation">
              <a href="#" onmouseover="status=''; return true;" class="menuLink">
                <i18n:text catalogue="skin" key="layout.variants"/>
                  (<xsl:value-of select="count($availableVariants/variants/variant)"/>)
              </a>
              <ul>
              <!-- The [1] is added inorder to stop variants from being shown twice (cfr DSY-231).  Supposedly this is a bug in xalan 2.7.0 -->
                <xsl:for-each select="$availableVariants[1]/variants/variant">
                  <li>
                    <a href="{@href}">
                      <span>
                        <xsl:if test="@current">
                          <xsl:attribute name="class">currentVariant</xsl:attribute>
                          <xsl:text>>> </xsl:text>
                        </xsl:if>
                        <xsl:value-of select="concat(@branchName, ' - ', @languageName)"/>
                      </span>
                    </a>
                  </li>
                </xsl:for-each>
                <xsl:if test="$availableVariants/createVariant">
                  <li><a href="{$availableVariants/createVariant/@href}"><i><i18n:text catalogue="skin" key="layout.add-variant"/></i></a></li>
                </xsl:if>
              </ul>
            </li>
          </xsl:if>
        </xsl:if>

        <xsl:if test="$user/name != 'guest' or $skinconf/versionmode/@hideFromGuest='false'">
          <li id="pointInTimeIndicator">
            <!-- LIVE/STAGING indicator and switch -->
            <div id="versionModePicker">
              <xsl:choose>
                <xsl:when test="$versionMode = 'live' or $versionMode = 'last'">
                  <i18n:text catalogue="skin" key="layout.versionmode-{$versionMode}"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="/page/context/localVersionMode"/>
                </xsl:otherwise>
              </xsl:choose>
            </div>
            <form id="versionModeForm" style="display:none" method="POST" action="{$mountPoint}/switchVersionMode?returnTo={$pageURIEncoded}">
              <input type="test" id="versionModeInput" name="versionMode" value="{$versionMode}"/>
            </form>
            <script>
              $(function(){
                $("#versionModePicker").versionmode({
                  inline: false,
                  valueInput: "#versionModeInput" <!-- storing our value in an input helps remembering the selected value on a manual page refresh -->
                  <xsl:if test="$skinconf/versionmode/@timePrecision='true'">
                  , showTime: true</xsl:if>
                  <xsl:if test="$user/name = 'guest' and $skinconf/versionmode/@hideLastFromGuest!='false'">
                  , hideLast: true</xsl:if>
                }).bind("select", function() {
                  $("#versionModeForm").submit();
                });
              });
            </script>
          </li>
          
        </xsl:if>
      </ul>
  </xsl:template>
</xsl:stylesheet>
