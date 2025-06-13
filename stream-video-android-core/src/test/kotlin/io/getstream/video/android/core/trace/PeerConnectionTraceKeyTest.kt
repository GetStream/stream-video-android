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

package io.getstream.video.android.core.trace

import org.junit.Assert.assertEquals
import org.junit.Test

class PeerConnectionTraceKeyTest {
    @Test
    fun `enum values have correct string representations`() {
        val expected = mapOf(
            PeerConnectionTraceKey.CREATE to "create",
            PeerConnectionTraceKey.JOIN_REQUEST to "joinRequest",
            PeerConnectionTraceKey.ON_ICE_CANDIDATE to "onicecandidate",
            PeerConnectionTraceKey.ON_TRACK to "ontrack",
            PeerConnectionTraceKey.ON_SIGNALING_STATE_CHANGE to "onsignalingstatechange",
            PeerConnectionTraceKey.ON_ICE_CONNECTION_STATE_CHANGE to "oniceconnectionstatechange",
            PeerConnectionTraceKey.ON_ICE_GATHERING_STATE_CHANGE to "onicegatheringstatechange",
            PeerConnectionTraceKey.ON_CONNECTION_STATE_CHANGE to "onconnectionstatechange",
            PeerConnectionTraceKey.ON_NEGOTIATION_NEEDED to "onnegotiationneeded",
            PeerConnectionTraceKey.ON_DATA_CHANNEL to "ondatachannel",
            PeerConnectionTraceKey.CLOSE to "close",
            PeerConnectionTraceKey.CREATE_OFFER to "createOffer",
            PeerConnectionTraceKey.CREATE_ANSWER to "createAnswer",
            PeerConnectionTraceKey.SET_LOCAL_DESCRIPTION to "setLocalDescription",
            PeerConnectionTraceKey.SET_REMOTE_DESCRIPTION to "setRemoteDescription",
            PeerConnectionTraceKey.ADD_ICE_CANDIDATE to "addIceCandidate",
            PeerConnectionTraceKey.CHANGE_PUBLISH_QUALITY to "changePublishQuality",
            PeerConnectionTraceKey.CHANGE_PUBLISH_OPTIONS to "changePublishOptions",
            PeerConnectionTraceKey.GO_AWAY to "goAway",
            PeerConnectionTraceKey.SFU_ERROR to "error",
            PeerConnectionTraceKey.CALL_ENDED to "callEnded",
        )
        for ((key, value) in expected) {
            assertEquals(value, key.value)
        }
    }

    @Test
    fun `toString returns correct value`() {
        for (key in PeerConnectionTraceKey.values()) {
            assertEquals(key.value, key.toString())
        }
    }
}
