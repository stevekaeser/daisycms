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
<xsl:stylesheet version="1.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:template name="generatePostLink">
    <xsl:param name="action"/>
    <xsl:param name="label"/>
    <xsl:param name="id"/>
    <xsl:param name="confirmMessage"/>
    <xsl:param name="class"/>
    <xsl:param name="title"/>
    <xsl:param name="i18n-attrs"/>

    <xsl:if test="boolean($confirmMessage)">
      <span id="{$id}-confirm-message" style="display: none"><xsl:copy-of select="$confirmMessage"/></span>
    </xsl:if>

    <xsl:variable name="script">
      <xsl:choose>
        <xsl:when test="boolean($confirmMessage)">if (confirm(daisyElementText(document.getElementById("<xsl:value-of select="$id"/>-confirm-message")))) { document.getElementById('<xsl:value-of select="$id"/>').submit(); } return false;</xsl:when>
        <xsl:otherwise>document.getElementById('<xsl:value-of select="$id"/>').submit(); return false;</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <form id="{$id}" action="{$action}" method="POST" style="display: none">
    </form>
    <a class="{$class}" href="#" onclick="{$script}" onmouseover="status=''; return true;">
      <xsl:if test="$class != ''">
        <xsl:attribute name="class"><xsl:value-of select="$class"/></xsl:attribute>
      </xsl:if>
      <xsl:if test="$title != ''">
        <xsl:attribute name="title"><xsl:value-of select="$title"/></xsl:attribute>
      </xsl:if>
      <xsl:if test="$i18n-attrs != ''">
        <xsl:attribute name="i18n:attr"><xsl:value-of select="$i18n-attrs"/></xsl:attribute>
      </xsl:if>
      <xsl:copy-of select="$label"/>
    </a>
  </xsl:template>

</xsl:stylesheet>
