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

Detection functions for all components

*/

;--------------------------------
;Macros

!macro SearchJAVA rootkey

  ReadRegStr $VersionJAVA ${rootkey} "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ; To Do 
  ; Version checking
  
  ReadRegStr $JavaHome ${rootkey} "SOFTWARE\JavaSoft\Java Runtime Environment\$VersionJAVA" "JavaHome"
  
  ${if} $JavaHome != ""
    !insertmacro callfunc TrimBackslash $JavaHome $JavaHome ;Just in case it's installed in a root directory
    StrCpy $PathJAVA $JavaHome 
    StrCpy $PathJAVA "$PathJAVA\bin"
  ${endif}
  
!macroend

;--------------------------------
;Functions

Function SearchAll

  Call SearchJAVA
  Call SearchJAI
  Call SearchMySQL
  
FunctionEnd

Function TrimBackslash

  ;Trim a trailing backslash of a directory

  Exch $R0
  Push $R1
  
  StrCpy $R1 $R0 1 -1
  
  ${if} $R1 == "\"
    StrLen $R1 $R0
    IntOp $R1 $R1 - 1
    StrCpy $R0 $R0 $R1
  ${endif}
  
  Pop $R1
  Exch $R0
  
FunctionEnd

Function SearchJAVA

  ;Search where the Java Virtual Machine is installed
  
  ${unless} ${FileExists} "$PathJAVA\${BIN_JAVA}"
    !insertmacro SearchJAVA HKLM
  ${endif}
  
  ${unless} ${FileExists} "$PathJAVA\${BIN_JAVA}"
    StrCpy $PathJAVA ""
  ${endif}

FunctionEnd

Function SearchJAI
    
  ${if} ${FileExists} "$PathJAVA\..\lib\ext\${BIN_JAI}"
    StrCpy $PathJAI "$PathJAVA\lib\ext\${BIN_JAI}"
  ${else}
    StrCpy $PathJAI ""
  ${endif}
    

FunctionEnd

Function SearchMySQL

 ;Check whether MySQL 5.1 is installed
  ReadRegStr $PathMySQL HKLM "SOFTWARE\MySQL AB\MySQL Server 5.1" "Location"
  IfErrors MySQL50 0
  ${if} ${FileExists} "$PathMySQLbin\${BIN_MySQL}"
    StrCpy $PathMySQL "$PathmySQLbin"
    Goto Done
  ${else}
    StrCpy $PathMySQL "" 0
  ${endif}

 ;Check whether MySQL 5.0 is installed
  MySQL50:
  ClearErrors
  ReadRegStr $PathMySQL HKLM "SOFTWARE\MySQL AB\MySQL Server 5.0" "Location"
  IfErrors MySQL41 0
  ${if} ${FileExists} "$PathMySQLbin\${BIN_MySQL}"
    StrCpy $PathMySQL "$PathmySQLbin"
    Goto Done
  ${else}
    StrCpy $PathMySQL "" 0
  ${endif}
  
 ;Check whether MySQL 4.1 is installed
  MySQL41:
  ReadRegStr $PathMySQL HKLM "SOFTWARE\MySQL AB\MySQL Server 4.1" "Location"
  IfErrors Done 0
  ${if} ${FileExists} "$PathMySQLbin\${BIN_MySQL}"
    StrCpy $PathMySQL "$PathmySQLbin"
  ${else}
    StrCpy $PathMySQL "" 0
  ${endif}
  
  Done:
  ${unless} ${FileExists} "$PathMySQL\${BIN_MySQL}"
    StrCpy $PathMySQL ""
  ${endif}

FunctionEnd