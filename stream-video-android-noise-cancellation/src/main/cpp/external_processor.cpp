#include <syslog.h>
#include <dlfcn.h>

#include "include/external_processor.hpp"
#include "noise_cancellation_processor.hpp"


namespace external {

    ExternalProcessor* processor_ptr = nullptr;

    extern "C" bool ExternalProcessorCreate() {
        syslog(LOG_INFO, "ExternalProcessorImpl: #Create; no args");
        processor_ptr = noise_cancellation::NoiseCancellationProcessor::getInstance();

        if (processor_ptr == nullptr) {
            syslog(LOG_ERR, "ExternalProcessorImpl: #Create; processor_ptr is nullptr");
            return false;
        }
        return processor_ptr->Create();
    }

    extern "C" bool ExternalProcessorInitialize(int sample_rate_hz,
                                                int num_channels) {
        if (processor_ptr == nullptr) {
            syslog(LOG_ERR, "ExternalProcessorImpl: #Init; processor_ptr is nullptr");
            return false;
        }
        syslog(LOG_INFO, "ExternalProcessorImpl: #Init; sample_rate_hz: %i, num_channels: %i",
               sample_rate_hz, num_channels);
        return processor_ptr->Initialize(sample_rate_hz, num_channels);
    }

    extern "C" bool ExternalProcessorProcessFrame(float *const *channels,
                                                  size_t num_frames,
                                                  size_t num_bands,
                                                  size_t num_channels) {


        if (processor_ptr == nullptr) {
            syslog(LOG_ERR, "ExternalProcessorImpl: #ProcessFrame; processor_ptr is nullptr");
            return false;
        }
        return processor_ptr->ProcessFrame(channels, num_frames, num_bands, num_channels);
    }

    extern "C" bool ExternalProcessorDestroy() {
        if (processor_ptr == nullptr) {
            syslog(LOG_ERR, "ExternalProcessorImpl: #Destroy; processor_ptr is nullptr");
            return false;
        }
        syslog(LOG_INFO, "ExternalProcessorImpl: #Destroy; no args");
        return processor_ptr->Destroy();
    }

}


