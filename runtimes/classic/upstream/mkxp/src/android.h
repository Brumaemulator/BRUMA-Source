#ifndef ANDROID_H
#define ANDROID_H

#ifdef __ANDROID__
#include <jni.h>
#endif

void androidInit();
const char* androidGetConfigFile();
void androidPrint(const char* message);
JNIEnv* androidGetJNIEnv();
jobject androidGetJNIContext();

#endif // ANDROID_H