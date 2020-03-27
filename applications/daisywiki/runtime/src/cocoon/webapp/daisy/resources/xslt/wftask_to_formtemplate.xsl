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
   Generates a piece of CForms template for a workflow (task) form.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:d="http://outerx.org/daisy/1.0"
  xmlns:wf="http://outerx.org/daisy/1.0#workflow"
  xmlns:ft="http://apache.org/cocoon/forms/1.0#template"
  xmlns:fi="http://apache.org/cocoon/forms/1.0#instance"
  xmlns:fd="http://apache.org/cocoon/forms/1.0#definition"
  xmlns:jx="http://apache.org/cocoon/templates/jx/1.0"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:variable name="isStartWorkflow" select="string(/page/isStartWorkflow) = 'true'"/>

  <xsl:template match="page">
    <page>
      <jx:import uri="resource://org/apache/cocoon/forms/generation/jx-macros.xml"/>

      ${pageContext}

      <xsl:variable name="title">
        <xsl:choose>
          <xsl:when test="$isStartWorkflow"><i18n:text key="wfuptsk.start-title"/></xsl:when>
          <xsl:otherwise><i18n:text key="wfuptsk.perform-title"/></xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

      <pageTitle><xsl:copy-of select="$title"/></pageTitle>

      <content>
        <h1><xsl:copy-of select="$title"/></h1>

        <ft:form-template action="${{submitPath}}" method="POST">
          <xsl:if test="wf:taskDefinition">
            <h2><xsl:copy-of select="wf:taskDefinition/wf:label/node()"/></h2>

            <xsl:copy-of select="wf:taskDefinition/wf:description/node()"/>
            <br/><br/>

            <ft:group id="variables">
              <xsl:apply-templates select="wf:taskDefinition"/>
            </ft:group>
            <xsl:call-template name="generalFields"/>
          </xsl:if>

          <br/>

          <xsl:apply-templates select="wf:nodeDefinition"/>

          <div class="wfTaskGeneralActions">
            <xsl:if test="not($isStartWorkflow)">
              <div class="wfActionsLabel"><i18n:text key="wfuptsk.actions-label.continue"/></div>
            </xsl:if>
            <div class="wfButtons">
              <xsl:if test="not($isStartWorkflow)">
                <!-- a task can be saved, without following any transition -->
                <ft:widget id="save">
                  <fi:styling class="wfDefaultAction wfAction"/>
                </ft:widget>
              </xsl:if>

              <ft:widget id="cancel" fi:class="wfAction"/>
            </div>
          </div>

        </ft:form-template>
      </content>
    </page>
  </xsl:template>

  <xsl:template match="wf:taskDefinition">
    <xsl:for-each select="wf:variableDefinitions/wf:variableDefinition">
      <xsl:call-template name="insertVariableInput"/>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="insertVariableInput">
    <div class="wfInput">
      <xsl:if test="@hidden = 'true'">
        <xsl:attribute name="style">display:none</xsl:attribute>
      </xsl:if>
      <xsl:call-template name="variableLabel"/>
      <br/>
      <xsl:apply-templates select=".">
        <xsl:with-param name="index" select="position()"/>
      </xsl:apply-templates>
    </div>
  </xsl:template>

  <xsl:template match="wf:variableDefinition">
    <xsl:param name="index"/>
    <xsl:variable name="styling" select="wf:styling"/>
    <ft:widget id="var{$index}">
      <fi:styling>
        <xsl:if test="$styling/@width">
          <xsl:attribute name="style">width: <xsl:value-of select="$styling/@width"/></xsl:attribute>
        </xsl:if>
        <xsl:if test="$styling/@rows">
          <xsl:attribute name="type">textarea</xsl:attribute>
          <xsl:attribute name="rows"><xsl:value-of select="$styling/@rows"/></xsl:attribute>
        </xsl:if>
        <xsl:if test="@state != 'active'">
          <xsl:attribute name="disabled">disabled</xsl:attribute>
        </xsl:if>
        <xsl:if test="@multiValue='true'">
          <xsl:attribute name="type">mv-free-entry</xsl:attribute>
        </xsl:if>
        <xsl:if test="@type='daisy-link'">
          <xsl:attribute name="dojoType">daisy:LinkEditor</xsl:attribute>
        </xsl:if>
        <xsl:if test="@type='user'">
          <xsl:attribute name="dojoType">daisy:UserSelector</xsl:attribute>
        </xsl:if>
      </fi:styling>
    </ft:widget>
  </xsl:template>
  
  <xsl:template name="userWidget">    
      <ft:group id="user">
        <i18n:text key="wf.login"/>: <ft:widget id="user" fi:size="10" fi:dojoType="daisy:UserSelector"/>
      </ft:group>
  </xsl:template>
  
  <xsl:template name="poolWidget">    
      <ft:group id="pool">
        <ft:widget id="pool" fi:list-type="double-listbox"/>
      </ft:group>
  </xsl:template>

  <xsl:template match="wf:variableDefinition[@type='actor' and @readOnly != 'true']">
    <xsl:param name="index"/>
    <xsl:variable name="styling" select="wf:styling"/>

    <ft:group id="var{$index}">
      <xsl:choose>
        <xsl:when test="$styling/@display='user'">
          <xsl:call-template name="userWidget"/>
        </xsl:when>
        <xsl:when test="$styling/@display='pool'">
          <xsl:call-template name="poolWidget"/>
        </xsl:when>
        <xsl:otherwise>
          <ft:widget id="actorCase">
            <fi:styling list-type="radio" list-orientation="horizontal"/>
          </ft:widget>
          
          <ft:union id="actor">
            <ft:case id="user">
              <xsl:call-template name="userWidget"/>
            </ft:case>
            <ft:case id="pool">
              <xsl:call-template name="poolWidget"/>
            </ft:case>            
          </ft:union>
        </xsl:otherwise>
      </xsl:choose>
      
      
    </ft:group>
  </xsl:template>

  <xsl:template name="variableLabel">
    <label for="variables.var{position()}">
      <span class="wfInputLabel"><xsl:copy-of select="wf:label/node()"/></span>
    </label>
    <xsl:if test="wf:description">
      <fi:standalone-help><xsl:copy-of select="wf:description/node()"/></fi:standalone-help>
    </xsl:if>
    <xsl:if test="@required='true' and @readOnly='false'">
      <span class="forms-field-required"> * </span>
    </xsl:if>
  </xsl:template>

  <xsl:template match="wf:nodeDefinition">
    <div class="wfTaskTransitions">
      <div class="wfActionsLabel">
        <xsl:choose>
          <xsl:when test="$isStartWorkflow">
            <i18n:text key="wfuptsk.actions-label.startprocess"/>
          </xsl:when>
          <xsl:otherwise>
            <i18n:text key="wfuptsk.actions-label.transition"/>
          </xsl:otherwise>
        </xsl:choose>
      </div>
      <div class="wfButtons">
        <xsl:apply-templates select="wf:leavingTransitions/wf:transitionDefinition"/>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="wf:transitionDefinition">
    <xsl:if test="wf:confirmation">
      <span id="confirm_{generate-id(wf:confirmation)}">
        <xsl:copy-of select="wf:confirmation/node()"/>
      </span>
    </xsl:if>
    <ft:widget id="trans{position()}">
      <xsl:choose>
        <xsl:when test="position() = 1">
          <fi:styling class="wfDefaultAction wfAction">
            <xsl:if test="wf:confirmation">
              <xsl:attribute name="onclick">return confirm(document.getElementById('confirm_<xsl:value-of select="generate-id(wf:confirmation)"/>').textContent);</xsl:attribute>
            </xsl:if>
          </fi:styling>
        </xsl:when>
        <xsl:otherwise>
          <fi:styling class="wfAction" onclick="return confirm(document.getElementById('confirm_{generate-id(wf:confirmation)}').textContent);">
            <xsl:if test="wf:confirmation">
              <xsl:attribute name="onclick">return confirm(document.getElementById('confirm_<xsl:value-of select="generate-id(wf:confirmation)"/>').textContent);</xsl:attribute>
            </xsl:if>
          </fi:styling>
        </xsl:otherwise>
      </xsl:choose>
    </ft:widget>
  </xsl:template>

  <xsl:template name="generalFields">
    <xsl:if test="not($isStartWorkflow)">
      <div class="wfInputGroupSeparator"/>
      <div class="wfInput">
        <span class="wfInputLabel"><i18n:text key="wfuptsk.due-date"/></span>
        <br/>
        <ft:widget id="dueDate"/>
      </div>
      <div class="wfInput">
        <span class="wfInputLabel"><i18n:text key="wfuptsk.priority"/></span>
        <br/>
        <ft:widget id="priority"/>
      </div>
    </xsl:if>
  </xsl:template>
  
</xsl:stylesheet>
