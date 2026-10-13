> HISTORICAL, SUPERSEDED 2026-10-02. The current classic runtime uses Ruby 1.9.3-p551. See ../FINAL-LICENSING-CLOSURE-20261002.md.

# Ruby 1.8.7-p374 exact classic build licensing audit

## Result: NOT RESOLVED for a conservatively combined GPLv3 BRUMA
Ruby License is a permitted alternative for the exact Ruby-owned material, including commercial inclusion subject to COPYING 1–4. This is not a determination that the old custom terms are GPLv3 compatible. No BSD-2-Clause grant was applied retroactively from Ruby 1.9.3/3.1.
The COPYING shipped here offers GPL **version 2**, not “or later”, or the Ruby-specific conditions. FSF's general Ruby entry establishes compatibility through dual licensing; it does not turn this particular GPLv2-only option into GPLv3. The Ruby project's own historical licensing issue explicitly identifies the old Ruby/custom dual offer as incompatible with GPLv3 and discusses the change for 1.9.3.
Primary evidence: https://www.gnu.org/licenses/license-list.html#Ruby ; https://bugs.ruby-lang.org/issues/2032 ; exact local COPYING/GPL/LEGAL and recorded file headers.

## Exact inputs and exceptions
41 translation units and their transitive Ruby headers are taken from Ninja's actual dependency database. ruby-build-inputs.json records every Ruby file included by those objects, full-file notice markers and hashes; it distinguishes source inputs from generated wrappers. No Windows, readline, OpenSSL, digest, socket or NKF Ruby extensions are compiled into the classic Ruby target. Native mkxp win32 compatibility code is a different component, not evidence that Ruby's win32/ directory was built.

- regex.c/h: LGPL-2.0-or-later; can select later LGPL/GPL-compatible route, keep LGPL and FSF notices.
- parse.c: GPL-2.0-or-later Bison skeleton with explicit exception allowing larger generated works under chosen terms; LEGAL explicitly permits generated Ruby parser under Ruby License. Do not delete the Bison exception.
- util.c: inspect the whole file. Contains Intergraph code under Perl's GPL/Artistic offer and a David M. Gay / Lucent permissive notice, in addition to Matsumoto code. Preserve all three notices. The Lucent requirement grants use/copy/modify/distribute without a license fee; it is not a newly imposed prohibition on charging for a combined app. The Perl-derived material is not the blocker in the Ruby-owned core; a precise GPL option/Perl provenance remains documented rather than flattening util.c to one license.
- random.c: BSD-3-Clause MT19937 contribution plus Ruby integration; keep attribution/non-endorsement/disclaimer.
- st.c/h, missing/memcmp.c: public-domain entries in LEGAL; retain headers.
- missing/crypt.c: the actual compiled header has three clauses and no advertising requirement; preserve that BSD-3-Clause text. Do not misread generic historical wording in LEGAL as an active advertising clause.
- missing/strtod.c and missing/strtoul.c (the “fee with the file itself” notices): **not built or included**. Do not claim those notices prohibit selling the actual compiled runtime.
- config.guess/sub/configure are source-build tools, not linked object code. Keep their exception/permission notices if distributed in the source kit.
- ext/zlib/zlib.c: UENO copyright and Ruby umbrella offer; system zlib is independently licensed, provided by Android.

## Why there is still a blocker
The issue is not one replaceable regexp/crypt routine. Ruby-owned parts of array.c, bignum.c, class.c, eval.c, gc.c, marshal.c, object.c, ruby.c and the other core files remain under the exact old dual offer. The full list is in ruby-build-inputs.json. Selecting only the custom alternative for those parts is allowed by Ruby, but positive GPLv3 compatibility cannot be established from the FSF dual-license sentence, especially against the project's own historical explanation. Meeting the custom source/modification clauses ourselves is not the same as demonstrating the combined GPLv3 recipient permissions contain no incompatible restrictions.
Do not assert that GPLv3 section 7 automatically overrides the separate Ruby copyright grant. No author permission or linking exception is invented. No reliance is placed on calling this statically embedded, specially adapted interpreter an Android System Library.

## Replacement research / safe next paths
- Ruby >=1.9.3 introduced the BSD alternative; modern Ruby 3.1 already has it. It is not a drop-in, behavior-identical substitute for the classic 1.8 API/semantics; mixing newer file headers or mechanically replacing COPYRIGHT would be invalid. No interpreter upgrade or compatibility refactor was performed.
- Public old Ruby branches/forks do not prove a new GPLv3 grant for these exact contributions just because they are public. A verified relicensed, compatible 1.8 fork, explicit permission from necessary rightsholders, or a legal determination of the exact custom terms is required before marking this resolved. No such grant has been located.
This task cannot safely replace the whole interpreter without a substantial compatibility port. Isolated BSD/LGPL/public-domain exceptions cannot fix the Ruby-owned core's grant.

## Corresponding Source / notices to preserve
Supply the complete chosen Ruby source used for building (not just exceptions), original COPYING, GPL, LGPL, LEGAL, all file-level copyright/disclaimers including Bison and Lucent, baseline/modified input hashes, Ruby patches, ARM64 config, CMake source list and scripts generating headers. If choosing Ruby-specific terms for an independently cleared distribution, publish modifications per 2(a), mark the non-standard libbruma_classic binary per 2(c), provide original upstream location https://cache.ruby-lang.org/pub/ruby/1.8/ruby-1.8.7-p374.tar.gz, and accompany it with machine-readable source per 3(b). None of this is represented as solving the GPLv3 combination on its own.
