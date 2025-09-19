/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core

import io.getstream.android.video.generated.models.MuteUsersResponse
import io.getstream.result.Result
import io.getstream.video.android.core.internal.InternalStreamVideoApi

/**
 * Interface for call actions that can be performed on a participant.
 */
@InternalStreamVideoApi
interface CallActions {
    /**
     * Mute audio for a specific user
     */
    suspend fun muteUserAudio(userId: String): Result<MuteUsersResponse>

    /**
     * Mute video for a specific user
     */
    suspend fun muteUserVideo(userId: String): Result<MuteUsersResponse>

    /**
     * Mute screen sharing for a specific user
     */
    suspend fun muteUserScreenShare(userId: String): Result<MuteUsersResponse>

    /**
     * Pin a participant
     */
    suspend fun pinParticipant(userId: String, sessionId: String)

    /**
     * Unpin a participant
     */
    suspend fun unpinParticipant(sessionId: String)

    /**
     * Check if a participant is local based on their session ID
     */
    fun isLocalParticipant(sessionId: String): Boolean
}
