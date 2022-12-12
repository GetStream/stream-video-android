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

package io.getstream.video.android.ui.xml.utils

import io.getstream.video.android.model.CallParticipantState
import stream.video.coordinator.user_v1.User

internal val mockParticipant: CallParticipantState
    inline get() = mockUsers[0].toCallParticipant()

internal val mockParticipantList: List<CallParticipantState>
    inline get() = mockUsers.map { it.toCallParticipant() }

private fun User.toCallParticipant() =
    CallParticipantState(
        id = this.id,
        name = this.name,
        profileImageURL = this.image_url,
        role = this.role,
        isOnline = false,
        track = null,
        videoTrackSize = 0 to 0,
        isLocal = false,
        audioLevel = 0f,
        idPrefix = "",
        sessionId = ""
    )

@PublishedApi
internal val mockUsers: List<User>
    inline get() = listOf(
        User(
            id = "filip_babic",
            name = "Filip",
            image_url = "https://avatars.githubusercontent.com/u/17215808?v=4",
        ),
        User(
            id = "jaewoong",
            name = "Jaewoong Eum",
            image_url = "https://ca.slack-edge.com/T02RM6X6B-U02HU1XR9LM-626fb91c334e-128"
        ),
        User(
            id = "toma_zdravkovic",
            name = "Toma Zdravkovic",
            image_url = "https://upload.wikimedia.org/wikipedia/commons/d/da/Toma_Zdravkovi%C4%87.jpg"
        )
    )
