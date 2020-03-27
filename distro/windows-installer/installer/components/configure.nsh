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

Create uninstaller, file associations and configure Daisy

*/

;--------------------------------
;Sections

Section -InstallData

  ;Registry information
  WriteRegStr SHELL_CONTEXT ${APP_REGKEY} "" $INSTDIR
  WriteRegStr SHELL_CONTEXT ${APP_REGKEY} "Version" "${APP_VERSION}.${APP_SERIES_KEY}"

  WriteRegStr SHELL_CONTEXT ${APP_REGKEY_SETUP} "JAVA Path" $PathJAVA
  WriteRegStr SHELL_CONTEXT ${APP_REGKEY_SETUP} "MySQL Path" $PathMySQL
 
  ;Uninstaller information
  !define REG_UNINSTALL 'WriteRegStr SHELL_CONTEXT "Software\Microsoft\Windows\CurrentVersion\Uninstall\${SETUP_UNINSTALLER_KEY}"'
  
  ${REG_UNINSTALL} "UninstallString" "$\"$INSTDIR\${SETUP_UNINSTALLER}$\""
  ${REG_UNINSTALL} "DisplayName" "${APP_NAME} ${APP_VERSION}"
  ${REG_UNINSTALL} "DisplayVersion" "${APP_VERSION}"
  ${REG_UNINSTALL} "DisplayIcon" "$INSTDIR\wrapper\resources\icons\daisy_32x32.ico"
  ${REG_UNINSTALL} "URLUpdateInfo" "http://www.daisycms.org"
  ${REG_UNINSTALL} "URLInfoAbout" "http://www.daisycms.org/daisy/45.html"
  ${REG_UNINSTALL} "Publisher" "Outerthought bvba"
  ${REG_UNINSTALL} "HelpLink" "http://lists.cocoondev.org/mailman/listinfo/daisy"  
 
  ;Create uninstaller
  WriteUninstaller "$INSTDIR\${SETUP_UNINSTALLER}"
  
SectionEnd

;--------------------------------
;Functions

Function CreateDesktopShortcut

  ;Creating a desktop shortcut is an option on the finish page
  !define SHORTCUT '\${APP_NAME} CMS.lnk" "$INSTDIR\wrapper\bin\DaisyStartup.bat" "" "$INSTDIR\resources\icons\daisy_32x32.ico" "" "" "" "${APP_INFO}"'
  CreateShortCut "$DESKTOP\${SHORTCUT}

FunctionEnd