# BRUMA 1.0.4 / 48 — optional landscape hardware

Adds uses-feature android.hardware.screen.landscape required=false. RPG activities retain sensorLandscape, but Play must no longer infer that the entire app requires devices declaring landscape hardware. No gameplay, emulation, layout, save, storage, cover, translation, privacy, permissions or SDK changes.

VersionCode 48 confirmed available by the owner; versionName remains 1.0.4. Candidate 47 remains preserved and its Git tag is not replaced. Google Play's app-level trader declaration is independent and must be completed by the account owner.

Validation covers signed AAB identity, release/debug status, manifest filtering, original permissions/orientations, native input/source parity and the existing Libretro client parity. Full gameplay is not rerun for this manifest-only update. No APK/AAB is uploaded to GitHub or to Google Play by this workflow.
