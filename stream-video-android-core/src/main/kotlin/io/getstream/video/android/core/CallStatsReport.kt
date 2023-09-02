package io.getstream.video.android.core

import io.getstream.video.android.core.call.stats.model.RtcStatsReport
import io.getstream.video.android.core.internal.InternalStreamVideoApi

@InternalStreamVideoApi
data class CallStatsReport(
    val publisher: RtcStatsReport?,
    val subscriber: RtcStatsReport?,
)
