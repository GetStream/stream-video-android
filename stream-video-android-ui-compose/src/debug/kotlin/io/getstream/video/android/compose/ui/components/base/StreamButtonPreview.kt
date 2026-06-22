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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.PhoneMissed
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.styling.ButtonStyles
import io.getstream.video.android.compose.ui.components.base.styling.StreamBadgeStyles
import io.getstream.video.android.compose.ui.components.base.styling.StyleSize

// Start of previews
@Preview
@Composable
private fun StreamIconButtonPreview() {
    VideoTheme {
        Column {
            Row {
                StreamIconButton(
                    icon = Icons.Default.GroupAdd,
                    style = ButtonStyles.primaryIconButtonStyle(),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamIconButton(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    style = ButtonStyles.secondaryIconButtonStyle(),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamIconButton(
                    icon = Icons.Default.Settings,
                    style = ButtonStyles.tertiaryIconButtonStyle(),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamIconButton(
                    icon = Icons.AutoMirrored.Filled.PhoneMissed,
                    style = ButtonStyles.alertIconButtonStyle(),
                )
            }

            Spacer(modifier = Modifier.size(48.dp))
            Row {
                StreamIconButton(
                    enabled = false,
                    icon = Icons.Default.GroupAdd,
                    style = ButtonStyles.primaryIconButtonStyle(),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamIconButton(
                    enabled = false,
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    style = ButtonStyles.secondaryIconButtonStyle(),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamIconButton(
                    enabled = false,
                    icon = Icons.Default.Settings,
                    style = ButtonStyles.tertiaryIconButtonStyle(),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamIconButton(
                    enabled = false,
                    icon = Icons.AutoMirrored.Filled.PhoneMissed,
                    style = ButtonStyles.alertIconButtonStyle(),
                )
            }

            Spacer(modifier = Modifier.size(48.dp))
            Row {
                Spacer(modifier = Modifier.size(16.dp))
                StreamIconButton(
                    icon = Icons.AutoMirrored.Filled.PhoneMissed,
                    style = ButtonStyles.alertIconButtonStyle(size = StyleSize.L),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamIconButton(
                    icon = Icons.AutoMirrored.Filled.PhoneMissed,
                    style = ButtonStyles.alertIconButtonStyle(size = StyleSize.M),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamIconButton(
                    icon = Icons.AutoMirrored.Filled.PhoneMissed,
                    style = ButtonStyles.alertIconButtonStyle(size = StyleSize.S),
                )
            }
        }
    }
}

@Preview
@Composable
private fun StreamDrawableButtonPreview() {
    VideoTheme {
        Column {
            Row {
                StreamDrawableButton(
                    drawable = R.drawable.example,
                    style = ButtonStyles.primaryDrawableButtonStyle(),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamDrawableButton(
                    drawable = R.drawable.example,
                    style = ButtonStyles.primaryDrawableButtonStyle(),
                    enabled = false,
                )
            }
        }
    }
}

@Preview
@Composable
private fun StreamButtonPreview() {
    VideoTheme {
        Column {
            // Default
            StreamButton(
                text = "Primary Button",
                style = ButtonStyles.primaryButtonStyle(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                text = "Secondary Button",
                style = ButtonStyles.secondaryButtonStyle(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                text = "Tertiary Button",
                style = ButtonStyles.tertiaryButtonStyle(),
            )
            Spacer(modifier = Modifier.height(48.dp))
            StreamButton(
                text = "Alert Button",
                style = ButtonStyles.alertButtonStyle(),
            )
            Spacer(modifier = Modifier.height(48.dp))

            // Disabled
            StreamButton(
                enabled = false,
                text = "Primary Button",
                style = ButtonStyles.primaryButtonStyle(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                enabled = false,
                text = "Secondary Button",
                style = ButtonStyles.secondaryButtonStyle(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                enabled = false,
                text = "Tertiary Button",
                style = ButtonStyles.tertiaryButtonStyle(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                enabled = false,
                text = "Alert Button",
                style = ButtonStyles.alertButtonStyle(),
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Preview
@Composable
private fun StreamButtonWithIconPreview() {
    VideoTheme {
        Column {
            // With icon
            StreamButton(
                icon = Icons.Filled.AccessAlarm,
                text = "Primary Button",
                style = ButtonStyles.primaryButtonStyle(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                icon = Icons.Filled.AccessAlarm,
                text = "Secondary Button",
                style = ButtonStyles.secondaryButtonStyle(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                icon = Icons.Filled.AccessAlarm,
                text = "Tertiary Button",
                style = ButtonStyles.tertiaryButtonStyle(),
            )
        }
    }
}

@Preview
@Composable
private fun StreamButtonSizePreview() {
    VideoTheme {
        Column {
            // Size
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                text = "Small Button",
                style = ButtonStyles.secondaryButtonStyle(size = StyleSize.S),
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                text = "Medium Button",
                style = ButtonStyles.secondaryButtonStyle(size = StyleSize.M),
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                text = "Large Button",
                style = ButtonStyles.secondaryButtonStyle(size = StyleSize.L),
            )
        }
    }
}

@Preview
@Composable
private fun StreamToggleIconButtonPreview() {
    VideoTheme {
        Row {
            // Size
            StreamIconToggleButton(
                onStyle = ButtonStyles.primaryIconButtonStyle(),
                offStyle = ButtonStyles.alertIconButtonStyle(),
                onIcon = Icons.Default.Videocam,
                offIcon = Icons.Default.VideocamOff,
            )
            Spacer(modifier = Modifier.width(24.dp))
            StreamIconToggleButton(
                toggleState = rememberUpdatedState(newValue = ToggleableState.On),
                onStyle = ButtonStyles.primaryIconButtonStyle(),
                offStyle = ButtonStyles.alertIconButtonStyle(),
                onIcon = Icons.Default.Videocam,
                offIcon = Icons.Default.VideocamOff,
            )

            Spacer(modifier = Modifier.width(24.dp))
            StreamBadgeBox(
                style = StreamBadgeStyles.defaultBadgeStyle().copy(
                    color = VideoTheme.colors.alertCaution,
                    textStyle = VideoTheme.typography.labelXS.copy(color = Color.Black),
                ),
                text = "!",
            ) {
                StreamIconToggleButton(
                    enabled = false,
                    toggleState = rememberUpdatedState(newValue = ToggleableState.Off),
                    onStyle = ButtonStyles.secondaryIconButtonStyle(),
                    offStyle = ButtonStyles.alertIconButtonStyle(),
                    onIcon = Icons.Default.Videocam,
                    offIcon = Icons.Default.VideocamOff,
                )
            }
        }
    }
}

@Preview
@Composable
private fun StreamDrawableToggleButtonPreview() {
    VideoTheme {
        Row {
            StreamDrawableToggleButton(
                toggleState = rememberUpdatedState(newValue = ToggleableState.On), // On
                onDrawable = R.drawable.example,
                onStyle = ButtonStyles.drawableToggleButtonStyleOn(), // On
            )
            Spacer(modifier = Modifier.width(8.dp))
            StreamDrawableToggleButton(
                toggleState = rememberUpdatedState(newValue = ToggleableState.Off), // Off
                onDrawable = R.drawable.example,
                onStyle = ButtonStyles.drawableToggleButtonStyleOn(),
                offStyle = ButtonStyles.drawableToggleButtonStyleOff(),
            )
            Spacer(modifier = Modifier.width(8.dp))
            StreamDrawableToggleButton(
                toggleState = rememberUpdatedState(newValue = ToggleableState.On),
                onDrawable = R.drawable.example,
                onStyle = ButtonStyles.drawableToggleButtonStyleOn(),
                offStyle = ButtonStyles.drawableToggleButtonStyleOff(),
                enabled = false, // Disabled
            )
        }
    }
}

@Preview
@Composable
private fun StreamToggleButtonPreview() {
    VideoTheme {
        Column {
            // Size
            StreamToggleButton(
                modifier = Modifier.fillMaxWidth(),
                toggleState = rememberUpdatedState(newValue = ToggleableState.On),
                onText = "Grid",
                offText = "Grid",
                onStyle = ButtonStyles.toggleButtonStyleOn(),
                offStyle = ButtonStyles.toggleButtonStyleOff(),
                onIcon = Icons.Filled.GridView,
                offIcon = Icons.Filled.GridView,
            )
            Spacer(modifier = Modifier.width(24.dp))
            StreamToggleButton(
                modifier = Modifier.fillMaxWidth(),
                toggleState = rememberUpdatedState(newValue = ToggleableState.Off),
                onText = "Grid (On)",
                offText = "Grid (Off)",
                onStyle = ButtonStyles.toggleButtonStyleOn(),
                offStyle = ButtonStyles.toggleButtonStyleOff(),
                onIcon = Icons.Filled.GridView,
                offIcon = Icons.Filled.GridView,
            )
        }
    }
}

@Preview
@Composable
private fun StreamProgressButtonsPreview() {
    VideoTheme {
        Column {
            StreamButton(text = "Progress", showProgress = true)
            StreamIconButton(
                icon = Icons.Default.Camera,
                style = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle(),
                showProgress = true,
            )
        }
    }
}
