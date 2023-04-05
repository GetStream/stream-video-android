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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.common.util.mockUsers
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.Avatar
import io.getstream.video.android.compose.ui.components.background.CallBackground
import io.getstream.video.android.core.model.CallType
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.model.CallUserState
import org.junit.Rule
import org.junit.Test

internal class CallBackgroundTest {
    @get:Rule
    val paparazzi = Paparazzi()

    @Test
    fun `snapshot CallBackground composable with an Avatar`() {
        paparazzi.snapshot {
            VideoTheme(isInDarkMode = false) {
                CallBackground(
                    participants = listOf(
                        mockUsers.first().let {
                            CallUser(
                                id = it.id,
                                name = it.name,
                                imageUrl = it.profileImageURL ?: "",
                                role = it.role,
                                teams = emptyList(),
                                updatedAt = null,
                                createdAt = null,
                                state = CallUserState("", false, false, false)
                            )
                        }
                    ),
                    callType = CallType.VIDEO, isIncoming = true
                ) {
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        Avatar(
                            modifier = Modifier.size(56.dp), imageUrl = "", initials = "CC"
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `snapshot CallBackground composable with an Avatar dark theme`() {
        paparazzi.snapshot {
            VideoTheme(isInDarkMode = true) {
                CallBackground(
                    participants = listOf(
                        mockUsers.first().let {
                            CallUser(
                                id = it.id,
                                name = it.name,
                                imageUrl = it.profileImageURL ?: "",
                                role = it.role,
                                teams = emptyList(),
                                updatedAt = null,
                                createdAt = null,
                                state = CallUserState("", false, false, false)
                            )
                        }
                    ),
                    callType = CallType.VIDEO, isIncoming = true
                ) {
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        Avatar(
                            modifier = Modifier.size(56.dp), imageUrl = "", initials = "CC"
                        )
                    }
                }
            }
        }
    }
}
