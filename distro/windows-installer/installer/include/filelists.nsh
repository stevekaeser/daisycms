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

Partial lists of files to include in the installer,
all directories that do not have subdirectories 

*/

!macro FileListDaisyBatch COMMAND DIRECTORY

  ${COMMAND} "${DIRECTORY}*.bat"

!macroend

!macro FileListDosBoxes COMMAND DIRECTORY

  ${COMMAND} "${DIRECTORY}dosbox-daisy-bin.bat"
  ${COMMAND} "${DIRECTORY}dosbox-daisy-install.bat"
  
!macroend

!macro FileListDaisyIcons COMMAND DIRECTORY

  ${COMMAND} "${DIRECTORY}daisy_32x32.ico"
  ${COMMAND} "${DIRECTORY}daisy_documentation_32x32.ico"
  ${COMMAND} "${DIRECTORY}mailing_list_32x32.ico"
  ${COMMAND} "${DIRECTORY}repository_32x32.ico"
  ${COMMAND} "${DIRECTORY}svn_32x32.ico"
  ${COMMAND} "${DIRECTORY}uninstall_32x32.ico"
  ${COMMAND} "${DIRECTORY}wiki_32x32.ico"
  ${COMMAND} "${DIRECTORY}backup_tool_32x32.ico"
  ${COMMAND} "${DIRECTORY}add_site_32x32.ico"

!macroend

!macro FileListWrapperBin COMMAND DIRECTORY

  ${COMMAND} "${DIRECTORY}bin\daisy-repository-server-service"
  ${COMMAND} "${DIRECTORY}bin\daisy-repository-server-service.bat"
  ${COMMAND} "${DIRECTORY}bin\daisy-wiki-service"
  ${COMMAND} "${DIRECTORY}bin\daisy-wiki-service.bat"
  ${COMMAND} "${DIRECTORY}bin\wrapper-windows-x86-32.exe"

!macroend

!macro FileListWrapperConf COMMAND DIRECTORY

  ${COMMAND} "${DIRECTORY}conf\*.conf"

!macroend

!macro FileListWrapperLib COMMAND DIRECTORY

  ${COMMAND} "${DIRECTORY}lib\wrapper.jar"
  ${COMMAND} "${DIRECTORY}lib\wrapper-windows-x86-32.dll"

!macroend

!macro FileListWrapperLog COMMAND DIRECTORY

  ;${COMMAND} "${DIRECTORY}\logs\dsy_repo.log"
  ;${COMMAND} "${DIRECTORY}\logs\dsy_wiki.log"

!macroend

!macro FileListWrapperService COMMAND DIRECTORY

  ${COMMAND} "${DIRECTORY}service\install-daisy-repository-server-service.bat"
  ${COMMAND} "${DIRECTORY}service\install-daisy-wiki-service.bat"

  ${COMMAND} "${DIRECTORY}service\uninstall-daisy-repository-server-service.bat"
  ${COMMAND} "${DIRECTORY}service\uninstall-daisy-wiki-service.bat"

  ${COMMAND} "${DIRECTORY}service\start-daisy-repository-server-service.bat"
  ${COMMAND} "${DIRECTORY}service\start-daisy-wiki-service.bat"

  ${COMMAND} "${DIRECTORY}service\stop-daisy-repository-server-service.bat"
  ${COMMAND} "${DIRECTORY}service\stop-daisy-wiki-service.bat"

  ${COMMAND} "${DIRECTORY}service\restart-daisy-repository-server-service.bat"
  ${COMMAND} "${DIRECTORY}service\restart-daisy-wiki-service.bat"

!macroend

!macro FileListMySQL COMMAND DIRECTORY

  ${COMMAND} "${DIRECTORY}\bin\mysql.exe"
  ${COMMAND} "${DIRECTORY}\bin\mysqladmin.exe"

!macroend