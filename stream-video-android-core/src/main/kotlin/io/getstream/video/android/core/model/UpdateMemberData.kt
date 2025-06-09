package io.getstream.video.android.core.model


public data class UpdateMemberData(
    val userId: String,
    val custom: Map<String, Any>? = null,
)
