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

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><i18n:text key="docbaset-nav.title"/></pageTitle>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1><i18n:text key="docbaset-nav.title"/></h1>

    <p><a href="../documentBasket"><i18n:text key="docbasket-nav.return-to-docbasket"/></a></p>

    <form method="POST">
      <input type="submit" value="docbasket-nav.add-selected-docs" i18n:attr="value"/>

      <xsl:apply-templates select="n:navigationTree"/>

      <input type="submit" value="docbasket-nav.add-selected-docs" i18n:attr="value"/>
    </form>
  </xsl:template>

  <xsl:template match="n:navigationTree">
    <ul class="nav2docbasket">
      <xsl:apply-templates/>
    </ul>
  </xsl:template>

  <xsl:template match="n:doc">
    <li>
      <input type="checkbox" name="document" value="{@documentId}.{@branchId}.{@languageId}"/><xsl:value-of select="@label"/>
      <xsl:if test="count(*) > 0">
        <ul class="nav2docbasket">
          <xsl:apply-templates/>
        </ul>
      </xsl:if>
    </li>
  </xsl:template>

  <xsl:template match="n:link|n:group">
    <li>
      <span style="visibility: hidden"><input type="checkbox" disabled="disabled"/></span><xsl:value-of select="@label"/>
      <xsl:if test="count(*) > 0">
        <ul class="nav2docbasket">
          <xsl:apply-templates/>
        </ul>
      </xsl:if>
    </li>
  </xsl:template>

</xsl:stylesheet>