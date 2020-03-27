echo off
net stop "Daisy CMS Repository Server"
if not errorlevel 1 goto :eof
pause
:eof