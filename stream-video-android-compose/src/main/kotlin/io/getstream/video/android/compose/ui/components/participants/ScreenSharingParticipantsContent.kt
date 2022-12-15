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

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.view.View
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import io.getstream.video.android.call.state.CallAction
import io.getstream.video.android.compose.ui.components.participants.internal.LandscapeScreenSharingContent
import io.getstream.video.android.compose.ui.components.participants.internal.PortraitScreenSharingContent
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.model.ScreenSharingSession

@Composable
public fun ScreenSharingCallParticipantsContent(
    call: Call,
    session: ScreenSharingSession,
    participants: List<CallParticipantState>,
    onCallAction: (CallAction) -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    isFullscreen: Boolean = false,
    onRender: (View) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation

    if (orientation == ORIENTATION_PORTRAIT) {
        PortraitScreenSharingContent(
            call = call,
            session = session,
            participants = participants,
            paddingValues = paddingValues,
            modifier = modifier,
            onRender = onRender,
            onCallAction = onCallAction
        )
    } else {
        LandscapeScreenSharingContent(
            call = call,
            session = session,
            participants = participants,
            paddingValues = paddingValues,
            modifier = modifier,
            onRender = onRender,
            isFullscreen = isFullscreen,
            onCallAction = onCallAction
        )
    }
}
