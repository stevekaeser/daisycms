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

External Components: Java, JAI, MySQL

*/

;--------------------------------
;Macros

!macro SetComponentState var component

  ${if} ${var} == "1"
  
    StrCpy $Setup${component} ${TRUE}
    
    StrCpy $R1 $Size${component}
    
    ${if} $Path${component} == ""
      ;Add size of component itself
      IntOp $R1 $R1 + ${SIZE_${component}}
    ${endif}
    
    SectionSetSize ${External${component}} $R1
    
  ${else}
  
    StrCpy $Setup${component} ${FALSE}
    SectionSetSize ${External${component}} 0
    
  ${endif}

!macroend

!macro ExternalComponent component extension

  ;Action depending on type of installer
  
  ${if} $Setup${component} == ${TRUE}
  
    StrCpy $Path${component} "" ;A new one will be installed
  
    !ifndef SETUPTYPE_BUNDLE
      !insertmacro DownloadComponent ${component} ${extension}
    !else
      !insertmacro InstallComponent ${component} ${extension}
    !endif
    
  ${endif}

!macroend

!macro SetupComponent component extension

  ;Run the setup application for a component

  install_${component}:
      
    ${if} ${extension} == "exe"
      ExecWait '"$PLUGINSDIR\${component}Setup.${extension}"'
    ${else}
      ExecWait '"msiexec" /i $PLUGINSDIR\${component}Setup.${extension}'
    ${endif}

    ; check whether component was installed successfully
    Call Search${component}
    ; in case of installation failure: ask whether to retry installation or not
    ${if} $Path${component} == ""  
      MessageBox MB_YESNO|MB_ICONEXCLAMATION $(TEXT_NOTINSTALLED_${component}) IDYES install_${component}
    ${endif}
      
    ; delete installation component
    Delete "$PLUGINSDIR\${component}Setup.${extension}"
     
!macroend

!ifndef SETUPTYPE_BUNDLE

  !macro DownloadComponent component extension

    download_${component}:

      ;Download using HTTP
      NSISdl::download "${DOWNLOAD_${component}}" "$PLUGINSDIR\${component}Setup.${extension}"
      Pop $R0
 
      ${if} $R0 != "success"
        ;Download failed
        MessageBox MB_YESNO|MB_ICONEXCLAMATION "$(TEXT_DOWNLOAD_FAILED_${component}) ($R0)" IDYES download_${component}
        Goto noinstall_${component}
      ${endif}
      
      !insertmacro SetupComponent ${component} ${extension}
      
    noinstall_${component}:

  !macroend

!else

  !macro InstallComponent component extension

    ; extract
    File /oname=$PLUGINSDIR\${component}Setup.${extension} "${FILES_BINARIES_BUNDLE}\${INSTALL_${component}}"
    
    ; and install
    !insertmacro SetupComponent ${component} ${extension}
    
  !macroend

!endif

!macro DialogExternalControl component

  ;Enable/disable the DirRequest control
  !insertmacro MUI_INSTALLOPTIONS_READ $R0 "external_${component}.ini" "Field 3" "State"
  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "external_${component}.ini" "Field 4" "HWND"
  EnableWindow $R1 $R0
  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "external_${component}.ini" "Field 4" "HWND2"
  EnableWindow $R1 $R0

!macroend

!macro DialogExternalShow component

  !insertmacro MUI_HEADER_TEXT $(TEXT_EXTERNAL_${component}_TITLE) $(TEXT_EXTERNAL_${component}_SUBTITLE)
  !insertmacro MUI_INSTALLOPTIONS_INITDIALOG "external_${component}.ini"
  !insertmacro DialogExternalControl ${component}
  !insertmacro MUI_INSTALLOPTIONS_SHOW

!macroend

!macro DialogExternalValidate component

  Push $R0
  Push $R1
  
  ;Next button pressed?
  !insertmacro MUI_INSTALLOPTIONS_READ $R0 "external_${component}.ini" "Settings" "State"
  ${if} $R0 != "0"
    !insertmacro DialogExternalControl ${component}
    Abort
  ${endif}
  
  ;Download?
  !insertmacro MUI_INSTALLOPTIONS_READ $R0 "external_${component}.ini" "Field 2" "State"
  !insertmacro SetComponentState $R0 ${component}
  
  ;Folder?
  !insertmacro MUI_INSTALLOPTIONS_READ $R0 "external_${component}.ini" "Field 3" "State"
  
  ${if} $R0 == "1"
    !insertmacro MUI_INSTALLOPTIONS_READ $R0 "external_${component}.ini" "Field 4" "State"
    ${unless} ${FileExists} "$R0\${BIN_${component}}"
      MessageBox MB_OK|MB_ICONEXCLAMATION $(TEXT_EXTERNAL_${component}_NOTFOUND)
      Abort
    ${endif}
    StrCpy $Path${component} $R0
  ${endif}

  Pop $R1
  Pop $R0

!macroend

;--------------------------------
;Sections

Section -JAVA ExternalJAVA
  !insertmacro ExternalComponent JAVA exe
SectionEnd

Section -JAI ExternalJAI
  !insertmacro ExternalComponent JAI exe
SectionEnd

Section -MySQL ExternalMySQL
  !insertmacro ExternalComponent MySQL msi
SectionEnd

;--------------------------------
;Functions

Function InitSizeExternal

  ;Get sizes of external component installers
  
  SectionGetSize ${ExternalJAVA} $SizeJAVA
  SectionGetSize ${ExternalJAVA} $SizeJAI
  SectionGetSize ${ExternalMySQL} $SizeMySQL
  
  !ifndef SETUPTYPE_BUNDLE
    ;Add download size
    IntOp $SizeJAVA $SizeJAVA + ${SIZE_DOWNLOAD_JAVA}
    IntOp $SizeJAI $SizeJAI + ${SIZE_DOWNLOAD_JAI}
    IntOp $SizeMySQL $SizeMySQL + ${SIZE_DOWNLOAD_MySQL}
  !endif
  
FunctionEnd

;--------------------------------
;Page functions

Function PageExternalJAVA
  !insertmacro DialogExternalShow JAVA
FunctionEnd

Function PageExternalJAVAValidate
  !insertmacro DialogExternalValidate JAVA
FunctionEnd

Function PageExternalJAI
  ${if} $PATHJAI != ""
    ;JAI is already installed, so we skip the installer page
    Abort
  ${endif}
  !insertmacro DialogExternalShow JAI
FunctionEnd

Function PageExternalJAIValidate
  !insertmacro MUI_INSTALLOPTIONS_READ $R0 "external_JAI.ini" "Field 2" "State"
  ${if} $R0 != "0"
    ;JAI will be installed
    !insertmacro DialogExternalValidate JAI
  ${endif}
FunctionEnd

Function PageExternalMySQL
  !insertmacro DialogExternalShow MySQL
FunctionEnd

Function PageExternalMySQLValidate
  !insertmacro DialogExternalValidate MySQL
FunctionEnd