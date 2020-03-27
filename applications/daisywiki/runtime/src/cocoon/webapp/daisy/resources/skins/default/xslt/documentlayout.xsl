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
  xmlns:d="http://outerx.org/daisy/1.0"
  xmlns:p="http://outerx.org/daisy/1.0#publisher"
  xmlns:n="http://outerx.org/daisy/1.0#navigation"
  xmlns:cinclude="http://apache.org/cocoon/include/1.0"
  xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
  xmlns:urlencoder="xalan://java.net.URLEncoder"
  xmlns:daisyutil="xalan://org.outerj.daisy.frontend.util.XslUtil"
  xmlns:jx="http://apache.org/cocoon/templates/jx/1.0">

  <xsl:import href="daisyskin:xslt/pagevars.xsl"/>
  <xsl:include href="daisyskin:xslt/util.xsl"/>

  <xsl:param name="inlineEditing">false</xsl:param>

  <xsl:variable name="pubdoc" select="/page/p:publisherResponse/p:document"/>
  <xsl:variable name="version" select="$pubdoc/d:version"/>
  <xsl:variable name="document" select="$pubdoc/d:document"/>
  <xsl:variable name="basePath" select="concat($mountPoint, '/', $siteName)"/>
  <xsl:variable name="canRead" select="boolean($pubdoc/d:aclResult/d:permissions/d:permission[@type='read' and @action='grant'])"/>
  <xsl:variable name="canReadNonLive" select="boolean($pubdoc/d:aclResult/d:permissions/d:permission[@type='read' and @action='grant']/d:accessDetails/d:permission[@type='non_live' and @action='grant'])"/>
  <xsl:variable name="isEditor" select="boolean($pubdoc/d:aclResult/d:permissions/d:permission[@type='write' and @action='grant'])"/>
  <xsl:variable name="onlyGuestRole" select="boolean($user/activeRoles/role[@name='guest']) and count($user/activeRoles/role) = 1"/>

  <xsl:template match="page">
    <page>
    
      <xsl:copy-of select="context"/>
      <xsl:call-template name="pageTitle"/>
      <xsl:copy-of select="p:publisherResponse/n:navigationTree"/>
      <navigationInfo>
        <xsl:copy-of select="p:publisherResponse/p:group[@id = 'navigationInfo']/p:document/*"/>        
      </navigationInfo>
      <xsl:call-template name="pageNavigation"/>
      <xsl:call-template name="availableVariants"/>
      <content><xsl:call-template name="content"/></content>
      <xsl:if test="$inlineEditing='true'">  <!-- TODO: doesn't feel quite right here, but move where? -->
        <extraHeadContent>
          <jx:import uri="resource://org/apache/cocoon/forms/generation/jx-macros.xml"/>

          <script language="javascript" src="{$mountPoint}/resources/js/daisy_edit.js"></script>
          <script language="javascript">
            dojo.require("daisy.dialog");
          </script>
          <script type="text/javascript"> <!-- TODO: shared code with document editor, move to daisy_edit? // tmEditor-->
            window.needsConfirmForLeaving = true;
            var unsetConfirmLeaving = new Object();
            unsetConfirmLeaving.forms_onsubmit = function() { window.needsConfirmForLeaving = false; return true; };
            dojo.addOnLoad(function() {
                cocoon.forms.addOnSubmitHandler(document.forms.editdoc, unsetConfirmLeaving);
            });

            window.onbeforeunload = function() {
                if (window.needsConfirmForLeaving) {
                    var pubName = '<xsl:value-of select="daisyutil:escape(p:publisherResponse/p:document/d:document/@name)"/>';
                    var displayName =
                            document.forms["editdoc"].elements['name']?document.forms["editdoc"].elements['name'].value:pubName;
                    return "<i18n:text key="doceditor.confirm-leave-editor"/>" + displayName;
                }

                // reset needsConfirmForLeaving
                window.needsConfirmForLeaving = true;
            }

            function switchTab(tabName) {
                document.getElementById("activeForm").value = tabName;
                cocoon.forms.submitForm(document.forms.editdoc.dummy);
                return false;
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
                window.setTimeout(heartbeat,<xsl:value-of select="/page/heartbeatInterval"/>);
            }

            dojo.addOnLoad(scheduleHeartbeat);
            
            function getBranchId() {
              return <xsl:value-of select="$document/@branchId"/>;
            }
            function getLanguageId() {
              return <xsl:value-of select="$document/@languageId"/>;
            }
            
          </script>

        </extraHeadContent>
      </xsl:if>
      <extraMainContent><xsl:call-template name="comments"/></extraMainContent>
      <document id="{$document/@id}"
                branchId="{$document/@branchId}" branch="{$document/@branch}"
                languageId="{$document/@languageId}" language="{$document/@language}"
                versionId="{$document/@dataVersionId}"
                name="{$document/@name}"/>
    </page>
  </xsl:template>

  <xsl:template name="pageNavigation">
    <pageNavigation>
      <xsl:if test="$isEditor">
        <link needsPost="true">
          <title><i18n:text key="doclayout.edit"/></title>
          <path><xsl:value-of select="concat($documentPath, '/edit', $variantQueryString)"/></path>
        </link>
        <separator/>
        <xsl:if test="$pubdoc/d:aclResult/d:permissions/d:permission[@type='delete' and @action='grant']
                   or $pubdoc/d:aclResult/d:permissions/d:permission[@type='write' and @action='grant']/d:accessDetails/d:permission[@type='retired' and @action='grant']">
          <link>
            <title><i18n:text key="doclayout.delete"/></title>
            <path><xsl:value-of select="concat($documentPath, '.html?action=delete', $variantParams)"/></path>
          </link>
        </xsl:if>
        <link needsPost="true">
          <title><i18n:text key="doclayout.duplicate"/></title>
          <path><xsl:value-of select="concat($mountPoint, '/', $siteName, '/new/edit?template=', $document/@id, $variantParams)"/></path>
        </link>
      </xsl:if>
      <xsl:if test="$user/updateableByUser/text() = 'true'">
        <xsl:choose>
          <xsl:when test="$pubdoc/p:subscriptionInfo/@subscribed = 'true'">
            <link needsPost="true">
              <title><i18n:text key="doclayout.unsubscribe"/></title>
              <path><xsl:value-of select="concat($documentPath, '.html?action=unsubscribe', $variantParams)"/></path>
            </link>
          </xsl:when>
          <xsl:otherwise>
            <xsl:if test="$canRead">
              <link needsPost="true">
                <title><i18n:text key="doclayout.subscribe"/></title>
                <path><xsl:value-of select="concat($documentPath, '.html?action=subscribe', $variantParams)"/></path>
              </link>
            </xsl:if>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:if>
      <xsl:if test="$user/activeRoles/role[@id='1'] or $document/@owner = $user/id/text()">
        <link>
          <title><i18n:text key="doclayout.change-owner"/></title>
          <path><xsl:value-of select="concat($documentPath, '.html?action=changeOwner', $variantParams)"/></path>
        </link>
      </xsl:if>
      <link needsPost="true">
        <title><i18n:text key="doclayout.add-to-basket"/></title>
        <path><xsl:value-of select="concat($documentPath, '.html?action=addToBasket', $variantParams)"/></path>
      </link>
      <separator/>
      <xsl:if test="$canReadNonLive">
        <xsl:if test="$document/@dataVersionId > 1">
          <link>
            <title><i18n:text key="doclayout.changes"/></title>
            <path><xsl:value-of select="concat($documentPath, '/version/', $document/@dataVersionId - 1, '/diff?otherVersion=', $document/@dataVersionId, $variantParams)"/></path>
          </link>
        </xsl:if>
        <link>
          <title><i18n:text key="doclayout.versions"/></title>
          <path><xsl:value-of select="concat($documentPath, '/versions.html', $variantQueryString)"/></path>
        </link>
        <link>
          <title><i18n:text key="doclayout.live-versions"/></title>
          <path><xsl:value-of select="concat($documentPath, '/live-versions.html', $variantQueryString)"/></path>
        </link>
      </xsl:if>
      <link>
        <title><i18n:text key="doclayout.referrers"/></title>
        <path><xsl:value-of select="concat($documentPath, '/referrers.html', $variantQueryString)"/></path>
      </link>
      <xsl:if test="not($onlyGuestRole)">
        <link>
          <title><i18n:text key="doclayout.related-workflows"/></title>
          <xsl:variable name="documentLink" select="concat('daisy:', $document/@id, '@', $document/@branch, ':', $document/@language)">
          </xsl:variable>
          <path><xsl:value-of select="concat($basePath, '/workflow/processSearch?state=open&amp;document=', urlencoder:encode($documentLink, 'UTF-8'))"/></path>
        </link>
      </xsl:if>
      <separator/>
      <link>
        <title><i18n:text key="hide-navigation"/></title>
        <path><xsl:value-of select="concat($documentPath, '.html?layoutType=plain', $variantParams)"/></path>
      </link>
      <link>
        <title><i18n:text key="doclayout.pdf"/></title>
        <path>
          <xsl:variable name="requestedVersion" select="string(/page/requestedVersion)"/>
          <xsl:choose>
            <xsl:when test="$requestedVersion != ''">
              <xsl:value-of select="concat($documentPath, '/version/', $requestedVersion, '.pdf', $variantQueryString)"/>                            
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="concat($documentPath, '.pdf', $variantQueryString)"/>
            </xsl:otherwise>
          </xsl:choose>
        </path>
      </link>
    </pageNavigation>
  </xsl:template>

  <!-- The availableVariants variable is picked up by the layout.xsl -->
  <xsl:template name="availableVariants">
    <availableVariants>
      <variants>
        <xsl:for-each select="$pubdoc/d:availableVariants/d:availableVariant">
          <xsl:sort select="@branchName"/>
          <xsl:sort select="@languageName"/>
          <!-- exclude retired versions and show unpublished variants only to editors -->
          <xsl:if test="@retired = 'false'">
            <xsl:if test="@liveVersionId != '-1' or ($isEditor and @liveVersionId = '-1')">
              <variant href="{concat($mountPoint, '/', $siteName, '/', urlencoder:encode($documentId), '.html?branch=', urlencoder:encode(@branchName), '&amp;language=', urlencoder:encode(@languageName))}"
                branchName="{@branchName}" languageName="{@languageName}">
                <xsl:if test="@branchId = $document/@branchId and @languageId = $document/@languageId">
                  <xsl:attribute name="current">true</xsl:attribute>
                </xsl:if>
              </variant>
            </xsl:if>
          </xsl:if>
        </xsl:for-each>
      </variants>
      <xsl:if test="$isEditor">
        <createVariant href="{concat($documentPath, '/createVariant', $variantQueryString)}"/>
      </xsl:if>
    </availableVariants>
  </xsl:template>

  <xsl:template name="pageTitle">
    <pageTitle>
      <xsl:choose>
        <xsl:when test="$version">
          <xsl:value-of select="$version/@documentName"/>
        </xsl:when>
        <xsl:otherwise>
          <i18n:text key="doclayout.in-preparation"/>
        </xsl:otherwise>
      </xsl:choose>
    </pageTitle>
  </xsl:template>

  <xsl:template name="content">
    <xsl:if test="pageMessage">
      <div id="pageMessage">
        <xsl:copy-of select="pageMessage/node()"/>
        <div style="text-align: right; font-size: x-small"><a href="#" onclick="this.parentNode.parentNode.style.display='none'; return false;" onmouseover="status='';return true;"><i18n:text catalogue="skin" key="layout.hide"/></a></div>
      </div>
    </xsl:if>
    
    <xsl:choose>
      <xsl:when test="not($version)">
        <xsl:call-template name="retiredNotice"/>
        <h1><i18n:text key="doclayout.in-preparation"/></h1>

        <p><i18n:text key="doclayout.in-preparation-info"/></p>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="retiredNotice"/>
        <xsl:variable name="modeType"><xsl:choose>
          <xsl:when test="string(/page/context/versionMode)='last'">last</xsl:when>
          <xsl:when test="string(/page/context/versionMode)='live'">live</xsl:when>
          <xsl:otherwise>pit</xsl:otherwise>
        </xsl:choose></xsl:variable>
        <xsl:variable name="modeLiveVersionId"><xsl:choose>
          <xsl:when test="string(/page/context/versionMode)='last'"><xsl:value-of select="$document/@liveVersionId"/></xsl:when>
          <xsl:otherwise><xsl:value-of select="$document/@modeVersionId"/></xsl:otherwise>
        </xsl:choose></xsl:variable>
        <xsl:choose>
          <xsl:when test="$pubdoc/@documentId='new'">
            <!-- new document don't have any versions (this can happend during inline editing) -->
          </xsl:when>
          <xsl:when test="$isEditor and $document/@dataVersionId != $document/@lastVersionId and $document/@dataVersionId = $modeLiveVersionId">
            <!-- editor should be warned when not viewing the last version -->
            <div class="info-message" id="version-warning">
              <strong><i18n:text key="warning-upper"/>: </strong>
              <i18n:translate>
                <xsl:choose>
                  <xsl:when test="$modeType != 'pit'">
                    <i18n:text key="doclayout.live-but-not-last"/>
                  </xsl:when>
                  <xsl:otherwise>
                    <i18n:text key="doclayout.pit-but-not-last"/>
                    <i18n:param><xsl:value-of select="/page/context/localVersionMode"/></i18n:param>
                  </xsl:otherwise>
                </xsl:choose>
              </i18n:translate>
            </div>
          </xsl:when>
          <xsl:when test="$document/@liveVersionId and $document/@dataVersionId != $modeLiveVersionId">
            <!-- warn people when looking at a version other than the one matching the version mode -->
            <div class="info-message" id="version-warning">
              <strong><i18n:text key="warning-upper"/>: </strong>
              <i18n:translate>
                <xsl:choose>
                  <xsl:when test="$modeType != 'pit' and $document/@dataVersionId = $modeLiveVersionId">
                    <i18n:text key="doclayout.not-live-but-last"/>
                  </xsl:when>
                  <xsl:when test="$modeType != 'pit' and $document/@dataVersionId > $modeLiveVersionId">
                    <i18n:text key="doclayout.not-live-but-more-recent"/>
                  </xsl:when>
                  <xsl:when test="$modeType != 'pit' and $document/@dataVersionId &lt; $modeLiveVersionId">
                    <i18n:text key="doclayout.not-live-but-older"/>
                  </xsl:when>
                  <xsl:when test="$modeType = 'pit' and $document/@dataVersionId = $modeLiveVersionId">
                    <i18n:text key="doclayout.not-pit-but-last"/>
                    <i18n:param><xsl:value-of select="/page/context/localVersionMode"/></i18n:param>
                  </xsl:when>
                  <xsl:when test="$modeType = 'pit' and $document/@dataVersionId > $modeLiveVersionId">
                    <i18n:text key="doclayout.not-pit-but-more recent"/>
                    <i18n:param><xsl:value-of select="/page/context/localVersionMode"/></i18n:param>
                  </xsl:when>
                  <xsl:when test="$modeType = 'pit' and $document/@dataVersionId &lt; $modeLiveVersionId">
                    <i18n:text key="doclayout.not-pit-but-older"/>
                    <i18n:param><xsl:value-of select="/page/context/localVersionMode"/></i18n:param>
                  </xsl:when>
                </xsl:choose>
              </i18n:translate>
              
              <xsl:if test="$document/@dataVersionId = $document/@lastVersionId and $modeType != 'pit' and boolean($pubdoc/d:aclResult/d:permissions/d:permission[@type='publish' and @action='grant'])">
                <xsl:variable name="returnTo" select="urlencoder:encode(concat($documentPath, '.html', $variantQueryString), 'UTF-8')"/>
                <xsl:call-template name="generatePostLink">
                  <xsl:with-param name="action" select="concat($documentPath, '/version/', $document/@dataVersionId, '?action=changeState&amp;makeLive=true&amp;newState=publish&amp;returnTo=', $returnTo, $variantParams)"></xsl:with-param>
                  <xsl:with-param name="label"><i18n:text key="doclayout.make-version-live"/></xsl:with-param>
                  <xsl:with-param name="id">make-last-version-live</xsl:with-param>
                </xsl:call-template>
              </xsl:if>
            </div>
          </xsl:when>
        </xsl:choose>
        <script>$(function() { $("a[href='{modeVersionUrl}']").attr("href", '<xsl:value-of select="concat($documentPath, '/version/', $document/@modeVersionId)"/>'); });</script>
        <script>$(function() { $("a[href='{lastVersionUrl}']").attr("href", '<xsl:value-of select="concat($documentPath, '/version/', $document/@lastVersionId)"/>'); });</script>

        <insertStyledDocument styledResultsId="{$pubdoc/p:preparedDocuments/@styledResultsId}"/>

      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="retiredNotice">
    <xsl:if test="$document/@retired='true'">
      <h1><i18n:text key="doclayout.retired"/></h1>

      <p><i18n:text key="doclayout.retired-info"/></p>

      <hr/>
    </xsl:if>
  </xsl:template>

  <xsl:template name="comments">
    <xsl:variable name="comments" select="$pubdoc/d:comments"/>
    <xsl:variable name="showComments" select="string(displayParams/param[@name='showComments']/@value)"/>
    <div id="commentContainer">
      <div class="corner">
        <span>
          <a id="showCommentsLink" href="#" onmouseover="window.status=''; return true;" onclick="document.getElementById('comments').style.display=''; document.getElementById('showCommentsLink').style.display='none'; return false;">
            <xsl:if test="$showComments = 'true'">
              <xsl:attribute name="style">display: none</xsl:attribute>
            </xsl:if>
            <xsl:text> </xsl:text>
            <i18n:text key="comments.show"/>
            (<xsl:value-of select="count($comments/d:comment)"/>)
          </a>
        </span>
  
        <a name="daisycomments"/>
        <div class="comments" id="comments">
          <xsl:if test="$showComments != 'true'">
            <xsl:attribute name="style">display: none</xsl:attribute>
          </xsl:if>
          <span class="commentsTitle"><i18n:text key="comments.title"/></span>
          <xsl:text> </xsl:text>
          <a href="#" onmouseover="window.status=''; return true;" onclick="document.getElementById('comments').style.display='none'; document.getElementById('showCommentsLink').style.display=''; return false;"><i18n:text key="comments.hide"/></a>
          <br/>
          <xsl:choose>
            <xsl:when test="not($comments/d:comment)">
              <i18n:text key="comments.no-comments"/>
            </xsl:when>
            <xsl:otherwise>
              <!-- The [1] after $comments is normally not necessary, but suddenly all comments
                   were displayed double after upgrading Cocoon (and thus Xalan). Seems like a bug (oct 24 2005) -->
              <xsl:apply-templates select="$comments[1]/d:comment"/>
            </xsl:otherwise>
          </xsl:choose>
    
          <div class="addCommentTitle"><i18n:text key="comments.add-comment"/></div>
          <div class="addComment">
            <xsl:choose>
              <xsl:when test="$user/login = 'guest'">
                <i18n:text key="comments.log-in-to-add-comments"/>
              </xsl:when>
              <xsl:otherwise>
                <a name="daisycommenteditor"/>
                <form action="{$documentPath}.html{$variantQueryString}" method="POST">
                  <input type="hidden" name="action" value="addComment"/>
                  <textarea name="commentText" style="width: 40em;" rows="5" cols="80"/>
                  <br/>
                  <i18n:text key="comments.visible-to"/>
                  <xsl:text> </xsl:text>
                  <select name="commentVisibility">
                    <option value="public"><i18n:text key="comments.visibility-everyone"/></option>
                    <xsl:if test="$isEditor">
                      <option value="editors"><i18n:text key="comments.visibility-editors"/></option>
                    </xsl:if>
                      <option value="private"><i18n:text key="comments.visibility-private"/></option>
                  </select>
                  <br/>
                  <input type="submit" value="comments.add" i18n:attr="value"/>
                </form>
              </xsl:otherwise>
            </xsl:choose>
          </div>
        </div>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="d:comment">
    <a name="daisycomment{@id}"/>
    <div class="comment {@visibility}Comment">
      <table class="plainTable" width="100%">
        <tr>
          <td>
            <div class="commentheader">
              <i18n:text key="comments.created-by"/><xsl:text> </xsl:text><xsl:value-of select="@createdByDisplayName"/>
              <xsl:text> </xsl:text>
              <i18n:text key="comments.created-on"/><xsl:text> </xsl:text><xsl:value-of select="@createdOnFormatted"/>
            </div>
          </td>
          <td align="right">
            <span class="commentVisibility">
              <xsl:choose>
                <xsl:when test="@visibility='private'">
                  <i18n:text key="comments.private-comment"/>
                </xsl:when>
                <xsl:when test="@visibility='editors'">
                  <i18n:text key="comments.editors-comment"/>
                </xsl:when>
              </xsl:choose>
            </span>
            <xsl:text> </xsl:text>
            <span class="commentActions">
              <xsl:if test="$isEditor or (@createdBy = $user/id and @visibility='private')">
                <form style="display:none" id="comment{@id}" action="?action=deleteComment&amp;commentId={@id}{$variantParams}" method="POST"/>
                <a href="#" onclick="document.getElementById('comment{@id}').submit(); return false;" onmouseover="status='';return true;"><i18n:text key="comments.delete"/></a>
              </xsl:if>
            </span>
          </td>
        </tr>
      </table>
      <div class="commentbody">
        <xsl:copy-of select="./node()"/>
      </div>
    </div>
  </xsl:template>

</xsl:stylesheet>
