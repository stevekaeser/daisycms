@echo off
setlocal

rem Copyright (c) 1999, 2006 Tanuki Software Inc.
rem
rem Java Service Wrapper general NT service uninstall script
rem

if "%DAISY_HOME%"=="" goto daisyHomeNotSet

if "%OS%"=="Windows_NT" goto nt
if "%OS%"=="WINNT" goto nt
echo This script only works with NT-based versions of Windows.
goto :eof

:nt
rem
rem Find the application home.
rem
rem %~dp0 is location of current script under NT
set _REALPATH=%~dp0
rem
rem this is the place of the wrapper binaries inside the daisy distribution
set _WRAPPER_BIN_DIR=%DAISY_HOME%\wrapper\bin\

rem Decide on the wrapper binary.
set _WRAPPER_BASE=wrapper
set _WRAPPER_EXE=%_WRAPPER_BIN_DIR%%_WRAPPER_BASE%-windows-x86-32.exe
if exist "%_WRAPPER_EXE%" goto conf
set _WRAPPER_EXE=%_WRAPPER_BIN_DIR%%_WRAPPER_BASE%-windows-x86-64.exe
if exist "%_WRAPPER_EXE%" goto conf
set _WRAPPER_EXE=%_WRAPPER_BIN_DIR%%_WRAPPER_BASE%.exe
if exist "%_WRAPPER_EXE%" goto conf
echo Unable to locate a Wrapper executable using any of the following names:
echo %_WRAPPER_BIN_DIR%%_WRAPPER_BASE%-windows-x86-32.exe
echo %_WRAPPER_BIN_DIR%%_WRAPPER_BASE%-windows-x86-64.exe
echo %_WRAPPER_BIN_DIR%%_WRAPPER_BASE%.exe
pause
goto :eof

rem
rem Find the wrapper.conf
rem
:conf
set _WRAPPER_CONF="%~f1"
if not %_WRAPPER_CONF%=="" goto startup
set _WRAPPER_CONF="%_REALPATH%daisy-repository-server-service.conf"

rem
rem Uninstall the Wrapper as an NT service.
rem
:startup
"%_WRAPPER_EXE%" -r %_WRAPPER_CONF%
if not errorlevel 1 goto :eof
pause
goto :eof

:daisyHomeNotSet
echo DAISY_HOME not set!
goto :eof

:eof
