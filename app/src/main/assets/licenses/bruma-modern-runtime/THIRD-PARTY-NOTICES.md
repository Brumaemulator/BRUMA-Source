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

## Integrated runtime and publication notes — 2026-09-30

BRUMA includes the ARM64 native libraries built from the locked source package in `third_party/BrumaModernRuntime/BrumaModernRuntime-Corresponding-Source.zip`. See `sources.lock`, `tools.lock.json`, `native-library-hashes.json`, and `INTEGRATION-TEST-REPORT.md` in that folder for exact source revisions, toolchain, notices, build results and native hashes. The source archive SHA-256 is `A572C5710832D3F9A1348D14F4D86FC82B09535FF84CE6034576F61E824C8874`.

The modern engine and its statically linked Ruby/SDL wrapper form a combined runtime component. Distribute that component under GPL-3.0-or-later with corresponding source, patches, build scripts, license texts, installation information, and notices. OpenSSL 3.5.8 is statically linked into Ruby under Apache-2.0; OpenSSL 1.1.1t is not included. OpenAL Soft remains LGPL-2.0-or-later and its notice/replacement requirements must be preserved.

Before Google Play publication, complete legal review and finalize the source offer for the exact distributed APK. Two clean builds succeeded; three libraries differed byte-for-byte between them, and some installed-tool installer hashes remain unavailable. Those reproducibility records and provenance caveats are documented in `native-library-hashes.json` and remain release-preparation work.



## Phase 1 provenance remediation — 2026-10-01

The integrated Java/controls adapter now uses original BRUMA replacements (MIT;
see ../bruma-rpg-adapter-MIT.txt). SDL Java remains under its zlib license with
BRUMA patches. The unlicensed Java/UI port is not a source dependency of this APK.
The modern binding/miniffi{.h,.cpp,-binding.cpp} files are replaced at source stage
by original BRUMA MIT implementations in overrides/mkxp-z/binding/. Ruby 1.8 terms
must not be conflated with Ruby 3.1's BSD alternative. Historical MiniFFI is not
used by the new binding; sources.lock marks it excluded and unverified.

Previously prepared source archives/hashes identify previous binaries; they are
superseded for this build. Phase-1-Sources.zip is an amendment, NOT complete
Corresponding Source. Regenerate the final exact source distribution before release,
excluding historical unlicensed Java/resources and including new overrides, lock,
build recipe and current Java adapter. Private rollback copies are not public sources.
Process separation within the current APK remains unchanged and is not a license boundary.
