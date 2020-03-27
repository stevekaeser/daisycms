<?xml version="1.0"?>
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
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:daisyutil="xalan://org.outerj.daisy.frontend.util.XslUtil"
  xmlns:fi="http://apache.org/cocoon/forms/1.0#instance">

  <xsl:template match="page">
    <xsl:apply-templates select="selectionList/fi:selection-list"/>
  </xsl:template>

  <xsl:template match="fi:selection-list">
    <options>
      <xsl:apply-templates select="fi:item"/>
    </options>
  </xsl:template>

  <xsl:template match="fi:item">
    <option value="{@value}" label="{fi:label}" hierarchicalLabel="{@hierarchicalLabel}"/>
  </xsl:template>
</xsl:stylesheet>