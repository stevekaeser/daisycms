echo off
net start "Daisy CMS Repository Server"
if "%_EXIT_ERRORLEVEL%"=="true" exit %ERRORLEVEL%
if not errorlevel 1 goto :eof
pause
:eof