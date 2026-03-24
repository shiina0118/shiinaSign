#include <jni.h>
#include <android/log.h>

#define TAG "shiinaSign"

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_WARN, TAG,
        "ECDH hook disabled: Dobby native library not available");
    return JNI_VERSION_1_6;
}
