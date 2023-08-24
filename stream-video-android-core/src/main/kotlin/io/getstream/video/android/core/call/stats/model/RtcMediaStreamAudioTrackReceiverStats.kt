package io.getstream.video.android.core.call.stats.model

// {
//  id: RTCMediaStreamTrack_receiver_5,
//  type: track,
//  timestamp: 1679434739982604.0,
//  totalAudioEnergy: 0.008388901526724536,
//  totalInterruptionDuration: 0.0,
//  removedSamplesForAcceleration: 373,
//  kind: audio,
//  audioLevel: 0.10351878414258248,
//  jitterBufferDelay: 17644.8,
//  interruptionCount: 0,
//  relativePacketArrivalDelay: 7.19,
//  jitterBufferFlushes: 2,
//  concealedSamples: 58080,
//  jitterBufferTargetDelay: 18892.8,
//  trackIdentifier: fb83345f-0514-4793-872b-10524723b9e4,
//  totalSamplesDuration: 8.589999999999861,
//  detached: false,
//  insertedSamplesForDeceleration: 3094,
//  jitterBufferEmittedCount: 236160,
//  delayedPacketOutageSamples: 0,
//  ended: false,
//  totalSamplesReceived: 296160,
//  concealmentEvents: 1,
//  remoteSource: true,
//  silentConcealedSamples: 57560
// }

data class RtcMediaStreamAudioTrackReceiverStats(
    override val id: String?,
    override val type: String?,
    override val timestampUs: Double?,
    override val kind: String?,
    override val trackIdentifier: String?,
    override val jitterBufferDelay: Double?,
    override val jitterBufferEmittedCount: Long?,
    override val remoteSource: Boolean?,
    override val detached: Boolean?,
    override val ended: Boolean?,
    val totalAudioEnergy: Double?,
    val totalInterruptionDuration: Double?,
    val removedSamplesForAcceleration: Long?,
    val audioLevel: Double?,
    val interruptionCount: Long?,
    val relativePacketArrivalDelay: Double?,
    val jitterBufferFlushes: Long?,
    val concealedSamples: Long?,
    val jitterBufferTargetDelay: Double?,
    val totalSamplesDuration: Double?,
    val insertedSamplesForDeceleration: Long?,
    val delayedPacketOutageSamples: Long?,
    val totalSamplesReceived: Long?,
    val concealmentEvents: Long?,
    val silentConcealedSamples: Long?,
) : RtcMediaStreamTrackReceiverStats() {

    companion object {
        const val KIND = "kind"
        const val TRACK_IDENTIFIER = "trackIdentifier"
        const val JITTER_BUFFER_DELAY = "jitterBufferDelay"
        const val JITTER_BUFFER_EMITTED_COUNT = "jitterBufferEmittedCount"
        const val REMOTE_SOURCE = "remoteSource"
        const val DETACHED = "detached"
        const val ENDED = "ended"
        const val TOTAL_AUDIO_ENERGY = "totalAudioEnergy"
        const val TOTAL_INTERRUPTION_DURATION = "totalInterruptionDuration"
        const val REMOVED_SAMPLES_FOR_ACCELERATION = "removedSamplesForAcceleration"
        const val AUDIO_LEVEL = "audioLevel"
        const val INTERRUPTION_COUNT = "interruptionCount"
        const val RELATIVE_PACKET_ARRIVAL_DELAY = "relativePacketArrivalDelay"
        const val JITTER_BUFFER_FLUSHES = "jitterBufferFlushes"
        const val CONCEALED_SAMPLES = "concealedSamples"
        const val JITTER_BUFFER_TARGET_DELAY = "jitterBufferTargetDelay"
        const val TOTAL_SAMPLES_DURATION = "totalSamplesDuration"
        const val INSERTED_SAMPLES_FOR_DECELERATION = "insertedSamplesForDeceleration"
        const val DELAYED_PACKET_OUTAGE_SAMPLES = "delayedPacketOutageSamples"
        const val TOTAL_SAMPLES_RECEIVED = "totalSamplesReceived"
        const val CONCEALMENT_EVENTS = "concealmentEvents"
        const val SILENT_CONCEALED_SAMPLES = "silentConcealedSamples"
    }

}