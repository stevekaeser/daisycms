@echo off
if "%5"=="" GOTO usage

set DAISY_CLI_CLASSPATH=%DAISY_HOME%\lib\daisy\daisy-repository-server-impl\@daisy.version@\daisy-repository-server-impl-@daisy.version@.jar;%DAISY_HOME%\lib\mysql\mysql-connector-java\3.1.12\mysql-connector-java-3.1.12.jar;%DAISY_HOME%\lib\xpp3\xpp3_min\1.1.3.4-RC8\xpp3_min-1.1.3.4-RC8.jar

%DAISY_HOME%/bin/daisy-js %~dp0regenerate-summaries.js %1 %2 %3 %4 %5 %6
goto end

:usage
echo Usage:
echo.
echo %0 ^<repoUrl^> ^<adminUser^> ^<adminPassword^> ^<dbUrl^> ^<dbUser^> [^<dbPassword^>]
echo.
echo Example:
echo.
echo %0 http://localhost:9263 testuser testuser jdbc:mysql://localhost:3306/daisyrepository root "rootpassword"
goto end

:end

