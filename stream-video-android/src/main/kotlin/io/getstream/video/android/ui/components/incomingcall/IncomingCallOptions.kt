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

package io.getstream.video.android.ui.components.incomingcall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.R
import io.getstream.video.android.model.CallType
import io.getstream.video.android.theme.VideoTheme

@Composable
public fun IncomingCallOptions(
    callId: String,
    callType: CallType,
    onDeclineCall: () -> Unit,
    onAcceptCall: (callId: String, isVideoEnabled: Boolean) -> Unit,
) {
    var isVideoEnabled by remember { mutableStateOf(true) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(
            modifier = Modifier
                .background(
                    color = VideoTheme.colors.errorAccent,
                    shape = VideoTheme.shapes.callButton
                )
                .size(VideoTheme.dimens.largeButtonSize),
            onClick = onDeclineCall,
            content = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_call_end),
                    contentDescription = "End call"
                )
            }
        )

        if (callType == CallType.VIDEO) {
            IconButton(
                modifier = Modifier
                    .background(
                        color = VideoTheme.colors.appBackground,
                        shape = VideoTheme.shapes.callButton
                    )
                    .size(VideoTheme.dimens.mediumButtonSize),
                onClick = {
                    isVideoEnabled = !isVideoEnabled
                },
                content = {
                    val cameraIcon =
                        if (isVideoEnabled) R.drawable.ic_videocam else R.drawable.ic_videocam_off

                    Icon(
                        painter = painterResource(id = cameraIcon),
                        contentDescription = "Toggle Video",
                        tint = VideoTheme.colors.textHighEmphasis
                    )
                }
            )
        }

        IconButton(
            modifier = Modifier
                .background(
                    color = VideoTheme.colors.infoAccent,
                    shape = VideoTheme.shapes.callButton
                )
                .size(VideoTheme.dimens.largeButtonSize),
            onClick = { onAcceptCall(callId, isVideoEnabled) },
            content = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_call),
                    contentDescription = "Accept call"
                )
            }
        )
    }
}

@Preview
@Composable
private fun IncomingCallOptionsPreview() {
    IncomingCallOptions(
        callId = "",
        callType = CallType.VIDEO,
        onDeclineCall = {},
        onAcceptCall = { _, _ -> }
    )
}
