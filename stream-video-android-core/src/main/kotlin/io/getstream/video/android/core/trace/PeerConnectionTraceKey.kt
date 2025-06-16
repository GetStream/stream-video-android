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

import io.getstream.video.android.core.internal.InternalStreamVideoApi

/**
 * All trace-key strings emitted by the peer-connection layer.
 */
@InternalStreamVideoApi
internal enum class PeerConnectionTraceKey(val value: String) {

    CREATE("create"),
    JOIN_REQUEST("joinRequest"),
    ON_ICE_CANDIDATE("onicecandidate"),
    ON_TRACK("ontrack"),
    ON_SIGNALING_STATE_CHANGE("onsignalingstatechange"),
    ON_ICE_CONNECTION_STATE_CHANGE("oniceconnectionstatechange"),
    ON_ICE_GATHERING_STATE_CHANGE("onicegatheringstatechange"),
    ON_CONNECTION_STATE_CHANGE("onconnectionstatechange"),
    ON_NEGOTIATION_NEEDED("onnegotiationneeded"),
    ON_DATA_CHANNEL("ondatachannel"),
    CLOSE("close"),
    CREATE_OFFER("createOffer"),
    CREATE_ANSWER("createAnswer"),
    SET_LOCAL_DESCRIPTION("setLocalDescription"),
    SET_REMOTE_DESCRIPTION("setRemoteDescription"),
    ADD_ICE_CANDIDATE("addIceCandidate"),
    CHANGE_PUBLISH_QUALITY("changePublishQuality"),
    CHANGE_PUBLISH_OPTIONS("changePublishOptions"),
    GO_AWAY("goAway"),
    SFU_ERROR("error"),
    CALL_ENDED("callEnded"),
    ;

    /** Log / serialise using the original string value. */
    override fun toString(): String = value
}
