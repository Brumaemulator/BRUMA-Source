# Build BRUMA 1.0.0
Windows x64; JDK 21; Android SDK platform 36; NDK 28.2.13676358; CMake 4.1.2; Gradle 9.6.0; AGP 9.4.0. Python 3, Git, Ninja and MSYS2 Bash/autotools/make/Ruby are needed for native rebuilds.
Set JAVA_HOME and ANDROID_HOME. Create local.properties locally with sdk.dir; it is intentionally not distributed. Run `gradlew.bat --no-daemon bundleRelease assembleRelease` from the repository root. No signing key is embedded. Sign the resulting AAB with your own upload key; recipients can build/sign/install modified APKs with their own keys (a differently signed app cannot update an existing install).

## Native rebuild
Run scripts/build-native.ps1 with explicit SDK/NDK/Python/Bash paths. The script refuses existing work directories. NDS and classic use CMake; modern uses MSYS2/autotools and ndk-build. Modern source staging applies the MIT MiniFFI override. Classic scripts generate patched Ruby/mkxp working copies from the included baselines. mGBA is compiled automatically by the Android build. Link flags include 16 KiB max-page-size. No proprietary Nintendo BIOS/firmware is included.
NDS, classic and modern native source builds succeeded in an independent tree during this packaging pass. The first classic build revealed missing pixman-extra MIT headers; these are now included and the corrected build passed. Additional unused public private-key fixtures were removed afterward. No generated binary replaced the current production runtime. Bit-for-bit equality is not asserted; see docs/NATIVE-REBUILD-EVIDENCE.json.

## LGPL / MPL
All covered source, patches and scripts are included. Dynamic runtime libraries may be replaced by rebuilding/repackaging/signing an APK. The mGBA/blip_buf static combination can be relinked from native_bridge.cpp and the included mGBA/blip_buf source with CMake. No EULA may prohibit the LGPL reverse-engineering/relinking rights. Preserve all notices and offer these sources to binary recipients.

Modern runtime now uses original MIT CMake recipes and scripts/build-engine.ps1 instead of inherited Android.mk/ndk-build. API 26, ARM64, NDK 28.2.13676358, C++ shared; see runtimes/modern/build-system/README.md.

## Classic runtime update — 2026-10-02

The older classic staging/prepare-native.py recipe is superseded. `scripts/build-native.ps1` now calls `runtimes/classic/scripts/build-runtime.ps1`, which configures and builds official Ruby 1.9.3-p551 from source in a fresh directory, then builds mkxp and all its auxiliary libraries under the bruma_classic SONAMEs. It does not use a prebuilt Ruby archive. See runtimes/classic/README.md, sources.lock.json and licenses/ruby193/. The standalone clean Ruby recipe and full ARM64 integrated engine build completed; game operation was accepted by the user. No bit-for-bit reproducibility claim is made.
