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

package io.getstream.video.android.compose.ui.components.participants

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.getstream.video.android.call.state.CallAction
import io.getstream.video.android.call.state.CallMediaState
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.LandscapeCallControls
import io.getstream.video.android.compose.ui.components.internal.OverlayAppBar
import io.getstream.video.android.compose.ui.components.participants.internal.Participants
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.state.StreamCallState

/**
 * Renders the CallParticipants when there are no screen sharing sessions, based on the orientation.
 *
 * @param call The call that contains all the participants state and tracks.
 * @param callMediaState The state of the call media, such as audio, video.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param modifier Modifier for styling.
 * @param paddingValues Padding within the parent.
 * @param onRender Handler when each of the Video views render their first frame.
 */
@Composable
public fun RegularCallParticipantsContent(
    call: Call,
    callMediaState: CallMediaState,
    callState: StreamCallState,
    onCallAction: (CallAction) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onRender: (View) -> Unit = {}
) {
    var parentSize: IntSize by remember { mutableStateOf(IntSize(0, 0)) }
    val orientation = LocalConfiguration.current.orientation

    Row(modifier = modifier.background(color = VideoTheme.colors.appBackground)) {
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val roomParticipants by call.callParticipants.collectAsState(emptyList())

            if (roomParticipants.isNotEmpty()) {
                Participants(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { parentSize = it },
                    call = call,
                    onRender = onRender,
                    paddingValues = paddingValues,
                    parentSize = parentSize
                )

                if (orientation == ORIENTATION_LANDSCAPE) {
                    OverlayAppBar(
                        callState = callState,
                        onBackPressed = onBackPressed,
                        onCallAction = onCallAction
                    )
                }
            }
        }

        if (orientation == ORIENTATION_LANDSCAPE) {
            LandscapeCallControls(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(VideoTheme.dimens.landscapeCallControlsSheetWidth),
                callMediaState = callMediaState,
                onCallAction = onCallAction,
                isScreenSharing = false
            )
        }
    }
}
