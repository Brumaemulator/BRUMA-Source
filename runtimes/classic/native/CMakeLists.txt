cmake_minimum_required(VERSION 3.22)
project(BrumaRuntime LANGUAGES C CXX ASM)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_C_STANDARD 99)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_POLICY_VERSION_MINIMUM 3.5)
add_compile_options(-Wno-error=implicit-function-declaration -Wno-error=incompatible-function-pointer-types -Wno-error=int-conversion)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(U "${CMAKE_CURRENT_SOURCE_DIR}/../upstream")
set(H "${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni")
set(SDL_SENSOR OFF CACHE BOOL "" FORCE)
set(SDL_SHARED ON CACHE BOOL "" FORCE)
set(SDL_STATIC OFF CACHE BOOL "" FORCE)
add_subdirectory(${U}/SDL sdl)
set(ALSOFT_UTILS OFF CACHE BOOL "" FORCE)
set(ALSOFT_EXAMPLES OFF CACHE BOOL "" FORCE)
set(ALSOFT_TESTS OFF CACHE BOOL "" FORCE)
set(ALSOFT_INSTALL OFF CACHE BOOL "" FORCE)
add_subdirectory(${U}/openal-soft openal)
add_subdirectory(${U}/ogg ogg)
add_subdirectory(${U}/vorbis vorbis)
set(OPUS_BUILD_PROGRAMS OFF CACHE BOOL "" FORCE)
set(OPUS_BUILD_TESTING OFF CACHE BOOL "" FORCE)
add_subdirectory(${U}/opus opus)
add_subdirectory(${U}/FluidLite fluid)
add_library(opusfile STATIC
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/opusfile/src/info.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/opusfile/src/internal.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/opusfile/src/opusfile.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/opusfile/src/stream.c")
target_include_directories(opusfile PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/../upstream/opusfile/include" "${CMAKE_CURRENT_SOURCE_DIR}/../upstream/opus/include" "${CMAKE_CURRENT_SOURCE_DIR}/../upstream/ogg/include")
target_compile_definitions(opusfile PRIVATE OP_DISABLE_HTTP)
target_link_libraries(opusfile PUBLIC opus ogg)
set(SIGCXX_MAJOR_VERSION 3)
set(SIGCXX_MINOR_VERSION 4)
set(SIGCXX_MICRO_VERSION 0)
configure_file(${U}/libsigcplusplus/sigc++config.h.cmake ${CMAKE_CURRENT_BINARY_DIR}/sigc++config.h)
add_library(sigc STATIC ${U}/libsigcplusplus/sigc++/connection.cc ${U}/libsigcplusplus/sigc++/signal_base.cc ${U}/libsigcplusplus/sigc++/trackable.cc ${U}/libsigcplusplus/sigc++/functors/slot_base.cc)
target_include_directories(sigc PUBLIC ${U}/libsigcplusplus ${CMAKE_CURRENT_BINARY_DIR})
add_library(iconv STATIC
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/libiconv/libcharset/lib/localcharset.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/libiconv/lib/iconv.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/libiconv/lib/relocatable.c")
target_include_directories(iconv PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/libiconv/include" "${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/libiconv/libcharset" "${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/libiconv/lib" "${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/libiconv/libcharset/include" "${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/libiconv/srclib")
target_compile_definitions(iconv PRIVATE BUILDING_LIBICONV IN_LIBRARY LIBDIR="c")
add_library(guess STATIC
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/libguess/src/libguess/dfa.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/libguess/src/libguess/guess_impl.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/libguess/src/libguess/guess.c")
target_include_directories(guess PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/libguess/src/libguess")
target_compile_definitions(guess PRIVATE LIBGUESS_CORE)
add_library(pixman STATIC
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-access.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-access-accessors.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-arm.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-combine32.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-combine-float.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-filter.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-glyph.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-mips.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-ppc.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-x86.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-gradient-walker.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-region16.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-region32.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-image.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-implementation.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-general.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-fast-path.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-solid-fill.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-conical-gradient.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-linear-gradient.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-radial-gradient.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-bits-image.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-utils.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-edge.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-edge-accessors.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-trap.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-mmx.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-timer.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-matrix.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman/pixman-noop.c")
target_include_directories(pixman PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman/pixman" "${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman-extra" "${CMAKE_CURRENT_SOURCE_DIR}/../historical/app/src/main/jni/pixman")
target_compile_definitions(pixman PRIVATE PIXMAN_NO_TLS PACKAGE="bruma")
target_compile_options(pixman PRIVATE -include limits.h)
set(PHYSFS_BUILD_TEST OFF CACHE BOOL "" FORCE)
set(PHYSFS_BUILD_SHARED OFF CACHE BOOL "" FORCE)
set(PHYSFS_BUILD_STATIC ON CACHE BOOL "" FORCE)
file(GLOB PHYSFS_SOURCES "${U}/physfs/src/*.c")
add_library(physfs-static STATIC ${PHYSFS_SOURCES})
target_include_directories(physfs-static PUBLIC ${U}/physfs/src)
add_library(jpeg STATIC
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jaricom.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jcapimin.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jcapistd.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jcarith.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jccoefct.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jccolor.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jcdctmgr.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jchuff.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jcinit.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jcmainct.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jcmarker.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jcmaster.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jcomapi.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jcparam.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jcprepct.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jcsample.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jctrans.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jdapimin.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jdapistd.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jdarith.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jdatadst.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jdatasrc.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jdcoefct.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jdcolor.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jddctmgr.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jdhuff.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jdinput.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jdmainct.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jdmarker.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jdmaster.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jdmerge.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jdpostct.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jdsample.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jdtrans.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jerror.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jfdctflt.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jfdctfst.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jfdctint.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jidctflt.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jidctfst.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jidctint.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jmem-android.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jmemmgr.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jmemnobs.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jpegtran.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jquant1.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jquant2.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b/jutils.c")
target_include_directories(jpeg PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b")
add_library(png STATIC
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/libpng-1.6.37/png.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/libpng-1.6.37/pngerror.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/libpng-1.6.37/pngget.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/libpng-1.6.37/pngmem.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/libpng-1.6.37/pngpread.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/libpng-1.6.37/pngread.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/libpng-1.6.37/pngrio.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/libpng-1.6.37/pngrtran.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/libpng-1.6.37/pngrutil.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/libpng-1.6.37/pngset.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/libpng-1.6.37/pngtrans.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/libpng-1.6.37/pngwio.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/libpng-1.6.37/pngwrite.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/libpng-1.6.37/pngwtran.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/libpng-1.6.37/pngwutil.c")
target_include_directories(png PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/libpng-1.6.37")
target_compile_definitions(png PRIVATE PNG_ARM_NEON_OPT=0)
target_link_libraries(png PUBLIC z m)
add_library(SDL2_image STATIC
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/IMG.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/IMG_bmp.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/IMG_gif.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/IMG_jpg.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/IMG_lbm.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/IMG_pcx.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/IMG_png.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/IMG_pnm.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/IMG_svg.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/IMG_tga.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/IMG_tif.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/IMG_webp.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/IMG_WIC.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/IMG_xcf.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/IMG_xpm.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/IMG_xv.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/IMG_xxx.c")
target_include_directories(SDL2_image PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image" "${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/jpeg-9b" "${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_image/external/libpng-1.6.37")
target_compile_definitions(SDL2_image PRIVATE LOAD_BMP LOAD_GIF LOAD_PNG LOAD_JPG LOAD_TGA LOAD_XPM LOAD_XV LOAD_PNM LOAD_PCX LOAD_LBM LOAD_XCF LOAD_SVG)
target_link_libraries(SDL2_image PUBLIC SDL2 jpeg png)
set(FT_WITH_ZLIB OFF CACHE BOOL "" FORCE)
set(FT_WITH_BZIP2 OFF CACHE BOOL "" FORCE)
set(FT_WITH_PNG OFF CACHE BOOL "" FORCE)
set(FT_WITH_HARFBUZZ OFF CACHE BOOL "" FORCE)
add_subdirectory("${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_ttf/external/freetype-2.9.1" freetype)
add_library(SDL2_ttf STATIC
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_ttf/SDL_ttf.c")
target_include_directories(SDL2_ttf PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_ttf")
target_link_libraries(SDL2_ttf PUBLIC SDL2 freetype)
add_library(SDL2_sound STATIC
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/aiff.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/au.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/ogg.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/raw.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/wav.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/mpg123.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/compat.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/parse.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/frame.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/format.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/decode_2to1.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/decode_4to1.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/decode_ntom.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/equalizer.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/icy.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/icy2utf8.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/id3.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/layer1.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/layer2.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/layer3.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/optimize.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/readers.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/tabinit.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/stringbuf.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/libmpg123.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/index.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/dct64.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123/decode.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/audio_convert.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/extra_rwops.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/SDL_sound.c")
target_include_directories(SDL2_sound PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound" "${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders" "${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL_sound/decoders/libmpg123" "${CMAKE_CURRENT_SOURCE_DIR}/../upstream/vorbis/include")
target_compile_definitions(SDL2_sound PRIVATE HAVE_CONFIG_H REAL_IS_FLOAT OPT_GENERIC MPG123_NO_CONFIGURE SOUND_SUPPORTS_MPG123 SOUND_SUPPORTS_OGG)
target_link_libraries(SDL2_sound PUBLIC SDL2 vorbisfile)
# Ruby 1.9.3 official core archive; Android extension/encoding registration is BRUMA-owned.
add_library(ruby193_core STATIC IMPORTED)
set_target_properties(ruby193_core PROPERTIES IMPORTED_LOCATION "${BRUMA_RUBY_BUILD_ROOT}/libruby-static.a")
add_library(ruby STATIC ${CMAKE_CURRENT_SOURCE_DIR}/ruby193-init.c ${CMAKE_CURRENT_SOURCE_DIR}/ruby193/ext/zlib/zlib.c
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/big5.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/cp949.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/emacs_mule.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/encdb.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/euc_jp.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/euc_kr.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/euc_tw.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/gb18030.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/gb2312.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/gbk.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/iso_8859_1.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/iso_8859_10.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/iso_8859_11.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/iso_8859_13.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/iso_8859_14.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/iso_8859_15.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/iso_8859_16.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/iso_8859_2.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/iso_8859_3.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/iso_8859_4.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/iso_8859_5.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/iso_8859_6.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/iso_8859_7.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/iso_8859_8.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/iso_8859_9.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/koi8_r.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/koi8_u.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/shift_jis.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/utf_16be.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/utf_16le.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/utf_32be.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/utf_32le.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/windows_1251.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/big5.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/chinese.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/emoji.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/emoji_iso2022_kddi.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/emoji_sjis_docomo.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/emoji_sjis_kddi.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/emoji_sjis_softbank.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/escape.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/gb18030.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/gbk.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/iso2022.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/japanese.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/japanese_euc.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/japanese_sjis.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/korean.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/single_byte.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/transdb.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/utf8_mac.c"
 "${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/utf_16_32.c"
)
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/big5.c" PROPERTIES COMPILE_DEFINITIONS "Init_big5=Init_bruma_trans_big5")
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/chinese.c" PROPERTIES COMPILE_DEFINITIONS "Init_chinese=Init_bruma_trans_chinese")
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/emoji.c" PROPERTIES COMPILE_DEFINITIONS "Init_emoji=Init_bruma_trans_emoji")
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/emoji_iso2022_kddi.c" PROPERTIES COMPILE_DEFINITIONS "Init_emoji_iso2022_kddi=Init_bruma_trans_emoji_iso2022_kddi")
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/emoji_sjis_docomo.c" PROPERTIES COMPILE_DEFINITIONS "Init_emoji_sjis_docomo=Init_bruma_trans_emoji_sjis_docomo")
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/emoji_sjis_kddi.c" PROPERTIES COMPILE_DEFINITIONS "Init_emoji_sjis_kddi=Init_bruma_trans_emoji_sjis_kddi")
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/emoji_sjis_softbank.c" PROPERTIES COMPILE_DEFINITIONS "Init_emoji_sjis_softbank=Init_bruma_trans_emoji_sjis_softbank")
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/escape.c" PROPERTIES COMPILE_DEFINITIONS "Init_escape=Init_bruma_trans_escape")
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/gb18030.c" PROPERTIES COMPILE_DEFINITIONS "Init_gb18030=Init_bruma_trans_gb18030")
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/gbk.c" PROPERTIES COMPILE_DEFINITIONS "Init_gbk=Init_bruma_trans_gbk")
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/iso2022.c" PROPERTIES COMPILE_DEFINITIONS "Init_iso2022=Init_bruma_trans_iso2022")
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/japanese.c" PROPERTIES COMPILE_DEFINITIONS "Init_japanese=Init_bruma_trans_japanese")
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/japanese_euc.c" PROPERTIES COMPILE_DEFINITIONS "Init_japanese_euc=Init_bruma_trans_japanese_euc")
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/japanese_sjis.c" PROPERTIES COMPILE_DEFINITIONS "Init_japanese_sjis=Init_bruma_trans_japanese_sjis")
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/korean.c" PROPERTIES COMPILE_DEFINITIONS "Init_korean=Init_bruma_trans_korean")
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/newline.c" PROPERTIES COMPILE_DEFINITIONS "Init_newline=Init_bruma_trans_newline")
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/single_byte.c" PROPERTIES COMPILE_DEFINITIONS "Init_single_byte=Init_bruma_trans_single_byte")
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/transdb.c" PROPERTIES COMPILE_DEFINITIONS "Init_transdb=Init_bruma_trans_transdb")
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/utf8_mac.c" PROPERTIES COMPILE_DEFINITIONS "Init_utf8_mac=Init_bruma_trans_utf8_mac")
set_source_files_properties("${CMAKE_CURRENT_SOURCE_DIR}/ruby193/enc/trans/utf_16_32.c" PROPERTIES COMPILE_DEFINITIONS "Init_utf_16_32=Init_bruma_trans_utf_16_32")
target_include_directories(ruby PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/ruby193/include ${BRUMA_RUBY_BUILD_ROOT}/.ext/include/aarch64-linux-android PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/ruby193)
target_compile_definitions(ruby PRIVATE RUBY_EXPORT OS_CODE=3 ONIG_ENC_REGISTER=rb_enc_register)
target_compile_options(ruby PRIVATE -fvisibility=hidden -fno-strict-aliasing)
target_link_libraries(ruby PUBLIC ruby193_core z dl m)
add_library(theoradec STATIC
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/theora/lib/apiwrapper.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/theora/lib/bitpack.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/theora/lib/decapiwrapper.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/theora/lib/decinfo.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/theora/lib/decode.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/theora/lib/dequant.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/theora/lib/fragment.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/theora/lib/huffdec.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/theora/lib/idct.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/theora/lib/info.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/theora/lib/internal.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/theora/lib/quant.c"
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/theora/lib/state.c")
target_include_directories(theoradec PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/../upstream/theora/include" "${CMAKE_CURRENT_SOURCE_DIR}/../upstream/theora/lib")
target_link_libraries(theoradec PUBLIC ogg)
add_library(theoraplay STATIC
"${CMAKE_CURRENT_SOURCE_DIR}/../upstream/theoraplay/theoraplay.c")
target_include_directories(theoraplay PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/../upstream/theoraplay")
target_link_libraries(theoraplay PUBLIC theoradec vorbis)
add_library(bruma_mkxp SHARED
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/main.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/audio.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/bitmap.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/eventthread.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/filesystem.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/patcher.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/font.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/input.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/plane.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/scene.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/sprite.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/table.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/tilequad.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/viewport.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/window.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/texpool.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/shader.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/glstate.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/autotiles.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/graphics.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/gl-debug.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/iniconfig.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/etc.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/config.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/settingsmenu.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/keybindings.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/sharedstate.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/gl-fun.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/gl-meta.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/vertex.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/soundemitter.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/sdlsoundsource.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/alstream.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/audiostream.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/rgssad.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/bundledfont.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/opussource.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/vorbissource.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/windowvx.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/tilemapvx.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/tileatlasvx.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/autotilesvx.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/midisource.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/fluid-fun.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/debugwriter.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/android.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/libnsgif/libnsgif.c"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/libnsgif/lzw.c"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/binding-mri.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/binding-util.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/bitmap-binding.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/table-binding.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/etc-binding.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/font-binding.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/graphics-binding.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/input-binding.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/sprite-binding.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/viewport-binding.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/plane-binding.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/window-binding.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/tilemap-binding.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/audio-binding.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/module_rpg.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/filesystem-binding.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/windowvx-binding.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/tilemapvx-binding.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/win32api-binding.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/nilclass-binding.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/encoding-binding.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri/http-binding.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/tileatlas.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src/tilemap.cpp"
"${CMAKE_CURRENT_SOURCE_DIR}/http-android.cpp")
target_include_directories(bruma_mkxp PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/mkxp/src" "${CMAKE_CURRENT_SOURCE_DIR}/mkxp/binding-mri" "${CMAKE_CURRENT_SOURCE_DIR}/generated" "${CMAKE_CURRENT_SOURCE_DIR}/../upstream/SDL/src/core/android")
target_compile_definitions(bruma_mkxp PRIVATE SHARED_FLUID GLES2_HEADER INI_ENCODING ARCH_64BIT ABI_ARM64_V8A)
target_link_libraries(bruma_mkxp PUBLIC SDL2 SDL2_image SDL2_ttf SDL2_sound OpenAL fluidlite physfs-static sigc pixman iconv guess ruby opusfile theoraplay vorbisfile log GLESv2 android)
target_compile_options(bruma_mkxp PRIVATE -fdeclspec)

target_include_directories(SDL2_ttf PRIVATE ${U}/SDL_ttf/external/freetype-2.9.1/include)
target_compile_definitions(bruma_mkxp PRIVATE RUBY_VERSION_MAJOR=1 RUBY_VERSION_MINOR=9)
target_compile_options(bruma_mkxp PRIVATE -Wno-register)
target_link_options(bruma_mkxp PRIVATE -Wl,--no-undefined -Wl,-z,max-page-size=16384)
target_compile_options(ruby PRIVATE -fgnu89-inline)

# Separate SONAMEs keep BRUMA's modern engine and its dependencies unchanged.
option(BRUMA_INTEGRATED_CLASSIC "Build independently named libraries for BRUMA" OFF)
if(BRUMA_INTEGRATED_CLASSIC)
 set_target_properties(SDL2 PROPERTIES OUTPUT_NAME bruma_classic_sdl2)
 set_target_properties(OpenAL PROPERTIES OUTPUT_NAME bruma_classic_openal)
 set_target_properties(fluidlite PROPERTIES OUTPUT_NAME bruma_classic_fluidlite)
 set_target_properties(bruma_mkxp PROPERTIES OUTPUT_NAME bruma_classic)
endif()

add_executable(bruma_ruby193_probe ruby193-probe.c)
target_link_libraries(bruma_ruby193_probe PRIVATE ruby)

