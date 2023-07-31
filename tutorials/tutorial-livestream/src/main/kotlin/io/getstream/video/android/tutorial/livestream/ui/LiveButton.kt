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

package io.getstream.video.android.tutorial.livestream.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.theme.VideoTheme

@Composable
fun LiveButton(
    modifier: Modifier,
    isBackstage: Boolean,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color(0xFF1C1E22),
            contentColor = Color(0xFF1C1E22)
        ),
        onClick = onClick
    ) {
        Icon(
            modifier = Modifier.padding(vertical = 3.dp, horizontal = 6.dp),
            imageVector = if (isBackstage) {
                Icons.Default.PlayArrow
            } else {
                Icons.Default.Close
            },
            tint = Color.White,
            contentDescription = null
        )

        Text(
            modifier = Modifier.padding(end = 6.dp),
            text = if (isBackstage) "Go Live" else "Stop Broadcast",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White
        )
    }
}

@Preview
@Composable
private fun LiveButtonPreview() {
    VideoTheme {
        LiveButton(
            modifier = Modifier,
            isBackstage = true,
        ) {}
    }
}
