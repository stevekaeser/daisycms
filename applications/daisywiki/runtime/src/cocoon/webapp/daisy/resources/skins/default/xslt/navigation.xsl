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
   | Usage note: this XSL is meant to be imported into other XSLS.
   | It assumes a variable called 'basePath' is defined which serves
   | as starting point for building the URLs, this basePath should
   | not end on a slash.
   -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:n="http://outerx.org/daisy/1.0#navigation"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1">
  <xsl:template match="n:navigationTree[n:navigationTreeError]">
    (An error occured while generating the navigation tree)
  </xsl:template>

  <xsl:template match="n:navigationTree">
      <ul class="navigation">
        <xsl:if test="$user/name != 'guest' and @invalidMillis"><li class="navinvalid">
          <img src="{/page/context/mountPoint}/resources/skins/{/page/context/skin}/images/warning.gif"/>
          <i18n:text key="navtree.is.stale"/><xsl:if test="/page/context/request/@method = 'GET'"><xsl:text> </xsl:text><a href="{/page/context/request/@uri}" class="naverror">(<i18n:text key="navtree.refresh"/>)</a></xsl:if>
        </li></xsl:if>
        <xsl:apply-templates>
          <xsl:with-param name="completeTree" select="@completeTree = 'true'"/>
        </xsl:apply-templates>
      </ul>
  </xsl:template>

  <xsl:template match="n:doc">
    <xsl:param name="completeTree"/>
    <xsl:variable name="hasChildren" select="boolean(*)"/>
    <xsl:variable name="hasNavChildren" select="@hasChildren = 'true' or $hasChildren"/>

    <xsl:variable name="class">
      <xsl:text>navigation </xsl:text>
      <xsl:if test="@active = 'true'">
        <xsl:text>active-navnode</xsl:text>
      </xsl:if>
      <xsl:if test="not($completeTree) and $hasNavChildren">
        <xsl:choose>
          <xsl:when test="$hasChildren">
            <xsl:text> navnode-open</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text> navnode-closed</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:if>
    </xsl:variable>

    <li class="{$class}">
      <xsl:variable name="href">
        <xsl:value-of select="concat($basePath, @path, '.html')"/>
        <xsl:choose>
          <xsl:when test="@branch and @language">
            <xsl:value-of select="concat('?branch=', @branch, '&amp;language=', @language)"/>
          </xsl:when>
          <xsl:when test="@language">
            <xsl:value-of select="concat('?language=', @language)"/>
          </xsl:when>
          <xsl:when test="@branch">
            <xsl:value-of select="concat('?branch=', @branch)"/>
          </xsl:when>
        </xsl:choose>
      </xsl:variable>
      <a  class="{$class}" href="{$href}">
        <xsl:if test="not($completeTree) and $hasNavChildren and @active = 'true'">
          <xsl:attribute name="onclick">return collapseExpandNavNode(this);</xsl:attribute>
        </xsl:if>
        <xsl:value-of select="@label"/>
      </a>
      <xsl:if test="count(*) > 0">
        <ul>
          <xsl:apply-templates>
            <xsl:with-param name="completeTree" select="$completeTree"/>
          </xsl:apply-templates>
        </ul>
      </xsl:if>
    </li>
  </xsl:template>

  <xsl:template match="n:group">
    <xsl:param name="completeTree"/>
    <!-- The different logic here for group nodes in contrast with document nodes
         is because of group nodes we know they always have children, so there
         is no @hasChildren -->
    <xsl:variable name="hasChildren" select="boolean(*)"/>

    <xsl:variable name="class">
      <xsl:text>navigation </xsl:text>
      <xsl:choose>
        <xsl:when test="$completeTree">
          <xsl:text>navgroup</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>navgroup-clickable</xsl:text>
          <xsl:choose>
            <xsl:when test="$hasChildren">
              <xsl:text> navnode-open</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text> navnode-closed</xsl:text>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

   <li class="{$class}">
     <a class="{$class}">
       <xsl:choose>
         <xsl:when test="$completeTree">
           <xsl:attribute name="href">#</xsl:attribute>
           <xsl:attribute name="onclick">return false;</xsl:attribute>
           <xsl:attribute name="onmouseover">status=''; return true;</xsl:attribute>
         </xsl:when>
         <xsl:otherwise>
           <xsl:attribute name="href"><xsl:value-of select="concat($basePath, @path)"/></xsl:attribute>
           <xsl:if test="$hasChildren">
             <xsl:attribute name="onclick">return collapseExpandNavNode(this);</xsl:attribute>
           </xsl:if>
         </xsl:otherwise>
       </xsl:choose>
       <xsl:value-of select="@label"/>
     </a>
     <xsl:if test="count(*) > 0">
       <ul>
         <xsl:apply-templates>
           <xsl:with-param name="completeTree" select="$completeTree"/>
         </xsl:apply-templates>
       </ul>
     </xsl:if>
   </li>
  </xsl:template>

  <xsl:template match="n:link">
    <xsl:param name="completeTree"/>
    <xsl:variable name="class">
      <xsl:if test="@active = 'true'">
        <xsl:text>active-navnode</xsl:text>
      </xsl:if>
      <xsl:text> navigation</xsl:text>
    </xsl:variable>

   <li class="{$class}">
     <a class="{$class}" href="{@url}"><xsl:value-of select="@label"/></a>
     <xsl:if test="count(*) > 0">
       <ul>
         <xsl:apply-templates>
           <xsl:with-param name="completeTree" select="$completeTree"/>
         </xsl:apply-templates>
       </ul>
     </xsl:if>
   </li>
  </xsl:template>

  <xsl:template match="n:error">
    <li>
      <a href="#" onmouseover="status=''; return true;" class="naverror"><xsl:value-of select="@message"/></a>
    </li>
  </xsl:template>

  <xsl:template match="n:separator">
    <li class="navigation">
      <div class="navseparator">&#160;</div>
    </li>
  </xsl:template>

</xsl:stylesheet>
