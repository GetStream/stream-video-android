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

package io.getstream.video.android.tutorial.livestream.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.TextFieldValue
import io.getstream.video.android.compose.ui.components.base.StreamTextField

data class CidInputState(
    val cid: String,
)

@Composable
fun rememberCidInputState(
    cid: String = "",
): MutableState<CidInputState> = remember { mutableStateOf(CidInputState(cid)) }

@Composable
fun CidInput(state: MutableState<CidInputState>) {
    val tfValue = remember { mutableStateOf(TextFieldValue(state.value.cid)) }
    StreamTextField(
        value = tfValue.value,
        placeholder = "Call Id (required)",
        onValueChange = { new ->
            state.value = state.value.copy(cid = new.text)
        },
    )
}
