@echo off
echo Building and installing gomod123...

REM Set directories
set WEAVE_DIR=%USERPROFILE%\.weave\mods
set PROJECT_DIR=C:\Users\admin0\Downloads\gomod123 - Copy (2)
set GRADLE=%PROJECT_DIR%\gradlew.bat

cd /d %PROJECT_DIR%

echo Building project...
call "%GRADLE%" build --console=plain

if %ERRORLEVEL% NEQ 0 (
    echo Build failed! See errors above.
    pause
    exit /b 1
)

echo Build successful!
echo Installing to Weave mods directory...

if not exist "%WEAVE_DIR%" (
    mkdir "%WEAVE_DIR%"
    echo Created directory: %WEAVE_DIR%
)

copy /Y "build\libs\gomod123-1.0.jar" "%WEAVE_DIR%\gomod123-1.0.jar"

if %ERRORLEVEL% NEQ 0 (
    echo Installation failed!
    pause
    exit /b 1
)

:: Change color to green for success messages
color 0A
echo Installation complete!
echo Mod has been installed to: %WEAVE_DIR%\gomod123-1.0.jar
:: Reset color back to default
color 07

pause
