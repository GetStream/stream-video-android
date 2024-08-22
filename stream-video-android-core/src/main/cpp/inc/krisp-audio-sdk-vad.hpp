///
/// Copyright Krisp, Inc
///

#ifndef KRISP_AUDIO_SDK_VAD_HPP_
#define KRISP_AUDIO_SDK_VAD_HPP_

#include "krisp-audio-sdk.hpp"

/*!
 * Krisp Audio bandwidth values
 */
typedef enum {
    BAND_WIDTH_UNKNOWN   = 0,
    BAND_WIDTH_4000HZ    = 1,
    BAND_WIDTH_8000HZ    = 2,
    BAND_WIDTH_16000HZ   = 3,
} KrispAudioBandWidth;

/*!
 * Krisp Audio real bandwidth info struct used by krispAudioVadFrameInt16Ex() and
 * krispAudioVadFrameFloatEx() APIs
 */
typedef struct KrispAudioBandWidthInfo_t {
    /* [out] Predicted real bandwidth, one of the @KrispAudioBandWidth values */
    KrispAudioBandWidth     realBandwidth;
    /* [in] Algorithm processing start point */
    int                     procStartDelayMs;
    /* [in] Algorithm processing duration counted from the procStartDelayMs */
    int                     procDurationMs;
    int                     reserved;
} KrispAudioBandWidthInfo;

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

/*!
 * @brief This function creates Voice Activity Detection session object ( VAD )
 *
 * @param[in] inputSampleRate Sampling frequency of the input data.
 * @param[in] frameDuration Frame duration
 * @param[in] modelName The session ties to this model, and processes the future frames using it
 *              If <b> modelName </b> is \em nullptr then the SDK auto-detects the model based on input sampleRate.
 * @attention Always provide modelName explicitly to avoid ambiguity
 *
 * @return created session handle
 */
KRISP_AUDIO_API KrispAudioSessionID
krispAudioVadCreateSession(KrispAudioSamplingRate   inputSampleRate,
                           KrispAudioFrameDuration  frameDuration,
                           const char*              modelName);

/*!
 * @brief This function releases all data tied to this particular session, closes the given VAD session
 *
 * @param[in, out] pSession Handle to the VAD session to be closed
 *
 * @retval 0  success, negative on error
 */
KRISP_AUDIO_API int
krispAudioVadCloseSession(KrispAudioSessionID pSession);

/*!
 * @brief This function processes the given frame and returns the VAD detection value. Works with shorts (int16) with value in range <b>[-2^15+1, 2^15]</b>
 *
 * @param[in] pSession The VAD Session to which the frame belongs
 * @param[in] pFrameIn Pointer to input frame. It's a continuous buffer with overall size of <b> frameDuration * inputSampleRate / 1000 </b>
 * @param[in] frameInSize This is buffer size which must be <b> frameDuration * inputSampleRate / 1000 </b>
 *
 * @return Value in range [0,1]. The scale is adjusted so that 0.5 corresponds to the best F1 score on our test dataset (based on TIMIT core test dataset speech examples).
 *      The Threshold needs to be adjusted to fit a particular use case.
 */
KRISP_AUDIO_API float
krispAudioVadFrameInt16(KrispAudioSessionID   pSession,
                        const short*          pFrameIn,
                        unsigned int          frameInSize);

/*!
 * @brief This function processes the given frame and returns the VAD detection value. Works with shorts (int16) with value in range <b>[-2^15+1, 2^15]</b>
 *
 * @param[in]     pSession The VAD Session to which the frame belongs
 * @param[in]     pFrameIn Pointer to input frame. It's a continuous buffer with overall size of <b> frameDuration * inputSampleRate / 1000 </b>
 * @param[in]     frameInSize This is buffer size which must be <b> frameDuration * inputSampleRate / 1000 </b>
 * @param[in,out] bandwidthInfo Returns BAND_WIDTH_UNKNOWN if still not predicted, otherwise the real bandwidth: one of the KrispAudioBandWidth values
 *
 * @return Value in range [0,1]. The scale is adjusted so that 0.5 corresponds to the best F1 score on our test dataset (based on TIMIT core test dataset speech examples).
 *      The Threshold needs to be adjusted to fit a particular use case.
 */
KRISP_AUDIO_API float
krispAudioVadFrameInt16Ex(KrispAudioSessionID       pSession,
                          const short*              pFrameIn,
                          unsigned int              frameInSize,
                          KrispAudioBandWidthInfo*  bandwidthInfo);

/*!
 * @brief This function processes the given frame and returns the VAD detection value. Works with float values normalized in range <b> [-1,1] </b>
 *
 * @param[in] pSession The VAD Session to which the frame belongs
 * @param[in] pFrameIn Pointer to input frame. It's a continuous buffer with overall size of <b> frameDuration * inputSampleRate / 1000 </b>
 * @param[in] frameInSize This is buffer size which must be <b> frameDuration * inputSampleRate / 1000 </b>
 *
 * @return Value in range [0,1]. The scale is adjusted so that 0.5 corresponds to the best F1 score on our test dataset (based on TIMIT core test dataset speech examples).
 *      The Threshold needs to be adjusted to fit a particular use case.
 */
KRISP_AUDIO_API float
krispAudioVadFrameFloat(KrispAudioSessionID pSession,
                        const float*        pFrameIn,
                        unsigned int        frameInSize);


/*!
 * @brief This function processes the given frame and returns the VAD detection value. Works with float values normalized in range <b> [-1,1] </b>
 *
 * @param[in]     pSession The VAD Session to which the frame belongs
 * @param[in]     pFrameIn Pointer to input frame. It's a continuous buffer with overall size of <b> frameDuration * inputSampleRate / 1000 </b>
 * @param[in]     frameInSize This is buffer size which must be <b> frameDuration * inputSampleRate / 1000 </b>
 * @param[in,out] bandwidthInfo Returns BAND_WIDTH_UNKNOWN if still not predicted, otherwise the real bandwidth: one of the KrispAudioBandWidth values
 *
 * @return Value in range [0,1]. The scale is adjusted so that 0.5 corresponds to the best F1 score on our test dataset (based on TIMIT core test dataset speech examples).
 *      The Threshold needs to be adjusted to fit a particular use case.
 */
KRISP_AUDIO_API float
krispAudioVadFrameFloatEx(KrispAudioSessionID       pSession,
                          const float*              pFrameIn,
                          unsigned int              frameInSize,
                          KrispAudioBandWidthInfo*  bandwidthInfo);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif //// KRISP_AUDIO_SDK_VAD_HPP_
