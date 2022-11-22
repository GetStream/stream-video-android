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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.model.state.StreamCallState
import io.getstream.video.android.viewmodel.CallViewModel

@Composable
internal fun ActiveCallAppBar(
    callViewModel: CallViewModel,
    onParticipantsMenuClick: () -> Unit = callViewModel::showParticipants
) {
    val callState by callViewModel.streamCallState.collectAsState(initial = StreamCallState.Idle)

    val callId = when (val state = callState) {
        is StreamCallState.Active -> state.callGuid.id
        else -> ""
    }
    val status = callState.formatAsTitle()

    val title = when (callId.isBlank()) {
        true -> status
        else -> "$status: $callId"
    }

    Box(
        modifier = Modifier
            .height(56.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colors.primary)
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Icon(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable(onClick = onParticipantsMenuClick)
                .padding(8.dp),
            imageVector = Icons.Default.Menu,
            contentDescription = "Participants info",
            tint = Color.White
        )
    }
}

@Composable
private fun StreamCallState.formatAsTitle() = when (this) {
    // TODO stringResource(id = )
    is StreamCallState.Drop -> "Drop"
    is StreamCallState.Joined -> "Joined"
    is StreamCallState.Initializing -> "Initializing"
    is StreamCallState.Connecting -> "Connecting"
    is StreamCallState.Connected -> "Connected"
    is StreamCallState.Incoming -> "Incoming"
    is StreamCallState.Joining -> "Joining"
    is StreamCallState.Outgoing -> "Outgoing"
    is StreamCallState.Starting -> "Starting"
    StreamCallState.Idle -> "Idle"
}
