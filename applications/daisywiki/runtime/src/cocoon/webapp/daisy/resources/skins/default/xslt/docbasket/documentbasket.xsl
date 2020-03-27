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
  xmlns:p="http://outerx.org/daisy/1.0#publisher"
  xmlns:n="http://outerx.org/daisy/1.0#navigation"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:import href="daisyskin:xslt/util.xsl"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><i18n:text key="docbasket.title"/></pageTitle>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1><i18n:text key="docbasket.title"/></h1>

    <xsl:if test="basketMessage">
      <div id="pageMessage">
        <xsl:copy-of select="basketMessage/node()"/>
        <div style="text-align: right; font-size: x-small"><a href="#" onclick="this.parentNode.parentNode.style.display='none'; return false;" onmouseover="status='';return true;"><i18n:text catalogue="skin" key="layout.hide"/></a></div>
      </div>
    </xsl:if>

    <xsl:choose>
      <xsl:when test="documentBasket/@size = '0'">
        <p><i18n:text key="docbasket.empty"/></p>
        <xsl:call-template name="basketAddActions"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="basketActions"/>
        <xsl:call-template name="basketAddActions"/>
        <xsl:apply-templates select="documentBasket"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="documentBasket">
    <h2><i18n:text key="docbasket.content-title"/></h2>

    <script>
      function submitForm(action) {
          var form = document.forms['basket'];
          form.basketAction.value = action;
          form.submit();
      }

      function selectEntries(state) {
          var form = document.forms['basket'];
          var i = 1;
          var entry;
          while ((entry = form["entry." + i]) != null) {
              entry.checked = state;
              i++;
          }
      }
    </script>

    <xsl:choose>
      <xsl:when test="@size = 1">
        <p><i18n:text key="docbasket.contains-one"/></p>
      </xsl:when>
      <xsl:otherwise>
        <p>
         <i18n:translate>
           <i18n:text key="docbasket.contains-documents"/>
           <i18n:param><xsl:value-of select="@size"/></i18n:param>
         </i18n:translate>
        </p>
      </xsl:otherwise>
    </xsl:choose>

    <p><a class="docbasketAction" href="#" onclick="window.location.assign('documentBasket'); return false;"  onmouseover="status=''; return true;"><i18n:text key="docbasket.refresh"/></a></p>

    <form name="basket" method="POST">
      <input type="hidden" name="basketUpdateCount" value="{@updateCount}"/>
      <input type="hidden" name="basketAction"/>

      <xsl:call-template name="basketOperations"/>

      <table class="default">
        <tr>
          <th/>
          <th><i18n:text key="docbasket.doc-name"/></th>
          <th>ID</th>
          <th><i18n:text key="branch"/></th>
          <th><i18n:text key="language"/></th>
          <!-- th>Version</th -->
        </tr>
        <xsl:apply-templates select="entry"/>
      </table>

      <xsl:call-template name="basketOperations"/>
    </form>
  </xsl:template>

  <xsl:template match="entry">
    <tr>
      <td><input type="checkbox" name="entry.{position()}" value="true"/></td>
      <td><xsl:value-of select="@name"/></td>
      <td><xsl:value-of select="@id"/></td>
      <td><xsl:value-of select="@branch"/></td>
      <td><xsl:value-of select="@language"/></td>
      <!-- td><xsl:value-of select="@version"/></td -->
    </tr>
  </xsl:template>

  <xsl:template name="basketOperations">
    <a class="docbasketAction" href="#" onmouseover="status=''; return true;" onclick="selectEntries(true); return false;">
      <i18n:text key="docbasket.op-select-all"/>
    </a>
    <xsl:text> | </xsl:text>
    <a class="docbasketAction" href="#" onmouseover="status=''; return true;" onclick="selectEntries(false); return false;">
      <i18n:text key="docbasket.op-unselect-all"/>
    </a>
    <xsl:text> | </xsl:text>
    <a class="docbasketAction" href="#" onmouseover="status=''; return true;" onclick="submitForm('removeSelected');">
      <i18n:text key="docbasket.op-remove-selected"/>
    </a>
    <xsl:text> | </xsl:text>
    <a class="docbasketAction" href="#" onmouseover="status=''; return true;" onclick="submitForm('clear');">
      <i18n:text key="docbasket.rop-remove-all"/>
    </a>
  </xsl:template>

  <xsl:template name="basketActions">
    <h2><i18n:text key="docbasket.actions-title"/></h2>
    <ul>
      <li><a class="docbasketAction" href="documentBasket/aggregate.html"><i18n:text key="docbasket.action-aggr-html"/></a></li>
      <li><a class="docbasketAction" href="documentBasket/aggregate.html?layoutType=plain"><i18n:text key="docbasket.action-aggr-html-plain"/></a></li>
      <li><a class="docbasketAction" href="documentBasket/aggregate.pdf"><i18n:text key="docbasket.action-aggr-pdf"/></a></li>
      <li>
        <xsl:call-template name="generatePostLink">
          <xsl:with-param name="action">../doctask/new?initFromDocumentBasket=true</xsl:with-param>
          <xsl:with-param name="id">doctask</xsl:with-param>
          <xsl:with-param name="label"><i18n:text key="docbasket.action-start-doctask"/></xsl:with-param>
          <xsl:with-param name="class">docbasketAction</xsl:with-param>
        </xsl:call-template>
      </li>
      <li>
        <xsl:call-template name="generatePostLink">
          <xsl:with-param name="action">searchAndReplace?useBasket=true</xsl:with-param>
          <xsl:with-param name="id">search-and-replace</xsl:with-param>
          <xsl:with-param name="label"><i18n:text key="docbasket.action-start-search-and-replace"/></xsl:with-param>
          <xsl:with-param name="class">docbasketAction</xsl:with-param>
        </xsl:call-template>
      </li>
    </ul>
  </xsl:template>

  <xsl:template name="basketAddActions">
    <h2><i18n:text key="docbasket.adding-title"/></h2>
    <ul>
      <li><i18n:text key="docbasket.adding-manual"/></li>
      <li><a class="docbasketAction" href="search"><i18n:text key="docbasket.adding-with-search"/></a></li>
      <li><a class="docbasketAction" href="querySearch"><i18n:text key="docbasket.adding-with-query"/></a></li>
      <li><a class="docbasketAction" href="documentBasket/selectFromNavigation"><i18n:text key="docbasket.adding-from-nav"/></a></li>
    </ul>
  </xsl:template>

</xsl:stylesheet>