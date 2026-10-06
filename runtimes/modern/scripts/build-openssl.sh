#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
UPSTREAM_ROOT="${BRUMA_UPSTREAM_DIR:-$ROOT/upstream}"
WORK_ROOT="${BRUMA_WORK_DIR:-$ROOT/work}"
PREFIX_ROOT="${BRUMA_PREFIX_DIR:-$ROOT/prefix}"
: "${ANDROID_NDK_ROOT:?Set ANDROID_NDK_ROOT to the pinned NDK r28c path}"
export ANDROID_NDK_HOME="$ANDROID_NDK_ROOT"
TC="$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/windows-x86_64"
export PATH="$TC/bin:/usr/bin:$PATH"
export SOURCE_DATE_EPOCH=1677628800
export ZERO_AR_DATE=1
mkdir -p "$WORK_ROOT/openssl"
cp -a "$UPSTREAM_ROOT/openssl/." "$WORK_ROOT/openssl/"
cd "$WORK_ROOT/openssl"
perl Configure android-arm64 no-shared no-tests -D__ANDROID_API__=26 --prefix="$(cygpath -m "$PREFIX_ROOT")" --openssldir=/system/etc/security
make -j8 --silent
make install_sw
