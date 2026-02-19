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

package io.getstream.video.android.compose.ui.components.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.theme.VideoTheme

/**
 * Represents the default AppBar that's shown in the audio room.
 *
 * @param title A title that will be shown.
 * @param modifier Modifier for styling.
 */
@Composable
public fun AudioAppBar(
    modifier: Modifier = Modifier,
    title: String,
) {
    Column(modifier.background(VideoTheme.colors.baseSheetSecondary)) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = VideoTheme.colors.basePrimary,
        )

        Spacer(modifier = Modifier.height(18.dp))

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(VideoTheme.colors.basePrimary),
        )
    }
}

@Preview
@Composable
private fun AudioAppBarPreview() {
    VideoTheme {
        AudioAppBar(
            modifier = Modifier.fillMaxWidth(),
            title = "Audio Room Number 01",
        )
    }
}
