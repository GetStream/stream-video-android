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

package io.getstream.video.android.core.model

import androidx.compose.runtime.Stable
import org.openapitools.client.models.MuteUsersRequest

/**
 * Represents the data used to request user's tracks to be muted.
 *
 * @param muteAllUsers If we should mute all the users in the call.
 * @param audio If the audio should be muted.
 * @param screenShare If the screen sharing should be muted.
 * @param video If the video should be muted.
 * @param users List of users to apply the mute change to.
 */
@Stable
public data class MuteUsersData(
    public val users: List<String>? = null,
    public val muteAllUsers: Boolean? = null,
    public val audio: Boolean? = null,
    public val screenShare: Boolean? = null,
    public val video: Boolean? = null,
)

/**
 * Maps the data to the request for the BE.
 */
public fun MuteUsersData.toRequest(): MuteUsersRequest {
    return MuteUsersRequest(
        audio = audio,
        video = video,
        screenshare = screenShare,
        muteAllUsers = muteAllUsers,
        userIds = users,
    )
}
