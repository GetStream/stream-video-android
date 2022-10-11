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

// TODO - internal sample app, clean up once full auth/login is set up
public data class UserCredentials(
    val id: String,
    val role: String,
    val token: String,
    val name: String,
    val image: String = "",
    val isSelected: Boolean = false
) {
    public fun isValid(): Boolean {
        return id.isNotEmpty() && token.isNotEmpty() && name.isNotEmpty()
    }

    public fun toUser(): User {
        return User(
            id = id,
            role = role,
            name = name,
            imageUrl = image,
            teams = emptyList(),
            extraData = emptyMap()
        )
    }
}
