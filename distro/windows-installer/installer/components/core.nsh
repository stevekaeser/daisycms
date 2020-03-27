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

Daisy Core, libraries, resources

*/

;--------------------------------
;Sections

SectionGroup /e "!$(TEXT_INSTALLER_SECTION_CORE)" SecCore
  
  ;Install and register the core Daisy files
  Section $(TEXT_INSTALLER_SECTION_REPOSITORY) SecRepo
    SectionIn RO
    SectionSetText SecCore $(TEXT_INSTALLER_SECTION_CORE)  
    SectionSetText SecRepo $(TEXT_INSTALLER_SECTION_REPOSITORY)
    
    ;Daisy repository server
    SetOutPath "$INSTDIR\daisyhome\repository-server"
    File /r "${FILES_DAISY}\repository-server\*"

  SectionEnd
    
  Section $(TEXT_INSTALLER_SECTION_WIKI) SecWiki
  
    ;Daisy wiki
    SectionIn RO
    SectionSetText SecWiki $(TEXT_INSTALLER_SECTION_WIKI)
    SetOutPath "$INSTDIR\daisyhome\daisywiki"
    File /r "${FILES_DAISY}\daisywiki\*.*"

  SectionEnd
  
  Section -DaisyHelper DsyHelper

    InitPluginsDir
  
    ;Daisy binaries
    SetOutPath "$INSTDIR\daisyhome\bin"
    !insertmacro FileListDaisyBatch File "${FILES_DAISY}\bin\"

    ;Daisy installation files
    SetOutPath "$INSTDIR\daisyhome\install"
    !insertmacro FileListDaisyBatch File "${FILES_DAISY}\install\"
    SetOutPath "$INSTDIR\daisyhome\install\conf"
    SetOutPath "$INSTDIR\daisyhome\install\log"
  
    ;Tanuki wrapper
    SetOutPath "$INSTDIR\daisyhome\wrapper\bin"
    !insertmacro FileListWrapperBin File "${FILES_WRAPPER}\"
  
    SetOutPath "$INSTDIR\daisyhome\wrapper\conf"
    !insertmacro FileListWrapperConf File "${FILES_WRAPPER}\"

    SetOutPath "$INSTDIR\daisyhome\wrapper\lib"
    !insertmacro FileListWrapperLib File "${FILES_WRAPPER}\"
    
    SetOutPath "$INSTDIR\daisyhome\wrapper\logs"
    !insertmacro FileListWrapperLog File "${FILES_WRAPPER}\"

    SetOutPath "$INSTDIR\daisyhome\wrapper\service"
    !insertmacro FileListWrapperService File "${FILES_WRAPPER}\"
      
    ; Batch files for dos boxes
    SetOutPath "$INSTDIR\batch"
    !insertmacro FileListDosBoxes File "${FILES_INSTALLER}\batch\"

    ;Daisy library files
    SetOutPath "$INSTDIR\daisyhome\lib"
    File /r "${FILES_DAISY}\lib\*.*"

    ;Daisy custom stuff
    SetOutPath "$INSTDIR\resources"
    ; sleep.bat
    File /r "${FILES_DAISY_CUSTOM}\sleep.bat"
    ; ToolsForEver Command Line Tools
    File /r "${FILES_DAISY_CUSTOM}\T4ePortPing.exe"

    ;Icons
    SetOutPath "$INSTDIR\resources\icons"
    !insertmacro FileListDaisyIcons File "${FILES_ICONS}\"
  
    ;MySQL client (binary, needed for creation of databases on other host)
    SetOutPath "$INSTDIR\mysql\bin"
    !insertmacro FileListMySQL File "${FILES_MYSQL}\"    
      
  SectionEnd
   
SectionGroupEnd

  Section $(TEXT_INSTALLER_SECTION_API) SecAPI
  
    ;Daisy API Docs
    SectionSetText SecWiki $(TEXT_INSTALLER_SECTION_API)
    SetOutPath "$INSTDIR\apidocs"
    File /r "${FILES_DAISY}\apidocs\*.*"

  SectionEnd
  
  Section $(TEXT_INSTALLER_SECTION_SERVICE) SecService
   SectionIn RO
   SectionSetText SecService $(TEXT_INSTALLER_SECTION_SERVICE)
   StrCpy $ServiceInstall ${TRUE}
    
SectionEnd