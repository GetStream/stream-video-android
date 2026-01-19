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

package io.getstream.video.android.core.notifications.internal.telecom.jetpack

import android.os.ParcelUuid
import android.telecom.DisconnectCause
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallEndpointCompat
import kotlinx.coroutines.channels.Channel

/**
 * Custom representation of a call state.
 */
internal sealed class TelecomCall {

    /**
     * There is no current or past calls in the stack
     */
    object None : TelecomCall()

    /**
     * Represents a registered call with the telecom stack with the values provided by the
     * Telecom SDK
     */
    data class Registered(
        val id: ParcelUuid,
        val callAttributes: CallAttributesCompat,
        val isActive: Boolean,
        val isOnHold: Boolean,
        val isMuted: Boolean,
        val errorCode: Int?,
        val currentCallEndpoint: CallEndpointCompat?,
        val availableCallEndpoints: List<CallEndpointCompat>,
        internal val actionSource: Channel<TelecomCallAction>,
    ) : TelecomCall() {

        /**
         * @return true if it's an incoming registered call, false otherwise
         */
        fun isIncoming() = callAttributes.direction == CallAttributesCompat.DIRECTION_INCOMING

        /**
         * Sends an action to the call session. It will be processed if it's still registered.
         *
         * @return true if the action was sent, false otherwise
         */
        fun processAction(action: TelecomCallAction): Boolean {
            return actionSource.trySend(action).isSuccess
        }
    }

    /**
     * Represent a previously registered call that was disconnected
     */
    data class Unregistered(
        val id: ParcelUuid,
        val callAttributes: CallAttributesCompat,
        val disconnectCause: DisconnectCause,
    ) : TelecomCall()
}
