/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.model

public data class User(
    val id: String,
    val role: String,
    val name: String,
    val token: String,
    val imageUrl: String?,
    val teams: List<String>,
    val extraData: Map<String, String>
)

public fun User.toCredentials(): UserCredentials {
    return UserCredentials(
        id = id,
        role = role,
        name = name,
        image = imageUrl ?: "",
        token = token
    )
}
