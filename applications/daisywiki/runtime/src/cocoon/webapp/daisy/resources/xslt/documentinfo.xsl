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
  xmlns:fi="http://apache.org/cocoon/forms/1.0#instance"
  xmlns:daisyutil="xalan://org.outerj.daisy.frontend.util.XslUtil"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:p="http://outerx.org/daisy/1.0#publisher"
  xmlns:d="http://outerx.org/daisy/1.0">

  <xsl:variable name="pubDoc" select="/page/p:publisherResponse/p:document"/>
  <xsl:variable name="doc" select="/page/p:publisherResponse/p:document/d:document"/>

  <xsl:template match="page">
    <documentInfo result="{result}" id="{$pubDoc/@documentId}"
                  branchId="{$pubDoc/@branchId}" branch="{$pubDoc/@branch}"
                  languageId="{$pubDoc/@languageId}" language="{$pubDoc/@language}">

      <resultLabel><i18n:text key="documentinfo.result.{result}"/></resultLabel>
      <name><xsl:value-of select="$doc/@name"/></name>
      <documentType><xsl:value-of select="$doc/@typeLabel"/></documentType>
      <summary><xsl:value-of select="$doc/d:summary"/></summary>

      <xsl:apply-templates select="$pubDoc/d:versions"/>

      <xsl:apply-templates select="$pubDoc/p:ids"/>
    </documentInfo>
  </xsl:template>

  <xsl:template match="d:versions">
    <versions>
      <xsl:apply-templates select="d:version"/>
    </versions>
  </xsl:template>

  <xsl:template match="d:version">
    <version id="{@id}" created="{@createdFormatted}" state="{@state}"/>
  </xsl:template>

  <xsl:template match="p:ids">
    <ids>
      <xsl:apply-templates select="p:id"/>
    </ids>
  </xsl:template>

  <xsl:template match="p:id">
    <id>
      <xsl:value-of select="."/>
    </id>
  </xsl:template>
</xsl:stylesheet>