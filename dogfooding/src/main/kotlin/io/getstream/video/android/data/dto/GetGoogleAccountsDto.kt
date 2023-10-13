package io.getstream.video.android.data.dto

import io.getstream.video.android.models.StreamUser
import java.util.Locale

data class GetGoogleAccountsResponseDto(
    val people: List<GoogleAccountDto>
)

data class GoogleAccountDto (
    val photos: List<PhotoDto>?,
    val emailAddresses: List<EmailAddressDto>
)

data class PhotoDto(
    val url: String
)

data class EmailAddressDto(
    val value: String
)

fun GoogleAccountDto.asDomainModel(): StreamUser {
    val email = emailAddresses.firstOrNull()?.value ?: ""

    return StreamUser(
        email = email,
        id = email.replace(".", "_"),
        name = email
            .split("@")
            .firstOrNull()
            ?.split(".")
            ?.firstOrNull()
            ?.capitalize(Locale.ROOT) ?: email,
        avatarUrl = photos?.firstOrNull()?.url,
        isFavorite = false
    )
}
