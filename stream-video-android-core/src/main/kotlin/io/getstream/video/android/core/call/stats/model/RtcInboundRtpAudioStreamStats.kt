package io.getstream.video.android.core.call.stats.model

import java.math.BigInteger


data class RtcInboundRtpAudioStreamStats(
    override val id: String?,
    override val type: String?,
    override val timestampUs: Double?,
    override val ssrc: Long?,
    override val kind: String?,
    override val transportId: String?,
    override val codecId: String?,
    override val packetsReceived: Long?,
    override val packetsLost: Long?,
    override val jitter: Double?,
    override val trackIdentifier: String?,
    override val mid: String?,
    override val remoteId: String?,
    override val lastPacketReceivedTimestamp: Double?,
    override val headerBytesReceived: BigInteger?,
    override val bytesReceived: BigInteger?,
    override val packetsDiscarded: BigInteger?,
    override val fecBytesReceived: BigInteger?,
    override val fecPacketsReceived: BigInteger?,
    override val fecPacketsDiscarded: BigInteger?,
    override val jitterBufferDelay: Double?,
    override val jitterBufferTargetDelay: Double?,
    override val jitterBufferEmittedCount: BigInteger?,
    override val jitterBufferMinimumDelay: Double?,
    override val nackCount: Long?,
    override val totalProcessingDelay: Double?,
    override val estimatedPlayoutTimestamp: Double?,
    override val decoderImplementation: String?,
    override val playoutId: String?,
    override val powerEfficientDecoder: Boolean?,
    override val retransmittedPacketsReceived: BigInteger?,
    override val retransmittedBytesReceived: BigInteger?,
    override val rtxSsrc: Long?,
    override val fecSsrc: Long?,
    val audioLevel: Double?,
    val totalAudioEnergy: Double?,
    val totalSamplesReceived: BigInteger?,
    val totalSamplesDuration: Double?,
    val concealedSamples: BigInteger?,
    val silentConcealedSamples: BigInteger?,
    val concealmentEvents: BigInteger?,
    val insertedSamplesForDeceleration: BigInteger?,
    val removedSamplesForAcceleration: BigInteger?,
) : RtcInboundRtpStreamStats() {

    companion object {
        const val AUDIO_LEVEL = "audioLevel"
        const val TOTAL_AUDIO_ENERGY = "totalAudioEnergy"
        const val TOTAL_SAMPLES_RECEIVED = "totalSamplesReceived"
        const val TOTAL_SAMPLES_DURATION = "totalSamplesDuration"
        const val CONCEALED_SAMPLES = "concealedSamples"
        const val SILENT_CONCEALED_SAMPLES = "silentConcealedSamples"
        const val CONCEALMENT_EVENTS = "concealmentEvents"
        const val INSERTED_SAMPLES_FOR_DECELERATION = "insertedSamplesForDeceleration"
        const val REMOVED_SAMPLES_FOR_ACCELERATION = "removedSamplesForAcceleration"
    }

}