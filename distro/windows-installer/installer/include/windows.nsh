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

Checks for Windows version

*/

Function CheckWindows

  Push $R0
  Push $R1

  ;Check for Windows NT 5.0 or later (2000, XP, 2003 etc.)

  ReadRegStr $R0 HKLM "Software\Microsoft\Windows NT\CurrentVersion" "CurrentVersion"
  ${VersionCompare} $R0 "5.0" $R1
  
  ${if} $R1 == "2"
    MessageBox MB_OK|MB_ICONSTOP "${APP_NAME}-${APP_VERSION}.${APP_SERIES_KEY} only supports Windows 2000, XP, 2003 and later."
    Quit
  ${endif}
    
  Pop $R1
  Pop $R0

FunctionEnd

Function CheckPrivileges

  Push $R0

  UserInfo::GetAccountType
  Pop $R0
  
  ${if} $R0 == "Admin"
    StrCpy $AdminOrPowerUser ${TRUE}
  ${elseif} $R0 == "Power"
    StrCpy $AdminOrPowerUser ${TRUE}
  ${else}
    StrCpy $AdminOrPowerUser ${FALSE}
  ${endif}
  
  ${if} $AdminOrPowerUser != ${TRUE}
    MessageBox MB_OK|MB_ICONEXCLAMATION $(TEXT_NO_PRIVILEGES)
  ${endif}
  
  Pop $R0
  
FunctionEnd

Function un.SetShellContext

  Push $R0

  ;Set the correct shell context depending on whether Daisy has been installed for the current user or all users

  UserInfo::GetAccountType
  Pop $R0
  
  ${if} $R0 == "Admin"
    StrCpy $AdminOrPowerUser ${TRUE}
  ${endif}
  
  ${if} $R0 == "Power"
    StrCpy $AdminOrPowerUser ${TRUE}
  ${endif}
  
  ReadRegStr $R0 HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${SETUP_UNINSTALLER_KEY}" "UninstallString"
  
  ${if} $R0 != ""
    ${if} $AdminOrPowerUser == ${FALSE}
      MessageBox MB_OK|MB_ICONSTOP "${APP_NAME} has been installed for all users. Therefore you need Administrator or Power User Privileges to uninstall."
      Quit
    ${else}
      SetShellVarContext all
    ${endif}
  ${else}
    SetShellVarContext current
  ${endif}
  
  Pop $R0
  
FunctionEnd