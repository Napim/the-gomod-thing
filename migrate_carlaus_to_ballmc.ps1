# Migration script to remove carlaus.gomod references and ensure all functionality is in me.ballmc.AntiShuffle
Write-Output "Starting migration from carlaus.gomod to me.ballmc.AntiShuffle..."

# Create a backup directory
$backupDir = "backup_carlaus_" + (Get-Date -Format "yyyyMMdd_HHmmss")
Write-Output "Creating backup in $backupDir"
New-Item -ItemType Directory -Path $backupDir | Out-Null

# Copy carlaus files to backup
Copy-Item -Path "src/main/java/carlaus" -Destination "$backupDir/carlaus" -Recurse -Force
Write-Output "Backup created successfully."

# Check each file in carlaus.gomod to ensure its functionality is in me.ballmc.AntiShuffle
$carlausFiles = Get-ChildItem -Path "src/main/java/carlaus/gomod" -Filter "*.java" -Recurse
$carlausCommands = Get-ChildItem -Path "src/main/java/carlaus/gomod/command" -Filter "*.java" -Recurse
$carlausFeatures = Get-ChildItem -Path "src/main/java/carlaus/gomod/features" -Filter "*.java" -Recurse
$carlausMixins = Get-ChildItem -Path "src/main/java/carlaus/gomod/mixins" -Filter "*.java" -Recurse

Write-Output "Found $($carlausFiles.Count) files in carlaus.gomod"
Write-Output "Checking if all functionality exists in me.ballmc.AntiShuffle..."

$missingFunctionality = @()

# Check commands
foreach ($file in $carlausCommands) {
    $commandName = $file.Name
    $ballmcPath = "src/main/java/me/ballmc/AntiShuffle/command/$commandName"
    
    if (-not (Test-Path $ballmcPath)) {
        Write-Output "Warning: Command $commandName not found in me.ballmc.AntiShuffle"
        $missingFunctionality += "Command: $commandName"
    }
}

# Check features
foreach ($file in $carlausFeatures) {
    $featureName = $file.Name
    $ballmcPath = "src/main/java/me/ballmc/AntiShuffle/features/$featureName"
    
    if (-not (Test-Path $ballmcPath)) {
        Write-Output "Warning: Feature $featureName not found in me.ballmc.AntiShuffle"
        $missingFunctionality += "Feature: $featureName"
    }
}

# Check mixins
foreach ($file in $carlausMixins) {
    $mixinName = $file.Name
    $ballmcPath = "src/main/java/me/ballmc/AntiShuffle/mixins/$mixinName"
    
    if (-not (Test-Path $ballmcPath)) {
        Write-Output "Warning: Mixin $mixinName not found in me.ballmc.AntiShuffle"
        $missingFunctionality += "Mixin: $mixinName"
    }
}

# Report and handle missing functionality
if ($missingFunctionality.Count -gt 0) {
    Write-Output ""
    Write-Output "The following functionality in carlaus.gomod is not found in me.ballmc.AntiShuffle:"
    foreach ($item in $missingFunctionality) {
        Write-Output "  - $item"
    }
    
    $choice = Read-Host "Do you want to continue with the migration? (Y/N)"
    if ($choice -ne "Y" -and $choice -ne "y") {
        Write-Output "Migration aborted. Please manually ensure all functionality is migrated."
        exit
    }
}

# Remove the carlaus.gomod directory
Write-Output "Removing carlaus.gomod directory..."
Remove-Item -Path "src/main/java/carlaus" -Recurse -Force

Write-Output "Migration completed successfully."
Write-Output "Please build and test your project to ensure all functionality works correctly." 