#include <jni.h>
#include <string>
#include <cstdarg>
#include <dlfcn.h>
#include <syslog.h>
#include "dynamic_audio_processor.hpp"

enum class KrispFunctionId
{
    krispAudioGlobalInit = 0,
    krispAudioGlobalDestroy = 1,
    krispAudioSetModel = 2,
    krispAudioSetModelBlob = 3,
    krispAudioRemoveModel = 4,
    krispAudioNcCreateSession = 5,
    krispAudioNcCloseSession = 6,
    krispAudioNcCleanAmbientNoiseFloat = 7
};

extern "C" {

int Init(int sample_rate_hz, int num_channels) {
    ::syslog(LOG_INFO, "DynamicProcessor: #Init; sample_rate_hz: %i, num_channels: %i", sample_rate_hz, num_channels);

    dlerror();
    auto krispDllPath = "libkrisp-audio-sdk.so";
    void *_dllHandle = dlopen(krispDllPath, RTLD_LAZY);
    if (!_dllHandle) {
        ::syslog(LOG_ERR, "DynamicProcessor: #Init; Failed to load the library = %s\n", krispDllPath);
        return -1;
    }

    const unsigned int _functionCount = 8;

    std::array<const char *, _functionCount> _functionNames =
            {
                    "krispAudioGlobalInit",
                    "krispAudioGlobalDestroy",
                    "krispAudioSetModel",
                    "krispAudioSetModelBlob",
                    "krispAudioRemoveModel",
                    "krispAudioNcCreateSession",
                    "krispAudioNcCloseSession",
                    "krispAudioNcCleanAmbientNoiseFloat"
            };

    std::array<void *, _functionCount> _functionPointers = {};

    for (size_t functionId = 0; functionId < _functionCount; ++functionId)
    {
        const char * functionName = _functionNames[functionId];
        syslog(LOG_INFO,"DynamicProcessor: #Init; Loading function: %s", functionName);
        void * functionPtr = dlsym(_dllHandle, functionName);
        const char* dlsym_error = dlerror();
        if (dlsym_error) {
            syslog(LOG_ERR, "DynamicProcessor: #Init; Failed to load the function: %s", dlsym_error);
            return -2;
        }
        _functionPointers[functionId] = functionPtr;
    }

    void* krispAudioGlobalInitPtr = _functionPointers[static_cast<size_t>(KrispFunctionId::krispAudioGlobalInit)];

    if (krispAudioGlobalInitPtr == nullptr) {
        syslog(LOG_ERR, "DynamicProcessor: #Init; Failed to get the krispAudioGlobalInit function");
        return -3;
    }

    using GlobalInitFuncType = int(*)(void*);

    auto globalInitFunc = reinterpret_cast<GlobalInitFuncType>(krispAudioGlobalInitPtr);

    int result = globalInitFunc(nullptr);

    if (result != 0) {
        syslog(LOG_ERR, "DynamicProcessor: #Init; Failed to initialize Krisp globals");
        return -4;
    }

    syslog(LOG_INFO, "DynamicProcessor: #Init; Successfully initialized Krisp globals!");

    return 0;
}

int ProcessFrame(float* const* channels,
                 size_t num_frames,
                 size_t num_bands,
                 size_t num_channels) {

    ::syslog(LOG_INFO, "DynamicProcessor: #ProcessFrame; num_frames: %zu, num_bands: %zu, num_channels: %zu", num_frames, num_bands, num_channels);
    return 0;
}

}