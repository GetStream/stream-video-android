package io.getstream.video.android.models

data class StreamUser(
    val email: String,
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val isFavorite: Boolean,
)