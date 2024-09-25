#include <jni.h>
#include <syslog.h>
#include <cstdarg>
#include <array>
#include <string>
#include <locale>
#include <codecvt>

#include "utils.h"
#include "noise_cancellation_processor.hpp"

namespace utils {

    // Convert multibyte std::string to std::wstring
    std::wstring convertMBString2WString(const std::string &str) {
        std::wstring w(str.begin(), str.end());
        return w;
    }

    // Convert std::wstring to UTF-8 std::string for logging
    std::string convertWStringToString(const std::wstring& wstr) {
        std::wstring_convert<std::codecvt_utf8_utf16<wchar_t>> converter;
        return converter.to_bytes(wstr);
    }
}

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
    std::string finalModelPath = utils::convertWStringToString(*modelPathPtr);

    // Log the final model path (converted back to UTF-8 string)
    ::syslog(LOG_INFO, "KrispNc: #initModel; final_model_path: %s", finalModelPath.c_str());



    // Set the model path using the setter method
    noise_cancellation::NoiseCancellationProcessor::getInstance()->setModelPath(nativePath);

    // Release the memory used by nativePath
    env->ReleaseStringUTFChars(path, nativePath);
}

