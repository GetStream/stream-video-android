/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications.internal.telecom

import android.os.ParcelUuid
import android.os.Parcelable
import android.telecom.DisconnectCause
import kotlinx.parcelize.Parcelize

/**
 * Simple interface to represent related call actions to communicate with the registered call scope
 * in the [JetpackTelecomRepository.registerCall]
 *
 * Note: we are using [Parcelize] to make the actions parcelable so they can be directly used in the
 * call notification.
 */
sealed interface TelecomCallAction : Parcelable {
    @Parcelize
    data class Answer(val isAudioCall: Boolean) : TelecomCallAction

    @Parcelize
    data class Disconnect(
        val cause: DisconnectCause,
        val source: InteractionSource,
    ) : TelecomCallAction

    @Parcelize
    object Hold : TelecomCallAction

    @Parcelize
    object Activate : TelecomCallAction

    @Parcelize
    data class ToggleMute(val isMute: Boolean) : TelecomCallAction

    @Parcelize
    data class SwitchAudioEndpoint(val endpointId: ParcelUuid) : TelecomCallAction

    @Parcelize
    data class TransferCall(val endpointId: ParcelUuid) : TelecomCallAction
}

enum class InteractionSource {
    PHONE, WEARABLE
}
