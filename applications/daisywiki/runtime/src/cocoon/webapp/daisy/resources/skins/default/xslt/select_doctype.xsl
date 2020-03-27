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

  <xsl:import href="daisyskin:xslt/util.xsl"/>

  <xsl:variable name="siteBranchId" select="string(/page/context/site/@branchId)"/>
  <xsl:variable name="siteLanguageId" select="string(/page/context/site/@languageId)"/>
  <xsl:variable name="goto" select="/page/goto"/>

  <xsl:variable name="selectedBranchId" select="string(/page/selectedBranchId)"/>
  <xsl:variable name="selectedLanguageId" select="string(/page/selectedLanguageId)"/>

  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><i18n:text key="doctype.title"/></pageTitle>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <h1><i18n:text key="doctype.title"/></h1>

    <form action="" method="GET">
      <i18n:text key="doctype.create-on-branch"/>&#160;
      <select name="branch" id="branch" onchange="this.form.submit()">
        <xsl:for-each select="branches/branch">
          <option value="{@name}">
            <xsl:if test="@id = $selectedBranchId"><xsl:attribute name="selected"/></xsl:if>
            <xsl:value-of select="@name"/>
          </option>
        </xsl:for-each>
      </select>
      &#160;<i18n:text key="doctype.create-in-language"/>&#160;
      <select name="language" id="language" onchange="this.form.submit()">
        <xsl:for-each select="languages/language">
          <option value="{@name}">
            <xsl:if test="@id = $selectedLanguageId"><xsl:attribute name="selected"/></xsl:if>
            <xsl:value-of select="@name"/>
          </option>
        </xsl:for-each>
      </select>
    </form>

    <br/>
    <br/>
    <i18n:text key="doctype.select"/>
    <br/>
    <br/>
    <xsl:apply-templates select="documentTypes"/>    

    <form id="CreateDocument" action="{$goto}" method="POST" style="display: none">
      <input type="hidden" name="documentType"/>
      <input type="hidden" name="branch"/>
      <input type="hidden" name="language"/>
    </form>

    <script type="text/javascript">
      function createDocument(doctype) {
        var form = document.forms.CreateDocument;
        form.documentType.value = doctype;
        form.branch.value = document.getElementById("branch").value;
        form.language.value = document.getElementById("language").value;
        form.submit();
      }
    </script>
  </xsl:template>

  <xsl:template match="documentTypes">
    <xsl:choose>
      <xsl:when test="documentType[string(deprecated) = 'false']">
        <xsl:variable name="defaultDocumentTypeId" select="string(../defaultDocumentTypeId)"/>
        <xsl:variable name="defaultDocumentType" select="documentType[id = $defaultDocumentTypeId]"/>
        <xsl:if test="$defaultDocumentType">
          <h2><i18n:text key="doctype.default-doctype"/></h2>
          <xsl:apply-templates select="$defaultDocumentType"/>
          <h2><i18n:text key="doctype.other-doctype"/></h2>
        </xsl:if>
        <xsl:apply-templates select="documentType[id != $defaultDocumentTypeId and string(deprecated) = 'false']">
          <xsl:sort select="label"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise>
        <p><i18n:text key="doctype.non-available"/></p>        
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="documentType">
    <div class="doctypeLabel">
      <a href="#" onclick="createDocument({id}); return false;" onmouseover="status=''; return true;"><xsl:value-of select="label"/></a>
    </div>
    <div class="doctypeDescription"><xsl:value-of select="description"/></div>
  </xsl:template>
</xsl:stylesheet>