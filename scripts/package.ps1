[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$artifactRoot = Join-Path $projectRoot 'artifacts'
New-Item -ItemType Directory -Force -Path $artifactRoot | Out-Null

# Package only source inputs, never caches, signing keys, crash logs or build output.
$sourceRoots = @('app\src', 'fixture\src', 'fixturetarget\src', 'gradle', 'docs', 'examples', 'scripts')
$sourceFiles = @('.gitignore','build.gradle','gradle.properties','gradlew','gradlew.bat','settings.gradle','README.md',
    'app\build.gradle','fixture\build.gradle','fixturetarget\build.gradle','.research\sensor-evidence.json')
$files = [Collections.Generic.List[IO.FileInfo]]::new()
foreach ($relative in $sourceRoots) {
    $directory = Join-Path $projectRoot $relative
    Get-ChildItem -LiteralPath $directory -File -Recurse | ForEach-Object { $files.Add($_) }
}
foreach ($relative in $sourceFiles) {
    $path = Join-Path $projectRoot $relative
    if (Test-Path -LiteralPath $path) { $files.Add((Get-Item -LiteralPath $path -Force)) }
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archivePath = Join-Path $artifactRoot 'qingping-0.4.1-source.zip'
$stream = [IO.File]::Open($archivePath, [IO.FileMode]::Create)
$zip = [IO.Compression.ZipArchive]::new($stream, [IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($file in $files | Sort-Object FullName -Unique) {
        $relative = [IO.Path]::GetRelativePath($projectRoot, $file.FullName).Replace('\','/')
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $file.FullName,
            ('QingPing/' + $relative), [IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
} finally { $zip.Dispose(); $stream.Dispose() }

$apks = @(
    @('app\build\outputs\apk\debug\app-debug.apk', 'qingping-0.4.1-debug.apk'),
    @('fixture\build\outputs\apk\debug\fixture-debug.apk', 'qingping-test-ad.apk'),
    @('fixturetarget\build\outputs\apk\debug\fixturetarget-debug.apk', 'qingping-test-target.apk')
)
foreach ($apk in $apks) {
    Copy-Item -LiteralPath (Join-Path $projectRoot $apk[0]) -Destination (Join-Path $artifactRoot $apk[1])
}
$hashes = foreach ($file in Get-ChildItem -LiteralPath $artifactRoot -File | Where-Object Extension -In '.apk','.zip' | Sort-Object Name) {
    ((Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant() + '  ' + $file.Name)
}
Set-Content -LiteralPath (Join-Path $artifactRoot 'SHA256SUMS.txt') -Value $hashes -Encoding utf8
Get-ChildItem -LiteralPath $artifactRoot -File | Select-Object Name,Length
