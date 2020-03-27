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
  xmlns:n="http://outerx.org/daisy/1.0#navigation"
  xmlns:p="http://outerx.org/daisy/1.0#publisher"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:urlencoder="xalan://java.net.URLEncoder"
  xmlns:daisyutil="xalan://org.outerj.daisy.frontend.util.XslUtil">

  <xsl:import href="daisyskin:xslt/navigation.xsl"/>
  <xsl:import href="daisyskin:xslt/util.xsl"/>
  <xsl:import href="daisyskin:xslt/menu.xsl"/>

  <xsl:param name="dojo-locale">en</xsl:param> <!-- Allows to configure the dojo locale from a parameter in the sitemap. This should be the same as the form locale. -->

  <!-- Create a variable with the normalized locale, dojo needs locale parts to be separated with a dash -->
  <xsl:variable name="dojoLocale">
    <xsl:choose>
      <xsl:when test="$dojo-locale != ''">
        <xsl:value-of select="translate($dojo-locale, '_', '-')"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>en</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="mountPoint" select="string(page/context/mountPoint)"/>
  <xsl:variable name="siteName" select="string(page/context/site/@name)"/>
  <xsl:variable name="siteNavigationDocId" select="string(page/context/site/@navigationDocId)"/>
  <xsl:variable name="basePath" select="concat($mountPoint, '/', $siteName)"/>
  <xsl:variable name="versionMode" select="string(page/context/versionMode)"/>
  <xsl:variable name="skinconf" select="page/context/skinconf"/>
  <xsl:variable name="user" select="page/context/user"/>
  <xsl:variable name="onlyGuestRole" select="boolean($user/activeRoles/role[@name='guest']) and count($user/activeRoles/role) = 1"/>
  <xsl:variable name="pageTitle" select="page/pageTitle"/>
  <xsl:variable name="requestMethod" select="page/context/request/@method"/>
  <xsl:variable name="pageURI" select="page/context/request/@uri"/>
  <xsl:variable name="pageURIEncoded" select="urlencoder:encode($pageURI, 'UTF-8')"/>
  <xsl:variable name="inSite" select="boolean(page/context/site)"/>
  <xsl:variable name="layoutType" select="page/context/layoutType"/>
  <xsl:variable name="navigationTree" select="page/n:navigationTree"/>
  <xsl:variable name="skin" select="page/context/skin"/>
  <xsl:variable name="pageNavigationInMenu" select="boolean('true')"/>
  <xsl:variable name="layoutHints" select="/page/layoutHints"/>
  <xsl:variable name="userLocale" select="string(/page/context/locale)"/>
  <xsl:variable name="userLanguage" select="string(/page/context/language)"/>

  <xsl:template match="page">
    <html>
      <head>
        <title><xsl:copy-of select="$pageTitle/node()"/></title>
        <xsl:choose>
          <xsl:when test="$layoutType = 'dialog'">
            <link rel="stylesheet" type="text/css" href="{$mountPoint}/resources/skins/{$skin}/css/dialog.css"/>
          </xsl:when>
          <xsl:when test="$layoutType = 'plain'">
            <link rel="stylesheet" type="text/css" href="{$mountPoint}/resources/skins/{$skin}/css/plain.css"/>
          </xsl:when>
          <xsl:otherwise>
            <link rel="stylesheet" type="text/css" href="{$mountPoint}/resources/skins/{$skin}/css/daisy.css"/>          
          </xsl:otherwise>  
        </xsl:choose>

        <!-- Make so basic things available to javascript -->
        <script type="text/javascript">
          var daisy;
          if (!daisy) daisy = new Object();
          daisy.mountPoint = "<xsl:value-of select='daisyutil:escape($mountPoint)'/>";
          daisy.skin = "<xsl:value-of select="daisyutil:escape($skin)"/>";
          <xsl:if test="/page/context/site">
            <xsl:variable name="site" select="/page/context/site"/>
            daisy.site = {};
            daisy.site.name = "<xsl:value-of select="daisyutil:escape($siteName)"/>";
            daisy.site.branch = "<xsl:value-of select="daisyutil:escape($site/@branch)"/>";
            daisy.site.branchId = "<xsl:value-of select="daisyutil:escape($site/@branchId)"/>";
            daisy.site.language = "<xsl:value-of select="daisyutil:escape($site/@language)"/>";
            daisy.site.languageId = "<xsl:value-of select="daisyutil:escape($site/@languageId)"/>";
            daisy.site.collection = "<xsl:value-of select="daisyutil:escape($site/@collection)"/>";            
          </xsl:if>
          daisy.user = {};
          daisy.user.id = "<xsl:value-of select="daisyutil:escape($user/id)"/>";
          daisy.user.login = "<xsl:value-of select="daisyutil:escape($user/login)"/>";
          daisy.user.locale = "<xsl:value-of select="$userLocale"/>";
          daisy.user.language = "<xsl:value-of select="$userLanguage"/>";
        </script>

        <xsl:if test="$layoutHints/@needsDojo = 'true'">
          <!-- Loading dojo is expensive, it should only be done on pages where it's needed, i.e. not
               on normal document pages. This code mimics what is in CForms. In case of form-pages,
               dojo is loaded by CForms. -->
          <script type="text/javascript">
            var djConfig = {};
            if (document.cookie.indexOf("dsy-dojo-debug=true") > -1) {
              djConfig.isDebug = true;
            }
            djConfig.locale = "<xsl:value-of select="$dojoLocale"/>";
          </script>
          <script type="text/javascript" src="{$mountPoint}/resources/cocoon/dojo/dojo.js"/>
          <script type="text/javascript">
            if (djConfig.isDebug) {
              dojo.require("dojo.debug.console");
              dojo.require("dojo.widget.Tree");
            }
            dojo.require("dojo.widget.*");
            dojo.registerModulePath("daisy", "../../js");
          </script>
        </xsl:if>
        <link type="text/css" href="{$mountPoint}/resources/skins/{$skin}/css/smoothness/jquery-ui-1.8.custom.css" rel="Stylesheet"/>
        <!-- 
          <script type="text/javascript" src="{$mountPoint}/resources/js/jquery/jquery-1.4.2.min.js"></script>
          <script type="text/javascript" src="{$mountPoint}/resources/js/jquery/jquery-ui-1.8.custom.min.js"></script>
          <script type="text/javascript" src="{$mountPoint}/resources/js/jquery/jquery.utils.min.js"></script>
        -->
        <script type="text/javascript" src="{$mountPoint}/resources/js/jquery/jquery-1.4.2.js"></script>
        <script type="text/javascript" src="{$mountPoint}/resources/js/jquery/jquery-ui-1.8.custom.js"></script>
        <script type="text/javascript" src="{$mountPoint}/resources/js/jquery/jquery.utils.js"></script>
        <script type="text/javascript">
          $.i18n(daisy.user.language); // set the desired user language
        </script>
        
        
        <!-- load english i18n strings. (for datepicker we don't need to load them explicitly as they are included in jquery-ui-1.8.custom.js -->
        <script src="{$mountPoint}/resources/js/jquery/jquery.daisy.i18n-en.js"></script>
        
        <xsl:if test="$userLanguage!='en'">
          <script src="{$mountPoint}/resources/js/jquery/jquery.daisy.i18n-{$userLanguage}.js"></script>
          <script src="{$mountPoint}/resources/js/jquery/ui/i18n/ui.datepicker-{$userLanguage}.js"></script>
        </xsl:if>

        <script type="text/javascript" src="{$mountPoint}/resources/js/daisy.js"/>
        <script type="text/javascript" src="{$mountPoint}/resources/js/jquery/jquery.datetimepicker.js"/>
        <script type="text/javascript" src="{$mountPoint}/resources/js/jquery/jquery.versionmode.js"/>
        <script type="text/javascript" src="{$mountPoint}/resources/skins/{$skin}/js/menu.js"/>        

        <xsl:copy-of select="extraHeadContent/node()"/>
      </head>
      <xsl:choose>
        <xsl:when test="$layoutType = 'plain'">
          <xsl:call-template name="plainLayout"/>
        </xsl:when>
        <xsl:when test="$layoutType = 'dialog'">
          <xsl:call-template name="dialogLayout"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="defaultLayout"/>
        </xsl:otherwise>
      </xsl:choose>
    </html>
  </xsl:template>
  
  <xsl:template name="plainLayout">
    <body>
      <xsl:copy-of select="content/node()"/>
    </body>
  </xsl:template>

  <xsl:template name="dialogLayout">
    <body>
      <!-- The dialog code relies on the entire displayed content of the dialog to be wrapped in this div -->
      <div id="dialogcontent">
        <xsl:copy-of select="content/node()"/>
      </div>
    </body>
  </xsl:template>

  <xsl:template name="defaultLayout">

    <body>
      <div class="container">          
        <div id="header" parseWidgets="false">
          <!-- The logo, linked to the site homepage -->
          <div class="logo">
            <a>
              <xsl:attribute name="href">
                <xsl:choose>
                  <xsl:when test="$inSite = 'true'">
                    <xsl:value-of select="concat($basePath, '/')"/>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:value-of select="concat($mountPoint, '/')"/>
                  </xsl:otherwise>
                </xsl:choose>
              </xsl:attribute>
              <img src="{$mountPoint}/{$skinconf/logo}"/>
            </a>
            <a href="http://www.daisycms.org/" class="daisyVersion">
              Daisy <xsl:value-of select="/page/context/versionInfo/@version"/>
            </a>                    
          </div>
          <div id="siteNavigation">
          <a href="{$mountPoint}/"><xsl:value-of select="$skinconf/daisy-home-link"/></a>
          <xsl:if test="$inSite = 'true'">
            |
            <a href="{$basePath}/"><xsl:value-of select="$skinconf/site-home-link"/></a>
          </xsl:if>
        </div>
        
        <!-- Fulltext search box -->
        <xsl:if test="$inSite = 'true'">
          <div id="search">
            <form action="{$basePath}/search">
              <input type="text" name="query" id="searchInput"/>
              <xsl:text> </xsl:text>
              <input type="submit" value="skin:layout.search" id="searchButton" i18n:attr="value"/>
            </form>
          </div>
        </xsl:if>
        </div> 

        <xsl:variable name="wideLayout" select="not($navigationTree) and $layoutHints/@wideLayout = 'true'"/>

        <div class="section">
        
          <div id="generalNavigation" class="raised" parseWidgets="false">
            <b class="top"><b class="b1"></b><b class="b2"></b><b class="b3"></b><b class="b4"></b></b>
            <div  class="boxcontent">                
                <span>&#160;</span>
                <xsl:call-template name="menu"/>
            </div>
            <b class="bottom"><b class="b4b"></b><b class="b3b"></b><b class="b2b"></b><b class="b1b"></b></b>
         </div>
        
          <div id="content">
            <xsl:if test="$wideLayout">
              <xsl:attribute name="class">content-wide</xsl:attribute>
            </xsl:if>

            <xsl:copy-of select="content/node()"/>
            <!-- Copy extraMainContent, which is only inserted in the default layout and not in the plain layout -->
            <xsl:copy-of select="extraMainContent/node()"/>
          </div>       
        
        </div> 

        <xsl:if test="not($wideLayout)">
          <div id="documentNavigation" parseWidgets="false">
            <xsl:if test="$navigationTree">
              <xsl:apply-templates select="$navigationTree"/>
              <xsl:call-template name="NavigationDocActions"/>
            </xsl:if>
          </div>
        </xsl:if>

        <xsl:if test="not($pageNavigationInMenu) and boolean(pageNavigation)">
          <div id="pageNavigation" parseWidgets="false">
            <xsl:apply-templates select="pageNavigation"/>
            <xsl:apply-templates select="availableVariants"/>
          </div>
        </xsl:if>
      </div>
    </body>
    
  </xsl:template>


  <xsl:template match="pageNavigation">
    <div>
      <ul class="navigation">
        <xsl:for-each select="link">
          <li class="navigation">
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
        </xsl:for-each>
      </ul>
    </div>
  </xsl:template>

  <xsl:template match="availableVariants">
    <div>
      <div style="text-align:center">
        <a href="#" onmouseover="status=''; return true;" onclick="toggleDisplay('variantlist'); return false;">
          <i18n:text catalogue="skin" key="layout.variants"/>
          (<xsl:value-of select="count(variants/variant)"/>)
        </a>
      </div>
      <div id="variantlist" style="display: none">
        <div style="text-align:center">
          <i18n:text catalogue="skin" key="layout.branch-language"/>
        </div>
        <ul class="navigation">
          <xsl:for-each select="variants/variant">
            <li class="navigation">
              <a href="{@href}">
                <span>
                  <xsl:if test="@current">
                    <xsl:attribute name="style">font-weight: bold</xsl:attribute>
                    <xsl:text>>> </xsl:text>
                  </xsl:if>
                  <xsl:value-of select="concat(@branchName, ' - ', @languageName)"/>
                </span>
              </a>
            </li>
          </xsl:for-each>
        </ul>
        <xsl:if test="createVariant">
          <i><a href="{createVariant/@href}"><i18n:text catalogue="skin" key="layout.add-variant"/></a></i>
        </xsl:if>
      </div>
    </div>
  </xsl:template>

  <xsl:template name="NavigationDocActions">
    <xsl:if test="$siteNavigationDocId != '' and not($onlyGuestRole)">
      <xsl:variable name="navDocPerm" select="/page/navigationInfo/d:aclResult/d:permissions"/>

      <xsl:if test="$navDocPerm/d:permission[@type='read' and @action='grant']">
        <span style="font-size:x-small">
          <i18n:text catalogue="skin" key="layout.navigationdoc"/><xsl:text> </xsl:text>

          <a href="{$basePath}/{$siteNavigationDocId}"><i18n:text catalogue="skin" key="layout.navigationdoc-view"/></a>

          <xsl:if test="$navDocPerm/d:permission[@type='write' and @action='grant']">
            <xsl:text> | </xsl:text>
            <xsl:variable name="url">
              <xsl:choose>
                <xsl:when test="$requestMethod = 'GET'">
                  <xsl:value-of select="concat($basePath, '/', $siteNavigationDocId, '/edit?returnTo=', $pageURIEncoded)"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="concat($basePath, '/', $siteNavigationDocId, '/edit')"/>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:variable>
            <xsl:call-template name="generatePostLink">
              <xsl:with-param name="action" select="$url"/>
              <xsl:with-param name="id">editnavdoc</xsl:with-param>
              <xsl:with-param name="label"><i18n:text catalogue="skin" key="layout.navigationdoc-edit"/></xsl:with-param>
            </xsl:call-template>
          </xsl:if>
        </span>
      </xsl:if>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
