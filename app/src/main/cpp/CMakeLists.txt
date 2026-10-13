cmake_minimum_required(VERSION 3.22.1)
project(linkcore LANGUAGES C CXX)

# mGBA 0.10.x predates CMake 4; retain its supported policy baseline.
set(CMAKE_POLICY_VERSION_MINIMUM 3.5)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(LIBMGBA_ONLY ON)
set(M_CORE_GBA ON)
set(M_CORE_GB ON)
set(BUILD_LTO OFF)
add_subdirectory(third_party/mgba-0.10.5 mgba EXCLUDE_FROM_ALL)
target_compile_options(mgba PRIVATE -O2)

add_library(linkcore SHARED native_bridge.cpp)
# Keep per-pixel video transfer optimized in debug APKs as well.
target_compile_options(linkcore PRIVATE -O2)
target_compile_definitions(linkcore PRIVATE M_CORE_GBA M_CORE_GB)
target_include_directories(linkcore PRIVATE third_party/mgba-0.10.5/src)
target_link_libraries(linkcore mgba log jnigraphics)

add_library(translationpatch SHARED translation_xdelta.cpp third_party/xdelta/xdelta3/xdelta3.c)
target_include_directories(translationpatch PRIVATE third_party/xdelta/xdelta3)
target_compile_definitions(translationpatch PRIVATE SIZEOF_SIZE_T=8 SIZEOF_UNSIGNED_LONG=8 SIZEOF_UNSIGNED_LONG_LONG=8 SIZEOF_UNSIGNED_INT=4 XD3_MAIN=0 SECONDARY_LZMA=0 SECONDARY_DJW=1 SECONDARY_FGK=1 HAVE_CONFIG_H=0)
target_compile_options(translationpatch PRIVATE -O2)

