<?xml version="1.0" encoding="UTF-8"?>
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
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:fo="http://www.w3.org/1999/XSL/Format"
  version="1.0">

  <xsl:variable name="colorMediumGray">#c9c9c9</xsl:variable>
  <xsl:variable name="colorLightGray">#dcdcdc</xsl:variable>
  <xsl:variable name="colorDarkGray">#454545</xsl:variable>
  <xsl:variable name="fontSansSerif">Helvetica,sans-serif</xsl:variable>
  <xsl:variable name="fontSerif">serif</xsl:variable>

  <!-- Some of the styles below are based on the maven XSL's. -->
  <xsl:attribute-set name="base.body.style">
      <xsl:attribute name="font-family"><xsl:value-of select="$fontSerif"/></xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="base.heading.style">
      <xsl:attribute name="font-family"><xsl:value-of select="$fontSansSerif"/></xsl:attribute>
      <xsl:attribute name="color">#000000</xsl:attribute>
      <xsl:attribute name="keep-with-next">always</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="title" use-attribute-sets="base.heading.style">
      <xsl:attribute name="font-size">16pt</xsl:attribute>
      <xsl:attribute name="font-weight">bold</xsl:attribute>
      <xsl:attribute name="space-before">18pt</xsl:attribute>
      <xsl:attribute name="space-after">3pt</xsl:attribute>
      <xsl:attribute name="padding-after">3pt</xsl:attribute>
      <xsl:attribute name="border-bottom-width">.1mm</xsl:attribute>
      <xsl:attribute name="border-bottom-style">solid</xsl:attribute>
      <xsl:attribute name="border-bottom-color">black</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="h1" use-attribute-sets="base.heading.style">
      <xsl:attribute name="font-size">16pt</xsl:attribute>
      <xsl:attribute name="font-weight">bold</xsl:attribute>
      <xsl:attribute name="space-before">18pt</xsl:attribute>
      <xsl:attribute name="space-after">6pt</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="h2" use-attribute-sets="base.heading.style">
    <xsl:attribute name="font-size">13pt</xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
    <xsl:attribute name="space-before">18pt</xsl:attribute>
    <xsl:attribute name="space-after">5pt</xsl:attribute>
    <xsl:attribute name="keep-with-next">always</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="h3" use-attribute-sets="base.heading.style">
    <xsl:attribute name="font-size">13pt</xsl:attribute>
    <xsl:attribute name="space-before">15pt</xsl:attribute>
    <xsl:attribute name="space-after">3pt</xsl:attribute>
    <xsl:attribute name="keep-with-next">always</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="h4" use-attribute-sets="base.heading.style">
    <xsl:attribute name="font-size">12pt</xsl:attribute>
    <xsl:attribute name="space-before">9pt</xsl:attribute>
    <xsl:attribute name="space-after">3pt</xsl:attribute>
    <xsl:attribute name="keep-with-next">always</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="h5" use-attribute-sets="base.heading.style">
    <xsl:attribute name="font-size">11pt</xsl:attribute>
    <xsl:attribute name="space-before">9pt</xsl:attribute>
    <xsl:attribute name="space-after">3pt</xsl:attribute>
    <xsl:attribute name="keep-with-next">always</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="h6" use-attribute-sets="base.heading.style">
    <xsl:attribute name="font-size">10pt</xsl:attribute>
    <xsl:attribute name="space-before">9pt</xsl:attribute>
    <xsl:attribute name="space-after">3pt</xsl:attribute>
    <xsl:attribute name="keep-with-next">always</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="body.text" use-attribute-sets="base.body.style">
      <xsl:attribute name="font-size">11pt</xsl:attribute>
      <xsl:attribute name="white-space-collapse">true</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="p" use-attribute-sets="body.text">
      <xsl:attribute name="space-before">3pt</xsl:attribute>
      <xsl:attribute name="space-after">6pt</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="blockquote" use-attribute-sets="body.text">
    <xsl:attribute name="start-indent">0.5cm + inherited-property-value(start-indent)</xsl:attribute>
    <xsl:attribute name="end-indent">0.5cm + inherited-property-value(start-indent)</xsl:attribute>
    <xsl:attribute name="space-before">3pt</xsl:attribute>
    <xsl:attribute name="space-after">6pt</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="pre">
    <xsl:attribute name="font-family">monospace</xsl:attribute>
    <xsl:attribute name="font-size">8pt</xsl:attribute>
    <xsl:attribute name="wrap-option">wrap</xsl:attribute>
    <xsl:attribute name="white-space-collapse">false</xsl:attribute>
    <xsl:attribute name="linefeed-treatment">preserve</xsl:attribute>
    <xsl:attribute name="white-space-treatment">preserve</xsl:attribute>
    <xsl:attribute name="color">black</xsl:attribute>
    <xsl:attribute name="border-style">solid</xsl:attribute>
    <xsl:attribute name="border-width">0.1mm</xsl:attribute>
    <xsl:attribute name="border-color"><xsl:value-of select="$colorMediumGray"/></xsl:attribute>
    <xsl:attribute name="padding-before">2mm</xsl:attribute>
    <xsl:attribute name="padding-after">2mm</xsl:attribute>
    <xsl:attribute name="padding-start">2mm</xsl:attribute>
    <xsl:attribute name="padding-end">2mm</xsl:attribute>
    <xsl:attribute name="start-indent">inherited-property-value(start-indent) + 2.5em</xsl:attribute>
    <xsl:attribute name="end-indent">inherited-property-value(end-indent) + 3em</xsl:attribute>
    <xsl:attribute name="space-before">0.75em</xsl:attribute>
    <xsl:attribute name="space-after">1em</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="list">
      <xsl:attribute name="start-indent">inherited-property-value(start-indent)</xsl:attribute>
      <xsl:attribute name="provisional-distance-between-starts">1.5em</xsl:attribute>
      <xsl:attribute name="provisional-label-separation">1em</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="list.item">
      <xsl:attribute name="start-indent">inherited-property-value(start-indent) + .5em</xsl:attribute>
      <xsl:attribute name="space-before">0.15em</xsl:attribute>
      <xsl:attribute name="space-after">0.25em</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="table">
    <xsl:attribute name="space-before">3pt</xsl:attribute>
    <xsl:attribute name="space-after">6pt</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="td">
    <xsl:attribute name="border-style">solid</xsl:attribute>
    <xsl:attribute name="border-width">.1mm</xsl:attribute>
    <xsl:attribute name="padding">1mm</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="th">
    <xsl:attribute name="background-color"><xsl:value-of select="$colorLightGray"/></xsl:attribute>
    <xsl:attribute name="border-style">solid</xsl:attribute>
    <xsl:attribute name="border-width">.1mm</xsl:attribute>
    <xsl:attribute name="padding">1mm</xsl:attribute>
    <xsl:attribute name="text-align">center</xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="td.borderless">
    <xsl:attribute name="padding">1mm</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="th.borderless">
    <xsl:attribute name="background-color"><xsl:value-of select="$colorLightGray"/></xsl:attribute>
    <xsl:attribute name="padding">1mm</xsl:attribute>
    <xsl:attribute name="text-align">center</xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="p.title">
      <xsl:attribute name="letter-spacing">2pt</xsl:attribute>
      <xsl:attribute name="font-family"><xsl:value-of select="$fontSansSerif"/></xsl:attribute>
      <xsl:attribute name="font-size">6pt</xsl:attribute>
      <xsl:attribute name="color"><xsl:value-of select="$colorDarkGray"/></xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="img.caption">
    <xsl:attribute name="font-family"><xsl:value-of select="$fontSerif"/></xsl:attribute>
    <xsl:attribute name="font-size">9pt</xsl:attribute>
    <xsl:attribute name="font-weight">normal</xsl:attribute>
    <xsl:attribute name="text-align">center</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="table.caption">
    <xsl:attribute name="font-family"><xsl:value-of select="$fontSerif"/></xsl:attribute>
    <xsl:attribute name="font-size">9pt</xsl:attribute>
    <xsl:attribute name="font-weight">normal</xsl:attribute>
    <xsl:attribute name="text-align">center</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="footnote.ref">
    <xsl:attribute name="font-size">7pt</xsl:attribute>
    <xsl:attribute name="vertical-align">super</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="footnote.text">
    <xsl:attribute name="font-family"><xsl:value-of select="$fontSerif"/></xsl:attribute>
    <xsl:attribute name="font-size">9pt</xsl:attribute>
    <xsl:attribute name="font-weight">normal</xsl:attribute>
    <xsl:attribute name="font-style">normal</xsl:attribute>
    <xsl:attribute name="text-align">left</xsl:attribute>
    <xsl:attribute name="color">#000000</xsl:attribute>
    <xsl:attribute name="text-decoration">no-underline no-overline no-line-through</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="unresolved-variable">
    <xsl:attribute name="background-color">red</xsl:attribute>
    <xsl:attribute name="color">white</xsl:attribute>
  </xsl:attribute-set>

</xsl:stylesheet>
