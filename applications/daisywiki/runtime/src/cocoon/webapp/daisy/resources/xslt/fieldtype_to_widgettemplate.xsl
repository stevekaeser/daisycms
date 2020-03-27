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
   Generates a piece of CForms template for a field.
   This is part of the DefaultFieldEditor.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:d="http://outerx.org/daisy/1.0"
  xmlns:ft="http://apache.org/cocoon/forms/1.0#template"
  xmlns:fi="http://apache.org/cocoon/forms/1.0#instance"
  xmlns:jx="http://apache.org/cocoon/templates/jx/1.0"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:daisyutil="xalan://org.outerj.daisy.frontend.util.XslUtil">
    
  <xsl:param name="displayMode">editor</xsl:param>



  <xsl:template match="d:fieldTypeUse">
    <xsl:apply-templates select="d:fieldType"/>
  </xsl:template>

  <xsl:template match="d:fieldType">
    <xsl:choose>
      <xsl:when test="$displayMode='inlineEditor'">
        <xsl:call-template name="fieldType-inlineMode"/>
      </xsl:when>
      <xsl:when test="$displayMode='workflow'">
        <xsl:call-template name="fieldType-inlineMode"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="fieldType-defaultMode"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template name="fieldType-inlineMode">
    <div class="field inlineedit">
      <ft:group id="field_{@id}">
        <xsl:choose>
          <xsl:when test="@valueType='boolean' and @multiValue != 'true'">
            <ft:widget id="field">
              <fi:styling submit-on-change="false"/>
              <xsl:if test="@description">
                <fi:standalone-help><xsl:value-of select="@description"/></fi:standalone-help>
              </xsl:if>
            </ft:widget>
            <label for="field_{@id}.field:input"><xsl:value-of select="@label"/></label>
            <xsl:call-template name="requiredIndicator"/>
          </xsl:when>
          <xsl:otherwise>
            <table>
              <tr>
                <td colspan="2" class="fieldLabel">
                  <xsl:value-of select="@label"/>
                  <xsl:if test="@description">
                      <fi:standalone-help><xsl:value-of select="@description"/></fi:standalone-help>
                  </xsl:if>
                  <xsl:call-template name="requiredIndicator"/>
                </td>
              </tr>
              <tr>
                <td>
                  <table class="plainTable">
                    <tr>
                      <td class="dsy-nowrap">
                        <xsl:call-template name="defaultWidget"/>
                      </td>
                      <td class="dsy-nowrap">
                        <xsl:if test="@multiValue = 'true'">
                          <xsl:attribute name="valign">top</xsl:attribute>
                        </xsl:if>
                        <xsl:call-template name="extraButtons"/>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
            </table>
          </xsl:otherwise>
        </xsl:choose>
      </ft:group>
    </div>
  </xsl:template>
  
  <xsl:template name="fieldType-defaultMode">
    <ft:group id="field_{@id}">
      <tr>
        <xsl:choose>
          <xsl:when test="@valueType='boolean' and @multiValue != 'true'">
            <td colspan="2">
              <ft:widget id="field">
                <fi:styling submit-on-change="false"/>
                <xsl:if test="@description">
                  <fi:standalone-help><xsl:value-of select="@description"/></fi:standalone-help>
                </xsl:if>
              </ft:widget>
              <label for="field_{@id}.field:input"><xsl:value-of select="@label"/></label>
              <xsl:call-template name="requiredIndicator"/>
            </td>
          </xsl:when>
          <xsl:otherwise>
            <td class="dsy-nowrap">
              <xsl:if test="@multiValue = 'true'">
                <xsl:attribute name="valign">top</xsl:attribute>
              </xsl:if>
              <xsl:value-of select="@label"/>
              <xsl:if test="@description">
                <fi:standalone-help><xsl:value-of select="@description"/></fi:standalone-help>
              </xsl:if>
              <xsl:call-template name="requiredIndicator"/>
            </td>
            <td class="dsyfrm-widgetcell">
              <table class="plainTable">
                <tr>
                  <td class="dsy-nowrap">
                    <xsl:call-template name="defaultWidget"/>
                  </td>
                  <td class="dsy-nowrap">
                    <xsl:if test="@multiValue = 'true'">
                      <xsl:attribute name="valign">top</xsl:attribute>
                    </xsl:if>
                    <xsl:call-template name="extraButtons"/>
                  </td>
                </tr>
              </table>
            </td>
          </xsl:otherwise>
        </xsl:choose>
        <td>
          <xsl:call-template name="aclWarning"/>
        </td>
      </tr>
    </ft:group>
  </xsl:template>

  <xsl:template name="defaultWidget">
    <ft:widget id="field">
      <fi:styling submit-on-change="false">
        <xsl:if test="@size != '0'">
          <xsl:attribute name="size"><xsl:value-of select="@size"/></xsl:attribute>
        </xsl:if>
        <xsl:choose>
          <xsl:when test="@loadSelectionListAsync = 'true' and @allowFreeEntry != 'true' and d:selectionList">
            <xsl:choose>
              <xsl:when test="@multiValue = 'true'">
                <xsl:attribute name="type">daisy-mv-dropdown-list</xsl:attribute>
                <xsl:attribute name="asyncList">true</xsl:attribute>
                <xsl:attribute name="fieldTypeId"><xsl:value-of select="@id"/></xsl:attribute>
              </xsl:when>
              <xsl:otherwise>
                <xsl:attribute name="type">daisy-async-dropdown-list</xsl:attribute>
                <xsl:attribute name="fieldTypeId"><xsl:value-of select="@id"/></xsl:attribute>
                <jx:attribute name="valueLabel" value="${{widget.getParent().getChild('field-label').value}}"/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:when test="@multiValue = 'true' and d:selectionList and @allowFreeEntry != 'true'">
            <xsl:attribute name="type">daisy-mv-dropdown-list</xsl:attribute>
          </xsl:when>
          <xsl:when test="@multiValue = 'true'">
            <xsl:attribute name="type">mv-free-entry</xsl:attribute>
          </xsl:when>
        </xsl:choose>
      </fi:styling>
    </ft:widget>
    <xsl:if test="(@valueType='link' and not(@loadSelectionListAsync = 'true' and d:selectionList)) and @multiValue != 'true'">
      <ft:widget id="field-label"/>
    </xsl:if>
  </xsl:template>

  <xsl:template name="extraButtons">
    <jx:if test="${{widget.getChild('field').state == Packages.org.apache.cocoon.forms.formmodel.WidgetState.ACTIVE}}">
      <xsl:if test="@allowFreeEntry = 'true' and d:selectionList">
        <input type="image" src="${{pageContext.mountPoint}}/resources/skins/${{pageContext.siteConf.skin}}/images/browse.gif" title="editdoc.select-from-list" i18n:attr="title"
          onclick="daisyShowSelectionList('${{widget.getChild('field').fullName}}', '{@id}'); return false;"/>
      </xsl:if>
      <xsl:text> </xsl:text>
      <xsl:if test="@valueType = 'link' and @hierarchical = 'false'">
        <xsl:variable name="whereclause" select="daisyutil:escape(d:selectionList/d:linkQuerySelectionList/d:whereClause)"/>
        <xsl:choose>
          <xsl:when test="@multiValue = 'true'">
            <input type="image" src="${{pageContext.mountPoint}}/resources/skins/${{pageContext.siteConf.skin}}/images/lookupdoc.gif" title="editdoc.links.lookupdocument" i18n:attr="title"
              onclick="openMultiValueDocBrowserDialog(document.getElementById('${{widget.getChild('field').fullName}}:input'),'{@name}','{$whereclause}'); return false;"/>
          </xsl:when>
          <xsl:otherwise>
            <input type="image" src="${{pageContext.mountPoint}}/resources/skins/${{pageContext.siteConf.skin}}/images/lookupdoc.gif" title="editdoc.links.lookupdocument" i18n:attr="title"
              onclick="lookupDocumentLinkForLinkField(document.getElementById('${{widget.getChild('field').fullName}}:input'), document.getElementById('${{widget.getChild('field-label').fullName}}'),'{@name}','{$whereclause}'); return false;"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:if>
      <xsl:text> </xsl:text>
    </jx:if>
    <xsl:if test="@valueType = 'link' and @hierarchical = 'false'">
      <input type="image" src="${{pageContext.mountPoint}}/resources/skins/${{pageContext.siteConf.skin}}/images/openlink.gif" title="editdoc.links.openlink" i18n:attr="title"
        onclick="openLinkField(document.getElementById('${{widget.getChild('field').fullName}}:input')); return false;"/>
    </xsl:if>
  </xsl:template>

  <xsl:template name="requiredIndicator">
    <xsl:if test="../@required='true'">
      <span class="forms-field-required"> * </span>
    </xsl:if>
  </xsl:template>

  <xsl:template name="aclWarning">
    <xsl:if test="@aclAllowed='true'">
      <a href="#" onclick="alert(i18n('editdoc.acl-field-warning')); return false;">ACL</a>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>