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

package io.getstream.video.android.core.telecom

import android.os.Build
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallControlScope
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.model.RejectReason
import kotlinx.coroutines.launch
import org.openapitools.client.models.CallAcceptedEvent
import org.openapitools.client.models.CallEndedEvent
import org.openapitools.client.models.CallRejectedEvent
import kotlin.getValue

@RequiresApi(Build.VERSION_CODES.O)
internal class StreamToTelecomEventBridge(private val telecomCall: TelecomCall) {
    private val logger by taggedLogger(TELECOM_LOG_TAG)
    private val streamCall = telecomCall.streamCall

    fun processEvents(callControlScope: CallControlScope) {
        streamCall.subscribeFor(
            CallAcceptedEvent::class.java,
            CallRejectedEvent::class.java,
            CallEndedEvent::class.java,
        ) { event ->
            logger.d { "[StreamToTelecomEventMapper#onEvent] Received event: ${event.getEventType()}" }
            with(callControlScope) {
                launch {
                    when (event) {
                        is CallAcceptedEvent -> {
                            logger.d { "[StreamToTelecomEventBridge#onEvent] Will call CallControlScope#answer" }
                            answer(telecomCall.mediaType)
                        }
                        is CallRejectedEvent -> {
                            logger.d { "[StreamToTelecomEventBridge#onEvent] Will call CallControlScope#disconnect" }
                            disconnect(
                                DisconnectCause(
                                    when (event.reason) {
                                        RejectReason.Cancel.alias -> DisconnectCause.LOCAL
                                        RejectReason.Decline.alias -> DisconnectCause.REJECTED
                                        else -> DisconnectCause.REMOTE
                                    },
                                ),
                            )
                            event.reason
                        }
                        is CallEndedEvent -> {
                            logger.d { "[StreamToTelecomEventBridge#onEvent] Will call CallControlScope#disconnect" }
                            disconnect(DisconnectCause(DisconnectCause.LOCAL))
                        }
                    }
                }
            }
        }
    }
}
