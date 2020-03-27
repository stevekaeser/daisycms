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

Language file handling

*/

!macro LanguageString name text

  !ifndef "${name}"
    !define `${name}` `${text}`
  !endif
  
!macroend

!macro LanguageStringAdd lang_name name

  ;Takes a define and puts that into a language string
  LangString `${name}` `${LANG_${lang_name}}` `${${name}}`
  !undef `${name}`

!macroend

!macro LanguageStringCreateExternal component

  ${LanguageStringAdd} TEXT_EXTERNAL_${component}_TITLE
  ${LanguageStringAdd} TEXT_EXTERNAL_${component}_SUBTITLE
  ${LanguageStringAdd} TEXT_EXTERNAL_${component}_INFO_${SETUPTYPE_NAME}
  ${LanguageStringAdd} TEXT_EXTERNAL_${component}_${SETUPTYPE_NAME}
  ${LanguageStringAdd} TEXT_EXTERNAL_${component}_FOLDER
  ${LanguageStringAdd} TEXT_EXTERNAL_${component}_FOLDER_INFO
  ${LanguageStringAdd} TEXT_EXTERNAL_${component}_NONE
  ${LanguageStringAdd} TEXT_EXTERNAL_${component}_NOTFOUND
  
!macroend

!macro LanguageStringCreateSetup component

  ${LanguageStringAdd} TEXT_SETUP_${component}_TITLE
  ${LanguageStringAdd} TEXT_SETUP_${component}_SUBTITLE
  ${LanguageStringAdd} TEXT_SETUP_${component}_INFO
  ${LanguageStringAdd} TEXT_SETUP_${component}_GROUPTITLE
  
!macroend

!macro LanguageStringCreate lang_name

  ;Creates all language strings
  !insertmacro ReDef LanguageStringAdd '!insertmacro LanguageStringAdd "${lang_name}"'

  ${LanguageStringAdd} TEXT_NO_PRIVILEGES

  ${LanguageStringAdd} TEXT_WELCOME_${SETUPTYPE_NAME}

  ${LanguageStringAdd} TEXT_USER_TITLE
  ${LanguageStringAdd} TEXT_USER_SUBTITLE
  ${LanguageStringAdd} TEXT_USER_INFO  
  ${LanguageStringAdd} TEXT_USER_CURRENT
  ${LanguageStringAdd} TEXT_USER_ALL
  
  ${LanguageStringAdd} TEXT_REINSTALL_TITLE
  ${LanguageStringAdd} TEXT_REINSTALL_SUBTITLE
  ${LanguageStringAdd} TEXT_REINSTALL_INFO
  ${LanguageStringAdd} TEXT_REINSTALL_ENABLE
  
  ${LanguageStringAdd} TEXT_EXTERNAL_NOPRIVILEGES

  !insertmacro LanguageStringCreateExternal JAVA
  !insertmacro LanguageStringCreateExternal JAI
  !insertmacro LanguageStringCreateExternal MYSQL

  !insertmacro LanguageStringCreateSetup MYSQL
  !insertmacro LanguageStringCreateSetup DATABASES
  !insertmacro LanguageStringCreateSetup DAISY
  !insertmacro LanguageStringCreateSetup SITES

  ${LanguageStringAdd} TEXT_SETUP_MYSQL_SERVER_NAME
  ${LanguageStringAdd} TEXT_SETUP_MYSQL_SERVER_NAME_EMPTY
  ${LanguageStringAdd} TEXT_SETUP_DATABASES_NONEXISTING
  ${LanguageStringAdd} TEXT_SETUP_DATABASES_EXISTING
  ${LanguageStringAdd} TEXT_SETUP_MYSQL_SERVER_PORT
  ${LanguageStringAdd} TEXT_SETUP_MYSQL_SERVER_ROOT
  ${LanguageStringAdd} TEXT_SETUP_MYSQL_SERVER_ROOT_EMPTY
  ${LanguageStringAdd} TEXT_SETUP_MYSQL_SERVER_ROOTPASSWORD
  
  ${LanguageStringAdd} TEXT_SETUP_DATABASES_NAME_ACTIVEMQ
  ${LanguageStringAdd} TEXT_SETUP_DATABASES_NAME_ACTIVEMQ_EMPTY
  ${LanguageStringAdd} TEXT_SETUP_DATABASES_GROUP_REPO_DB
  ${LanguageStringAdd} TEXT_SETUP_DATABASES_NAME_REPOSITORY
  ${LanguageStringAdd} TEXT_SETUP_DATABASES_NAME_REPOSITORY_EMPTY
  ${LanguageStringAdd} TEXT_SETUP_DATABASES_JMS_USER
  ${LanguageStringAdd} TEXT_SETUP_DATABASES_JMS_USER_EMPTY
  ${LanguageStringAdd} TEXT_SETUP_DATABASES_JMS_USER_PASSWORD
  ${LanguageStringAdd} TEXT_SETUP_DATABASES_REPO_USER
  ${LanguageStringAdd} TEXT_SETUP_DATABASES_REPO_USER_EMPTY
  ${LanguageStringAdd} TEXT_SETUP_DATABASES_REPO_USER_PASSWORD
  ${LanguageStringAdd} TEXT_SETUP_DATABASES_SUCCESS
  ${LanguageStringAdd} TEXT_SETUP_DATABASES_FAILURE
  ${LanguageStringAdd} TEXT_SETUP_DATABASES_CONNECTION_FAILURE
  
  ${LanguageStringAdd} TEXT_SETUP_MAIL_SMTP_SERVER
  ${LanguageStringAdd} TEXT_SETUP_MAIL_ADDRESS

  ${LanguageStringAdd} TEXT_SETUP_NAMESPACE_TITLE
  ${LanguageStringAdd} TEXT_SETUP_NAMESPACE_SUBTITLE
  ${LanguageStringAdd} TEXT_SETUP_DAISY_NAMESPACE_REPO_EXPLANATION
  ${LanguageStringAdd} TEXT_SETUP_DAISY_NAMESPACE_REPO
  ${LanguageStringAdd} TEXT_SETUP_DAISY_NAMESPACE_REPO_EMPTY
  ${LanguageStringAdd} TEXT_SETUP_DAISY_NAMESPACE_REPO_INVALID
  
  ${LanguageStringAdd} TEXT_SETUP_DAISY_DATADIR_REPO
  ${LanguageStringAdd} TEXT_SETUP_DAISY_DATADIR_REPO_EMPTY
  ${LanguageStringAdd} TEXT_SETUP_DAISY_DATADIR_REPO_NOT_EMPTY
  ${LanguageStringAdd} TEXT_SETUP_DAISY_DATADIR_WIKI
  ${LanguageStringAdd} TEXT_SETUP_DAISY_DATADIR_WIKI_EMPTY
  ${LanguageStringAdd} TEXT_SETUP_DAISY_DATADIR_WIKI_NOT_EMPTY
  ${LanguageStringAdd} TEXT_SETUP_DAISY_REPO_ADMIN
  ${LanguageStringAdd} TEXT_SETUP_DAISY_REPO_ADMIN_EMPTY
  ${LanguageStringAdd} TEXT_SETUP_DAISY_REPO_ADMIN_PASSWORD
  ${LanguageStringAdd} TEXT_SETUP_DAISY_ACTION_ENVVAR
  ${LanguageStringAdd} TEXT_SETUP_DAISY_ACTION_REPOSITORY_INITIALIZATION
  ${LanguageStringAdd} TEXT_SETUP_DAISY_FAILURE_REPOSITORY_INITIALIZATION
  ${LanguageStringAdd} TEXT_SETUP_DAISY_ACTION_REPOSITORY_SERVICE
  ${LanguageStringAdd} TEXT_SETUP_DAISY_FAILURE_CREATE_REPOSITORY_SERVICEDIR
  ${LanguageStringAdd} TEXT_SETUP_DAISY_FAILURE_INSTALL_REPOSITORY_SERVICE
  ${LanguageStringAdd} TEXT_SETUP_DAISY_FAILURE_START_REPOSITORY_SERVICE
  ${LanguageStringAdd} TEXT_SETUP_DAISY_FAILURE_STOP_REPOSITORY_SERVICE  
  ${LanguageStringAdd} TEXT_SETUP_DAISY_ACTION_CONNECT_TO_REPOSITORY
  ${LanguageStringAdd} TEXT_SETUP_DAISY_REPO_CONNECT_AGAIN
  ${LanguageStringAdd} TEXT_SETUP_DAISY_FAILURE_REPO_CONNECT
  ${LanguageStringAdd} TEXT_SETUP_DAISY_ACTION_WIKI_INITIALIZATION
  ${LanguageStringAdd} TEXT_SETUP_DAISY_FAILURE_WIKI_INITIALIZATION
  ${LanguageStringAdd} TEXT_SETUP_DAISY_FAILURE_WIKIDATA_INITIALIZATION
  ${LanguageStringAdd} TEXT_SETUP_DAISY_ACTION_WIKI_SERVICE
  ${LanguageStringAdd} TEXT_SETUP_DAISY_FAILURE_CREATE_WIKI_SERVICEDIR
  ${LanguageStringAdd} TEXT_SETUP_DAISY_FAILURE_INSTALL_WIKI_SERVICE
  ${LanguageStringAdd} TEXT_SETUP_DAISY_FAILURE_START_WIKI_SERVICE
  ${LanguageStringAdd} TEXT_SETUP_DAISY_FAILURE_STOP_WIKI_SERVICE  
  ${LanguageStringAdd} TEXT_SETUP_DAISY_SUCCESS
  ${LanguageStringAdd} TEXT_SETUP_DAISY_ACTION_SITECREATION
  ${LanguageStringAdd} TEXT_SETUP_SITES_SITENAME
  ${LanguageStringAdd} TEXT_SETUP_SITES_SITELANGUAGE
  ${LanguageStringAdd} TEXT_SETUP_SITES_SITE
  ${LanguageStringAdd} TEXT_SETUP_SITES_WARNING_NO_SITE
  ${LanguageStringAdd} TEXT_SETUP_SITES_WARNING_NO_SPACE_IN_SITENAME1
  ${LanguageStringAdd} TEXT_SETUP_SITES_WARNING_NO_SPACE_IN_SITENAME2
  ${LanguageStringAdd} TEXT_SETUP_SITES_WARNING_NO_SPACE_IN_SITENAME3
  ${LanguageStringAdd} TEXT_SETUP_SITES_WARNING_NO_SPACE_IN_SITENAME4
  ${LanguageStringAdd} TEXT_SETUP_SITES_SUCCESS
  ${LanguageStringAdd} TEXT_SETUP_SITE1_FAILURE
  ${LanguageStringAdd} TEXT_SETUP_SITE2_FAILURE
  ${LanguageStringAdd} TEXT_SETUP_SITE3_FAILURE
  ${LanguageStringAdd} TEXT_SETUP_SITE4_FAILURE
  ${LanguageStringAdd} TEXT_SETUP_SITES_FAILURE_UNKNOWN
    
  !ifndef SETUPTYPE_BUNDLE
    ${LanguageStringAdd} TEXT_DOWNLOAD_FAILED_JAVA
    ${LanguageStringAdd} TEXT_DOWNLOAD_FAILED_MySQL
    ${LanguageStringAdd} TEXT_DOWNLOAD_FAILED_JAI
  !endif 
  
  ${LanguageStringAdd} TEXT_NOTINSTALLED_JAVA
  ${LanguageStringAdd} TEXT_NOTINSTALLED_JAI
  ${LanguageStringAdd} TEXT_NOTINSTALLED_MySQL
  ${LanguageStringAdd} TEXT_FINISH_BROWSE_DAISY
  ${LanguageStringAdd} TEXT_FINISH_DESKTOP
  ${LanguageStringAdd} TEXT_FINISH_WEBSITE
  
  ${LanguageStringAdd} UNTEXT_WELCOME

  ${LanguageStringAdd} TEXT_STARTMENU_LINK_DAISY_STARTUP
  ${LanguageStringAdd} TEXT_STARTMENU_FOLDER_WEB
  ${LanguageStringAdd} TEXT_STARTMENU_LINK_DAISY_MAIN
  ${LanguageStringAdd} TEXT_STARTMENU_LINK_DAISY_DOCU
  ${LanguageStringAdd} TEXT_STARTMENU_LINK_DAISY_SVN
  ${LanguageStringAdd} TEXT_STARTMENU_LINK_DAISY_MAILINGLIST
  ${LanguageStringAdd} TEXT_STARTMENU_FOLDER_STARTUP
  ${LanguageStringAdd} TEXT_STARTMENU_FOLDER_SERVICE
  ${LanguageStringAdd} TEXT_STARTMENU_FOLDER_SERVICE_INSTALL
  ${LanguageStringAdd} TEXT_STARTMENU_FOLDER_SERVICE_UNINSTALL
  ${LanguageStringAdd} TEXT_STARTMENU_FOLDER_SERVICE_START
  ${LanguageStringAdd} TEXT_STARTMENU_FOLDER_SERVICE_STOP
  ${LanguageStringAdd} TEXT_STARTMENU_FOLDER_SERVICE_RESTART
  ${LanguageStringAdd} TEXT_STARTMENU_LINK_WIKISERVICE
  ${LanguageStringAdd} TEXT_STARTMENU_LINK_REPOSITORYSERVICE
  ${LanguageStringAdd} TEXT_STARTMENU_LINK_DAISY_UNINSTALL
  ${LanguageStringAdd} TEXT_STARTMENU_FOLDER_DOS_BOXES
  ${LanguageStringAdd} TEXT_STARTMENU_LINK_DAISY_BIN
  ${LanguageStringAdd} TEXT_STARTMENU_LINK_DAISY_INSTALL
  ${LanguageStringAdd} TEXT_STARTMENU_LINK_WRAPPER_SCRIPTS
  

  ${LanguageStringAdd} TEXT_INSTALLER_SECTION_CORE
  ${LanguageStringAdd} TEXT_INSTALLER_SECTION_REPOSITORY
  ${LanguageStringAdd} TEXT_INSTALLER_SECTION_WIKI
  ${LanguageStringAdd} TEXT_INSTALLER_SECTION_SERVICE
  ${LanguageStringAdd} TEXT_INSTALLER_SECTION_API

  ${LanguageStringAdd} TEXT_UNINSTALLER_SECTION_REPODATA
  ${LanguageStringAdd} TEXT_UNINSTALLER_SECTION_WIKIDATA
  ${LanguageStringAdd} TEXT_UNINSTALLER_SECTION_SERVICE
  ${LanguageStringAdd} TEXT_UNINSTALLER_DESCRIPTION_REPODATA
  ${LanguageStringAdd} TEXT_UNINSTALLER_DESCRIPTION_WIKIDATA
  ${LanguageStringAdd} TEXT_UNINSTALLER_CONFIRM_REMOVAL_REPODATA
  ${LanguageStringAdd} TEXT_UNINSTALLER_CONFIRM_REMOVAL_WIKIDATA

  ${LanguageStringAdd} TEXT_INSTALLER_DESCRIPTION_CORE
  ${LanguageStringAdd} TEXT_INSTALLER_DESCRIPTION_REPOSITORY
  ${LanguageStringAdd} TEXT_INSTALLER_DESCRIPTION_WIKI
  ${LanguageStringAdd} TEXT_INSTALLER_DESCRIPTION_SERVICE
  ${LanguageStringAdd} TEXT_UNINSTALLER_DESCRIPTION_SERVICE
  ${LanguageStringAdd} TEXT_INSTALLER_DESCRIPTION_API

!macroend

!macro IncludeLang langname

  ;Include NSIS language file
  
  !insertmacro MUI_LANGUAGE "${langname}"
  
  !include "lang\${langname}.nsh"
  !include "lang\English.nsh" ;Use English for missing strings in translation
  
  !insertmacro LanguageStringCreate "${langname}"
  
!macroend