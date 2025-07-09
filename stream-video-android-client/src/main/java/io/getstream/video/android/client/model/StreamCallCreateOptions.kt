package io.getstream.video.android.client.model

import io.getstream.android.video.generated.models.CallSettingsRequest
import java.time.OffsetDateTime

public data class StreamCallCreateOptions(
    val custom: Map<String, Any>? = null,
    val settings: CallSettingsRequest? = null,
    val startsAt: OffsetDateTime? = null,
    val team: String? = null,
)