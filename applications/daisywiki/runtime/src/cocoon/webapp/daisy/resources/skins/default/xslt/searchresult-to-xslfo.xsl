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
  xmlns:d="http://outerx.org/daisy/1.0"
  xmlns:p="http://outerx.org/daisy/1.0#publisher"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  version="1.0">

  <!-- Include custom query result stylings -->
  <xsl:include href="daisyskin:query-styling/query-styling-xslfo.xsl"/>

  <xsl:include href="daisyskin:xslt/xslfo-styles.xsl"/>
  
  <xsl:template match="data">
    <fo:root>
      <fo:layout-master-set>
        <fo:simple-page-master master-name="simplepage"
          margin-top="1cm"
          margin-bottom="1cm"
          margin-left="2cm"
          margin-right="2cm">
          <fo:region-body region-name="body"
            margin-bottom="2.5cm"/>
          <fo:region-after region-name="bottom" extent="1.5cm"/>
        </fo:simple-page-master>
      </fo:layout-master-set>

      <fo:page-sequence master-reference="simplepage">
        <fo:static-content flow-name="xsl-footnote-separator">
          <fo:block margin="0cm" padding="0cm" start-indent="0cm" end-indent="0cm">
            <fo:leader leader-pattern="rule"
                       leader-length="100%"
                       rule-style="solid"
                       rule-thickness="0.1mm"/>
          </fo:block>
        </fo:static-content>
        <fo:static-content flow-name="bottom">
          <fo:block>TODO: footer</fo:block>
          <!--
          <xsl:call-template name="footer"/>
          -->
        </fo:static-content>
        <fo:flow flow-name="body">
          <fo:block>
            <xsl:apply-templates select="d:searchResult"/>
          </fo:block>
        </fo:flow>
      </fo:page-sequence>
    </fo:root>
  </xsl:template>
</xsl:stylesheet>
