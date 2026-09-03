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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.theme.VideoTheme

@Preview
@Composable
private fun StreamDialogRootPreview() {
    VideoTheme {
        StreamDialogPreview()
    }
}

/**
 * The recording consent dialog: icon, title, message and two stacked actions.
 */
@Composable
internal fun StreamDialogPreview() {
    StreamDialog(
        onDismissRequest = {},
        icon = painterResource(R.drawable.stream_design_ic_recording_fill),
        title = "This Call is Being Recorded",
        message = "By staying in the call you’re consenting to being recorded.",
    ) {
        StreamTextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
            text = "Continue",
            size = StreamButtonSize.Large,
        )
        StreamTextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
            text = "Leave Call",
            style = StreamButtonStyleDefaults.secondaryOutline,
            size = StreamButtonSize.Large,
        )
    }
}

@Preview
@Composable
private fun StreamDialogWithInputRootPreview() {
    VideoTheme {
        StreamDialogWithInputPreview()
    }
}

/**
 * A dialog with custom content: two text fields above the actions.
 */
@Composable
internal fun StreamDialogWithInputPreview() {
    StreamDialog(
        onDismissRequest = {},
        title = "How is your call going?",
        message = "All feedback is celebrated!",
    ) {
        StreamTextField(
            value = TextFieldValue(""),
            onValueChange = {},
            placeholder = "Email address (required)",
        )
        StreamTextField(
            value = TextFieldValue(""),
            onValueChange = {},
            placeholder = "Message",
            minLines = 4,
        )
        StreamTextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
            text = "Submit",
            size = StreamButtonSize.Large,
        )
        StreamTextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
            text = "Not now",
            style = StreamButtonStyleDefaults.secondaryGhost,
            size = StreamButtonSize.Large,
        )
    }
}
