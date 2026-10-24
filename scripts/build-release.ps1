[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
foreach ($name in @('QINGPING_KEYSTORE','QINGPING_STORE_PASSWORD','QINGPING_KEY_PASSWORD')) {
    if (![Environment]::GetEnvironmentVariable($name)) { throw "Set $name before building a release." }
}
$projectRoot = Split-Path -Parent $PSScriptRoot
if (!(Test-Path -LiteralPath $env:QINGPING_KEYSTORE)) { throw 'Release keystore not found.' }
$portableJava = Join-Path $projectRoot '.tools\jdk\jdk-17.0.20.1+1'
if (Test-Path -LiteralPath $portableJava) { $env:JAVA_HOME = $portableJava }
$portableSdk = Join-Path $projectRoot '.tools\android-sdk'
if (Test-Path -LiteralPath $portableSdk) { $env:ANDROID_HOME = $portableSdk }
$portableGradle = Join-Path $projectRoot '.tools\gradle\gradle-8.11.1\bin\gradle.bat'
if (Test-Path -LiteralPath $portableGradle) {
    $gradleExecutable = $portableGradle
    $env:GRADLE_USER_HOME = Join-Path $projectRoot '.tools\gradle-home'
    $env:ANDROID_USER_HOME = Join-Path $projectRoot '.tools\android-user'
} else { $gradleExecutable = Join-Path $projectRoot 'gradlew.bat' }
Push-Location -LiteralPath $projectRoot
try {
    & $gradleExecutable :app:assembleRelease :app:lintRelease --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Release verification failed.' }
} finally { Pop-Location }
