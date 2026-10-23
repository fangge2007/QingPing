[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$toolsRoot = Join-Path $projectRoot '.tools'
$portableJava = Join-Path $toolsRoot 'jdk\jdk-17.0.20.1+1'
$portableSdk = Join-Path $toolsRoot 'android-sdk'
$portableGradle = Join-Path $toolsRoot 'gradle\gradle-8.11.1\bin\gradle.bat'
$env:ANDROID_USER_HOME = Join-Path $toolsRoot 'android-user'
New-Item -ItemType Directory -Force -Path $env:ANDROID_USER_HOME | Out-Null
if (Test-Path -LiteralPath $portableJava) { $env:JAVA_HOME = $portableJava }
if (Test-Path -LiteralPath $portableSdk) { $env:ANDROID_HOME = $portableSdk }
if (Test-Path -LiteralPath $portableGradle) {
    $gradleExecutable = $portableGradle
    $env:GRADLE_USER_HOME = Join-Path $toolsRoot 'gradle-home'
} else { $gradleExecutable = Join-Path $projectRoot 'gradlew.bat' }
Push-Location -LiteralPath $projectRoot
try {
    # C1 avoids a C2 compiler crash observed in this portable JDK during Lint.
    & $gradleExecutable :app:assembleDebug :app:testDebugUnitTest :app:lintDebug `
        :fixture:assembleDebug :fixture:lintDebug :fixturetarget:assembleDebug --no-daemon --console=plain `
        --max-workers=2 '-Dorg.gradle.jvmargs=-Xmx2048m -XX:TieredStopAtLevel=1 -Dfile.encoding=UTF-8'
    if ($LASTEXITCODE -ne 0) { throw "Verification failed with code $LASTEXITCODE" }
} finally { Pop-Location }
