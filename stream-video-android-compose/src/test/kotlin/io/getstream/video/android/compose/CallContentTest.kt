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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.common.util.mockParticipant
import io.getstream.video.android.common.util.mockParticipantList
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.incomingcall.IncomingCallContent
import io.getstream.video.android.compose.ui.components.call.outgoingcall.OutgoingCallContent
import io.getstream.video.android.core.call.state.CallMediaState
import io.getstream.video.android.core.model.CallType
import io.getstream.video.android.core.model.CallUser
import org.junit.Rule
import org.junit.Test

internal class CallContentTest {
    @get:Rule
    val paparazzi = Paparazzi()

    @Test
    fun `snapshot IncomingCallContent Video type with one participant composable`() {
        paparazzi.snapshot {
            VideoTheme {
                IncomingCallContent(
                    callType = CallType.VIDEO,
                    participants = listOf(
                        mockParticipant.let {
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
                    ),
                    isVideoEnabled = false,
                    modifier = Modifier.fillMaxSize(),
                    onBackPressed = {},
                    onCallAction = {}
                )
            }
        }
    }

    @Test
    fun `snapshot IncomingCallContent Video type with multiple participants composable`() {
        paparazzi.snapshot {
            VideoTheme {
                IncomingCallContent(
                    callType = CallType.VIDEO,
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
                    },
                    isVideoEnabled = false,
                    modifier = Modifier.fillMaxSize(),
                    onBackPressed = {},
                    onCallAction = {}
                )
            }
        }
    }

    @Test
    fun `snapshot OutgoingCallContent Video type with one participant composable`() {
        paparazzi.snapshot {
            VideoTheme {
                OutgoingCallContent(
                    callType = CallType.VIDEO,
                    participants = listOf(
                        mockParticipant.let {
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
                    ),
                    callMediaState = CallMediaState(),
                    modifier = Modifier.fillMaxSize(),
                    onBackPressed = {},
                    onCallAction = {}
                )
            }
        }
    }

    @Test
    fun `snapshot OutgoingCallContent Video type with multiple participants composable`() {
        paparazzi.snapshot {
            VideoTheme {
                OutgoingCallContent(
                    callType = CallType.VIDEO,
                    participants =
                    mockParticipantList.map {
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
                    },
                    callMediaState = CallMediaState(),
                    modifier = Modifier.fillMaxSize(),
                    onBackPressed = {},
                    onCallAction = {}
                )
            }
        }
    }
}
