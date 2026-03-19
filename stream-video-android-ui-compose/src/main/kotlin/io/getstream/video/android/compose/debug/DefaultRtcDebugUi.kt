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

package io.getstream.video.android.compose.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.call.connection.trackers.DebugPeerConnectionStateTracker
import org.webrtc.PeerConnection
import kotlin.math.abs

private const val ENABLE_DEBUG_UI = true

@Composable
internal fun RtcDebugUi(call: Call) {
    if (ENABLE_DEBUG_UI) {
        DefaultRtcDebugUi(call)
    }
}

@Composable
private fun DefaultRtcDebugUi(call: Call) {
    val publisherConnectionState by call.state.rtcDebugger
        .getDebugPublisherConnectionState()
        .collectAsStateWithLifecycle(null)

    val subscriberConnectionState by call.state.rtcDebugger
        .getDebugSubscriberConnectionState()
        .collectAsStateWithLifecycle(null)

    val firstPackReceived by call.state.rtcDebugger
        .firstPacketReceivedFlow()
        .collectAsStateWithLifecycle(false)

    val startRingingStateActiveTransitionTime by call.state.startRingingStateActiveTransitionTime.collectAsStateWithLifecycle()
    val isIncoming =
        call.state.previousRingingStates.any { it is RingingState.Incoming }
    val isOutgoing =
        call.state.previousRingingStates.any { it is RingingState.Outgoing }

    val callStateTimeLine by call.state.callStateTimeLine.collectAsStateWithLifecycle()

    Column {
        if (isIncoming) {
            val incomingPublisherUiText = incomingPublisherUiText(
                publisherConnectionState,
                call.state.publisherConnectionStateTracker,
                callStateTimeLine.joinFinishTime,
            )
            val incomingSubscriberUiText = incomingSubscriberUiText(
                subscriberConnectionState,
                call.state.subscriberConnectionStateTracker,
                callStateTimeLine.joinFinishTime,
            )
            Text(
                text = "Callee",
                color = Color.White,
                fontSize = 16.sp,
            )
            Text(
                text = "Publisher: $incomingPublisherUiText",
                color = Color.White,
                fontSize = 16.sp,
            )
            Text(
                text = "Subscriber: $incomingSubscriberUiText",
                color = Color.White,
                fontSize = 16.sp,
            )
        } else if (isOutgoing) {
            val outgoingPublisherUiText = outgoingPublisherUiText(
                publisherConnectionState,
                call.state.publisherConnectionStateTracker,
                callStateTimeLine.joinFinishTime,
            )
            val outgoingSubscriberUiText = outgoingSubscriberUiText(
                subscriberConnectionState,
                call.state.subscriberConnectionStateTracker,
                callStateTimeLine.acceptedEventTime,
            )
            Text(
                text = "Caller",
                color = Color.White,
                fontSize = 16.sp,
            )
            Text(
                text = "Publisher: $outgoingPublisherUiText",
                color = Color.White,
                fontSize = 16.sp,
            )
            Text(
                text = "Subscriber: $outgoingSubscriberUiText",
                color = Color.White,
                fontSize = 16.sp,
            )
        }
        Text(
            text = getPeerConnectionText(
                call,
                publisherConnectionState,
                true,
                startRingingStateActiveTransitionTime,
                isIncoming,
            ),
            color = Color.White,
            fontSize = 16.sp,
        )

        Text(
            text = getPeerConnectionText(
                call,
                subscriberConnectionState,
                false,
                startRingingStateActiveTransitionTime,
                isIncoming,
            ),
            color = Color.White,
            fontSize = 16.sp,
        )

        Text(
            text = "First Packet Received: $firstPackReceived",
            color = Color.White,
            fontSize = 16.sp,
        )
        Text(
            text = "------------------------------------------------------------",
            color = Color.White,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Time line",
            color = Color.White,
            fontSize = 18.sp,
        )

        if (isIncoming) {
            Text(
                text = "Call Accept Start time: ${callStateTimeLine.acceptStartTime}",
                color = Color.White,
                fontSize = 16.sp,
            )

            Text(
                text = "Call Accept Finish time: ${callStateTimeLine.acceptFinishTime}, time taken: ${callStateTimeLine.acceptFinishTime - callStateTimeLine.acceptStartTime} ",
                color = Color.White,
                fontSize = 16.sp,
            )

            Text(
                text = "Call Join Start time: ${callStateTimeLine.joinStartTime}",
                color = Color.White,
                fontSize = 16.sp,
            )

            Text(
                text = "Call Join Finish time: ${callStateTimeLine.joinFinishTime}, time taken: ${callStateTimeLine.joinFinishTime - callStateTimeLine.joinStartTime}",
                color = Color.White,
                fontSize = 16.sp,
            )
        }

        if (isOutgoing) {
            Text(
                text = "Call Accept Event Time: ${callStateTimeLine.acceptedEventTime}",
                color = Color.White,
                fontSize = 16.sp,
            )
        }

        startRingingStateActiveTransitionTime?.let { startRingingStateActiveTransitionTime ->
            Text(
                text = "Time at transitionToActiveState $startRingingStateActiveTransitionTime",
                color = Color.White,
                fontSize = 16.sp,
            )
            val publisherConnectedStateTime = call.state.publisherConnectionStateTracker.stateTimeline[PeerConnection.PeerConnectionState.CONNECTED]
            publisherConnectedStateTime?.let {
                Text(
                    text = "Publisher Connected State time: $publisherConnectedStateTime, diff: ${
                        abs(
                            startRingingStateActiveTransitionTime - it,
                        )
                    }",
                    color = Color.White,
                    fontSize = 16.sp,
                )
            }

            val subscriberConnectedStateTime = call.state.subscriberConnectionStateTracker.stateTimeline[PeerConnection.PeerConnectionState.CONNECTED]
            subscriberConnectedStateTime?.let { it ->
                Text(
                    text = "Subscriber Connected State time: $subscriberConnectedStateTime, diff: ${abs(
                        startRingingStateActiveTransitionTime - it,
                    )}",
                    color = Color.White,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

private fun getPeerConnectionText(
    call: Call,
    peerConnectionState: PeerConnection.PeerConnectionState?,
    isPublisher: Boolean,
    startRingingStateActiveTransitionTime: Long?,
    isIncoming: Boolean,
): String {
    val peerConnectionTypeText = if (isPublisher) "Publisher" else "Subscriber"
    val peerConnectionStateTracker = if (isPublisher) call.state.publisherConnectionStateTracker else call.state.subscriberConnectionStateTracker

    var formattedText = ""
    peerConnectionState?.let {
        formattedText = when (it) {
            PeerConnection.PeerConnectionState.CONNECTED -> {
                call.state.publisherConnectionStateTracker
                call.state.subscriberConnectionStateTracker
                val durationInMillis = peerConnectionStateTracker.getDuration(
                    PeerConnection.PeerConnectionState.CONNECTING,
                    PeerConnection.PeerConnectionState.CONNECTED,
                )
                val timeAtConnectedState = peerConnectionStateTracker.stateTimeline[PeerConnection.PeerConnectionState.CONNECTED]
                if (startRingingStateActiveTransitionTime != null) {
                    val timeDiffFromTransitionTime =
                        abs(timeAtConnectedState!! - startRingingStateActiveTransitionTime)
                    if (isIncoming) {
                        "$peerConnectionTypeText: ${peerConnectionState.name} in $durationInMillis ms, diff: $timeDiffFromTransitionTime"
                    } else {
                        "$peerConnectionTypeText: ${peerConnectionState.name} in $durationInMillis ms, diff: $timeDiffFromTransitionTime"
                    }
                } else {
                    "$peerConnectionTypeText: ${peerConnectionState.name} in $durationInMillis ms"
                }
            }

            else -> "$peerConnectionTypeText: ${peerConnectionState.name}"
        }
    }

    return formattedText
}

private fun incomingPublisherUiText(
    state: PeerConnection.PeerConnectionState?,
    publisherConnectionStateTracker: DebugPeerConnectionStateTracker,
    joinFinishTime: Long,
): String {
    if (state == PeerConnection.PeerConnectionState.CONNECTED) {
        val timeDiff = publisherConnectionStateTracker.stateTimeline[PeerConnection.PeerConnectionState.CONNECTED]!! - joinFinishTime
        return "Connected in $timeDiff since JoinFinish"
    }
    return ""
}

private fun incomingSubscriberUiText(
    state: PeerConnection.PeerConnectionState?,
    subscriberConnectionStateTracker: DebugPeerConnectionStateTracker,
    joinFinishTime: Long,
): String {
    if (state == PeerConnection.PeerConnectionState.CONNECTED) {
        val timeDiff =
            subscriberConnectionStateTracker.stateTimeline[PeerConnection.PeerConnectionState.CONNECTED]!! - joinFinishTime
        return "Connected in $timeDiff since JoinFinish"
    }
    return ""
}

private fun outgoingPublisherUiText(
    state: PeerConnection.PeerConnectionState?,
    publisherConnectionStateTracker: DebugPeerConnectionStateTracker,
    joinFinishTime: Long,
): String {
    if (state == PeerConnection.PeerConnectionState.CONNECTED) {
        val timeDiff =
            publisherConnectionStateTracker.stateTimeline[PeerConnection.PeerConnectionState.CONNECTED]!! - joinFinishTime
        return "Connected in $timeDiff since JoinFinish"
    }
    return ""
}

private fun outgoingSubscriberUiText(
    state: PeerConnection.PeerConnectionState?,
    subscriberConnectionStateTracker: DebugPeerConnectionStateTracker,
    callAcceptEventTime: Long,
): String {
    if (state == PeerConnection.PeerConnectionState.CONNECTED) {
        val timeDiff =
            subscriberConnectionStateTracker.stateTimeline[PeerConnection.PeerConnectionState.CONNECTED]!! - callAcceptEventTime
        return "Connected in $timeDiff since CallAcceptEvent"
    }
    return ""
}
