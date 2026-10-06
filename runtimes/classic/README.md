# BRUMA classic runtime — Ruby 1.9.3-p551

Source component for the BRUMA combined GPLv3 work. This is not a signed release and no GitHub upload/tag has been made.

Ruby comes exclusively from the official 1.9.3-p551 archive in archives/, whose published SHA-256 is locked. The selected Ruby license option is BSD-2-Clause, with the per-file exceptions in licenses/ruby193/LEGAL and NOTICE.txt. mkxp retains GPL-2.0-or-later. The combined BRUMA distribution uses GPLv3. Dependencies retain their original licenses in their source trees.

See docs/RUBY193-LICENSE-REVIEW.md, sources.lock.json and evidence/ruby193-build-inputs.json. No Ruby 1.8.7 or libmkxp18.so is used by this runtime.

## Rebuild

Windows x64; MSYS2 with make and host Ruby 3.4, Android SDK with NDK 28.2.13676358 and CMake 4.1.2. From PowerShell run scripts/build-runtime.ps1 -MsysBash <path-to-bash.exe> -SdkRoot <Android-SDK-path> -BuildRoot <new-empty-directory> -Integrated. The Ruby core is freshly configured and built before CMake builds the engine. Sources are included; no prebuilt Ruby/native libraries are build inputs. The .so outputs have the independent bruma_classic names when -Integrated is supplied.

Copy the four resulting bruma_classic libraries into BRUMA app/src/main/jniLibs/arm64-v8a. Its classic launcher must preload integration/bruma-ruby193-compat.rb and preserve the new runtime's settings. The modern and NDS engines are unchanged.

All necessary BRUMA/frontend sources must accompany this component in the full Corresponding Source. This archive alone is not the entire application's GPL compliance package. User games, saves, screenshots, device logs, signing keys and private data are deliberately excluded.
