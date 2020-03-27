echo off
net stop "Daisy CMS Wiki Web Application"
if not errorlevel 1 goto :eof
pause
:eof