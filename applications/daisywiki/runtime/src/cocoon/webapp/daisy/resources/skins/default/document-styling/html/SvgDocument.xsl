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
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:d="http://outerx.org/daisy/1.0">

  <xsl:import href="daisyskin:xslt/document-to-html.xsl"/>

  <xsl:template match="d:document">
    <br/>

    <!-- Note: can't use generate-id() since the doctype XSLs are independent of each other but there results are merged. -->
    <xsl:variable name="svgId" select="translate(concat(@id, @branch, @language), '-', '_')"/>

    <embed src="{$documentBasePath}{@id}/version/{@dataVersionId}/part/SvgData/data/image.svg"
           type="image/svg+xml" id="{$svgId}"
           pluginspage="http://www.adobe.com/svg/viewer/install/">
           
      <noembed>
        <img src="{$documentBasePath}ext/svg/svg-to-png?documentId={@id}&amp;branch={@branch}&amp;language={@language}&amp;version={@dataVersionId}&amp;part=SvgData"/>
      </noembed>
    </embed>
    <!-- Embed seems to work better on IE then object
    <object data="{$documentBasePath}{@id}/version/{@dataVersionId}/part/SvgData/data/image.svg"
           type="image/svg+xml" id="{@id}"
           pluginspage="http://www.adobe.com/svg/viewer/install/">
      <img src="{$documentBasePath}ext/svg/svg-to-png?documentId={@id}&amp;branch={@branch}&amp;language={@language}&amp;version={@dataVersionId}&amp;part=SvgData"/>
    </object>
    -->
    <!-- TODO better to put this function in some shared location -->
    <script>
      function initSvg<xsl:value-of select="$svgId"/>() {
          var svgObject = document.getElementById("<xsl:value-of select="$svgId"/>");

          if (svgObject != null) {
            // The line below is the official way to access the SVG DOM in case the object tag
            // is used. Works in firefox.
            // var svgRoot = svgObject.contentDocument.documentElement;

            // This is how we do it for the embed tag
            var svgDoc;
            try {
              svgDoc = svgObject.getSVGDocument();
            } catch (e) {
              // unsupported, ignore
              return;
            }
            if (svgDoc == null)
              return;

            var svgRoot = svgDoc.documentElement;

            var width = svgRoot.getAttribute("width");
            var height = svgRoot.getAttribute("height");
            if (width != null)
              svgObject.width = width;
            if (height != null)
              svgObject.height = height;
          }
      }

      daisyPushOnLoad(initSvg<xsl:value-of select="$svgId"/>);
    </script>
    <br/>
  </xsl:template>

</xsl:stylesheet>
