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

package io.getstream.video.android.compose.state.ui.internal

/**
 * Represents the mode of the CallParticipantsInfo menu.
 */
public sealed interface CallParticipantInfoMode

/**
 * Shown when the user is observing the active list of participants.
 */
internal object ParticipantListMode : CallParticipantInfoMode

/**
 * Shown when the user is in the process of inviting people to an active call.
 */
internal object ParticipantInvitesMode : CallParticipantInfoMode
