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

package io.getstream.video.android.core.telecom.bridge.streamtotelecom

import android.os.Build
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallControlScope
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.REJECT_REASON_TIMEOUT
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.telecom.TELECOM_LOG_TAG
import io.getstream.video.android.core.telecom.TelecomCall
import kotlinx.coroutines.launch
import org.openapitools.client.models.CallAcceptedEvent
import org.openapitools.client.models.CallEndedEvent
import org.openapitools.client.models.CallRejectedEvent
import kotlin.getValue

internal fun defaultStreamToTelecomEventBridgeFactory(): StreamToTelecomEventBridgeFactory {
    return DefaultStreamToTelecomEventBridgeFactory()
}

internal class DefaultStreamToTelecomEventBridgeFactory : StreamToTelecomEventBridgeFactory {
    override fun create(telecomCall: TelecomCall): StreamToTelecomEventBridge {
        return DefaultStreamToTelecomEventBridge(telecomCall)
    }
}

internal class DefaultStreamToTelecomEventBridge(private val telecomCall: TelecomCall) : StreamToTelecomEventBridge {
    private val logger by taggedLogger(TELECOM_LOG_TAG)
    private val streamCall = telecomCall.streamCall

    @RequiresApi(Build.VERSION_CODES.O)
    override fun processEvents(callControlScope: CallControlScope) {
        streamCall.subscribeFor(
            CallAcceptedEvent::class.java,
            CallRejectedEvent::class.java,
            CallEndedEvent::class.java,
        ) { event ->
            logger.d { "[DefaultStreamToTelecomEventBridge#onEvent] Received event: ${event.getEventType()}" }

            with(callControlScope) {
                launch {
                    when (event) {
                        is CallAcceptedEvent -> {
                            answer(telecomCall.mediaType).let {
                                logger.d { "[DefaultStreamToTelecomEventBridge#onEvent] Answered with result: $it" }
                            }
                        }
                        is CallRejectedEvent -> {
                            logger.d { "[DefaultStreamToTelecomEventBridge#onEvent] Reject reason: ${event.reason}" }

                            /*
                            Possible values for DisconnectCause:
                            LOCAL - Disconnected because of a local user-initiated action, such as hanging up.
                            REMOTE - Disconnected because the remote party hung up an ongoing call, or because an outgoing call was not answered by the remote party.
                            REJECTED - Disconnected because the user rejected an incoming call.
                            MISSED - Disconnected because there was no response to an incoming call.
                             */
                            disconnect(
                                DisconnectCause(
                                    when (event.reason) {
                                        RejectReason.Cancel.alias -> DisconnectCause.LOCAL
                                        RejectReason.Decline.alias -> DisconnectCause.REJECTED
                                        REJECT_REASON_TIMEOUT -> DisconnectCause.MISSED
                                        else -> DisconnectCause.REMOTE
                                    },
                                ),
                            ).let {
                                logger.d { "[DefaultStreamToTelecomEventBridge#onEvent] Disconnected with result: $it" }
                            }
                        }
                        is CallEndedEvent -> {
                            disconnect(DisconnectCause(DisconnectCause.REMOTE)).let {
                                logger.d { "[DefaultStreamToTelecomEventBridge#onEvent] Disconnected with result: $it" }
                            }
                        }
                    }
                }
            }
        }
    }
}
