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

Install type setting (current user/all users)

*/

;--------------------------------
;Macros

!macro GetDirExternal component

  ReadRegStr $R0 SHELL_CONTEXT "${APP_REGKEY_SETUP}" "${component} Path"
  
  ${if} ${FileExists} "$R0\${BIN_${component}}"

    ${if} $R0 != ""
      StrCpy $Path${component} $R0
    ${endif}
  
  ${endif}

!macroend

;--------------------------------
;Functions

Function InitUser

  ;Get directories from registry

  ReadRegStr $R0 SHELL_CONTEXT "${APP_REGKEY}" ""
  
  ${if} $R0 != ""
    StrCpy $INSTDIR $R0
  ${endif}

  !insertmacro GetDirExternal JAVA
  !insertmacro GetDirExternal MySQL
  
  ;Set directories in dialogs

  !insertmacro InitDialogExternalDir java
  !insertmacro InitDialogExternalDir jai
  !insertmacro InitDialogExternalDir mysql
  
FunctionEnd

;--------------------------------
;Page functions

Function PageUser

  ; set environment variable
  SetEnv::SetEnvVar "_EXIT_ERRORLEVEL" "true"

  WriteRegStr SHELL_CONTEXT ${APP_REGKEY_SETUP} "Setup Context" "Current user"
  ;Only show page if installing for all users is possible
  ${if} $AdminOrPowerUser == ${FALSE}
    Call InitUser
    Abort
  ${endif}
  
  !insertmacro MUI_HEADER_TEXT $(TEXT_USER_TITLE) $(TEXT_USER_SUBTITLE)
  !insertmacro MUI_INSTALLOPTIONS_DISPLAY "user.ini"

FunctionEnd

Function PageUserValidate
  
  !insertmacro MUI_INSTALLOPTIONS_READ $R0 "user.ini" "Field 2" "State"
  
  ${if} $R0 == "1"
    WriteRegStr SHELL_CONTEXT ${APP_REGKEY_SETUP} "Setup Context" "Current user"
    SetShellVarContext current
  ${else}
    WriteRegStr SHELL_CONTEXT ${APP_REGKEY_SETUP} "Setup Context" "All users"
    SetShellVarContext all
    StrCpy $AllUsersInstall ${TRUE}
  ${endif}
  
  Call InitUser
  
FunctionEnd
