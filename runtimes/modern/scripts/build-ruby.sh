#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK_ROOT="${BRUMA_WORK_DIR:-$ROOT/work}"
PREFIX_ROOT="${BRUMA_PREFIX_DIR:-$ROOT/prefix}"
: "${ANDROID_NDK_ROOT:?Set ANDROID_NDK_ROOT to the pinned NDK r28c path}"
export ANDROID_NDK_HOME="$ANDROID_NDK_ROOT"
TC="$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/windows-x86_64"
export PATH="$TC/bin:/usr/bin:$PATH"
export SOURCE_DATE_EPOCH=1677628800
export ZERO_AR_DATE=1
cd "$WORK_ROOT/ruby"
autoreconf -fi
export CC="clang --target=aarch64-linux-android26"
export CXX="clang++ --target=aarch64-linux-android26"
export AR=llvm-ar NM=llvm-nm RANLIB=llvm-ranlib STRIP=llvm-strip
export CFLAGS="-O2 -fPIC -std=gnu11 -Wno-error=implicit-function-declaration"
export CPPFLAGS="-I$(cygpath -m "$PREFIX_ROOT/include")"
export LDFLAGS="-L$(cygpath -m "$PREFIX_ROOT/lib") -lm -lz"
export DLDFLAGS="-Wl,-soname,libruby.so -Wl,-z,max-page-size=16384"
./configure --host=aarch64-linux-android --build=x86_64-pc-msys --prefix="$(cygpath -m "$PREFIX_ROOT")" --enable-shared --enable-static --enable-install-static-library --disable-jit-support --disable-install-doc --disable-rubygems --with-static-linked-ext --with-out-ext=readline,dbm,gdbm,win32,win32ole,fiddle --without-gmp --with-baseruby=/usr/bin/ruby
make -j8
make install
