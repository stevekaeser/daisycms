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
   Generates a piece of CForms form definition for editing a workflow task.
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
    <fd:form>
      <fd:widgets>
        <fd:group id="documentData">
          <fd:widgets>
            <xsl:apply-templates select="wf:documentPart"/>
            <xsl:apply-templates select="wf:documentField"/>
          </fd:widgets>
        </fd:group>

        <fd:group id="variables">
          <fd:widgets>
            <xsl:apply-templates select="wf:taskDefinition"/>
          </fd:widgets>
        </fd:group>

        <xsl:if test="not($isStartWorkflow)">
          <fd:field id="dueDate">
            <fd:datatype base="date">
              <fd:convertor type="formatting" variant="datetime" style="short" timeStyle="medium" lenient="false"/>
            </fd:datatype>
          </fd:field>
          <fd:field id="priority">
            <fd:datatype base="string"/>
            <fd:selection-list>
              <fd:item value="lowest"><fd:label><i18n:text key="wf.priority-lowest"/></fd:label></fd:item>
              <fd:item value="low"><fd:label><i18n:text key="wf.priority-low"/></fd:label></fd:item>
              <fd:item value="normal"><fd:label><i18n:text key="wf.priority-normal"/></fd:label></fd:item>
              <fd:item value="high"><fd:label><i18n:text key="wf.priority-high"/></fd:label></fd:item>
              <fd:item value="highest"><fd:label><i18n:text key="wf.priority-highest"/></fd:label></fd:item>
            </fd:selection-list>
          </fd:field>
        </xsl:if>
        
        <!-- when a task is saved, this results in an updateTask (no transition) or an endTask (following a transition) -->
        <fd:submit id="save">
          <fd:label><i18n:text key="wfuptsk.save"/></fd:label>
        </fd:submit>

        <fd:submit id="cancel" validate="false">
          <fd:label><i18n:text key="wfuptsk.cancel"/></fd:label>
        </fd:submit>
        
        <jx:forEach items="{'${partEditors}'}" var="partEditor">
          <fd:group id="part_{'${partEditor.partTypeUse.partType.id}'}">
            <fd:widgets>
              ${partEditor.generateFormDefinitionFragment(cocoon.consumer, locale, displayMode, serviceManager)}
            </fd:widgets>
          </fd:group>
        </jx:forEach>
        
        <jx:forEach items="{'${fieldEditors}'}" var="fieldEditor">
          <fd:group id="field_{'${fieldEditor.fieldTypeUse.fieldType.id}'}">
            <fd:widgets>
              ${fieldEditor.generateFormDefinitionFragment(cocoon.consumer, locale, displayMode, serviceManager)}
            </fd:widgets>
          </fd:group>
        </jx:forEach>

        <xsl:apply-templates select="wf:nodeDefinition"/>

      </fd:widgets>
    </fd:form>
  </xsl:template>

  <xsl:template match="wf:taskDefinition">
    <xsl:apply-templates select="wf:variableDefinitions/wf:variableDefinition"/>
  </xsl:template>

  <xsl:template match="wf:variableDefinition">
    <fd:field id="var{position()}">
      <xsl:if test="@readOnly = 'true'">
        <xsl:attribute name="state">output</xsl:attribute>
      </xsl:if>

      <fd:datatype>
        <xsl:attribute name="base">
          <xsl:choose>
            <xsl:when test="@type = 'datetime'">date</xsl:when>
            <xsl:when test="@type = 'daisy-link'">string</xsl:when>
            <xsl:when test="@type = 'user'">string</xsl:when>
            <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>

        <xsl:choose>
          <xsl:when test="@type = 'datetime'">
            <fd:convertor type="formatting" variant="datetime" style="short" timeStyle="medium" lenient="false"/>
          </xsl:when>
          <xsl:when test="@type = 'daisy-link'">
            <fd:convertor type="wfversionkey"/>
          </xsl:when>
        </xsl:choose>

      </fd:datatype>

      <fd:validation>
        <xsl:if test="@type = 'user'">
          <fd:java class="org.outerj.daisy.frontend.UserLoginValidator"/>
        </xsl:if>
        <xsl:if test="@type = 'daisy-link'">
          <fd:java class="org.outerj.daisy.frontend.workflow.WfVersionKeyValidator"/>
        </xsl:if>
      </fd:validation>

      <xsl:call-template name="attributes"/>
    </fd:field>
  </xsl:template>

  <xsl:template match="wf:variableDefinition[@type='boolean']">
    <fd:booleanfield id="var{position()}">
      <xsl:if test="@readOnly = 'true'">
        <xsl:attribute name="state">output</xsl:attribute>
      </xsl:if>
      <xsl:call-template name="attributes"/>
    </fd:booleanfield>
  </xsl:template>

  <xsl:template match="wf:variableDefinition[@type='actor' and @readOnly='true']">
    <fd:field id="var{position()}" state="output">
      <fd:datatype base="string"/>
      <xsl:call-template name="attributes"/>
    </fd:field>
  </xsl:template>
  
  <xsl:template name="userWidget">
    <fd:group id="user">
      <fd:widgets>
        <fd:field id="user">
          <fd:datatype base="string"/>
          <fd:validation>
            <fd:java class="org.outerj.daisy.frontend.UserLoginValidator"/>
          </fd:validation>
        </fd:field>
      </fd:widgets>
    </fd:group>
  </xsl:template>
  
  <xsl:template name="poolWidget">
    <fd:group id="pool">
      <fd:widgets>
        <fd:multivaluefield id="pool">
          <fd:datatype base="long"/>
          <fd:selection-list type="flow-jxpath" list-path="pools" value-path="id" label-path="name" />
        </fd:multivaluefield>
      </fd:widgets>
    </fd:group>
  </xsl:template>

  <xsl:template match="wf:variableDefinition[@type='actor' and @readOnly != 'true']">
    <xsl:variable name="styling" select="wf:styling"/>
    
    <fd:group id="var{position()}">
      <fd:widgets>
        <xsl:choose>
          <xsl:when test="$styling/@display='user'">
            <xsl:call-template name="userWidget"/>
          </xsl:when>
          <xsl:when test="$styling/@display='pool'">
            <xsl:call-template name="poolWidget"/>
          </xsl:when>
          <xsl:otherwise>
            <fd:field id="actorCase">
              <fd:datatype base="string"/>
              <fd:selection-list>
                <fd:item value="user"><fd:label><i18n:text key="wf.actor-user"/></fd:label></fd:item>
                <fd:item value="pool"><fd:label><i18n:text key="wf.actor-pools"/></fd:label></fd:item>
              </fd:selection-list>
              <fd:initial-value>user</fd:initial-value>
            </fd:field>
            
            <fd:union id="actor" case="actorCase">              
              <fd:widgets>
                <xsl:call-template name="userWidget"/>                
                <xsl:call-template name="poolWidget"/>
              </fd:widgets>
            </fd:union>
          </xsl:otherwise>
        </xsl:choose>
                
      </fd:widgets>

      <xsl:call-template name="attributes"/>
    </fd:group>
  </xsl:template>

  <xsl:template name="attributes">
    <fd:attributes>
      <fd:attribute name="variableName" value="{@name}"/>
      <fd:attribute name="variableScope" value="{@scope}"/>
      <fd:attribute name="variableType" value="{@type}"/>
      <fd:attribute name="variableRequired" value="{@required}"/>
      <fd:attribute name="variableReadOnly" value="{@readOnly}"/>
    </fd:attributes>
  </xsl:template>

  <xsl:template match="wf:nodeDefinition">
    <fd:field id="transitionName">
      <fd:datatype base="string"/>
    </fd:field>
  </xsl:template>

</xsl:stylesheet>