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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens

@Preview
@Composable
private fun StreamTextFieldRootPreview() {
    VideoTheme {
        StreamTextFieldPreview()
    }
}

/**
 * The text field states: empty with placeholder, with icons, filled, disabled, error and multi line.
 */
@Composable
internal fun StreamTextFieldPreview() {
    val icon = painterResource(R.drawable.stream_design_ic_search)
    Column(
        modifier = Modifier.padding(StreamTokens.spacingMd),
        verticalArrangement = Arrangement.spacedBy(StreamTokens.spacingMd),
    ) {
        StreamTextField(value = TextFieldValue(""), onValueChange = {}, placeholder = "Call ID")
        StreamTextField(value = TextFieldValue(""), onValueChange = {
        }, placeholder = "Call ID", leadingIcon = icon)
        StreamTextField(
            value = TextFieldValue("xStream-sync"),
            onValueChange = {},
            trailingIcon = icon,
        )
        StreamTextField(value = TextFieldValue(""), onValueChange = {
        }, placeholder = "Call ID", enabled = false)
        StreamTextField(
            value = TextFieldValue("xStream-sync"),
            onValueChange = {},
            errorText = "Incorrect Call ID. Try again.",
        )
        StreamTextField(value = TextFieldValue(""), onValueChange = {
        }, placeholder = "Message", minLines = 4)
    }
}
