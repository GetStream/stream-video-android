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

package io.getstream.video.android.ui.xml.binding

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.getstream.log.StreamLog
import io.getstream.video.android.call.state.FlipCamera
import io.getstream.video.android.call.state.LeaveCall
import io.getstream.video.android.call.state.ToggleCamera
import io.getstream.video.android.call.state.ToggleMicrophone
import io.getstream.video.android.call.state.ToggleSpeakerphone
import io.getstream.video.android.model.state.StreamCallState
import io.getstream.video.android.ui.common.R
import io.getstream.video.android.ui.xml.widget.active.ActiveCallView
import io.getstream.video.android.ui.xml.widget.control.CallControlItem
import io.getstream.video.android.viewmodel.CallViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

public fun ActiveCallView.bindView(
    viewModel: CallViewModel,
    lifecycleOwner: LifecycleOwner,
) {
    callControlActionListener = { item ->
        viewModel.onCallAction(item)
    }

    setControlItems(buildDefaultControlList())

    observeStreamCallState(viewModel, lifecycleOwner)
    observeCallState(viewModel, lifecycleOwner)
    observeParticipantList(viewModel, lifecycleOwner)
    observeMediaState(viewModel, lifecycleOwner)
}

private fun buildDefaultControlList(): List<CallControlItem> {
    return listOf(
        CallControlItem(R.drawable.ic_speaker_on, ToggleSpeakerphone(isEnabled = true)),
        CallControlItem(R.drawable.ic_videocam_on, ToggleCamera(isEnabled = true)),
        CallControlItem(R.drawable.ic_mic_on, ToggleMicrophone(isEnabled = true)),
        CallControlItem(R.drawable.ic_camera_flip, FlipCamera),
        CallControlItem(R.drawable.ic_call_end, LeaveCall),
    )
}

private fun ActiveCallView.observeStreamCallState(
    viewModel: CallViewModel,
    lifecycleOwner: LifecycleOwner,
) {
    lifecycleOwner.lifecycleScope.launchWhenCreated {
        viewModel.streamCallState.collect {
            val callId = when (val state = it) {
                is StreamCallState.Active -> state.callGuid.id
                else -> ""
            }
            val status = it.formatAsTitle()

            val title = when (callId.isBlank()) {
                true -> status
                else -> "$status: $callId"
            }
            setToolbarTitle(title)
        }
    }
}

private fun ActiveCallView.observeCallState(
    viewModel: CallViewModel,
    lifecycleOwner: LifecycleOwner,
) {
    lifecycleOwner.lifecycleScope.launchWhenResumed {
        viewModel.callState.filterNotNull().distinctUntilChanged().collectLatest { call ->
            setParticipantsRendererInitializer { videoRenderer, trackId, onRender ->
                call.initRenderer(videoRenderer, trackId, onRender)
            }
        }
    }
}

private fun ActiveCallView.observeParticipantList(
    viewModel: CallViewModel,
    lifecycleOwner: LifecycleOwner,
) {
    lifecycleOwner.lifecycleScope.launchWhenResumed {
        viewModel.participantList.collectLatest {
            updateParticipants(it)
        }
    }
}

private fun ActiveCallView.observeMediaState(
    viewModel: CallViewModel,
    lifecycleOwner: LifecycleOwner,
) {
    lifecycleOwner.lifecycleScope.launchWhenResumed {
        viewModel.callMediaState.collectLatest {
            StreamLog.d("callMediaState") { it.toString() }
            updateControlItems(
                listOf(
                    CallControlItem(
                        icon = if (it.isSpeakerphoneEnabled) {
                            R.drawable.ic_speaker_on
                        } else {
                            R.drawable.ic_speaker_off
                        },
                        action = ToggleSpeakerphone(it.isSpeakerphoneEnabled)),
                    CallControlItem(
                        icon = if (it.isCameraEnabled) {
                            R.drawable.ic_videocam_on
                        } else {
                            R.drawable.ic_videocam_off
                        },
                        action = ToggleCamera(it.isCameraEnabled)),
                    CallControlItem(
                        icon = if (it.isMicrophoneEnabled) {
                            R.drawable.ic_mic_on
                        } else {
                            R.drawable.ic_mic_off
                        },
                        action = ToggleMicrophone(it.isMicrophoneEnabled))
                )
            )
        }
    }
}

private fun StreamCallState.formatAsTitle() = when (this) {
    is StreamCallState.Drop -> "Drop"
    is StreamCallState.Joined -> "Joined"
    is StreamCallState.Connecting -> "Connecting"
    is StreamCallState.Connected -> "Connected"
    is StreamCallState.Incoming -> "Incoming"
    is StreamCallState.Joining -> "Joining"
    is StreamCallState.Outgoing -> "Outgoing"
    StreamCallState.Idle -> "Idle"
}
