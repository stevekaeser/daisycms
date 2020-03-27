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

NSIS Script - Daisy 2.0 Installer for Win32
Author: Andreas Deininger
based on LyX 1.4 Installer, from Joost Verburg, Angus Leeming, Uwe St�hr
Requires NSIS 2.16 or later

Licence details for the installer scripts can be at
http://daisycms.org/daisy/44

*/
 
!include "include\declarations.nsh"

OutFile "${SETUP_EXE}"

;--------------------------------
;Functions

Function .onInit
  !insertmacro MUI_LANGDLL_DISPLAY
  Call CheckWindows
  Call SearchAll
FunctionEnd

Function un.onInit
  !insertmacro MUI_UNGETLANGUAGE
  Call un.SetShellContext
FunctionEnd

;--------------------------------
;Components

!include "components\core.nsh"
!include "components\user.nsh"
!include "components\reinstall.nsh"
!include "components\external.nsh"
!include "components\setup.nsh"
!include "components\configure.nsh"
!include "components\uninstall.nsh"
