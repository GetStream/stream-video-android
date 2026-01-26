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

package io.getstream.video.android.tutorial.livestream.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimeLabel(
    modifier: Modifier = Modifier,
    sessionTime: Long,
) {
    val time by remember(sessionTime) {
        val date = Date(sessionTime)
        val format = SimpleDateFormat("mm:ss", Locale.US)
        mutableStateOf(format.format(date))
    }

    Row(
        modifier = modifier
            .background(Color(0xFF1C1E22), RoundedCornerShape(6.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .size(28.dp)
                .padding(start = 12.dp),
            imageVector = Icons.Default.CheckCircle,
            tint = VideoTheme.colors.basePrimary,
            contentDescription = null,
        )

        Text(
            modifier = Modifier.padding(horizontal = 12.dp),
            text = time,
            color = Color.White,
        )
    }
}

@Preview
@Composable
private fun TimeLabelPreview() {
    VideoTheme {
        TimeLabel(
            modifier = Modifier,
            sessionTime = 12000,
        )
    }
}
