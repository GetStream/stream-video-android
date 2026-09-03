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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamButtonSize
import io.getstream.video.android.compose.ui.components.base.StreamButtonStyleDefaults
import io.getstream.video.android.compose.ui.components.base.StreamDialog
import io.getstream.video.android.compose.ui.components.base.StreamTextButton
import io.getstream.video.android.compose.ui.components.base.StreamTextField
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
        StreamDialog(
            onDismissRequest = onDismiss,
            title = if (feedbackError) "Something went wrong" else "Your message was successfully sent",
            message = if (feedbackError) {
                "Something happened and we could not process your request. Please try again later."
            } else {
                "Thank you for letting us know how we can continue to improve our product and " +
                    "deliver the best calling experience possible. Hope you had a good call."
            },
        ) {
            StreamTextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss,
                text = "Close",
                style = StreamButtonStyleDefaults.secondaryOutline,
                size = StreamButtonSize.Large,
            )
        }
    } else {
        StreamDialog(
            onDismissRequest = onDismiss,
            title = "How is your call going?",
            message = "All feedback is celebrated!",
        ) {
            StreamTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email address (required)",
                errorText = if (isError) "Enter a valid email address." else null,
            )
            StreamTextField(
                value = message,
                onValueChange = { message = it },
                placeholder = "Message",
                minLines = 4,
            )
            StreamTextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (email.text.isEmpty() || !sender.isValidEmail(email.text)) {
                        isError = true
                    } else {
                        sender.sendFeedback(email.text, message.text, call.cid) {
                            feedbackError = it
                            feedbackFinished = true
                        }
                    }
                },
                text = "Submit",
                size = StreamButtonSize.Large,
            )
            StreamTextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss,
                text = "Not now",
                style = StreamButtonStyleDefaults.secondaryGhost,
                size = StreamButtonSize.Large,
            )
        }
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
