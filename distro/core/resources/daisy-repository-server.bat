@echo off
if "%OS%"=="Windows_NT" @setlocal
if "%OS%"=="WINNT" @setlocal

if "%DAISY_HOME%"=="" goto daisyHomeNotSet

Rem if there are quotes around the argument given, remove them (source: http://www.ss64.com/ntsyntax/esc.html)
SET DATADIR=###%1%###
SET DATADIR=%DATADIR:"###=%
SET DATADIR=%DATADIR:###"=%
SET DATADIR=%DATADIR:###=%

if "%DATADIR%"=="" goto argumentMissing   rem no argument
if "%DATADIR%"=="###=" goto argumentMissing   rem "" as argument

set REPOSERVER_HOME=%DAISY_HOME%\repository-server

set DAISY_JVM_OPTS=-Xmx128m "-Ddaisy.logdir=%DATADIR%\logs" "-Ddaisy.datadir=%DATADIR%\." "-Ddaisy.configLocation=%DATADIR%\conf\myconfig.xml" "-Ddaisy.home=%DAISY_HOME%\." -Dfile.encoding=UTF-8 "-Djava.security.auth.login.config=%DATADIR%\conf\login.config" -Duser.language=en -Duser.country=US -Duser.variant=

set DAISY_RUNTIME_OPTS=--config "%REPOSERVER_HOME%\conf\runtime-config.xml" --repository "%DAISY_HOME%\lib" --log-configuration "%REPOSERVER_HOME%\conf\repository-log4j.properties"

set DAISY_LAUNCHER="%DAISY_HOME%\lib\daisy\daisy-launcher.jar"

"%JAVA_HOME%\bin\java" %DAISY_JVM_OPTS% "-Ddaisy.launcher.repository=%DAISY_HOME%\lib" -jar %DAISY_LAUNCHER% %DAISY_RUNTIME_OPTS% %1 %2 %3 %4 %5

goto end

:argumentMissing
echo Please specify the daisy data directory as argument.
goto end

:daisyHomeNotSet
echo DAISY_HOME not set!
goto end

:end
