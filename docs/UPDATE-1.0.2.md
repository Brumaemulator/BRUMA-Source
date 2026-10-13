# BRUMA 1.0.2 / 44 — translations release

Base: public BRUMA 1.0.1, including its classic Ruby fixes, Android Back menu,
asynchronous RPG preparation and library spinner. No emulator runtime input was
changed; Ruby remains 1.9.3-p551. Creators backend, AAB 43 and production are untouched.

Added: optional translations catalog; compatible game matching by SHA-256;
IPS/BPS/UPS patching and Apache-2.0 xdelta3 3.2.2 decoder; downloaded patch and
result validation; verified reverse delta; removal/restoration action; one
library entry per patched game; localized interface in all nine existing languages.
User-supplied external game files are not overwritten. No games/patch payloads
are included in the app/source. RPG Maker file-translation contract is inactive.

Automatic access: the native client requests an anonymous signed one-hour
session only when downloading, stores it in memory, and retries once after a
401. No enrollment code, shared credential, permanent installation identity,
Google SDK, attestation or purchase validation. Tickets expire after 120 seconds
and require the bound bearer. Browser requests to bootstrap are rejected, but
headers can be imitated by an external client: no exclusivity guarantee.
The Netlify function config contains an edge 60-per-minute IP/domain limiter.
The deployed function metadata confirms this rule. Existing manual pilot
endpoints remain backward compatible; the public Android client never uses them.

Backend and privacy deployed and verified on 13 October 2026. Crown download,
application, and original restoration passed on the owner's phone with the signed
1.0.2 / 44 APK. Version code 44 is confirmed available by the owner.
No Creators production changes. Google Play Data Safety must be reviewed before rollout.

Native build: the CMake translationpatch target compiles translation_xdelta.cpp
and xdelta3.c with DJW/FGK, without secondary LZMA or xdelta1. Full upstream
source, license and exact revision are included; see translation-xdelta.lock.json.
Existing engines and their complete Corresponding Source remain included.
