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

package io.getstream.video.android.compose.ui.components.audio

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState

@Composable
public fun ParticipantAudio(
    call: Call,
    participant: ParticipantState,
    modifier: Modifier = Modifier,
    style: AudioRendererStyle = RegularAudioRendererStyle(),
    microphoneIndicatorContent: @Composable BoxScope.(ParticipantState) -> Unit = {
    }
) {
    val participants by call.state.participants.collectAsStateWithLifecycle()
}
