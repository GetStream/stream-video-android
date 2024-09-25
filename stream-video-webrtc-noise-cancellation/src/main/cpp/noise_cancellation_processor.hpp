#ifndef NOISE_CANCELLATION_PROCESSOR_HPP
#define NOISE_CANCELLATION_PROCESSOR_HPP

#include <string>

#include "include/external_processor.hpp"

namespace noise_cancellation {

    static constexpr unsigned int kFunctionCount = 8;

    class NoiseCancellationProcessor : public external::ExternalProcessor {
    public:
        NoiseCancellationProcessor(const NoiseCancellationProcessor &) = delete;

        NoiseCancellationProcessor(NoiseCancellationProcessor &&) = delete;

        NoiseCancellationProcessor &operator=(const NoiseCancellationProcessor &) = delete;

        NoiseCancellationProcessor &operator=(NoiseCancellationProcessor &&) = delete;

        ~NoiseCancellationProcessor() override;

        static NoiseCancellationProcessor *getInstance() {
            if (m_instance == nullptr) {
                m_instance = new NoiseCancellationProcessor();
            }
            return m_instance;
        }

        void setModelPath(const std::string& model_path);

        bool Create() override;

        bool Destroy() override;

        bool Initialize(int sample_rate_hz, int num_channels) override;

        bool ProcessFrame(float *const *channels,
                         size_t num_frames,
                         size_t num_bands,
                         size_t num_channels) override;

    private:
        NoiseCancellationProcessor();

        std::string m_model_path;

        void* m_handle = nullptr;
        std::array<void *, kFunctionCount> m_functionPointers = {};

        bool m_enabled;
        int m_sample_rate_hz;
        int m_num_channels;
        long m_last_time_stamp;
        void* m_session;

        static NoiseCancellationProcessor* m_instance;

        void createSession(int rate);
    };

}  // namespace noise_cancellation

#endif  // NOISE_CANCELLATION_PROCESSOR_HPP
