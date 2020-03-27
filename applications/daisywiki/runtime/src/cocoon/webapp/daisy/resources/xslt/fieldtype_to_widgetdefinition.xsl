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
<!--
   Generates a piece of CForms form definition for a field.
   This is part of the DefaultFieldEditor.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:d="http://outerx.org/daisy/1.0"
  xmlns:ft="http://apache.org/cocoon/forms/1.0#template"
  xmlns:fi="http://apache.org/cocoon/forms/1.0#instance"
  xmlns:fd="http://apache.org/cocoon/forms/1.0#definition"
  xmlns:jx="http://apache.org/cocoon/templates/jx/1.0"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:template match="d:fieldTypeUse">
    <xsl:apply-templates select="d:fieldType"/>
  </xsl:template>

  <xsl:template match="d:fieldType[@multiValue='true']">
    <fd:multivaluefield id="field">
      <xsl:call-template name="fieldConfig"/>
    </fd:multivaluefield>
  </xsl:template>

  <!-- Special handling for non-multivalue boolean fields -->
  <xsl:template match="d:fieldType[@multiValue!='true' and @valueType='boolean']">
    <fd:booleanfield id="field">
    </fd:booleanfield>
  </xsl:template>

  <xsl:template match="d:fieldType">
    <fd:field id="field">
      <xsl:call-template name="fieldConfig"/>
      <xsl:if test="@valueType = 'string' and @multiValue != 'true' and @hierarchical != 'true'">
        <fd:validation>
          <fd:length max="255"/>
        </fd:validation>
      </xsl:if>
    </fd:field>

    <xsl:if test="@valueType = 'link' or (@loadSelectionListAsync = 'true' and d:selectionList)">
      <fd:output id="field-label">
        <fd:datatype base="string"/>
      </fd:output>
    </xsl:if>
  </xsl:template>

  <xsl:template name="fieldConfig">
    <xsl:variable name="valueType" select="string(@valueType)"/>
    <fd:datatype>
      <xsl:attribute name="base">
        <xsl:choose>
          <xsl:when test="$valueType = 'datetime'">date</xsl:when>
          <xsl:when test="$valueType = 'link'">string</xsl:when>
          <xsl:otherwise><xsl:value-of select="$valueType"/></xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>

      <xsl:if test="$valueType = 'datetime'">
        <fd:convertor type="formatting" variant="datetime" style="short" timeStyle="medium" lenient="false"/>
      </xsl:if>

    </fd:datatype>
  </xsl:template>

</xsl:stylesheet>