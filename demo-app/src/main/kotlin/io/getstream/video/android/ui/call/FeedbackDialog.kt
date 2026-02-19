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

package io.getstream.video.android.ui.call

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamButton
import io.getstream.video.android.compose.ui.components.base.StreamDialog
import io.getstream.video.android.compose.ui.components.base.StreamDialogPositiveNegative
import io.getstream.video.android.compose.ui.components.base.StreamTextField
import io.getstream.video.android.compose.ui.components.base.styling.ButtonStyles
import io.getstream.video.android.compose.ui.components.base.styling.StreamDialogStyles
import io.getstream.video.android.compose.ui.components.base.styling.StreamTextFieldStyles
import io.getstream.video.android.compose.ui.components.base.styling.StyleSize
import io.getstream.video.android.core.Call
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.util.FeedbackSender

@Composable
fun FeedbackDialog(call: Call, onDismiss: () -> Unit) {
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var message by remember { mutableStateOf(TextFieldValue("")) }
    var isError by remember { mutableStateOf(false) }
    var feedbackFinished by remember { mutableStateOf(false) }
    var feedbackError by remember { mutableStateOf(false) }
    val sender = remember { FeedbackSender() }

    if (feedbackFinished) {
        StreamDialog(style = VideoTheme.styles.dialogStyles.defaultDialogStyle()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.feedback_artwork),
                    contentDescription = "artwork",
                )
                if (feedbackError) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "alert",
                        tint = VideoTheme.colors.alertWarning,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Text(
                    text = "Your message was successfully sent",
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
                    text =
                    if (feedbackError) {
                        "Something happened and we could not process your request.\n Please try agian later."
                    } else {
                        "Thank you for letting us know how we can continue to improve our\n" +
                            "product and deliver the best calling experience possible. Hope you had\n" +
                            "a good call."
                    },
                    style = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 18.5.sp,
                        fontWeight = FontWeight(400),
                        color = VideoTheme.colors.baseSecondary,
                        textAlign = TextAlign.Center,
                    ),
                )

                Spacer(modifier = Modifier.size(24.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    StreamButton(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        text = "Close",
                        style = VideoTheme.styles.buttonStyles.tertiaryButtonStyle(),
                    ) {
                        onDismiss()
                    }
                }
            }
        }
    } else {
        StreamDialogPositiveNegative(
            onDismiss = onDismiss,
            content = {
                Image(
                    painter = painterResource(id = R.drawable.feedback_artwork),
                    contentDescription = "artwork",
                )
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
                    value = email,
                    placeholder = "Email address (required)",
                    onValueChange = {
                        email = it
                    },
                    error = isError,
                    style = StreamTextFieldStyles.defaultTextField(StyleSize.S),
                )

                Spacer(modifier = Modifier.size(16.dp))
                StreamTextField(
                    value = message,
                    placeholder = "Message",
                    onValueChange = {
                        message = it
                    },
                    minLines = 7,
                    style = StreamTextFieldStyles.defaultTextField(StyleSize.S),
                )
            },
            style = StreamDialogStyles.defaultDialogStyle(),
            positiveButton = Triple(
                "Submit",
                ButtonStyles.secondaryButtonStyle(StyleSize.S),
            ) {
                if (email.text.isEmpty() || !sender.isValidEmail(email.text)) {
                    isError = true
                } else {
                    sender.sendFeedback(email.text, message.text, call.cid) {
                        feedbackError = it
                        feedbackFinished = true
                    }
                }
            },
            negativeButton = Triple(
                "Not now",
                ButtonStyles.tertiaryButtonStyle(StyleSize.S),
            ) {
                onDismiss()
            },
        )
    }
}

@Preview
@Composable
private fun FeedbackDialogPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        FeedbackDialog(call = previewCall) {
        }
    }
}
