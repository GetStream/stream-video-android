#include <jni.h>
#include <syslog.h>
#include <string>

#include "string_utils.h"
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

    auto modelPath = string_utils::convertMBString2WString(nativePath);

    auto modelPathPtr = &modelPath;

    // Convert the wstring modelPath to std::string for logging
    std::string finalModelPath = string_utils::convertWStringToString(*modelPathPtr);

    // Log the final model path (converted back to UTF-8 string)
    ::syslog(LOG_INFO, "KrispNc: #initModel; final_model_path: %s", finalModelPath.c_str());



    // Set the model path using the setter method
    noise_cancellation::NoiseCancellationProcessor::getInstance()->setModelPath(finalModelPath.c_str());

    // Release the memory used by nativePath
    env->ReleaseStringUTFChars(path, nativePath);
}

