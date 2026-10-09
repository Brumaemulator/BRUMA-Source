# BRUMA 1.0.1 / versionCode 41

Package: com.brumastudio.bruma. Minor bug-fix release; no engine binaries or
third-party dependencies replaced. Original components and notices unchanged.

Changes: Ruby 1.9 compatibility bridges for the legacy private helper calls
pbAddDependency2, pbRgssOpen and raiseSpeciesStats; narrow in-memory callback
early-return normalization; Android Back opens existing RPG options; RPG
preparation runs on a serial background worker; small localized library-update
label and animated spinner appear only during refresh.

Pokémon Z V2.18 was tested privately: starter choice, Pokémon and move names,
first battle and return to the map passed. Android Back/options passed. Host
regression passed. Library loading label/spinner verified on device. User
accepted this scope. No games or user saves are included in source/tests.

No new motion-pacing optimization is claimed. Modern-game startup ANR behavior
has not been verified with a captured ANR trace. One existing battle WAV sound
was reported unsupported; this release does not claim universal compatibility.
Prior Lint baseline remains; release build, dependencies and bundle validation
are recorded alongside the source release. Debug regression launchers exist
only in the debug source set and are excluded from the production AAB.

Signing material is not part of Corresponding Source. Native rebuild scripts,
sources, locks, patches and license texts remain in runtimes/ and licenses/.
The exact commit, AAB digest and source ZIP digest appear in RELEASE-MANIFEST.json.
