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
import io.getstream.video.android.xml.widget.participant.PictureInPictureView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart

fun PictureInPictureView.bindView(
    viewModel: CallViewModel,
    lifecycleOwner: LifecycleOwner,
) {
    startJob(lifecycleOwner) {
        viewModel.callState.filterNotNull().collectLatest { call ->
            setRendererInitializer { videoRenderer, trackId, trackType, onRender ->
                call.initRenderer(videoRenderer, trackId, trackType, onRender)
            }
        }
    }

    startJob(lifecycleOwner) {
        combine(
            viewModel.primarySpeaker,
            viewModel.screenSharingSessions,
            viewModel.localParticipant
        ) { primarySpeaker, screenSharingSessions, localParticipant ->
            Triple(primarySpeaker, screenSharingSessions.firstOrNull(), localParticipant)
        }.onStart {
            when {
                viewModel.screenSharingSessions.value.firstOrNull() != null -> {
                    showScreenShare(viewModel.screenSharingSessions.value.first())
                }
                viewModel.primarySpeaker.value != null -> {
                    viewModel.primarySpeaker.value?.let {
                        showCallParticipantView(it)
                    }
                }
                else -> {
                    viewModel.localParticipant.value?.let {
                        showCallParticipantView(it)
                    }
                }
            }
        }.collect { (primarySpeaker, screenSharingSession, localParticipant) ->
            when {
                screenSharingSession != null -> showScreenShare(screenSharingSession)
                primarySpeaker != null -> {
                    showCallParticipantView(primarySpeaker)
                }
                localParticipant != null -> {
                    showCallParticipantView(localParticipant)
                }
            }
        }
    }
}
