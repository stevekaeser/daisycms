@echo off
if "%OS%"=="Windows_NT" @setlocal
if "%OS%"=="WINNT" @setlocal

if "%DAISY_HOME%"=="" goto daisyHomeNotSet

Rem if there are quotes around the argument given, remove them (source: http://www.ss64.com/ntsyntax/esc.html)
SET DAISYWIKI_DATA=###%1%###
SET DAISYWIKI_DATA=%DAISYWIKI_DATA:"###=%
SET DAISYWIKI_DATA=%DAISYWIKI_DATA:###"=%
SET DAISYWIKI_DATA=%DAISYWIKI_DATA:###=%

if "%DAISYWIKI_DATA%"=="" goto argumentMissing            rem no argument
if "%DAISYWIKI_DATA%"=="###=" goto argumentMissing   rem "" as argument

set DAISYWIKI_HOME=%DAISY_HOME%\daisywiki
set JAVA_ENDORSED_DIRS=%DAISYWIKI_HOME%\endorsedlibs\\

set CONFFILE="%DAISY_HOME%\daisywiki\conf\jetty-daisywiki.xml"
if exist "%DAISYWIKI_DATA%\jetty-daisywiki.xml" set CONFFILE="%DAISYWIKI_DATA%\jetty-daisywiki.xml"

cd /d "%DAISYWIKI_HOME%\jetty"
"%JAVA_HOME%\bin\java" -Xmx128m "-Djava.endorsed.dirs=%JAVA_ENDORSED_DIRS%" "-Ddaisywiki.home=%DAISYWIKI_HOME%" -Dorg.mortbay.util.URI.charset=UTF-8 -Duser.language=en -Duser.country=US -Duser.variant= -Dfile.encoding=UTF-8 "-Ddaisywiki.data=%DAISYWIKI_DATA%\." "-Djava.io.tmpdir=%DAISYWIKI_DATA%\tmp" -jar start.jar %CONFFILE%

goto end

:daisyHomeNotSet
echo DAISY_HOME not set!
goto end

:argumentMissing
echo Specify the daisy wiki data directory as argument.
goto end

:end
