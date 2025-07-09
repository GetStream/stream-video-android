package io.getstream.video.android.client.model

/**
 * Represents the data used to connect a user to the client.
 */
public data class ConnectUserData(
    val token: String,
    val id: String,
    val name: String? = null,
    val image: String? = null,
    val invisible: Boolean = false,
    val language: String? = null,
    val custom: Map<String, Any?>? = null,
)