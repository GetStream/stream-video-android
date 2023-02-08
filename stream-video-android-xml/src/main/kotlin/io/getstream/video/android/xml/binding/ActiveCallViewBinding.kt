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
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Binds [ActiveCallView] with [CallViewModel], updating the view's state based on data provided by the ViewModel,
 * and propagating view events to the ViewModel as needed.
 *
 * This function sets listeners on the view and ViewModel. Call this method
 * before setting any additional listeners on these objects yourself.
 */
public fun ActiveCallView.bindView(
    viewModel: CallViewModel,
    lifecycleOwner: LifecycleOwner,
) {
    callActionListener = { item ->
        viewModel.onCallAction(item)
    }

    setControlItems(buildDefaultControlList())

    lifecycleOwner.lifecycleScope.launchWhenResumed {
        viewModel.callState.filterNotNull().distinctUntilChanged().collectLatest { call ->
            setParticipantsRendererInitializer { videoRenderer, trackId, trackType, onRender ->
                call.initRenderer(videoRenderer, trackId, trackType, onRender)
            }
        }
    }

    lifecycleOwner.lifecycleScope.launchWhenResumed {
        viewModel.participantList.collectLatest {
            updateParticipants(it)
        }
    }

    lifecycleOwner.lifecycleScope.launchWhenResumed {
        viewModel.primarySpeaker.collectLatest {
            updatePrimarySpeaker(it)
        }
    }

    lifecycleOwner.lifecycleScope.launchWhenResumed {
        viewModel.callMediaState.collectLatest {
            updateControlItems(
                listOf(
                    CallControlItem(
                        icon = if (it.isSpeakerphoneEnabled) {
                            RCommon.drawable.ic_speaker_on
                        } else {
                            RCommon.drawable.ic_speaker_off
                        },
                        iconTint = R.color.stream_black,
                        backgroundTint = RCommon.color.stream_app_background,
                        action = ToggleSpeakerphone(it.isSpeakerphoneEnabled)
                    ),
                    CallControlItem(
                        icon = if (it.isCameraEnabled) {
                            RCommon.drawable.ic_videocam_on
                        } else {
                            RCommon.drawable.ic_videocam_off
                        },
                        iconTint = R.color.stream_black,
                        backgroundTint = RCommon.color.stream_app_background,
                        action = ToggleCamera(it.isCameraEnabled)
                    ),
                    CallControlItem(
                        icon = if (it.isMicrophoneEnabled) {
                            RCommon.drawable.ic_mic_on
                        } else {
                            RCommon.drawable.ic_mic_off
                        },
                        iconTint = R.color.stream_black,
                        backgroundTint = RCommon.color.stream_app_background,
                        action = ToggleMicrophone(it.isMicrophoneEnabled)
                    )
                )
            )
        }
    }
}

private fun buildDefaultControlList(): List<CallControlItem> {
    return listOf(
        CallControlItem(
            icon = RCommon.drawable.ic_speaker_on,
            iconTint = R.color.stream_black,
            backgroundTint = RCommon.color.stream_app_background,
            action = ToggleSpeakerphone(isEnabled = true)
        ),
        CallControlItem(
            icon = RCommon.drawable.ic_videocam_on,
            iconTint = R.color.stream_black,
            backgroundTint = RCommon.color.stream_app_background,
            action = ToggleCamera(isEnabled = true)
        ),
        CallControlItem(
            icon = RCommon.drawable.ic_mic_on,
            iconTint = R.color.stream_black,
            backgroundTint = RCommon.color.stream_app_background,
            action = ToggleMicrophone(isEnabled = true)
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
