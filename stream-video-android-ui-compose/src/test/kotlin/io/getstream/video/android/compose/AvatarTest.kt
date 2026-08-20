/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.compose.ui.PIXEL_4A_HDPI
import io.getstream.video.android.compose.ui.PaparazziComposeTest
import io.getstream.video.android.compose.ui.components.avatar.Avatar
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.mock.previewParticipant
import org.junit.Rule
import org.junit.Test

internal class AvatarTest : PaparazziComposeTest {

    @get:Rule
    override val paparazzi = Paparazzi(deviceConfig = PIXEL_4A_HDPI)

    @Test
    fun `snapshot AvatarInitial composable`() {
        snapshotWithDarkModeRow {
            Avatar(
                modifier = Modifier.size(72.dp),
                fallbackText = "Thierry",
            )
        }
    }

    @Test
    fun `snapshot UserAvatar composable`() {
        snapshotWithDarkModeRow {
            val name by previewParticipant.name.collectAsStateWithLifecycle()
            val image by previewParticipant.image.collectAsStateWithLifecycle()
            UserAvatar(
                modifier = Modifier.size(82.dp),
                userImage = image,
                userName = name,
                isShowingOnlineIndicator = true,
            )
        }
    }
}
