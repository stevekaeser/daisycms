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
  xmlns:urlencoder="xalan://java.net.URLEncoder"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:param name="realPath"/>

  <xsl:variable name="user" select="page/context/user"/>
  <xsl:variable name="mountPoint" select="page/context/mountPoint"/>
  <xsl:variable name="httpMethod" select="page/context/request/@method"/>
  <xsl:variable name="pageURI" select="page/context/request/@uri"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <content><xsl:apply-templates select="error"/></content>
    </page>
  </xsl:template>

  <xsl:template match="error">

    <h1><i18n:text key="error.title"/></h1>
    
    <xsl:choose>
      <!-- special treatment for accessexceptions: let user change login and return to original page. -->
      <xsl:when test="exceptionChain/throwable[2]/@class = 'org.outerj.daisy.repository.DocumentReadDeniedException' and $httpMethod = 'GET'">
        <xsl:value-of select="exceptionChain/throwable[2]/@message"/>

        <p>
          <i18n:translate>
            <i18n:text key="error.login-info"/>
            <i18n:param><xsl:value-of select="$user/name"/></i18n:param>
            <i18n:param>
              <xsl:call-template name="roleList">
                <xsl:with-param name="roles" select="$user/activeRoles"/>
              </xsl:call-template>
            </i18n:param>
          </i18n:translate>
        </p>

        <p><a href="{$mountPoint}/login?returnTo={urlencoder:encode($pageURI, 'UTF-8')}"><i18n:text key="error.change-login"/></a></p>
      </xsl:when>
      <xsl:otherwise>
        <xsl:for-each select="exceptionChain/throwable">
          <!-- The xsl:if test is for hiding the additional exception Cocoon wraps around exceptions that happen
               in flow code (or actions when in a subsitemap) -->
          <xsl:if test="not(@class='org.outerj.daisy.publisher.GlobalPublisherException' or starts-with(@message, 'Sitemap: error calling function') or starts-with(@message, 'Sitemap: error when calling sub-sitemap'))">
            <xsl:value-of select="@message"/><br/>
          </xsl:if>
        </xsl:for-each>
        <br/>
        <br/>
        <xsl:call-template name="exceptionDetails"/>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>

  <xsl:template name="roleList">
    <xsl:param name="roles"/>
    <xsl:for-each select="$roles/role">
      <xsl:if test="position() != 1">, </xsl:if>
      <xsl:value-of select="@name"/>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="exceptionDetails">
    <script type="text/javascript">
      function showDetails() {
        document.getElementById('detailsHandle').style.display = 'none';
        document.getElementById('details').style.display = 'block';
      }
      function hideDetails() {
        document.getElementById('detailsHandle').style.display = 'block';
        document.getElementById('details').style.display = 'none';
      }
    </script>
    <div id="detailsHandle">
      <a href="#" onclick="showDetails(); return false;"><i18n:text key="error.show-details"/></a>
    </div>
    <div id="details" style="display: none;">
      <a href="#" onclick="hideDetails(); return false;"><i18n:text key="error.hide-details"/></a>
      <br/>
      <xsl:if test="publisherStackTrace/locations">
        <h2>Publisher Stack Trace</h2>
        <xsl:apply-templates select="publisherStackTrace"/>
      </xsl:if>

      <xsl:if test="cocoonStackTrace/exception">
        <h2>Cocoon Stack Trace</h2>
        <xsl:apply-templates select="cocoonStackTrace"/>        
      </xsl:if>

      <h2>Java Stack Trace</h2>
      <xsl:apply-templates select="exceptionChain"/>
    </div>
  </xsl:template>

  <xsl:template match="exceptionChain">
    <xsl:apply-templates select="throwable"/>
    <br/>
  </xsl:template>

  <xsl:template match="throwable">
    <div style="margin-left: 2em; margin-top: 1em;">
      <strong>Message: </strong><xsl:value-of select="@message"/><br/>
      <strong>Class: </strong><xsl:value-of select="@class"/><br/>
      <strong>Stacktrace:</strong><br/>
      <div style="margin-left: 2em;">
        <xsl:apply-templates select="stackTrace"/>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="stackTrace">
    <xsl:if test="@remote='true'">
      <strong>ATTENTION:</strong> this is the stacktrace of an exception that happened on the repository server.<br/>
    </xsl:if>
    <xsl:apply-templates select="stackTraceElement"/>
  </xsl:template>

  <xsl:template match="stackTraceElement">
    <xsl:value-of select="@className"/>.<i><xsl:value-of select="@methodName"/></i>
    <xsl:if test="@nativeMethod">
      [native method]
    </xsl:if>
    <xsl:text> </xsl:text>
    <span style="color: gray;">(<xsl:value-of select="@fileName"/>:<xsl:value-of select="@lineNumber"/>)</span><br/>
  </xsl:template>

  <!--
     The styling for the Cocoon stack trace is based on that from Cocoon's
     default exception2html.xslt
  -->
  <xsl:template match="cocoonStackTrace">
    <xsl:for-each select="exception">
      <xsl:sort select="position()" order="descending"/>
      <strong>Message: </strong><xsl:value-of select="message"/>
      <table class="plainTable" style="margin-left: 2em; margin-bottom: 1em; color: gray;">
         <xsl:for-each select="locations/*[string(.) != '[cause location]']">
           <!-- [cause location] indicates location of a cause, which
                the exception generator outputs separately -->
          <tr>
             <td><xsl:call-template name="printLocation"/></td>
             <td><em><xsl:value-of select="."/></em></td>
          </tr>
        </xsl:for-each>
      </table>
      <br/>
     </xsl:for-each>
  </xsl:template>

  <xsl:template name="printLocation">
     <xsl:choose>
       <xsl:when test="contains(@uri, $realPath)">
         <xsl:text>context:/</xsl:text>
         <xsl:value-of select="substring-after(@uri, $realPath)"/>
       </xsl:when>
       <xsl:otherwise>
         <xsl:value-of select="@uri"/>
       </xsl:otherwise>
      </xsl:choose>
      <xsl:text> - </xsl:text>
      <xsl:value-of select="@line"/>:<xsl:value-of select="@column"/>
  </xsl:template>

  <xsl:template match="publisherStackTrace">
    <xsl:for-each select="locations/location">
      <span style="color: gray;"><xsl:value-of select="."/></span><br/>
     </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>