# SPDX-License-Identifier: MIT
param([Parameter(Mandatory=$true)][string]$MsysBash,[Parameter(Mandatory=$true)][string]$SdkRoot,[string]$BuildRoot,[switch]$Integrated)
$ErrorActionPreference='Stop'
$RuntimeRoot=Split-Path $PSScriptRoot -Parent
if(!$BuildRoot){$BuildRoot=Join-Path $RuntimeRoot 'build-clean'}
if(Test-Path -LiteralPath $BuildRoot){throw 'Choose a new empty build directory.'}
New-Item -ItemType Directory -Path $BuildRoot | Out-Null
function Posix([string]$p){($p -replace '^([A-Za-z]):', '/$1').Replace('\','/')}
$env:ANDROID_NDK_HOME=Posix (Join-Path $SdkRoot 'ndk/28.2.13676358')
$env:BRUMA_RUBY_BUILD_DIR=Posix (Join-Path $BuildRoot 'ruby-core')
& $MsysBash -c ('export PATH=/usr/bin:$PATH; bash "'+(Posix (Join-Path $PSScriptRoot 'build-ruby193.sh'))+'"')
if($LASTEXITCODE){throw 'Ruby core build failed'}
$cmake=Join-Path $SdkRoot 'cmake/4.1.2/bin/cmake.exe'
$ninja=Join-Path $SdkRoot 'cmake/4.1.2/bin/ninja.exe'
$mode=if($Integrated){'ON'}else{'OFF'}
& $cmake -S (Join-Path $RuntimeRoot native) -B (Join-Path $BuildRoot engine) -G Ninja ('-DCMAKE_TOOLCHAIN_FILE='+$SdkRoot+'/ndk/28.2.13676358/build/cmake/android.toolchain.cmake') -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-26 -DCMAKE_BUILD_TYPE=Release ('-DCMAKE_MAKE_PROGRAM='+$ninja) ('-DBRUMA_RUBY_BUILD_ROOT='+$BuildRoot+'/ruby-core') ('-DBRUMA_INTEGRATED_CLASSIC='+$mode)
if($LASTEXITCODE){throw 'Engine configure failed'}
& $cmake --build (Join-Path $BuildRoot engine) --target bruma_mkxp -j 8
if($LASTEXITCODE){throw 'Engine build failed'}
