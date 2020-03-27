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

Reinstall options and upgrade

*/

;--------------------------------
;macros

!macro DaisyRepositoryService action action_UC
  nsExec::Exec '"$dataDirLocation\service\${action}-daisy-repository-server-service.bat" 1> "$INSTDIR\daisyhome\install\log\${action}-daisy-repository-server-service.log" 2> "$INSTDIR\daisyhome\install\log\${action}-daisy-repository-server-service-error.log"'
  Pop $0
  ${if} $0 <> 0
    Marquee::stop
    MessageBox MB_OK "$(TEXT_SETUP_DAISY_FAILURE_${action_UC}_REPOSITORY_SERVICE)"
    ABORT "$(TEXT_SETUP_DAISY_FAILURE_${action_UC}_REPOSITORY_SERVICE)"
  ${endif}
!macroend

!macro DaisyWikiService action action_UC
  nsExec::Exec '"$wikiDirLocation\service\${action}-daisy-wiki-service.bat" 1> "$INSTDIR\daisyhome\install\log\${action}-daisy-wiki-service.log" 2> "$INSTDIR\daisyhome\install\log\${action}-daisy-wiki-service-error.log"'
  Pop $0
  ${if} $0 <> 0
    Marquee::stop
    MessageBox MB_OK "$(TEXT_SETUP_DAISY_FAILURE_${action_UC}_WIKI_SERVICE)"
    ABORT "$(TEXT_SETUP_DAISY_FAILURE_${action_UC}_WIKI_SERVICE)"
  ${endif}
!macroend

;--------------------------------
;Page functions

Function PageReinstall

  ;Check whether this version is already installed

  ReadRegStr $R0 SHELL_CONTEXT ${APP_REGKEY} "Version"

  ${if} $R0 != "${APP_VERSION}"
    ; TODO "Performing upgrade from Version $R0"
    ;!insertmacro DaisyRepositoryService stop STOP
    ;!insertmacro DaisyWikiService stop STOP
    Abort
  ${endif}

  !insertmacro MUI_HEADER_TEXT $(TEXT_REINSTALL_TITLE) $(TEXT_REINSTALL_SUBTITLE)
  !insertmacro MUI_INSTALLOPTIONS_DISPLAY "reinstall.ini"

FunctionEnd

Function PageReinstallValidate

  !insertmacro MUI_INSTALLOPTIONS_READ $R0 "reinstall.ini" "Field 2" "State"

  ${if} $R0 == "1"
    !insertmacro SelectSection ${SecRepo}
    !insertmacro SelectSection ${SecWiki}
    !insertmacro SelectSection ${DsyHelper}
    !insertmacro SelectSection ${SecCore}
    !insertmacro SelectSection ${SecAPI}
    !insertmacro SelectSection ${SecService}
  ${else}
    !insertmacro UnselectSection ${SecRepo}
    !insertmacro UnselectSection ${SecWiki}
    !insertmacro UnselectSection ${DsyHelper}
    !insertmacro UnselectSection ${SecCore}
    !insertmacro UnselectSection ${SecAPI}
    !insertmacro UnselectSection ${SecService}

  ${endif}

  Call InitUser

FunctionEnd