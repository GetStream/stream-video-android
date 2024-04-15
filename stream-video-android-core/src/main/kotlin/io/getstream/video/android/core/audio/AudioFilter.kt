package io.getstream.video.android.core.audio

import org.webrtc.AudioProcessingFactory

/**
 * Interface for audio processing.
 */
interface AudioFilter {

    /**
     * True if the audio processing is enabled, false otherwise.
     */
    var isEnabled: Boolean

    /**
     * Toggles the audio processing on or off.
     *
     * @return True if the audio processing is enabled, false otherwise.
     */
    fun toggle(): Boolean

    /**
     * The [AudioProcessingFactory] that can be used to process audio.
     */
    val delegate: AudioProcessingFactory

}