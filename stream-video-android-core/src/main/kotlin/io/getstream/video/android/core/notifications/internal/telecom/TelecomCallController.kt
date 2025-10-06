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

/**
 * Valid disconnected cause: [DisconnectCause.LOCAL, DisconnectCause.REMOTE, DisconnectCause.MISSED, or DisconnectCause.REJECTED]
 */
class TelecomCallController(val context: Context) {
    private val telecomPermissions = TelecomPermissions()
    private val telecomHelper = TelecomHelper()

    fun onRejectFromNotification(call: Call) {
        onDeclineOngoingCall(call)
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
                                    DisconnectCause.LOCAL,
                                ),
                                DisconnectSource.PHONE,
                            ),
                        )
                    }
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
                                    DisconnectCause.LOCAL,
                                ),
                                DisconnectSource.PHONE,
                            ),
                        )
                    }
                }
            }
        }
    }

    fun onAnswer(call: Call) {
        if (telecomPermissions.canUseTelecom(context)) {
            if (telecomHelper.canUseJetpackTelecom()) {
                val jetpackTelecomCall =
                    call.state.jetpackTelecomRepository?.currentCall?.value
                jetpackTelecomCall?.let {
                    if (it is TelecomCall.Registered) {
                        it.processAction(TelecomCallAction.Activate)
                        it.processAction(TelecomCallAction.Answer(!isVideoCall(call)))
                    }
                }
            }
        }
    }

    private fun isVideoCall(call: Call): Boolean {
        return call.hasCapability(OwnCapability.SendVideo) || call.isVideoEnabled()
    }
}
