package io.getstream.video.android.core.errors


/**
 * Triggered if for whatever reason we fail to access the camera
 * There are dozens of underlying reasons for why the camera can fail
 * We catch them and wrap them in this exception
 */
class CameraException(
    message: String? = null, cause: Throwable? = null
) : Exception(message, cause)


/**
 * Triggered if for whatever reason we fail to access the microphone
 */
class MicrophoneException(
    message: String? = null, cause: Throwable? = null
) : Exception(message, cause)


/**
 * Wraps any exception that occurs in the RTC layer
 * With a recommendation if we should retry the current SFU, stop
 * or switch to a different one
 */
class RtcException(
    message: String? = null, cause: Throwable? = null,
    retryCurrentSfu: Boolean = false,
    switchSfu: Boolean = false,
) : Exception(message, cause)