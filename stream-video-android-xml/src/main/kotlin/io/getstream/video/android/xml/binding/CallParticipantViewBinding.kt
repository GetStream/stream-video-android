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

package io.getstream.video.android.xml.binding

import androidx.lifecycle.LifecycleOwner
import io.getstream.video.android.core.viewmodel.CallViewModel
import io.getstream.video.android.xml.widget.participant.CallParticipantView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart

public fun CallParticipantView.bindPipView(
    viewModel: CallViewModel,
    lifecycleOwner: LifecycleOwner,
) {
    startJob(lifecycleOwner) {
        viewModel.callState.filterNotNull().collectLatest { call ->
            setRendererInitializer { videoRenderer, streamId, trackType, onRender ->
                call.initRenderer(videoRenderer, streamId, trackType, onRender)
            }
        }
    }

    startJob(lifecycleOwner) {
        viewModel.primarySpeaker.combine(viewModel.localParticipant) { primarySpeaker, localParticipant ->
            primarySpeaker to localParticipant
        }.onStart {
            emit(viewModel.primarySpeaker.value to viewModel.localParticipant.value)
        }.collectLatest { (primarySpeaker, localParticipant) ->
            setParticipant(primarySpeaker ?: localParticipant ?: return@collectLatest)
        }
    }
}
