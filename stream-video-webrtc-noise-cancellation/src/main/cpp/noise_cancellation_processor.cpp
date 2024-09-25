#include <dlfcn.h>
#include <syslog.h>
#include <cstdarg>
#include <cstdlib>
#include <array>
#include <string>
#include <utility>

#include "noise_cancellation_processor.hpp"

#include "inc/krisp-audio-sdk.hpp"
#include "inc/krisp-audio-sdk-nc.hpp"

namespace krisp {

    std::wstring convertMBString2WString(const std::string& str) {
        std::wstring w(str.begin(), str.end());
        return w;
    }

    struct FunctionId {
        static constexpr unsigned int krispAudioGlobalInit = 0;
        static constexpr unsigned int krispAudioGlobalDestroy = 1;
        static constexpr unsigned int krispAudioSetModel = 2;
        static constexpr unsigned int krispAudioSetModelBlob = 3;
        static constexpr unsigned int krispAudioRemoveModel = 4;
        static constexpr unsigned int krispAudioNcCreateSession = 5;
        static constexpr unsigned int krispAudioNcCloseSession = 6;
        static constexpr unsigned int krispAudioNcCleanAmbientNoiseFloat = 7;
    };

    // Define the function pointer types
    using GlobalInitFuncType = int (*)(const wchar_t*);
    using GlobalDestroyFuncType = int (*)();
    using SetModelFuncType = int (*)(const wchar_t*, const char*);


    KrispAudioFrameDuration GetFrameDuration(size_t duration) {
        switch (duration) {
            case 10:
                return KRISP_AUDIO_FRAME_DURATION_10MS;
            default:
                ::syslog(LOG_INFO, "KRISP-CIT: Frame duration: %zu \
                is not supported. Switching to default 10ms", duration);
                return KRISP_AUDIO_FRAME_DURATION_10MS;
        }
    }

    KrispAudioSamplingRate GetSampleRate(size_t rate) {
        switch (rate) {
            case 8000:
                return KRISP_AUDIO_SAMPLING_RATE_8000HZ;
            case 16000:
                return KRISP_AUDIO_SAMPLING_RATE_16000HZ;
            case 24000:
                return KRISP_AUDIO_SAMPLING_RATE_24000HZ;
            case 32000:
                return KRISP_AUDIO_SAMPLING_RATE_32000HZ;
            case 44100:
                return KRISP_AUDIO_SAMPLING_RATE_44100HZ;
            case 48000:
                return KRISP_AUDIO_SAMPLING_RATE_48000HZ;
            case 88200:
                return KRISP_AUDIO_SAMPLING_RATE_88200HZ;
            case 96000:
                return KRISP_AUDIO_SAMPLING_RATE_96000HZ;
            default:
                ::syslog(LOG_INFO, "KRISP-CIT: The input sampling rate: %zu \
             is not supported. Using default 48khz.", rate);
                return KRISP_AUDIO_SAMPLING_RATE_48000HZ;
        }
    }

    static constexpr std::array<const char *, noise_cancellation::kFunctionCount> functionNames =
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
}

namespace noise_cancellation {

    NoiseCancellationProcessor* NoiseCancellationProcessor::m_instance = nullptr;


    NoiseCancellationProcessor::NoiseCancellationProcessor() {

    }

    NoiseCancellationProcessor::~NoiseCancellationProcessor() {

    }

    bool NoiseCancellationProcessor::Create() {
        auto krispDllPath = "libkrisp-audio-sdk.so";
        dlerror();
        m_handle = dlopen(krispDllPath, RTLD_LAZY);
        if (!m_handle) {
            syslog(LOG_ERR, "KrispNc: #Create; Failed to load the library = %s\n", krispDllPath);
            return false;
        }
        syslog(LOG_INFO, "KrispNc: #Create; Loaded: %s", krispDllPath);

        dlerror();
        for (size_t functionId = 0; functionId < kFunctionCount; ++functionId) {
            const char *functionName = krisp::functionNames[functionId];
            syslog(LOG_INFO, "KrispNc: #Create; load functionName: %s", functionName);
            void *functionPtr = dlsym(m_handle, functionName);
            const char *dlsym_error = dlerror();
            if (dlsym_error) {
                syslog(LOG_ERR, "KrispNc: #Create; Failed loading function: %s", dlsym_error);
                return false;
            }
            m_functionPointers[functionId] = functionPtr;
        }

        void *krispAudioGlobalInitPtr = m_functionPointers[krisp::FunctionId::krispAudioGlobalInit];

        if (krispAudioGlobalInitPtr == nullptr) {
            syslog(LOG_ERR, "KrispNc: #Create; Failed to get the krispAudioGlobalInit function");
            return false;
        }

        auto initFunc = reinterpret_cast<krisp::GlobalInitFuncType>(krispAudioGlobalInitPtr);
        if (initFunc(nullptr) != 0) {
            syslog(LOG_ERR, "KrispNc: #Create; Failed to initialize Krisp globals");
            return false;
        }

        syslog(LOG_INFO, "KrispNc: #Create; Successfully initialized Krisp globals!");


        //=========================================================================================================

        void *krispAudioSetModelPtr = m_functionPointers[krisp::FunctionId::krispAudioSetModel];

        if (krispAudioSetModelPtr == nullptr) {
            syslog(LOG_ERR, "KrispNc: #Create; Failed to get the krispAudioSetModel function");
            return false;
        }

        auto setModelFunc = reinterpret_cast<krisp::SetModelFuncType>(krispAudioSetModelPtr);

        const char *model = "/data/user/0/io.getstream.video.android.dogfooding.debug/files/krisp/c6.f.s.ced125.kw";
        auto final_model = krisp::convertMBString2WString(m_model_path);
        syslog(LOG_INFO, "KrispNc: #Create; m_model_path: %s", m_model_path);

        syslog(LOG_INFO, "KrispNc: #Create; final_model: %s", final_model.c_str());

        int setModelResult = setModelFunc(final_model.c_str(), "default");
        if (setModelResult != 0) {
            syslog(LOG_ERR, "KrispNc: #Create; Failed to set wt file: %s", model);
            return false;
        }


        return false;
    }

    bool NoiseCancellationProcessor::Destroy() {
        syslog(LOG_INFO, "KrispNc: #Destroy; no args");
        if (m_functionPointers.size() <=
            static_cast<size_t>(krisp::FunctionId::krispAudioGlobalDestroy)) {
            ::syslog(LOG_ERR,
                     "KrispNc: #Destroy; m_functionPointers is not large enough");
            return false;
        }

        void* krispAudioGlobalDestroyPtr = m_functionPointers[krisp::FunctionId::krispAudioGlobalDestroy];
        if (krispAudioGlobalDestroyPtr) {
            ::syslog(LOG_INFO,"KrispNc: #Destroy; Invoke krispAudioGlobalDestroy function");

            auto destroyFunc = reinterpret_cast<krisp::GlobalDestroyFuncType>(krispAudioGlobalDestroyPtr);
            if (destroyFunc()) {
                ::syslog(LOG_INFO,
                         "KrispNc: #Destroy; Invoked krispAudioGlobalDestroy successfully");
            }
        }
        for (auto& functionPtr : m_functionPointers) {
            functionPtr = nullptr;
        }
        if (m_handle) {
            dlclose(m_handle);
            m_handle = nullptr;
        }

        syslog(LOG_INFO, "KrispNc: #Destroy; Destroyed successfully");

        return true;
    }

    bool NoiseCancellationProcessor::Initialize(int sample_rate_hz, int num_channels) {
        syslog(LOG_INFO, "KrispNc: #Initialize; sample_rate_hz: %i, num_channels: %i", sample_rate_hz, num_channels);
        m_sample_rate_hz = sample_rate_hz;
        m_num_channels = num_channels;
        if (m_session == nullptr) {
            createSession(sample_rate_hz);
        }
        return true;
    }

    bool NoiseCancellationProcessor::ProcessFrame(float *const *channels, size_t num_frames,
                                                  size_t num_bands, size_t num_channels) {

        syslog(LOG_INFO, "KrispNc: #ProcessFrame; num_frames: %zu, num_bands: %zu, num_channels: %zu",
               num_frames, num_bands, num_channels);

        return true;
    }

    void NoiseCancellationProcessor::createSession(int rate) {
        auto krisp_rate = krisp::GetSampleRate(rate);
        auto krisp_duration = krisp::GetFrameDuration(10);
        //m_session = krispAudioNcCreateSession(krisp_rate, krisp_rate, krisp_duration, "default");
    }

    void NoiseCancellationProcessor::setModelPath(const char* model_path) {
        m_model_path = model_path;
    }

}
