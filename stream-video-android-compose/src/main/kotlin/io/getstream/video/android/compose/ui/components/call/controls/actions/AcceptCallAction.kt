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

package io.getstream.video.android.compose.ui.components.call.controls.actions

import androidx.compose.foundation.background
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.call.state.AcceptCall
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.ui.common.R

@Composable
public fun AcceptCallAction(
    modifier: Modifier = Modifier,
    onCallAction: (CallAction) -> Unit,
) {
    IconButton(
        modifier = modifier.background(
            color = VideoTheme.colors.infoAccent,
            shape = VideoTheme.shapes.callButton
        ),
        onClick = { onCallAction(AcceptCall) },
        content = {
            Icon(
                painter = painterResource(id = R.drawable.stream_video_ic_call),
                tint = Color.White,
                contentDescription = stringResource(R.string.stream_video_call_controls_accept_call)
            )
        }
    )
}
