///
/// Copyright Krisp, Inc
///

#ifndef KRISP_AUDIO_SDK_RT_HPP_
#define KRISP_AUDIO_SDK_RT_HPP_

#include "krisp-audio-sdk.hpp"

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

/*!
 * @brief This function creates Ringtone detection session object
 * 
 * @param[in] inputSampleRate Sampling frequency of the input data.
 * @param[in] frameDuration Frame duration
 * @param[in] modelName The session ties to this model, and processes the future frames using it
 *            If <b> modelName </b> is \em nullptr then the SDK auto-detects the model based on input sampleRate.
 * @attention Always provide modelName explicitly to avoid ambiguity
 *
 * @return created session handle
 */
KRISP_AUDIO_API KrispAudioSessionID
krispAudioRingtoneCreateSession(KrispAudioSamplingRate  inputSampleRate,
                                KrispAudioFrameDuration frameDuration,
                                const char*             modelName);

/*!
 * @brief This function releases all data tied to this particular session, closes the given Ringtone session
 * 
 * @param[in, out] pSession Handle to the Ringtone session to be closed
 * 
 * @retval 0  success, negative on error
 */
KRISP_AUDIO_API int
krispAudioRingtoneCloseSession(KrispAudioSessionID pSession);

/*!
 * @brief This function processes the given frame and returns the Ringtone detection value. Works with shorts (int16) with value in range <b>[-2^15+1, 2^15]</b>
 * 
 * @param[in] pSession The Ringtone Session to which the frame belongs
 * @param[in] pFrameIn Pointer to input frame. It's a continuous buffer with overall size of <b> frameDuration * inputSampleRate / 1000 </b>
 * @param[in] frameInSize This is buffer size which must be <b> frameDuration * inputSampleRate / 1000 </b>
 * 
 * @return Value in range [0,1].
 */
KRISP_AUDIO_API float
krispAudioDetectRingtoneFrameInt16(KrispAudioSessionID  pSession,
                                   const short*         pFrameIn,
                                   unsigned int         frameInSize);

/*!
 * @brief This function processes the given frame and returns the Ringtone detection value. Works with float values normalized in range <b> [-1,1] </b>
 * 
 * @param[in] pSession The Ringtone Session to which the frame belongs
 * @param[in] pFrameIn Pointer to input frame. It's a continuous buffer with overall size of <b> frameDuration * inputSampleRate / 1000 </b>
 * @param[in] frameInSize This is buffer size which must be <b> frameDuration * inputSampleRate / 1000 </b>
 * 
 * @return Value in range [0,1].
 */
KRISP_AUDIO_API float
krispAudioDetectRingtoneFrameFloat(KrispAudioSessionID  pSession,
                                   const float*         pFrameIn,
                                   unsigned int         frameInSize);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif //// KRISP_AUDIO_SDK_RT_HPP_
