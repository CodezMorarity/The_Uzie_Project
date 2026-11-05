#!/usr/bin/env pwsh
$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $ProjectRoot
try {
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Java was not found. Install a 64-bit JDK 21 and configure PATH.' }
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'Maven was not found. Install Maven 3.9 or later and configure PATH.' }
    $versionOutput = (& java -version 2>&1 | Out-String)
    if ($versionOutput -notmatch '(?:openjdk|java)\s+21(?:[.+\s])') { throw "Java 21 is required. Detected: $versionOutput" }
    & mvn -B test
    if ($LASTEXITCODE -ne 0) { throw "Maven tests failed with exit code $LASTEXITCODE." }
    Write-Host 'VaultX tests passed.' -ForegroundColor Green
}
finally { Pop-Location }
