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

package io.getstream.video.android.compose.ui.components.call.activecall.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import io.getstream.video.android.call.state.CallAction
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.model.state.StreamCallState
import io.getstream.video.android.utils.formatAsTitle
import io.getstream.video.android.viewmodel.CallViewModel

@Composable
internal fun ActiveCallAppBar(
    callViewModel: CallViewModel,
    onBackPressed: () -> Unit,
    onCallAction: (CallAction) -> Unit
) {
    val callState by callViewModel.streamCallState.collectAsState(initial = StreamCallState.Idle)
    val isShowingCallInfo by callViewModel.isShowingCallInfo.collectAsState()

    val callId = when (val state = callState) {
        is StreamCallState.Active -> state.callGuid.id
        else -> ""
    }
    val status = callState.formatAsTitle(LocalContext.current)

    val title = when (callId.isBlank()) {
        true -> status
        else -> "$status: $callId"
    }

    CallAppBar(
        title = title,
        isShowingOverlays = isShowingCallInfo,
        onBackPressed = onBackPressed,
        onCallAction = onCallAction
    )
}
