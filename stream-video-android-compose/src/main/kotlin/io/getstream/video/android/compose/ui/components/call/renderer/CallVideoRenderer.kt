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

package io.getstream.video.android.compose.ui.components.call.renderer

import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.common.util.MockUtils
import io.getstream.video.android.common.util.mockCall
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.renderer.internal.RegularCallVideoRenderer
import io.getstream.video.android.compose.ui.components.call.renderer.internal.ScreenSharingCallVideoRenderer
import io.getstream.video.android.core.Call

/**
 * Renders all the CallParticipants, based on the number of people in a call and the call state.
 * Also takes into account if there are any screen sharing sessions active and adjusts the UI
 * accordingly.
 *
 * @param call The call that contains all the participants state and tracks.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param modifier Modifier for styling.
 * @param onRender Handler when each of the Video views render their first frame.
 * @param onBackPressed Handler when the user taps back.
 */
@Composable
public fun CallVideoRenderer(
    call: Call,
    modifier: Modifier = Modifier,
    onRender: (View) -> Unit = {},
) {
    if (LocalInspectionMode.current) {
        RegularCallVideoRenderer(
            call = call,
            modifier = modifier,
            onRender = onRender,
        )
        return
    }

    val screenSharingSession = call.state.screenSharingSession.collectAsState()
    val screenSharing = screenSharingSession.value

    if (screenSharing == null) {
        RegularCallVideoRenderer(
            call = call,
            modifier = modifier,
            onRender = onRender,
        )
    } else {
        val participants by call.state.participants.collectAsState()

        ScreenSharingCallVideoRenderer(
            call = call,
            modifier = modifier,
            session = screenSharing,
            participants = participants,
            onRender = onRender,
        )
    }
}

@Preview
@Composable
private fun CallVideoRendererPreview() {
    MockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallVideoRenderer(
            call = mockCall,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
