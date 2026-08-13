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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.styling.StreamTextFieldStyles

@Preview
@Composable
private fun StreamInputFieldsPreviews() {
    VideoTheme {
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
