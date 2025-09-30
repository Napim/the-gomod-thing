@echo off
echo Building and installing gomod123...

REM Set directories
set WEAVE_DIR=%USERPROFILE%\.weave\mods
set GRADLE=gradlew.bat

echo Building project...
call %GRADLE% build --console=plain

if %ERRORLEVEL% NEQ 0 (
    echo Build failed! See errors above.
    pause
    exit /b 1
)

echo Build successful!
echo Installing to Weave mods directory...

REM Make sure the Weave mods directory exists
if not exist "%WEAVE_DIR%" (
    mkdir "%WEAVE_DIR%"
    echo Created directory: %WEAVE_DIR%
)

REM Copy the built JAR file to the Weave mods directory
copy /Y "build\libs\gomod123-1.0.jar" "%WEAVE_DIR%\gomod123-1.0.jar"

if %ERRORLEVEL% NEQ 0 (
    echo Installation failed!
    pause
    exit /b 1
)

echo Installation complete!
echo Mod has been installed to: %WEAVE_DIR%\gomod123-1.0.jar
echo.
echo Start Minecraft with Weave Loader to use the mod.
echo Available commands:
echo - /ke [player/ALL/toggle] - View kill effects
echo - /kc - Display kill count
echo - /tspam - Team spam toggle
echo - /opacity [0-100] - Set opacity
echo - /blitzstart - Automatic kit selection
echo - /gmhelp - Show all commands
echo and more...

pause 