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

!macro CreateInternetShortcut FILENAME URL ICONFILE ICONINDEX
WriteINIStr "${FILENAME}.url" "InternetShortcut" "URL" "${URL}"
WriteINIStr "${FILENAME}.url" "InternetShortcut" "IconFile" "${ICONFILE}"
WriteINIStr "${FILENAME}.url" "InternetShortcut" "IconIndex" "${ICONINDEX}"
!macroend

!macro CreateStartMenu

;Creating daisy program group and shortcuts

; main directory
CreateDirectory "$SMPROGRAMS\${APP_NAME} CMS"

; startup daisy
CreateShortCut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_LINK_DAISY_STARTUP).lnk" \
                 "$INSTDIR\batch\daisy-startup.bat" "" "$INSTDIR\resources\icons\daisy_32x32.ico" "" SW_SHOWNORMAL "" "$(TEXT_STARTMENU_LINK_DAISY_STARTUP)"

                 CreateShortCut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_WEB)\$(TEXT_STARTMENU_LINK_REPOSITORYSERVICE).lnk" \
                   "$INSTDIR\wrapper\bin\DaisyRepo.bat" "" "$INSTDIR\resources\icons\repository_32x32.ico" "" SW_SHOWMINIMIZED "" "$(TEXT_STARTMENU_LINK_REPOSITORYSERVICE)"

                   ; directory and links for startup
                   CreateDirectory "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_STARTUP)"
                   CreateShortCut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_STARTUP)\$(TEXT_STARTMENU_LINK_REPOSITORYSERVICE).lnk" \
                     "$dataDirLocation\service\daisy-repository-server-service.bat" "" "$INSTDIR\resources\icons\repository_32x32.ico" "" SW_SHOWMINIMIZED "" "$(TEXT_STARTMENU_LINK_REPOSITORYSERVICE)"
                     CreateShortCut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_STARTUP)\$(TEXT_STARTMENU_LINK_WIKISERVICE).lnk" \
                       "$wikiDirLocation\service\daisy-wiki-service.bat" "" "$INSTDIR\resources\icons\wiki_32x32.ico" "" SW_SHOWMINIMIZED "" "$(TEXT_STARTMENU_LINK_WIKISERVICE)"

                       ; directory and links for dos boxes
                       CreateDirectory "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_DOS_BOXES)"
                       CreateShortCut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_DOS_BOXES)\$(TEXT_STARTMENU_LINK_DAISY_BIN).lnk" \
                         "$INSTDIR\batch\dosbox-daisy-bin.bat" "" "$INSTDIR\resources\icons\backup_tool_32x32.ico" "" SW_SHOWMINIMIZED "" "$(TEXT_STARTMENU_LINK_DAISY_BIN)"
                         CreateShortCut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_DOS_BOXES)\$(TEXT_STARTMENU_LINK_DAISY_INSTALL).lnk" \
                           "$INSTDIR\batch\dosbox-daisy-install.bat" "" "$INSTDIR\resources\icons\add_site_32x32.ico" "" SW_SHOWMINIMIZED "" "$(TEXT_STARTMENU_LINK_DAISY_INSTALL)"
                           CreateDirectory "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_DOS_BOXES)\$(TEXT_STARTMENU_LINK_WRAPPER_SCRIPTS)"
                           CreateShortCut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_DOS_BOXES)\$(TEXT_STARTMENU_LINK_WRAPPER_SCRIPTS)\$(TEXT_STARTMENU_LINK_REPOSITORYSERVICE).lnk" \
                             "$INSTDIR\batch\dosbox-repository-wrapper-scripts.bat" "" "$INSTDIR\resources\icons\repository_32x32.ico" "" SW_SHOWMINIMIZED "" "$(TEXT_STARTMENU_LINK_REPOSITORYSERVICE)"
                             CreateShortCut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_DOS_BOXES)\$(TEXT_STARTMENU_LINK_WRAPPER_SCRIPTS)\$(TEXT_STARTMENU_LINK_WIKISERVICE).lnk" \
                               "$INSTDIR\batch\dosbox-wiki-wrapper-scripts.bat" "" "$INSTDIR\resources\icons\wiki_32x32.ico" "" SW_SHOWMINIMIZED "" "$(TEXT_STARTMENU_LINK_WIKISERVICE)"

                               ; main directory for service links
                               CreateDirectory "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_SERVICE)\$(TEXT_STARTMENU_FOLDER_SERVICE_INSTALL)"

                               ; subdirectory and links and for windows service installation  
                               CreateDirectory "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_SERVICE)\$(TEXT_STARTMENU_FOLDER_SERVICE_INSTALL)"
                               CreateShortCut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_SERVICE)\$(TEXT_STARTMENU_FOLDER_SERVICE_INSTALL)\$(TEXT_STARTMENU_LINK_REPOSITORYSERVICE).lnk"  \
                                 "$dataDirLocation\service\install-daisy-repository-server-service.bat" "" "$INSTDIR\resources\icons\repository_32x32.ico" "" SW_SHOWNORMAL "" "$(TEXT_STARTMENU_LINK_REPOSITORYSERVICE)"  
                                 CreateShortCut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_SERVICE)\$(TEXT_STARTMENU_FOLDER_SERVICE_INSTALL)\$(TEXT_STARTMENU_LINK_WIKISERVICE).lnk"  \
                                   "$wikiDirLocation\service\install-daisy-wiki-service.bat" "" "$INSTDIR\resources\icons\wiki_32x32.ico" "" SW_SHOWNORMAL "" "$(TEXT_STARTMENU_LINK_WIKISERVICE)"  

                                   ; subdirectory and links for windows service uninstallation
                                   CreateDirectory "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_SERVICE)\$(TEXT_STARTMENU_FOLDER_SERVICE_UNINSTALL)"
                                   CreateShortCut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_SERVICE)\$(TEXT_STARTMENU_FOLDER_SERVICE_UNINSTALL)\$(TEXT_STARTMENU_LINK_REPOSITORYSERVICE).lnk"  \
                                     "$dataDirLocation\service\uninstall-daisy-repository-server-service.bat" "" "$INSTDIR\resources\icons\repository_32x32.ico" "" SW_SHOWNORMAL "" "$(TEXT_STARTMENU_LINK_REPOSITORYSERVICE)"  
                                     CreateShortCut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_SERVICE)\$(TEXT_STARTMENU_FOLDER_SERVICE_UNINSTALL)\$(TEXT_STARTMENU_LINK_WIKISERVICE).lnk"  \
                                       "$wikiDirLocation\service\uninstall-daisy-wiki-service.bat" "" "$INSTDIR\resources\icons\wiki_32x32.ico" "" SW_SHOWNORMAL "" "$(TEXT_STARTMENU_LINK_WIKISERVICE)"  

                                       ; subdirectory and links and for windows service start
                                       CreateDirectory "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_SERVICE)\$(TEXT_STARTMENU_FOLDER_SERVICE_START)"
                                       CreateShortCut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_SERVICE)\$(TEXT_STARTMENU_FOLDER_SERVICE_START)\$(TEXT_STARTMENU_LINK_REPOSITORYSERVICE).lnk"  \
                                         "$dataDirLocation\service\start-daisy-repository-server-service.bat" "" "$INSTDIR\resources\icons\repository_32x32.ico" "" SW_SHOWNORMAL "" "$(TEXT_STARTMENU_LINK_REPOSITORYSERVICE)"  
                                         CreateShortCut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_SERVICE)\$(TEXT_STARTMENU_FOLDER_SERVICE_START)\$(TEXT_STARTMENU_LINK_WIKISERVICE).lnk"  \
                                           "$wikiDirLocation\service\start-daisy-wiki-service.bat" "" "$INSTDIR\resources\icons\wiki_32x32.ico" "" SW_SHOWNORMAL "" "$(TEXT_STARTMENU_LINK_WIKISERVICE)"  

                                           ; subdirectory and links for windows service stop
                                           CreateDirectory "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_SERVICE)\$(TEXT_STARTMENU_FOLDER_SERVICE_STOP)"
                                           CreateShortCut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_SERVICE)\$(TEXT_STARTMENU_FOLDER_SERVICE_STOP)\$(TEXT_STARTMENU_LINK_REPOSITORYSERVICE).lnk"  \
                                             "$dataDirLocation\service\stop-daisy-repository-server-service.bat" "" "$INSTDIR\resources\icons\repository_32x32.ico" "" SW_SHOWNORMAL "" "$(TEXT_STARTMENU_LINK_REPOSITORYSERVICE)"  
                                             CreateShortCut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_SERVICE)\$(TEXT_STARTMENU_FOLDER_SERVICE_STOP)\$(TEXT_STARTMENU_LINK_WIKISERVICE).lnk"  \
                                               "$wikiDirLocation\service\stop-daisy-wiki-service.bat" "" "$INSTDIR\resources\icons\wiki_32x32.ico" "" SW_SHOWNORMAL "" "$(TEXT_STARTMENU_LINK_WIKISERVICE)"  

                                               ; subdirectory and links and for windows service restart
                                               CreateDirectory "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_SERVICE)\$(TEXT_STARTMENU_FOLDER_SERVICE_RESTART)"
                                               CreateShortCut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_SERVICE)\$(TEXT_STARTMENU_FOLDER_SERVICE_RESTART)\$(TEXT_STARTMENU_LINK_REPOSITORYSERVICE).lnk"  \
                                                 "$dataDirLocation\service\restart-daisy-repository-server-service.bat" "" "$INSTDIR\resources\icons\repository_32x32.ico" "" SW_SHOWNORMAL "" "$(TEXT_STARTMENU_LINK_REPOSITORYSERVICE)"  
                                                 CreateShortCut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_SERVICE)\$(TEXT_STARTMENU_FOLDER_SERVICE_RESTART)\$(TEXT_STARTMENU_LINK_WIKISERVICE).lnk"  \
                                                   "$wikiDirLocation\service\restart-daisy-wiki-service.bat" "" "$INSTDIR\resources\icons\wiki_32x32.ico" "" SW_SHOWNORMAL "" "$(TEXT_STARTMENU_LINK_WIKISERVICE)"  

                                                   ; web directory and web links  
                                                   CreateDirectory "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_WEB)"
                                                   !insertmacro CreateInternetShortcut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_WEB)\$(TEXT_STARTMENU_LINK_DAISY_MAIN)" \
                                                     "http://daisycms.org/daisy" "$INSTDIR\resources\icons\daisy_32x32.ico" "0"

                                                     ; TODO ${WordReplace} ${APP_VERSION} "." "_" "+" $R6
                                                     !insertmacro CreateInternetShortcut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_WEB)\$(TEXT_STARTMENU_LINK_DAISY_DOCU)" \
                                                       "http://daisycms.org/daisydocs-2_2" "$INSTDIR\resources\icons\daisy_documentation_32x32.ico" "0"

                                                       !insertmacro CreateInternetShortcut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_WEB)\$(TEXT_STARTMENU_LINK_DAISY_MAILINGLIST)" \
                                                         "http://lists.cocoondev.org/mailman/listinfo/daisy" "$INSTDIR\resources\icons\mailing_list_32x32.ico" "0"

                                                         !insertmacro CreateInternetShortcut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_FOLDER_WEB)\$(TEXT_STARTMENU_LINK_DAISY_SVN)" \
                                                           "http://svn.daisycms.org/viewsvn/?root=daisy" "$INSTDIR\resources\icons\svn_32x32.ico" "0"

                                                           ; uninstall daisy
                                                           CreateShortCut "$SMPROGRAMS\${APP_NAME} CMS\$(TEXT_STARTMENU_LINK_DAISY_UNINSTALL).lnk" \
                                                             "$INSTDIR\Uninstall-Daisy.exe" "" "$INSTDIR\resources\icons\uninstall_32x32.ico" "" SW_SHOWMINIMIZED "" "$(TEXT_STARTMENU_LINK_DAISY_UNINSTALL)"

                                                             !macroend

                                                             !macro WriteEnvVarDaisyJava

                                                             ; create environment variable DAISY_HOME
                                                             Push "DAISY_HOME"
                                                             Push "$INSTDIR\daisyhome"
                                                             Call WriteEnvStr

                                                             ; create environment variable JAVA_HOME
                                                             Push "JAVA_HOME"
                                                             Push $JavaHome
                                                             Call WriteEnvStr

                                                             ; set environment variables for active installer process    
                                                             SetEnv::SetEnvVar "DAISY_HOME" "$INSTDIR\daisyhome"
                                                             SetEnv::SetEnvVar "JAVA_HOME" "$JavaHome"

                                                             !macroend

                                                             !macro WriteRegistryKeys

                                                             WriteRegStr SHELL_CONTEXT ${APP_REGKEY_SETUP} "Database Server" $DbServer
                                                             WriteRegStr SHELL_CONTEXT ${APP_REGKEY_SETUP} "Database Server Port" $DbPort
                                                             WriteRegStr SHELL_CONTEXT ${APP_REGKEY_SETUP} "Database ActiveMQ" $JmsDb
                                                             WriteRegStr SHELL_CONTEXT ${APP_REGKEY_SETUP} "Database User ActiveMQ" $JmsDbUser
                                                             WriteRegStr SHELL_CONTEXT ${APP_REGKEY_SETUP} "Database Repository" $RepoDb
                                                             WriteRegStr SHELL_CONTEXT ${APP_REGKEY_SETUP} "Database User Repository" $RepoDbUser

                                                             WriteRegStr SHELL_CONTEXT ${APP_REGKEY_SETUP} "Repository Data Directory" $dataDirLocation
                                                             WriteRegStr SHELL_CONTEXT ${APP_REGKEY_SETUP} "Wiki Data Directory" $wikiDirLocation

                                                             !macroend

                                                             !macro DialogSetupShow component

!insertmacro MUI_HEADER_TEXT $(TEXT_SETUP_${component}_TITLE) $(TEXT_SETUP_${component}_SUBTITLE)
  !insertmacro MUI_INSTALLOPTIONS_INITDIALOG "setup_${component}.ini"
  !insertmacro DialogExternalControl ${component}
  !insertmacro MUI_INSTALLOPTIONS_SHOW

  !macroend

  !macro ReadUserInputMySQL

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_MySQL.ini" "Field 3" "State" ; db exist 0/1
  Push $R1
  Pop $DbExist

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_MySQL.ini" "Field 6" "State" ; db server
  Push $R1
  Pop $DbServer

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_MySQL.ini" "Field 8" "State" ; port number
  Push $R1
  Pop $DbPort

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_MySQL.ini" "Field 10" "State" ; db root
  Push $R1
  Pop $DbRoot

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_MySQL.ini" "Field 12" "State" ; db root password
  Push $R1
  Pop $DbRootPassword

  !macroend

  !macro ReadUserInputMySQLDatabases

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Databases.ini" "Field 3" "State" ; activemq db
  Push $R1
  Pop $JmsDb

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Databases.ini" "Field 5" "State" ; activemq restricted user 
  Push $R1
  Pop $JmsDbUser

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Databases.ini" "Field 7" "State" ; activemq restricted password
  Push $R1
  Pop $JmsDbUserPassword

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Databases.ini" "Field 10" "State" ; repo db
  Push $R1
  Pop $RepoDb

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Databases.ini" "Field 12" "State" ; repo restricted user 
  Push $R1
  Pop $RepoDbUser

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Databases.ini" "Field 14" "State" ; repo restricted password
  Push $R1
  Pop $RepoDbUserPassword

  !macroend

  !macro ReadUserInputSiteCreation

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Sites.ini" "Field 6" "State" ; Site 1
  Push $R1
  Pop $Site_1

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Sites.ini" "Field 7" "State" ; SiteLanguage 1
  Push $R1
  Pop $SiteLanguage_1

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Sites.ini" "Field 9" "State" ; Site 2
  Push $R1
  Pop $Site_2

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Sites.ini" "Field 10" "State" ; SiteLanguage 2
  Push $R1
  Pop $SiteLanguage_2

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Sites.ini" "Field 12" "State" ; Site 3
  Push $R1
  Pop $Site_3

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Sites.ini" "Field 13" "State" ; SiteLanguage 3
  Push $R1
  Pop $SiteLanguage_3

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Sites.ini" "Field 15" "State" ; Site 4
  Push $R1
  Pop $Site_4

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Sites.ini" "Field 16" "State" ; SiteLanguage 4
  Push $R1
  Pop $SiteLanguage_4

  !macroend

  !macro ReadUserInputRepository

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Daisy.ini" "Field 2" "State"  ; repo directory
  Push $R1
  Pop $dataDirLocation

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Daisy.ini" "Field 4" "State"  ; wiki directory
  Push $R1
  Pop $wikiDirLocation

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Daisy.ini" "Field 6" "State"  ; repo admin
  Push $R1
  Pop $initialUserLogin

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Daisy.ini" "Field 8" "State"  ; repo admin password
  Push $R1
  Pop $initialUserPassword

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Daisy.ini" "Field 10" "State" ; e-mail from
  Push $R1
  Pop $mailFromAddress

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Daisy.ini" "Field 12" "State" ; smtp
  Push $R1
  Pop $smtpServer

  !macroend

  !macro ReadUserInputNamespace

  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "setup_Namespace.ini" "Field 3" "State"  ; repo namespace
  Push $R1
  Pop $RepoNamespace

  !macroend

  !macro WriteDatabaseCreationSQL

  FileOpen $8 $INSTDIR\daisyhome\install\daisy-create-databases.sql w

  FileWrite $8 "SET sql_mode='';$\r$\n"
  FileWrite $8 "CREATE DATABASE $RepoDb CHARACTER SET 'utf8';$\r$\n"
  FileWrite $8 "GRANT ALL ON $RepoDb.* TO $RepoDbUser@'%' IDENTIFIED BY '$RepoDbUserPassword';$\r$\n"
  FileWrite $8 "GRANT ALL ON $RepoDb.* TO $RepoDbUser@localhost IDENTIFIED BY '$RepoDbUserPassword';$\r$\n"
  FileWrite $8 "CREATE DATABASE $JmsDb CHARACTER SET 'utf8';$\r$\n"
  FileWrite $8 "GRANT ALL ON $JmsDb.* TO $JmsDbUser@'%' IDENTIFIED BY '$JmsDbUserPassword';$\r$\n"
  FileWrite $8 "GRANT ALL ON $JmsDb.* TO $JmsDbUser@localhost IDENTIFIED BY '$JmsDbUserPassword';$\r$\n"
  
  FileClose $8

!macroend

!macro WriteRepositoryInitProperties

  FileOpen $9 $INSTDIR\daisyhome\install\conf\daisy-repository-init.properties w
  FileWrite $9 "repo.dbName=$RepoDb$\r$\n"
  FileWrite $9 "repo.dbUser=$RepoDbUser$\r$\n"
  FileWrite $9 "repo.dbPass=$RepoDbUserPassword$\r$\n"
  FileWrite $9 "repo.adminUser=$initialUserLogin$\r$\n"
  FileWrite $9 "repo.adminPass=$initialUserPassword$\r$\n"
  ; Replace backslashes in path with slashes, otherwise the property cannot be read (backslashes could be escaped, too!)
  ${WordReplace} $dataDirLocation "\" "/" "+" $R8
  FileWrite $9 "repo.datadir=$R8$\r$\n"
  FileWrite $9 "repo.namespace=$RepoNamespace$\r$\n"
  FileWrite $9 "repo.mail.smtp.host=$smtpServer$\r$\n"
  FileWrite $9 "repo.mail.from=$mailFromAddress$\r$\n"  
  FileWrite $9 "repo.jms.dbName=$JmsDb$\r$\n"
  FileWrite $9 "repo.jms.dbUser=$JmsDbUser$\r$\n"
  FileWrite $9 "repo.jms.dbPass=$JmsDbUserPassword$\r$\n"

  ${if} $DbPort == ""
    FileWrite $9 "repo.db.mysql5.url=jdbc:mysql://$DbServer/$${xxdbname}?characterEncoding=UTF-8$\r$\n"
  ${else}
    FileWrite $9 "repo.db.mysql5.url=jdbc:mysql://$DbServer:$DbPort/$${xxdbname}?characterEncoding=UTF-8$\r$\n"
  ${endif}

  FileClose $9

!macroend

!macro WriteRepositoryServiceDosBox

  FileOpen $8 $INSTDIR\batch\dosbox-wiki-wrapper-scripts.bat w

  FileWrite $8 "echo off$\r$\n"
  FileWrite $8 "set DAISYWIKI_DATADIR=$wikiDirLocation$\r$\n"
  FileWrite $8 "cd %DAISYWIKI_DATADIR%/service$\r$\n$\r$\n"
  FileWrite $8 "start $\"Daisy Wiki Wrapper Scripts$\" cmd$\r$\n"
  
  FileClose $8

!macroend

!macro WriteDaisyStartupBatch

  FileOpen $8 $INSTDIR\batch\daisy-startup.bat w

  FileWrite $8 "echo on$\r$\n"
  FileWrite $8 "set JETTY_HOME=%DAISY_HOME%\daisywiki\jetty$\r$\n"
  FileWrite $8 "set WRAPPER_HOME=%DAISY_HOME%\..\wrapper$\r$\n$\r$\n"
  FileWrite $8 "REM ##############################################################$\r$\n"
  
  FileWrite $8 "REM # This script is part of the Daisy Windows Installer         #$\r$\n"
  FileWrite $8 "REM # Author: Andreas Deininger                                  #$\r$\n"
  FileWrite $8 "REM ##############################################################$\r$\n$\r$\n"
  FileWrite $8 "cd $\"%DAISY_HOME%\wrapper\bin$\"$\r$\n"
  FileWrite $8 "REM start the repository$\r$\n"
  FileWrite $8 "START /MIN $dataDirLocation\service\daisy-repository-server-service.bat$\r$\n"
  FileWrite $8 "REM the repository has to start up, so we give it some time$\r$\n"
  FileWrite $8 "set triesrepoconnectcount=0$\r$\n"
  FileWrite $8 ":repoconnectloop$\r$\n"
  FileWrite $8 "CALL ..\..\..\resources\sleep.bat 5$\r$\n"
  FileWrite $8 "CALL ..\..\..\resources\T4ePortPing.exe 127.0.0.1 9263$\r$\n"
  FileWrite $8 "REM continue if connection succeeded$\r$\n"
  FileWrite $8 "IF %ERRORLEVEL% == 0 goto startwiki$\r$\n"
  FileWrite $8 "set /a triesrepoconnectcount=%triesrepoconnectcount%+1$\r$\n"
  FileWrite $8 "REM give up after 6 tries$\r$\n"
  FileWrite $8 "if %triesrepoconnectcount% ==6 exit 2$\r$\n"
  FileWrite $8 "goto repoconnectloop$\r$\n"
  FileWrite $8 "REM start the daisy wiki$\r$\n"
  FileWrite $8 ":startwiki$\r$\n"
  FileWrite $8 "START /MIN $wikiDirLocation\service\daisy-wiki-service.bat$\r$\n"
  FileWrite $8 "REM the wiki has to start up, so we give it some time$\r$\n"
  FileWrite $8 "set trieswikiconnectcount=0$\r$\n"
  FileWrite $8 ":wikiconnectloop$\r$\n"
  FileWrite $8 "CALL ..\..\..\resources\sleep.bat 5$\r$\n"
  FileWrite $8 "CALL ..\..\..\resources\T4ePortPing.exe 127.0.0.1 8888$\r$\n"
  FileWrite $8 "REM continue if connection succeeded$\r$\n"
  FileWrite $8 "IF %ERRORLEVEL% == 0 exit 0$\r$\n"
  FileWrite $8 "set /a trieswikiconnectcount=%trieswikiconnectcount%+1$\r$\n"
  FileWrite $8 "REM give up after 6 tries$\r$\n"
  FileWrite $8 "if %trieswikiconnectcount% ==6 exit 5$\r$\n"
  FileWrite $8 "goto wikiconnectloop$\r$\n"
  FileWrite $8 "echo Daisy started up successfully$\r$\n"
  FileWrite $8 "pause$\r$\n"
  
  FileClose $8

!macroend

!macro WriteWikiInitProperties component

  FileOpen $8 $INSTDIR\daisyhome\install\conf\daisy-${component}-init.properties w
  
  FileWrite $8 "daisyLogin=$initialUserLogin$\r$\n"
  FileWrite $8 "daisyPassword=$initialUserPassword$\r$\n"
  FileWrite $8 "daisyUrl=http://127.0.0.1:9263$\r$\n"
  
   ${if} ${component} == "wikidata"
     ${WordReplace} $dataDirLocation "\" "/" "+" $R4
     FileWrite $8 "repoDataDir=$R4$\r$\n"
   ${endif}
  
  FileClose $8

!macroend

!macro AddWikiSite sitename sitenumber

  ${if} ${sitename} != ""  
   
    ClearErrors
    nsExec::Exec '"$INSTDIR\daisyhome\install\daisy-wiki-add-site.bat" --conf "$INSTDIR\daisyhome\install\conf\daisy-wiki-add-site${sitenumber}.properties" 1> "$INSTDIR\daisyhome\install\log\daisy-wiki-add-site${sitenumber}.log" 2> "$INSTDIR\daisyhome\install\log\daisy-wiki-add-site${sitenumber}-error.log"'
    Pop $0
    ${if} $0 <> 0
      MessageBox MB_OK "$(TEXT_SETUP_SITE${sitenumber}_FAILURE)"
      ABORT "$(TEXT_SETUP_SITE${sitenumber}_FAILURE)"
    ${endif}

  ${endif}

!macroend

!macro WriteWikiServiceDosBox

  FileOpen $8 $INSTDIR\batch\dosbox-repository-wrapper-scripts.bat w

  FileWrite $8 "echo off$\r$\n"
  FileWrite $8 "set DAISY_DATADIR=$dataDirLocation$\r$\n"
  FileWrite $8 "cd %DAISY_DATADIR%/service$\r$\n$\r$\n"
  FileWrite $8 "start $\"Daisy Repository Wrapper Scripts$\" cmd$\r$\n"
  
  FileClose $8

!macroend

!macro WriteWikiAddSiteProperties name language sitenumber

  ${if} ${name} != ""
    ClearErrors
    ${WordFind} $Site_${sitenumber} " " "E+1{" $R0 
    IfErrors end${sitenumber} 0
    MessageBox MB_OK "$(TEXT_SETUP_SITES_WARNING_NO_SPACE_IN_SITENAME${sitenumber}) $R0"
    ABORT "$(TEXT_SETUP_SITES_WARNING_NO_SPACE_IN_SITENAME${sitenumber})"
    end${sitenumber}:
    
    FileOpen $8 $INSTDIR\daisyhome\install\conf\daisy-wiki-add-site${sitenumber}.properties w
  
    FileWrite $8 "daisyLogin=$initialUserLogin$\r$\n"
    FileWrite $8 "daisyPassword=$initialUserPassword$\r$\n"
    FileWrite $8 "daisyUrl=http://127.0.0.1:9263$\r$\n"
    FileWrite $8 "siteName=${name}$\r$\n"
    ${if} ${language} == ""
      FileWrite $8 "siteLanguage=default$\r$\n"
    ${else}
      FileWrite $8 "siteLanguage=${language}$\r$\n"
    ${endif}
/*  TODO ${if} ${multilanguage setup} == ""
      FileWrite $8 "siteLanguage.1=${language2}$\r$\n"
      FileWrite $8 "siteLanguage.1=${language1}$\r$\n"
      FileWrite $8 "defaultReferenceLanguage=${referencelanguage}$\r$\n"
    ${endif}
*/    
    ${WordReplace} $wikiDirLocation "\" "/" "+" $R8
    FileWrite $8 "wikiDataDir=$R8$\r$\n"
  
    FileClose $8

  ${endif}
  
!macroend

!macro PingToDbServer

  ReadEnvStr $CommandProcessor COMSPEC
  ClearErrors

  ${if} $DbRootPassword == ""
    ; if 1==1 : cf. http://forums.winamp.com/showthread.php?postid=2245105
    ; using mysqladmin status instead of ping since with ping errorlevel is 0 if server is alive (even if access is denied)
    nsExec::Exec '"$CommandProcessor" /c if 1==1 "$INSTDIR\mysql\bin\${BIN_MySQLAdmin}" status -h$DbServer -P$DbPort -u$DbRoot 1> "$INSTDIR\daisyhome\install\log\ping-mysql-server.log" 2> "$INSTDIR\daisyhome\install\log\ping-mysql-server-error.log"'
  ${else}
    nsExec::Exec '"$CommandProcessor" /c if 1==1 "$INSTDIR\mysql\bin\${BIN_MySQLAdmin}" status -h$DbServer -P$DbPort -u$DbRoot -p$DbRootPassword 1> "$INSTDIR\daisyhome\install\log\ping-mysql-server.log" 2> "$INSTDIR\daisyhome\install\log\ping-mysql-server-error.log"'
  ${endif}    
  Pop $0
  ${if} $0 <> 0
    MessageBox MB_OK "$(TEXT_SETUP_DATABASES_CONNECTION_FAILURE)"
    ABORT "$(TEXT_SETUP_DATABASES_CONNECTION_FAILURE)"    
  ${endif}

!macroend

!macro CreateDatabases

  ReadEnvStr $CommandProcessor COMSPEC
  ClearErrors

  ${if} $DbExist <> 1
    ${if} $DbRootPassword == ""
      nsExec::Exec '"$CommandProcessor" /c if 1==1 "$INSTDIR\mysql\bin\${BIN_MySQL}" -h$DbServer -P$DbPort -u$DbRoot < "$INSTDIR\daisyhome\install\daisy-create-databases.sql" 1> "$INSTDIR\daisyhome\install\log\daisy-create-databases.log" 2> "$INSTDIR\daisyhome\install\log\daisy-create-databases-error.log"'
    ${else}
      nsExec::Exec '"$CommandProcessor" /c if 1==1 "$INSTDIR\mysql\bin\${BIN_MySQL}" -h$DbServer -P$DbPort -u$DbRoot -p$DbRootPassword < "$INSTDIR\daisyhome\install\daisy-create-databases.sql" 1> "$INSTDIR\daisyhome\install\log\daisy-create-databases.log" 2> "$INSTDIR\daisyhome\install\log\daisy-create-databases-error.log"'    
    ${endif}    
    Pop $0    
    ${if} $0 <> 0  
      MessageBox MB_OK "$(TEXT_SETUP_DATABASES_FAILURE)"
      ABORT "$(TEXT_SETUP_DATABASES_FAILURE)"
    ${else}
      MessageBox MB_OK "$(TEXT_SETUP_DATABASES_SUCCESS)"
    ${endif}
  ${endif}

!macroend

!macro CheckInput field message

  ; check whether input field has a value
  ${if} ${field} == ""
    MessageBox MB_OK "$(${message})"
    ABORT "$(${message})"
  ${endif}

!macroend

!macro CheckEmptyDirectory datadirectory message nocaller

  ; check whether data directory does not exist or is empty
  ${if} ${FileExists} "${datadirectory}\*.*"
  
    Push "${datadirectory}"
    Call isEmptyDir
    Pop $0
    StrCmp $0 0 0 ende${nocaller}
    ; directory is NOT empty
    MessageBox MB_OK "$(${message})"
    ABORT "$(${message})"    
    ende${nocaller}:
    
  ${endif}
  
!macroend

!macro InitialiseDaisyRepository
  nsExec::Exec '"$INSTDIR\daisyhome\install\daisy-repository-init.bat" -a -f -i "$INSTDIR\daisyhome\install\conf\daisy-repository-init.properties" 1> "$INSTDIR\daisyhome\install\log\initialise-daisy-repository-server.log" 2> "$INSTDIR\daisyhome\install\log\initialise-daisy-repository-server-error.log"'
  Pop $0
  ${if} $0 <> 0
    Marquee::stop
    MessageBox MB_OK "$(TEXT_SETUP_DAISY_FAILURE_REPOSITORY_INITIALIZATION)"
    ABORT "$(TEXT_SETUP_DAISY_FAILURE_REPOSITORY_INITIALIZATION)"
  ${endif}
  Marquee::stop
!macroend

!macro CreateDataServiceDir servicetype datadirectory
  ${if} ${servicetype} == "repository"
    nsExec::Exec '"$INSTDIR\daisyhome\install\daisy-service-install.bat" -r "${datadirectory}" 1> "$INSTDIR\daisyhome\install\log\create-daisy-repository-server-servicedir.log" 2> "$INSTDIR\daisyhome\install\log\create-daisy-repository-server-servicedir-error.log"'
    Pop $0
  ${if} $0 <> 0
    Marquee::stop
    MessageBox MB_OK "$(TEXT_SETUP_DAISY_FAILURE_CREATE_REPOSITORY_SERVICEDIR)"
    ABORT "$(TEXT_SETUP_DAISY_FAILURE_CREATE_REPOSITORY_SERVICEDIR)"
  ${endif}
  ${elseif} ${servicetype} == "wiki"
    nsExec::Exec '"$INSTDIR\daisyhome\install\daisy-service-install.bat" -w "${datadirectory}" 1> "$INSTDIR\daisyhome\install\log\create-daisy-wiki-servicedir.log" 2> "$INSTDIR\daisyhome\install\log\create-daisy-wiki-servicedir-error.log"'
    Pop $0
  ${if} $0 <> 0
    Marquee::stop
    MessageBox MB_OK "$(TEXT_SETUP_DAISY_FAILURE_CREATE_WIKI_SERVICEDIR)"
    ABORT "$(TEXT_SETUP_DAISY_FAILURE_CREATE_WIKI_SERVICEDIR)"
  ${endif}
  ${else}
    ; never should come here, TODO: issue warning
  ${endif}
  Marquee::stop
!macroend

!macro InstallDaisyRepositoryService
 ;check if repository service exists
 !insertmacro SERVICE "installed" "Daisy Repository" ""
  Pop $R0
  ${if} $R0 != "true"
    ; and run service installation script
    nsExec::Exec '"$dataDirLocation\service\install-daisy-repository-server-service.bat" 1> "$INSTDIR\daisyhome\install\log\install-daisy-repository-server-service.log" 2> "$INSTDIR\daisyhome\install\log\install-daisy-repository-server-service-error.log"'
    Pop $0
    ${if} $0 <> 0
      Marquee::stop
      MessageBox MB_OK "$(TEXT_SETUP_DAISY_FAILURE_INSTALL_REPOSITORY_SERVICE)"
      ABORT "$(TEXT_SETUP_DAISY_FAILURE_INSTALL_REPOSITORY_SERVICE)"
    ${endif}
  ${endif}
!macroend

!macro PingToRepository
  ;give repository some time to start up
  nsExec::Exec '"$INSTDIR\resources\sleep.bat" 15'
  waitagain:
  nsExec::Exec '"$INSTDIR\resources\sleep.bat" 5'
  ;ping repository port
  nsExec::Exec '"$INSTDIR\resources\T4ePortPing.exe" 127.0.0.1 9263'
  Pop $0
  ${if} $0 <> 0
    Marquee::stop
    MessageBox MB_YESNO $(TEXT_SETUP_DAISY_REPO_CONNECT_AGAIN) IDYES waitagain IDNO 0
    ABORT "$(TEXT_SETUP_DAISY_FAILURE_REPO_CONNECT)"
  ${endif}
!macroend

!macro InitialiseDaisyWiki
  ; step 1: wiki initialisation
  nsExec::Exec '"$INSTDIR\daisyhome\install\daisy-wiki-init.bat" --conf "$INSTDIR\daisyhome\install\conf\daisy-wiki-init.properties" 1> "$INSTDIR\daisyhome\install\log\initialise-daisy-wiki.log" 2> "$INSTDIR\daisyhome\install\log\initialise-daisy-wiki-error.log"'
  Pop $0
  ${if} $0 <> 0
    Marquee::stop
    MessageBox MB_OK "$(TEXT_SETUP_DAISY_FAILURE_WIKI_INITIALIZATION)"
    ABORT "$(TEXT_SETUP_DAISY_FAILURE_WIKI_INITIALIZATION)"
  ${endif}
  ; step 2: wikidata dir creation
  nsExec::Exec '"$INSTDIR\daisyhome\install\daisy-wikidata-init.bat" --conf "$INSTDIR\daisyhome\install\conf\daisy-wikidata-init.properties" --wikidata "$wikiDirLocation" 1> "$INSTDIR\daisyhome\install\log\create-daisywiki-datadir.log" 2> "$INSTDIR\daisyhome\install\log\create-daisywiki-datadir-error.log"'
  Pop $0
  ${if} $0 <> 0
    Marquee::stop
    MessageBox MB_OK "$(TEXT_SETUP_DAISY_FAILURE_WIKIDATA_INITIALIZATION)"
    ABORT "$(TEXT_SETUP_DAISY_FAILURE_WIKIDATA_INITIALIZATION)"
  ${endif}  
!macroend

!macro InstallDaisyWikiService
  ;check if wiki service exists     
  !insertmacro SERVICE "installed" "Daisy Wiki" ""
  Pop $R0
  ${if} $R0 != "true"
    ; and run service installation script
    nsExec::Exec '"$wikiDirLocation\service\install-daisy-wiki-service.bat" 1> "$INSTDIR\daisyhome\install\log\install-daisy-wiki-service.log" 2> "$INSTDIR\daisyhome\install\log\install-daisy-wiki-service-error.log"' $0
    Pop $0
    ${if} $0 <> 0
      Marquee::stop
      MessageBox MB_OK "$(TEXT_SETUP_DAISY_FAILURE_INSTALL_WIKI_SERVICE)"
      ABORT "$(TEXT_SETUP_DAISY_FAILURE_INSTALL_WIKI_SERVICE)"
    ${endif}
  ${endif}
!macroend

Function PageSetupMySQL

  !insertmacro DialogSetupShow MySQL
FunctionEnd

Function PageSetupMySQLValidate

  ; read input from installation form
  !insertmacro ReadUserInputMySQL
  
  ; check whether certain input fields are not empty
  !insertmacro CheckInput "$DbServer" "TEXT_SETUP_MYSQL_SERVER_NAME_EMPTY"
  !insertmacro CheckInput "$DbRoot" "TEXT_SETUP_MYSQL_SERVER_ROOT_EMPTY"

  ; ping to DBServer to check whether it is alive
  !insertmacro PingToDbServer
  
  ; remove dependency to MySQL service with remote database server
  ${if} $DbServer != "localhost"
  ${andif} $DbServer != "127.0.0.1"
    Push wrapper.ntservice.dependency.1=MySQL #text to be replaced
    Push "#wrapper.ntservice.dependency.1=MySQL" #replace with
    Push 0 #replace first occurrence
    Push 1 #replace only one occurrence
    Push $INSTDIR\daisyhome\wrapper\conf\daisy-repository-server-service-global.conf #file to replace in
    Call AdvReplaceInFile
    
    Push wrapper.ntservice.dependency.2=Netman #text to be replaced
    Push wrapper.ntservice.dependency.1=Netman #replace with
    Push 0 #replace first occurrence
    Push 1 #replace only one occurrence
    Push $INSTDIR\daisyhome\wrapper\conf\daisy-repository-server-service-global.conf #file to replace in
    Call AdvReplaceInFile
  ${endif}   
 
FunctionEnd

Function PageSetupTables
  !insertmacro DialogSetupShow Databases
FunctionEnd

Function PageSetupTablesValidate

  ; read input from installation form
  !insertmacro ReadUserInputMySQLDatabases

  ; check whether certain input fields are not empty
  !insertmacro CheckInput "$JmsDb" "TEXT_SETUP_DATABASES_NAME_ACTIVEMQ_EMPTY"
  !insertmacro CheckInput "$RepoDb" "TEXT_SETUP_DATABASES_NAME_REPOSITORY_EMPTY"
  !insertmacro CheckInput "$JmsDbUser" "TEXT_SETUP_DATABASES_JMS_USER_EMPTY"
  !insertmacro CheckInput "$RepoDbUser" "TEXT_SETUP_DATABASES_REPO_USER_EMPTY"
  
  Marquee::start /NOUNLOAD /face=Verdana /step=3 /top=0 /weight=400 $(TEXT_SETUP_DAISY_ACTION_ENVVAR)
  !insertmacro WriteEnvVarDaisyJava
  Marquee::stop

  ; create sql script and batch script
  !insertmacro WriteDatabaseCreationSQL
  
  ; create MySQL Databases
  !insertmacro CreateDatabases
  
  ; drop sql script since it contains password for restricted user
  delete $INSTDIR\daisyhome\install\daisy-create-databases.sql
    
FunctionEnd

Function PageSetupDaisy
  ;Hide back button complelety
  ;GetDlgItem $0 $HWNDPARENT 3
  ;ShowWindow $0 0
  
  ;Disable back button
  GetDlgItem $0 $HWNDPARENT 3
  EnableWindow $0 0 
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Daisy.ini" "Field 2" "State" "$INSTDIR\RepoData"
  !insertmacro MUI_INSTALLOPTIONS_WRITE "setup_Daisy.ini" "Field 4" "State" "$INSTDIR\WikiData"
  !insertmacro DialogSetupShow Daisy
FunctionEnd

Function PageSetupDaisyValidate

  ; read input from installation form
  !insertmacro ReadUserInputRepository

  ; check whether certain input fields are not empty
  !insertmacro CheckInput "$dataDirLocation" "TEXT_SETUP_DAISY_DATADIR_REPO_EMPTY"
  !insertmacro CheckInput "$wikiDirLocation" "TEXT_SETUP_DAISY_DATADIR_WIKI_EMPTY"
  !insertmacro CheckInput "$initialUserLogin" "TEXT_SETUP_DAISY_REPO_ADMIN_EMPTY"

  ; check whether data directories do not exist or are not empty
  !insertmacro CheckEmptyDirectory "$dataDirLocation" "TEXT_SETUP_DAISY_DATADIR_REPO_NOT_EMPTY" 1
  !insertmacro CheckEmptyDirectory "$wikiDirLocation" "TEXT_SETUP_DAISY_DATADIR_WIKI_NOT_EMPTY" 2
  
    ;Start Menu shortcut
  !insertmacro CreateStartMenu
  
FunctionEnd

Function PageSetupNamespace
  !insertmacro DialogSetupShow NameSpace
FunctionEnd

Function PageSetupNamespaceValidate

  ; read input from installation form
  !insertmacro ReadUserInputNamespace
  ; check whether certain input fields are not empty
  !insertmacro CheckInput "$RepoNamespace" "TEXT_SETUP_DAISY_NAMESPACE_REPO_EMPTY"
  
  ; check whether namespace for repo is valid
  ${if} $RepoNamespace !~ "^[a-zA-Z0-9_]{1,200}$"
    MessageBox MB_OK "$(TEXT_SETUP_DAISY_NAMESPACE_REPO_INVALID)"
    ABORT "$(TEXT_SETUP_DAISY_NAMESPACE_REPO_INVALID)"
  ${endif}
  
  !insertmacro WriteRegistryKeys

  ; create properties files for automated install
  !insertmacro WriteRepositoryInitProperties
  !insertmacro WriteWikiInitProperties wiki
  !insertmacro WriteWikiInitProperties wikidata

  ; repository initialisation and startup
  Marquee::start /NOUNLOAD /face=Verdana /step=3 /top=1 /weight=400 $(TEXT_SETUP_DAISY_ACTION_REPOSITORY_INITIALIZATION)
  !insertmacro InitialiseDaisyRepository
  Marquee::stop
  Marquee::start /NOUNLOAD /face=Verdana /step=3 /top=1 /weight=400 $(TEXT_SETUP_DAISY_ACTION_REPOSITORY_SERVICE)
  !insertmacro CreateDataServiceDir repository $dataDirLocation
  !insertmacro InstallDaisyRepositoryService
  !insertmacro DaisyRepositoryService start START
  ; entries in Start menu
  !insertmacro WriteRepositoryServiceDosBox
  !insertmacro WriteDaisyStartupBatch
  Marquee::stop
  Marquee::start /NOUNLOAD /face=Verdana /step=3 /top=1 /weight=400 $(TEXT_SETUP_DAISY_ACTION_CONNECT_TO_REPOSITORY)  
  !insertmacro PingToRepository
  
  ; wiki initialisation and startup
  Marquee::stop
  Marquee::start /NOUNLOAD /face=Verdana /step=3 /top=1 /weight=400 $(TEXT_SETUP_DAISY_ACTION_WIKI_INITIALIZATION)
  !insertmacro InitialiseDaisyWiki
  Marquee::stop
  Marquee::start /NOUNLOAD /face=Verdana /step=3 /top=1 /weight=400 $(TEXT_SETUP_DAISY_ACTION_WIKI_SERVICE)
  !insertmacro CreateDataServiceDir wiki $wikiDirLocation
  !insertmacro InstallDaisyWikiService
  !insertmacro DaisyWikiService start START
  !insertmacro WriteWikiServiceDosBox
  Marquee::stop
  MessageBox MB_OK "$(TEXT_SETUP_DAISY_SUCCESS)"
  
  ; drop property files since they contain password of the daisy admin
  delete $INSTDIR\daisyhome\install\conf\daisy-repository-init.properties
  delete $INSTDIR\daisyhome\install\conf\daisy-wiki-init.properties
  delete $INSTDIR\daisyhome\install\conf\daisy-wikidata-init.properties

FunctionEnd

Function PageSetupSites
  !insertmacro DialogSetupShow Sites
FunctionEnd

Function PageSetupSitesValidate

  ; read input from installation form
  !insertmacro ReadUserInputSiteCreation
  
  ; create properties-files for automated install
  !insertmacro WriteWikiAddSiteProperties $Site_1 $SiteLanguage_1 1
  !insertmacro WriteWikiAddSiteProperties $Site_2 $SiteLanguage_2 2
  !insertmacro WriteWikiAddSiteProperties $Site_3 $SiteLanguage_3 3
  !insertmacro WriteWikiAddSiteProperties $Site_4 $SiteLanguage_4 4

  ; check whether at least one site was given
  ${if} $Site_1 == ""
  ${andif} $Site_2 == ""
  ${andif} $Site_3 == ""
  ${andif} $Site_4 == ""
    MessageBox MB_OK "$(TEXT_SETUP_SITES_WARNING_NO_SITE)"
    ABORT "$(TEXT_SETUP_SITES_WARNING_NO_SITE)"
  ${endif}

  Marquee::start /NOUNLOAD /face=Verdana /step=3 /top=1 /weight=400 $(TEXT_SETUP_DAISY_ACTION_SITECREATION)
  
  ; add sites (this also checks whether the sitename contains spaces) 
  !insertmacro AddWikiSite $Site_1 1
  !insertmacro AddWikiSite $Site_2 2
  !insertmacro AddWikiSite $Site_3 3
  !insertmacro AddWikiSite $Site_4 4
   
  Marquee::stop
  
    ; drop property files since they contain password of the daisy admin
  delete $INSTDIR\daisyhome\install\conf\daisy-wiki-add-site1.properties
  delete $INSTDIR\daisyhome\install\conf\daisy-wiki-add-site2.properties
  delete $INSTDIR\daisyhome\install\conf\daisy-wiki-add-site3.properties
  delete $INSTDIR\daisyhome\install\conf\daisy-wiki-add-site4.properties
  delete $INSTDIR\daisyhome\install\conf\
  
FunctionEnd

Function isEmptyDir

  ; source: http://nsis.sourceforge.net/Check_if_dir_is_empty

  # Stack ->                    # Stack: <directory>
  Exch $0                       # Stack: $0
  Push $1                       # Stack: $1, $0
  FindFirst $0 $1 "$0\*.*"
  strcmp $1 "." 0 _notempty
    FindNext $0 $1
    strcmp $1 ".." 0 _notempty
      ClearErrors
      FindNext $0 $1
      IfErrors 0 _notempty
        FindClose $0
        Pop $1                  # Stack: $0
        StrCpy $0 1
        Exch $0                 # Stack: 1 (true)
        goto _end
     _notempty:
       FindClose $0
       Pop $1                   # Stack: $0
       StrCpy $0 0
       Exch $0                  # Stack: 0 (false)
  _end:
FunctionEnd

Function AdvReplaceInFile

; source: http://nsis.sourceforge.net/More_advanced_replace_text_in_file

Exch $0 ;file to replace in
Exch
Exch $1 ;number to replace after
Exch
Exch 2
Exch $2 ;replace and onwards
Exch 2
Exch 3
Exch $3 ;replace with
Exch 3
Exch 4
Exch $4 ;to replace
Exch 4
Push $5 ;minus count
Push $6 ;universal
Push $7 ;end string
Push $8 ;left string
Push $9 ;right string
Push $R0 ;file1
Push $R1 ;file2
Push $R2 ;read
Push $R3 ;universal
Push $R4 ;count (onwards)
Push $R5 ;count (after)
Push $R6 ;temp file name
 
  GetTempFileName $R6
  FileOpen $R1 $0 r ;file to search in
  FileOpen $R0 $R6 w ;temp file
   StrLen $R3 $4
   StrCpy $R4 -1
   StrCpy $R5 -1
 
loop_read:
 ClearErrors
 FileRead $R1 $R2 ;read line
 IfErrors exit
 
   StrCpy $5 0
   StrCpy $7 $R2
 
loop_filter:
   IntOp $5 $5 - 1
   StrCpy $6 $7 $R3 $5 ;search
   StrCmp $6 "" file_write2
   StrCmp $6 $4 0 loop_filter
 
StrCpy $8 $7 $5 ;left part
IntOp $6 $5 + $R3
IntCmp $6 0 is0 not0
is0:
StrCpy $9 ""
Goto done
not0:
StrCpy $9 $7 "" $6 ;right part
done:
StrCpy $7 $8$3$9 ;re-join
 
IntOp $R4 $R4 + 1
StrCmp $2 all file_write1
StrCmp $R4 $2 0 file_write2
IntOp $R4 $R4 - 1
 
IntOp $R5 $R5 + 1
StrCmp $1 all file_write1
StrCmp $R5 $1 0 file_write1
IntOp $R5 $R5 - 1
Goto file_write2
 
file_write1:
 FileWrite $R0 $7 ;write modified line
Goto loop_read
 
file_write2:
 FileWrite $R0 $R2 ;write unmodified line
Goto loop_read
 
exit:
  FileClose $R0
  FileClose $R1
 
   SetDetailsPrint none
  Delete $0
  Rename $R6 $0
  Delete $R6
   SetDetailsPrint both
 
Pop $R6
Pop $R5
Pop $R4
Pop $R3
Pop $R2
Pop $R1
Pop $R0
Pop $9
Pop $8
Pop $7
Pop $6
Pop $5
Pop $0
Pop $1
Pop $2
Pop $3
Pop $4
FunctionEnd
