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

Uninstaller

*/

;--------------------------------
;Sections

Section "un.$(TEXT_UNINSTALLER_SECTION_SERVICE)" UnSecService

  SectionSetText SecWiki $(TEXT_UNINSTALLER_SECTION_SERVICE)
  
  ;remove Daisy Windows services

  ReadRegStr $R0 SHELL_CONTEXT ${APP_REGKEY_SETUP} "Wiki Data Directory"
  ${if} $R0 != ""
    nsExec::Exec '"$R0\service\uninstall-daisy-wiki-service.bat" 2> "$INSTDIR\daisy-uninstall-error.log"'
  ${endif}

  ReadRegStr $R0 SHELL_CONTEXT ${APP_REGKEY_SETUP} "Repository Data Directory"
  ${if} $R0 != ""
    nsExec::Exec '"$R0\service\uninstall-daisy-repository-server-service.bat" 2>> "$INSTDIR\daisy-uninstall-error.log"'
  ${endif}
  
SectionEnd

SectionGroup /e "!un.$(TEXT_INSTALLER_SECTION_CORE)" un.SecCore

  Section "un.$(TEXT_INSTALLER_SECTION_REPOSITORY)" un.SecRepo
    SectionIn RO
    SectionSetText SecCore $(TEXT_INSTALLER_SECTION_CORE)  
    SectionSetText SecRepo $(TEXT_INSTALLER_SECTION_REPOSITORY)

    ;stop windows services if they exist
    !undef UN
    !define UN "un."

    !insertmacro SERVICE "installed" "Daisy Wiki" "stop"
    !insertmacro SERVICE "installed" "Daisy Repository" "stop"

    ;Daisy repository server
    RmDir /r /rebootok "$INSTDIR\daisyhome\repository-server"
 
  SectionEnd

  Section "un.$(TEXT_INSTALLER_SECTION_WIKI)" un.SecWiki

    ;Daisy wiki
    SectionIn RO
    SectionSetText un.SecWiki $(TEXT_INSTALLER_SECTION_WIKI)
    SetOutPath "$INSTDIR\daisyhome\daisywiki"
    RmDir /r /rebootok "$INSTDIR\daisyhome\daisywiki"

  SectionEnd

  Section -un.DaisyHelper un.DsyHelper

    ;remove Daisy binaries
    !insertmacro FileListDaisyBatch Delete "$INSTDIR\daisyhome\bin\"
    RMDir /REBOOTOK "$INSTDIR\daisyhome\bin"
    
    ;remove Daisy installation files
    !insertmacro FileListDaisyBatch Delete "$INSTDIR\daisyhome\install\"
    ;remove configuration and log files
    RmDir /r /REBOOTOK "$INSTDIR\daisyhome\install\conf"
    RMDir "$INSTDIR\daisyhome\install\conf"
    RmDir /r /REBOOTOK  "$INSTDIR\daisyhome\install\log"
    RMDir /REBOOTOK "$INSTDIR\daisyhome\install\log"
    RMDir /REBOOTOK "$INSTDIR\daisyhome\install"
    
    ;remove batch files for dos boxes
    !insertmacro FileListDosBoxes Delete "$INSTDIR\batch\"
    RMDir /REBOOTOK "$INSTDIR\batch"
    Delete /REBOOTOK "$INSTDIR\batch\dosbox-repository-wrapper-scripts.bat"
    Delete /REBOOTOK "$INSTDIR\batch\dosbox-wiki-wrapper-scripts.bat"
    Delete /REBOOTOK "$INSTDIR\batch\daisy-startup.bat"
    ;remove Daisy library files
    RmDir /r /REBOOTOK "$INSTDIR\daisyhome\lib"

    ;Remove Daisy custom stuff
    ; sleep.bat
    Delete /REBOOTOK "$INSTDIR\resources\sleep.bat"
    ; ToolsForEver Command Line Tools
    Delete /REBOOTOK "$INSTDIR\resources\T4EPortPing.exe"
    ;Icons
    !insertmacro FileListDaisyIcons Delete "$INSTDIR\resources\icons\"
    ;remove resources directory + subdirectory for icons
    RMDir /REBOOTOK "$INSTDIR\resources\icons"
    RMDir /REBOOTOK "$INSTDIR\resources"

    ; Wrapper Scripts
    !insertmacro FileListWrapperBin Delete "$INSTDIR\daisyhome\wrapper\"
    RMDir /REBOOTOK "$INSTDIR\daisyhome\wrapper\bin"
    !insertmacro FileListWrapperConf Delete "$INSTDIR\daisyhome\wrapper\"
    RMDir /REBOOTOK "$INSTDIR\daisyhome\wrapper\conf"
    !insertmacro FileListWrapperLib Delete "$INSTDIR\daisyhome\wrapper\"
    RMDir /REBOOTOK "$INSTDIR\daisyhome\wrapper\lib"
    !insertmacro FileListWrapperLog Delete "$INSTDIR\daisyhome\wrapper\"
    RMDir /REBOOTOK "$INSTDIR\daisyhome\wrapper\log"  
    !insertmacro FileListWrapperService Delete "$INSTDIR\daisyhome\wrapper\"
    RMDir /REBOOTOK "$INSTDIR\daisyhome\wrapper\service"
    RMDir /REBOOTOK "$INSTDIR\daisyhome\wrapper"

    ;MySQL client (binary, needed for creation of databases on other host)
    !insertmacro FileListMySQL Delete "$INSTDIR\mysql\"
    RMDir /REBOOTOK "$INSTDIR\mysql\bin"
    RMDir /REBOOTOK "$INSTDIR\mysql"

  SectionEnd

SectionGroupEnd

Section "un.$(TEXT_INSTALLER_SECTION_API)" un.SecAPI
  
    ;Daisy API Docs
    SectionSetText SecWiki $(TEXT_INSTALLER_SECTION_API)
    RMDir /r /REBOOTOK "$INSTDIR\apidocs"

SectionEnd

Section /o "un.$(TEXT_UNINSTALLER_SECTION_REPODATA)" un.SecRepoData
  
  ;Daisy API Docs
  SectionSetText SecWiki $(TEXT_UNINSTALLER_SECTION_REPODATA)

  ReadRegStr $R0 SHELL_CONTEXT ${APP_REGKEY_SETUP} "Repository Data Directory"

  ${if} $R0 != ""
    MessageBox MB_YESNO $(TEXT_UNINSTALLER_CONFIRM_REMOVAL_REPODATA) IDYES 0 IDNO skipremoverepodir
    RMDir /r /REBOOTOK "$R0"
    skipremoverepodir:
  ${endif}

SectionEnd

Section /o "un.$(TEXT_UNINSTALLER_SECTION_WIKIDATA)" un.SecWikiData
  
  ;Daisy API Docs
  SectionSetText SecWiki $(TEXT_UNINSTALLER_SECTION_WIKIDATA)

  ReadRegStr $R0 SHELL_CONTEXT ${APP_REGKEY_SETUP} "Wiki Data Directory"
  ${if} $R0 != ""
    MessageBox MB_YESNO $(TEXT_UNINSTALLER_CONFIRM_REMOVAL_WIKIDATA) IDYES 0 IDNO skipremovewikidir
    RMDir /r /REBOOTOK "$R0"
    skipremovewikidir:
  ${endif}

SectionEnd

/*
Section /o "un.$(TEXT_UNINSTALLER_SECTION_MYSQLDATABASES) un.SecMySQLDb
  
  ;remove MySQL Databases
  StrCpy $RemoveDatabases ${TRUE}
  ; TODO Implementation of that feature

SectionEnd
*/
  
Section -un.Cleanup un.SecCleanup 
    
  ;Start menu entries
  RmDir /R /REBOOTOK  "$SMPROGRAMS\${APP_NAME} CMS"

  ;Shortcut on desktop
  Delete "$DESKTOP\${APP_NAME} CMS.lnk"

  ;uninstaller itself
  Delete "$INSTDIR\${SETUP_UNINSTALLER}"
  
  ;remove installation directory if empty
  SetOutPath "$PROGRAMFILES"
  RMDir "$INSTDIR"
  
  ;Remove registry keys
  DeleteRegKey SHELL_CONTEXT "${APP_REGKEY_SETUP}"
  DeleteRegKey SHELL_CONTEXT "${APP_REGKEY}"
  DeleteRegKey SHELL_CONTEXT "Software\Microsoft\Windows\CurrentVersion\Uninstall\${SETUP_UNINSTALLER_KEY}"
  
  ;Remove environment variables
  Push "DAISY_HOME"
  Call un.DeleteEnvStr
  
  Push "DAISY_DATADIR"
  Call un.DeleteEnvStr

  Push "DAISYWIKI_DATADIR"
  Call un.DeleteEnvStr

  Push "JETTY_HOME"
  Call un.DeleteEnvStr

  Push "WRAPPER_HOME"
  Call un.DeleteEnvStr

SectionEnd

;--------------------------------
; define description for installer sections

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN

!insertmacro MUI_DESCRIPTION_TEXT ${SecCore} $(TEXT_INSTALLER_DESCRIPTION_CORE)
!insertmacro MUI_DESCRIPTION_TEXT ${SecRepo} $(TEXT_INSTALLER_DESCRIPTION_REPOSITORY)
!insertmacro MUI_DESCRIPTION_TEXT ${SecWiki} $(TEXT_INSTALLER_DESCRIPTION_WIKI)
!insertmacro MUI_DESCRIPTION_TEXT ${SecService} $(TEXT_INSTALLER_DESCRIPTION_SERVICE)
!insertmacro MUI_DESCRIPTION_TEXT ${SecAPI} $(TEXT_INSTALLER_DESCRIPTION_API)

!insertmacro MUI_FUNCTION_DESCRIPTION_END

; define description for uninstaller sections

!insertmacro MUI_UNFUNCTION_DESCRIPTION_BEGIN

!insertmacro MUI_DESCRIPTION_TEXT ${un.SecCore} $(TEXT_INSTALLER_DESCRIPTION_CORE)
!insertmacro MUI_DESCRIPTION_TEXT ${un.SecRepo} $(TEXT_INSTALLER_DESCRIPTION_REPOSITORY)
!insertmacro MUI_DESCRIPTION_TEXT ${un.SecWiki} $(TEXT_INSTALLER_DESCRIPTION_WIKI)
!insertmacro MUI_DESCRIPTION_TEXT ${un.SecRepoData} $(TEXT_UNINSTALLER_DESCRIPTION_REPODATA)
!insertmacro MUI_DESCRIPTION_TEXT ${un.SecWikiData} $(TEXT_UNINSTALLER_DESCRIPTION_WIKIDATA)
!insertmacro MUI_DESCRIPTION_TEXT ${UnSecService} $(TEXT_UNINSTALLER_DESCRIPTION_SERVICE)

!insertmacro MUI_UNFUNCTION_DESCRIPTION_END
