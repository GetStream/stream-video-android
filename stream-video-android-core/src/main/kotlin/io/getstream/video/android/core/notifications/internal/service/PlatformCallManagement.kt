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
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.notifications.internal.service.CallTriggers.TRIGGER_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallTriggers.TRIGGER_ONGOING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallTriggers.TRIGGER_OUTGOING_CALL
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.openapitools.client.models.CallAcceptedEvent
import org.openapitools.client.models.CallEndedEvent
import org.openapitools.client.models.CallRejectedEvent
import org.openapitools.client.models.CallSessionParticipantJoinedEvent
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class PlatformCallManagement
private constructor(
    private val scope: CoroutineScope, private val callManager: CallsManager
) {
    companion object {
        private val logger by taggedLogger("PlatformCallManagement")
        lateinit var instance: PlatformCallManagement

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

        @OptIn(ExperimentalContracts::class)
        fun isSupported(
            context: Context
        ): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 14+, check the TELECOM feature directly.
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELECOM)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {/*
                For API 26+ presume that the FEATURE_TELECOM feature is available if the device supports SIM.
                To check for SIM availability we check if the device is SIM capable
                instead of using TelephonyManager.getSimState(slot) to see if there is actual sim,
                we need to only know if the device can have SIM.

                According to the docs, most devices with SIM card have telecom implementation.
                https://developer.android.com/develop/connectivity/telecom/voip-app#supported_telecom_device
                */
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) && context.packageManager.hasSystemFeature(
                PackageManager.FEATURE_TELEPHONY_GSM
            )
        } else {
            false
        }

        // Factory
        /**
         * Create an instance of [PlatformCallManagement] to manage the current call.
         */
        @RequiresApi(Build.VERSION_CODES.O)
        fun initialize(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
            logger.e { "[telecom] Initializing..." }
            instance = PlatformCallManagement(
                scope = scope, callManager = CallsManager(context)
            )
        }
    }

    // Internal variables
    private val managedCalls: MutableMap<String, Pair<Int, CallControlScope>> = mutableMapOf()

    // API
    @RequiresApi(Build.VERSION_CODES.O)
    fun answerCall(callId: StreamCallId) {
        scope.launch {
            managedCalls[callId.id]?.let {
                it.second.answer(it.first)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun rejectCall(callId: StreamCallId) {
        scope.launch {
            managedCalls[callId.id]?.second?.disconnect(DisconnectCause(DisconnectCause.REJECTED))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun endCall(callId: StreamCallId) {
        scope.launch {
            managedCalls[callId.id]?.second?.disconnect(DisconnectCause(DisconnectCause.LOCAL))
        }
    }

    fun endAll() {
        scope.launch {
            managedCalls.forEach {
                it.value.second.disconnect(DisconnectCause(DisconnectCause.LOCAL))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addCall(
        callId: StreamCallId, displayName: String = callId.id, @CallTrigger trigger: String
    ) {
        scope.launch {
            val call = StreamVideo.instanceOrNull()?.call(callId.type, callId.id)
            call?.get() // update the call
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
            val type = when (callId.type) {
                "default" -> CALL_TYPE_VIDEO_CALL or CALL_TYPE_AUDIO_CALL
                else -> CALL_TYPE_AUDIO_CALL
            }

            logger.e { "[telecom] Adding call [${callId.cid}, $displayName, $trigger, $type, $direction]" }
            callManager.addCall(
                CallAttributesCompat(
                    displayName = displayName,
                    address = Uri.parse("https://getstream.io/video/join/${callId.cid}"),
                    direction = direction,
                    callType = type,
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
                val callScope = this
                logger.e { "[telecom] Call scope created for call: [${callId.cid}]" }

                managedCalls[callId.id] = Pair(type, callScope)
                scope.launch {
                    call?.subscribe { event ->
                        launch {
                            when (event) {
                                is CallRejectedEvent -> {
                                    callScope.disconnect(DisconnectCause(DisconnectCause.REJECTED))
                                }

                                is CallEndedEvent -> {
                                    callScope.disconnect(DisconnectCause(DisconnectCause.REMOTE))
                                }

                                is CallAcceptedEvent, is JoinCallResponseEvent, is CallSessionParticipantJoinedEvent -> {
                                    callScope.answer(type)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
