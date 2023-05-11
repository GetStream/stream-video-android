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

package io.getstream.video.android.core.call.state

import io.getstream.video.android.core.audio.AudioDevice
import io.getstream.video.android.model.User

/**
 * Represents various actions users can take while in a call.
 */
public sealed interface CallAction

/**
 * Action to toggle if the speakerphone is on or off.
 */
public data class ToggleSpeakerphone(
    val isEnabled: Boolean
) : CallAction

/**
 * Action to select an audio device for playback.
 */
public data class SelectAudioDevice(
    val audioDevice: AudioDevice
) : CallAction

/**
 * Action to toggle if the camera is on or off.
 */
public data class ToggleCamera(
    val isEnabled: Boolean
) : CallAction

/**
 * Action to toggle if the microphone is on or off.
 */
public data class ToggleMicrophone(
    val isEnabled: Boolean
) : CallAction

/**
 * Action to flip the active camera.
 */
public object FlipCamera : CallAction

/**
 * Action to accept a call in Incoming Call state.
 */
public object AcceptCall : CallAction

/**
 * Action used to cancel an outgoing call.
 */
public object CancelCall : CallAction

/**
 * Action to decline an oncoming call.
 */
public object DeclineCall : CallAction

/**
 * Action to leave the call.
 */
public object LeaveCall : CallAction

/**
 * Action to show a chat dialog.
 */
public object ChatDialog : CallAction

/**
 * Action to invite other users to a call.
 */
public data class InviteUsersToCall(
    val users: List<io.getstream.video.android.model.User>
) : CallAction

/**
 * Used to trigger Screen UI configuration changes when observing screen share sessions.
 *
 * @param isFullscreen If we should show full screen UI or not. This is usable only with landscape.
 * @param isLandscape If the orientation should be landscape. Can be used without full screen.
 */
public data class ToggleScreenConfiguration(
    val isFullscreen: Boolean,
    val isLandscape: Boolean
) : CallAction

/**
 * Used to set the state to showing call participant info.
 */
public object ShowCallParticipantInfo : CallAction

/**
 * Custom action used to handle any custom behavior with the given [data], such as opening chat,
 * inviting people, sharing the screen and more.
 */
public open class CustomAction(
    val data: Map<Any, Any> = emptyMap()
) : CallAction
