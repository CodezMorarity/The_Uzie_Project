#!/usr/bin/env pwsh
param([switch]$PortableOnly)
$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $ProjectRoot
try {
    $OnWindows = [System.Runtime.InteropServices.RuntimeInformation]::IsOSPlatform([System.Runtime.InteropServices.OSPlatform]::Windows)
    if (-not $OnWindows) { throw 'This script must run on 64-bit Windows 10/11. Use the windows-build GitHub Actions workflow from Linux.' }
    if ([Environment]::Is64BitOperatingSystem -eq $false) { throw 'VaultX packages target Windows x64.' }
    if (-not $env:JAVA_HOME) { throw 'JAVA_HOME must point to a 64-bit JDK 21 installation.' }
    $Java = Join-Path $env:JAVA_HOME 'bin\java.exe'
    $JPackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
    if (-not (Test-Path $Java) -or -not (Test-Path $JPackage)) { throw 'JAVA_HOME must contain java.exe and jpackage.exe from a full JDK 21.' }
    $VersionOutput = (& $Java -version 2>&1 | Out-String)
    if ($VersionOutput -notmatch '(?m)^(?:openjdk|java)(?: version)?\s+"?21(?:\.|\s|")') { throw "JDK 21 is required. Detected: $VersionOutput" }
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'Maven 3.9 or later was not found on PATH.' }

    Write-Host 'Running clean test and package build...' -ForegroundColor Cyan
    & mvn -B clean test package
    if ($LASTEXITCODE -ne 0) { throw "Maven build/tests failed with exit code $LASTEXITCODE." }

    $JavaFxModules = Join-Path $ProjectRoot 'target\javafx-modules'
    & mvn -B dependency:copy-dependencies '-DincludeGroupIds=org.openjfx' '-DincludeScope=runtime' "-DoutputDirectory=$JavaFxModules"
    if ($LASTEXITCODE -ne 0) { throw "Could not stage JavaFX runtime modules (exit code $LASTEXITCODE)." }
    if (-not (Get-ChildItem $JavaFxModules -Filter 'javafx-controls-*-win.jar' -ErrorAction SilentlyContinue)) {
        throw "The Windows JavaFX controls module was not resolved under $JavaFxModules. Check the JDK/Maven platform settings."
    }
    if (-not (Test-Path 'target\VaultX.jar')) { throw 'The shaded application jar target\VaultX.jar was not produced.' }
    if (-not (Test-Path 'src\main\resources\com\vaultx\brand\vaultx.ico')) { throw 'The Windows application icon is missing.' }

    $InputDir = Join-Path $ProjectRoot 'target\package-input'
    $DistDir = Join-Path $ProjectRoot 'target\dist'
    if (Test-Path $InputDir) { Remove-Item -LiteralPath $InputDir -Recurse -Force }
    if (Test-Path $DistDir) { Remove-Item -LiteralPath $DistDir -Recurse -Force }
    New-Item -ItemType Directory -Force $InputDir | Out-Null
    New-Item -ItemType Directory -Force $DistDir | Out-Null
    Copy-Item 'target\VaultX.jar' (Join-Path $InputDir 'VaultX.jar') -Force
    $ModulePath = "$env:JAVA_HOME\jmods;$JavaFxModules"
    $Common = @('--name', 'VaultX', '--app-version', '1.0.0', '--vendor', 'VaultX contributors',
        '--input', $InputDir, '--main-jar', 'VaultX.jar', '--main-class', 'com.vaultx.Main',
        '--icon', 'src\main\resources\com\vaultx\brand\vaultx.ico', '--module-path', $ModulePath,
        '--add-modules', 'javafx.controls,javafx.graphics,javafx.base,java.sql,java.logging,jdk.unsupported')

    Write-Host 'Creating self-contained portable app image...' -ForegroundColor Cyan
    & $JPackage @Common '--type' 'app-image' '--dest' $DistDir
    if ($LASTEXITCODE -ne 0) { throw "jpackage app-image creation failed with exit code $LASTEXITCODE." }
    $AppImage = Join-Path $DistDir 'VaultX'
    if (-not (Test-Path (Join-Path $AppImage 'VaultX.exe'))) { throw 'jpackage did not create VaultX.exe.' }
    Compress-Archive -Path (Join-Path $AppImage '*') -DestinationPath (Join-Path $DistDir 'VaultX-portable.zip') -Force

    if (-not $PortableOnly) {
        $Wix = Get-Command candle.exe -ErrorAction SilentlyContinue
        if (-not $Wix) {
            $Candidate = Join-Path ${env:ProgramFiles(x86)} 'WiX Toolset v3.14\bin'
            if (Test-Path $Candidate) { $env:PATH = "$Candidate;$env:PATH"; $Wix = Get-Command candle.exe -ErrorAction SilentlyContinue }
        }
        if (-not $Wix) { throw 'WiX Toolset 3.x is required for the MSI. Install WiX or rerun with -PortableOnly.' }
        $MsiDir = Join-Path $DistDir 'msi'
        New-Item -ItemType Directory -Force $MsiDir | Out-Null
        Write-Host 'Creating Windows MSI installer...' -ForegroundColor Cyan
        & $JPackage @Common '--type' 'msi' '--dest' $MsiDir '--win-menu' '--win-shortcut' '--win-dir-chooser'
        if ($LASTEXITCODE -ne 0) { throw "jpackage MSI creation failed with exit code $LASTEXITCODE." }
        $MsiPath = Join-Path $MsiDir 'VaultX-1.0.0.msi'
        if (-not (Test-Path $MsiPath)) { throw 'Expected MSI was not produced.' }
        Move-Item $MsiPath (Join-Path $DistDir 'VaultX-1.0.0.msi') -Force
    }
    Write-Host "Windows distribution created under $DistDir" -ForegroundColor Green
}
catch {
    Write-Error "VaultX Windows build failed: $_"
    exit 1
}
finally { Pop-Location }
