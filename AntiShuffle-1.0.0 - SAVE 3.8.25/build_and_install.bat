@echo off
echo Building AntiShuffle mod...

:: Run gradle build
call gradlew.bat build

if %ERRORLEVEL% NEQ 0 (
    echo Build failed! Press any key to exit...
    pause >nul
    exit /b %ERRORLEVEL%
)

:: Create mods directory if it doesn't exist
if not exist "%USERPROFILE%\.weave\mods" mkdir "%USERPROFILE%\.weave\mods"

:: Copy the jar file
echo Copying mod to Weave mods folder...
copy /Y "build\libs\AntiShuffle-1.0.jar" "%USERPROFILE%\.weave\mods\"

if %ERRORLEVEL% NEQ 0 (
    echo Failed to copy mod file! Press any key to exit...
    pause >nul
    exit /b %ERRORLEVEL%
)

echo.
echo Successfully built and installed the mod!
echo Location: %USERPROFILE%\.weave\mods\AntiShuffle-1.0.jar
echo.
echo Press any key to exit...
pause >nul 