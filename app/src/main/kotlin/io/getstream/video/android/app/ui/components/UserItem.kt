/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.getstream.video.android.model.UserCredentials

@Composable
fun UserItem(
    credentials: UserCredentials,
    onClick: (UserCredentials) -> Unit
) {
    val buttonColor = if (!credentials.isSelected) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.secondary
    }

    val colors = ButtonDefaults.textButtonColors(
        backgroundColor = buttonColor,
        contentColor = Color.White
    )

    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        colors = colors,
        content = {
            Text(text = credentials.name)
        },
        onClick = { onClick(credentials) },
    )
}
