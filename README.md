# BRUMA Emulator â€” Corresponding Source v1.0.4

BRUMA is a paid Android application for user-supplied GB, GBC, GBA, NDS and compatible RPG Maker games. It has no advertising SDK, subscriptions or in-app purchases. No games or proprietary BIOS/firmware are included.

Version: **1.0.4**, **versionCode 48**, package `com.brumastudio.bruma`. Official source repository: https://github.com/Brumaemulator/BRUMA-Source .

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

The release source ZIP and its SHA-256 must accompany the signed AAB. The release manifest outside the source tree records the exact Git commit and final signed AAB hash, avoiding self-referential hashes. To obtain the exact source, use the commit recorded in RELEASE-MANIFEST.json or the matching `BRUMA-1.0.4-48-Corresponding-Source.zip`, not an unversioned upstream branch.

Signing keys, passwords, local configuration, user games/saves, backups and private data are deliberately excluded. Recipients can rebuild/relink and sign modified APKs with their own keys. No additional restriction is imposed on LGPL debugging, relinking or GPL rights.

See docs/UPDATE-1.0.4.md for changes and validation limits.

## Translations

Optional catalog at Bruma Studio, exact game/patch/result SHA-256 validation, IPS/BPS/UPS and xdelta3 decoding. Restoration uses a verified reverse delta and retains one library entry. RPG translation adapter is reserved, not enabled. No Creators/referral code is integrated in this release candidate.

The automatic-access backend and privacy update remain the ones deployed on 13 October 2026 for 1.0.2/44. Version 1.0.3/46 introduced the local storage correction retained here. Version 1.0.4/47 repairs optional cover downloads using the official GitHub-hosted catalog as fallback; update the privacy provider disclosure before public rollout. No new SDKs, permissions, backend changes or Creators/referral features. See docs/UPDATE-1.0.4.md and the retained storage regression evidence. The external release manifest identifies the signed AAB and exact source snapshot.

## 1.0.4 / 47 — cover service repair

This update restores optional cover retrieval using the official libretro-thumbnails repositories when the legacy server returns 404 or is unavailable. New optional HTTPS recipients: api.github.com and raw.githubusercontent.com. No account, API key or new SDK. Cached/local images are retained. Engines, storage fixes, translations, saves, controls, permissions and referral behavior are unchanged. The public privacy section should list these additional cover delivery hosts before public rollout. Sources prepared locally; publication status is recorded outside this tree.

## Build 48 — orientation availability correction

Marks android.hardware.screen.landscape as optional to override the hardware filter inferred from the RPG activities. The existing RPG landscape activity layouts and all engines remain unchanged. Public versionName remains 1.0.4; this build replaces candidate 47 and retains its Libretro correction. See docs/UPDATE-1.0.4-48.md.
