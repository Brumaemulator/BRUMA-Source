#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export BRUMA_UPSTREAM_DIR="${BRUMA_UPSTREAM_DIR:-$ROOT/clean-pass3/upstream}"
export BRUMA_WORK_DIR="${BRUMA_WORK_DIR:-$ROOT/clean-pass3/work}"
export BRUMA_PREFIX_DIR="${BRUMA_PREFIX_DIR:-$ROOT/clean-pass3/prefix}"
: "${ANDROID_NDK_ROOT:?Set ANDROID_NDK_ROOT to NDK r28c}"
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT}"
# PowerShell passes drive-letter paths as C:/...; MSYS Bash needs /c/... for
# PATH lookup and native tools. Convert every path supplied by the PS driver.
ANDROID_NDK_ROOT="$(cygpath -u "$ANDROID_NDK_ROOT")"
ANDROID_SDK_ROOT="$(cygpath -u "$ANDROID_SDK_ROOT")"
BRUMA_UPSTREAM_DIR="$(cygpath -u "$BRUMA_UPSTREAM_DIR")"
BRUMA_WORK_DIR="$(cygpath -u "$BRUMA_WORK_DIR")"
BRUMA_PREFIX_DIR="$(cygpath -u "$BRUMA_PREFIX_DIR")"
export ANDROID_NDK_ROOT ANDROID_SDK_ROOT BRUMA_UPSTREAM_DIR BRUMA_WORK_DIR BRUMA_PREFIX_DIR
export ANDROID_NDK_HOME="$ANDROID_NDK_ROOT"
export SOURCE_DATE_EPOCH=1677628800 ZERO_AR_DATE=1
TC="$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/windows-x86_64"
CMAKE="$ANDROID_SDK_ROOT/cmake/4.1.2/bin/cmake.exe"
NINJA="$ANDROID_SDK_ROOT/cmake/4.1.2/bin/ninja.exe"
export PATH="$TC/bin:$(dirname "$CMAKE"):/usr/bin:$PATH"

mkdir -p "$BRUMA_WORK_DIR" "$BRUMA_PREFIX_DIR"
cp -a "$BRUMA_UPSTREAM_DIR/libiconv" "$BRUMA_WORK_DIR/libiconv"
cp -a "$BRUMA_UPSTREAM_DIR/pixman" "$BRUMA_WORK_DIR/pixman"
cp -a "$BRUMA_UPSTREAM_DIR/openal" "$BRUMA_WORK_DIR/openal"

bash "$ROOT/scripts/build-openssl.sh"
bash "$ROOT/scripts/build-support.sh"
bash "$ROOT/scripts/build-ruby.sh"

ANDROID_TOOLCHAIN="$(cygpath -m "$ANDROID_NDK_ROOT/build/cmake/android.toolchain.cmake")"
INSTALL_PREFIX="$(cygpath -m "$BRUMA_PREFIX_DIR")"
cmake -S "$BRUMA_WORK_DIR/openal" -B "$BRUMA_WORK_DIR/openal-build" -G Ninja \
  -DCMAKE_MAKE_PROGRAM="$(cygpath -m "$NINJA")" \
  -DCMAKE_TOOLCHAIN_FILE="$ANDROID_TOOLCHAIN" \
  -DCMAKE_SYSTEM_NAME=Android -DCMAKE_SYSTEM_VERSION=26 \
  -DCMAKE_POLICY_VERSION_MINIMUM=3.5 \
  -DANDROID_PLATFORM=android-26 -DANDROID_ABI=arm64-v8a \
  -DCMAKE_ANDROID_ARCH_ABI=arm64-v8a -DCMAKE_ANDROID_STL_TYPE=c++_shared \
  -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX="$INSTALL_PREFIX" \
  -DLIBTYPE=SHARED -DALSOFT_BACKEND_OPENSL=ON -DALSOFT_REQUIRE_OPENSL=ON \
  -DALSOFT_EXAMPLES=OFF -DALSOFT_UTILS=OFF -DALSOFT_TESTS=OFF
cmake --build "$BRUMA_WORK_DIR/openal-build" --parallel 8
cmake --install "$BRUMA_WORK_DIR/openal-build"

# mkxp-z's native Android libraries use the NDK libc++ ABI (std::__ndk1).
# Use the exact shared runtime shipped with the pinned NDK instead of building
# upstream libc++ with a different namespace/ABI and packaging an unusable ELF.
mkdir -p "$BRUMA_WORK_DIR/native-out"
NDK_LIBCXX="$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/windows-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so"
test -f "$NDK_LIBCXX"
cp "$NDK_LIBCXX" "$BRUMA_WORK_DIR/native-out/libc++_shared.so"
if [ -f "$BRUMA_WORK_DIR/libs/arm64-v8a/libc++_shared.so" ]; then
  cmp "$NDK_LIBCXX" "$BRUMA_WORK_DIR/libs/arm64-v8a/libc++_shared.so"
fi
mkdir -p "$BRUMA_WORK_DIR/native-out"
echo "Clean dependency build complete; install prefix: $BRUMA_PREFIX_DIR"
