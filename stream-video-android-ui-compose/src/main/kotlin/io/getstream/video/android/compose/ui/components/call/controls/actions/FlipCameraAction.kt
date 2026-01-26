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

package io.getstream.video.android.compose.ui.components.call.controls.actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraIos
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.FlipCamera

/**
 * A call action button represents flipping a camera.
 *
 * @param modifier Optional Modifier for this action button.
 * @param enabled Whether or not this action button will handle input events.
 * @param onCallAction A [CallAction] event that will be fired.
 */
@Composable
public fun FlipCameraAction(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color? = null,
    iconTint: Color? = null,
    onCallAction: (FlipCamera) -> Unit,
): Unit = GenericAction(
    modifier = modifier,
    enabled = enabled,
    color = color,
    iconTint = iconTint,
    icon = Icons.Default.FlipCameraIos,
) {
    onCallAction(FlipCamera)
}

@Preview
@Composable
private fun FlipActionPreview() {
    VideoTheme {
        FlipCameraAction {
        }
    }
}
