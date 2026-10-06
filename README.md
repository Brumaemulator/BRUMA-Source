# BRUMA Emulator — Corresponding Source v1.0.0

BRUMA is a paid Android application for user-supplied GB, GBC, GBA, NDS and compatible RPG Maker games. It has no advertising SDK, subscriptions or in-app purchases. No games or proprietary BIOS/firmware are included.

Version: **1.0.0**, **versionCode 40**, package `com.brumastudio.bruma`. Official source repository: https://github.com/Brumaemulator/BRUMA-Source .

This snapshot includes all integrated BRUMA frontend, library, scraping, import, configuration, save handling, automatic engine detection, controls, JNI and build code conservatively under GPL-3.0-or-later. It includes the latest save import and library/deletion/navigation changes. No frontend layer is withheld.

## Engines and licenses

- mGBA 0.10.5 for GB/GBC/GBA: MPL-2.0, with original dependency notices and LGPL blip_buf source.
- melonDS for NDS: GPL-3.0-or-later; exact revision in `runtimes/nds/sources.lock.json`.
- Classic RPG Maker: joiplay/mkxp GPL-2.0-or-later, selecting the GPLv3 option in the combined application. Interpreter: official Ruby **1.9.3-p551**, selected BSD-2-Clause alternative and individually retained notices. Ruby 1.8.7 and libmkxp18.so are not distributed runtime inputs.
- Modern RPG Maker: mkxp-z Android Reworked, GPL-2.0-or-later, selecting GPLv3 for this combined distribution. Ruby 3.1 and OpenSSL 3.5.8 retain BSD/Ruby and Apache-2.0 licenses respectively. The original MIT BRUMA CMake recipes replace inherited unverified top-level Android recipes. The RPG Java harness and replacement MiniFFI retain their MIT grants.
- SDL/zlib, OpenAL/LGPL, Ogg/Vorbis/Theora/BSD, MIT, Apache-2.0, OFL, FTL and other notices remain component-specific.

Full texts: `COPYING-GPL-3.0.txt`, `LICENSE.md`, `NOTICE.md`, `THIRD-PARTY-NOTICES.txt`, `licenses/` and the individual upstream COPYING/LICENSE/LEGAL files. The combined distribution is GPLv3; individual components retain their original licenses. BRUMA branding identifies the official product; the code license does not grant trademark ownership or prohibit permitted code copying/modification.

## Compile

See `BUILDING.md`. The exact native input libraries are included in `app/src/main/jniLibs`, and their full corresponding sources, patches, configurations and rebuild scripts are in `runtimes/`. mGBA source is in `app/src/main/cpp/third_party`. SHA256SUMS identifies every published file. `docs/dependency-inventory.json`, engine locks and `docs/RELEASE-INPUTS.json` document provenance.

The release source ZIP and its SHA-256 must accompany the signed AAB. The release manifest outside the source tree records the exact Git commit and final signed AAB hash, avoiding self-referential hashes. To obtain the exact source, use the commit recorded in RELEASE-MANIFEST.json or the matching `BRUMA-1.0.0-Corresponding-Source.zip`, not an unversioned upstream branch.

Signing keys, passwords, local configuration, user games/saves, backups and private data are deliberately excluded. Recipients can rebuild/relink and sign modified APKs with their own keys. No additional restriction is imposed on LGPL debugging, relinking or GPL rights.
