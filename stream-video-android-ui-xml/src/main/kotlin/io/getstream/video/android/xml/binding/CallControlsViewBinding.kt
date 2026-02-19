/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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
import io.getstream.video.android.core.call.state.FlipCamera
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.call.state.ToggleSpeakerphone
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.state.CallDeviceState
import io.getstream.video.android.xml.viewmodel.CallViewModel
import io.getstream.video.android.xml.widget.control.CallControlItem
import io.getstream.video.android.xml.widget.control.CallControlsView
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Binds [CallControlsView] with [CallViewModel], updating the view's state based on data provided by the ViewModel,
 * and propagating view events to the ViewModel as needed.
 *
 * This function sets listeners on the view and ViewModel. Call this method
 * before setting any additional listeners on these objects yourself.
 *
 * @param viewModel [CallViewModel] for observing data and running actions.
 * @param lifecycleOwner The lifecycle owner, root component containing [CallControlsView]. Usually an Activity or
 * Fragment.
 * @param fetchCallMediaState Handler used to fetch the new state of the call controls buttons when the media state
 * changes.
 * @param onCallAction Handler that listens to interactions with call media controls.
 */
public fun CallControlsView.bindView(
    viewModel: CallViewModel,
    lifecycleOwner: LifecycleOwner,
    fetchCallMediaState: (
        CallDeviceState,
        Boolean,
    ) -> List<CallControlItem> = { mediaState, isScreenSharingActive ->
        defaultControlList(mediaState, isScreenSharingActive)
    },
    // onCallAction: (CallAction) -> Unit = viewModel::onCallAction,
) {
    this.onCallAction = onCallAction

    startJob(lifecycleOwner) {
//        viewModel.callMediaState.combine(viewModel.screenSharingSessions) { mediaState, screenSharingSessions ->
//            mediaState to screenSharingSessions.firstOrNull()
//        }.collect { (mediaState, screenSharingSession) ->
//            setItems(fetchCallMediaState(mediaState, screenSharingSession != null))
//        }
    }
}

internal fun defaultControlList(callDeviceState: CallDeviceState, isScreenSharingActive: Boolean): List<CallControlItem> {
    return listOf(
        CallControlItem(
            icon = if (callDeviceState.isSpeakerphoneEnabled) {
                RCommon.drawable.stream_video_ic_speaker_on
            } else {
                RCommon.drawable.stream_video_ic_speaker_off
            },
            iconTint = R.color.stream_video_black,
            backgroundTint = R.color.stream_video_white,
            action = ToggleSpeakerphone(!callDeviceState.isSpeakerphoneEnabled),
        ),
        CallControlItem(
            icon = if (callDeviceState.isCameraEnabled) {
                RCommon.drawable.stream_video_ic_videocam_on
            } else {
                RCommon.drawable.stream_video_ic_videocam_off
            },
            iconTint = R.color.stream_video_black,
            backgroundTint = R.color.stream_video_white,
            action = ToggleCamera(!callDeviceState.isCameraEnabled),
        ),
        CallControlItem(
            icon = if (callDeviceState.isMicrophoneEnabled) {
                RCommon.drawable.stream_video_ic_mic_on
            } else {
                RCommon.drawable.stream_video_ic_mic_off
            },
            iconTint = R.color.stream_video_black,
            backgroundTint = R.color.stream_video_white,
            action = ToggleMicrophone(!callDeviceState.isMicrophoneEnabled),
        ),
        CallControlItem(
            icon = RCommon.drawable.stream_video_ic_camera_flip,
            iconTint = R.color.stream_video_black,
            backgroundTint = if (!isScreenSharingActive) R.color.stream_video_white else RCommon.color.stream_video_disabled,
            action = FlipCamera,
            enabled = !isScreenSharingActive,
        ),
        CallControlItem(
            icon = RCommon.drawable.stream_video_ic_call_end,
            iconTint = R.color.stream_video_white,
            backgroundTint = RCommon.color.stream_video_error_accent,
            action = LeaveCall,
        ),
    )
}
