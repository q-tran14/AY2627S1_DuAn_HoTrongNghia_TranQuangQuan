<#
Build all SPMF Java source files into build/classes.

Requires JDK 25 or a compatible JDK that supports --release 25.
#>

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

if ($null -eq (Get-Command javac -ErrorAction SilentlyContinue)) {
    throw "javac was not found. Install JDK 25 and add its bin directory to PATH."
}

New-Item -ItemType Directory -Force "build/classes" | Out-Null

$sourceList = "build/sources.txt"
Get-ChildItem -Path "ca" -Recurse -Filter "*.java" |
    ForEach-Object FullName |
    Set-Content -Path $sourceList -Encoding ascii

# The upstream SPMF tree contains comments saved in several legacy encodings.
# ISO-8859-1 accepts every byte while leaving Java syntax and ASCII strings intact.
& javac --release 25 -encoding ISO-8859-1 -d "build/classes" "@$sourceList"
if ($LASTEXITCODE -ne 0) {
    throw "Compilation failed with exit code $LASTEXITCODE."
}

Write-Host "Build completed: build/classes"
