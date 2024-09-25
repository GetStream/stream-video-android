#include <jni.h>
#include <syslog.h>
#include <cstdarg>
#include <array>
#include <string>

#include "noise_cancellation_processor.hpp"

extern "C"
JNIEXPORT void JNICALL
Java_io_getstream_webrtc_noise_cancellation_NoiseCancellationFactory_initModel(
        JNIEnv *env,
        jobject thiz,
        jstring path
) {
    // Convert jstring to a C string (const char*)
    const char* nativePath = env->GetStringUTFChars(path, nullptr);

    // Log the path
    ::syslog(LOG_INFO, "KrispNc: #initModel; model_path: %s", nativePath);

    // Explicitly construct a std::string from nativePath
    const std::string &str = nativePath;
    std::wstring modelPath(str.begin(), str.end());

    // Set the model path using the setter method
    noise_cancellation::NoiseCancellationProcessor::getInstance()->setModelPath(nativePath);

    // Release the memory used by nativePath
    //env->ReleaseStringUTFChars(path, nativePath);
}

