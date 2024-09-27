#include <jni.h>
#include <syslog.h>
#include <string>

#include "string_utils.h"
#include "noise_cancellation_processor.hpp"

extern "C"
JNIEXPORT void JNICALL
Java_io_getstream_webrtc_noise_cancellation_NoiseCancellation_initModelNative(
        JNIEnv *env,
        jobject thiz,
        jstring path
) {
    // Convert jstring to a C string (const char*)
    const char* nativePath = env->GetStringUTFChars(path, nullptr);

    // Log the path
    ::syslog(LOG_INFO, "KrispNc: #initModel; model_path: %s", nativePath);

    auto modelPath = string_utils::convertMBStringToWString(nativePath);

    // Set the model path using the setter method
    noise_cancellation::NoiseCancellationProcessor::getInstance()->SetModelPath(modelPath);

    // Release the memory used by nativePath
    env->ReleaseStringUTFChars(path, nativePath);
}

extern "C"
JNIEXPORT void JNICALL
Java_io_getstream_webrtc_noise_cancellation_NoiseCancellation_setEnabled(
        JNIEnv *env, jobject thiz,
        jboolean enabled
) {
    bool cppEnabled = (enabled == JNI_TRUE);
    noise_cancellation::NoiseCancellationProcessor::getInstance()->SetEnabled(cppEnabled);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_io_getstream_webrtc_noise_cancellation_NoiseCancellation_isEnabled(
        JNIEnv *env,
        jobject thiz
) {
    bool isEnabled = noise_cancellation::NoiseCancellationProcessor::getInstance()->IsEnabled();
    return isEnabled ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT void JNICALL
Java_io_getstream_webrtc_noise_cancellation_NoiseCancellation_setEnabledNative(JNIEnv *env,
                                                                               jobject thiz,
                                                                               jboolean enabled) {
    bool cppEnabled = (enabled == JNI_TRUE);
    noise_cancellation::NoiseCancellationProcessor::getInstance()->SetEnabled(cppEnabled);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_io_getstream_webrtc_noise_cancellation_NoiseCancellation_isEnabledNative(
        JNIEnv *env,
        jobject thiz
) {
    bool isEnabled = noise_cancellation::NoiseCancellationProcessor::getInstance()->IsEnabled();
    return isEnabled ? JNI_TRUE : JNI_FALSE;
}