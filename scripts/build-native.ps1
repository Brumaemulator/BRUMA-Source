param([Parameter(Mandatory=$true)][string]$SdkRoot,[Parameter(Mandatory=$true)][string]$NdkRoot,[Parameter(Mandatory=$true)][string]$PythonExe,[Parameter(Mandatory=$true)][string]$BashExe)
$ErrorActionPreference='Stop'
$Root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$Cmake=Join-Path $SdkRoot 'cmake/4.1.2/bin/cmake.exe'
$Ninja=Join-Path $SdkRoot 'cmake/4.1.2/bin/ninja.exe'
$Toolchain=Join-Path $NdkRoot 'build/cmake/android.toolchain.cmake'
$Lib=Join-Path $Root 'app/src/main/jniLibs/arm64-v8a'
foreach($Name in @('nds')) {
 $R=Join-Path $Root "runtimes/$Name"; $Build=Join-Path $R 'native-clean-build'
 if(Test-Path -LiteralPath $Build){throw "Existing build directory: $Build"}
 & $Cmake -S (Join-Path $R 'native') -B $Build -G Ninja "-DCMAKE_MAKE_PROGRAM=$Ninja" "-DCMAKE_TOOLCHAIN_FILE=$Toolchain" -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-26 -DCMAKE_BUILD_TYPE=Release -DBRUMA_INTEGRATED_CLASSIC=ON
 if($LASTEXITCODE -ne 0){throw "Configure failed: $Name"}
 & $Cmake --build $Build --parallel 4
 if($LASTEXITCODE -ne 0){throw "Build failed: $Name"}
 Get-ChildItem -LiteralPath $Build -Recurse -Filter 'libbruma_*.so' | Copy-Item -Destination $Lib
}
$Classic=Join-Path $Root 'runtimes/classic'
$ClassicBuild=Join-Path $Classic 'native-clean-build'
& (Join-Path $Classic 'scripts/build-runtime.ps1') -MsysBash $BashExe -SdkRoot $SdkRoot -BuildRoot $ClassicBuild -Integrated
if($LASTEXITCODE -ne 0){throw 'Ruby 1.9.3 classic source rebuild failed'}
Get-ChildItem (Join-Path $ClassicBuild 'engine') -Recurse -Filter 'libbruma_classic*.so' | Copy-Item -Destination $Lib
$R=Join-Path $Root 'runtimes/modern';$Work=Join-Path $R 'native-clean-work';$Prefix=Join-Path $R 'native-clean-prefix'
if((Test-Path -LiteralPath $Work) -or (Test-Path -LiteralPath $Prefix)){throw 'Modern clean paths already exist'}
$env:BRUMA_UPSTREAM_DIR=(Join-Path $R 'upstream').Replace('\','/')
$env:BRUMA_WORK_DIR=$Work.Replace('\','/');$env:BRUMA_PREFIX_DIR=$Prefix.Replace('\','/')
$env:ANDROID_NDK_ROOT=$NdkRoot.Replace('\','/');$env:ANDROID_SDK_ROOT=$SdkRoot.Replace('\','/')
& $PythonExe (Join-Path $R 'scripts/stage-native.py');if($LASTEXITCODE -ne 0){throw 'Modern staging failed'}
& $BashExe --login (Join-Path $R 'scripts/build-clean-native.sh').Replace('\','/');if($LASTEXITCODE -ne 0){throw 'Modern dependencies failed'}
& (Join-Path $R 'scripts/build-engine.ps1') -NdkRoot $NdkRoot -SdkRoot $SdkRoot -Work $Work -Prefix $Prefix
if($LASTEXITCODE -ne 0){throw 'Modern source CMake build failed'}
Get-ChildItem (Join-Path $Work 'libs/arm64-v8a') -Filter '*.so' | Copy-Item -Destination $Lib
Copy-Item (Join-Path $Work 'native-out/libc++_shared.so') $Lib
