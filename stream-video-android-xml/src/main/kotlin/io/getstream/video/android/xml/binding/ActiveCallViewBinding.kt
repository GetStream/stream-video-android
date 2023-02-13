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

package io.getstream.video.android.xml.binding

import androidx.lifecycle.LifecycleOwner
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallMediaState
import io.getstream.log.StreamLog
import io.getstream.video.android.core.call.state.FlipCamera
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.call.state.ToggleSpeakerphone
import io.getstream.video.android.core.viewmodel.CallViewModel
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.widget.active.ActiveCallView
import io.getstream.video.android.xml.widget.control.CallControlItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Binds [ActiveCallView] with [CallViewModel], updating the view's state based on data provided by the ViewModel,
 * and propagating view events to the ViewModel as needed.
 *
 * This function sets listeners on the view and ViewModel. Call this method
 * before setting any additional listeners on these objects yourself.
 *
 * @param viewModel [CallViewModel] for observing data and running actions.
 * @param lifecycleOwner The lifecycle owner, root component containing [ActiveCallView]. Usually an Activity or
 * Fragment.
 * @param updateCallMediaState Called every time [CallMediaState] changes to update the UI.
 * @param onCallAction Handler that listens to interactions with call media controls.
 */
public fun ActiveCallView.bindView(
    viewModel: CallViewModel,
    lifecycleOwner: LifecycleOwner,
    updateCallMediaState: (CallMediaState) -> List<CallControlItem> = { defaultControlList(it) },
    onCallAction: (CallAction) -> Unit = viewModel::onCallAction
) {

    this.callActionListener = onCallAction

    startJob(lifecycleOwner) {
        viewModel.callState.filterNotNull().collectLatest { call ->
            setParticipantsRendererInitializer { videoRenderer, trackId, trackType, onRender ->
                call.initRenderer(videoRenderer, trackId, trackType, onRender)
            }
        }
    }

    startJob(lifecycleOwner) {
        viewModel.participantList.collectLatest {
            updateParticipants(it)
        }
    }

    startJob(lifecycleOwner) {
        viewModel.screenSharingSessions.collectLatest {
            StreamLog.getLogger("CallParticipantsView").d { "update screen sharing: $it" }
            updateScreenSharingSession(it.firstOrNull())
        }
    }

    lifecycleOwner.lifecycleScope.launchWhenResumed {
        viewModel.primarySpeaker.collectLatest {
            updatePrimarySpeaker(it)
        }
    }

    startJob(lifecycleOwner) {
        viewModel.callMediaState.collectLatest {
            setControlItems(updateCallMediaState(it))
        }
    }
}

private fun defaultControlList(callMediaState: CallMediaState): List<CallControlItem> {
    return listOf(
        CallControlItem(
            icon = if (callMediaState.isSpeakerphoneEnabled) {
                RCommon.drawable.ic_speaker_on
            } else {
                RCommon.drawable.ic_speaker_off
            },
            iconTint = R.color.stream_black,
            backgroundTint = RCommon.color.stream_app_background,
            action = ToggleSpeakerphone(callMediaState.isSpeakerphoneEnabled)
        ),
        CallControlItem(
            icon = if (callMediaState.isCameraEnabled) {
                RCommon.drawable.ic_videocam_on
            } else {
                RCommon.drawable.ic_videocam_off
            },
            iconTint = R.color.stream_black,
            backgroundTint = RCommon.color.stream_app_background,
            action = ToggleCamera(callMediaState.isCameraEnabled)
        ),
        CallControlItem(
            icon = if (callMediaState.isMicrophoneEnabled) {
                RCommon.drawable.ic_mic_on
            } else {
                RCommon.drawable.ic_mic_off
            },
            iconTint = R.color.stream_black,
            backgroundTint = RCommon.color.stream_app_background,
            action = ToggleMicrophone(callMediaState.isMicrophoneEnabled)
        ),
        CallControlItem(
            icon = RCommon.drawable.ic_camera_flip,
            iconTint = R.color.stream_black,
            backgroundTint = RCommon.color.stream_app_background,
            action = FlipCamera
        ),
        CallControlItem(
            icon = RCommon.drawable.ic_call_end,
            iconTint = R.color.stream_black,
            backgroundTint = RCommon.color.stream_error_accent,
            action = LeaveCall
        ),
    )
}
