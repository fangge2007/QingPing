[CmdletBinding()]
param()

# Windows portable build tools. No system installation or PATH changes.
# Android SDK terms: https://developer.android.com/studio/terms
# This script downloads the official archives; it does not create license
# acceptance files or bypass any license prompt from Android Studio/sdkmanager.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$projectRoot = Split-Path -Parent $PSScriptRoot
$toolsRoot = Join-Path $projectRoot '.tools'
New-Item -ItemType Directory -Force -Path $toolsRoot | Out-Null

function Get-VerifiedArchive {
    param([string]$Name, [string]$Url, [string]$Algorithm, [string]$Checksum)
    $archivePath = Join-Path $toolsRoot $Name
    if (Test-Path -LiteralPath $archivePath) {
        $actualHash = (Get-FileHash -LiteralPath $archivePath -Algorithm $Algorithm).Hash
        if ($actualHash -eq $Checksum) { return $archivePath }
        throw "Existing archive checksum mismatch: $archivePath. Inspect/remove that file before retrying."
    }
    Write-Host "Downloading $Name from the publisher..."
    Invoke-WebRequest -Uri $Url -OutFile $archivePath -UseBasicParsing
    $actualHash = (Get-FileHash -LiteralPath $archivePath -Algorithm $Algorithm).Hash
    if ($actualHash -ne $Checksum) { throw "Checksum mismatch: $archivePath" }
    return $archivePath
}

function Expand-OneRoot {
    param([string]$ArchivePath, [string]$Destination)
    if (Test-Path -LiteralPath $Destination) { return }
    $stagingRoot = Join-Path $toolsRoot ('extract-' + [guid]::NewGuid().ToString('N'))
    [IO.Compression.ZipFile]::ExtractToDirectory($ArchivePath, $stagingRoot)
    $topItems = @(Get-ChildItem -LiteralPath $stagingRoot -Force)
    if ($topItems.Count -ne 1 -or -not $topItems[0].PSIsContainer) {
        throw "Archive must contain one root folder: $ArchivePath"
    }
    $resolvedSource = [IO.Path]::GetFullPath($topItems[0].FullName)
    $resolvedTarget = [IO.Path]::GetFullPath($Destination)
    $allowedPrefix = [IO.Path]::GetFullPath($toolsRoot).TrimEnd('\') + '\'
    if (-not $resolvedSource.StartsWith($allowedPrefix, [StringComparison]::OrdinalIgnoreCase) -or
        -not $resolvedTarget.StartsWith($allowedPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Tool extraction path escaped the project tool directory.'
    }
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $resolvedTarget) | Out-Null
    Move-Item -LiteralPath $resolvedSource -Destination $resolvedTarget
}

$jdkArchive = Get-VerifiedArchive 'jdk.zip' `
    'https://aka.ms/download-jdk/microsoft-jdk-17.0.20.1-windows-x64.zip' `
    'SHA256' '3d9006956fc8af5601cd24ffc4f468bef48279c7ebd8171b9bdf90d0aabfbf1f'
$gradleArchive = Get-VerifiedArchive 'gradle.zip' `
    'https://downloads.gradle.org/distributions/gradle-8.11.1-bin.zip' `
    'SHA256' 'f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6'
Expand-OneRoot $jdkArchive (Join-Path $toolsRoot 'jdk\jdk-17.0.20.1+1')
Expand-OneRoot $gradleArchive (Join-Path $toolsRoot 'gradle\gradle-8.11.1')

# Checksums are taken from Google's repository2-1.xml. Packages are pinned to
# the stable base API 36 platform (r2), not a preview/extension SDK.
$sdkRoot = Join-Path $toolsRoot 'android-sdk'
$packages = @(
    @('platform-36_r02.zip', 'platforms\android-36', '2c1a80dd4d9f7d0e6dd336ec603d9b5c55a6f576'),
    @('build-tools_r35_windows.zip', 'build-tools\35.0.0', 'af059bb67cf7786f45ee0db85e2d24985df1b4b6'),
    @('platform-tools_r37.0.1-win.zip', 'platform-tools', 'e03e78b1d80b396f1c3358e31251cb31740e1110')
)
foreach ($sdkPackage in $packages) {
    $sdkArchive = Get-VerifiedArchive $sdkPackage[0] `
        ('https://dl.google.com/android/repository/' + $sdkPackage[0]) 'SHA1' $sdkPackage[2]
    Expand-OneRoot $sdkArchive (Join-Path $sdkRoot $sdkPackage[1])
}

Write-Host 'Portable tools are ready. Set these variables in this terminal:'
Write-Host ('$env:JAVA_HOME = ''' + (Join-Path $toolsRoot 'jdk\jdk-17.0.20.1+1') + '''')
Write-Host ('$env:ANDROID_HOME = ''' + $sdkRoot + '''')
Write-Host 'Then run .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug'
