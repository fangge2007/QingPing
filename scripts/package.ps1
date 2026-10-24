[CmdletBinding()]
param([string]$Destination = 'C:\ChatGPT\QingPing-publish')
$ErrorActionPreference = 'Stop'
# Package only the allowlisted public tree, excluding signing keys and private notes.
& (Join-Path $PSScriptRoot 'prepare-publication.ps1') -Destination $Destination
