echo off

net stop "Daisy CMS Repository Server"
if not errorlevel 1 goto startrepo
goto error

:startrepo
net start "Daisy CMS Repository Server"
if not errorlevel 1 goto end

:error
pause

:end
