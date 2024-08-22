//
// Created by Kanat Kiialbaev on 2024-08-19.
//

#ifndef KRISP_AUDIO_FILTER_DYNAMIC_AUDIO_PROCESSOR_HPP
#define KRISP_AUDIO_FILTER_DYNAMIC_AUDIO_PROCESSOR_HPP


#include <cstddef>

#ifdef __cplusplus
extern "C" {
#endif

int Init(int sample_rate_hz, int num_channels);

int ProcessFrame(float* const* channels,
                 size_t num_frames,
                 size_t num_bands,
                 size_t num_channels);

#ifdef __cplusplus
}
#endif


#endif //KRISP_AUDIO_FILTER_DYNAMIC_AUDIO_PROCESSOR_HPP
