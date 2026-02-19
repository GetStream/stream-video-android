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

package io.getstream.video.android.models

import io.getstream.video.android.model.User

data class GoogleAccount(
    val email: String?,
    val id: String?,
    val name: String?,
    val photoUrl: String?,
    val isFavorite: Boolean = false,
)

fun GoogleAccount.toUser(): User {
    return User(
        id = id ?: error("GoogleAccount id can not be null"),
        name = name.orEmpty(),
        image = photoUrl.orEmpty(),
    )
}

fun List<GoogleAccount>.toUsers(): List<User> {
    return map { it.toUser() }
}
