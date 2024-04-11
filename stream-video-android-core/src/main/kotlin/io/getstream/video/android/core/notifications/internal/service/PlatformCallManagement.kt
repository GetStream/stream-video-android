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

package io.getstream.video.android.core.notifications.internal.service

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallAttributesCompat.Companion.CALL_TYPE_AUDIO_CALL
import androidx.core.telecom.CallAttributesCompat.Companion.CALL_TYPE_VIDEO_CALL
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.notifications.internal.service.CallTriggers.TRIGGER_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallTriggers.TRIGGER_ONGOING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallTriggers.TRIGGER_OUTGOING_CALL
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.launch
import org.openapitools.client.models.CallAcceptedEvent
import org.openapitools.client.models.CallEndedEvent
import org.openapitools.client.models.CallRejectedEvent
import org.openapitools.client.models.CallSessionParticipantJoinedEvent
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class PlatformCallManagement(private val call: Call, private val callManager: CallsManager) {
    companion object {
        private val logger by taggedLogger("PlatformCallManagement")

        // Utility methods
        /**
         * Check if [PackageManager.FEATURE_TELECOM] is supported.
         * [isSupported] will only return true on API 26+
         */
        @OptIn(ExperimentalContracts::class)
        fun checkSupport(context: Context, supported: () -> Unit, notSupported: () -> Unit) {
            contract {
                callsInPlace(supported, InvocationKind.AT_MOST_ONCE)
                callsInPlace(notSupported, InvocationKind.AT_MOST_ONCE)
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                // Pre-API 26, not supported
                logger.e { "[telecom] Telecom API is not supported (API < 26)" }
                notSupported()
            } else if (isSupported(context)) {
                logger.e { "[telecom] Telecom API is supported." }
                supported()
            } else {
                logger.e { "[telecom] Telecom API is not supported." }
                notSupported()
            }
        }

        fun isSupported(
            context: Context,
        ): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 14+, check the TELECOM feature directly.
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELECOM)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            /*
                For API 26+ presume that the FEATURE_TELECOM feature is available if the device supports SIM.
                To check for SIM availability we check if the device is SIM capable
                instead of using TelephonyManager.getSimState(slot) to see if there is actual sim,
                we need to only know if the device can have SIM.

                According to the docs, most devices with SIM card have telecom implementation.
                https://developer.android.com/develop/connectivity/telecom/voip-app#supported_telecom_device
             */
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) && context.packageManager.hasSystemFeature(
                PackageManager.FEATURE_TELEPHONY_GSM,
            )
        } else {
            false
        }

        suspend fun addCall(callId: StreamCallId, displayName: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val call = StreamVideo.instance().call(callId.type, callId.id)
                val telecomIntegration = call.telecomIntegration
                telecomIntegration?.addCall(displayName, trigger = TRIGGER_INCOMING_CALL)
            }
        }
    }

    // Internal variables

    // API
    private lateinit var callControlScope: CallControlScope
    private var callType: Int = 0

    suspend fun answerCall() = callControlScope.answer(callType)

    suspend fun active() = callControlScope.setActive()

    suspend fun rejectCall() =
        callControlScope.disconnect(DisconnectCause(DisconnectCause.REJECTED))

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    suspend fun answeredElsewhere() =
        callControlScope.disconnect(DisconnectCause(DisconnectCause.ANSWERED_ELSEWHERE))

    suspend fun missed() = callControlScope.disconnect(DisconnectCause(DisconnectCause.MISSED))

    suspend fun endCall() = callControlScope.disconnect(DisconnectCause(DisconnectCause.LOCAL))

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun addCall(
        displayName: String = call.id,
        @CallTrigger trigger: String,
    ) {
        call.get() // update the call
        val direction = when (trigger) {
            TRIGGER_INCOMING_CALL -> {
                CallAttributesCompat.DIRECTION_INCOMING
            }

            TRIGGER_OUTGOING_CALL -> {
                CallAttributesCompat.DIRECTION_OUTGOING
            }

            TRIGGER_ONGOING_CALL -> {
                CallAttributesCompat.DIRECTION_OUTGOING
            }

            else -> {
                throw IllegalArgumentException("Wrong trigger: $trigger")
            }
        }
        callType = when (call.type) {
            "default" -> CALL_TYPE_VIDEO_CALL or CALL_TYPE_AUDIO_CALL
            else -> CALL_TYPE_AUDIO_CALL
        }

        logger.e { "[telecom] Adding call [${call.cid}, $displayName, $trigger, $callType, $direction]" }
        callManager.addCall(
            CallAttributesCompat(
                displayName = displayName,
                address = Uri.parse("https://getstream.io/video/join/${call.cid}"),
                direction = direction,
                callType = callType,
                callCapabilities = CallAttributesCompat.SUPPORTS_SET_INACTIVE,
            ),
            onDisconnect = {
                call?.leave()
            },
            onAnswer = {
                call?.join()
            },
            onSetActive = {
                call?.join()
            },
            onSetInactive = {
                call?.leave()
            },
        ) {
            callControlScope = this
            logger.e { "[telecom] Call scope created for call: [${call.cid}]" }
            call?.subscribe { event ->
                launch {
                    when (event) {
                        is CallRejectedEvent -> {
                            callControlScope.disconnect(DisconnectCause(DisconnectCause.REJECTED))
                        }

                        is CallEndedEvent -> {
                            callControlScope.disconnect(DisconnectCause(DisconnectCause.REMOTE))
                        }

                        is CallAcceptedEvent, is JoinCallResponseEvent, is CallSessionParticipantJoinedEvent -> {
                            callControlScope.answer(callType)
                        }
                    }
                }
            }
        }
    }

    suspend fun disconnect() {
        callControlScope.disconnect(DisconnectCause(DisconnectCause.UNKNOWN))
    }
}
