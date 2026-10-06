#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK_ROOT="${BRUMA_WORK_DIR:-$ROOT/work}"
PREFIX_ROOT="${BRUMA_PREFIX_DIR:-$ROOT/prefix}"
: "${ANDROID_NDK_ROOT:?Set ANDROID_NDK_ROOT to the pinned NDK r28c path}"
export ANDROID_NDK_HOME="$ANDROID_NDK_ROOT"
TC="$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/windows-x86_64"
export PATH="$TC/bin:/usr/bin:$PATH"
export CC="clang --target=aarch64-linux-android26" CXX="clang++ --target=aarch64-linux-android26"
export AR=llvm-ar NM=llvm-nm RANLIB=llvm-ranlib STRIP=llvm-strip
export CFLAGS="-O2 -fPIC" LDFLAGS="-Wl,-z,max-page-size=16384"
for name in libiconv pixman; do
 cd "$WORK_ROOT/$name"
 if [ "$name" = pixman ]; then
  autoreconf -fi
 fi
 ./configure --host=aarch64-linux-android --build=x86_64-pc-msys --prefix="$(cygpath -m "$PREFIX_ROOT")" --enable-static --disable-shared --disable-arm-a64-neon
 make -j8
 make install
 done
