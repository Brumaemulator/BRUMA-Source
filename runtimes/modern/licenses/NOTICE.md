# BRUMA mkxp-z Android Reworked — third-party notices

This runtime includes or links the source components listed below. Exact source revisions and archive checksums are in ../sources.lock; corresponding source files are in ../upstream.

| Component | Version | License selected | Notice |
|---|---|---|---|
| mkxp-z Android Reworked | 2.4 / commit 0828373 | GPL-2.0-or-later; the combined runtime selects GPL-3.0-or-later to combine with OpenSSL 3 | upstream/mkxp-z-android-reworked/app/jni/mkxp-z/COPYING |
| Ruby mkxp-z fork | 3.1.0p0 / a2d396e | Ruby License or BSD-2-Clause; BSD-2-Clause selected; LEGAL covers bundled parts | upstream/ruby/COPYING, BSDL, LEGAL |
| SDL | 2.30.4 | zlib | upstream/SDL2/COPYING.txt |
| SDL_image | 2.6.3 | zlib; bundled STB image code is public domain or MIT alternative | upstream/SDL2_image/COPYING.txt and source headers |
| SDL_ttf | 2.20.2 | zlib | upstream/SDL2_ttf/COPYING.txt |
| FreeType | SDL submodule commit 6fc77cee | FTL selected | upstream/freetype/LICENSE.TXT |
| HarfBuzz | SDL submodule commit 43931e3 | MIT; inspect per-directory notices | upstream/harfbuzz/COPYING |
| SDL_sound | 2.0.1 | zlib; includes public-domain/CC0 decoders and public-domain modplug sources | upstream/SDL2_sound/LICENSE.txt and source headers |
| OpenAL Soft | 1.23.0 | LGPL-2.0-or-later | upstream/openal/COPYING |
| OpenSSL | 3.5.8 | Apache-2.0 | upstream/openssl/LICENSE |
| Ogg, Vorbis, Theora | 1.3.5, 1.3.7, 1.1.1 | Xiph BSD-style terms | each component COPYING |
| PhysicsFS | 3.2.0 | zlib | upstream/physfs/LICENSE.txt |
| pixman | 0.42.2 | MIT | upstream/pixman/COPYING |
| GNU libiconv | 1.17 | LGPL-2.1-or-later for the library | upstream/libiconv/COPYING.LIB |
| uchardet | 0.0.8 | MPL-1.1, GPL-2.0-or-later, or LGPL-2.1-or-later; GPL-2.0-or-later selected | upstream/uchardet/COPYING |
| json5pp | 2.2.0 | MIT | pinned source in sources.lock |
| Android libc++ shared runtime | NDK r28c / 28.2.13676358, `libc++_shared.so` from the ARM64 Android sysroot; SHA-256 in tools.lock.json | Apache-2.0 WITH LLVM-exception | NDK LLVM license; matching source revision pinned in sources.lock |
| Android system libraries | API 26+ | Android platform terms; supplied by Android, not copied into APK | NDK platform |
| Fonts | Liberation Sans 2.00.1; WenQuanYi Micro Hei 0.2.0-beta | SIL OFL-1.1; Apache-2.0 | metadata embedded in font files |
| Controller mapping database | SDL_GameControllerDB snapshot (exact embedded file pinned by parent engine commit + SHA-256 in sources.lock) | zlib | sources.lock entry; preserve the source URL and zlib notice |
| sdp-android | 1.1.0 | MIT | upstream POM declares MIT; retain [LICENSE](upstream/sdp-android/LICENSE); AAR/POM hashes are in Gradle verification metadata |

Gradle and Android Gradle Plugin are build-time tools and are not packaged. The pinned NDK supplies the compiler, headers and `libc++_shared.so`. We initially built libc++ from the separately pinned upstream LLVM tree, but the on-device loader rejected that binary because it used a different C++ ABI namespace from the Android libraries. The corrected clean recipe packages the NDK's matching ARM64 libc++ and records its exact SHA-256 in tools.lock.json. LLVM source remains pinned for license/source traceability; it is not independently used to build the packaged C++ runtime.

## Source and notices for distribution

The APK contains the mkxp-z engine in libmkxp-z.so, Ruby and statically linked libraries. Provide the complete corresponding source for the exact APK, the patches in patches/, build recipes, and installation scripts under GPLv3 for this combined configuration. Retain copyright and license notices, include [GPL-3.0.txt](GPL-3.0.txt), and give users a durable source download or written source offer. The engine source also includes GPL-2.0 in `licenses/upstream/mkxp-z-android-reworked/app/jni/mkxp-z/COPYING`; its headers allow version 2 or later. OpenAL Soft is dynamic LGPL and needs its notice and a way to replace/relink it. OpenSSL 3 requires its Apache-2.0 license and notices.

Since libmkxp-z.so directly links Ruby, SDL, OpenSSL and the Android JNI wrapper in one APK and process, do not treat a .so boundary as proof BRUMA can remain closed. For release review, treat this as a combined work and publish its covered corresponding source. Have counsel review final packaging and the exact source offer before Play publication. GPL permits commercial distribution but does not make GPL-covered binaries proprietary.

## Release blockers and scope

This source-built release-test is a development artifact, not a release candidate. The following items remain open and must be resolved before commercial distribution:

- `sources.lock` now attributes the controller mappings to SDL_GameControllerDB under zlib and miniffi to Ruby 1.8 Win32API.c under Ruby License/BSD-2-Clause; hashes pin the exact embedded files. The controller database original history point and the exact Ruby 1.8 patch level are not stated upstream, so retain a provenance caveat in any source release.
- The first clean ARM64 build completed, but its first device launch exposed an unusable libc++ ABI. After correcting the recipe to use the exact runtime from pinned NDK r28c, Añil reached its title screen on the POCO X8 Pro. The full corrected recipe and independent second build still need to complete before this runtime can be considered validated.
- Toolchain archive hashes remain incomplete for JDK and MSYS2 pacman packages; the NDK archive SHA-1 is recorded but SHA-256 is unavailable. The Gradle wrapper archive checksum and Maven artifact checksums are verified.
- The app is a test harness. The unsigned `assembleRelease` APK and the local debug-key-signed test copy are not commercial release artifacts.
- The device-test flavor adds a launcher without exporting `MainActivity`; the ordinary isolated flavor remains without a launcher. Añil now reaches its title screen in the corrected device build. Save/load, physical-controller input, pause/resume, long stability, audio listening quality, and a controlled comparison against BRUMA's current runtime remain unverified. Pokémon Z belongs to the classic runtime and was not targeted here. MIDI support is unavailable without FluidSynth.

The runtime embeds the Android JNI wrapper and engine in one process and links Ruby/SDL/OpenSSL into the engine. Keeping unrelated BRUMA UI closed cannot be concluded from a `.so` boundary alone. For this test APK, the safe distribution assumption is that the combined runtime component must be supplied under GPL-3.0-or-later with corresponding source, build scripts and notices. Whether a separately packaged BRUMA app can remain proprietary under a future IPC boundary requires a distinct architectural and legal review; this runtime project does not establish that separation.


