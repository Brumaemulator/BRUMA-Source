LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

SOURCES := \
	$(LOCAL_PATH)/src/main.cpp \
	$(LOCAL_PATH)/src/audio.cpp \
	$(LOCAL_PATH)/src/bitmap.cpp \
	$(LOCAL_PATH)/src/eventthread.cpp \
	$(LOCAL_PATH)/src/filesystem.cpp \
	$(LOCAL_PATH)/src/patcher.cpp \
	$(LOCAL_PATH)/src/font.cpp \
	$(LOCAL_PATH)/src/input.cpp \
	$(LOCAL_PATH)/src/plane.cpp \
	$(LOCAL_PATH)/src/scene.cpp \
	$(LOCAL_PATH)/src/sprite.cpp \
	$(LOCAL_PATH)/src/table.cpp \
	$(LOCAL_PATH)/src/tilequad.cpp \
	$(LOCAL_PATH)/src/viewport.cpp \
	$(LOCAL_PATH)/src/window.cpp \
	$(LOCAL_PATH)/src/texpool.cpp \
	$(LOCAL_PATH)/src/shader.cpp \
	$(LOCAL_PATH)/src/glstate.cpp \
	$(LOCAL_PATH)/src/autotiles.cpp \
	$(LOCAL_PATH)/src/graphics.cpp \
	$(LOCAL_PATH)/src/gl-debug.cpp \
	$(LOCAL_PATH)/src/iniconfig.cpp \
	$(LOCAL_PATH)/src/etc.cpp \
	$(LOCAL_PATH)/src/config.cpp \
	$(LOCAL_PATH)/src/settingsmenu.cpp \
	$(LOCAL_PATH)/src/keybindings.cpp \
	$(LOCAL_PATH)/src/sharedstate.cpp \
	$(LOCAL_PATH)/src/gl-fun.cpp \
	$(LOCAL_PATH)/src/gl-meta.cpp \
	$(LOCAL_PATH)/src/vertex.cpp \
	$(LOCAL_PATH)/src/soundemitter.cpp \
	$(LOCAL_PATH)/src/sdlsoundsource.cpp \
	$(LOCAL_PATH)/src/alstream.cpp \
	$(LOCAL_PATH)/src/audiostream.cpp \
	$(LOCAL_PATH)/src/rgssad.cpp \
	$(LOCAL_PATH)/src/bundledfont.cpp \
    $(LOCAL_PATH)/src/opussource.cpp \
	$(LOCAL_PATH)/src/vorbissource.cpp \
	$(LOCAL_PATH)/src/windowvx.cpp \
	$(LOCAL_PATH)/src/tilemapvx.cpp \
	$(LOCAL_PATH)/src/tileatlasvx.cpp \
	$(LOCAL_PATH)/src/autotilesvx.cpp \
	$(LOCAL_PATH)/src/midisource.cpp \
	$(LOCAL_PATH)/src/fluid-fun.cpp \
    $(LOCAL_PATH)/src/debugwriter.cpp \
	$(LOCAL_PATH)/src/android.cpp \
	$(LOCAL_PATH)/src/http.cpp \
    $(LOCAL_PATH)/src/libnsgif/libnsgif.c \
    $(LOCAL_PATH)/src/libnsgif/lzw.c \
	$(LOCAL_PATH)/binding-mri/binding-mri.cpp \
	$(LOCAL_PATH)/binding-mri/binding-util.cpp \
	$(LOCAL_PATH)/binding-mri/bitmap-binding.cpp \
	$(LOCAL_PATH)/binding-mri/table-binding.cpp \
	$(LOCAL_PATH)/binding-mri/etc-binding.cpp \
	$(LOCAL_PATH)/binding-mri/font-binding.cpp \
	$(LOCAL_PATH)/binding-mri/graphics-binding.cpp \
	$(LOCAL_PATH)/binding-mri/input-binding.cpp \
	$(LOCAL_PATH)/binding-mri/sprite-binding.cpp \
	$(LOCAL_PATH)/binding-mri/viewport-binding.cpp \
	$(LOCAL_PATH)/binding-mri/plane-binding.cpp \
	$(LOCAL_PATH)/binding-mri/window-binding.cpp \
	$(LOCAL_PATH)/binding-mri/tilemap-binding.cpp \
	$(LOCAL_PATH)/binding-mri/audio-binding.cpp \
	$(LOCAL_PATH)/binding-mri/module_rpg.cpp \
	$(LOCAL_PATH)/binding-mri/filesystem-binding.cpp \
	$(LOCAL_PATH)/binding-mri/windowvx-binding.cpp \
	$(LOCAL_PATH)/binding-mri/tilemapvx-binding.cpp \
    $(LOCAL_PATH)/binding-mri/win32api-binding.cpp \
    $(LOCAL_PATH)/binding-mri/nilclass-binding.cpp \
	$(LOCAL_PATH)/binding-mri/encoding-binding.cpp \
	$(LOCAL_PATH)/binding-mri/http-binding.cpp

INCLUDES := $(LOCAL_PATH)/../SDL_sound $(LOCAL_PATH)/src $(LOCAL_PATH)/shader \
	$(LOCAL_PATH)/assets $(LOCAL_PATH)/../SDL/include $(LOCAL_PATH)/../SDL/src/core/android
    

FLAGS:=-std=c++17 -DSHARED_FLUID -DGLES2_HEADER -Os -DINI_ENCODING -D_LIBCPP_HAS_NO_OFF_T_FUNCTIONS -fdeclspec -funwind-tables

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
	FLAGS += -DARCH_32BIT -DABI_ARMEABI_V7A
else ifeq ($(TARGET_ARCH_ABI), arm64-v8a)
	FLAGS += -DARCH_64BIT -DABI_ARM64_V8A
else ifeq ($(TARGET_ARCH_ABI), x86)
	FLAGS += -DARCH_32BIT -DABI_X86
else ifeq ($(TARGET_ARCH_ABI), x86_64)
	FLAGS += -DARCH_64BIT -DABI_X86_64
endif

STATIC_LIBRARIES := vorbis physfs sigc++ pixman iconv \
				  ogg opusfile libguess theoraplay ssl crypto \
				  SDL2 SDL2_sound SDL2_image SDL2_ttf 

SHARED_LIBRARIES := c++_shared fluidlite OpenAL 

LDLIBS:=-lz -llog -ldl -lm -lOpenSLES -pthread

########################
## MKXP with Ruby 1.8 ##
######################## 
include $(CLEAR_VARS)

LOCAL_MODULE:= mkxp18
LOCAL_MODULE_FILENAME:= libmkxp18

LOCAL_CPPFLAGS:= $(FLAGS)

LOCAL_C_INCLUDES := $(INCLUDES) \
	$(LOCAL_PATH)/../ruby18 \
	$(LOCAL_PATH)/../ruby18/include

LOCAL_SRC_FILES := $(SOURCES) \
	$(LOCAL_PATH)/src/tileatlas.cpp \
	$(LOCAL_PATH)/src/tilemap.cpp
	
LOCAL_STATIC_LIBRARIES:= $(STATIC_LIBRARIES) ruby18

LOCAL_SHARED_LIBRARIES:= $(SHARED_LIBRARIES)
LOCAL_LDLIBS:=$(LDLIBS)
LOCAL_ARM_NEON := true

include $(BUILD_SHARED_LIBRARY)

########################
## MKXP with Ruby 1.9 ##
######################## 
include $(CLEAR_VARS)

LOCAL_MODULE:= mkxp19
LOCAL_MODULE_FILENAME:= libmkxp19

LOCAL_CPPFLAGS:= $(FLAGS)

LOCAL_C_INCLUDES := $(INCLUDES) \
	$(LOCAL_PATH)/../ruby19/include

LOCAL_SRC_FILES := $(SOURCES) \
	$(LOCAL_PATH)/src/tileatlas.cpp \
	$(LOCAL_PATH)/src/tilemap.cpp

LOCAL_STATIC_LIBRARIES := $(STATIC_LIBRARIES) ruby19

LOCAL_SHARED_LIBRARIES:= $(SHARED_LIBRARIES)
LOCAL_LDLIBS:=$(LDLIBS)
LOCAL_ARM_NEON := true

include $(BUILD_SHARED_LIBRARY)

########################
## MKXP with Ruby 3.0 ##
######################## 
include $(CLEAR_VARS)

LOCAL_MODULE:= mkxp30
LOCAL_MODULE_FILENAME:= libmkxp30

LOCAL_CPPFLAGS:= $(FLAGS) -DMKXP_Z

LOCAL_C_INCLUDES := $(INCLUDES) \
	$(LOCAL_PATH)/../ruby3/include \
	$(LOCAL_PATH)/../ruby3/config/$(TARGET_ARCH_ABI)

LOCAL_SRC_FILES := $(SOURCES) \
	$(LOCAL_PATH)/src/tileatlasz.cpp \
	$(LOCAL_PATH)/src/tilemapz.cpp

LOCAL_STATIC_LIBRARIES :=  $(STATIC_LIBRARIES) ruby3

LOCAL_SHARED_LIBRARIES:= $(SHARED_LIBRARIES)
LOCAL_LDLIBS:=$(LDLIBS)
LOCAL_ARM_NEON := true

include $(BUILD_SHARED_LIBRARY)