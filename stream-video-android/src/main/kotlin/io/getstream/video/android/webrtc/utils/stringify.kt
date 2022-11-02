/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.webrtc.utils

import org.webrtc.IceCandidateErrorEvent
import org.webrtc.MediaStreamTrack
import org.webrtc.SessionDescription
import org.webrtc.audio.JavaAudioDeviceModule
import stream.video.sfu.models.PeerType

internal fun SessionDescription.stringify(): String = "SessionDescription(type=$type, description=$description)"

internal fun MediaStreamTrack.stringify(): String {
    return "MediaStreamTrack(id=${id()}, kind=${kind()}, enabled: ${enabled()}, state=${state()})"
}

internal fun IceCandidateErrorEvent.stringify(): String {
    return "IceCandidateErrorEvent(errorCode=$errorCode, $errorText, address=$address, port=$port, url=$url)"
}

internal fun JavaAudioDeviceModule.AudioSamples.stringify(): String {
    return "AudioSamples(audioFormat=$audioFormat, channelCount=$channelCount" +
        ", sampleRate=$sampleRate, data.size=${data.size})"
}

internal fun PeerType.stringify() = when (this) {
    PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED -> "publisher"
    PeerType.PEER_TYPE_SUBSCRIBER -> "subscriber"
}
