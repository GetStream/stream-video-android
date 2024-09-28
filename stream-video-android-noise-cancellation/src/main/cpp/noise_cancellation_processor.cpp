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
#include "string_utils.h"
#include "time_utils.h"

namespace krisp {

    constexpr size_t kNsFrameSize = 160;

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
    using RemoveModelFuncType = int (*)(const char*);
    using CreateSessionType = KrispAudioSessionID (*)(KrispAudioSamplingRate, KrispAudioSamplingRate,
                                                      KrispAudioFrameDuration, const char*);
    using CloseSessionType = int (*)(KrispAudioSessionID);
    using CleanAmbientNoiseFloatType = int (*)(KrispAudioSessionID, const float*, unsigned int, float*, unsigned int);

    KrispAudioFrameDuration GetFrameDuration(size_t duration) {
        switch (duration) {
            case 10:
                return KRISP_AUDIO_FRAME_DURATION_10MS;
            default:
                ::syslog(LOG_INFO, "KrispNc: #GetFrameDuration; Frame duration %zu is not supported. Switching to default 10ms", duration);
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
                ::syslog(LOG_INFO, "KrispNc: #GetSampleRate; The input sampling rate %zu is not supported. Using default 48khz.", rate);
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

    static constexpr const char* kKrispFilename = "libkrisp-audio-sdk.so";
    static constexpr const char* kKrispModelName = "default";

    NoiseCancellationProcessor* NoiseCancellationProcessor::m_instance = nullptr;


    NoiseCancellationProcessor::NoiseCancellationProcessor() = default;

    NoiseCancellationProcessor::~NoiseCancellationProcessor() {
        syslog(LOG_INFO, "KrispNc: #Destructor; no args");
        destroyAll();

        if (m_instance) {
            delete m_instance;
            m_instance = nullptr;
        }
    }

    void NoiseCancellationProcessor::SetModelPath(const std::wstring& model_path) {
        ::syslog(LOG_INFO, "KrispNc: #SetModelPath; model_path: %s", string_utils::convertWStringToString(model_path).c_str());
        m_model_path = model_path;
    }

    void NoiseCancellationProcessor::SetEnabled(bool enabled) {
        ::syslog(LOG_INFO, "KrispNc: #SetEnabled; enabled: %s", enabled ? "true" : "false");
        m_enabled = enabled;
    }

    [[nodiscard]] bool NoiseCancellationProcessor::IsEnabled() const {
        ::syslog(LOG_INFO, "KrispNc: #IsEnabled; no args");
        return m_enabled;
    }

    bool NoiseCancellationProcessor::Create() {
        dlerror();
        m_handle = dlopen(kKrispFilename, RTLD_LAZY);
        if (!m_handle) {
            syslog(LOG_ERR, "KrispNc: #Create; Failed to load the library = %s\n", kKrispFilename);
            return false;
        }
        syslog(LOG_INFO, "KrispNc: #Create; Loaded: %s", kKrispFilename);

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

        if (m_model_path.empty()) {
            syslog(LOG_ERR, "KrispNc: #Create; m_model_path is empty");
            return false;
        }

        void *krispAudioSetModelPtr = m_functionPointers[krisp::FunctionId::krispAudioSetModel];

        if (krispAudioSetModelPtr == nullptr) {
            syslog(LOG_ERR, "KrispNc: #Create; Failed to get the krispAudioSetModel function");
            return false;
        }

        auto setModelFunc = reinterpret_cast<krisp::SetModelFuncType>(krispAudioSetModelPtr);
        int setModelResult = setModelFunc(m_model_path.c_str(), kKrispModelName);
        if (setModelResult != 0) {
            auto model_path = string_utils::convertWStringToString(m_model_path);
            syslog(LOG_ERR, "KrispNc: #Create; Failed to set wt file: %s", model_path.c_str());
            return false;
        }
        syslog(LOG_INFO, "KrispNc: #Create; Successfully set model: %s", kKrispModelName);
        return true;
    }

    bool NoiseCancellationProcessor::Destroy() {
        syslog(LOG_INFO, "KrispNc: #Destroy; no args");
        destroyAll();

        syslog(LOG_INFO, "KrispNc: #Destroy; Destroyed successfully");

        return true;
    }

    bool NoiseCancellationProcessor::destroyAll() {
        if (!removeModel(kKrispModelName)) {
            syslog(LOG_WARNING, "KrispNc: #destroyAll; Failed to remove model: %s", kKrispModelName);
        }

        if (!closeSession(m_session)) {
            syslog(LOG_WARNING, "KrispNc: #destroyAll; Failed to close session");
        }
        m_session = nullptr;

        if (!globalDestroy()) {
            syslog(LOG_WARNING, "KrispNc: #destroyAll; Failed to destroy Krisp globals");
        }

        // Reset all function pointers
        for (auto& functionPtr : m_functionPointers) {
            functionPtr = nullptr;
        }
        if (m_handle) {
            dlclose(m_handle);
            m_handle = nullptr;
        }
        return true;
    }

    bool NoiseCancellationProcessor::Initialize(int sample_rate_hz, int num_channels) {
        syslog(LOG_INFO, "KrispNc: #Initialize; sample_rate_hz: %i, num_channels: %i", sample_rate_hz, num_channels);
        m_sample_rate_hz = sample_rate_hz;
        m_num_channels = num_channels;
        if (m_session == nullptr) {
            m_session = createSession(sample_rate_hz);
        }
        return true;
    }

    bool NoiseCancellationProcessor::ProcessFrame(
            float *const *channels,
            size_t num_frames,
            size_t num_bands,
            size_t num_channels
    ) {

        auto now = time_utils::TimeMillis();
        constexpr const long k_logs_interval = 10000;
        constexpr const long k_stats_interval = 10000;

        if(!m_enabled) {
            if (now - m_last_logs_ts > k_logs_interval) {
                ::syslog(LOG_DEBUG, "KrispNc: #ProcessFrame; Noise cancellation is disabled");
                m_last_logs_ts = now;
            }
            return false;
        }

        int rate = num_frames * 1000;
        if (now - m_last_stats_ts > k_stats_interval) {
            ::syslog(LOG_INFO, "KrispNc: #ProcessFrame; num_frames: %zu, num_bands: %zu, num_channels: %zu, rate: %i",
                     num_frames, num_bands, num_channels, rate);

            m_last_stats_ts = now;
        }
        if(rate != m_sample_rate_hz) {
            Reset(rate);
        }

        if (m_session == nullptr) {
            ::syslog(LOG_INFO, "KrispNc: #ProcessFrame; Session creation failed");
            return false;
        }

        std::vector<float> bufferIn;
        std::vector<float> bufferOut;
        auto num_bands_ = num_bands;

        auto kNsFrameSize = krisp::kNsFrameSize;
        bufferIn.resize(kNsFrameSize * num_bands_);
        bufferOut.resize(kNsFrameSize * num_bands_);

        for (size_t jj = 0; jj < kNsFrameSize*num_bands_; ++jj) {
            bufferIn[jj] = channels[0][jj] / 32768.f;
        }

        const auto ret_val = cleanAmbientNoise(
                m_session, bufferIn.data(), num_bands_ * kNsFrameSize,
                bufferOut.data(), num_bands_ * kNsFrameSize);
        if (ret_val != 0) {
            ::syslog(LOG_INFO, "KrispNc: #ProcessFrame; Krisp noise cleanup error");
            return false;
        }

        for (size_t jj = 0; jj < kNsFrameSize*num_bands_; ++jj) {
            channels[0][jj] = bufferOut[jj] * 32768.f;
        }

        return true;
    }

    void NoiseCancellationProcessor::Reset(int new_rate) {
        syslog(LOG_INFO, "KrispNc: #Reset; new_rate: %i", new_rate);
        closeSession(m_session);
        m_sample_rate_hz = new_rate;
        m_session = createSession(new_rate);
    }

    int NoiseCancellationProcessor::cleanAmbientNoise(void *session, const float *pFrameIn,
                                                      unsigned int frameInSize, float *pFrameOut,
                                                      unsigned int frameOutSize) {
        void* krispAudioNcCleanAmbientNoiseFloatPtr = m_functionPointers[krisp::FunctionId::krispAudioNcCleanAmbientNoiseFloat];
        if (krispAudioNcCleanAmbientNoiseFloatPtr == nullptr) {
            syslog(LOG_ERR, "KrispNc: #cleanAmbientNoise; Failed to get the krispAudioNcCleanAmbientNoiseFloat function");
            return -1;
        }

        auto cleanAmbientNoiseFunc = reinterpret_cast<krisp::CleanAmbientNoiseFloatType>(krispAudioNcCleanAmbientNoiseFloatPtr);

        return cleanAmbientNoiseFunc(session, pFrameIn, frameInSize, pFrameOut, frameOutSize);
    }

    bool NoiseCancellationProcessor::globalDestroy() {
        syslog(LOG_INFO, "KrispNc: #globalDestroy; no args");
        void* krispAudioGlobalDestroyPtr = m_functionPointers[krisp::FunctionId::krispAudioGlobalDestroy];
        if (krispAudioGlobalDestroyPtr == nullptr) {
            syslog(LOG_ERR, "KrispNc: #globalDestroy; Failed to get the krispAudioGlobalDestroy function");
            return false;
        }
        auto destroyFunc = reinterpret_cast<krisp::GlobalDestroyFuncType>(krispAudioGlobalDestroyPtr);
        if (destroyFunc() != 0) {
            syslog(LOG_ERR, "KrispNc: #globalDestroy; Failed to destroy Krisp globals");
            return false;
        }
        ::syslog(LOG_INFO, "KrispNc: #globalDestroy; Invoked krispAudioGlobalDestroy successfully");
        return true;
    }

    bool NoiseCancellationProcessor::removeModel(const char* modelName) {
        syslog(LOG_INFO, "KrispNc: #removeModel; modelName: %s", modelName);
        if (m_model_path.empty()) {
            syslog(LOG_ERR, "KrispNc: #removeModel; m_model_path is empty");
            return false;
        }

        void *krispAudioRemoveModelPtr = m_functionPointers[krisp::FunctionId::krispAudioRemoveModel];
        if (krispAudioRemoveModelPtr == nullptr) {
            syslog(LOG_ERR, "KrispNc: #removeModel; Failed to get the krispAudioRemoveModel function");
            return false;
        }

        auto removeModelFunc = reinterpret_cast<krisp::RemoveModelFuncType>(krispAudioRemoveModelPtr);
        if (removeModelFunc(modelName) != 0) {
            syslog(LOG_ERR, "KrispNc: #removeModel; Failed to remove model: %s", modelName);
            return false;
        }
        return true;
    }

    bool NoiseCancellationProcessor::closeSession(void* session) {
        if (session == nullptr) {
            syslog(LOG_INFO, "KrispNc: #closeSession; session is null");
            return false;
        }
        void* krispAudioNcCloseSessionPtr = m_functionPointers[krisp::FunctionId::krispAudioNcCloseSession];
        if (krispAudioNcCloseSessionPtr == nullptr) {
            syslog(LOG_ERR, "KrispNc: #closeSession; Failed to get the krispAudioNcCloseSession function");
            return false;
        }
        auto closeSessionFunc = reinterpret_cast<krisp::CloseSessionType>(krispAudioNcCloseSessionPtr);
        auto closeSessionResult = closeSessionFunc(session);
        if (closeSessionResult != 0) {
            syslog(LOG_ERR, "KrispNc: #closeSession; Failed to close the session");
            return false;
        }
        return true;
    }

    void* NoiseCancellationProcessor::createSession(int rate) {
        auto krisp_rate = krisp::GetSampleRate(rate);
        auto krisp_duration = krisp::GetFrameDuration(10);
        syslog(LOG_INFO, "KrispNc: #createSession; krisp_rate: %i, krisp_duration: %i", krisp_rate, krisp_duration);

        void *krispAudioNcCreateSessionPtr = m_functionPointers[krisp::FunctionId::krispAudioNcCreateSession];

        if (krispAudioNcCreateSessionPtr == nullptr) {
            syslog(LOG_ERR, "KrispNc: #Create; Failed to get the krispAudioNcCreateSession function");
            return nullptr;
        }

        auto createSessionFunc = reinterpret_cast<krisp::CreateSessionType>(krispAudioNcCreateSessionPtr);
        return createSessionFunc(krisp_rate, krisp_rate, krisp_duration, kKrispModelName);
    }

}
