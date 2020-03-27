@echo off

set DAISY_REPO_DEBUG_PORT=8001
set DAISY_REPO_MAX_MEM=150M

set DAISY_DEV_REPODATA=..\..\..\devrepodata

set DAISY_DEV_REPO_CONF=%DAISY_DEV_REPODATA%\conf

set DAISY_JAVA_OPTIONS=-Xmx%DAISY_REPO_MAX_MEM% -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=%DAISY_REPO_DEBUG_PORT% "-Djava.security.auth.login.config=%DAISY_DEV_REPO_CONF%\login.config" -Dfile.encoding=UTF-8 -Dcom.sun.management.jmxremote "-Ddaisy.configLocation=%DAISY_DEV_REPO_CONF%\myconfig.xml" "-Ddaisy.datadir=%DAISY_DEV_REPODATA%" -Duser.language=en -Duser.country=US -Duser.variant=

set DAISY_MAVEN_HOME=%MAVEN_HOME_LOCAL%
if "%DAISY_MAVEN_HOME%"=="" set DAISY_MAVEN_HOME=%HOMEDRIVE%%HOMEPATH%\.m2

echo
echo Using data directory %DAISY_DEV_REPODATA%
echo
echo Using Maven repository %DAISY_MAVEN_HOME%
echo

set DAISY_RUNTIME_OPTS=--config src\conf\runtime-config.xml --repository "%DAISY_MAVEN_HOME%\repository"

../runtime/target/daisy-runtime-dev.bat %1 %2 %3 %4 %5 %DAISY_RUNTIME_OPTS%
