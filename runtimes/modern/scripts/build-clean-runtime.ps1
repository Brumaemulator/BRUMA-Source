# SPDX-License-Identifier: MIT
# Copyright (c) 2026 BRUMA contributors
param(
    [Parameter(Mandatory = $true)][string]$NdkRoot,
    [Parameter(Mandatory = $true)][string]$SdkRoot,
    [Parameter(Mandatory = $true)][string]$JdkRoot,
    [Parameter(Mandatory = $true)][string]$PythonExe,
    [string]$BashExe = (Join-Path $PSScriptRoot '..\..\tools\msys64\usr\bin\bash.exe'),
    [string]$PassName = 'clean-build'
)

$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$PassRoot = Join-Path $Root $PassName
if (Test-Path -LiteralPath $PassRoot) {
    throw "Pass directory already exists; choose a new PassName: $PassRoot"
}
$Upstream = Join-Path $PassRoot 'upstream'
$Work = Join-Path $PassRoot 'work'
$Prefix = Join-Path $PassRoot 'prefix'
New-Item -ItemType Directory -Force $Work | Out-Null

$env:BRUMA_UPSTREAM_DIR = $Upstream.Replace('\', '/')
$env:BRUMA_WORK_DIR = $Work.Replace('\', '/')
$env:BRUMA_PREFIX_DIR = $Prefix.Replace('\', '/')
$env:ANDROID_NDK_ROOT = (Resolve-Path $NdkRoot).Path.Replace('\', '/')
$env:ANDROID_SDK_ROOT = (Resolve-Path $SdkRoot).Path.Replace('\', '/')
$env:SOURCE_DATE_EPOCH = '1677628800'
$env:ZERO_AR_DATE = '1'

& $PythonExe (Join-Path $Root 'scripts\verify-source-lock.py')
if ($LASTEXITCODE -ne 0) { throw 'Source lock verification failed.' }
& $PythonExe (Join-Path $Root 'scripts\restore-locked-sources.py')
if ($LASTEXITCODE -ne 0) { throw 'Locked source restore failed.' }
& $PythonExe (Join-Path $Root 'scripts\stage-native.py')
if ($LASTEXITCODE -ne 0) { throw 'Clean native source staging failed.' }
& $BashExe --login (Join-Path $Root 'scripts\build-clean-native.sh').Replace('\', '/')
if ($LASTEXITCODE -ne 0) { throw 'Clean dependency build failed.' }

& (Join-Path $Root 'scripts/build-engine.ps1') -NdkRoot $NdkRoot -SdkRoot $SdkRoot -Work $Work -Prefix $Prefix
if ($LASTEXITCODE -ne 0) { throw 'Modern engine build failed.' }
Write-Output "Clean runtime output: $(Join-Path $Work 'libs/arm64-v8a')"
# APK assembly is intentionally separate: the old upstream test harness is not licensed.
return

