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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.ChatDialog
import io.getstream.video.android.ui.common.R

/**
 * A call action button represents displaying a chat dialog.
 *
 * @param modifier Optional Modifier for this action button.
 * @param enabled Whether or not this action button will handle input events.
 * @param onCallAction A [CallAction] event that will be fired.
 */
@Composable
public fun ChatDialogAction(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCallAction: (CallAction) -> Unit
) {
    CallControlActionBackground(
        modifier = modifier,
        isEnabled = true,
    ) {
        Icon(
            modifier = Modifier
                .padding(13.dp)
                .clickable(enabled = enabled) { onCallAction(ChatDialog) },
            tint = VideoTheme.colors.callActionIconEnabled,
            painter = painterResource(id = R.drawable.stream_video_ic_message),
            contentDescription = stringResource(R.string.stream_video_call_controls_chat_dialog)
        )
    }
}
