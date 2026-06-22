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

package io.getstream.video.android.compose.ui.components.livestream

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.ui.common.R

@Preview(
    name = "Portrait Preview",
    showBackground = true,
    uiMode = Configuration.UI_MODE_TYPE_NORMAL,
    device = "spec:width=411dp,height=891dp,dpi=420",
)
@Composable
private fun LivestreamBackstagePortraitPreview() {
    VideoTheme {
        Box {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(
                        id = R.string.stream_video_livestreaming_on_backstage,
                    ),
                    fontSize = 14.sp,
                    color = VideoTheme.colors.basePrimary,
                )
                Text(
                    modifier = Modifier,
                    text = "2:00",
                    fontSize = 16.sp,
                    color = VideoTheme.colors.basePrimary,
                )
                Text(
                    modifier = Modifier,
                    text = "2 participants have joined the call",
                    fontSize = 12.sp,
                    color = VideoTheme.colors.baseSecondary,
                )
            }
        }
    }
}
