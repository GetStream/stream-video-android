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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.styling.ButtonStyles
import io.getstream.video.android.compose.ui.components.base.styling.DialogStyle
import io.getstream.video.android.compose.ui.components.base.styling.StreamButtonStyle
import io.getstream.video.android.compose.ui.components.base.styling.StreamDialogStyles
import io.getstream.video.android.compose.ui.components.base.styling.StreamTextFieldStyles
import io.getstream.video.android.compose.ui.components.base.styling.StyleSize

@Composable
public fun StreamDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    style: DialogStyle,
    dialogProperties: DialogProperties = DialogProperties(),
    content: @Composable BoxScope.() -> Unit,
): Unit = Dialog(onDismissRequest = onDismiss, dialogProperties) {
    Box(
        modifier = modifier
            .clip(style.shape)
            .background(color = style.backgroundColor, shape = style.shape)
            .padding(style.contentPaddings),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
public fun StreamDialogPositiveNegative(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    dialogProperties: DialogProperties = DialogProperties(),
    style: DialogStyle = VideoTheme.styles.dialogStyles.defaultDialogStyle(),
    title: String? = null,
    icon: ImageVector? = null,
    contentText: String? = null,
    positiveButton: Triple<String, StreamButtonStyle, () -> Unit>? = null,
    negativeButton: Triple<String, StreamButtonStyle, () -> Unit>? = null,
    content: (@Composable () -> Unit)? = null,
): Unit = StreamDialog(
    modifier = modifier,
    style = style,
    onDismiss = onDismiss,
    dialogProperties = dialogProperties,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            icon?.let {
                val iconStyle = style.iconStyle
                Icon(
                    imageVector = icon,
                    contentDescription = "",
                    tint = iconStyle.color,
                )
                Spacer(modifier = Modifier.width(iconStyle.padding.calculateTopPadding()))
            }
            title?.let {
                Text(text = it, style = style.titleStyle)
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        contentText?.let {
            Text(text = it, style = style.contentTextStyle)
            Spacer(modifier = Modifier.size(32.dp))
        }
        content?.let {
            content()
            Spacer(modifier = Modifier.size(32.dp))
        }
        val horizontalArr = if (negativeButton == null) {
            Arrangement.End
        } else {
            Arrangement.SpaceBetween
        }
        Spacer(modifier = Modifier.size(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = horizontalArr) {
            negativeButton?.let {
                StreamButton(
                    modifier = Modifier.weight(1f),
                    text = it.first,
                    style = it.second,
                    onClick = it.third,
                )
                Spacer(modifier = Modifier.size(16.dp))
            }
            positiveButton?.let {
                StreamButton(
                    modifier = Modifier.weight(1f),
                    text = it.first,
                    style = it.second,
                    onClick = it.third,
                )
            }
        }
    }
}

@Preview
@Composable
private fun StreamDialogPreview() {
    VideoTheme {
        StreamDialogPositiveNegative(
            icon = Icons.Default.StopCircle,
            title = "This Call is Being Recorded",
            // Color is for preview only
            style = StreamDialogStyles.defaultDialogStyle().copy(
                backgroundColor = VideoTheme.colors.baseSheetTertiary,
            ),
            contentText = "By staying in the call you’re consenting to being recorded.",
            positiveButton = Triple(
                "Continue",
                ButtonStyles.secondaryButtonStyle(StyleSize.S),
            ) {
                // Do nothing
            },
            negativeButton = Triple(
                "Leave call",
                ButtonStyles.tertiaryButtonStyle(StyleSize.S),
            ) {
                // Do nothing
            },
        )
    }
}

@Preview
@Composable
private fun StreamDialogWithInputPreview() {
    VideoTheme {
        StreamDialogPositiveNegative(
            content = {
                Text(
                    text = "How is your call Going?",
                    style = TextStyle(
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight(500),
                        color = VideoTheme.colors.basePrimary,
                        textAlign = TextAlign.Center,
                    ),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "All feedback is celebrated!",
                    style = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 18.5.sp,
                        fontWeight = FontWeight(400),
                        color = VideoTheme.colors.baseSecondary,
                        textAlign = TextAlign.Center,
                    ),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamTextField(
                    value = TextFieldValue(""),
                    placeholder = "Email address (required)",
                    onValueChange = {},
                    style = StreamTextFieldStyles.defaultTextField(StyleSize.S),
                )

                Spacer(modifier = Modifier.size(16.dp))
                StreamTextField(
                    value = TextFieldValue(""),
                    placeholder = "Message",
                    onValueChange = {},
                    minLines = 7,
                    style = StreamTextFieldStyles.defaultTextField(StyleSize.S),
                )
            },
            // Color is for preview only
            style = StreamDialogStyles.defaultDialogStyle().copy(
                backgroundColor = VideoTheme.colors.baseSheetTertiary,
            ),
            positiveButton = Triple(
                "Submit",
                ButtonStyles.secondaryButtonStyle(StyleSize.S),
            ) {
                // Do nothing
            },
            negativeButton = Triple(
                "Not now",
                ButtonStyles.tertiaryButtonStyle(StyleSize.S),
            ) {
                // Do nothing
            },
        )
    }
}
