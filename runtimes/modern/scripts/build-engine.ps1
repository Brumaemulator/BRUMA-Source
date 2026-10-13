# SPDX-License-Identifier: MIT
# Copyright (c) 2026 BRUMA contributors
param([Parameter(Mandatory=$true)][string]$NdkRoot,[Parameter(Mandatory=$true)][string]$SdkRoot,[Parameter(Mandatory=$true)][string]$Work,[Parameter(Mandatory=$true)][string]$Prefix)
$ErrorActionPreference='Stop'
$Root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$Cmake=Join-Path $SdkRoot 'cmake/4.1.2/bin/cmake.exe'
$Ninja=Join-Path $SdkRoot 'cmake/4.1.2/bin/ninja.exe'
$Build=Join-Path $Work 'cmake-build'
& $Cmake -S (Join-Path $Root 'build-system') -B $Build -G Ninja "-DCMAKE_MAKE_PROGRAM=$Ninja" "-DCMAKE_TOOLCHAIN_FILE=$NdkRoot/build/cmake/android.toolchain.cmake" -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-26 -DANDROID_STL=c++_shared -DCMAKE_BUILD_TYPE=Release "-DBRUMA_JNI_DIR=$Work/jni" "-DBRUMA_PREFIX_DIR=$Prefix"
if($LASTEXITCODE -ne 0){throw 'Modern CMake configure failed'}
& $Cmake --build $Build --parallel 4
if($LASTEXITCODE -ne 0){throw 'Modern CMake build failed'}
$Out=Join-Path $Work 'libs/arm64-v8a'
New-Item -ItemType Directory -Force $Out | Out-Null
Get-ChildItem (Join-Path $Build 'out') -Filter '*.so' | Copy-Item -Destination $Out -Force
foreach($Name in @('libruby.so','libopenal.so')){Copy-Item (Join-Path $Prefix "lib/$Name") $Out -Force}
Copy-Item (Join-Path $NdkRoot 'toolchains/llvm/prebuilt/windows-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so') $Out -Force
Write-Output "Source-built runtime: $Out"

