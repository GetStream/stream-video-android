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

package io.getstream.video.android.core.utils

import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// TODO - remove after testing & Coordinator integration
public fun generateSFUToken(
    userId: String,
    callId: String,
    imageUrl: String? = null
): String {
    val params = UserTokenWrapper(
        app_id = 42,
        call_id = callId,
        user = UserWrapper(
            id = userId,
            image_url = imageUrl ?: "",
        ),
        grants = mapOf(
            "can_join_call" to true,
            "can_publish_video" to true,
            "can_publish_audio" to true,
            "can_screen_share" to true,
            "can_mute_video" to true,
            "can_mute_audio" to true
        ),
        iss = "dev-only.pubkey.ecdsa256",
        aud = listOf("localhost")
    )

    val json = Json.encodeToString(params)

    val paramsString =
        Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP)

    return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.$paramsString.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        .replace("=", "")
}

@Serializable
internal data class UserTokenWrapper(
    val app_id: Int,
    val call_id: String,
    val user: UserWrapper,
    val grants: Map<String, Boolean>,
    val iss: String,
    val aud: List<String>
)

@Serializable
internal data class UserWrapper(
    val id: String,
    val image_url: String?,
)
