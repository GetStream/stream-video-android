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

package io.getstream.video.android.ui.outgoing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.ui.theme.Colors
import io.getstream.video.android.ui.theme.StreamButton

@Composable
fun DebugCallScreen(
    navigateToRingCall: (callId: String, membersList: String) -> Unit
) {
    var callId by remember { mutableStateOf("") }
    var membersList by remember { mutableStateOf("") }

    VideoTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Colors.background)
                    .padding(12.dp),
                horizontalAlignment = Alignment.Start
            ) {

                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = "Call ID (optional)",
                    color = Color(0xFF979797),
                    fontSize = 13.sp,
                )

                Spacer(modifier = Modifier.height(4.dp))

                TextField(
                    modifier = Modifier.border(
                        BorderStroke(1.dp, Color(0xFF4C525C)),
                        RoundedCornerShape(6.dp)
                    ),
                    value = callId,
                    onValueChange = { callId = it },
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = Color.White,
                        focusedLabelColor = VideoTheme.colors.primaryAccent,
                        unfocusedIndicatorColor = Colors.secondBackground,
                        focusedIndicatorColor = Colors.secondBackground,
                        backgroundColor = Colors.secondBackground
                    ),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = "Members list - separated by comma",
                    color = Color(0xFF979797),
                    fontSize = 13.sp,
                )

                Spacer(modifier = Modifier.height(4.dp))

                TextField(
                    modifier = Modifier.border(
                        BorderStroke(1.dp, Color(0xFF4C525C)),
                        RoundedCornerShape(6.dp)
                    ),
                    value = membersList,
                    onValueChange = { membersList = it },
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = Color.White,
                        focusedLabelColor = VideoTheme.colors.primaryAccent,
                        unfocusedIndicatorColor = Colors.secondBackground,
                        focusedIndicatorColor = Colors.secondBackground,
                        backgroundColor = Colors.secondBackground
                    ),
                )

                Spacer(modifier = Modifier.height(4.dp))

                StreamButton(
                    modifier = Modifier,
                    onClick = {
                        navigateToRingCall.invoke(callId, membersList)
                    },
                    text = "Ring"
                )
            }
        }
    }
}

@Preview
@Composable
private fun CallLobbyScreenPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        DebugCallScreen(
            navigateToRingCall = { _, _ -> }
        )
    }
}
