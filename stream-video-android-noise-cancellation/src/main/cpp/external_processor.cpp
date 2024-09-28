#include <syslog.h>
#include <dlfcn.h>

#include "include/external_processor.hpp"
#include "noise_cancellation_processor.hpp"


namespace external {

    noise_cancellation::NoiseCancellationProcessor* processor_ptr = nullptr;

    extern "C" bool ExternalProcessorCreate() {
        syslog(LOG_INFO, "ExternalProcessor: #Create; no args");
        processor_ptr = noise_cancellation::NoiseCancellationProcessor::getInstance();

        if (processor_ptr == nullptr) {
            syslog(LOG_ERR, "ExternalProcessor: #Create; processor_ptr is nullptr");
            return false;
        }
        processor_ptr->Create();
        return true;
    }

    extern "C" bool ExternalProcessorInitialize(int sample_rate_hz,
                                                int num_channels) {
        if (processor_ptr == nullptr) {
            syslog(LOG_ERR, "ExternalProcessor: #Init; processor_ptr is nullptr");
            return false;
        }
        syslog(LOG_INFO, "ExternalProcessor: #Init; sample_rate_hz: %i, num_channels: %i",
               sample_rate_hz, num_channels);
        processor_ptr->Initialize(sample_rate_hz, num_channels);
        return true;
    }

    extern "C" bool ExternalProcessorProcessFrame(float *const *channels,
                                                  size_t num_frames,
                                                  size_t num_bands,
                                                  size_t num_channels) {


        if (processor_ptr == nullptr) {
            syslog(LOG_ERR, "ExternalProcessor: #ProcessFrame; processor_ptr is nullptr");
            return false;
        }
        processor_ptr->ProcessFrame(channels, num_frames, num_bands, num_channels);
        return true;
    }

    extern "C" bool ExternalProcessorDestroy() {
        if (processor_ptr == nullptr) {
            syslog(LOG_ERR, "ExternalProcessor: #Destroy; processor_ptr is nullptr");
            return false;
        }
        syslog(LOG_INFO, "ExternalProcessor: #Destroy; no args");
        processor_ptr->Destroy();
        return true;
    }

}


