# BRUMA 1.0.4 / 47 — 20 November 2026

Fix: the historical Libretro thumbnail endpoint returns 404. The optional cover client now falls back to the official libretro-thumbnails GitHub repository for GB/GBC/GBA/NDS. Catalog lookup remains on-device, covers/catalogs are cached, and existing personal/downloaded covers remain valid. Two shallow Git-tree queries avoid the GitHub 1,000-file directory listing limit; caches reduce requests. No credentials, account, new dependency, ROM upload or SDK.

HTTPS only; fixed repository/host allowlist; redirects rejected; 8 MiB catalog/image response limits; cancellation and unsafe filenames checked. The legacy endpoint is attempted once per platform/client when unavailable, rather than repeatedly for every image. GitHub unauthenticated rate limits and network failures can still temporarily prevent new downloads; existing covers and game operation remain available.

Tests: exact LibretroClient source compiled in an isolated Android test app, successful live catalog/matching/image decode/cache for GB (1,647 entries), GBC (1,524), GBA (5,961), NDS (7,335); cancellation and traversal rejection PASS. No third-party image persisted in test artifacts. Release AAB/APK/lint compilation succeeds with the inherited lint baseline (existing warnings are not claimed fixed). Full on-device gameplay was not rerun: engine/storage/translation sources and native inputs are unchanged and bundle parity is checked separately.

No production app, website, backend, Creators or Pocket changes. VersionCode 47 confirmed free by the owner. Exact corresponding sources accompany the signed upload-key AAB. Sources and the privacy recipient update must be made available before public rollout. No Google Play upload or rollout performed.
