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

import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.state.ToggleableState
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamIconButton
import io.getstream.video.android.compose.ui.components.base.StreamIconToggleButton
import io.getstream.video.android.compose.ui.components.base.styling.StreamFixedSizeButtonStyle
import io.getstream.video.android.core.call.state.CallAction

@Composable
public fun GenericAction(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    enabled: Boolean = true,
    shape: Shape? = null,
    color: Color? = null,
    iconTint: Color? = null,
    style: StreamFixedSizeButtonStyle? = null,
    onAction: () -> Unit,
): Unit = StreamIconButton(
    modifier = modifier,
    enabled = enabled,
    icon = icon,
    style = style?.let {
        it.copyFixed(
            shape = shape ?: it.shape,
            colors = color?.let {
                ButtonDefaults.buttonColors(
                    backgroundColor = color,
                    disabledBackgroundColor = color.copy(alpha = 0.16f),
                )
            }
                ?: it.colors,
            iconStyle = it.iconStyle.copy(
                default = it.iconStyle.default.copy(
                    color = iconTint ?: it.iconStyle.default.color,
                ),
            ),
        )
    } ?: VideoTheme.styles.buttonStyles.primaryIconButtonStyle()
        .let {
            it.copyFixed(
                shape = shape ?: it.shape,
                colors = color?.let {
                    ButtonDefaults.buttonColors(
                        backgroundColor = color,
                        disabledBackgroundColor = color.copy(alpha = 0.16f),
                    )
                }
                    ?: it.colors,
                iconStyle = it.iconStyle.copy(
                    default = it.iconStyle.default.copy(
                        color = iconTint ?: it.iconStyle.default.color,
                    ),
                ),
            )
        },
    onClick = {
        onAction()
    },
)

/**
 * A call action button represents toggling a microphone.
 *
 * @param modifier Optional Modifier for this action button.
 * @param isMicrophoneEnabled Represent is camera enabled.
 * @param enabled Whether or not this action button will handle input events.
 * @param onCallAction A [CallAction] event that will be fired.
 */
@Composable
public fun ToggleAction(
    modifier: Modifier = Modifier,
    isActionActive: Boolean,
    iconOnOff: Pair<ImageVector, ImageVector>,
    enabled: Boolean = true,
    shape: Shape? = null,
    enabledColor: Color? = null,
    disabledColor: Color? = null,
    enabledIconTint: Color? = null,
    disabledIconTint: Color? = null,
    progress: Boolean = false,
    onStyle: StreamFixedSizeButtonStyle? = null,
    offStyle: StreamFixedSizeButtonStyle? = null,
    onAction: () -> Unit,
): Unit = StreamIconToggleButton(
    modifier = modifier,
    enabled = enabled,
    showProgress = progress,
    toggleState = rememberUpdatedState(newValue = ToggleableState(isActionActive)),
    onIcon = iconOnOff.first,
    offIcon = iconOnOff.second,
    onStyle = onStyle ?: VideoTheme.styles.buttonStyles.primaryIconButtonStyle()
        .let {
            it.copyFixed(
                shape = shape ?: it.shape,
                colors = enabledColor?.let { ButtonDefaults.buttonColors(backgroundColor = enabledColor) }
                    ?: it.colors,
                iconStyle = it.iconStyle.copy(
                    default = it.iconStyle.default.copy(
                        color = enabledIconTint ?: it.iconStyle.default.color,
                    ),
                ),
            )
        },
    offStyle = offStyle ?: VideoTheme.styles.buttonStyles.alertIconButtonStyle().let {
        it.copyFixed(
            shape = shape ?: it.shape,
            colors = disabledColor?.let {
                ButtonDefaults.buttonColors(
                    backgroundColor = disabledColor,
                    disabledBackgroundColor = disabledColor,
                )
            }
                ?: it.colors,
            iconStyle = it.iconStyle.copy(
                default = it.iconStyle.default.copy(
                    color = disabledIconTint ?: it.iconStyle.default.color,
                ),
            ),
        )
    },
    onClick = {
        onAction()
    },
)
