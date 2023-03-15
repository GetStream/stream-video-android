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
import androidx.compose.ui.platform.LocalContext
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.model.state.StreamCallState
import io.getstream.video.android.core.utils.formatAsTitle

@Composable
internal fun ActiveCallAppBar(
    callState: StreamCallState,
    isShowingCallInfo: Boolean,
    onBackPressed: () -> Unit,
    onCallAction: (CallAction) -> Unit,
) {
    val callId = when (callState) {
        is StreamCallState.Active -> callState.callGuid.id
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
