@ECHO OFF
:: Use local environment
SETLOCAL

:: Check if a timeout period is specified
IF [%1]==[] GOTO Syntax

:: Filter out slashes, they make the IF command crash
ECHO.%1 | FIND "/" >NUL
IF NOT ERRORLEVEL 1 GOTO Syntax

:: Check if specified timeout period is within limits
IF %1 LSS 1 GOTO Syntax
IF %1 GTR 3600 GOTO Syntax

:: Check for a non-existent IP address
:: Note: this causes a small extra delay!
IF NOT DEFINED NonExist SET NonExist=169.254.255.255
PING %NonExist% -n 1 -w 100 2>NUL | FIND "TTL=" >NUL
IF NOT ERRORLEVEL 1 (
	SET NonExist=1.1.1.1
	PING 1.1.1.1 -n 1 -w 100 2>NUL | FIND "TTL=" >NUL
	IF NOT ERRORLEVEL 1 GOTO NoNonExist
)

:: Use PING time-outs to create the delay
PING %NonExist% -n 1 -w %1000 2>NUL | FIND "TTL=" >NUL

:: Show online help on errors
IF NOT ERRORLEVEL 1 GOTO NoNonExist

:: Done
GOTO End

:NoNonExist
ECHO.
ECHO This batch file needs an invalid IP address to function
ECHO correctly.
ECHO Please specify an invalid IP address in an environment
ECHO variable named NonExist and run this batch file again.

:Syntax
ECHO.
ECHO PMSleep.bat
ECHO Poor Man's SLEEP utility,  Version 2.12 for Windows NT 4 / 2000 / XP
ECHO Wait for a specified number of seconds.
ECHO.
ECHO Usage:  CALL  PMSLEEP  nn
ECHO.
ECHO Where:  nn  is the number of seconds to wait
ECHO         nn  can range from 1 to 3600
ECHO.
ECHO Note:   Due to "overhead" the actual delay may
ECHO         prove to be up to a second longer
ECHO.
ECHO Written by Rob van der Woude
ECHO http://www.robvanderwoude.com
ECHO Corrected and improved by Aaron Thoma,
ECHO Todd Renzema, Greg Hassler and Joe Christl

:End
ENDLOCAL