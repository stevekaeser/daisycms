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

Declarations

*/

!include "settings.nsh"

;--------------------------------
;Defines based on settings

!ifndef SETUPTYPE_BUNDLE
  !define SETUPTYPE_NAME DOWNLOAD
!else
  !define SETUPTYPE_NAME INSTALL
!endif

;--------------------------------
;Standard header files

!include "MUI.nsh"
!include "LogicLib.nsh"
!include "FileFunc.nsh"
!include "WordFunc.nsh"
!insertmacro WordReplace
!insertmacro WordFind

!include "Sections.nsh"
;regular expressions
!include "include\NSISpcre.nsh"
!insertmacro REMatches

;--------------------------------
;Windows constants

!define SHCNE_ASSOCCHANGED 0x08000000
!define SHCNF_IDLIST 0x0000

;--------------------------------
;Reserve Files
;These files should come first in the compressed data (for faster GUI)

ReserveFile "${NSISDIR}\Plugins\UserInfo.dll"
ReserveFile "dialogs\user.ini"
ReserveFile "dialogs\external.ini"
ReserveFile "dialogs\externalJAI.ini"
!insertmacro MUI_RESERVEFILE_INSTALLOPTIONS

;--------------------------------
;Variables

Var AdminOrPowerUser
Var AllUsersInstall

Var JavaHome
Var PathJAVA
Var VersionJAVA
Var PathJAI
Var PathMySQL

Var SetupJAVA
Var SetupJAI
Var SetupMySQL

Var SizeJAVA
Var SizeJAI
Var SizeMySQL

Var CommandProcessor
Var DbServer
Var DbPort
Var DbRoot
Var DbRootPassword
Var JmsDb
Var JmsDbUser
Var JmsDbUserPassword
Var RepoDb
Var RepoDbUser
Var RepoDbUserPassword
Var RepoNamespace
Var DBExist
Var initialUserLogin
Var initialUserPassword
Var smtpServer
Var mailFromAddress
Var dataDirLocation
Var wikiDirLocation
Var Site_1
Var Site_2
Var Site_3
Var Site_4
Var SiteLanguage_1
Var SiteLanguage_2
Var SiteLanguage_3
Var SiteLanguage_4
Var ServiceInstall

; Uninstaller
;VAR RemoveDatabases 

;--------------------------------
;Generic defines

!define FALSE 0
!define TRUE 1
  
;--------------------------------
;Include standard functions

!insertmacro VersionCompare

;--------------------------------
;Macros  

!macro ReDef name value

  ;Redefine a pre-processor definition

  !ifdef `${name}`
    !undef `${name}`
  !endif

  !define `${name}` `${value}`

!macroend
  
!macro CallFunc function input var_output
  
  ;Calls a function that modifies a single value on the stack

  Push ${input}
    Call ${function}
  Pop ${var_output}

!macroend

;--------------------------------
;Daisy installer header files  

!include "include\windows.nsh"
!include "include\lang.nsh"
!include "include\gui.nsh"
!include "include\detection.nsh"
!include "include\filelists.nsh"
!define ALL_USERS
!include "include\writeEnvStr.nsh"
!include "include\servicelib.nsh"