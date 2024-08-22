///
/// Copyright Krisp, Inc
///

#ifndef KRISP_AUDIO_SDK_HPP_
#define KRISP_AUDIO_SDK_HPP_
#if defined _WIN32 || defined __CYGWIN__
    #ifdef KRISP_AUDIO_STATIC
        #define KRISP_AUDIO_API
    #else
        #ifdef KRISP_AUDIO_EXPORTS
            #ifdef __GNUC__
                #define KRISP_AUDIO_API __attribute__ ((dllexport))
            #else
                #define KRISP_AUDIO_API __declspec(dllexport) // Note: actually gcc seems to also support this syntax.
            #endif
        #else
            #ifdef __GNUC__
                #define KRISP_AUDIO_API __attribute__ ((dllimport))
            #else
                #define KRISP_AUDIO_API __declspec(dllimport) // Note: actually gcc seems to also support this syntax.
            #endif
        #endif
    #endif
#else
    #if __GNUC__ >= 4
        #define KRISP_AUDIO_API __attribute__ ((visibility ("default")))
    #else
        #define KRISP_AUDIO_API
    #endif
#endif

typedef void*  KrispAudioSessionID;

typedef enum {
    KRISP_AUDIO_SAMPLING_RATE_8000HZ=8000,
    KRISP_AUDIO_SAMPLING_RATE_16000HZ=16000,
    KRISP_AUDIO_SAMPLING_RATE_24000HZ=24000,
    KRISP_AUDIO_SAMPLING_RATE_32000HZ=32000,
    KRISP_AUDIO_SAMPLING_RATE_44100HZ=44100,
    KRISP_AUDIO_SAMPLING_RATE_48000HZ=48000,
    KRISP_AUDIO_SAMPLING_RATE_88200HZ=88200,
    KRISP_AUDIO_SAMPLING_RATE_96000HZ=96000
} KrispAudioSamplingRate;

typedef enum {
    KRISP_AUDIO_FRAME_DURATION_10MS=10
} KrispAudioFrameDuration;

typedef struct krispAudioVersionInfo_t {
    unsigned short major;
    unsigned short minor;
    unsigned short patch;
    unsigned short build;
} KrispAudioVersionInfo;


#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

/*!
 * @brief This function initializes the global data needed for the SDK
 *
 * @param[in] workingPath The path to the working directory. Can be nullptr to have the default behavior.
 *
 * @retval 0  success, negative on error
*/
KRISP_AUDIO_API int
krispAudioGlobalInit(const wchar_t* workingPath);


/*!
 * @brief This function frees all global resources allocated by SDK. The session's data will also be released and can't be used in future.
 *
 * @retval 0  success, negative on error
 */
KRISP_AUDIO_API int
krispAudioGlobalDestroy();

/*!
 * @brief This function populates the <b> versionInfo </b> structure with API version information upon successful completion.
 *
 * @param[in,out] versionInfo The structure that gets populated upon successful completion of this call.
 *              Contains <b> major </b>, <b> minor </b>, <b> patch </b> and <b> build </b> components.
 *
 * @retval 0  success, negative on error
 */
KRISP_AUDIO_API int
krispAudioGetVersion(KrispAudioVersionInfo* versionInfo);

/*!
 * @brief This function sets the Krisp model to be used. The weight file for provided model must exist. Several models can be set.
 *        The specified model is later tied to specific session during the session creation process.
 *
 * @param[in] weightFilePath The Krisp model weight file associated with the model
 * @param[in] modelName      Model name alias that allows to later distinguish between different models that have been set by this function call
 *
 * @retval 0  success, negative on error
 */
KRISP_AUDIO_API int
krispAudioSetModel(const wchar_t*   weightFilePath,
                   const char*      modelName);

/*!
 * @brief This function sets the Krisp model by giving weight-config blob data. Weight blob data must be valid.
 * The model specified model is later tied to specific session during the session creation process.
 * @param[in] weightBlob The Krisp model weight blob data
 * @param[in] blobSize Blob data size
 * @param[in] modelName Model name alias that allows to later distinguish between different models that have been set by this function call
 * @retval 0 Success
 * @retval -1 Error
 */
KRISP_AUDIO_API int krispAudioSetModelBlob(const void* weightBlob, unsigned int blobSize, const char* modelName);

/*!
 * @brief This function removes the Krisp model.
 *
 * @param[in] modelName    Model name alias that allows to remove model that has been set.
 *
 * @retval 0  success, negative on error
 */
KRISP_AUDIO_API int
krispAudioRemoveModel(const char*   modelName);

/*!
 * @brief This function returns the energy amount for the given frame. Works with floats with values normalized in range <b>[-1,1]</b>
 *        Note: It may be used without initializing global SDK context by krispAudioGlobalInit()
 * @param[in] pFrameIn  pFrameIn Pointer to input frame.
 * @param[in] frameInSize This is buffer size.
 * @return Value in range [0, 100].
 */
KRISP_AUDIO_API unsigned int
krispAudioGetFrameEnergyFloat(const float* pFrameIn,
                              unsigned int frameInSize);

/*!
 * @brief This function returns the energy amount for the given frame. Works with shorts (int16) with value in range <b>[-2^15+1, 2^15]</b>
 *        Note: It may be used without initializing global SDK context by krispAudioGlobalInit()
 * @param[in] pFrameIn  pFrameIn Pointer to input frame.
 * @param[in] frameInSize This is buffer size.
 * @return Value in range [0, 100].
 */
KRISP_AUDIO_API unsigned int
krispAudioGetFrameEnergyInt16(const short* pFrameIn,
                              unsigned int frameInSize);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif //// KRISP_AUDIO_SDK_HPP_
