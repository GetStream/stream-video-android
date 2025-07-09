package io.getstream.video.android.client.model

import kotlin.collections.Map

/**
 * Represents the user that is connected to the client.
 */
public data class StreamConnectedUser(
    val createdAt: org.threeten.bp.OffsetDateTime,
    val id: String,
    val language: String,
    val role: String,
    val updatedAt: org.threeten.bp.OffsetDateTime,
    val blockedUserIds: List<String>,
    val teams: List<String>,
    val custom: Map<String, Any?> = emptyMap(),
    val deactivatedAt: org.threeten.bp.OffsetDateTime? = null,
    val deletedAt: org.threeten.bp.OffsetDateTime? = null,
    val image: String? = null,
    val lastActive: org.threeten.bp.OffsetDateTime? = null,
    val name: String? = null,
)