package io.getstream.video.android.core.model

import org.threeten.bp.OffsetDateTime

public data class CallTranscription(
    val endTime: OffsetDateTime,
    val filename: String,
    val startTime: OffsetDateTime,
    val url: String,
)
