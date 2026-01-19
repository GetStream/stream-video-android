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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.chat.android.ui.common.state.messages.list.MessageItemState

@Composable
fun ChatOverly(
    modifier: Modifier = Modifier,
    messages: List<MessageItemState>,
) {
    val configuration = LocalConfiguration.current
    Column(modifier = modifier.width((configuration.screenWidthDp * 0.65f).dp)) {
        if (messages.isNotEmpty()) {
            Message(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .alpha(0.15f),
                messageItemState = messages[0],
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (messages.size > 1) {
            Message(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .alpha(0.3f),
                messageItemState = messages[1],
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (messages.size > 2) {
            Message(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .alpha(0.45f),
                messageItemState = messages[2],
            )
        }
    }
}

@Composable
private fun Message(
    modifier: Modifier,
    messageItemState: MessageItemState,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = modifier
                .matchParentSize()
                .background(Color.Black),
        )

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            text = messageItemState.message.text,
            color = Color.White,
            fontSize = 13.sp,
        )
    }
}
