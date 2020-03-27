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

  <xsl:import href="daisyskin:xslt/pagevars.xsl"/>
  <xsl:import href="daisyskin:xslt/util.xsl"/>

  <xsl:variable name="skin" select="page/context/skin"/>
  
  <xsl:variable name="pubdoc" select="/page/p:publisherResponse/p:document"/>
  <xsl:variable name="document" select="$pubdoc/d:document"/>
  <xsl:variable name="version" select="$pubdoc/d:version"/>
  <xsl:variable name="layoutType" select="page/context/layoutType"/>
  <xsl:variable name="activePath" select="page/activePath"/>
  
  <xsl:template match="page">
    <page>
      <xsl:copy-of select="context"/>
      <pageTitle><i18n:text key="docinfo.pageTitle"/></pageTitle>
      <content><xsl:call-template name="content"/></content>
    </page>
  </xsl:template>

  <xsl:template name="content">
    <table class="plainTable" width="100%">
      <col width="5%"/> <!-- small width here, see div in first left cell -->
      <col width="95%"/>
      <tbody>
        <tr>
          <td colspan="2"><h2><i18n:text key="docinfo.title"/></h2></td>
        </tr>
        <tr>
          <td>
            <!-- force a minimum width for the left column, for the case values in
                 the right column get big (such as the version comment) -->
            <div style="width: 15em;"><i18n:text key="docinfo.name"/>:</div>
          </td>
          <td><xsl:value-of select="$document/@name"/></td>
        </tr>
        <tr>
          <td><i18n:text key="docinfo.id"/>:</td>
          <td><xsl:value-of select="$document/@id"/></td>
        </tr>
        <tr>
          <td><i18n:text key="branch"/>:</td>
          <td><xsl:value-of select="$document/@branch"/></td>
        </tr>
        <tr>
          <td><i18n:text key="language"/>:</td>
          <td><xsl:value-of select="$document/@language"/></td>
        </tr>
        <tr>
          <td><i18n:text key="reference-language"/>:</td>
          <td><xsl:value-of select="$document/@referenceLanguage"/></td>
        </tr>
        <tr>
          <td><i18n:text key="docinfo.id-based-link"/>:</td>
          <td><a id="docIdLink"></a></td>
        </tr>
        <tr>
          <td><i18n:text key="docinfo.navigation-based-link"/>:</td>
          <td><a id="docNavLink"></a></td>
        </tr>
        <tr>
          <td><i18n:text key="docinfo.owner"/>:</td>
          <td><xsl:value-of select="$document/@ownerDisplayName"/></td>
        </tr>
        <tr>
          <td><i18n:text key="docinfo.created"/>:</td>
          <td><xsl:value-of select="$document/@createdFormatted"/></td>
        </tr>
        <tr>
          <td><i18n:text key="docinfo.last-modified"/>:</td>
          <td><xsl:value-of select="$document/@lastModifiedFormatted"/><xsl:text> </xsl:text><i18n:text key="docinfo.by"/><xsl:text> </xsl:text><xsl:value-of select="$document/@lastModifierDisplayName"/></td>
        </tr>
        <tr>
          <td><i18n:text key="docinfo.doctype"/>:</td>
          <td><xsl:value-of select="$document/@typeLabel"/></td>
        </tr>
        <tr>
          <td><i18n:text key="collections"/>:</td>
          <td>
            <xsl:for-each select="$document/d:collectionIds/d:collectionId">
              <xsl:if test="position() != 1">, </xsl:if>
              <xsl:value-of select="@name"/>
            </xsl:for-each>
          </td>
        </tr>
        <tr>
          <td colspan="2" style="padding-top: 1em;"><h2><i18n:text key="docinfo.versioninfo.title"/></h2></td>
        </tr>
        <tr>
          <td><i18n:text key="docinfo.version"/>:</td>
          <td><xsl:value-of select="$document/@dataVersionId"/> (<i18n:text key="docinfo.version-of"/><xsl:text> </xsl:text><xsl:value-of select="$document/@lastVersionId"/>)</td>
        </tr>
       <tr>
          <td><i18n:text key="docinfo.version-state"/>:</td>
          <td><i18n:text key="{$version/@state}"/></td>
        </tr>
        <tr>
          <td><i18n:text key="docinfo.version-synced-with"/>:</td>
          <td>
            <xsl:if test="$version/@syncedWithLanguageId != '-1'">
              <xsl:value-of select="$version/@syncedWithLanguage"/><xsl:text> </xsl:text><i18n:text key="docinfo.version-abbrev"/><xsl:text> </xsl:text><xsl:value-of select="$version/@syncedWithVersionId"/> 
            </xsl:if>
          </td>
        </tr>
        <tr>
          <td><i18n:text key="docinfo.version-change-type"/>:</td>
          <td><i18n:text key="{$version/@changeType}"/></td>
        </tr>
        <tr>
          <td><i18n:text key="docinfo.version-change-comment"/>:</td>
          <td><xsl:value-of select="$version/d:changeComment"/></td>
        </tr>
        <tr>
          <td><i18n:text key="docinfo.id-based-link"/>:</td>
          <td><a id="versionIdLink"></a></td>
        </tr>
        <tr>
          <td><i18n:text key="docinfo.navigation-based-link"/>:</td>
          <td><a id="versionNavLink"></a></td>
        </tr>
        <tr>
          <td><i18n:text key="docinfo.version-created"/>:</td>
          <td><xsl:value-of select="$version/@createdFormatted"/><xsl:text> </xsl:text><i18n:text key="docinfo.by"/><xsl:text> </xsl:text><xsl:value-of select="$version/@creatorDisplayName"/></td>
        </tr>
        <tr>
          <td><i18n:text key="docinfo.version-last-modified"/>:</td>
          <td><xsl:value-of select="$version/@lastModifiedFormatted"/><xsl:text> </xsl:text><i18n:text key="docinfo.by"/><xsl:text> </xsl:text><xsl:value-of select="$version/@lastModifierDisplayName"/></td>
        </tr>
      </tbody>
    </table>

    <br/>
    <a href="#" onclick="window.close();" onmouseover="window.status=''; return true;"><i18n:text key="docinfo.close"/></a>

    <xsl:variable name="idLink" select="concat($mountPoint, '/', $siteName, '/', $document/@id)"/>
    <script type="text/javascript">
      function createLinkNode(anchorId, href) {
        var anchor = document.getElementById(anchorId);
        anchor.appendChild(document.createTextNode(href));
        anchor.href = href;
      }
     
      var loc = window.location;
      var server = loc.protocol + "//" + loc.hostname;
      if (loc.protocol != 'http' || (loc.port != "80" &amp;&amp; loc.port != ""))
        server += ":" + loc.port
      
      createLinkNode('docIdLink', server + "<xsl:value-of select="concat($idLink, '.html', $variantQueryString)"/>");
      createLinkNode('docNavLink', server + "<xsl:value-of select="concat($mountPoint, '/', $siteName, '/', $activePath, '.html', $variantQueryString)"/>"); 

      createLinkNode('versionIdLink', server + "<xsl:value-of select="concat($idLink, '/version/' , $version/@id, $variantQueryString)"/>"); 
      createLinkNode('versionNavLink', server + "<xsl:value-of select="concat($mountPoint, '/', $siteName, '/', $activePath, '/version/', $version/@id, $variantQueryString)"/>"); 

    </script>
  </xsl:template>

</xsl:stylesheet>