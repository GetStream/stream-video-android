package io.getstream.video.android.models

data class GoogleAccount(
    val email: String,
    val id: String,
    val name: String,
    val photoUrl: String?,
    val isFavorite: Boolean,
)