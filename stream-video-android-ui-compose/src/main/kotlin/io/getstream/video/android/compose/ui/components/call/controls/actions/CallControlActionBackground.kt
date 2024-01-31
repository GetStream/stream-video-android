/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.compose.ui.components.call.controls.actions

import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import io.getstream.video.android.compose.theme.VideoTheme

@Composable
public fun CallControlActionBackground(
    modifier: Modifier = Modifier,
    isEnabled: Boolean,
    enabledColor: Color = VideoTheme.colors.callActionIconEnabledBackground,
    disabledColor: Color = VideoTheme.colors.callActionIconDisabledBackground,
    shape: Shape = VideoTheme.shapes.callControlsButton,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = shape,
        backgroundColor = if (isEnabled) {
            enabledColor
        } else {
            disabledColor
        },
        content = content,
    )
}
