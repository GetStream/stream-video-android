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

package io.getstream.video.android.tutorial.ringing

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.actions.GenericAction
import io.getstream.video.android.core.call.state.CustomAction

@Composable
public fun BusyCallAction(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCallAction: (BusyCall) -> Unit,
    icon: ImageVector? = null,
    bgColor: Color? = null,
    iconTint: Color? = null,
): Unit = GenericAction(
    modifier = modifier,
    enabled = enabled,
    onAction = { onCallAction(BusyCall) },
    icon = icon ?: Icons.Default.Close,
    color = bgColor ?: VideoTheme.colors.alertWarning,
    iconTint = iconTint ?: VideoTheme.colors.basePrimary,
)

public data object BusyCall : CustomAction(tag = "busy")
