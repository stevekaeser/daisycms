/*
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
*/

/*

Installer User Interface

*/

;--------------------------------
;General

Name "${APP_NAME} ${APP_VERSION}.${APP_SERIES_KEY}"

;Default installation folder
InstallDir "${SETUP_DEFAULT_DIRECTORY}"

;--------------------------------
;Installer language

;Get from registry if available
!define MUI_LANGDLL_REGISTRY_ROOT SHELL_CONTEXT
!define MUI_LANGDLL_REGISTRY_KEY "${APP_REGKEY_SETUP}"
!define MUI_LANGDLL_REGISTRY_VALUENAME "Setup Language"

;--------------------------------
;Interface settings

!define MUI_ABORTWARNING
!define MUI_ICON "${SETUP_ICON}"
!define MUI_UNICON "${SETUP_ICON}"
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "${SETUP_HEADERIMAGE}"
!define MUI_HEADERIMAGE_RIGHT
!define MUI_WELCOMEFINISHPAGE_BITMAP "${SETUP_WIZARDIMAGE}"
!define MUI_UNWELCOMEFINISHPAGE_BITMAP "${SETUP_WIZARDIMAGE}"
!define MUI_CUSTOMFUNCTION_GUIINIT InitInterface
;!define MUI_COMPONENTSPAGE_NODESC

;--------------------------------
;Pages

;Installer
;The window title of the language selection dialog.
!define MUI_LANGDLL_WINDOWTITLE "${APP_NAME}-${APP_VERSION}.${APP_SERIES_KEY}: Installer Language"
;The text to display on the language selection dialog.
;!define MUI_LANGDLL_INFO "Select your language please"
!define MUI_WELCOMEPAGE_TITLE_3LINES
!define MUI_WELCOMEPAGE_TEXT $(TEXT_WELCOME_${SETUPTYPE_NAME})
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "${FILES_LICENSE}"

Page custom PageUser PageUserValidate
Page custom PageReinstall PageReinstallValidate
Page custom PageExternalJAVA PageExternalJAVAValidate
Page custom PageExternalJAI PageExternalJAIValidate
Page custom PageExternalMySQL PageExternalMySQLValidate

!insertmacro MUI_PAGE_COMPONENTS ;for service installation

!insertmacro MUI_PAGE_DIRECTORY
ShowInstDetails Show
!insertmacro MUI_PAGE_INSTFILES
Page custom PageSetupMySQL PageSetupMySQLValidate
Page custom PageSetupTables PageSetupTablesValidate
Page custom PageSetupDaisy PageSetupDaisyValidate
Page custom PageSetupNamespace PageSetupNamespaceValidate
Page custom PageSetupSites PageSetupSitesValidate

!define MUI_FINISHPAGE_RUN "$INSTDIR\${APP_RUN}"
!define MUI_FINISHPAGE_RUN_TEXT $(TEXT_FINISH_BROWSE_DAISY)
!define MUI_FINISHPAGE_RUN_FUNCTION LaunchDaisy
!define MUI_FINISHPAGE_SHOWREADME
!define MUI_FINISHPAGE_SHOWREADME_NOTCHECKED
!define MUI_FINISHPAGE_SHOWREADME_FUNCTION CreateDesktopShortcut
!define MUI_FINISHPAGE_SHOWREADME_TEXT $(TEXT_FINISH_DESKTOP)
!define MUI_FINISHPAGE_LINK $(TEXT_FINISH_WEBSITE)
!define MUI_FINISHPAGE_LINK_LOCATION "http://www.daisycms.org"
!insertmacro MUI_PAGE_FINISH

;Uninstaller

!define MUI_WELCOMEPAGE_TITLE_3LINES
!define MUI_WELCOMEPAGE_TEXT $(UNTEXT_WELCOME)
!insertmacro MUI_UNPAGE_WELCOME
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_COMPONENTS
ShowUnInstDetails Show
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_UNPAGE_FINISH

;--------------------------------
;Languages

!insertmacro IncludeLang "english"
!insertmacro IncludeLang "german"
!insertmacro IncludeLang "dutch"
!insertmacro IncludeLang "russian"
!insertmacro IncludeLang "french"

;--------------------------------
;Macros

!macro InitDialogExternal component

  !insertmacro MUI_INSTALLOPTIONS_WRITE "external_${component}.ini" "Field 1" "Text" $(TEXT_EXTERNAL_${component}_INFO_${SETUPTYPE_NAME})
  !insertmacro MUI_INSTALLOPTIONS_WRITE "external_${component}.ini" "Field 3" "Text" $(TEXT_EXTERNAL_${component}_FOLDER)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "external_${component}.ini" "Field 5" "Text" $(TEXT_EXTERNAL_${component}_FOLDER_INFO)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "external_${component}.ini" "Field 6" "Text" $(TEXT_EXTERNAL_${component}_NONE)
  
    ${if} $AdminOrPowerUser == ${TRUE} 
      !insertmacro MUI_INSTALLOPTIONS_WRITE "external_${component}.ini" "Field 2" "Text" "$(TEXT_EXTERNAL_${component}_${SETUPTYPE_NAME})"
    ${else}
      !insertmacro MUI_INSTALLOPTIONS_WRITE "external_${component}.ini" "Field 2" "Text" "$(TEXT_EXTERNAL_${component}_${SETUPTYPE_NAME}) $(TEXT_EXTERNAL_NOPRIVILEGES)"
      !insertmacro MUI_INSTALLOPTIONS_WRITE "external_${component}.ini" "Field 2" "Flags" "DISABLED"
    ${endif}
  
!macroend

!macro InitDialogExternalDir component

  !insertmacro MUI_INSTALLOPTIONS_WRITE "external_${component}.ini" "Field 4" "State" $Path${component}
  
    ${if} $AdminOrPowerUser == ${TRUE}
  
      ${if} $Path${component} == ""
        !insertmacro MUI_INSTALLOPTIONS_WRITE "external_${component}.ini" "Field 2" "State" "1"
      ${else}
        !insertmacro MUI_INSTALLOPTIONS_WRITE "external_${component}.ini" "Field 3" "State" "1"
      ${endif}
    
    ${else}
      !insertmacro MUI_INSTALLOPTIONS_WRITE "external_${component}.ini" "Field 3" "State" "1"
    ${endif}

  
!macroend

;--------------------------------
;Functions

Function InitDialogs

  ;Extract dialogs
  
  !insertmacro MUI_INSTALLOPTIONS_EXTRACT_AS "dialogs\user.ini" "user.ini"
  !insertmacro MUI_INSTALLOPTIONS_EXTRACT_AS "dialogs\reinstall.ini" "reinstall.ini"
  !insertmacro MUI_INSTALLOPTIONS_EXTRACT_AS "dialogs\external.ini" "external_java.ini"
  !insertmacro MUI_INSTALLOPTIONS_EXTRACT_AS "dialogs\externalJAI.ini" "external_JAI.ini"
  !insertmacro MUI_INSTALLOPTIONS_EXTRACT_AS "dialogs\external.ini" "external_MySQL.ini"
  !insertmacro MUI_INSTALLOPTIONS_EXTRACT_AS "dialogs\createdatabases.ini" "setup_Databases.ini"
  !insertmacro MUI_INSTALLOPTIONS_EXTRACT_AS "dialogs\setupmysql.ini" "setup_MySQL.ini"
  !insertmacro MUI_INSTALLOPTIONS_EXTRACT_AS "dialogs\setupdaisy.ini" "setup_Daisy.ini"
  !insertmacro MUI_INSTALLOPTIONS_EXTRACT_AS "dialogs\reponamespace.ini" "setup_Namespace.ini"
  !insertmacro MUI_INSTALLOPTIONS_EXTRACT_AS "dialogs\createsites.ini" "setup_Sites.ini"

  ;Write texts
  
  !insertmacro MUI_INSTALLOPTIONS_WRITE "user.ini" "Field 1" "Text" $(TEXT_USER_INFO)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "user.ini" "Field 2" "Text" $(TEXT_USER_CURRENT)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "user.ini" "Field 3" "Text" $(TEXT_USER_ALL)
  
  !insertmacro MUI_INSTALLOPTIONS_WRITE "reinstall.ini" "Field 1" "Text" $(TEXT_REINSTALL_INFO)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "reinstall.ini" "Field 2" "Text" $(TEXT_REINSTALL_ENABLE)
  
  !insertmacro InitDialogExternal java
  !insertmacro InitDialogExternal JAI
  !insertmacro InitDialogExternal MySQL
    
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_MySQL.ini" "Field 1" "Text" $(TEXT_SETUP_MYSQL_INFO)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_MySQL.ini" "Field 2" "Text" $(TEXT_SETUP_DATABASES_NONEXISTING)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_MySQL.ini" "Field 3" "Text" $(TEXT_SETUP_DATABASES_EXISTING)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_MySQL.ini" "Field 4" "Text" $(TEXT_SETUP_MYSQL_GROUPTITLE)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_MySQL.ini" "Field 5" "Text" $(TEXT_SETUP_MYSQL_SERVER_NAME)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_MySQL.ini" "Field 7" "Text" $(TEXT_SETUP_MYSQL_SERVER_PORT)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_MySQL.ini" "Field 9" "Text" $(TEXT_SETUP_MYSQL_SERVER_ROOT)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_MySQL.ini" "Field 11" "Text" $(TEXT_SETUP_MYSQL_SERVER_ROOTPASSWORD)
  
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Databases.ini" "Field 1" "Text" $(TEXT_SETUP_DATABASES_GROUPTITLE)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Databases.ini" "Field 2" "Text" $(TEXT_SETUP_DATABASES_NAME_ACTIVEMQ)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Databases.ini" "Field 4" "Text" $(TEXT_SETUP_DATABASES_REPO_USER)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Databases.ini" "Field 6" "Text" $(TEXT_SETUP_DATABASES_REPO_USER_PASSWORD)   
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Databases.ini" "Field 8" "Text" $(TEXT_SETUP_DATABASES_GROUP_REPO_DB)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Databases.ini" "Field 9" "Text" $(TEXT_SETUP_DATABASES_NAME_REPOSITORY)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Databases.ini" "Field 11" "Text" $(TEXT_SETUP_DATABASES_REPO_USER)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Databases.ini" "Field 13" "Text" $(TEXT_SETUP_DATABASES_REPO_USER_PASSWORD)  

  ;!insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Daisy.ini" "Field 1" "Text" $(TEXT_SETUP_DAISY_INFO)
  ;!insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Daisy.ini" "Field 3" "Text" $(TEXT_SETUP_DAISY_GROUPTITLE)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Daisy.ini" "Field 1" "Text" $(TEXT_SETUP_DAISY_DATADIR_REPO)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Daisy.ini" "Field 3" "Text" $(TEXT_SETUP_DAISY_DATADIR_WIKI)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Daisy.ini" "Field 5" "Text" $(TEXT_SETUP_DAISY_REPO_ADMIN)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Daisy.ini" "Field 7" "Text" $(TEXT_SETUP_DAISY_REPO_ADMIN_PASSWORD)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Daisy.ini" "Field 9" "Text" $(TEXT_SETUP_MAIL_SMTP_SERVER)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Daisy.ini" "Field 11" "Text" $(TEXT_SETUP_MAIL_ADDRESS)
  
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Namespace.ini" "Field 1" "Text" $(TEXT_SETUP_DAISY_NAMESPACE_REPO_EXPLANATION)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Namespace.ini" "Field 2" "Text" $(TEXT_SETUP_DAISY_NAMESPACE_REPO)

  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Sites.ini" "Field 1" "Text" $(TEXT_SETUP_SITES_INFO)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Sites.ini" "Field 2" "Text" $(TEXT_SETUP_SITES_GROUPTITLE)
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Sites.ini" "Field 3" "Text" "$(TEXT_SETUP_SITES_SITENAME)"
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Sites.ini" "Field 4" "Text" "$(TEXT_SETUP_SITES_SITELANGUAGE)"
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Sites.ini" "Field 5" "Text" "$(TEXT_SETUP_SITES_SITE) 1"
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Sites.ini" "Field 8" "Text" "$(TEXT_SETUP_SITES_SITE) 2"
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Sites.ini" "Field 11" "Text" "$(TEXT_SETUP_SITES_SITE) 3"
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Sites.ini" "Field 14" "Text" "$(TEXT_SETUP_SITES_SITE) 4"

FunctionEnd

Function InitInterface

  Call CheckPrivileges
  Call InitDialogs
  Call InitSizeExternal

FunctionEnd

Function LaunchDaisy

  ;source: http://nsis.sourceforge.net/Open_link_in_new_browser_window

  Push "http://127.0.0.1:8888/daisy/"
  Pop $0

  Push $3 
  Push $2
  Push $1
  Push $0
  ReadRegStr $0 HKCR "http\shell\open\command" ""
# Get browser path
    DetailPrint $0
  StrCpy $2 '"'
  StrCpy $1 $0 1
  StrCmp $1 $2 +2 # if path is not enclosed in " look for space as final char
    StrCpy $2 ' '
  StrCpy $3 1
  loop:
    StrCpy $1 $0 1 $3
    DetailPrint $1
    StrCmp $1 $2 found
    StrCmp $1 "" found
    IntOp $3 $3 + 1
    Goto loop
 
  found:
    StrCpy $1 $0 $3
    StrCmp $2 " " +2
      StrCpy $1 '$1"'
 
  Pop $0
  Exec '$1 $0'
  Pop $1
  Pop $2
  Pop $3
FunctionEnd