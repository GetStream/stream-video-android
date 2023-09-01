package io.getstream.video.android.core.call.stats.model

data class RtcAudioSourceStats(
    override val id: String?,
    override val type: String?,
    override val timestampUs: Double?,
    override val kind: String?,
    override val trackIdentifier: String?,
    val audioLevel: Double?,
    val totalAudioEnergy: Double?,
    val totalSamplesDuration: Double?,
    val echoReturnLoss: Double?,
    val echoReturnLossEnhancement: Double?,
    val droppedSamplesDuration: Double?,
    val droppedSamplesEvents: Long?,
    val totalCaptureDelay: Double?,
    val totalSamplesCaptured: Long?,
) : RtcMediaSourceStats {

    companion object {
        const val KIND = RtcMediaSourceStats.KIND
        const val TRACK_IDENTIFIER = RtcMediaSourceStats.TRACK_IDENTIFIER
        const val AUDIO_LEVEL = "audioLevel"
        const val TOTAL_AUDIO_ENERGY = "totalAudioEnergy"
        const val TOTAL_SAMPLES_DURATION = "totalSamplesDuration"
        const val ECHO_RETURN_LOSS = "echoReturnLoss"
        const val ECHO_RETURN_LOSS_ENHANCEMENT = "echoReturnLossEnhancement"
        const val DROPPED_SAMPLES_DURATION = "droppedSamplesDuration"
        const val DROPPED_SAMPLES_EVENTS = "droppedSamplesEvents"
        const val TOTAL_CAPTURE_DELAY = "totalCaptureDelay"
        const val TOTAL_SAMPLES_CAPTURED = "totalSamplesCaptured"
    }
}