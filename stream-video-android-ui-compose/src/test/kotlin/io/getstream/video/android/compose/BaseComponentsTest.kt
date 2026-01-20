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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.compose.base.BaseComposeTest
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.GenericContainer
import io.getstream.video.android.compose.ui.components.base.StreamBadgeBox
import io.getstream.video.android.compose.ui.components.base.StreamButton
import io.getstream.video.android.compose.ui.components.base.StreamIconButton
import io.getstream.video.android.compose.ui.components.base.StreamIconToggleButton
import io.getstream.video.android.compose.ui.components.base.StreamTextField
import io.getstream.video.android.compose.ui.components.base.StreamToggleButton
import io.getstream.video.android.compose.ui.components.base.styling.ButtonStyles
import io.getstream.video.android.compose.ui.components.base.styling.StreamBadgeStyles
import io.getstream.video.android.compose.ui.components.base.styling.StreamTextFieldStyles
import io.getstream.video.android.compose.ui.components.base.styling.StyleSize
import org.junit.Rule
import org.junit.Test

internal class BaseComponentsTest : BaseComposeTest() {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_4A)

    override fun basePaparazzi(): Paparazzi = paparazzi

    @Test
    fun `Regular icon buttons`() {
        snapshot {
            Column {
                Row {
                    StreamIconButton(
                        icon = Icons.Default.GroupAdd,
                        style = ButtonStyles.primaryIconButtonStyle(),
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    StreamIconButton(
                        icon = Icons.Default.ExitToApp,
                        style = ButtonStyles.secondaryIconButtonStyle(),
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    StreamIconButton(
                        icon = Icons.Default.Settings,
                        style = ButtonStyles.tertiaryIconButtonStyle(),
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    StreamIconButton(
                        icon = Icons.Default.PhoneMissed,
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
                        icon = Icons.Default.ExitToApp,
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
                        icon = Icons.Default.PhoneMissed,
                        style = ButtonStyles.alertIconButtonStyle(),
                    )
                }

                Spacer(modifier = Modifier.size(48.dp))
                Row {
                    Spacer(modifier = Modifier.size(16.dp))
                    StreamIconButton(
                        icon = Icons.Default.PhoneMissed,
                        style = ButtonStyles.alertIconButtonStyle(size = StyleSize.L),
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    StreamIconButton(
                        icon = Icons.Default.PhoneMissed,
                        style = ButtonStyles.alertIconButtonStyle(size = StyleSize.M),
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    StreamIconButton(
                        icon = Icons.Default.PhoneMissed,
                        style = ButtonStyles.alertIconButtonStyle(size = StyleSize.S),
                    )
                }
            }
        }
    }

    @Test
    fun `Regular buttons`() {
        snapshot {
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

    @Test
    fun `Button with icons`() {
        snapshot {
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

    @Test
    fun `Different size buttons`() {
        snapshot {
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

    @Test
    fun `Toggle icon buttons`() {
        snapshot {
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

    @Test
    fun `Toggle buttons`() {
        snapshot {
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

    @Test
    fun `Show progress into icon buttons`() {
        snapshot {
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

    @Test
    fun `Input fields`() {
        snapshot {
            Column {
                // Empty
                StreamTextField(
                    value = TextFieldValue(""),
                    placeholder = "Call ID",
                    onValueChange = { },
                    style = StreamTextFieldStyles.defaultTextField(),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamTextField(
                    value = TextFieldValue(""),
                    onValueChange = { },
                    style = StreamTextFieldStyles.defaultTextField(),
                )
                Spacer(modifier = Modifier.size(16.dp))

                StreamTextField(
                    icon = Icons.Outlined.Phone,
                    value = TextFieldValue(""),
                    onValueChange = { },
                    style = StreamTextFieldStyles.defaultTextField(),
                )
                Spacer(modifier = Modifier.size(16.dp))

                // Not empty
                StreamTextField(
                    value = TextFieldValue("Some value"),
                    placeholder = "Call ID",
                    onValueChange = { },
                    style = StreamTextFieldStyles.defaultTextField(),
                )
                Spacer(modifier = Modifier.size(16.dp))

                StreamTextField(
                    icon = Icons.Outlined.Phone,
                    value = TextFieldValue("+ 123 456 789"),
                    placeholder = "Call ID",
                    onValueChange = { },
                    style = StreamTextFieldStyles.defaultTextField(),
                )
                Spacer(modifier = Modifier.size(16.dp))

                // Disabled
                StreamTextField(
                    enabled = false,
                    value = TextFieldValue(""),
                    placeholder = "Call ID",
                    onValueChange = { },
                    style = StreamTextFieldStyles.defaultTextField(),
                )
                Spacer(modifier = Modifier.size(16.dp))

                // Error
                StreamTextField(
                    error = true,
                    value = TextFieldValue("Wrong data"),
                    placeholder = "Call ID",
                    onValueChange = { },
                    style = StreamTextFieldStyles.defaultTextField(),
                )
                Spacer(modifier = Modifier.size(16.dp))

                StreamTextField(
                    value = TextFieldValue(""),
                    placeholder = "Message",
                    onValueChange = { },
                    minLines = 8,
                    style = StreamTextFieldStyles.defaultTextField(),
                )
                Spacer(modifier = Modifier.size(16.dp))
            }
        }
    }

    @Test
    fun `Generic container (for info messages)`() {
        snapshot {
            GenericContainer {
                Text(text = "Contained text!", color = Color.White)
            }
        }
    }

    @Test
    fun `Badges with buttons`() {
        snapshot {
            Column {
                StreamBadgeBox(
                    text = "!",
                    style = StreamBadgeStyles.defaultBadgeStyle(),
                ) {
                    StreamIconButton(
                        icon = Icons.Default.AddAlert,
                        style = ButtonStyles.secondaryIconButtonStyle(),
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))
                StreamBadgeBox(
                    text = "10",
                    style = StreamBadgeStyles.defaultBadgeStyle(),
                ) {
                    StreamButton(
                        icon = Icons.Default.Info,
                        text = "Secondary Button",
                        style = ButtonStyles.secondaryButtonStyle(),
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))
                StreamBadgeBox(
                    text = "10+",
                    style = StreamBadgeStyles.defaultBadgeStyle(),
                ) {
                    StreamIconButton(
                        icon = Icons.Default.QuestionAnswer,
                        style = ButtonStyles.primaryIconButtonStyle(),
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))
                StreamBadgeBox(
                    showWithoutValue = false,
                    style = StreamBadgeStyles.defaultBadgeStyle(),
                ) {
                    StreamIconButton(
                        icon = Icons.Default.QuestionAnswer,
                        style = ButtonStyles.primaryIconButtonStyle(),
                    )
                }
            }
        }
    }
}
