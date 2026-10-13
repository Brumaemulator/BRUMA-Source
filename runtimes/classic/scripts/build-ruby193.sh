#!/bin/bash
# SPDX-License-Identifier: MIT
set -eu
export PATH=/usr/bin:$PATH
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NDK="${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to the MSYS path of NDK 28.2.13676358}"
TC="$NDK/toolchains/llvm/prebuilt/windows-x86_64"
export PATH="$TC/bin:/usr/bin:$PATH"
export CC="clang --target=aarch64-linux-android26"
export AR=llvm-ar RANLIB=llvm-ranlib NM=llvm-nm STRIP=llvm-strip
export CFLAGS="-O2 -fPIC -fgnu89-inline -Wno-error=implicit-function-declaration -Wno-error=int-conversion -Wno-error=incompatible-function-pointer-types"
export LDFLAGS="-lm -ldl"
cp "$ROOT/upstream/SDL/build-scripts/config.sub" "$ROOT/native/ruby193/tool/config.sub"
cp "$ROOT/upstream/SDL/build-scripts/config.guess" "$ROOT/native/ruby193/tool/config.guess"
BUILD_DIR="${BRUMA_RUBY_BUILD_DIR:-$ROOT/native/ruby193-build}"
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"
bash "$ROOT/native/ruby193/configure" --host=aarch64-linux-android --build=x86_64-w64-mingw32 --disable-shared --disable-install-doc --disable-rubygems --with-baseruby="$ROOT/scripts/host-ruby.sh" --with-static-linked-ext --with-out-ext=openssl,readline,dbm,gdbm,win32,win32ole,fiddle ac_cv_func_setpgrp_void=yes rb_cv_stack_grow_dir=-1 ac_cv_func_getcontext=no ac_cv_func_setcontext=no ac_cv_func_makecontext=no ac_cv_func_swapcontext=no
make -j8 libruby-static.a

