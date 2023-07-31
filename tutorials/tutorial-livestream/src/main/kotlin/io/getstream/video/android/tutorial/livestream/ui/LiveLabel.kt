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

package io.getstream.video.android.tutorial.livestream.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme

@Composable
fun LiveLabel(
    modifier: Modifier,
    liveCount: Int
) {
    Row(modifier = modifier.clip(RoundedCornerShape(6.dp))) {
        Text(
            modifier = Modifier
                .background(VideoTheme.colors.primaryAccent)
                .padding(vertical = 3.dp, horizontal = 12.dp),
            text = "Live",
            color = Color.White
        )

        Text(
            modifier = Modifier
                .background(Color(0xFF1C1E22))
                .padding(vertical = 3.dp, horizontal = 12.dp),
            text = liveCount.toString(),
            color = Color.White
        )
    }
}

@Preview
@Composable
private fun LiveLabelPreview() {
    VideoTheme {
        LiveLabel(
            modifier = Modifier,
            liveCount = 4321
        )
    }
}
