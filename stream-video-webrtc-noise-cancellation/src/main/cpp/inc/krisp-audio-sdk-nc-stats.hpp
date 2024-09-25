///
/// Copyright Krisp, Inc
///

#ifndef KRISP_AUDIO_SDK_NC_STATS_HPP_
#define KRISP_AUDIO_SDK_NC_STATS_HPP_

#include "krisp-audio-sdk.hpp"

/*!
 * Cleaned secondary speech states
 */
typedef enum {
    UNDEFINED       = 0,
    DETECTED        = 1,
    NOT_DETECTED    = 2
} KrispAudioCleanedSecondarySpeechStatus;

/*!
 * Krisp audio per-frame info
 */
typedef struct krispAudioNcPerFrameInfo_t {
    /*!
     * Voice energy level in the processed frame. Value range [0,100]
     */
    unsigned int voiceEnergy;

    /*!
     * Noise energy level in the processed frame. Value range [0,100]
     */
    unsigned int noiseEnergy;

    /*!
     * BVC specific feature.
     * Returns the removed secondary speech state, e.g. in case if secondary speech detected and removed returns TRUE,
     * otherwise returns FALSE.
     * UNDEFINED will be returned in case of running the NC.
     */
    KrispAudioCleanedSecondarySpeechStatus cleanedSecondarySpeechStatus;
} KrispAudioNcPerFrameInfo;

/*!
 * Krisp audio voice stats
 */
typedef struct krispAudioNcVoiceStats_t {
    unsigned int talkTimeMs;
} KrispAudioNcVoiceStats;

/*!
 * Krisp audio noise stats based on the noise intensity level
 */
typedef struct krispAudioNcNoiseStats_t {
    unsigned int noNoiseMs;
    unsigned int lowNoiseMs;
    unsigned int mediumNoiseMs;
    unsigned int highNoiseMs;
} KrispAudioNcNoiseStats;

/*!
 * Krisp audio noise/voice stats
 */
typedef struct krispAudioNcStats_t {
    KrispAudioNcVoiceStats  voiceStats;
    KrispAudioNcNoiseStats  noiseStats;
} KrispAudioNcStats;

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

/*!
 * @brief This function creates Speech Enhance(Noise Canceler NC with stats) session object
 * 
 * @param[in] inputSampleRate Sampling frequency of the input data
 * @param[in] outputSampleRate Sampling frequency of the output data
 * @param[in] frameDuration Frame duration
 * @param[in] modelName The session ties to this model, and cleans the future frames using it.
 *              If <b> modelName </b> is \em nullptr then the SDK auto-detects the model based on input sampleRate
 * @attention Always provide modelName explicitly to avoid ambiguity
 *
 * @return created session handle
 */
KRISP_AUDIO_API KrispAudioSessionID
krispAudioNcWithStatsCreateSession(KrispAudioSamplingRate   inputSampleRate,
                                   KrispAudioSamplingRate   outputSampleRate,
                                   KrispAudioFrameDuration  frameDuration,
                                   const char*              modelName);

/*!
 * @brief This function releases all data tied to this particular session, closes the given NC session
 *
 * @param[in, out] pSession Handle to the NC with stats session to be closed
 *
 * @retval 0  success, negative on error
 */
KRISP_AUDIO_API int
krispAudioNcWithStatsCloseSession(KrispAudioSessionID pSession);

/*!
 * @brief This function cleans the ambient noise for the given single frame. Works with shorts (int16) with value in range <b>[-2^15+1, 2^15]</b>
 * 
 * @param[in] pSession The NC With Stats Session to which the frame belongs
 * @param[in] pFrameIn Pointer to input frame. It's a continuous buffer with overall size of <b> frameDuration * inputSampleRate / 1000</b>
 * @param[in] frameInSize This is input buffer size which must be <b> frameDuration * inputSampleRate / 1000 </b>
 * @param[in,out] pFrameOut Processed frames. The caller should allocate a buffer of at least <b> frameDuration * outputSampleRate / 1000 </b> size
 * @param[in] frameOutSize  : this is output buffer size which must be <b> frameDuration * outputSampleRate / 100 </b>
 * @param[out] energyInfo Returns voice and noise energy levels of the current frame
 * 
 * @retval 0  success, negative on error
 */
KRISP_AUDIO_API int
krispAudioNcWithStatsCleanAmbientNoiseInt16(KrispAudioSessionID             pSession,
                                            const short*                    pFrameIn,
                                            unsigned int                    frameInSize,
                                            short*                          pFrameOut,
                                            unsigned int                    frameOutSize,
                                            KrispAudioNcPerFrameInfo*       energyInfo);

/*!
 * @brief This function cleans the ambient noise for the given single frame,if there is no ringtone. Works with shorts (int16) with value in range <b>[-2^15+1, 2^15]</b>
 * 
 * @param[in] pSession The NC With Stats Session to which the frame belongs
 * @param[in] pFrameIn Pointer to input frame. It's a continuous buffer with overall size of <b> frameDuration * inputSampleRate / 1000</b>
 * @param[in] frameInSize This is input buffer size which must be <b> frameDuration * inputSampleRate / 1000 </b>
 * @param[in,out] pFrameOut Processed frames. The caller should allocate a buffer of at least <b> frameDuration * outputSampleRate / 1000 </b> size
 * @param[in] frameOutSize  : this is output buffer size which must be <b> frameDuration * outputSampleRate / 100 </b>
 * @param[in] ringtone  : This specifies whether there is ringtone in the pFrameIn input buffer.
 * @param[out] energyInfo Returns voice and noise energy levels of the current frame if ringtone is false otherwise 0.
 * 
 * @retval 0  success, negative on error
 */
KRISP_AUDIO_API int
krispAudioNcWithStatsCleanAmbientNoiseWithRingtoneInt16(KrispAudioSessionID             pSession,
                                                        const short*                    pFrameIn,
                                                        unsigned int                    frameInSize,
                                                        short*                          pFrameOut,
                                                        unsigned int                    frameOutSize,
                                                        bool                            ringtone,
                                                        KrispAudioNcPerFrameInfo*       energyInfo);

/*!
 * @brief This function cleans the ambient noise for the given single frame. Works with floats with values normalized in range <b>[-1,1]</b>
 * @param[in] pSession The NC With Stats Session to which the frame belongs
 * @param[in] pFrameIn Pointer to input frame. It's a continuous buffer with overall size of <b> frameDuration * inputSampleRate / 1000</b>
 * @param[in] frameInSize This is input buffer size which must be <b> frameDuration * inputSampleRate / 1000 </b>
 * @param[in,out] pFrameOut Processed frames. The caller should allocate a buffer of at least <b> frameDuration * outputSampleRate / 1000 </b> size
 * @param[in] frameOutSize  This is output buffer size which must be <b> frameDuration * outputSampleRate / 100 </b>
 * @param[out] energyInfo Returns voice and noise energy levels of the current frame
 * 
 * @retval 0  success, negative on error
 */
KRISP_AUDIO_API int
krispAudioNcWithStatsCleanAmbientNoiseFloat(KrispAudioSessionID             pSession,
                                            const float*                    pFrameIn,
                                            unsigned int                    frameInSize,
                                            float*                          pFrameOut,
                                            unsigned int                    frameOutSize,
                                            KrispAudioNcPerFrameInfo*       energyInfo);

/*!
 * @brief This function cleans the ambient noise for the given single frame, if there is no ringtone. Works with floats with values normalized in range <b>[-1,1]</b>
 * 
 * @param[in] pSession The NC With Stats Session to which the frame belongs
 * @param[in] pFrameIn Pointer to input frame. It's a continuous buffer with overall size of <b> frameDuration * inputSampleRate / 1000</b>
 * @param[in] frameInSize This is input buffer size which must be <b> frameDuration * inputSampleRate / 1000 </b>
 * @param[in,out] pFrameOut Processed frames. The caller should allocate a buffer of at least <b> frameDuration * outputSampleRate / 1000 </b> size
 * @param[in] frameOutSize  This is output buffer size which must be <b> frameDuration * outputSampleRate / 100 </b>
 * @param[in] ringtone  : This specifies whether there is ringtone in the pFrameIn input buffer. * @retval 0  Success
 * @param[out] energyInfo Returns voice and noise energy levels of the current frame if ringtone is false otherwise 0.
 * 
 * @retval 0  success, negative on error
 */
KRISP_AUDIO_API int
krispAudioNcWithStatsCleanAmbientNoiseWithRingtoneFloat(KrispAudioSessionID             pSession,
                                                        const float*                    pFrameIn,
                                                        unsigned int                    frameInSize,
                                                        float*                          pFrameOut,
                                                        unsigned int                    frameOutSize,
                                                        bool                            ringtone,
                                                        KrispAudioNcPerFrameInfo*       energyInfo);

/*!
 * @brief This function used to retrieve the noise/voice stats while processing noise canceler.
 *        The recommended stats retrieval frequency is bigger or equal to 200ms.
 *        If it's required only at the end of the noise canceler processing (end of the call/audio stream)
 *        function should be called before pSession becomes invalid, i.e. after closing the specified session.
 * 
 * @param[in]  pSession The NC With Stats Session to which the stats belongs
 * @param[out] pStats   Noise/Voice stats returned
 * 
 * @retval 0  success, negative on error
 */
KRISP_AUDIO_API int
krispAudioNcWithStatsRetrieveStats(KrispAudioSessionID  pSession,
                                   KrispAudioNcStats*   pStats);


#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif // KRISP_AUDIO_SDK_NC_STATS_HPP_
