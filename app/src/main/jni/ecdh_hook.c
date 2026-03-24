#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <string.h>
#include <stdio.h>
#include <malloc.h>
#include <dobby.h>

#define TAG "shiinaSign"

// Original function pointer
typedef int (*ECDH_compute_key_t)(void *out, size_t outlen, const void *pub_key, const void *ecdh);
static ECDH_compute_key_t orig_ECDH_compute_key = NULL;

// JNI references
static JavaVM *g_jvm = NULL;
static jclass g_capture_class = NULL;
static jmethodID g_onComputed_method = NULL;

static void bytes_to_hex(const unsigned char *data, size_t len, char *out_hex) {
    static const char hex_chars[] = "0123456789abcdef";
    for (size_t i = 0; i < len; i++) {
        out_hex[i * 2] = hex_chars[(data[i] >> 4) & 0x0F];
        out_hex[i * 2 + 1] = hex_chars[data[i] & 0x0F];
    }
    out_hex[len * 2] = '\0';
}

static char *extract_ec_point_hex(const void *group, const void *point) {
    typedef size_t (*EC_POINT_point2oct_t)(const void *group, const void *point, int form,
                                           unsigned char *buf, size_t len, void *ctx);
    EC_POINT_point2oct_t point2oct = (EC_POINT_point2oct_t)dlsym(RTLD_DEFAULT, "EC_POINT_point2oct");
    if (!point2oct || !group || !point) return NULL;

    // Try POINT_CONVERSION_UNCOMPRESSED (4) first
    size_t len = point2oct(group, point, 4, NULL, 0, NULL);
    if (len == 0) {
        // Try POINT_CONVERSION_COMPRESSED (2)
        len = point2oct(group, point, 2, NULL, 0, NULL);
    }
    if (len == 0 || len > 1024) return NULL;

    unsigned char *buf = (unsigned char *)malloc(len);
    if (!buf) return NULL;

    size_t actual = point2oct(group, point, 4, buf, len, NULL);
    if (actual == 0) {
        actual = point2oct(group, point, 2, buf, len, NULL);
    }
    if (actual == 0) {
        free(buf);
        return NULL;
    }

    char *hex = (char *)malloc(actual * 2 + 1);
    if (hex) {
        bytes_to_hex(buf, actual, hex);
    }
    free(buf);
    return hex;
}

// Hooked ECDH_compute_key
static int hooked_ECDH_compute_key(void *out, size_t outlen, const void *pub_key, const void *ecdh) {
    // Call original function first
    int ret = orig_ECDH_compute_key(out, outlen, pub_key, ecdh);

    if (ret > 0 && out != NULL && g_jvm != NULL && g_onComputed_method != NULL) {
        JNIEnv *env = NULL;
        int attached = (*g_jvm)->GetEnv(g_jvm, (void **)&env, JNI_VERSION_1_6);

        if (attached == JNI_EDETACHED) {
            if ((*g_jvm)->AttachCurrentThread(g_jvm, &env, NULL) == JNI_OK) {
                attached = JNI_OK;
            }
        }

        if (env && attached == JNI_OK) {
            // Convert shared secret to hex string
            char *secret_hex = (char *)malloc((size_t)ret * 2 + 1);
            if (secret_hex) {
                bytes_to_hex((const unsigned char *)out, (size_t)ret, secret_hex);

                // Try to extract server public key from pub_key (EC_POINT)
                // We need the group from ecdh to convert the point
                char *pub_hex = strdup("(unavailable)");

                typedef const void *(*EC_KEY_get0_group_t)(const void *eckey);
                EC_KEY_get0_group_t get0_group = (EC_KEY_get0_group_t)dlsym(RTLD_DEFAULT, "EC_KEY_get0_group");

                if (get0_group && ecdh) {
                    const void *group = get0_group(ecdh);
                    if (group) {
                        char *extracted = extract_ec_point_hex(group, pub_key);
                        if (extracted) {
                            free(pub_hex);
                            pub_hex = extracted;
                        }
                    }
                }

                jstring jSecretHex = (*env)->NewStringUTF(env, secret_hex);
                jstring jPubHex = (*env)->NewStringUTF(env, pub_hex);
                (*env)->CallStaticVoidMethod(env, g_capture_class, g_onComputed_method,
                                             jSecretHex, (jint)ret, jPubHex);

                if (jSecretHex) (*env)->DeleteLocalRef(env, jSecretHex);
                if (jPubHex) (*env)->DeleteLocalRef(env, jPubHex);

                free(secret_hex);
                free(pub_hex);
            }
        }

        __android_log_print(ANDROID_LOG_INFO, TAG, "ECDH_compute_key intercepted: shared_secret_len=%d", ret);
    }

    return ret;
}

static int install_ecdh_hook() {
    // Find ECDH_compute_key symbol
    void *ecdh_func = dlsym(RTLD_DEFAULT, "ECDH_compute_key");
    if (!ecdh_func) {
        void *crypto = dlopen("libcrypto.so", RTLD_NOW);
        if (crypto) {
            ecdh_func = dlsym(crypto, "ECDH_compute_key");
        }
    }

    if (!ecdh_func) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "ECDH_compute_key symbol not found");
        return -1;
    }

    __android_log_print(ANDROID_LOG_INFO, TAG, "Found ECDH_compute_key at %p", ecdh_func);

    // Use Dobby to install inline hook
    int result = DobbyHook(ecdh_func, (void *)hooked_ECDH_compute_key, (void **)&orig_ECDH_compute_key);
    if (result == 0) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "ECDH hook installed successfully");
        return 0;
    } else {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "DobbyHook failed: %d", result);
        return result;
    }
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    g_jvm = vm;
    JNIEnv *env = NULL;

    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "JNI GetEnv failed");
        return JNI_ERR;
    }

    // Find EcdhCapture class and its callback method
    jclass cls = (*env)->FindClass(env, "com/shiinasign/xposed/EcdhCapture");
    if (!cls) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "EcdhCapture class not found");
        return JNI_ERR;
    }
    g_capture_class = (jclass)(*env)->NewGlobalRef(env, cls);

    g_onComputed_method = (*env)->GetStaticMethodID(env, cls, "onEcdhComputed",
                                                     "(Ljava/lang/String;ILjava/lang/String;)V");
    if (!g_onComputed_method) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "onEcdhComputed method not found");
        return JNI_ERR;
    }

    // Install the ECDH hook
    install_ecdh_hook();

    return JNI_VERSION_1_6;
}
