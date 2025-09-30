@echo off
echo Migrating code from me.ballmc.AntiShuffle to carlaus.gomod

:: Check if source directories exist
if not exist "src\main\java\me\ballmc\AntiShuffle\command" (
    echo Source command directory not found!
    exit /b 1
)

if not exist "src\main\java\me\ballmc\AntiShuffle\features" (
    echo Source features directory not found!
    exit /b 1
)

:: Create target directories if they don't exist
if not exist "src\main\java\carlaus\gomod\command" mkdir "src\main\java\carlaus\gomod\command"
if not exist "src\main\java\carlaus\gomod\features" mkdir "src\main\java\carlaus\gomod\features"

:: Copy command files and replace package declarations
for %%f in (src\main\java\me\ballmc\AntiShuffle\command\*.java) do (
    echo Migrating %%~nxf
    
    :: Read the file, replace package declaration, and write to new location
    (
        echo package carlaus.gomod.command;
        
        :: Skip the first line (old package declaration) and process rest of the file
        for /f "skip=1 delims=" %%l in (%%f) do (
            :: Replace import statements
            set "line=%%l"
            setlocal enabledelayedexpansion
            
            set "line=!line:import me.ballmc.AntiShuffle.=import carlaus.gomod.!"
            
            echo !line!
            endlocal
        )
    ) > "src\main\java\carlaus\gomod\command\%%~nxf"
)

:: Copy features files and replace package declarations
for %%f in (src\main\java\me\ballmc\AntiShuffle\features\*.java) do (
    echo Migrating %%~nxf
    
    :: Read the file, replace package declaration, and write to new location
    (
        echo package carlaus.gomod.features;
        
        :: Skip the first line (old package declaration) and process rest of the file
        for /f "skip=1 delims=" %%l in (%%f) do (
            :: Replace import statements
            set "line=%%l"
            setlocal enabledelayedexpansion
            
            set "line=!line:import me.ballmc.AntiShuffle.=import carlaus.gomod.!"
            
            echo !line!
            endlocal
        )
    ) > "src\main\java\carlaus\gomod\features\%%~nxf"
)

:: Also copy Main.java if it exists
if exist "src\main\java\me\ballmc\AntiShuffle\Main.java" (
    echo Migrating Main.java
    
    :: Read the file, replace package declaration, and write to new location
    (
        echo package carlaus.gomod;
        
        :: Skip the first line (old package declaration) and process rest of the file
        for /f "skip=1 delims=" %%l in (src\main\java\me\ballmc\AntiShuffle\Main.java) do (
            :: Replace import statements
            set "line=%%l"
            setlocal enabledelayedexpansion
            
            set "line=!line:import me.ballmc.AntiShuffle.=import carlaus.gomod.!"
            set "line=!line:AntiShuffle!= gomod!"
            
            echo !line!
            endlocal
        )
    ) > "src\main\java\carlaus\gomod\Main.java"
)

echo Migration completed. Please check the files for any remaining issues. 