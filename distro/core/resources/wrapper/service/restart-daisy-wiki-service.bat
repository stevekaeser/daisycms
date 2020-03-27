echo off

net stop "Daisy CMS Wiki Web Application"
if not errorlevel 1 goto startrepo
goto error

:startrepo
net start "Daisy CMS Wiki Web Application"
if not errorlevel 1 goto end

:error
pause

:end
