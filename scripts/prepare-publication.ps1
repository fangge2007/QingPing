[CmdletBinding()]
param(
    [string]$Destination = 'C:\ChatGPT\QingPing-publish',
    [ValidatePattern('^[0-9]+\.[0-9]+\.[0-9]+$')][string]$Version = '0.4.2'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$destinationPath = [IO.Path]::GetFullPath($Destination)
if ($destinationPath -eq $projectRoot -or $destinationPath.StartsWith($projectRoot + [IO.Path]::DirectorySeparatorChar)) {
    throw 'Use a separate directory outside the development project.'
}
New-Item -ItemType Directory -Force -Path $destinationPath | Out-Null
$sourceRoots = @('app\src', 'fixture\src', 'fixturetarget\src', 'gradle', 'docs', 'examples', 'scripts', '.github')
$sourceFiles = @('.gitignore','build.gradle','gradle.properties','gradlew','gradlew.bat','settings.gradle',
    'README.md','README.en.md','LICENSE','THIRD_PARTY_NOTICES.txt','app\build.gradle','fixture\build.gradle',
    'fixturetarget\build.gradle','.research\sensor-evidence.json')
$files = [Collections.Generic.List[IO.FileInfo]]::new()
foreach ($relative in $sourceRoots) {
    Get-ChildItem -LiteralPath (Join-Path $projectRoot $relative) -File -Recurse | ForEach-Object { $files.Add($_) }
}
foreach ($relative in $sourceFiles) { $files.Add((Get-Item -LiteralPath (Join-Path $projectRoot $relative) -Force)) }
foreach ($file in $files) {
    $relative = [IO.Path]::GetRelativePath($projectRoot, $file.FullName)
    # The private handoff is not part of the public project documentation.
    if ($relative -eq 'docs\NEXT_CHAT_BRIEF.md') { continue }
    $target = Join-Path $destinationPath $relative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
    if ($relative.StartsWith('docs\') -and $file.Extension -eq '.md') {
        $publicText = [IO.File]::ReadAllText($file.FullName).Replace('主人', '用户')
        [IO.File]::WriteAllText($target, $publicText, [Text.UTF8Encoding]::new($false))
    } else { Copy-Item -LiteralPath $file.FullName -Destination $target }
}
$publicIgnorePath = Join-Path $destinationPath '.gitignore'
$publicIgnoreLines = @(Get-Content -LiteralPath $publicIgnorePath)
foreach ($ignoreRule in @('*.log', 'release-assets/')) {
    if ($publicIgnoreLines -notcontains $ignoreRule) {
        Add-Content -LiteralPath $publicIgnorePath -Value $ignoreRule
    }
}
$assets = Join-Path $destinationPath ('release-assets\' + $Version)
New-Item -ItemType Directory -Force -Path $assets | Out-Null
foreach ($apk in @(
    @('app\build\outputs\apk\release\app-release.apk', "qingping-$Version-release.apk"),
    @('fixture\build\outputs\apk\debug\fixture-debug.apk', 'qingping-test-ad.apk'),
    @('fixturetarget\build\outputs\apk\debug\fixturetarget-debug.apk', 'qingping-test-target.apk')
)) {
    Copy-Item -LiteralPath (Join-Path $projectRoot $apk[0]) -Destination (Join-Path $assets $apk[1])
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archivePath = Join-Path $assets "qingping-$Version-source.zip"
$stream = [IO.File]::Open($archivePath, [IO.FileMode]::Create)
$zip = [IO.Compression.ZipArchive]::new($stream, [IO.Compression.ZipArchiveMode]::Create)
try {
    Get-ChildItem -LiteralPath $destinationPath -File -Recurse -Force | Where-Object {
        $relativePath = [IO.Path]::GetRelativePath($destinationPath, $_.FullName)
        $relativePath -notmatch '^(release-assets|\.git)(\\|/)'
    } | Sort-Object FullName | ForEach-Object {
        $entry = 'QingPing/' + [IO.Path]::GetRelativePath($destinationPath, $_.FullName).Replace('\','/')
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $_.FullName, $entry, [IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
} finally { $zip.Dispose(); $stream.Dispose() }
$hashes = Get-ChildItem -LiteralPath $assets -File | Where-Object Extension -In '.apk','.zip' | Sort-Object Name | ForEach-Object {
    (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant() + '  ' + $_.Name
}
Set-Content -LiteralPath (Join-Path $assets 'SHA256SUMS.txt') -Value $hashes -Encoding utf8
Get-ChildItem -LiteralPath $assets -File | Select-Object Name,Length
