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

import android.content.Context
import android.telecom.DisconnectCause
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.internal.service.TelecomHelper
import io.getstream.video.android.core.notifications.internal.service.triggers.TelecomServiceLauncher
import io.getstream.video.android.core.notifications.internal.telecom.connection.SuccessIncomingTelecomConnection
import io.getstream.video.android.core.telecom.TelecomPermissions
import io.getstream.video.android.model.StreamCallId

class TelecomCallController(val context: Context) {
    private val telecomPermissions = TelecomPermissions()
    private val telecomHelper = TelecomHelper()

    fun onRejectFromNotification(call: Call) {
        onDeclineOngoingCall(call)
//        if (telecomPermissions.canUseTelecom(context)) {
//            if (telecomHelper.canUseJetpackTelecom()) {
//                val jetpackTelecomCall = call.state.jetpackTelecomRepository?.currentCall?.value
//                jetpackTelecomCall?.let {
//                    if (it is TelecomCall.Registered) {
//                        it.processAction(
//                            TelecomCallAction.Disconnect(
//                                DisconnectCause(
//                                    DisconnectCause.REJECTED
//                                )
//                            )
//                        )
//                    }
//                }
//                call.state.jetpackTelecomRepository = null
//            } else {
//                call.state.telecomConnection.value?.let {
//                    it.setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
//                    it.destroy()
//                }
//                call.state.updateTelecomConnection(null)
//            }
//        }
    }

    fun leaveCurrentCall(call: Call) {
        onCancelOutgoingCall(call)
    }

    fun onCancelOutgoingCall(call: Call) {
        if (telecomPermissions.canUseTelecom(context)) {
            if (telecomHelper.canUseJetpackTelecom()) {
                val jetpackTelecomCall = call.state.jetpackTelecomRepository?.currentCall?.value
                jetpackTelecomCall?.let {
                    if (it is TelecomCall.Registered) {
                        it.processAction(
                            TelecomCallAction.Disconnect(
                                DisconnectCause(
                                    DisconnectCause.CANCELED,
                                ),
                            ),
                        )
                    }
                }
            } else {
                val telecomConnection = call.state.telecomConnection.value
                if (telecomConnection != null && telecomConnection is SuccessIncomingTelecomConnection) {
                    telecomConnection.setDisconnected(DisconnectCause(DisconnectCause.CANCELED))
                }
            }
        }
    }

    fun onDeclineOngoingCall(call: Call) {
        if (telecomPermissions.canUseTelecom(context)) {
            if (telecomHelper.canUseJetpackTelecom()) {
                val jetpackTelecomCall = call.state.jetpackTelecomRepository?.currentCall?.value
                jetpackTelecomCall?.let {
                    if (it is TelecomCall.Registered) {
                        it.processAction(
                            TelecomCallAction.Disconnect(
                                DisconnectCause(
                                    DisconnectCause.REJECTED,
                                ),
                            ),
                        )
                    }
                }
            } else {
                val telecomConnection = call.state.telecomConnection.value
                if (telecomConnection != null && telecomConnection is SuccessIncomingTelecomConnection) {
                    telecomConnection.setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
                }
            }
        }
    }

    fun onAnswer(call: Call) {
        val telecomServiceLauncher = TelecomServiceLauncher()
        if (telecomPermissions.canUseTelecom(context)) {
            if (telecomHelper.canUseJetpackTelecom()) {
                val jetpackTelecomCall =
                    call.state.jetpackTelecomRepository?.currentCall?.value
                jetpackTelecomCall?.let {
                    if (it is TelecomCall.Registered) {
                        it.processAction(TelecomCallAction.Answer(!isVideoCall(call)))
                        it.processAction(TelecomCallAction.Activate)
                    }
                }
            } else {
                //TODO Rahul Seems wrong should be telecomServiceLauncher.addOnGoingCall()
                telecomServiceLauncher.addOutgoingCallToTelecom(
                    context,
                    callId = StreamCallId(call.type, call.id),
                    callDisplayName = "NOT SET YET", // TODO Rahul Later
                    isVideo = call.isVideoEnabled(),
                    streamVideo = StreamVideo.instance(),
                )
            }
        }
    }

    private fun isVideoCall(call: Call): Boolean {
        return call.hasCapability(OwnCapability.SendVideo) || call.isVideoEnabled()
    }
}
