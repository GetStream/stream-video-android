#ifndef EXTERNAL_PROCESSOR_HPP
#define EXTERNAL_PROCESSOR_HPP

#include <cstdint>
#include <array>

namespace external {

    class ExternalProcessor {
    public:
        // Creates all necessary resources for the processor.
        virtual bool Create() = 0;
        // Destroys the processor.
        virtual bool Destroy() = 0;
        // Initializes the processor with a specific sample rate and number of
        // channels.
        virtual bool Initialize(int sample_rate_hz, int num_channels) = 0;
        // Processes the audio data.
        virtual bool ProcessFrame(float* const* channels,
                                 size_t num_frames,
                                 size_t num_bands,
                                 size_t num_channels) = 0;

        virtual ~ExternalProcessor() {}
    };

    extern "C" bool ExternalProcessorCreate();

    extern "C" bool ExternalProcessorInitialize(int sample_rate_hz,
                                                int num_channels);

    extern "C" bool ExternalProcessorProcessFrame(float* const* channels,
                                                  size_t num_frames,
                                                  size_t num_bands,
                                                  size_t num_channels);

    extern "C" bool ExternalProcessorDestroy();

}  // namespace external

#endif  // EXTERNAL_PROCESSOR_HPP
