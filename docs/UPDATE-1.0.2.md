# BRUMA 1.0.2 / 44 — translations candidate

Base: public BRUMA 1.0.1, including its classic Ruby fixes, Android Back menu,
asynchronous RPG preparation and library spinner. No emulator runtime input was
changed; Ruby remains 1.9.3-p551. Creators backend, AAB 43 and production are untouched.

Added: optional translations catalog; compatible game matching by SHA-256;
IPS/BPS/UPS patching and Apache-2.0 xdelta3 3.2.2 decoder; downloaded patch and
result validation; verified reverse delta; removal/restoration action; one
library entry per patched game; localized interface in all nine existing languages.
User-supplied external game files are not overwritten. No games/patch payloads
are included in the app/source. RPG Maker file-translation contract is inactive.

Known release blocker: manual 10-minute enrollment codes and 24-hour client grants
belong to the pilot. Public users cannot download automatically yet. No Google SDK
or embedded shared secret is added to work around this. General publication waits
for an authorized public-client access design and verification.

Native build: the CMake translationpatch target compiles translation_xdelta.cpp
and xdelta3.c with DJW/FGK, without secondary LZMA or xdelta1. Full upstream
source, license and exact revision are included; see translation-xdelta.lock.json.
Existing engines and their complete Corresponding Source remain included.
