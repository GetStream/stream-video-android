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

package io.getstream.video.android.core.call.state

import io.getstream.video.android.core.ParticipantState

/**
 * Represents various actions users can take while in a call.
 */
public sealed interface CallAction

/**
 * Action to toggle if the speakerphone is on or off.
 */
public data class ToggleSpeakerphone(
    val isEnabled: Boolean,
) : CallAction

/**
 * Action to toggle if the camera is on or off.
 */
public data class ToggleCamera(
    val isEnabled: Boolean,
) : CallAction

/**
 * Action to toggle if the microphone is on or off.
 */
public data class ToggleMicrophone(
    val isEnabled: Boolean,
) : CallAction

/**
 * Action to toggle audio bitrate profile between VOICE_STANDARD_UNSPECIFIED and MUSIC_HIGH_QUALITY.
 */
public data class ToggleHifiAudio(
    val isHifiAudioEnabled: Boolean,
) : CallAction

/**
 * Action to flip the active camera.
 */
public data object FlipCamera : CallAction

/**
 * Action to accept a call in Incoming Call state.
 */
public data object AcceptCall : CallAction

/**
 * Action used to cancel an outgoing call.
 */
public data object CancelCall : CallAction

/**
 * Action to decline an oncoming call.
 */
public data object DeclineCall : CallAction

/**
 * Action to leave the call.
 */
public data object LeaveCall : CallAction

/**
 * Action to show a chat dialog.
 */
public data object ChatDialog : CallAction

/**
 * Action to show a settings.
 */
public data class Settings(
    val isEnabled: Boolean,
) : CallAction

public data class ClosedCaptionsAction(
    val isEnabled: Boolean,
) : CallAction

/**
 * Action to show a reaction popup.
 */
public data object Reaction : CallAction

/**
 * Action to show a layout chooser.
 */
public data object ChooseLayout : CallAction

/**
 * Action to invite other users to a call.
 */
public data class InviteUsersToCall(
    val users: List<ParticipantState>,
) : CallAction

/**
 * Used to trigger Screen UI configuration changes when observing screen share sessions.
 *
 * @param isFullscreen If we should show full screen UI or not. This is usable only with landscape.
 * @param isLandscape If the orientation should be landscape. Can be used without full screen.
 */
public data class ToggleScreenConfiguration(
    val isFullscreen: Boolean,
    val isLandscape: Boolean,
) : CallAction

/**
 * Used to set the state to showing call participant info.
 */
public data object ShowCallParticipantInfo : CallAction

/**
 * Custom action used to handle any custom behavior with the given [data] and [tag], such as opening chat,
 * inviting people, sharing the screen and more.
 */
public open class CustomAction(
    val data: Map<Any, Any> = emptyMap(),
    val tag: String,
) : CallAction
