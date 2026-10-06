# Final licensing closure — 2026-10-02

RUBY LICENSE BLOCKER: RESOLVED
MODERN ANDROID BUILD RECIPES BLOCKER: RESOLVED (previously completed, unchanged)
FINAL LICENSING BLOCKERS REMAINING: None identified within the reviewed release licensing scope.

Classic Ruby 1.8.7-p374 has been replaced by the source-built official Ruby 1.9.3-p551, electing BSD-2-Clause and preserving all compiled-file exceptions. See runtimes/classic/docs/RUBY193-LICENSE-REVIEW.md and licenses. The combined BRUMA application remains GPLv3; this does not change the original component licenses.

Functional test runtime accepted by the user. Physical-controller, long-session, battle and legacy-save tests that were not independently completed remain recorded as unverified, not falsely marked as executed. The user explicitly authorized integration without these additional tests on 2026-10-02.

Release actions still separate: create upload key, sign the final AAB and record its final signed hash; publish the exact complete Corresponding Source and notices at https://github.com/Brumaemulator/BRUMA-Source before binary distribution. No signing, GitHub publication or v1.0.0 tag/release has been performed.
