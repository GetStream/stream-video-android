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

package io.getstream.video.android.compose

import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.common.util.mockParticipantList
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantAvatars
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantInformation
import io.getstream.video.android.core.model.CallStatus
import io.getstream.video.android.core.model.CallUser
import org.junit.Rule
import org.junit.Test

internal class ParticipantsTest {

    @get:Rule
    val paparazzi = Paparazzi()

    @Test
    fun `snapshot ParticipantAvatars composable`() {
        paparazzi.snapshot {
            VideoTheme {
                ParticipantAvatars(participants = mockParticipantList)
            }
        }
    }

    @Test
    fun `snapshot ParticipantInformation composable`() {
        paparazzi.snapshot {
            VideoTheme {
                ParticipantInformation(
                    callStatus = CallStatus.Incoming,
                    participants = mockParticipantList.map {
                        CallUser(
                            id = it.id,
                            name = it.name,
                            role = it.role,
                            state = null,
                            imageUrl = it.profileImageURL ?: "",
                            createdAt = null,
                            updatedAt = null,
                            teams = emptyList()
                        )
                    }
                )
            }
        }
    }
}
