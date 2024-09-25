#include <jni.h>
#include <android/log.h>
#include <syslog.h>

extern "C"
JNIEXPORT void JNICALL
Java_io_getstream_webrtc_noise_cancellation_NoiseCancellationFactory_initModel(
        JNIEnv *env,
        jobject thiz,
        jstring path
) {
    // Convert jstring to a C string (const char*)
    const char *nativePath = env->GetStringUTFChars(path, nullptr);

    // Log the path
    ::syslog(LOG_INFO, "KrispNc: #initModel; model_path: %s", nativePath);

    // Release the memory used by nativePath
    env->ReleaseStringUTFChars(path, nativePath);
}