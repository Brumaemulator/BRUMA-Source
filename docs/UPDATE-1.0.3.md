# BRUMA 1.0.3 / 46

Cartridge libraries no longer create persistent copies during ZIP scans, ordinary launches or translation catalog lookups. GB/GBC/GBA/NDS read the original when native pathname access is supported. Compressed entries and providers that prohibit direct reopening use only a session file, removed after close; abandoned process sessions are swept on a later scan.

Legacy copies are reclaimed only after both the accessible external original and managed copy match SHA-256. Active games and installed translations are protected. Save identities and formats are unchanged. RPG Maker still requires local game folders. Applying translations still retains the patched game and restoration information.

No engine, dependency, Google SDK, backend, Creators/referral, permission or collection changes. Native engine/library inputs and LICENSE/NOTICE/THIRD-PARTY-NOTICES are unchanged from 1.0.2.

Physical-device regression checks: 500 loose GBC and 500 zipped entries create zero persistent cartridge payload; one selected ZIP entry uses only its temporary payload; GB/GBC/GBA/NDS native video, GBA save/reopen, NDS isolated Activity, mixed RPG/cartridge ZIP, protected legacy cleanup, real translation catalog and library lifecycle checks passed. See STORAGE-REGRESSION-2026-10-28.txt.

Test-only generated ROM binaries are deliberately excluded from this source distribution and the application bundle. The original fixture generators/source and local validation evidence are retained in the private workspace. None of those ROM binaries is required to build the app or native engines.
