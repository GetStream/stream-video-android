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

package io.getstream.video.android.compose.ui.components.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import io.getstream.video.android.compose.theme.VideoTheme

@Composable
public fun GenericContainer(
    modifier: Modifier = Modifier,
    background: Color = VideoTheme.colors.buttonPrimaryDefault,
    roundness: Dp = VideoTheme.dimens.roundnessL,
    content: @Composable BoxScope.() -> Unit,
): Unit = Box(
    modifier = modifier
        .background(
            color = background,
            shape = RoundedCornerShape(roundness),
        )
        .padding(VideoTheme.dimens.spacingXs),
    content = content,
)

@Preview
@Composable
private fun GenericContainerPreview() {
    VideoTheme {
        GenericContainer {
            Text(text = "Contained text!", color = Color.White)
        }
    }
}
