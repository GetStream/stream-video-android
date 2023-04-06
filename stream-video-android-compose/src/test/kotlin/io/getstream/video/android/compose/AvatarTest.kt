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

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.common.util.mockParticipant
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.Avatar
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.core.model.toUser
import org.junit.Rule
import org.junit.Test

internal class AvatarTest {

    @get:Rule
    val paparazzi = Paparazzi()

    @Test
    fun `snapshot AvatarInitial composable`() {
        paparazzi.snapshot {
            VideoTheme {
                Avatar(
                    modifier = Modifier.size(72.dp),
                    initials = "Thierry"
                )
            }
        }
    }

    @Test
    fun `snapshot UserAvatar composable`() {
        paparazzi.snapshot {
            VideoTheme {
                UserAvatar(
                    user = mockParticipant.toUser(),
                    modifier = Modifier.size(82.dp)
                )
            }
        }
    }
}
