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

package io.getstream.video.android.compose.state.ui.participants

import androidx.compose.runtime.Stable
import io.getstream.video.android.model.User

/**
 * Actions which can be taken in the participants info UI in a call.
 */
@Stable
public sealed interface ParticipantInfoAction

/**
 * Triggers a mute change state for the active participant.
 *
 * @param isEnabled If the microphone is enabled or not.
 */
@Stable
public data class ChangeMuteState(
    val isEnabled: Boolean,
) : ParticipantInfoAction

/**
 * Triggers an invite action for users.
 *
 * @param users The users to invite.
 */
@Stable
public data class InviteUsers(val users: List<User>) : ParticipantInfoAction
