///
/// Copyright Krisp, Inc
///

#ifndef KRISP_AUDIO_SDK_NC_HPP_
#define KRISP_AUDIO_SDK_NC_HPP_

#include "krisp-audio-sdk.hpp"
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

/*!
 * @brief This function creates Speech Enhance(Noise Canceler NC) session object
 *
 * @param[in] inputSampleRate Sampling frequency of the input data
 * @param[in] outputSampleRate Sampling frequency of the output data
 * @param[in] frameDuration Frame duration
 * @param[in] modelName The session ties to this model, and cleans the future frames using it.
 *              If <b> modelName </b> is \em nullptr than the sdk auto-detecs the model based on input sampleRate
 * @attention Always provide modelName explicitly to avoid ambiguity
 *
 * @return created session handle
 */
KRISP_AUDIO_API KrispAudioSessionID
krispAudioNcCreateSession(KrispAudioSamplingRate    inputSampleRate,
                          KrispAudioSamplingRate    outputSampleRate,
                          KrispAudioFrameDuration   frameDuration,
                          const char*               modelName);

/*!
 * @brief This function releases all data tied to this particular session, closes the given NC session
 *
 * @param[in, out] pSession Handle to the NC session to be closed
 *
 * @retval 0  success, negative on error
 */
KRISP_AUDIO_API int
krispAudioNcCloseSession(KrispAudioSessionID pSession);

/*!
 * @brief This function cleans the ambient noise for the given single frame. Works with shorts (int16) with value in range <b>[-2^15+1, 2^15]</b>
 *
 * @param[in] pSession The NC Session to which the frame belongs
 * @param[in] pFrameIn Pointer to input frame. It's a continuous buffer with overall size of <b> frameDuration * inputSampleRate / 1000</b>
 * @param[in] frameInSize This is input buffer size which must be <b> frameDuration * inputSampleRate / 1000 </b>
 * @param[in,out] pFrameOut Processed frames. The caller should allocate a buffer of at least <b> frameDuration * outputSampleRate / 1000 </b> size
 * @param[in] frameOutSize  : this is output buffer size which must be <b> frameDuration * outputSampleRate / 100 </b>
 *
 * @retval 0  success, negative on error
 */
KRISP_AUDIO_API int
krispAudioNcCleanAmbientNoiseInt16(KrispAudioSessionID  pSession,
                                   const short*         pFrameIn,
                                   unsigned int         frameInSize,
                                   short*               pFrameOut,
                                   unsigned int         frameOutSize);

/*!
 * @brief This function cleans the ambient noise for the given single frame,if there is no ringtone. Works with shorts (int16) with value in range <b>[-2^15+1, 2^15]</b>
 *
 * @param[in] pSession The NC Session to which the frame belongs
 * @param[in] pFrameIn Pointer to input frame. It's a continuous buffer with overall size of <b> frameDuration * inputSampleRate / 1000</b>
 * @param[in] frameInSize This is input buffer size which must be <b> frameDuration * inputSampleRate / 1000 </b>
 * @param[in,out] pFrameOut Processed frames. The caller should allocate a buffer of at least <b> frameDuration * outputSampleRate / 1000 </b> size
 * @param[in] frameOutSize  : this is output buffer size which must be <b> frameDuration * outputSampleRate / 100 </b>
 * @param[in] ringtone  : This specifies whether there is ringtone in the pFrameIn input buffer.
 *
 * @retval 0  success, negative on error
 */
KRISP_AUDIO_API int
krispAudioNcCleanAmbientNoiseWithRingtoneInt16(KrispAudioSessionID  pSession,
                                               const short*         pFrameIn,
                                               unsigned int         frameInSize,
                                               short*               pFrameOut,
                                               unsigned int         frameOutSize,
                                               bool                 ringtone);

/*!
 * @brief This function cleans the ambient noise for the given single frame. Works with floats with values normalized in range <b>[-1,1]</b>
 *
 * @param[in] pSession The NC Session to which the frame belongs
 * @param[in] pFrameIn Pointer to input frame. It's a continuous buffer with overall size of <b> frameDuration * inputSampleRate / 1000</b>
 * @param[in] frameInSize This is input buffer size which must be <b> frameDuration * inputSampleRate / 1000 </b>
 * @param[in,out] pFrameOut Processed frames. The caller should allocate a buffer of at least <b> frameDuration * outputSampleRate / 1000 </b> size
 * @param[in] frameOutSize  This is output buffer size which must be <b> frameDuration * outputSampleRate / 100 </b>
 *
 * @retval 0  success, negative on error
 */
KRISP_AUDIO_API int
krispAudioNcCleanAmbientNoiseFloat(KrispAudioSessionID  pSession,
                                   const float*         pFrameIn,
                                   unsigned int         frameInSize,
                                   float*               pFrameOut,
                                   unsigned int         frameOutSize);

/*!
 * @brief This function cleans the ambient noise for the given single frame, if there is no ringtone. Works with floats with values normalized in range <b>[-1,1]</b>
 *
 * @param[in] pSession The NC Session to which the frame belongs
 * @param[in] pFrameIn Pointer to input frame. It's a continuous buffer with overall size of <b> frameDuration * inputSampleRate / 1000</b>
 * @param[in] frameInSize This is input buffer size which must be <b> frameDuration * inputSampleRate / 1000 </b>
 * @param[in,out] pFrameOut Processed frames. The caller should allocate a buffer of at least <b> frameDuration * outputSampleRate / 1000 </b> size
 * @param[in] frameOutSize  This is output buffer size which must be <b> frameDuration * outputSampleRate / 100 </b>
 * @param[in] ringtone  : This specifies whether there is ringtone in the pFrameIn input buffer. * @retval 0  Success
 *
 * @retval 0  success, negative on error
 */
KRISP_AUDIO_API int
krispAudioNcCleanAmbientNoiseWithRingtoneFloat(KrispAudioSessionID  pSession,
                                               const float*         pFrameIn,
                                               unsigned int         frameInSize,
                                               float*               pFrameOut,
                                               unsigned int         frameOutSize,
                                               bool                 ringtone);

/*!
 * @brief This function turns on/off background speaker fix feature.
 *
 * @param[in] pSession   The NC Session to which the frame belongs
 * @param[in] on         on/off background speaker fix feature
 * @return 0   Value was set successfully
 * @return 1   Background speaker fix feature missing for this type of noise_cleaner
 *
 * @return -1, -2, -3, 2  Errors
 */
KRISP_AUDIO_API bool
krispAudioNcBackgroundSpeakerFixOnOff(KrispAudioSessionID pSession,
                                      bool                on);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif //// KRISP_AUDIO_SDK_NC_HPP_
