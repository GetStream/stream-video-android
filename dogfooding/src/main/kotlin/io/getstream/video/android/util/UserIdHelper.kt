/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.util

object UserIdHelper {
    fun generateRandomString(length: Int = 8, upperCaseOnly: Boolean = false): String {
        val allowedChars: List<Char> = ('A'..'Z') + ('0'..'9') + if (!upperCaseOnly) {
            ('a'..'z')
        } else {
            emptyList()
        }

        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    fun getUserIdFromEmail(email: String) = email
        .replace(" ", "")
        .replace(".", "")
        .replace("@", "")
}
