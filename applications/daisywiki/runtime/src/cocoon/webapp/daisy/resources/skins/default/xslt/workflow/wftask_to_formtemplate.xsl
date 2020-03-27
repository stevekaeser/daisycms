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
  <xsl:variable name="mountPoint" select="/page/pageContext/mountPoint"/>

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

      <extraHeadContent>
        <jx:macro name="partEditor">
          <jx:parameter name="partEditor"/>
          <div class="wfInput">
            <label for="wf.editdoc.part${{partEditor.partTypeUse.partType.id}}">
              <span class="wfInputLabel">${partEditor.partTypeUse.partType.getLabel(locale)}</span>
              <jx:if test="${{partEditor.partTypeUse.partType.getDescription(locale) != null}}">
                <span dojoType="forms:infopopup" style="display:none" class="forms-help-popup" icon="help.gif">
                  ${partEditor.partTypeUse.partType.getDescription(locale)}
                </span>
              </jx:if>
            </label>
            <span id="wf.editdoc.part${{partEditor.partTypeUse.partType.id}}">
              <ft:group id="part_${{partEditor.partTypeUse.partType.id})">
                <jx:import uri="${{partEditor.formTemplate}}"/>
              </ft:group>
            </span>
          </div>
        </jx:macro>

        <jx:if test="${{partEditors}}">
          <script language="javascript" src="{$mountPoint}/resources/js/daisy_edit.js"></script>
          <script language="javascript">
            dojo.require("daisy.dialog");
          </script>
          <script type="text/javascript"> <!-- FIXME: code duplication with document editor and documentlayout.xsl, move to daisy_edit? -->
            window.needsConfirmForLeaving = true;
            var unsetConfirmLeaving = new Object();
            unsetConfirmLeaving.forms_onsubmit = function() { window.needsConfirmForLeaving = false; return true; };
            dojo.addOnLoad(function() {
                cocoon.forms.addOnSubmitHandler(document.forms.editdoc, unsetConfirmLeaving);
            });
      
            window.onbeforeunload = function() {
              if (window.needsConfirmForLeaving) {
                var pubName = '<xsl:value-of select="/page/wfDocumentName"/>';
                var displayName = document.forms["editdoc"].elements['name']?document.forms["editdoc"].elements['name'].value:pubName;
                return "<i18n:text key="doceditor.confirm-leave-editor"/> " + displayName;
              }
                
              // reset needsConfirmForLeaving
              window.needsConfirmForLeaving = true;
            }
      
            dojo.require("dojo.io");
            dojo.require("dojo.dom");
      
            function heartbeat() {
              dojo.io.bind({
                url: "heartbeat",
                preventCache: true,
                load: function(type, data, evt) {
                  var error = dojo.dom.firstElement(data.documentElement, "error");
                  if (error != null) {
                     alert(dojo.dom.textContent(error));
                  }
                  // reschedule
                  scheduleHeartbeat();
                },
                error: function(type, data, evt) {
                  alert("Error calling heartbeat (to keep your editing session and document lock alive): " + data.message);
                  // reschedule
                  scheduleHeartbeat();
                },
                mimetype: "text/xml"
              });
            }
      
            function scheduleHeartbeat() {
              window.setTimeout(heartbeat, <xsl:value-of select="/page/heartbeatInterval"/>);
            }

            function getBranchId() {
              return '${document.branchId}';
            }
            function getLanguageId() {
              return '${document.languageId}';
            }

            dojo.addOnLoad(scheduleHeartbeat);
          </script>
        </jx:if>
        <script>
          function showTransitionDialog() {
            dojo.widget.byId("transitionDialog").show();
          }
          
          function hideTransitionDialog() {
            dojo.widget.byId("transitionDialog").hide();
          }
          
          function showConfirmationMessage(transition) {
            var confirmation = document.getElementById('confirmTransition_'+transition)
            if (confirmation) {
              return confirm(dojo.dom.textContent(confirmation));
            }
            return true;
          }
          
          dojo.addOnLoad(function() {
            cocoon.forms.addOnSubmitHandler(document.getElementById('editdoc'), { forms_onsubmit : function() {
              var dlgTransitionName = dojo.byId('dlgTransitionName');
              var transitionName = dojo.byId('transitionName:input');
              transitionName.value = dlgTransitionName.value;
              return true;
            }});
          });
        </script>
      </extraHeadContent>

      <content>
        <link rel="stylesheet" type="text/css" href="${{pageContext.mountPoint}}/resources/skins/${{pageContext.skin}}/css/inlinedialog.css"/>
      
        <h1><xsl:copy-of select="$title"/></h1>
        <ft:form-template id="editdoc" name="editdoc" action="${{submitPath}}" method="POST" enctype="multipart/form-data">
          <xsl:if test="wf:taskDefinition">
            <h2><xsl:copy-of select="wf:taskDefinition/wf:label/node()"/></h2>

            <xsl:copy-of select="wf:taskDefinition/wf:description/node()"/>
            <br/><br/>

            <ft:group id="variables">
              <xsl:apply-templates select="wf:taskDefinition"/>
            </ft:group>
            
            <jx:if test="${{daisyDocumentVariable.getVersion(repository).getId() &lt; document.getLastVersionId()}}">
              <p class="daisy-error">
                <i18n:text key="wfuptsk.warn-version-mismatch"/>
              </p>
            </jx:if>
            
            <jx:forEach items="${{partEditors}}" var="partEditor">
              <partEditor partEditor="${{partEditor}}"/>
            </jx:forEach>
            
            <jx:forEach items="${{fieldEditors}}" var="fieldEditor">
              <div class="wfInput">
                <jx:import uri="cocoon:/internal/documentEditor/fieldEditorTemplate/${{fieldEditor.fieldTypeUse.fieldType.name}}"/>
              </div>
            </jx:forEach>
            
            <xsl:call-template name="generalFields"/>
          </xsl:if>

          <br/>

          <div style="display:none">
            <ft:widget id="transitionName"/>
            
            <ft:widget id="save">
              <fi:styling class="wfDefaultAction wfAction" onclick="window.needsConfirmForLeaving = true; return showConfirmationMessage(dojo.byId('dlgTransitionName').value);"/>
            </ft:widget>
          </div>

          <xsl:apply-templates select="wf:nodeDefinition"/>
                    
          <div class="wfTaskGeneralActions">
            <div class="wfButtons">
              <xsl:choose>
                <xsl:when test="$isStartWorkflow">
                  <input type="button" class="wfDefaultAction wfAction" onclick="showTransitionDialog();" value="wfuptsk.actions-label.startprocess" i18n:attr="value"/>
                </xsl:when>
                <xsl:otherwise>
                  <input type="button" class="wfDefaultAction wfAction" onclick="showTransitionDialog();" value="wfuptsk.save" i18n:attr="value"/>
                </xsl:otherwise>
              </xsl:choose>
            
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
    <div id="transitionDialog" dojoType="dialog" bgColor="white" style="display:none">
      <div class="dsydlg-title"><i18n:text key="wfuptsk.select-transition"/></div>
      <div class="wfTaskTransitions">
        <div>
          <select id="dlgTransitionName">
            <xsl:if test="not($isStartWorkflow)">
              <option value=""><i18n:text key="wfuptsk.transition-none"/></option>
            </xsl:if>
            <xsl:for-each select="wf:leavingTransitions/wf:transitionDefinition">
              <option value="{@name}"><xsl:copy-of select="wf:label/node()"/></option>
            </xsl:for-each>
          </select>
          <span dojoType="forms:infopopup" style="display:none" class="forms-help-popup" icon="help.gif">
           <i18n:text key="wfuptsk.info-transition"/>
          </span>

          <span id="confirmTransitions" style="display:none">
            <xsl:for-each select="wf:leavingTransitions/wf:transitionDefinition">
              <xsl:if test="wf:confirmation">
                <span id="confirmTransition_{@name}">TRANS<xsl:value-of select="@name"/><xsl:copy-of select="wf:confirmation/node()"/></span>
              </xsl:if>
            </xsl:for-each>
          </span>
          <script>
            <![CDATA[
            dojo.addOnLoad(function() {
              var dlgTransitionName = dojo.byId('dlgTransitionName');
              var transitionName = dojo.byId('transitionName:input');
              for (var i=0;i<dlgTransitionName.options.length;i++) {
                if (dlgTransitionName.options[i].value==transitionName.value) {
                  dlgTransitionName.selectedIndex = i;
                  break;
                }
              }
            });
            ]]>
          </script>
        </div>
        
        <div class="wfButtons">
          <input type="button" class="wfDefaultAction wfAction" onclick="document.getElementById('save').click();"
               value="wfuptsk.save" i18n:attr="value"/>

          <input id="hideTransitionDialog" type="button" class="wfAction" value="wfuptsk.cancel" i18n:attr="value" onclick="hideTransitionDialog();"/>
        </div>
      </div>
    </div>
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
