#include <fstream>
#include <string>

#include "android.h"

#ifdef __ANDROID__
#include <android/fdsan.h>
#include <android/log.h>

#include <dlfcn.h>
#include <SDL_system.h>
#endif

#ifdef __ANDROID__
typedef void (*android_fdsan_set_error_level)(enum android_fdsan_error_level newlevel);
#endif

void androidInit()
{
#ifdef __ANDROID__
    //Disable fdsan
    void *lib_handle = dlopen("libc.so", RTLD_LAZY);
    if (lib_handle) {
        android_fdsan_set_error_level set_fdsan_error_level = (android_fdsan_set_error_level) dlsym(lib_handle, "android_fdsan_set_error_level");
        if (set_fdsan_error_level) {
            set_fdsan_error_level(ANDROID_FDSAN_ERROR_LEVEL_DISABLED);
        }
    }
#endif
}

const char* androidGetConfigFile()
{
#ifdef __ANDROID__
    std::string androidConfFile = std::string(SDL_AndroidGetExternalStoragePath()) + std::string("/mkxp.json");
    return androidConfFile.c_str();
#else
    return "mkxp.json";
#endif
}

void androidPrint(const char* message)
{
#ifdef __ANDROID__
    __android_log_write(ANDROID_LOG_DEBUG, "mkxp", message);
#endif
}

JNIEnv* androidGetJNIEnv()
{
    return (JNIEnv *) SDL_AndroidGetJNIEnv();
}

jobject androidGetJNIContext()
{
    return (jobject) SDL_AndroidGetActivity();
}