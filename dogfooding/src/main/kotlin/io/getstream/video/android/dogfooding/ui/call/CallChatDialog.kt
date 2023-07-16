/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

@file:OptIn(ExperimentalMaterialApi::class)

package io.getstream.video.android.dogfooding.ui.call

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun CallChatDialog(
    state: ModalBottomSheetState,
    content: @Composable () -> Unit,
    onDismissed: () -> Unit
) {
    ModalBottomSheetLayout(
        modifier = Modifier.fillMaxWidth(),
        sheetState = state,
        sheetContent = {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .clickable { onDismissed.invoke() },
                text = "qwdqwdqwdqwd"
            )
        },
        content = content
    )
}
