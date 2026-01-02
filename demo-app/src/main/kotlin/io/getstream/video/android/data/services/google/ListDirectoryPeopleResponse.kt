/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.data.services.google

import io.getstream.video.android.models.GoogleAccount
import io.getstream.video.android.util.UserHelper

data class ListDirectoryPeopleResponse(
    val people: List<GoogleAccountDto>,
)

data class GoogleAccountDto(
    val photos: List<PhotoDto>?,
    val emailAddresses: List<EmailAddressDto>,
)

data class PhotoDto(
    val url: String,
)

data class EmailAddressDto(
    val value: String,
)

fun GoogleAccountDto.asDomainModel(): GoogleAccount {
    val email = emailAddresses.firstOrNull()?.value

    return GoogleAccount(
        email = email,
        id = email?.let { UserHelper.getUserIdFromEmail(it) },
        name = email?.let { UserHelper.getFullNameFromEmail(it) },
        photoUrl = photos?.firstOrNull()?.url,
    )
}
