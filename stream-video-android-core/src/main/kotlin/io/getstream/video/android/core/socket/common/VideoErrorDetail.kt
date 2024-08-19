package io.getstream.video.android.core.socket.common

/**
 * The error detail.
 *
 * @property code The error code.
 * @property messages The error messages.
 */
public data class VideoErrorDetail(
    public val code: Int,
    public val messages: List<String>,
)