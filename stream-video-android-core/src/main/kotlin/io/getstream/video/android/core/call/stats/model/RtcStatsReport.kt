package io.getstream.video.android.core.call.stats.model

import io.getstream.video.android.core.call.stats.model.discriminator.RtcReportType
import org.webrtc.RTCStatsReport

data class RtcStatsReport(
    val origin: RTCStatsReport,
    val parsed: Map<RtcReportType, Set<RtcStats>>
)