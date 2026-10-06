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

